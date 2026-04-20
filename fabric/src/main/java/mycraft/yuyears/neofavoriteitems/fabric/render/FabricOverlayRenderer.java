
package mycraft.yuyears.neofavoriteitems.fabric.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.NativeImage;
import mycraft.yuyears.neofavoriteitems.fabric.FabricSlotInteractionHandler;
import mycraft.yuyears.neofavoriteitems.fabric.FabricSlotResolver;
import mycraft.yuyears.neofavoriteitems.fabric.NeoFavoriteItemsFabricClient;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import mycraft.yuyears.neofavoriteitems.render.OverlayRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

import java.lang.reflect.Field;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FabricOverlayRenderer extends OverlayRenderer {
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
    private boolean lastLoggedLockOperationKeyState;

    public FabricOverlayRenderer() {
        registerEvents();
        registerHudEvents();
        DebugLogger.debug("Fabric overlay renderer registered");
    }

    private void registerEvents() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?>) {
                DebugLogger.debug("Fabric container screen initialized: screen={} size={}x{}", screen.getClass().getName(), scaledWidth, scaledHeight);
                ScreenMouseEvents.allowMouseClick(screen).register((screen1, mouseX, mouseY, button) -> {
                    if (screen1 instanceof AbstractContainerScreen<?> containerScreen) {
                        return allowMouseClick(containerScreen, mouseX, mouseY, button);
                    }
                    return true;
                });
                ScreenEvents.afterRender(screen).register((screen1, context, mouseX, mouseY, delta) -> {
                    if (screen1 instanceof AbstractContainerScreen<?>) {
                        renderHandledScreenOverlays((AbstractContainerScreen<?>) screen1, context, mouseX, mouseY, delta);
                    }
                });
            }
        });
    }

    private boolean allowMouseClick(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        if (button != 0 || !NeoFavoriteItemsFabricClient.isLockOperationKeyHeld()) {
            return true;
        }

        Slot slot = findPlayerInventorySlotAt(screen, mouseX, mouseY);
        if (slot == null) {
            DebugLogger.debug("Fabric lock-operation mouse click ignored: reason=no_player_inventory_slot mouseX={} mouseY={}", mouseX, mouseY);
            return true;
        }

        FabricSlotInteractionHandler.handleLockOperationToggle(slot);
        return false;
    }

    private Slot findPlayerInventorySlotAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }

        int left = getScreenLeft(screen);
        int top = getScreenTop(screen);
        for (Slot slot : screen.getMenu().slots) {
            if (!FabricSlotResolver.isPlayerInventorySlot(slot, player)) {
                continue;
            }
            int x = left + slot.x;
            int y = top + slot.y;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return slot;
            }
        }
        return null;
    }

    private void registerHudEvents() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> renderHotbarOverlays(context));
    }

    private void renderHandledScreenOverlays(AbstractContainerScreen<?> screen, GuiGraphics context, int mouseX, int mouseY, float delta) {
        boolean isHoldingBypassKey = NeoFavoriteItemsFabricClient.isBypassKeyHeld();
        boolean isHoldingLockOperationKey = NeoFavoriteItemsFabricClient.isLockOperationKeyHeld();
        NeoFavoriteItemsFabricClient.logKeyStatesIfChanged();
        
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        int highlightableSlots = 0;
        int lockedSlots = 0;
        for (Slot slot : screen.getMenu().slots) {
            // 只处理属于玩家物品栏的槽位
            if (!FabricSlotResolver.isPlayerInventorySlot(slot, player)) {
                continue;
            }
            
            int inventoryIndex = FabricSlotResolver.getPlayerInventoryIndex(slot);
            boolean hasItem = FabricSlotResolver.hasItem(slot);
            var logicalSlot = SlotMappingService.fromPlayerInventoryIndex(inventoryIndex);
            if (logicalSlot.isEmpty()) {
                continue;
            }
            if (hasItem) {
                highlightableSlots++;
            }
            if (shouldRenderOverlay(logicalSlot.get())) {
                lockedSlots++;
            }
            if (hasItem || shouldRenderOverlay(logicalSlot.get())) {
                int x = slot.x + getScreenLeft(screen);
                int y = slot.y + getScreenTop(screen);
                renderSlotOverlay(context, x, y, logicalSlot.get(), hasItem, isHoldingBypassKey, isHoldingLockOperationKey);
            }
        }
        if (isHoldingLockOperationKey != lastLoggedLockOperationKeyState) {
            DebugLogger.debug(
                "Fabric overlay lock-operation render state: active={} screen={} highlightableSlots={} lockedSlots={}",
                isHoldingLockOperationKey,
                screen.getClass().getName(),
                highlightableSlots,
                lockedSlots
            );
            lastLoggedLockOperationKeyState = isHoldingLockOperationKey;
        }
    }

    public void renderSlotOverlay(GuiGraphics context, int x, int y, LogicalSlotIndex slotIndex, boolean isHoldingBypassKey) {
        renderSlotOverlay(context, x, y, slotIndex, true, isHoldingBypassKey, false);
    }

    public void renderSlotOverlay(GuiGraphics context, int x, int y, LogicalSlotIndex slotIndex, boolean hasItem, boolean isHoldingBypassKey, boolean isHoldingLockOperationKey) {
        if (isHoldingLockOperationKey && (hasItem || shouldRenderOverlay(slotIndex))) {
            boolean isFavorite = favoritesManager.isSlotFavorite(slotIndex);
            int highlightColor = isFavorite ? getUnlockableHighlightColor() : getLockableHighlightColor();
            float highlightOpacity = isFavorite ? getUnlockableHighlightOpacity() : getLockableHighlightOpacity();
            boolean renderInFront = isFavorite ? shouldRenderUnlockableHighlightInFront() : shouldRenderLockableHighlightInFront();
            renderStyle(context, x, y, getHighlightStyle(), highlightColor, highlightOpacity, 1.0f, renderInFront);
            return;
        }

        if (shouldRenderOverlay(slotIndex)) {
            float multiplier = isHoldingBypassKey ? getBypassOverlayOpacityMultiplier() : 1.0f;
            renderStyle(context, x, y, getOverlayStyle(slotIndex, isHoldingBypassKey), getLockedOverlayColor(), getLockedOverlayOpacity(), multiplier, shouldRenderLockedOverlayInFront());
        }
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
                case COLOR_OVERLAY -> renderColorOverlay(context, x, y, color, opacity, multiplier);
            }
        } finally {
            context.pose().popPose();
        }
    }

    public void renderTooltipOverlay(GuiGraphics context, int x, int y, String text) {
        context.renderTooltip(Minecraft.getInstance().font, net.minecraft.network.chat.Component.literal(text), x, y);
    }

    private void renderTextureOverlay(GuiGraphics context, int x, int y, ResourceLocation texture, int color, float opacity, float multiplier) {
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
        } catch (IOException e) {
            DebugLogger.debug("Fabric overlay texture size fallback: texture={} error={}", texture, e.toString());
        }
        return TextureSize.DEFAULT;
    }

    private void renderColorOverlay(GuiGraphics context, int x, int y, int color, float opacity, float multiplier) {
        context.fill(x, y, x + 16, y + 16, getColorArgb(color, opacity, multiplier * 0.3f));
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

    private record TextureSize(int width, int height) {
        private static final TextureSize DEFAULT = new TextureSize(16, 16);
    }
}
