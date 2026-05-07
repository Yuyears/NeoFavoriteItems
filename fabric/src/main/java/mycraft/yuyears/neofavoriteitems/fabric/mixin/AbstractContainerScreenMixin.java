package mycraft.yuyears.neofavoriteitems.fabric.mixin;

import mycraft.yuyears.neofavoriteitems.fabric.FabricSlotInteractionHandler;
import mycraft.yuyears.neofavoriteitems.fabric.FabricSlotResolver;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.fabric.FabricMouseTweaksBridge;
import mycraft.yuyears.neofavoriteitems.fabric.NeoFavoriteItemsFabricClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Unique
    private static final int neoFavoriteItems$LEFT_BUTTON = 0;
    @Shadow
    private Slot findSlot(double mouseX, double mouseY) {
        return null;
    }

    @Unique
    private boolean neoFavoriteItems$lockOperationActive;
    @Unique
    private Slot neoFavoriteItems$lastLockSlot;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$handleMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = findSlot(mouseX, mouseY);
        if (button == neoFavoriteItems$LEFT_BUTTON && NeoFavoriteItemsFabricClient.isLockOperationKeyHeld()) {
            if (neoFavoriteItems$beginLockOperation(slot, mouseX, mouseY)) {
                cir.setReturnValue(true);
            }
            return;
        }

        if (neoFavoriteItems$guardNormalClick(slot, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$handleMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        if (button != neoFavoriteItems$LEFT_BUTTON || !neoFavoriteItems$lockOperationActive || FabricMouseTweaksBridge.isAvailable()) {
            return;
        }
        if (!NeoFavoriteItemsFabricClient.isLockOperationKeyHeld()) {
            neoFavoriteItems$finishLockOperation();
            return;
        }
        DebugLogger.debug("Fabric container state machine drag consumed without sampling");
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$handleMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == neoFavoriteItems$LEFT_BUTTON && neoFavoriteItems$lockOperationActive) {
            neoFavoriteItems$finishLockOperation();
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean neoFavoriteItems$beginLockOperation(Slot slot, double mouseX, double mouseY) {
        if (slot == null || !FabricSlotResolver.isPlayerInventorySlot(slot)) {
            return false;
        }
        neoFavoriteItems$lockOperationActive = true;
        neoFavoriteItems$lastLockSlot = null;
        neoFavoriteItems$toggleLockSlot(slot, "click");
        return true;
    }

    @Unique
    private void neoFavoriteItems$toggleLockSlot(Slot slot, String phase) {
        if (slot == null || slot == neoFavoriteItems$lastLockSlot || !FabricSlotResolver.isPlayerInventorySlot(slot)) {
            return;
        }
        neoFavoriteItems$lastLockSlot = slot;
        if (FabricSlotInteractionHandler.handleLockOperationToggle(slot)) {
            DebugLogger.debug(
                "Fabric container state machine lock {} handled: inventoryIndex={}",
                phase,
                FabricSlotResolver.getPlayerInventoryIndex(slot)
            );
        }
    }

    @Unique
    private boolean neoFavoriteItems$guardNormalClick(Slot slot, int button) {
        neoFavoriteItems$finishLockOperation();
        if (slot == null || !FabricSlotResolver.isPlayerInventorySlot(slot)) {
            return false;
        }

        ClickType clickType = net.minecraft.client.gui.screens.Screen.hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP;
        return FabricSlotInteractionHandler.shouldCancelGuardedInteraction(slot, clickType, button);
    }

    @Unique
    private void neoFavoriteItems$finishLockOperation() {
        neoFavoriteItems$lockOperationActive = false;
        neoFavoriteItems$lastLockSlot = null;
    }

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

        if (NeoFavoriteItemsFabricClient.isLockOperationKeyHeld() && j == 0
            && (clickType == ClickType.QUICK_MOVE || clickType == ClickType.PICKUP)) {
            DebugLogger.debug(
                "Fabric slotClicked canceled after lock-operation mouse interception: slotId={} clickType={}",
                i,
                clickType
            );
            ci.cancel();
            return;
        }

        if (FabricSlotInteractionHandler.shouldCancelGuardedInteraction(slot, clickType, j)) {
            ci.cancel();
        }
    }
}
