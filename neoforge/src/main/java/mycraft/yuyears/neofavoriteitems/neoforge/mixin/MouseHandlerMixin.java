package mycraft.yuyears.neofavoriteitems.neoforge.mixin;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoFavoriteItemsNeoForge;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeLockOperationStateMachine;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeMouseTweaksBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = net.minecraft.client.MouseHandler.class, priority = 2000)
public abstract class MouseHandlerMixin {
    @Shadow
    private double xpos;

    @Shadow
    private double ypos;

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$handleLockOperationPress(long window, int button, int action, int modifiers, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getWindow().getWindow() != window
            || button != GLFW.GLFW_MOUSE_BUTTON_LEFT
            || action != GLFW.GLFW_PRESS
            || !(minecraft.screen instanceof AbstractContainerScreen<?>)
            || !NeoFavoriteItemsNeoForge.isLockOperationKeyHeld()) {
            return;
        }

        double scaledMouseX = xpos * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        double scaledMouseY = ypos * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();
        if (NeoForgeLockOperationStateMachine.INSTANCE.beginPress(scaledMouseX, scaledMouseY, "mouse-handler")) {
            if (NeoForgeMouseTweaksBridge.isAvailable()) {
                DebugLogger.debug(
                    "NeoForge mouse handler press primed lock state machine for Mouse Tweaks: mouseX={} mouseY={}",
                    scaledMouseX,
                    scaledMouseY
                );
            } else {
                DebugLogger.debug("NeoForge mouse handler press consumed by lock state machine: mouseX={} mouseY={}", scaledMouseX, scaledMouseY);
                ci.cancel();
            }
        }
    }
}
