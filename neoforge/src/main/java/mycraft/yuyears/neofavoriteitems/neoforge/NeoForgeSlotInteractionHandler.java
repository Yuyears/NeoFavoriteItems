package mycraft.yuyears.neofavoriteitems.neoforge;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.application.InteractionGuardService;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
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
        if (slot == null || !NeoForgeSlotResolver.isPlayerInventorySlot(slot)) {
            return false;
        }

        int inventoryIndex = NeoForgeSlotResolver.getPlayerInventoryIndex(slot);
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
