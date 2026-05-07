package mycraft.yuyears.neofavoriteitems.neoforge.mixin;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoFavoriteItemsNeoForge;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeLockOperationStateMachine;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeMouseTweaksBridge;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeSlotInteractionHandler;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeSlotResolver;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$handleLockOperationMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
            && NeoFavoriteItemsNeoForge.isLockOperationKeyHeld()
            && NeoForgeLockOperationStateMachine.INSTANCE.beginPress(mouseX, mouseY, "container-mouseClicked")) {
            DebugLogger.debug("NeoForge container mouseClicked consumed by lock state machine: mouseX={} mouseY={}", mouseX, mouseY);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged(DDIDD)Z", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$handleLockOperationMouseDragged(
        double mouseX,
        double mouseY,
        int button,
        double dragX,
        double dragY,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
            && !NeoForgeMouseTweaksBridge.isAvailable()
            && NeoForgeLockOperationStateMachine.INSTANCE.consumeActiveDrag("container-mouseDragged")) {
            DebugLogger.debug("NeoForge container mouseDragged consumed by lock state machine without sampling: mouseX={} mouseY={}", mouseX, mouseY);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$handleLockOperationMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && NeoForgeLockOperationStateMachine.INSTANCE.isActive()) {
            NeoForgeLockOperationStateMachine.INSTANCE.finish();
            DebugLogger.debug("NeoForge container mouseReleased finished lock state machine: mouseX={} mouseY={}", mouseX, mouseY);
            cir.setReturnValue(true);
        }
    }

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

        if ((NeoFavoriteItemsNeoForge.isLockOperationKeyHeld() || NeoForgeLockOperationStateMachine.INSTANCE.isActive()) && button == 0
            && (clickType == ClickType.QUICK_MOVE || clickType == ClickType.PICKUP)) {
            DebugLogger.debug(
                "NeoForge slotClicked canceled after lock-operation mouse interception: slotId={} clickType={}",
                slotId,
                clickType
            );
            ci.cancel();
            return;
        }

        if (NeoForgeSlotInteractionHandler.shouldCancelGuardedInteraction(slot, clickType, button)) {
            ci.cancel();
        }
    }
}
