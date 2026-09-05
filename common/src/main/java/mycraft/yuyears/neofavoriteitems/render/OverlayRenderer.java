
package mycraft.yuyears.neofavoriteitems.render;

import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.application.FavoriteLockRules;
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
    private final OverlayProfileCompiler profileCompiler = new OverlayProfileCompiler();

    public OverlayRenderer() {
        this.favoritesManager = FavoritesManager.getInstance();
        this.configManager = ConfigManager.getInstance();
    }

    protected OverlayProfile lockedOverlayProfile(LogicalSlotIndex slotIndex, boolean isHoldingBypassKey) {
        NeoFavoriteItemsConfig.Overlay overlay = configManager.getConfig().overlay;
        return profileCompiler.resolve(
            overlay,
            isHoldingBypassKey ? OverlayMode.BYPASS_LOCKED : OverlayMode.LOCKED,
            configManager.getRevision()
        );
    }

    protected OverlayProfile highlightOverlayProfile(LogicalSlotIndex slotIndex, boolean hasItem) {
        NeoFavoriteItemsConfig.Overlay overlay = configManager.getConfig().overlay;
        boolean unlockable = favoritesManager.isSlotFavorite(slotIndex);
        return profileCompiler.resolve(
            overlay,
            unlockable ? OverlayMode.UNLOCKABLE : OverlayMode.LOCKABLE,
            configManager.getRevision()
        );
    }

    protected boolean isLockableSlot(LogicalSlotIndex slotIndex, boolean hasItem) {
        return hasItem || FavoriteLockRules.canKeepEmptySlotLocked(configManager.getConfig());
    }

    protected boolean shouldRenderOverlay(LogicalSlotIndex slotIndex) {
        return favoritesManager.isSlotFavorite(slotIndex);
    }

    protected OverlayProfile resolveProfile(SlotRenderTarget target, boolean isHoldingBypassKey,
                                            boolean isHoldingLockOperationKey) {
        if (isHoldingLockOperationKey && isLockableSlot(target.logicalSlot(), target.hasItem())) {
            return highlightOverlayProfile(target.logicalSlot(), target.hasItem());
        }
        if (shouldRenderOverlay(target.logicalSlot())) {
            return lockedOverlayProfile(target.logicalSlot(), isHoldingBypassKey);
        }
        return null;
    }

    /** HUD has no lock-operation preview; CTRL only selects bypass profile. */
    protected OverlayProfile resolveHudProfile(SlotRenderTarget target, boolean isHoldingBypassKey) {
        return shouldRenderOverlay(target.logicalSlot())
            ? lockedOverlayProfile(target.logicalSlot(), isHoldingBypassKey)
            : null;
    }

}
