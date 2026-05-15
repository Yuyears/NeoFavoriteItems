package mycraft.yuyears.neofavoriteitems.neoforge.render;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.application.InteractionGuardService;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoFavoriteItemsNeoForge;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeLockOperationStateMachine;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeMouseTweaksBridge;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeSlotResolver;
import mycraft.yuyears.neofavoriteitems.render.OverlayRenderDescriptor;
import mycraft.yuyears.neofavoriteitems.render.OverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NeoForgeOverlayRenderer extends OverlayRenderer {
    private static final float OVERLAY_Z_OFFSET = 300.0f;
    private static final ResourceLocation BORDER_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, BORDER_TEXTURE_PATH);
    private static final ResourceLocation CLASSIC_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, CLASSIC_TEXTURE_PATH);
    private static final ResourceLocation FRAMEWORK_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, FRAMEWORK_TEXTURE_PATH);
    private static final ResourceLocation HIGHLIGHT_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, HIGHLIGHT_TEXTURE_PATH);
    private static final ResourceLocation BRACKETS_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, BRACKETS_TEXTURE_PATH);
    private static final ResourceLocation LOCK_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, LOCK_TEXTURE_PATH);
    private static final ResourceLocation MARK_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, MARK_TEXTURE_PATH);
    private static final ResourceLocation TAG_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, TAG_TEXTURE_PATH);
    private static final ResourceLocation STAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, STAR_TEXTURE_PATH);
    private final Map<ResourceLocation, TextureSize> textureSizes = new HashMap<>();

    public NeoForgeOverlayRenderer() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?>) {
            renderContainerScreenOverlays((AbstractContainerScreen<?>) event.getScreen(), event.getGuiGraphics());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            DebugLogger.debug(
                "NeoForge mouse click pre: screen={} button={} lockOperation={} bypass={}",
                screen.getClass().getName(),
                event.getButton(),
                NeoFavoriteItemsNeoForge.isLockOperationKeyHeld(),
                NeoFavoriteItemsNeoForge.isBypassKeyHeld()
            );
            Slot slot = findSlotAt(screen, event.getMouseX(), event.getMouseY());
            boolean lockOperation = NeoFavoriteItemsNeoForge.isLockOperationKeyHeld()
                && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT;

            if (!lockOperation) {
                guardFavoriteSlotClick(event, slot);
            } else {
                if (NeoForgeLockOperationStateMachine.INSTANCE.beginPress(event.getMouseX(), event.getMouseY(), "screen-event")) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (event.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
            && !NeoForgeMouseTweaksBridge.isAvailable()
            && NeoForgeLockOperationStateMachine.INSTANCE.consumeActiveDrag("screen-event-drag")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouseButtonReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && NeoForgeLockOperationStateMachine.INSTANCE.isActive()) {
            NeoForgeLockOperationStateMachine.INSTANCE.finish();
            event.setCanceled(true);
            return;
        }

        if ((event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT || event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
            && event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            Slot slot = findSlotAt(screen, event.getMouseX(), event.getMouseY());
            if (shouldCancelFavoriteSlotInteraction(slot)) {
                event.setCanceled(true);
            }
        }
    }

    private void guardFavoriteSlotClick(ScreenEvent.MouseButtonPressed.Pre event, Slot slot) {
        if (shouldCancelFavoriteSlotInteraction(slot)) {
            event.setCanceled(true);
        }
    }

    private boolean shouldCancelFavoriteSlotInteraction(Slot slot) {
        if (slot == null || !isPlayerInventorySlot(slot)) {
            return false;
        }

        int inventoryIndex = getContainerSlotIndex(slot);
        InteractionType interactionType = hasShiftDown() ? InteractionType.QUICK_MOVE : InteractionType.CLICK;
        var decision = InteractionGuardService.getInstance().evaluate(
            inventoryIndex,
            interactionType,
            NeoFavoriteItemsNeoForge.isBypassKeyHeld(),
            hasItem(slot)
        );
        if (decision.denied()) {
            DebugLogger.debug("NeoForge slot interaction canceled: inventoryIndex={} interactionType={}", inventoryIndex, interactionType);
            return true;
        }
        return false;
    }

    private boolean hasShiftDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
            || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private void renderContainerScreenOverlays(AbstractContainerScreen<?> screen, GuiGraphics context) {
        boolean isHoldingBypassKey = NeoFavoriteItemsNeoForge.isBypassKeyHeld();
        boolean isHoldingLockOperationKey = NeoFavoriteItemsNeoForge.isLockOperationKeyHeld();

        for (Slot slot : screen.getMenu().slots) {
            if (!isPlayerInventorySlot(slot)) {
                continue;
            }

            var logicalSlot = SlotMappingService.fromPlayerInventoryIndex(getContainerSlotIndex(slot));
            if (logicalSlot.isEmpty()) {
                continue;
            }

            boolean hasItem = hasItem(slot);
            if (isLockableSlot(logicalSlot.get(), hasItem) || shouldRenderOverlay(logicalSlot.get())) {
                int x = slot.x + getScreenLeft(screen);
                int y = slot.y + getScreenTop(screen);
                renderSlotOverlay(context, x, y, logicalSlot.get(), hasItem, isHoldingBypassKey, isHoldingLockOperationKey);
            }
        }
    }

    public void renderSlotOverlay(GuiGraphics context, int x, int y, LogicalSlotIndex slotIndex, boolean isHoldingBypassKey) {
        renderSlotOverlay(context, x, y, slotIndex, true, isHoldingBypassKey, false);
    }

    public void renderSlotOverlay(GuiGraphics context, int x, int y, LogicalSlotIndex slotIndex, boolean hasItem, boolean isHoldingBypassKey, boolean isHoldingLockOperationKey) {
        if (isHoldingLockOperationKey && isLockableSlot(slotIndex, hasItem)) {
            renderStyle(context, x, y, highlightOverlayDescriptor(slotIndex, hasItem));
        }

        if (!shouldRenderOverlay(slotIndex)) {
            return;
        }

        renderStyle(context, x, y, lockedOverlayDescriptor(slotIndex, isHoldingBypassKey));
    }

    public void renderHotbarOverlays(GuiGraphics context) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.screen != null) {
            return;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int y = screenHeight - 19;

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            LogicalSlotIndex slotIndex = LogicalSlotIndex.of(hotbarSlot);
            if (shouldRenderOverlay(slotIndex)) {
                int x = screenWidth / 2 - 88 + hotbarSlot * 20;
                renderStyle(context, x, y, lockedOverlayDescriptor(slotIndex, false));
            }
        }
    }

    private void renderStyle(GuiGraphics context, int x, int y, OverlayRenderDescriptor descriptor) {
        context.pose().pushPose();
        if (descriptor.renderInFront()) {
            context.pose().translate(0.0f, 0.0f, OVERLAY_Z_OFFSET);
        }
        try {
            switch (descriptor.style()) {
                case BORDER -> renderTextureOverlay(context, x, y, BORDER_TEXTURE, descriptor);
                case CLASSIC -> renderTextureOverlay(context, x, y, CLASSIC_TEXTURE, descriptor);
                case FRAMEWORK -> renderTextureOverlay(context, x, y, FRAMEWORK_TEXTURE, descriptor);
                case HIGHLIGHT -> renderTextureOverlay(context, x, y, HIGHLIGHT_TEXTURE, descriptor);
                case BRACKETS -> renderTextureOverlay(context, x, y, BRACKETS_TEXTURE, descriptor);
                case LOCK -> renderTextureOverlay(context, x, y, LOCK_TEXTURE, descriptor);
                case MARK -> renderTextureOverlay(context, x, y, MARK_TEXTURE, descriptor);
                case TAG -> renderTextureOverlay(context, x, y, TAG_TEXTURE, descriptor);
                case STAR -> renderTextureOverlay(context, x, y, STAR_TEXTURE, descriptor);
                case COLOR_OVERLAY -> renderColorOverlay(context, x, y, descriptor.color(), getColorOverlayOpacity(), descriptor.multiplier());
            }
        } finally {
            context.pose().popPose();
        }
    }

    public void renderTooltipOverlay(GuiGraphics context, int x, int y, String text) {
        context.renderTooltip(Minecraft.getInstance().font, Component.literal(text), x, y);
    }

    private void renderTextureOverlay(GuiGraphics context, int x, int y, ResourceLocation texture, OverlayRenderDescriptor descriptor) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        applyOverlayTint(descriptor.color(), descriptor.opacity(), descriptor.multiplier());
        TextureSize size = getTextureSize(texture);
        context.blit(texture, x, y, 16, 16, 0.0f, 0.0f, size.width(), size.height(), size.width(), size.height());
        resetOverlayTint();
    }

    private TextureSize getTextureSize(ResourceLocation texture) {
        return textureSizes.computeIfAbsent(texture, this::readTextureSize);
    }

    private TextureSize readTextureSize(ResourceLocation texture) {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isPresent()) {
                try (var stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
                    return new TextureSize(image.getWidth(), image.getHeight());
                }
            }
        } catch (IOException ignored) {
        }
        return TextureSize.DEFAULT;
    }

    private void renderColorOverlay(GuiGraphics context, int x, int y, int color, float opacity, float multiplier) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.fill(x, y, x + 16, y + 16, getColorArgb(color, opacity, multiplier));
    }

    private Slot findSlotAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        Slot lockSlot = NeoForgeLockOperationStateMachine.INSTANCE.findLockOperationSlot(screen, mouseX, mouseY);
        if (lockSlot != null) {
            return lockSlot;
        }

        Slot sophisticatedSlot = invokeSophisticatedFindSlot(screen, mouseX, mouseY);
        if (sophisticatedSlot != null) {
            return sophisticatedSlot;
        }

        Slot screenSlot = invokeScreenFindSlot(screen, mouseX, mouseY);
        if (screenSlot != null) {
            return screenSlot;
        }

        Slot hoveredSlot = readSlotField(screen, "hoveredSlot");
        if (hoveredSlot != null) {
            return hoveredSlot;
        }

        int left = getScreenLeft(screen);
        int top = getScreenTop(screen);
        for (Slot slot : screen.getMenu().slots) {
            int x = left + slot.x;
            int y = top + slot.y;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return slot;
            }
        }
        return null;
    }

    private Slot invokeScreenFindSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        try {
            Method method = findMethod(screen.getClass(), "findSlot", double.class, double.class);
            if (method == null) {
                return null;
            }
            Object result = method.invoke(screen, mouseX, mouseY);
            return result instanceof Slot slot ? slot : null;
        } catch (ReflectiveOperationException e) {
            DebugLogger.debug("NeoForge screen slot lookup failed: screen={} error={}", screen.getClass().getName(), e.toString());
            return null;
        }
    }

    private Slot invokeSophisticatedFindSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        if (!isSophisticatedStorageScreen(screen)) {
            return null;
        }

        try {
            Method method = screen.getClass().getMethod("findSlot", double.class, double.class);
            Object result = method.invoke(screen, mouseX, mouseY);
            return result instanceof Slot slot ? slot : null;
        } catch (ReflectiveOperationException e) {
            DebugLogger.debug("NeoForge lock-operation sophisticated slot lookup failed: {}", e.toString());
            return null;
        }
    }

    private Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private boolean isSophisticatedStorageScreen(AbstractContainerScreen<?> screen) {
        Class<?> current = screen.getClass();
        while (current != null) {
            if ("net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase".equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private boolean isPlayerInventorySlot(Slot slot) {
        return NeoForgeSlotResolver.isPlayerInventorySlot(slot);
    }

    private boolean hasItem(Slot slot) {
        return NeoForgeSlotResolver.hasItem(slot);
    }

    private void applyOverlayTint(int color, float opacity, float multiplier) {
        RenderSystem.setShaderColor(
            getColorRed(color),
            getColorGreen(color),
            getColorBlue(color),
            getColorAlpha(color, opacity, multiplier)
        );
    }

    private void resetOverlayTint() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private int getContainerSlotIndex(Slot slot) {
        return NeoForgeSlotResolver.getPlayerInventoryIndex(slot);
    }

    private int getScreenLeft(AbstractContainerScreen<?> screen) {
        Integer value = readIntField(screen, "leftPos");
        return value == null ? 0 : value;
    }

    private int getScreenTop(AbstractContainerScreen<?> screen) {
        Integer value = readIntField(screen, "topPos");
        return value == null ? 0 : value;
    }

    private Integer readIntField(Object target, String name) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.getInt(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }
        return null;
    }

    private Slot readSlotField(Object target, String name) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                Object value = field.get(target);
                return value instanceof Slot slot ? slot : null;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }
        return null;
    }

    private record TextureSize(int width, int height) {
        private static final TextureSize DEFAULT = new TextureSize(16, 16);
    }
}
