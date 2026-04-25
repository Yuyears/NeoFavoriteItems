package mycraft.yuyears.neofavoriteitems.neoforge;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.application.InteractionGuardService;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public final class NeoForgeSlotInteractionHandler {
    private NeoForgeSlotInteractionHandler() {}

    public static boolean handleLockOperationToggle(Slot slot) {
        if (slot == null || !NeoForgeSlotResolver.isPlayerInventorySlot(slot)) {
            return false;
        }

        int inventoryIndex = NeoForgeSlotResolver.getPlayerInventoryIndex(slot);
        boolean hasItem = NeoForgeSlotResolver.hasItem(slot);
        var logicalSlot = SlotMappingService.fromPlayerInventoryIndex(inventoryIndex);
        if (logicalSlot.isEmpty()) {
            DebugLogger.debug(
                "NeoForge slot toggle ignored: inventoryIndex={} reason={}",
                inventoryIndex,
                "unmapped_slot"
            );
            return true;
        }
        if (!hasItem && !ConfigManager.getInstance().getConfig().general.lockEmptySlots) {
            DebugLogger.debug(
                "NeoForge slot toggle ignored: inventoryIndex={} hasItem=false reason=empty_slot_disabled",
                inventoryIndex
            );
            return true;
        }

        if (NeoForgeFavoriteNetworking.trySendToggle(inventoryIndex)) {
            return true;
        }

        FavoritesManager.getInstance().toggleSlotFavorite(logicalSlot.get());
        NeoFavoriteItemsNeoForge.showSlotToggleMessage(logicalSlot.get());
        DebugLogger.debug(
            "NeoForge slot lock toggled: logicalSlot={} inventoryIndex={} nowLocked={}",
            logicalSlot.get().value(),
            inventoryIndex,
            FavoritesManager.getInstance().isSlotFavorite(logicalSlot.get())
        );
        return true;
    }

    public static boolean shouldCancelGuardedInteraction(Slot slot, ClickType clickType) {
        return shouldCancelGuardedInteraction(slot, clickType, -1);
    }

    public static boolean shouldCancelGuardedInteraction(Slot slot, ClickType clickType, int button) {
        if (slot == null || !NeoForgeSlotResolver.isPlayerInventorySlot(slot)) {
            return false;
        }

        int inventoryIndex = NeoForgeSlotResolver.getPlayerInventoryIndex(slot);
        if (clickType == ClickType.SWAP && shouldCancelSwap(inventoryIndex, button, NeoForgeSlotResolver.hasItem(slot))) {
            DebugLogger.debug("NeoForge slot swap canceled: inventoryIndex={} button={}", inventoryIndex, button);
            return true;
        }

        var decision = InteractionGuardService.getInstance().evaluate(
            inventoryIndex,
            toInteractionType(clickType),
            NeoFavoriteItemsNeoForge.isBypassKeyHeld(),
            NeoForgeSlotResolver.hasItem(slot)
        );
        if (decision.denied()) {
            DebugLogger.debug("NeoForge slot interaction canceled: inventoryIndex={} clickType={}", inventoryIndex, clickType);
            return true;
        }
        return false;
    }

    public static boolean shouldCancelOffhandSwap(int selectedHotbarSlot) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        boolean selectedHasItem = !player.getInventory().getItem(selectedHotbarSlot).isEmpty();
        return shouldCancelSwap(selectedHotbarSlot, 40, selectedHasItem);
    }

    private static boolean shouldCancelSwap(int inventoryIndex, int button, boolean clickedHasItem) {
        int partnerInventoryIndex = swapButtonToInventoryIndex(button);
        var player = Minecraft.getInstance().player;
        if (player == null || partnerInventoryIndex < 0 || partnerInventoryIndex == inventoryIndex) {
            return false;
        }

        boolean partnerHasItem = !player.getInventory().getItem(partnerInventoryIndex).isEmpty();
        boolean bypass = NeoFavoriteItemsNeoForge.isBypassKeyHeld();
        InteractionGuardService guard = InteractionGuardService.getInstance();
        return guard.evaluate(inventoryIndex, InteractionType.SWAP, bypass, clickedHasItem).denied()
            || guard.evaluateIncomingItem(inventoryIndex, InteractionType.SWAP, bypass, partnerHasItem).denied()
            || guard.evaluate(partnerInventoryIndex, InteractionType.SWAP, bypass, partnerHasItem).denied()
            || guard.evaluateIncomingItem(partnerInventoryIndex, InteractionType.SWAP, bypass, clickedHasItem).denied();
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

    public static InteractionType toInteractionType(ClickType clickType) {
        return switch (clickType) {
            case PICKUP, PICKUP_ALL -> InteractionType.CLICK;
            case QUICK_MOVE -> InteractionType.QUICK_MOVE;
            case SWAP -> InteractionType.SWAP;
            case THROW -> InteractionType.DROP;
            case QUICK_CRAFT -> InteractionType.DRAG;
            case CLONE -> InteractionType.UNKNOWN;
        };
    }
}
