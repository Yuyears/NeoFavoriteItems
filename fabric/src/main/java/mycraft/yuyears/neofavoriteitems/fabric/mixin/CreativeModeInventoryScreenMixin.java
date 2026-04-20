package mycraft.yuyears.neofavoriteitems.fabric.mixin;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.fabric.FabricSlotInteractionHandler;
import mycraft.yuyears.neofavoriteitems.fabric.FabricSlotResolver;
import mycraft.yuyears.neofavoriteitems.fabric.NeoFavoriteItemsFabricClient;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardCreativeFavoriteSlot(Slot slot, int slotId, int button, ClickType clickType, CallbackInfo ci) {
        DebugLogger.debug(
            "Fabric creative slotClicked: slotNull={} slotId={} button={} clickType={} playerInventory={} lockOperation={} bypass={}",
            slot == null,
            slotId,
            button,
            clickType,
            slot != null && FabricSlotResolver.isPlayerInventorySlot(slot),
            NeoFavoriteItemsFabricClient.isLockOperationKeyHeld(),
            NeoFavoriteItemsFabricClient.isBypassKeyHeld()
        );

        if (slot == null || !FabricSlotResolver.isPlayerInventorySlot(slot)) {
            return;
        }

        if (NeoFavoriteItemsFabricClient.isLockOperationKeyHeld() && button == 0 && clickType == ClickType.PICKUP) {
            if (FabricSlotInteractionHandler.handleLockOperationToggle(slot)) {
                ci.cancel();
            }
            return;
        }

        if (FabricSlotInteractionHandler.shouldCancelGuardedInteraction(slot, clickType)) {
            ci.cancel();
        }
    }
}
