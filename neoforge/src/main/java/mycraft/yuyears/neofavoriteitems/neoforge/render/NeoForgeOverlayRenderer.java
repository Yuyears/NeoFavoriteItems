package mycraft.yuyears.neofavoriteitems.neoforge.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoFavoriteItemsNeoForge;
import mycraft.yuyears.neofavoriteitems.render.OverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
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

    @SubscribeEvent
    public void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!NeoFavoriteItemsNeoForge.isLockOperationKeyHeld() || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }

        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            Slot slot = findSlotAt(screen, event.getMouseX(), event.getMouseY());
            if (slot == null || !isPlayerInventorySlot(slot)) {
                return;
            }

            var logicalSlot = SlotMappingService.fromPlayerInventoryIndex(getContainerSlotIndex(slot));
            if (logicalSlot.isPresent() && isLockableSlot(logicalSlot.get(), slot.hasItem())) {
                FavoritesManager.getInstance().toggleSlotFavorite(logicalSlot.get());
                NeoFavoriteItemsNeoForge.showSlotToggleMessage(logicalSlot.get());
                event.setCanceled(true);
            }
        }
    }

    private void renderContainerScreenOverlays(AbstractContainerScreen<?> screen, GuiGraphics context) {
        boolean isHoldingBypassKey = NeoFavoriteItemsNeoForge.isBypassKeyHeld();
        boolean isHoldingLockOperationKey = NeoFavoriteItemsNeoForge.isLockOperationKeyHeld();

        for (Slot slot : screen.getMenu().slots) {
            var logicalSlot = SlotMappingService.fromPlayerInventoryIndex(getContainerSlotIndex(slot));
            if (logicalSlot.isEmpty()) {
                continue;
            }

            if (isLockableSlot(logicalSlot.get(), slot.hasItem()) || shouldRenderOverlay(logicalSlot.get())) {
                int x = slot.x + getScreenLeft(screen);
                int y = slot.y + getScreenTop(screen);
                renderSlotOverlay(context, x, y, logicalSlot.get(), slot.hasItem(), isHoldingBypassKey, isHoldingLockOperationKey);
            }
        }
    }

    public void renderSlotOverlay(GuiGraphics context, int x, int y, LogicalSlotIndex slotIndex, boolean isHoldingBypassKey) {
        renderSlotOverlay(context, x, y, slotIndex, true, isHoldingBypassKey, false);
    }

    public void renderSlotOverlay(GuiGraphics context, int x, int y, LogicalSlotIndex slotIndex, boolean hasItem, boolean isHoldingBypassKey, boolean isHoldingLockOperationKey) {
        if (isHoldingLockOperationKey && isLockableSlot(slotIndex, hasItem)) {
            boolean isFavorite = favoritesManager.isSlotFavorite(slotIndex);
            boolean renderInFront = isFavorite ? shouldRenderUnlockableHighlightInFront() : shouldRenderLockableHighlightInFront();
            renderStyle(context, x, y, getHighlightStyle(), 0.7f, renderInFront);
        }

        if (!shouldRenderOverlay(slotIndex)) {
            return;
        }

        renderStyle(context, x, y, getOverlayStyle(slotIndex, isHoldingBypassKey), 1.0f, shouldRenderLockedOverlayInFront());
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
                renderStyle(context, x, y, getOverlayStyle(slotIndex, false), 1.0f, shouldRenderLockedOverlayInFront());
            }
        }
    }

    private void renderStyle(GuiGraphics context, int x, int y, NeoFavoriteItemsConfig.OverlayStyle style, float multiplier, boolean renderInFront) {
        context.pose().pushPose();
        if (renderInFront) {
            context.pose().translate(0.0f, 0.0f, OVERLAY_Z_OFFSET);
        }
        try {
            switch (style) {
                case BORDER -> renderTextureOverlay(context, x, y, BORDER_TEXTURE, multiplier);
                case CLASSIC -> renderTextureOverlay(context, x, y, CLASSIC_TEXTURE, multiplier);
                case FRAMEWORK -> renderTextureOverlay(context, x, y, FRAMEWORK_TEXTURE, multiplier);
                case HIGHLIGHT -> renderTextureOverlay(context, x, y, HIGHLIGHT_TEXTURE, multiplier);
                case BRACKETS -> renderTextureOverlay(context, x, y, BRACKETS_TEXTURE, multiplier);
                case LOCK -> renderTextureOverlay(context, x, y, LOCK_TEXTURE, multiplier);
                case MARK -> renderTextureOverlay(context, x, y, MARK_TEXTURE, multiplier);
                case TAG -> renderTextureOverlay(context, x, y, TAG_TEXTURE, multiplier);
                case STAR -> renderTextureOverlay(context, x, y, STAR_TEXTURE, multiplier);
                case COLOR_OVERLAY -> renderColorOverlay(context, x, y, multiplier);
            }
        } finally {
            context.pose().popPose();
        }
    }

    public void renderTooltipOverlay(GuiGraphics context, int x, int y, String text) {
        context.renderTooltip(Minecraft.getInstance().font, Component.literal(text), x, y);
    }

    private void renderTextureOverlay(GuiGraphics context, int x, int y, ResourceLocation texture, float multiplier) {
        applyOverlayTint(multiplier);
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

    private void renderColorOverlay(GuiGraphics context, int x, int y, float multiplier) {
        context.fill(x, y, x + 16, y + 16, getColorArgb(getLockedOverlayColor(), getLockedOverlayOpacity(), multiplier * 0.3f));
    }

    private Slot findSlotAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
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

    private boolean isPlayerInventorySlot(Slot slot) {
        var player = Minecraft.getInstance().player;
        return player != null && slot.container == player.getInventory();
    }

    private void applyOverlayTint(float multiplier) {
        int color = getLockedOverlayColor();
        RenderSystem.setShaderColor(
            getColorRed(color),
            getColorGreen(color),
            getColorBlue(color),
            getColorAlpha(color, getLockedOverlayOpacity(), multiplier)
        );
    }

    private void resetOverlayTint() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private int getContainerSlotIndex(Slot slot) {
        Integer methodResult = invokeIntMethod(slot, "getContainerSlot");
        if (methodResult != null) {
            return methodResult;
        }

        Integer fieldResult = readIntField(slot, "slot");
        if (fieldResult != null) {
            return fieldResult;
        }

        fieldResult = readIntField(slot, "index");
        return fieldResult == null ? -1 : fieldResult;
    }

    private int getScreenLeft(AbstractContainerScreen<?> screen) {
        Integer value = readIntField(screen, "leftPos");
        return value == null ? 0 : value;
    }

    private int getScreenTop(AbstractContainerScreen<?> screen) {
        Integer value = readIntField(screen, "topPos");
        return value == null ? 0 : value;
    }

    private Integer invokeIntMethod(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            return (Integer) method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
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

    private record TextureSize(int width, int height) {
        private static final TextureSize DEFAULT = new TextureSize(16, 16);
    }
}
