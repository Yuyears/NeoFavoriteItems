package mycraft.yuyears.neofavoriteitems.neoforge.mixin.compat;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoFavoriteItemsNeoForge;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeLockOperationStateMachine;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeMouseTweaksBridge;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeSlotInteractionHandler;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeSlotResolver;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase", remap = false)
public abstract class SophisticatedStorageScreenCompatMixin {
    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void neoFavoriteItems$interceptSophisticatedMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        DebugLogger.debug(
            "NeoForge Sophisticated mouseClicked head: button={} lockOperation={} bypass={} mouseX={} mouseY={}",
            button,
            NeoFavoriteItemsNeoForge.isLockOperationKeyHeld(),
            NeoFavoriteItemsNeoForge.isBypassKeyHeld(),
            mouseX,
            mouseY
        );
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
            && NeoFavoriteItemsNeoForge.isLockOperationKeyHeld()
            && NeoForgeLockOperationStateMachine.INSTANCE.beginPress(mouseX, mouseY, "sophisticated-mouseClicked")) {
            DebugLogger.debug("NeoForge Sophisticated mouseClicked consumed by lock state machine: mouseX={} mouseY={}", mouseX, mouseY);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged(DDIDD)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void neoFavoriteItems$interceptSophisticatedMouseDragged(
        double mouseX,
        double mouseY,
        int button,
        double dragX,
        double dragY,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
            && !NeoForgeMouseTweaksBridge.isAvailable()
            && NeoForgeLockOperationStateMachine.INSTANCE.consumeActiveDrag("sophisticated-mouseDragged")) {
            DebugLogger.debug("NeoForge Sophisticated mouseDragged consumed by lock state machine without sampling: mouseX={} mouseY={}", mouseX, mouseY);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased(DDI)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void neoFavoriteItems$interceptSophisticatedMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && NeoForgeLockOperationStateMachine.INSTANCE.isActive()) {
            NeoForgeLockOperationStateMachine.INSTANCE.finish();
            DebugLogger.debug("NeoForge Sophisticated mouseReleased finished lock state machine: mouseX={} mouseY={}", mouseX, mouseY);
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void neoFavoriteItems$guardSophisticatedSlotClick(Slot slot, int slotId, int button, ClickType clickType, CallbackInfo ci) {
        DebugLogger.debug(
            "NeoForge Sophisticated slotClicked: slotNull={} slotId={} button={} clickType={} lockOperation={} bypass={}",
            slot == null,
            slotId,
            button,
            clickType,
            NeoFavoriteItemsNeoForge.isLockOperationKeyHeld(),
            NeoFavoriteItemsNeoForge.isBypassKeyHeld()
        );
        if (slot == null || !NeoForgeSlotResolver.isPlayerInventorySlot(slot)) {
            return;
        }

        if (NeoFavoriteItemsNeoForge.isLockOperationKeyHeld() && button == 0
            && (clickType == ClickType.QUICK_MOVE || clickType == ClickType.PICKUP)) {
            if (!NeoForgeLockOperationStateMachine.INSTANCE.isActive()
                && NeoForgeLockOperationStateMachine.INSTANCE.toggleLeakedSlotClick(slot, "sophisticated-slotClicked")) {
                DebugLogger.debug(
                    "NeoForge Sophisticated slotClicked routed synthetic lock click: slotId={} inventoryIndex={}",
                    slotId,
                    NeoForgeSlotResolver.getPlayerInventoryIndex(slot)
                );
            }
            DebugLogger.debug(
                "NeoForge Sophisticated slotClicked canceled after lock-operation mouse interception: slotId={} clickType={}",
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
