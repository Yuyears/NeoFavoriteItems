package mycraft.yuyears.neofavoriteitems.forge.render;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.application.InteractionGuardService;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.forge.ForgeFavoriteNetworking;
import mycraft.yuyears.neofavoriteitems.forge.ForgeSlotResolver;
import mycraft.yuyears.neofavoriteitems.forge.NeoFavoriteItemsForge;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import mycraft.yuyears.neofavoriteitems.render.OverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import mycraft.yuyears.neofavoriteitems.common.util.ReflectionHelper;

import java.io.IOException;
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

    @SubscribeEvent
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
                return;
            }

            if (slot == null || !isPlayerInventorySlot(slot)) {
                DebugLogger.debug(
                    "Forge lock-operation click ignored: reason={} button={}",
                    slot == null ? "no_slot" : "not_player_inventory",
                    event.getButton()
                );
                return;
            }

            var logicalSlot = SlotMappingService.fromPlayerInventoryIndex(getContainerSlotIndex(slot));
            if (logicalSlot.isEmpty()) {
                DebugLogger.debug(
                    "Forge lock-operation click ignored: reason=unmapped_slot inventoryIndex={}",
                    getContainerSlotIndex(slot)
                );
                return;
            }

            event.setCanceled(true);
            int inventoryIndex = getContainerSlotIndex(slot);
            if (ForgeFavoriteNetworking.trySendToggle(inventoryIndex)) {
                return;
            }

            if (isLockableSlot(logicalSlot.get(), hasItem(slot))) {
                FavoritesManager.getInstance().toggleSlotFavorite(logicalSlot.get());
                NeoFavoriteItemsForge.showSlotToggleMessage(logicalSlot.get());
                DebugLogger.debug(
                    "Forge slot lock toggled: logicalSlot={} inventoryIndex={} nowLocked={}",
                    logicalSlot.get().value(),
                    inventoryIndex,
                    FavoritesManager.getInstance().isSlotFavorite(logicalSlot.get())
                );
            } else {
                DebugLogger.debug(
                    "Forge slot toggle ignored: logicalSlot={} inventoryIndex={} hasItem=false reason=empty_slot",
                    logicalSlot.get().value(),
                    getContainerSlotIndex(slot)
                );
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
            boolean isFavorite = favoritesManager.isSlotFavorite(slotIndex);
            int highlightColor = isFavorite ? getUnlockableHighlightColor() : getLockableHighlightColor();
            float highlightOpacity = isFavorite ? getUnlockableHighlightOpacity() : getLockableHighlightOpacity();
            boolean renderInFront = isFavorite ? shouldRenderUnlockableHighlightInFront() : shouldRenderLockableHighlightInFront();
            renderStyle(context, x, y, getHighlightStyle(), highlightColor, highlightOpacity, 1.0f, renderInFront);
        }

        if (!shouldRenderOverlay(slotIndex)) {
            return;
        }

        float multiplier = isHoldingBypassKey ? getBypassOverlayOpacityMultiplier() : 1.0f;
        renderStyle(context, x, y, getOverlayStyle(slotIndex, isHoldingBypassKey), getLockedOverlayColor(), getLockedOverlayOpacity(), multiplier, shouldRenderLockedOverlayInFront());
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
                renderStyle(context, x, y, getOverlayStyle(slotIndex, false), getLockedOverlayColor(), getLockedOverlayOpacity(), 1.0f, shouldRenderLockedOverlayInFront());
            }
        }
    }

    private void renderStyle(GuiGraphics context, int x, int y, NeoFavoriteItemsConfig.OverlayStyle style, int color, float opacity, float multiplier, boolean renderInFront) {
        context.pose().pushPose();
        if (renderInFront) {
            context.pose().translate(0.0f, 0.0f, OVERLAY_Z_OFFSET);
        }
        try {
            switch (style) {
                case BORDER -> renderTextureOverlay(context, x, y, BORDER_TEXTURE, color, opacity, multiplier);
                case CLASSIC -> renderTextureOverlay(context, x, y, CLASSIC_TEXTURE, color, opacity, multiplier);
                case FRAMEWORK -> renderTextureOverlay(context, x, y, FRAMEWORK_TEXTURE, color, opacity, multiplier);
                case HIGHLIGHT -> renderTextureOverlay(context, x, y, HIGHLIGHT_TEXTURE, color, opacity, multiplier);
                case BRACKETS -> renderTextureOverlay(context, x, y, BRACKETS_TEXTURE, color, opacity, multiplier);
                case LOCK -> renderTextureOverlay(context, x, y, LOCK_TEXTURE, color, opacity, multiplier);
                case MARK -> renderTextureOverlay(context, x, y, MARK_TEXTURE, color, opacity, multiplier);
                case TAG -> renderTextureOverlay(context, x, y, TAG_TEXTURE, color, opacity, multiplier);
                case STAR -> renderTextureOverlay(context, x, y, STAR_TEXTURE, color, opacity, multiplier);
                case COLOR_OVERLAY -> renderColorOverlay(context, x, y, color, getColorOverlayOpacity(), multiplier);
            }
        } finally {
            context.pose().popPose();
        }
    }

    public void renderTooltipOverlay(GuiGraphics context, int x, int y, String text) {
        context.renderTooltip(Minecraft.getInstance().font, Component.literal(text), x, y);
    }

    private void renderTextureOverlay(GuiGraphics context, int x, int y, ResourceLocation texture, int color, float opacity, float multiplier) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        applyOverlayTint(color, opacity, multiplier);
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
