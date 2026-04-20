
package mycraft.yuyears.neofavoriteitems.render;

import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;

public abstract class OverlayRenderer {
    protected static final String CLASSIC_TEXTURE_PATH = "textures/classic.png";
    protected static final String BORDER_TEXTURE_PATH = "textures/border.png";
    protected static final String FRAMEWORK_TEXTURE_PATH = "textures/framework.png";
    protected static final String HIGHLIGHT_TEXTURE_PATH = "textures/highlight.png";
    protected static final String BRACKETS_TEXTURE_PATH = "textures/brackets.png";
    protected static final String LOCK_TEXTURE_PATH = "textures/lock.png";
    protected static final String MARK_TEXTURE_PATH = "textures/mark.png";
    protected static final String TAG_TEXTURE_PATH = "textures/tag.png";
    protected static final String STAR_TEXTURE_PATH = "textures/star.png";
    
    protected final FavoritesManager favoritesManager;
    protected final ConfigManager configManager;

    public OverlayRenderer() {
        this.favoritesManager = FavoritesManager.getInstance();
        this.configManager = ConfigManager.getInstance();
    }

    protected NeoFavoriteItemsConfig.OverlayStyle getOverlayStyle(LogicalSlotIndex slotIndex, boolean isHoldingBypassKey) {
        NeoFavoriteItemsConfig config = configManager.getConfig();
        return isHoldingBypassKey ? config.overlay.holdingKeyLockedStyle : config.overlay.lockedStyle;
    }

    protected int getLockedOverlayColor() {
        return configManager.getConfig().overlay.lockedOverlayColor;
    }

    protected float getLockedOverlayOpacity() {
        return configManager.getConfig().overlay.lockedOverlayOpacity;
    }

    protected int getLockableHighlightColor() {
        return configManager.getConfig().overlay.lockableHighlightColor;
    }

    protected float getLockableHighlightOpacity() {
        return configManager.getConfig().overlay.lockableHighlightOpacity;
    }

    protected int getUnlockableHighlightColor() {
        return configManager.getConfig().overlay.unlockableHighlightColor;
    }

    protected float getUnlockableHighlightOpacity() {
        return configManager.getConfig().overlay.unlockableHighlightOpacity;
    }

    protected float getBypassOverlayOpacityMultiplier() {
        return configManager.getConfig().overlay.bypassOverlayOpacityMultiplier;
    }

    protected boolean shouldRenderLockedOverlayInFront() {
        return configManager.getConfig().overlay.renderLockedOverlayInFront;
    }

    protected boolean shouldRenderLockableHighlightInFront() {
        return configManager.getConfig().overlay.renderLockableHighlightInFront;
    }

    protected boolean shouldRenderUnlockableHighlightInFront() {
        return configManager.getConfig().overlay.renderUnlockableHighlightInFront;
    }

    protected float getColorRed(int color) {
        return ((color >> 16) & 0xFF) / 255.0f;
    }

    protected float getColorGreen(int color) {
        return ((color >> 8) & 0xFF) / 255.0f;
    }

    protected float getColorBlue(int color) {
        return (color & 0xFF) / 255.0f;
    }

    protected float getColorAlpha(int color, float opacity, float multiplier) {
        float configuredAlpha = (color & 0xFF000000) == 0
            ? 1.0f
            : ((color >>> 24) & 0xFF) / 255.0f;
        return clamp01(configuredAlpha * opacity * multiplier);
    }

    protected int getColorArgb(int color, float opacity, float multiplier) {
        int alpha = Math.round(getColorAlpha(color, opacity, multiplier) * 255.0f);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    protected NeoFavoriteItemsConfig.OverlayStyle getHighlightStyle() {
        return configManager.getConfig().overlay.highlightStyle;
    }

    protected boolean isLockableSlot(LogicalSlotIndex slotIndex, boolean hasItem) {
        return hasItem || configManager.getConfig().general.lockEmptySlots;
    }

    private float clamp01(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        return Math.min(value, 1.0f);
    }

    protected boolean shouldRenderOverlay(LogicalSlotIndex slotIndex) {
        return favoritesManager.isSlotFavorite(slotIndex);
    }
}
