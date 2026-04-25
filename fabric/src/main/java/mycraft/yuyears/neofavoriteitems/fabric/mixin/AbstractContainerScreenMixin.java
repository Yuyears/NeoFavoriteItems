package mycraft.yuyears.neofavoriteitems.fabric.mixin;

import mycraft.yuyears.neofavoriteitems.fabric.FabricSlotInteractionHandler;
import mycraft.yuyears.neofavoriteitems.fabric.FabricSlotResolver;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.fabric.NeoFavoriteItemsFabricClient;
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
    private void neoFavoriteItems$guardFavoriteSlot(Slot slot, int i, int j, ClickType clickType, CallbackInfo ci) {
        DebugLogger.debug(
            "Fabric slotClicked: slotNull={} slotId={} button={} clickType={} lockOperation={} bypass={}",
            slot == null,
                i,
                j,
            clickType,
            NeoFavoriteItemsFabricClient.isLockOperationKeyHeld(),
            NeoFavoriteItemsFabricClient.isBypassKeyHeld()
        );
        if (slot == null || !FabricSlotResolver.isPlayerInventorySlot(slot)) {
            DebugLogger.debug("Fabric slotClicked ignored: reason={} slotId={}", slot == null ? "null_slot" : "not_player_inventory", i);
            return;
        }

        if (NeoFavoriteItemsFabricClient.isLockOperationKeyHeld()
            && j == 0
            && clickType == ClickType.PICKUP) {
            if (FabricSlotInteractionHandler.handleLockOperationToggle(slot)) {
                ci.cancel();
            }
            return;
        }

        if (NeoFavoriteItemsFabricClient.isLockOperationKeyHeld()
            && j == 0
            && clickType == ClickType.QUICK_MOVE) {
            ci.cancel();
            return;
        }

        if (FabricSlotInteractionHandler.shouldCancelGuardedInteraction(slot, clickType)) {
            ci.cancel();
        }
    }
}
