package mycraft.yuyears.neofavoriteitems.forge.render;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.application.InteractionGuardService;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.forge.ForgeSlotResolver;
import mycraft.yuyears.neofavoriteitems.forge.NeoFavoriteItemsForge;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import mycraft.yuyears.neofavoriteitems.render.OverlayRenderDescriptor;
import mycraft.yuyears.neofavoriteitems.render.OverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import mycraft.yuyears.neofavoriteitems.common.util.ReflectionHelper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ForgeOverlayRenderer extends OverlayRenderer {
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

    public ForgeOverlayRenderer() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
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
            Slot slot = findSlotAt(screen, event.getMouseX(), event.getMouseY());
            boolean lockOperation = NeoFavoriteItemsForge.isLockOperationKeyHeld()
                && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT;

            if (!lockOperation) {
                guardFavoriteSlotClick(event, slot);
            } else {
                DebugLogger.debug("Forge lock-operation click left for container state machine");
            }
        }
    }

    private void guardFavoriteSlotClick(ScreenEvent.MouseButtonPressed.Pre event, Slot slot) {
        if (slot == null || !isPlayerInventorySlot(slot)) {
            return;
        }

        int inventoryIndex = getContainerSlotIndex(slot);
        InteractionType interactionType = hasShiftDown() ? InteractionType.QUICK_MOVE : InteractionType.CLICK;
        var decision = InteractionGuardService.getInstance().evaluate(
            inventoryIndex,
            interactionType,
            NeoFavoriteItemsForge.isBypassKeyHeld(),
            hasItem(slot)
        );
        if (decision.denied()) {
            DebugLogger.debug("Forge slot interaction canceled: inventoryIndex={} interactionType={}", inventoryIndex, interactionType);
            event.setCanceled(true);
        }
    }

    private boolean hasShiftDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
            || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private void renderContainerScreenOverlays(AbstractContainerScreen<?> screen, GuiGraphics context) {
        boolean isHoldingBypassKey = NeoFavoriteItemsForge.isBypassKeyHeld();
        boolean isHoldingLockOperationKey = NeoFavoriteItemsForge.isLockOperationKeyHeld();

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
        Slot screenSlot = invokeScreenFindSlot(screen, mouseX, mouseY);
        if (screenSlot != null) {
            return screenSlot;
        }

        Slot hoveredSlot = ReflectionHelper.readField(screen, "hoveredSlot", Slot.class);
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
            DebugLogger.debug("Forge screen slot lookup failed: screen={} error={}", screen.getClass().getName(), e.toString());
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

    private boolean isPlayerInventorySlot(Slot slot) {
        return ForgeSlotResolver.isPlayerInventorySlot(slot);
    }

    private boolean hasItem(Slot slot) {
        return ForgeSlotResolver.hasItem(slot);
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
        return ForgeSlotResolver.getPlayerInventoryIndex(slot);
    }

    private int getScreenLeft(AbstractContainerScreen<?> screen) {
        Integer value = ReflectionHelper.readIntField(screen, "leftPos");
        return value == null ? 0 : value;
    }

    private int getScreenTop(AbstractContainerScreen<?> screen) {
        Integer value = ReflectionHelper.readIntField(screen, "topPos");
        return value == null ? 0 : value;
    }

    private record TextureSize(int width, int height) {
        private static final TextureSize DEFAULT = new TextureSize(16, 16);
    }
}
