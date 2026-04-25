package mycraft.yuyears.neofavoriteitems.neoforge.mixin;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoFavoriteItemsNeoForge;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeSlotInteractionHandler;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeSlotResolver;
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
public abstract class MouseTweaksMainMixin {
    @Inject(
        method = "onMouseClicked(Lnet/minecraft/client/gui/screens/Screen;DDLyalter/mousetweaks/MouseButton;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lyalter/mousetweaks/Main;updateScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
            shift = At.Shift.AFTER
        ),
        require = 0
    )
    private static void neoFavoriteItems$enableLockDrag(
        Screen screen,
        double x,
        double y,
        @Coerce Object button,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (isLeftButton(button) && NeoFavoriteItemsNeoForge.isLockOperationKeyHeld() && readMouseTweaksField("handler") != null) {
            writeMouseTweaksField("canDoLMBDrag", true);
            DebugLogger.debug("NeoForge Mouse Tweaks lock drag enabled");
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
    private static void neoFavoriteItems$handleLockDrag(
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
        if (handler == null) {
            return;
        }

        Slot selectedSlot = resolveSlotUnderMouse(screen, handler, x, y);
        Slot previousSlot = readMouseTweaksField("oldSelectedSlot", Slot.class);
        if (selectedSlot == null || selectedSlot == previousSlot || isIgnored(handler, selectedSlot)) {
            return;
        }

        if (!NeoForgeSlotResolver.isPlayerInventorySlot(selectedSlot)) {
            writeMouseTweaksField("oldSelectedSlot", selectedSlot);
            cir.setReturnValue(false);
            return;
        }

        if (NeoForgeSlotInteractionHandler.handleLockOperationToggle(selectedSlot)) {
            writeMouseTweaksField("oldSelectedSlot", selectedSlot);
            DebugLogger.debug(
                "NeoForge Mouse Tweaks lock drag toggle handled: inventoryIndex={}",
                NeoForgeSlotResolver.getPlayerInventoryIndex(selectedSlot)
            );
            cir.setReturnValue(true);
        }
    }

    private static boolean isLeftButton(Object button) {
        return button instanceof Enum<?> enumButton && "LEFT".equals(enumButton.name());
    }

    private static Slot resolveSlotUnderMouse(Screen screen, Object handler, double x, double y) {
        Slot sophisticatedSlot = invokeSophisticatedFindSlot(screen, x, y);
        return sophisticatedSlot != null ? sophisticatedSlot : invokeSlotUnderMouse(handler, x, y);
    }

    private static Slot invokeSophisticatedFindSlot(Screen screen, double x, double y) {
        if (!isSophisticatedStorageScreen(screen)) {
            return null;
        }

        try {
            Method method = screen.getClass().getMethod("findSlot", double.class, double.class);
            Object result = method.invoke(screen, x, y);
            return result instanceof Slot slot ? slot : null;
        } catch (ReflectiveOperationException e) {
            DebugLogger.debug("NeoForge Mouse Tweaks lock drag sophisticated slot lookup failed: {}", e.toString());
            return null;
        }
    }

    private static boolean isSophisticatedStorageScreen(Screen screen) {
        if (screen == null) {
            return false;
        }

        Class<?> current = screen.getClass();
        while (current != null) {
            if ("net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase".equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private static Slot invokeSlotUnderMouse(Object handler, double x, double y) {
        try {
            Method method = handler.getClass().getMethod("getSlotUnderMouse", double.class, double.class);
            return (Slot) method.invoke(handler, x, y);
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

    private static boolean invokeMouseTweaksClickSlot(Object handler, Slot slot, Object button) {
        try {
            Method method = handler.getClass().getMethod("clickSlot", Slot.class, button.getClass(), boolean.class);
            method.invoke(handler, slot, button, false);
            return true;
        } catch (ReflectiveOperationException e) {
            DebugLogger.debug("NeoForge Mouse Tweaks lock drag click simulation failed: {}", e.toString());
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
