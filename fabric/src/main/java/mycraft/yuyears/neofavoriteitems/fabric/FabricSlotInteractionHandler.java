package mycraft.yuyears.neofavoriteitems.fabric;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.application.InteractionGuardService;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public final class FabricSlotInteractionHandler {
    private FabricSlotInteractionHandler() {}

    public static boolean handleLockOperationToggle(Slot slot) {
        if (slot == null || !FabricSlotResolver.isPlayerInventorySlot(slot)) {
            return false;
        }

        int inventoryIndex = FabricSlotResolver.getPlayerInventoryIndex(slot);
        boolean hasItem = FabricSlotResolver.hasItem(slot);
        var logicalSlot = SlotMappingService.fromPlayerInventoryIndex(inventoryIndex);
        if (logicalSlot.isEmpty() || !hasItem) {
            DebugLogger.debug(
                "Fabric slot toggle ignored: inventoryIndex={} hasItem={} reason={}",
                inventoryIndex,
                hasItem,
                logicalSlot.isEmpty() ? "unmapped_slot" : "empty_slot"
            );
            return true;
        }

        if (FabricFavoriteNetworking.trySendToggle(inventoryIndex)) {
            return true;
        }

        FavoritesManager.getInstance().toggleSlotFavorite(logicalSlot.get());
        NeoFavoriteItemsFabricClient.showSlotToggleMessage(logicalSlot.get());
        DebugLogger.debug(
            "Fabric slot lock toggled: logicalSlot={} inventoryIndex={} nowLocked={}",
            logicalSlot.get().value(),
            inventoryIndex,
            FavoritesManager.getInstance().isSlotFavorite(logicalSlot.get())
        );
        return true;
    }

    public static boolean shouldCancelGuardedInteraction(Slot slot, ClickType clickType) {
        if (slot == null || !FabricSlotResolver.isPlayerInventorySlot(slot)) {
            return false;
        }

        int inventoryIndex = FabricSlotResolver.getPlayerInventoryIndex(slot);
        var decision = InteractionGuardService.getInstance().evaluate(
            inventoryIndex,
            toInteractionType(clickType),
            NeoFavoriteItemsFabricClient.isBypassKeyHeld(),
            FabricSlotResolver.hasItem(slot)
        );
        if (decision.denied()) {
            DebugLogger.debug("Fabric slot interaction canceled: inventoryIndex={} clickType={}", inventoryIndex, clickType);
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
