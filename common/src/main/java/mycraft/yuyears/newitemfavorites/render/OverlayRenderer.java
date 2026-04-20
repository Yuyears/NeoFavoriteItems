
package mycraft.yuyears.newitemfavorites.render;

import mycraft.yuyears.newitemfavorites.ConfigManager;
import mycraft.yuyears.newitemfavorites.FavoritesManager;
import mycraft.yuyears.newitemfavorites.NewItemFavoritesConfig;
import mycraft.yuyears.newitemfavorites.domain.LogicalSlotIndex;

public abstract class OverlayRenderer {
    protected static final String LOCK_ICON_PATH = "textures/lock.png";
    protected static final String STAR_ICON_PATH = "textures/star.png";
    protected static final String CHECKMARK_ICON_PATH = "textures/checkmark.png";
    
    protected final FavoritesManager favoritesManager;
    protected final ConfigManager configManager;

    public OverlayRenderer() {
        this.favoritesManager = FavoritesManager.getInstance();
        this.configManager = ConfigManager.getInstance();
    }

    protected NewItemFavoritesConfig.OverlayStyle getOverlayStyle(LogicalSlotIndex slotIndex, boolean isHoldingBypassKey) {
        boolean isFavorite = favoritesManager.isSlotFavorite(slotIndex);
        NewItemFavoritesConfig config = configManager.getConfig();
        
        if (isHoldingBypassKey) {
            return isFavorite ? config.overlay.holdingKeyLockedStyle : config.overlay.holdingKeyUnlockedStyle;
        } else {
            return isFavorite ? config.overlay.lockedStyle : config.overlay.unlockedStyle;
        }
    }

    protected int getOverlayColor() {
        return configManager.getConfig().overlay.overlayColor;
    }

    protected float getOverlayOpacity() {
        return configManager.getConfig().overlay.overlayOpacity;
    }

    protected boolean shouldRenderOverlay(LogicalSlotIndex slotIndex) {
        return favoritesManager.isSlotFavorite(slotIndex);
    }
}
