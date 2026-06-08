package mycraft.yuyears.neofavoriteitems.neoforge.mixin.compat;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoFavoriteItemsNeoForge;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeLockOperationStateMachine;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "yalter.mousetweaks.Main", remap = false)
public abstract class MouseTweaksMainCompatMixin {
    private static boolean neoFavoriteItems$lockDragPrimed;

    @Inject(
        method = "onMouseClicked(Lnet/minecraft/client/gui/screens/Screen;DDLyalter/mousetweaks/MouseButton;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lyalter/mousetweaks/Main;updateScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true,
        require = 0
    )
    private static void neoFavoriteItems$passLockClickToStateMachine(
        Screen screen,
        double x,
        double y,
        @Coerce Object button,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!isLeftButton(button) || !NeoFavoriteItemsNeoForge.isLockOperationKeyHeld()) {
            return;
        }

        Object handler = readMouseTweaksField("handler");
        if (handler != null) {
            Slot selectedSlot = invokeSlotUnderMouse(handler, x, y);
            writeMouseTweaksField("oldSelectedSlot", selectedSlot);
            writeMouseTweaksField("canDoLMBDrag", true);
            neoFavoriteItems$lockDragPrimed = true;
            DebugLogger.debug(
                "NeoForge Mouse Tweaks lock drag primed for state machine: slotNull={}",
                selectedSlot == null
            );
            cir.setReturnValue(false);
        } else {
            neoFavoriteItems$lockDragPrimed = false;
        }
    }

    @Inject(
        method = "onMouseDrag(Lnet/minecraft/client/gui/screens/Screen;DDLyalter/mousetweaks/MouseButton;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lyalter/mousetweaks/Main;updateScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true,
        require = 0
    )
    private static void neoFavoriteItems$passLockDragToStateMachine(
        Screen screen,
        double x,
        double y,
        @Coerce Object button,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!isLeftButton(button) || !NeoFavoriteItemsNeoForge.isLockOperationKeyHeld()) {
            neoFavoriteItems$lockDragPrimed = false;
            return;
        }

        Object handler = readMouseTweaksField("handler");
        if (handler == null) {
            neoFavoriteItems$lockDragPrimed = false;
            DebugLogger.debug("NeoForge Mouse Tweaks lock drag suppressed without handler");
            cir.setReturnValue(false);
            return;
        }

        Slot selectedSlot = invokeSlotUnderMouse(handler, x, y);
        Slot previousSlot = neoFavoriteItems$lockDragPrimed ? readMouseTweaksField("oldSelectedSlot", Slot.class) : null;
        neoFavoriteItems$lockDragPrimed = true;

        if (selectedSlot == null) {
            writeMouseTweaksField("oldSelectedSlot", null);
            DebugLogger.debug("NeoForge Mouse Tweaks lock drag released to state machine: reason=no_slot");
            cir.setReturnValue(true);
            return;
        }
        if (selectedSlot == previousSlot) {
            cir.setReturnValue(true);
            return;
        }
        if (isIgnored(handler, selectedSlot)) {
            writeMouseTweaksField("oldSelectedSlot", selectedSlot);
            DebugLogger.debug("NeoForge Mouse Tweaks lock drag released to state machine: reason=ignored_slot");
            cir.setReturnValue(true);
            return;
        }

        writeMouseTweaksField("oldSelectedSlot", selectedSlot);
        if (NeoForgeLockOperationStateMachine.INSTANCE.toggleEnteredSlot(selectedSlot, "mousetweaks-drag-slot")) {
            DebugLogger.debug("NeoForge Mouse Tweaks lock drag slot released to state machine: slotId={}", selectedSlot.index);
        } else {
            DebugLogger.debug("NeoForge Mouse Tweaks lock drag suppressed without active state machine");
        }
        cir.setReturnValue(true);
    }

    @Inject(
        method = "onMouseReleased(Lnet/minecraft/client/gui/screens/Screen;DDLyalter/mousetweaks/MouseButton;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lyalter/mousetweaks/Main;updateScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true,
        require = 0
    )
    private static void neoFavoriteItems$passLockReleaseToStateMachine(
        Screen screen,
        double x,
        double y,
        @Coerce Object button,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (isLeftButton(button) && neoFavoriteItems$lockDragPrimed) {
            neoFavoriteItems$lockDragPrimed = false;
            DebugLogger.debug("NeoForge Mouse Tweaks lock operation released");
            cir.setReturnValue(false);
        }
    }

    private static boolean isLeftButton(Object button) {
        return button instanceof Enum<?> enumButton && "LEFT".equals(enumButton.name());
    }

    private static Slot invokeSlotUnderMouse(Object handler, double x, double y) {
        try {
            Method method = handler.getClass().getMethod("getSlotUnderMouse", double.class, double.class);
            Object result = method.invoke(handler, x, y);
            return result instanceof Slot slot ? slot : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static boolean isIgnored(Object handler, Slot slot) {
        try {
            Method method = handler.getClass().getMethod("isIgnored", Slot.class);
            return Boolean.TRUE.equals(method.invoke(handler, slot));
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static Object readMouseTweaksField(String name) {
        return readMouseTweaksField(name, Object.class);
    }

    private static <T> T readMouseTweaksField(String name, Class<T> type) {
        try {
            Field field = Class.forName("yalter.mousetweaks.Main").getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(null);
            return type.isInstance(value) ? type.cast(value) : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static void writeMouseTweaksField(String name, Object value) {
        try {
            Field field = Class.forName("yalter.mousetweaks.Main").getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
