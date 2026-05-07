package mycraft.yuyears.neofavoriteitems.forge.mixin;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.forge.ForgeMouseTweaksBridge;
import mycraft.yuyears.neofavoriteitems.forge.ForgeSlotInteractionHandler;
import mycraft.yuyears.neofavoriteitems.forge.ForgeSlotResolver;
import mycraft.yuyears.neofavoriteitems.forge.NeoFavoriteItemsForge;
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
        if (button == neoFavoriteItems$LEFT_BUTTON && NeoFavoriteItemsForge.isLockOperationKeyHeld()) {
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
        if (button != neoFavoriteItems$LEFT_BUTTON || !neoFavoriteItems$lockOperationActive || ForgeMouseTweaksBridge.isAvailable()) {
            return;
        }
        if (!NeoFavoriteItemsForge.isLockOperationKeyHeld()) {
            neoFavoriteItems$finishLockOperation();
            return;
        }
        DebugLogger.debug("Forge container state machine drag consumed without sampling");
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
        if (slot == null || !ForgeSlotResolver.isPlayerInventorySlot(slot)) {
            return false;
        }
        neoFavoriteItems$lockOperationActive = true;
        neoFavoriteItems$lastLockSlot = null;
        neoFavoriteItems$toggleLockSlot(slot, "click");
        return true;
    }

    @Unique
    private void neoFavoriteItems$toggleLockSlot(Slot slot, String phase) {
        if (slot == null || slot == neoFavoriteItems$lastLockSlot || !ForgeSlotResolver.isPlayerInventorySlot(slot)) {
            return;
        }
        neoFavoriteItems$lastLockSlot = slot;
        if (ForgeSlotInteractionHandler.handleLockOperationToggle(slot)) {
            DebugLogger.debug(
                "Forge container state machine lock {} handled: inventoryIndex={}",
                phase,
                ForgeSlotResolver.getPlayerInventoryIndex(slot)
            );
        }
    }

    @Unique
    private boolean neoFavoriteItems$guardNormalClick(Slot slot, int button) {
        neoFavoriteItems$finishLockOperation();
        if (slot == null || !ForgeSlotResolver.isPlayerInventorySlot(slot)) {
            return false;
        }

        ClickType clickType = net.minecraft.client.gui.screens.Screen.hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP;
        return ForgeSlotInteractionHandler.shouldCancelGuardedInteraction(slot, clickType, button);
    }

    @Unique
    private void neoFavoriteItems$finishLockOperation() {
        neoFavoriteItems$lockOperationActive = false;
        neoFavoriteItems$lastLockSlot = null;
    }

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

        if (NeoFavoriteItemsForge.isLockOperationKeyHeld() && button == 0
            && (clickType == ClickType.QUICK_MOVE || clickType == ClickType.PICKUP)) {
            DebugLogger.debug(
                "Forge slotClicked canceled after lock-operation mouse interception: slotId={} clickType={}",
                slotId,
                clickType
            );
            ci.cancel();
            return;
        }

        if (ForgeSlotInteractionHandler.shouldCancelGuardedInteraction(slot, clickType, button)) {
            ci.cancel();
        }
    }
}
