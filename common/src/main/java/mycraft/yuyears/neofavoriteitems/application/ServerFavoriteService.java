package mycraft.yuyears.neofavoriteitems.application;

import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.domain.InteractionDecision;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import mycraft.yuyears.neofavoriteitems.persistence.DataPersistenceManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ServerFavoriteService {
    private static final Map<UUID, Long> revisionsByPlayer = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> bypassStateByPlayer = new ConcurrentHashMap<>();

    private ServerFavoriteService() {}

    public static ToggleResult toggleFavorite(ServerPlayer player, int inventoryIndex) {
        var logicalSlot = SlotMappingService.fromPlayerInventoryIndex(inventoryIndex);
        if (logicalSlot.isEmpty()) {
            DebugLogger.debug("Server rejected toggle: player={} inventoryIndex={} reason=invalid_index", player.getName().getString(), inventoryIndex);
            return ToggleResult.rejected();
        }

        FavoritesManager.getStateService().setPlayer(player.getUUID());
        FavoritesManager favoritesManager = FavoritesManager.getInstance();
        boolean isFavorite = favoritesManager.isSlotFavorite(logicalSlot.get());
        boolean hasItem = !player.getInventory().getItem(inventoryIndex).isEmpty();
        if (!isFavorite && !hasItem && !ConfigManager.getInstance().getConfig().general.lockEmptySlots) {
            DebugLogger.debug("Server rejected toggle: player={} inventoryIndex={} reason=empty_slot", player.getName().getString(), inventoryIndex);
            return ToggleResult.rejected();
        }

        favoritesManager.toggleSlotFavorite(logicalSlot.get());
        DataPersistenceManager.getInstance().saveData(player.getUUID());
        boolean nowFavorite = favoritesManager.isSlotFavorite(logicalSlot.get());
        long revision = nextRevision(player);
        DebugLogger.debug(
            "Server toggled favorite: player={} inventoryIndex={} nowLocked={} revision={}",
            player.getName().getString(),
            inventoryIndex,
            nowFavorite,
            revision
        );
        return ToggleResult.accepted(inventoryIndex, nowFavorite, revision, favoritesManager.getFavoriteSlots());
    }

    public static Set<Integer> getFavoritesFor(ServerPlayer player) {
        FavoritesManager.getStateService().setPlayer(player.getUUID());
        return FavoritesManager.getStateService().getFavoriteSlots();
    }

    public static long currentRevision(ServerPlayer player) {
        return revisionsByPlayer.getOrDefault(player.getUUID(), 0L);
    }

    public static void resetRevision(ServerPlayer player) {
        revisionsByPlayer.put(player.getUUID(), 0L);
        bypassStateByPlayer.put(player.getUUID(), false);
    }

    public static void updateBypassState(ServerPlayer player, boolean held) {
        bypassStateByPlayer.put(player.getUUID(), held);
        DebugLogger.debug("Server updated bypass key state: player={} held={}", player.getName().getString(), held);
    }

    public static void clearPlayerState(Player player) {
        revisionsByPlayer.remove(player.getUUID());
        bypassStateByPlayer.remove(player.getUUID());
    }

    public static boolean shouldCancelMenuClick(AbstractContainerMenu menu, Player player, int slotId, int button, ClickType clickType) {
        if (player == null || slotId < 0 || slotId >= menu.slots.size()) {
            return false;
        }

        Slot slot = menu.slots.get(slotId);
        int inventoryIndex = resolvePlayerInventoryIndex(slot, player);
        if (inventoryIndex < 0) {
            return false;
        }

        FavoritesManager.getInstance().setPlayer(player.getUUID());
        if (clickType == ClickType.SWAP && shouldCancelSwap(player, inventoryIndex, button, slot.hasItem())) {
            DebugLogger.debug(
                "Server canceled menu swap: player={} inventoryIndex={} slotId={} button={}",
                player.getName().getString(),
                inventoryIndex,
                slotId,
                button
            );
            return true;
        }
        if (clickType == ClickType.QUICK_MOVE && shouldCancelQuickMoveTarget(player, slot.getItem())) {
            DebugLogger.debug(
                "Server canceled quick move into locked target: player={} sourceInventoryIndex={} slotId={}",
                player.getName().getString(),
                inventoryIndex,
                slotId
            );
            return true;
        }

        var decision = evaluateExistingItem(player, inventoryIndex, toInteractionType(clickType), slot.hasItem());
        if (decision.denied()) {
            DebugLogger.debug(
                "Server canceled menu click: player={} inventoryIndex={} slotId={} clickType={} button={}",
                player.getName().getString(),
                inventoryIndex,
                slotId,
                clickType,
                button
            );
            return true;
        }
        return false;
    }

    public static boolean shouldCancelOffhandSwap(Player player) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        return shouldCancelSwap(
            player,
            player.getInventory().selected,
            40,
            !player.getInventory().getItem(player.getInventory().selected).isEmpty()
        );
    }

    public static boolean shouldPreventSlotPickup(Slot slot, Player player) {
        if (!isServerPlayerInventorySlot(slot, player)) {
            return false;
        }

        FavoritesManager.getInstance().setPlayer(player.getUUID());
        int inventoryIndex = resolvePlayerInventoryIndex(slot, player);
        var decision = InteractionGuardService.getInstance().evaluate(
            inventoryIndex,
            InteractionType.CLICK,
            isBypassKeyHeld(player),
            slot.hasItem()
        );
        if (decision.denied()) {
            DebugLogger.debug(
                "Server prevented slot pickup: player={} inventoryIndex={} reason={}",
                player.getName().getString(),
                inventoryIndex,
                decision.reason()
            );
            return true;
        }
        return false;
    }

    public static boolean shouldPreventSlotPlace(Slot slot, Player player, ItemStack incomingStack) {
        if (!isServerPlayerInventorySlot(slot, player)) {
            return false;
        }

        FavoritesManager.getInstance().setPlayer(player.getUUID());
        int inventoryIndex = resolvePlayerInventoryIndex(slot, player);
        var decision = InteractionGuardService.getInstance().evaluateIncomingItem(
            inventoryIndex,
            InteractionType.CLICK,
            isBypassKeyHeld(player),
            incomingStack != null && !incomingStack.isEmpty()
        );
        if (decision.denied()) {
            DebugLogger.debug(
                "Server prevented slot place: player={} inventoryIndex={} reason={}",
                player.getName().getString(),
                inventoryIndex,
                decision.reason()
            );
            return true;
        }
        return false;
    }

    public static boolean shouldPreventInventoryRemove(Inventory inventory, int inventoryIndex) {
        if (!isServerPlayerInventoryIndex(inventory, inventoryIndex)) {
            return false;
        }

        Player player = inventory.player;
        FavoritesManager.getInstance().setPlayer(player.getUUID());
        var decision = InteractionGuardService.getInstance().evaluate(
            inventoryIndex,
            InteractionType.QUICK_MOVE,
            isBypassKeyHeld(player),
            !inventory.getItem(inventoryIndex).isEmpty()
        );
        if (decision.denied()) {
            DebugLogger.debug(
                "Server prevented inventory remove: player={} inventoryIndex={} reason={}",
                player.getName().getString(),
                inventoryIndex,
                decision.reason()
            );
            return true;
        }
        return false;
    }

    public static boolean shouldPreventInventorySet(Inventory inventory, int inventoryIndex, ItemStack newStack) {
        if (!isServerPlayerInventoryIndex(inventory, inventoryIndex)) {
            return false;
        }

        ItemStack currentStack = inventory.getItem(inventoryIndex);
        if (ItemStack.matches(currentStack, newStack) && currentStack.getCount() == newStack.getCount()) {
            return false;
        }

        Player player = inventory.player;
        FavoritesManager.getInstance().setPlayer(player.getUUID());
        var decision = newStack.isEmpty()
            ? InteractionGuardService.getInstance().evaluate(
                inventoryIndex,
                InteractionType.QUICK_MOVE,
                isBypassKeyHeld(player),
                !currentStack.isEmpty()
            )
            : InteractionGuardService.getInstance().evaluateIncomingItem(
                inventoryIndex,
                InteractionType.QUICK_MOVE,
                isBypassKeyHeld(player),
                true
            );
        if (decision.denied()) {
            DebugLogger.debug(
                "Server prevented inventory set: player={} inventoryIndex={} reason={} currentEmpty={} newEmpty={}",
                player.getName().getString(),
                inventoryIndex,
                decision.reason(),
                currentStack.isEmpty(),
                newStack.isEmpty()
            );
            return true;
        }
        return false;
    }

    public static boolean shouldPreventInventoryReceive(Inventory inventory, int inventoryIndex, ItemStack incomingStack) {
        if (!isServerPlayerInventoryIndex(inventory, inventoryIndex) || incomingStack.isEmpty()) {
            return false;
        }

        ItemStack currentStack = inventory.getItem(inventoryIndex);
        ItemStack expectedStack = incomingStack.copy();
        if (!currentStack.isEmpty()) {
            expectedStack = currentStack.copy();
            expectedStack.grow(Math.min(
                incomingStack.getCount(),
                currentStack.getMaxStackSize() - currentStack.getCount()
            ));
        }
        return shouldPreventInventorySet(inventory, inventoryIndex, expectedStack);
    }

    public static boolean shouldProtectInventorySlotForExternalMove(Inventory inventory, int inventoryIndex) {
        if (!isServerPlayerInventoryIndex(inventory, inventoryIndex)) {
            return false;
        }

        Player player = inventory.player;
        FavoritesManager.getInstance().setPlayer(player.getUUID());
        ItemStack currentStack = inventory.getItem(inventoryIndex);
        var decision = InteractionGuardService.getInstance().evaluate(
            inventoryIndex,
            InteractionType.QUICK_MOVE,
            isBypassKeyHeld(player),
            !currentStack.isEmpty()
        );
        if (decision.denied()) {
            DebugLogger.debug(
                "Server protected inventory slot from external move: player={} inventoryIndex={} reason={} currentEmpty={}",
                player.getName().getString(),
                inventoryIndex,
                decision.reason(),
                currentStack.isEmpty()
            );
            return true;
        }
        return false;
    }

    private static boolean isServerPlayerInventorySlot(Slot slot, Player player) {
        return player != null
            && !player.level().isClientSide()
            && slot != null
            && resolvePlayerInventoryIndex(slot, player) >= 0;
    }

    private static boolean isServerPlayerInventoryIndex(Inventory inventory, int inventoryIndex) {
        return inventory != null
            && inventory.player != null
            && !inventory.player.level().isClientSide()
            && SlotMappingService.isPlayerInventoryIndex(inventoryIndex);
    }

    private static int resolvePlayerInventoryIndex(Slot slot, Player player) {
        if (slot == null || player == null || player.level().isClientSide()) {
            return -1;
        }
        Inventory inventory = player.getInventory();
        if (slot.container instanceof Inventory && slot.container == inventory) {
            return slot.getContainerSlot();
        }

        Object handler = invokeNoArg(slot, "getItemHandler");
        if (handler != null) {
            return resolveItemHandlerInventoryIndex(handler, slot.getContainerSlot(), inventory);
        }
        return -1;
    }

    private static int resolveItemHandlerInventoryIndex(Object handler, int slot, Inventory inventory) {
        String className = handler.getClass().getName();
        if (className.endsWith(".items.wrapper.InvWrapper")) {
            Object container = invokeNoArg(handler, "getInv");
            return container == inventory ? slot : -1;
        }
        if (className.endsWith(".items.wrapper.RangedWrapper")) {
            Object compose = readField(handler, "compose");
            Integer minSlot = readIntField(handler, "minSlot");
            if (compose != null && minSlot != null) {
                return resolveItemHandlerInventoryIndex(compose, minSlot + slot, inventory);
            }
        }
        return -1;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object readField(Object target, String name) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer readIntField(Object target, String name) {
        Object value = readField(target, name);
        return value instanceof Integer integer ? integer : null;
    }

    private static boolean isBypassKeyHeld(Player player) {
        return bypassStateByPlayer.getOrDefault(player.getUUID(), false);
    }

    private static InteractionDecision evaluateExistingItem(Player player, int inventoryIndex, InteractionType type, boolean hasItem) {
        return InteractionGuardService.getInstance().evaluate(
            inventoryIndex,
            type,
            isBypassKeyHeld(player),
            hasItem
        );
    }

    private static InteractionDecision evaluateIncomingItem(Player player, int inventoryIndex, InteractionType type, boolean incomingHasItem) {
        return InteractionGuardService.getInstance().evaluateIncomingItem(
            inventoryIndex,
            type,
            isBypassKeyHeld(player),
            incomingHasItem
        );
    }

    private static boolean shouldCancelSwap(Player player, int clickedInventoryIndex, int button, boolean clickedHasItem) {
        int partnerInventoryIndex = swapButtonToInventoryIndex(button);
        if (!SlotMappingService.isPlayerInventoryIndex(partnerInventoryIndex) || partnerInventoryIndex == clickedInventoryIndex) {
            return false;
        }

        FavoritesManager.getInstance().setPlayer(player.getUUID());
        Inventory inventory = player.getInventory();
        boolean partnerHasItem = !inventory.getItem(partnerInventoryIndex).isEmpty();
        return evaluateExistingItem(player, clickedInventoryIndex, InteractionType.SWAP, clickedHasItem).denied()
            || evaluateIncomingItem(player, clickedInventoryIndex, InteractionType.SWAP, partnerHasItem).denied()
            || evaluateExistingItem(player, partnerInventoryIndex, InteractionType.SWAP, partnerHasItem).denied()
            || evaluateIncomingItem(player, partnerInventoryIndex, InteractionType.SWAP, clickedHasItem).denied();
    }

    private static int swapButtonToInventoryIndex(int button) {
        if (button >= 0 && button <= 8) {
            return button;
        }
        if (button == 40) {
            return 40;
        }
        return -1;
    }

    private static boolean shouldCancelQuickMoveTarget(Player player, ItemStack sourceStack) {
        if (sourceStack.isEmpty()) {
            return false;
        }
        int targetInventoryIndex = equipmentInventoryIndexFor(sourceStack);
        return targetInventoryIndex >= 0
            && evaluateIncomingItem(player, targetInventoryIndex, InteractionType.QUICK_MOVE, true).denied();
    }

    private static int equipmentInventoryIndexFor(ItemStack stack) {
        Equipable equipable = Equipable.get(stack);
        if (equipable == null) {
            return -1;
        }
        EquipmentSlot slot = equipable.getEquipmentSlot();
        return switch (slot) {
            case HEAD -> 39;
            case CHEST -> 38;
            case LEGS -> 37;
            case FEET -> 36;
            case OFFHAND -> 40;
            default -> -1;
        };
    }

    private static InteractionType toInteractionType(ClickType clickType) {
        return switch (clickType) {
            case PICKUP, PICKUP_ALL -> InteractionType.CLICK;
            case QUICK_MOVE -> InteractionType.QUICK_MOVE;
            case SWAP -> InteractionType.SWAP;
            case THROW -> InteractionType.DROP;
            case QUICK_CRAFT -> InteractionType.DRAG;
            case CLONE -> InteractionType.UNKNOWN;
        };
    }

    private static long nextRevision(ServerPlayer player) {
        return revisionsByPlayer.merge(player.getUUID(), 1L, Long::sum);
    }

    public record ToggleResult(boolean accepted, int changedSlot, boolean nowFavorite, long revision, Set<Integer> favoriteSlots) {
        public static ToggleResult accepted(int changedSlot, boolean nowFavorite, long revision, Set<Integer> favoriteSlots) {
            return new ToggleResult(true, changedSlot, nowFavorite, revision, favoriteSlots);
        }

        public static ToggleResult rejected() {
            return new ToggleResult(false, -1, false, -1L, Set.of());
        }
    }
}
