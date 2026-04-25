package mycraft.yuyears.neofavoriteitems.neoforge.mixin;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoFavoriteItemsNeoForge;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeSlotInteractionHandler;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeSlotResolver;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardFavoriteSlot(Slot slot, int slotId, int button, ClickType clickType, CallbackInfo ci) {
        DebugLogger.debug(
            "NeoForge slotClicked: slotNull={} slotId={} button={} clickType={} lockOperation={} bypass={}",
            slot == null,
            slotId,
            button,
            clickType,
            NeoFavoriteItemsNeoForge.isLockOperationKeyHeld(),
            NeoFavoriteItemsNeoForge.isBypassKeyHeld()
        );
        if (slot == null || !NeoForgeSlotResolver.isPlayerInventorySlot(slot)) {
            DebugLogger.debug("NeoForge slotClicked ignored: reason={} slotId={}", slot == null ? "null_slot" : "not_player_inventory", slotId);
            return;
        }

        if (NeoFavoriteItemsNeoForge.isLockOperationKeyHeld()
            && button == 0
            && clickType == ClickType.PICKUP) {
            if (NeoForgeSlotInteractionHandler.handleLockOperationToggle(slot)) {
                ci.cancel();
            }
            return;
        }

        if (NeoFavoriteItemsNeoForge.isLockOperationKeyHeld()
            && button == 0
            && clickType == ClickType.QUICK_MOVE) {
            ci.cancel();
            return;
        }

        if (NeoForgeSlotInteractionHandler.shouldCancelGuardedInteraction(slot, clickType)) {
            ci.cancel();
        }
    }
}
