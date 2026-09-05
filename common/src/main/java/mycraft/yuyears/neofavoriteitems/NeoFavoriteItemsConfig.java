
package mycraft.yuyears.neofavoriteitems;

import java.util.List;
import mycraft.yuyears.neofavoriteitems.render.OverlayLayerList;
import mycraft.yuyears.neofavoriteitems.render.OverlayMode;

public class NeoFavoriteItemsConfig {
    public static class General {
        public boolean lockEmptySlots = true;
        public boolean autoUnlockEmptySlots = false;
        public boolean allowItemsIntoLockedEmptySlots = false;
    }

    public static class LockBehavior {
        public boolean preventClick = true;
        public boolean preventDrop = true;
        public boolean preventQuickMove = true;
        public boolean preventShiftClick = true;
        public boolean preventDrag = true;
        public boolean preventSwap = true;
        public boolean allowBypassWithKey = true;
    }

    public static enum SlotMoveBehavior {
        FOLLOW_ITEM,
        STAY_AT_POSITION
    }

    public static class SlotBehavior {
        public SlotMoveBehavior moveBehavior = SlotMoveBehavior.STAY_AT_POSITION;
    }

    public static class DeathBehavior {
        public boolean preserveLockedSlotContents = false;
    }

    public static enum OverlayStyle {
        BORDER,
        CLASSIC,
        FRAMEWORK,
        HIGHLIGHT,
        BRACKETS,
        LOCK,
        MARK,
        TAG,
        STAR,
        COLOR_OVERLAY
    }

    public static class Overlay {
        public OverlayStyle lockedStyle = OverlayStyle.MARK;
        public OverlayStyle holdingKeyLockedStyle = OverlayStyle.MARK;
        public OverlayStyle highlightStyle = OverlayStyle.BORDER;
        public int lockedOverlayColor = OverlayProfileConfig.DEFAULT_LOCKED_COLOR;
        public float lockedOverlayOpacity = OverlayProfileConfig.DEFAULT_LOCKED_OPACITY;
        public int lockableHighlightColor = OverlayProfileConfig.DEFAULT_LOCKABLE_COLOR;
        public float lockableHighlightOpacity = 1.0f;
        public int unlockableHighlightColor = OverlayProfileConfig.DEFAULT_UNLOCKABLE_COLOR;
        public float unlockableHighlightOpacity = 1.0f;
        public float colorOverlayOpacity = 0.35f;
        public float bypassOverlayOpacityMultiplier = 0.35f;
        public boolean renderLockedOverlayInFront = true;
        public boolean renderLockableHighlightInFront = true;
        public boolean renderUnlockableHighlightInFront = true;
        public OverlayProfileConfig locked = OverlayProfileConfig.defaultLocked();
        public OverlayProfileConfig bypass = OverlayProfileConfig.defaultBypass();
        public OverlayProfileConfig lockable = OverlayProfileConfig.defaultLockable();
        public OverlayProfileConfig unlockable = OverlayProfileConfig.defaultUnlockable();
        /** New bounded model; legacy fields remain until codec/render migration. */
        public List<OverlayProfileConfig> lockedLayers = standardLayers(OverlayProfileConfig::defaultLocked);
        public List<OverlayProfileConfig> bypassLayers = standardLayers(OverlayProfileConfig::defaultBypass);
        public List<OverlayProfileConfig> lockableLayers = standardLayers(OverlayProfileConfig::defaultLockable);
        public List<OverlayProfileConfig> unlockableLayers = standardLayers(OverlayProfileConfig::defaultUnlockable);

        private static List<OverlayProfileConfig> standardLayers(java.util.function.Supplier<OverlayProfileConfig> factory) {
            return List.of(factory.get());
        }

        public List<OverlayProfileConfig> layers(OverlayMode mode) {
            return switch (mode) {
                case LOCKED -> OverlayLayerList.normalize(lockedLayers, OverlayProfileConfig::defaultLocked);
                case BYPASS_LOCKED -> OverlayLayerList.normalize(bypassLayers, OverlayProfileConfig::defaultBypass);
                case LOCKABLE -> OverlayLayerList.normalize(lockableLayers, OverlayProfileConfig::defaultLockable);
                case UNLOCKABLE -> OverlayLayerList.normalize(unlockableLayers, OverlayProfileConfig::defaultUnlockable);
            };
        }

        public void syncProfilesFromLegacy() {
            locked.style = lockedStyle;
            locked.materialId = mycraft.yuyears.neofavoriteitems.render.OverlayTextureCatalog.presetId(lockedStyle);
            locked.color = lockedOverlayColor;
            locked.opacity = lockedOverlayOpacity;
            locked.zIndex = renderLockedOverlayInFront
                ? mycraft.yuyears.neofavoriteitems.render.OverlayZIndex.ABOVE_ITEM
                : mycraft.yuyears.neofavoriteitems.render.OverlayZIndex.BELOW_ITEM;
            bypass.style = holdingKeyLockedStyle;
            bypass.materialId = mycraft.yuyears.neofavoriteitems.render.OverlayTextureCatalog.presetId(holdingKeyLockedStyle);
            bypass.color = lockedOverlayColor;
            bypass.opacity = bypassOverlayOpacityMultiplier;
            bypass.opacityBehavior = OverlayProfileConfig.OpacityBehavior.MULTIPLY_LOCKED;
            bypass.zIndex = locked.zIndex;
            lockable.style = highlightStyle;
            lockable.materialId = mycraft.yuyears.neofavoriteitems.render.OverlayTextureCatalog.presetId(highlightStyle);
            lockable.color = lockableHighlightColor;
            lockable.opacity = lockableHighlightOpacity;
            lockable.zIndex = renderLockableHighlightInFront
                ? mycraft.yuyears.neofavoriteitems.render.OverlayZIndex.ABOVE_ITEM
                : mycraft.yuyears.neofavoriteitems.render.OverlayZIndex.BELOW_ITEM;
            unlockable.style = highlightStyle;
            unlockable.materialId = mycraft.yuyears.neofavoriteitems.render.OverlayTextureCatalog.presetId(highlightStyle);
            unlockable.color = unlockableHighlightColor;
            unlockable.opacity = unlockableHighlightOpacity;
            unlockable.zIndex = renderUnlockableHighlightInFront
                ? mycraft.yuyears.neofavoriteitems.render.OverlayZIndex.ABOVE_ITEM
                : mycraft.yuyears.neofavoriteitems.render.OverlayZIndex.BELOW_ITEM;
        }
    }

    public static class Feedback {
        public boolean showVisualFeedback = true;
        public boolean playSoundFeedback = true;
        public String feedbackSound = "minecraft:block.chain.break";
        public float feedbackVolume = 0.25f;
        public float feedbackPitch = 1.5f;
        /** Persisted client UI theme name; parsed defensively by ConfigManager. */
        public String uiTheme = "DEFAULT";
    }

    public static class Debug {
        public boolean enabled = false;
    }

    public General general = new General();
    public LockBehavior lockBehavior = new LockBehavior();
    public SlotBehavior slotBehavior = new SlotBehavior();
    public DeathBehavior deathBehavior = new DeathBehavior();
    public Overlay overlay = new Overlay();
    public Feedback feedback = new Feedback();
    public Debug debug = new Debug();

    public NeoFavoriteItemsConfig() {}
}
