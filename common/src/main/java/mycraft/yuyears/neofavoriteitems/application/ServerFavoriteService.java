package mycraft.yuyears.neofavoriteitems.application;

import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.domain.InteractionDecision;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import mycraft.yuyears.neofavoriteitems.persistence.DataPersistenceManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ServerFavoriteService {
    private static final Map<UUID, Long> revisionsByPlayer = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> bypassStateByPlayer = new ConcurrentHashMap<>();
    private static final Map<UUID, InstantSwapTransaction> instantSwapTransactionsByPlayer = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> correctionSyncTicksByPlayer = new ConcurrentHashMap<>();
    private static Consumer<ServerPlayer> correctionSyncSender = player -> {};

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
        if (!FavoriteLockRules.canToggleFavorite(isFavorite, hasItem, ConfigManager.getInstance().getConfig())) {
            DebugLogger.debug("Server rejected toggle: player={} inventoryIndex={} reason=empty_slot", player.getName().getString(), inventoryIndex);
            return ToggleResult.rejected();
        }

        favoritesManager.toggleSlotFavorite(logicalSlot.get());
        DataPersistenceManager.getInstance().cacheData(player.getUUID());
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

    public static long markFavoriteStateChanged(ServerPlayer player, String reason) {
        if (player == null) {
            return -1L;
        }

        FavoritesManager.getStateService().setPlayer(player.getUUID());
        DataPersistenceManager.getInstance().cacheData(player.getUUID());
        long revision = nextRevision(player);
        DebugLogger.debug(
            "Server marked favorite state changed: player={} reason={} revision={}",
            player.getName().getString(),
            reason,
            revision
        );
        return revision;
    }

    public static void resetRevision(ServerPlayer player) {
        revisionsByPlayer.put(player.getUUID(), 0L);
        bypassStateByPlayer.put(player.getUUID(), false);
        correctionSyncTicksByPlayer.remove(player.getUUID());
    }

    public static void setCorrectionSyncSender(Consumer<ServerPlayer> sender) {
        correctionSyncSender = sender == null ? player -> {} : sender;
    }

    public static void updateBypassState(ServerPlayer player, boolean held) {
        bypassStateByPlayer.put(player.getUUID(), held);
        DebugLogger.debug("Server updated bypass key state: player={} held={}", player.getName().getString(), held);
    }

    public static void clearPlayerState(Player player) {
        revisionsByPlayer.remove(player.getUUID());
        bypassStateByPlayer.remove(player.getUUID());
        instantSwapTransactionsByPlayer.remove(player.getUUID());
        correctionSyncTicksByPlayer.remove(player.getUUID());
    }

    public static void runWithInventoryGuardsBypassed(Player player, Runnable action) {
        if (player == null || action == null) {
            return;
        }

        beginInventoryGuardBypass(player.getUUID());
        try {
            action.run();
        } finally {
            endInventoryGuardBypass(player.getUUID());
        }
    }

    public static boolean prepareInstantSwap(
        ServerPlayer player,
        InstantSwapCompatService.Operation operation,
        int containerId,
        int targetMenuSlot,
        int hotbarIndex,
        int auxiliaryMenuSlot
    ) {
        if (player == null
            || !player.isAlive()
            || player.isSpectator()
            || operation == null
            || player.containerMenu.containerId != containerId
            || targetMenuSlot < 0
            || targetMenuSlot >= player.containerMenu.slots.size()
            || hotbarIndex < 0
            || hotbarIndex > 8) {
            return false;
        }

        AbstractContainerMenu menu = player.containerMenu;
        Slot targetSlot = menu.slots.get(targetMenuSlot);
        int targetInventoryIndex = resolvePlayerInventoryIndex(targetSlot, player);
        if (targetInventoryIndex >= 0
            && !InstantSwapCompatService.isHotbarMainInventoryPair(targetInventoryIndex, hotbarIndex)) {
            return false;
        }
        Slot hotbarSlot = findPlayerInventoryMenuSlot(menu, player, hotbarIndex);
        if (hotbarSlot == null || hotbarSlot.index == targetMenuSlot) {
            return false;
        }

        int auxiliaryInventoryIndex = -1;
        if (operation == InstantSwapCompatService.Operation.PICKUP_EXCHANGE
            && (!menu.getCarried().isEmpty()
                || hotbarIndex != player.getInventory().selected
                || (!targetSlot.hasItem() && !hotbarSlot.hasItem()))) {
            return false;
        }
        if (operation == InstantSwapCompatService.Operation.HOTBAR_STASH) {
            if (!menu.getCarried().isEmpty()
                || hotbarIndex != player.getInventory().selected
                || auxiliaryMenuSlot < 0
                || auxiliaryMenuSlot >= menu.slots.size()) {
                return false;
            }
            Slot emptySlot = menu.slots.get(auxiliaryMenuSlot);
            auxiliaryInventoryIndex = resolvePlayerInventoryIndex(emptySlot, player);
            if (auxiliaryInventoryIndex < 0
                || auxiliaryInventoryIndex > 8
                || auxiliaryInventoryIndex == hotbarIndex
                || emptySlot.hasItem()
                || !targetSlot.hasItem()
                || !hotbarSlot.hasItem()) {
                return false;
            }
        }

        var plan = InstantSwapCompatService.createPlan(
            operation,
            targetMenuSlot,
            targetInventoryIndex,
            hotbarSlot.index,
            hotbarIndex,
            auxiliaryMenuSlot,
            auxiliaryInventoryIndex,
            targetSlot.hasItem(),
            hotbarSlot.hasItem()
        ).orElse(null);
        if (plan == null) {
            return false;
        }
        int[] favoriteCycle = plan.favoriteCycle().stream().mapToInt(Integer::intValue).toArray();
        FavoritesManager.getStateService().setPlayer(player.getUUID());
        if (!InstantSwapCompatService.containsFavoriteSlot(
            FavoritesManager.getStateService().getFavoriteSlots(),
            favoriteCycle
        )) {
            return false;
        }
        instantSwapTransactionsByPlayer.put(
            player.getUUID(),
            new InstantSwapTransaction(menu, plan, favoriteCycle, player.level().getGameTime() + 5L)
        );
        return true;
    }

    public static boolean beginAuthorizedInstantSwapClick(
        AbstractContainerMenu menu,
        Player player,
        int slotId,
        int button,
        ClickType clickType
    ) {
        if (!(player instanceof ServerPlayer)) {
            return false;
        }
        InstantSwapTransaction transaction = instantSwapTransactionsByPlayer.get(player.getUUID());
        if (transaction == null
            || transaction.completed
            || transaction.activeClick
            || transaction.containerId != menu.containerId
            || transaction.expiresAt < player.level().getGameTime()
            || !transaction.nextClick().matches(slotId, button, clickType)) {
            if (transaction != null && transaction.expiresAt < player.level().getGameTime()) {
                instantSwapTransactionsByPlayer.remove(player.getUUID(), transaction);
            }
            return false;
        }

        transaction.activeClick = true;
        transaction.nextClick++;
        return true;
    }

    public static void finishAuthorizedInstantSwapClick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        InstantSwapTransaction transaction = instantSwapTransactionsByPlayer.get(player.getUUID());
        if (transaction == null || !transaction.activeClick) {
            return;
        }
        transaction.activeClick = false;
        if (transaction.nextClick == transaction.clicks.size()
            && transaction.contentFollowsCycle(serverPlayer.containerMenu)) {
            applyInstantSwapFavoriteCycle(serverPlayer, transaction.favoriteCycle);
            transaction.completed = true;
        }
    }

    public static boolean completeInstantSwap(ServerPlayer player) {
        InstantSwapTransaction transaction = instantSwapTransactionsByPlayer.remove(player.getUUID());
        return transaction != null && transaction.completed;
    }

    public static boolean executeInstantSwap(
        ServerPlayer player,
        InstantSwapCompatService.Operation operation,
        int containerId,
        int targetMenuSlot,
        int hotbarIndex,
        int auxiliaryMenuSlot
    ) {
        if (!prepareInstantSwap(player, operation, containerId, targetMenuSlot, hotbarIndex, auxiliaryMenuSlot)) {
            DebugLogger.debug(
                "Server rejected Su's Instant Swap execution: player={} operation={} target={} hotbar={}",
                player == null ? "null" : player.getName().getString(),
                operation,
                targetMenuSlot,
                hotbarIndex
            );
            return false;
        }
        InstantSwapTransaction transaction = instantSwapTransactionsByPlayer.remove(player.getUUID());
        if (transaction == null) {
            return false;
        }
        beginInventoryGuardBypass(player.getUUID());
        try {
            for (InstantSwapCompatService.PlannedClick click : transaction.clicks) {
                transaction.menu.clicked(click.slotId(), click.button(), click.clickType(), player);
            }
        } finally {
            endInventoryGuardBypass(player.getUUID());
        }
        if (!transaction.contentFollowsCycle(transaction.menu)) {
            DebugLogger.debug(
                "Server rejected Su's Instant Swap result: player={} operation={} target={} hotbar={}",
                player.getName().getString(),
                operation,
                targetMenuSlot,
                hotbarIndex
            );
            return false;
        }
        applyInstantSwapFavoriteCycle(player, transaction.favoriteCycle);
        // Direct server-side menu clicks do not guarantee an immediate client slot update.
        player.containerMenu.broadcastChanges();
        DebugLogger.debug(
            "Server completed Su's Instant Swap: player={} operation={} target={} hotbar={}",
            player.getName().getString(),
            operation,
            targetMenuSlot,
            hotbarIndex
        );
        return true;
    }

    public static boolean moveCreativeFavoritePairs(ServerPlayer player, int[] slotPairs) {
        if (player == null
            || !player.isAlive()
            || player.isSpectator()
            || !player.getAbilities().instabuild
            || !InstantSwapCompatService.areHotbarMainInventoryPairs(slotPairs)) {
            return false;
        }
        FavoritesManager.getStateService().setPlayer(player.getUUID());
        Set<Integer> currentFavorites = FavoritesManager.getStateService().getFavoriteSlots();
        Set<Integer> movedFavorites = InstantSwapCompatService.moveFavoritePairs(currentFavorites, slotPairs);
        if (currentFavorites.equals(movedFavorites)) {
            return false;
        }
        FavoritesManager favoritesManager = FavoritesManager.getInstance();
        for (int inventoryIndex : slotPairs) {
            favoritesManager.setSlotFavorite(inventoryIndex, movedFavorites.contains(inventoryIndex));
        }
        markFavoriteStateChanged(player, "creative_instant_swap");
        return true;
    }

    public static boolean executeCreativeSwapPairs(ServerPlayer player, int[] slotPairs) {
        if (player == null
            || !player.isAlive()
            || player.isSpectator()
            || !player.getAbilities().instabuild
            || !InstantSwapCompatService.areHotbarMainInventoryPairs(slotPairs)) {
            return false;
        }
        FavoritesManager.getStateService().setPlayer(player.getUUID());
        Set<Integer> currentFavorites = FavoritesManager.getStateService().getFavoriteSlots();
        Set<Integer> movedFavorites = InstantSwapCompatService.moveFavoritePairs(currentFavorites, slotPairs);
        runWithInventoryGuardsBypassed(player, () -> {
            for (int i = 0; i < slotPairs.length; i += 2) {
                int first = slotPairs[i];
                int second = slotPairs[i + 1];
                ItemStack firstStack = player.getInventory().getItem(first).copy();
                ItemStack secondStack = player.getInventory().getItem(second).copy();
                player.getInventory().setItem(first, secondStack);
                player.getInventory().setItem(second, firstStack);
            }
        });
        FavoritesManager favoritesManager = FavoritesManager.getInstance();
        for (int inventoryIndex : slotPairs) {
            favoritesManager.setSlotFavorite(inventoryIndex, movedFavorites.contains(inventoryIndex));
        }
        markFavoriteStateChanged(player, "creative_instant_swap");
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    public static void beginRespawnInventoryRestoreBypass(Player newPlayer, boolean keepEverything) {
        if (shouldBypassRespawnInventoryRestore(newPlayer, keepEverything)) {
            beginInventoryGuardBypass(newPlayer.getUUID());
            DebugLogger.debug(
                "Server began respawn inventory restore bypass: player={} keepEverything={} keepInventory={} preserveLockedSlotContents={}",
                newPlayer.getName().getString(),
                keepEverything,
                newPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY),
                ConfigManager.getInstance().getConfig().deathBehavior.preserveLockedSlotContents
            );
        }
    }

    public static void endRespawnInventoryRestoreBypass(Player newPlayer, boolean keepEverything) {
        if (shouldBypassRespawnInventoryRestore(newPlayer, keepEverything)) {
            endInventoryGuardBypass(newPlayer.getUUID());
            DebugLogger.debug(
                "Server ended respawn inventory restore bypass: player={} keepEverything={} keepInventory={} preserveLockedSlotContents={}",
                newPlayer.getName().getString(),
                keepEverything,
                newPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY),
                ConfigManager.getInstance().getConfig().deathBehavior.preserveLockedSlotContents
            );
        }
    }

    public static boolean shouldBypassRespawnInventoryRestore(Player player, boolean keepEverything) {
        return player != null
            && shouldBypassRespawnInventoryRestore(
                player.level().isClientSide(),
                keepEverything,
                player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY),
                ConfigManager.getInstance().getConfig().deathBehavior.preserveLockedSlotContents
            );
    }

    public static void beginDeathDropPreservation(Player player) {
        if (shouldPreserveLockedSlotContentsAfterDeath(player)) {
            beginDeathDropPreservation(player.getUUID());
            DebugLogger.debug(
                "Server began death drop preservation: player={}",
                player.getName().getString()
            );
        }
    }

    public static void endDeathDropPreservation(Player player) {
        if (shouldPreserveLockedSlotContentsAfterDeath(player)) {
            endDeathDropPreservation(player.getUUID());
            DebugLogger.debug(
                "Server ended death drop preservation: player={}",
                player.getName().getString()
            );
        }
    }

    public static boolean shouldPreserveInventorySlotOnDeath(Inventory inventory, int inventoryIndex) {
        if (!isServerPlayerInventoryIndex(inventory, inventoryIndex)
            || inventory.getItem(inventoryIndex).isEmpty()
            || !isDeathDropPreservationActive(inventory.player)) {
            return false;
        }

        FavoritesManager.getInstance().setPlayer(inventory.player.getUUID());
        return SlotMappingService.fromPlayerInventoryIndex(inventoryIndex)
            .map(FavoritesManager.getInstance()::isSlotFavorite)
            .orElse(false);
    }

    public static void restorePreservedLockedSlotsAfterDeath(Player originalPlayer, Player newPlayer) {
        if (originalPlayer == null || newPlayer == null || newPlayer.level().isClientSide()) {
            return;
        }

        FavoritesManager.getStateService().setPlayer(newPlayer.getUUID());
        Set<Integer> favoriteSlots = FavoritesManager.getStateService().getFavoriteSlots();
        runWithInventoryGuardsBypassed(newPlayer, () -> {
            Inventory originalInventory = originalPlayer.getInventory();
            Inventory newInventory = newPlayer.getInventory();
            for (int inventoryIndex : favoriteSlots) {
                if (!SlotMappingService.isPlayerInventoryIndex(inventoryIndex)) {
                    continue;
                }
                ItemStack originalStack = originalInventory.getItem(inventoryIndex);
                if (!originalStack.isEmpty()) {
                    newInventory.setItem(inventoryIndex, originalStack.copy());
                }
            }
        });
        DebugLogger.debug(
            "Server restored preserved locked death slots: player={} slots={}",
            newPlayer.getName().getString(),
            favoriteSlots
        );
    }

    public static boolean shouldPreserveLockedSlotContentsAfterDeath(Player player) {
        return player != null
            && !player.level().isClientSide()
            && ConfigManager.getInstance().getConfig().deathBehavior.preserveLockedSlotContents;
    }

    public static boolean shouldCancelMenuClick(AbstractContainerMenu menu, Player player, int slotId, int button, ClickType clickType) {
        if (player == null || slotId < 0 || slotId >= menu.slots.size()) {
            return false;
        }
        if (isInventoryGuardBypassed(player)) {
            return false;
        }
        Slot slot = menu.slots.get(slotId);
        int inventoryIndex = resolvePlayerInventoryIndex(slot, player);
        if (clickType == ClickType.SWAP && shouldCancelSwap(player, inventoryIndex, button, slot.hasItem())) {
            DebugLogger.debug(
                "Server canceled menu swap: player={} inventoryIndex={} slotId={} button={}",
                player.getName().getString(),
                inventoryIndex,
                slotId,
                button
            );
            return rejectAndRequestCorrectionSync(player);
        }
        if (isSophisticatedStorageNonPlayerSlot(menu, slotId) || inventoryIndex < 0) {
            return false;
        }

        FavoritesManager.getInstance().setPlayer(player.getUUID());
        if (clickType == ClickType.QUICK_MOVE && shouldCancelQuickMoveTarget(player, slot.getItem())) {
            DebugLogger.debug(
                "Server canceled quick move into locked target: player={} sourceInventoryIndex={} slotId={}",
                player.getName().getString(),
                inventoryIndex,
                slotId
            );
            return rejectAndRequestCorrectionSync(player);
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
            return rejectAndRequestCorrectionSync(player);
        }
        return false;
    }

    public static boolean shouldCancelOffhandSwap(Player player) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        boolean denied = shouldCancelSwap(
            player,
            player.getInventory().selected,
            40,
            !player.getInventory().getItem(player.getInventory().selected).isEmpty()
        );
        return denied && rejectAndRequestCorrectionSync(player);
    }

    public static boolean shouldCancelCreativeSlotSet(ServerPlayer player, int menuSlot, ItemStack newStack) {
        if (player == null
            || !player.getAbilities().instabuild
            || menuSlot < 0
            || menuSlot >= player.inventoryMenu.slots.size()) {
            return false;
        }
        Slot slot = player.inventoryMenu.getSlot(menuSlot);
        ItemStack currentStack = slot.getItem();
        if (ItemStack.matches(currentStack, newStack) && currentStack.getCount() == newStack.getCount()) {
            return false;
        }
        boolean denied = !currentStack.isEmpty() && shouldPreventSlotPickup(slot, player)
            || newStack != null && !newStack.isEmpty() && shouldPreventSlotPlace(slot, player, newStack);
        return denied && rejectAndRequestCorrectionSync(player);
    }

    public static boolean shouldPreventSlotPickup(Slot slot, Player player) {
        if (!isServerPlayerInventorySlot(slot, player)) {
            return false;
        }
        if (isInventoryGuardBypassed(player)) {
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
        if (isInventoryGuardBypassed(player)) {
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
        if (isInventoryGuardBypassed(player)) {
            return false;
        }
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

        Player player = inventory.player;
        if (isInventoryGuardBypassed(player)) {
            return false;
        }

        ItemStack currentStack = inventory.getItem(inventoryIndex);
        if (ItemStack.matches(currentStack, newStack) && currentStack.getCount() == newStack.getCount()) {
            return false;
        }

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

    public static boolean shouldRerouteInventorySet(Inventory inventory, int inventoryIndex, ItemStack incomingStack) {
        if (!isServerPlayerInventoryIndex(inventory, inventoryIndex)) {
            return false;
        }

        Player player = inventory.player;
        if (isInventoryGuardBypassed(player)) {
            return false;
        }

        ItemStack currentStack = inventory.getItem(inventoryIndex);
        if (!currentStack.isEmpty() || incomingStack == null || incomingStack.isEmpty()) {
            return false;
        }

        FavoritesManager.getInstance().setPlayer(player.getUUID());
        InteractionDecision decision = InteractionGuardService.getInstance().evaluateIncomingItem(
            inventoryIndex,
            InteractionType.QUICK_MOVE,
            isBypassKeyHeld(player),
            true
        );
        return decision.denied()
            && tryRerouteDeniedIncomingStack(inventory, inventoryIndex, currentStack, incomingStack, decision);
    }

    public static boolean shouldRerouteMainHandSet(Player player, InteractionHand hand, ItemStack incomingStack) {
        if (player == null || hand != InteractionHand.MAIN_HAND) {
            return false;
        }
        Inventory inventory = player.getInventory();
        if (!isServerPlayerInventoryIndex(inventory, inventory.selected)) {
            return false;
        }
        if (isInventoryGuardBypassed(player)) {
            return false;
        }

        ItemStack currentStack = inventory.getItem(inventory.selected);
        if (!currentStack.isEmpty() || incomingStack == null || incomingStack.isEmpty()) {
            return false;
        }

        FavoritesManager.getInstance().setPlayer(player.getUUID());
        InteractionDecision decision = InteractionGuardService.getInstance().evaluateIncomingItem(
            inventory.selected,
            InteractionType.QUICK_MOVE,
            isBypassKeyHeld(player),
            true
        );
        return decision.denied()
            && tryRerouteDeniedIncomingStack(inventory, inventory.selected, currentStack, incomingStack, decision);
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

    public static int resolveFreeSlotForIncomingItem(Inventory inventory, int firstFreeSlot) {
        if (!isServerPlayerMainInventoryIndex(inventory, firstFreeSlot)
            || !shouldSkipLockedEmptySlotForIncomingItem(inventory, firstFreeSlot)) {
            return firstFreeSlot;
        }

        int resolvedSlot = resolveFreeSlotForIncomingItem(
            firstFreeSlot,
            Inventory.INVENTORY_SIZE,
            slot -> inventory.getItem(slot).isEmpty(),
            slot -> shouldSkipLockedEmptySlotForIncomingItem(inventory, slot)
        );
        DebugLogger.debug(
            "Server skipped locked empty slot for incoming item: player={} firstFreeSlot={} resolvedSlot={}",
            inventory.player.getName().getString(),
            firstFreeSlot,
            resolvedSlot
        );
        return resolvedSlot;
    }

    public static int resolveSlotWithRemainingSpace(Inventory inventory, ItemStack incomingStack, int firstSlot) {
        if (!shouldSkipLockedOccupiedSlotForIncomingItem(inventory, firstSlot, incomingStack)) {
            return firstSlot;
        }

        return resolveSlotWithRemainingSpace(
            firstSlot,
            inventory.selected,
            slot -> hasRemainingSpaceForItem(inventory, slot, incomingStack),
            slot -> shouldSkipLockedOccupiedSlotForIncomingItem(inventory, slot, incomingStack)
        );
    }

    public static boolean shouldPreventInPlaceSlotMerge(Slot slot, ItemStack incomingStack) {
        if (slot == null || slot.getItem().isEmpty() || incomingStack == null || incomingStack.isEmpty()) {
            return false;
        }
        if (!(slot.container instanceof Inventory inventory)) {
            return false;
        }
        return shouldPreventInventoryReceive(inventory, slot.getContainerSlot(), incomingStack);
    }

    private static boolean tryRerouteDeniedIncomingStack(Inventory inventory, int inventoryIndex, ItemStack currentStack, ItemStack incomingStack, InteractionDecision decision) {
        if (inventory == null
            || inventory.player == null
            || currentStack == null
            || !currentStack.isEmpty()
            || incomingStack == null
            || incomingStack.isEmpty()) {
            return false;
        }

        Player player = inventory.player;
        int fallbackSlot = LockedEmptySlotFallback.findEmptyUnlockedFallbackSlot(
            slot -> inventory.getItem(slot).isEmpty(),
            slot -> isIncomingTargetLocked(player, slot)
        );
        ItemStack stackToMove = incomingStack.copy();
        if (fallbackSlot >= 0) {
            runWithInventoryGuardsBypassed(player, () -> inventory.setItem(fallbackSlot, stackToMove));
            incomingStack.setCount(0);
            DebugLogger.debug(
                "Server rerouted incoming stack away from locked empty slot: player={} blockedSlot={} fallbackSlot={} reason={}",
                player.getName().getString(),
                inventoryIndex,
                fallbackSlot,
                decision.reason()
            );
            return true;
        }

        if (player.drop(stackToMove, false) != null) {
            incomingStack.setCount(0);
            DebugLogger.debug(
                "Server dropped incoming stack away from locked empty slot: player={} blockedSlot={} reason={}",
                player.getName().getString(),
                inventoryIndex,
                decision.reason()
            );
            return true;
        }
        return false;
    }

    static int resolveFreeSlotForIncomingItem(
        int firstFreeSlot,
        int inventorySize,
        IntPredicate isEmptySlot,
        IntPredicate shouldSkipSlot
    ) {
        if (firstFreeSlot < 0 || firstFreeSlot >= inventorySize || !isEmptySlot.test(firstFreeSlot) || !shouldSkipSlot.test(firstFreeSlot)) {
            return firstFreeSlot;
        }
        for (int slot = firstFreeSlot + 1; slot < inventorySize; slot++) {
            if (isEmptySlot.test(slot) && !shouldSkipSlot.test(slot)) {
                return slot;
            }
        }
        return -1;
    }

    static int resolveSlotWithRemainingSpace(
        int firstSlot,
        int selectedSlot,
        IntPredicate hasRemainingSpace,
        IntPredicate shouldSkipSlot
    ) {
        if (firstSlot < 0 || !shouldSkipSlot.test(firstSlot)) {
            return firstSlot;
        }
        if (hasRemainingSpace.test(selectedSlot) && !shouldSkipSlot.test(selectedSlot)) {
            return selectedSlot;
        }
        if (hasRemainingSpace.test(40) && !shouldSkipSlot.test(40)) {
            return 40;
        }
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (hasRemainingSpace.test(slot) && !shouldSkipSlot.test(slot)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean hasRemainingSpaceForItem(Inventory inventory, int inventoryIndex, ItemStack incomingStack) {
        ItemStack currentStack = inventory.getItem(inventoryIndex);
        return !currentStack.isEmpty()
            && ItemStack.isSameItemSameComponents(currentStack, incomingStack)
            && currentStack.isStackable()
            && currentStack.getCount() < Math.min(currentStack.getMaxStackSize(), inventory.getMaxStackSize());
    }

    private static boolean shouldSkipLockedOccupiedSlotForIncomingItem(
        Inventory inventory,
        int inventoryIndex,
        ItemStack incomingStack
    ) {
        return isServerPlayerInventoryIndex(inventory, inventoryIndex)
            && !inventory.getItem(inventoryIndex).isEmpty()
            && shouldPreventInventoryReceive(inventory, inventoryIndex, incomingStack);
    }

    private static boolean shouldSkipLockedEmptySlotForIncomingItem(Inventory inventory, int inventoryIndex) {
        if (!isServerPlayerMainInventoryIndex(inventory, inventoryIndex)
            || !inventory.getItem(inventoryIndex).isEmpty()
            || isInventoryGuardBypassed(inventory.player)) {
            return false;
        }

        var config = ConfigManager.getInstance().getConfig();
        if (!config.general.lockEmptySlots
            || config.general.autoUnlockEmptySlots
            || config.general.allowItemsIntoLockedEmptySlots) {
            return false;
        }
        if (isBypassKeyHeld(inventory.player) && config.lockBehavior.allowBypassWithKey) {
            return false;
        }

        FavoritesManager.getInstance().setPlayer(inventory.player.getUUID());
        return SlotMappingService.fromPlayerInventoryIndex(inventoryIndex)
            .map(FavoritesManager.getInstance()::isSlotFavorite)
            .orElse(false);
    }

    private static boolean isIncomingTargetLocked(Player player, int inventoryIndex) {
        return evaluateIncomingItem(player, inventoryIndex, InteractionType.QUICK_MOVE, true).denied();
    }

    public static boolean shouldProtectInventorySlotForExternalMove(Inventory inventory, int inventoryIndex) {
        if (!isServerPlayerInventoryIndex(inventory, inventoryIndex)) {
            return false;
        }

        Player player = inventory.player;
        if (isInventoryGuardBypassed(player)) {
            return false;
        }
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

    private static boolean isServerPlayerMainInventoryIndex(Inventory inventory, int inventoryIndex) {
        return isServerPlayerInventoryIndex(inventory, inventoryIndex)
            && inventoryIndex >= 0
            && inventoryIndex < Inventory.INVENTORY_SIZE;
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

    private static Slot findPlayerInventoryMenuSlot(AbstractContainerMenu menu, Player player, int inventoryIndex) {
        for (Slot slot : menu.slots) {
            if (resolvePlayerInventoryIndex(slot, player) == inventoryIndex) {
                return slot;
            }
        }
        return null;
    }

    private static void applyInstantSwapFavoriteCycle(ServerPlayer player, int[] favoriteCycle) {
        FavoritesManager.getStateService().setPlayer(player.getUUID());
        Set<Integer> currentFavorites = FavoritesManager.getStateService().getFavoriteSlots();
        Set<Integer> movedFavorites = InstantSwapCompatService.moveFavoriteSlots(currentFavorites, favoriteCycle);
        if (currentFavorites.equals(movedFavorites)) {
            return;
        }
        FavoritesManager favoritesManager = FavoritesManager.getInstance();
        for (int inventoryIndex : favoriteCycle) {
            if (inventoryIndex >= 0) {
                favoritesManager.setSlotFavorite(inventoryIndex, movedFavorites.contains(inventoryIndex));
            }
        }
        markFavoriteStateChanged(player, "instant_swap");
    }

    private static boolean isSophisticatedStorageNonPlayerSlot(AbstractContainerMenu menu, int slotId) {
        if (menu == null || !isInstanceOf(menu, "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase")) {
            return false;
        }

        Integer storageSlotsSize = invokeIntNoArg(menu, "getNumberOfStorageInventorySlots");
        if (storageSlotsSize == null) {
            return false;
        }

        int firstPlayerSlot = storageSlotsSize;
        int afterPlayerSlots = firstPlayerSlot + 36;
        return slotId < firstPlayerSlot || slotId >= afterPlayerSlots;
    }

    private static boolean isInstanceOf(Object target, String className) {
        Class<?> type = target.getClass();
        while (type != null) {
            if (type.getName().equals(className)) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static Integer invokeIntNoArg(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Integer integer ? integer : null;
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

    static boolean shouldSendCorrectionSync(UUID playerId, long gameTime) {
        return !Long.valueOf(gameTime).equals(correctionSyncTicksByPlayer.put(playerId, gameTime));
    }

    private static boolean rejectAndRequestCorrectionSync(Player player) {
        if (player instanceof ServerPlayer serverPlayer
            && shouldSendCorrectionSync(player.getUUID(), player.level().getGameTime())) {
            correctionSyncSender.accept(serverPlayer);
            DebugLogger.debug(
                "Server sent corrective favorite full sync after rejected inventory operation: player={}",
                player.getName().getString()
            );
        }
        return true;
    }

    public static boolean isInventoryGuardBypassed(UUID playerId) {
        return ScopedPlayerOperationService.isInventoryGuardBypassed(playerId);
    }

    public static boolean isInventoryGuardBypassed(Player player) {
        if (player == null) {
            return false;
        }
        InstantSwapTransaction transaction = instantSwapTransactionsByPlayer.get(player.getUUID());
        return isInventoryGuardBypassed(player.getUUID())
            || transaction != null
            && transaction.activeClick
            && transaction.expiresAt >= player.level().getGameTime();
    }

    static boolean shouldBypassRespawnInventoryRestore(
        boolean clientSide,
        boolean keepEverything,
        boolean keepInventory,
        boolean preserveLockedSlotContents
    ) {
        return !clientSide && (keepEverything || keepInventory || preserveLockedSlotContents);
    }

    public static void beginInventoryGuardBypass(UUID playerId) {
        ScopedPlayerOperationService.beginInventoryGuardBypass(playerId);
    }

    public static void endInventoryGuardBypass(UUID playerId) {
        ScopedPlayerOperationService.endInventoryGuardBypass(playerId);
    }

    static boolean isDeathDropPreservationActive(UUID playerId) {
        return ScopedPlayerOperationService.isDeathDropPreservationActive(playerId);
    }

    private static boolean isDeathDropPreservationActive(Player player) {
        return player != null && isDeathDropPreservationActive(player.getUUID());
    }

    static void beginDeathDropPreservation(UUID playerId) {
        ScopedPlayerOperationService.beginDeathDropPreservation(playerId);
    }

    static void endDeathDropPreservation(UUID playerId) {
        ScopedPlayerOperationService.endDeathDropPreservation(playerId);
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
        return InteractionGuardService.getInstance().shouldCancelSwap(
            clickedInventoryIndex,
            clickedHasItem,
            partnerInventoryIndex,
            partnerHasItem,
            isBypassKeyHeld(player)
        );
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

    private static final class InstantSwapTransaction {
        private final AbstractContainerMenu menu;
        private final int containerId;
        private final List<InstantSwapCompatService.PlannedClick> clicks;
        private final int[] favoriteCycle;
        private final int[] menuSlotCycle;
        private final List<ItemStack> beforeStacks;
        private final long expiresAt;
        private int nextClick;
        private boolean activeClick;
        private boolean completed;

        private InstantSwapTransaction(
            AbstractContainerMenu menu,
            InstantSwapCompatService.OperationPlan plan,
            int[] favoriteCycle,
            long expiresAt
        ) {
            this.menu = menu;
            this.containerId = menu.containerId;
            this.clicks = plan.clicks();
            this.favoriteCycle = favoriteCycle.clone();
            this.menuSlotCycle = plan.menuSlotCycle().stream().mapToInt(Integer::intValue).toArray();
            this.beforeStacks = stacksAt(menu, this.menuSlotCycle);
            this.expiresAt = expiresAt;
        }

        private InstantSwapCompatService.PlannedClick nextClick() {
            return clicks.get(nextClick);
        }

        private boolean contentFollowsCycle(AbstractContainerMenu menu) {
            return menu.containerId == containerId
                && InstantSwapCompatService.contentFollowsCycle(
                    beforeStacks,
                    stacksAt(menu, menuSlotCycle),
                    ItemStack::matches
                );
        }

        private static List<ItemStack> stacksAt(AbstractContainerMenu menu, int[] menuSlots) {
            List<ItemStack> stacks = new ArrayList<>(menuSlots.length);
            for (int menuSlot : menuSlots) {
                if (menuSlot < 0 || menuSlot >= menu.slots.size()) {
                    return List.of();
                }
                stacks.add(menu.slots.get(menuSlot).getItem().copy());
            }
            return stacks;
        }
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
