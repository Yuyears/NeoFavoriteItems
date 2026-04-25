package mycraft.yuyears.neofavoriteitems.forge.mixin;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.forge.ForgeSlotInteractionHandler;
import mycraft.yuyears.neofavoriteitems.forge.ForgeSlotResolver;
import mycraft.yuyears.neofavoriteitems.forge.NeoFavoriteItemsForge;
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
            "Forge slotClicked: slotNull={} slotId={} button={} clickType={} lockOperation={} bypass={}",
            slot == null,
            slotId,
            button,
            clickType,
            NeoFavoriteItemsForge.isLockOperationKeyHeld(),
            NeoFavoriteItemsForge.isBypassKeyHeld()
        );
        if (slot == null || !ForgeSlotResolver.isPlayerInventorySlot(slot)) {
            DebugLogger.debug("Forge slotClicked ignored: reason={} slotId={}", slot == null ? "null_slot" : "not_player_inventory", slotId);
            return;
        }

        if (NeoFavoriteItemsForge.isLockOperationKeyHeld()
            && button == 0
            && clickType == ClickType.PICKUP) {
            if (ForgeSlotInteractionHandler.handleLockOperationToggle(slot)) {
                ci.cancel();
            }
            return;
        }

        if (NeoFavoriteItemsForge.isLockOperationKeyHeld()
            && button == 0
            && clickType == ClickType.QUICK_MOVE) {
            ci.cancel();
            return;
        }

        if (ForgeSlotInteractionHandler.shouldCancelGuardedInteraction(slot, clickType, button)) {
            ci.cancel();
        }
    }
}
