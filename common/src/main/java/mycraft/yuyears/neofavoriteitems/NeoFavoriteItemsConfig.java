
package mycraft.yuyears.neofavoriteitems;

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
        public int lockedOverlayColor = 0xFAFF413C;
        public float lockedOverlayOpacity = 0.7f;
        public int lockableHighlightColor = 0xC823E600;
        public float lockableHighlightOpacity = 0.55f;
        public int unlockableHighlightColor = 0xB4FFC335;
        public float unlockableHighlightOpacity = 0.65f;
        public float colorOverlayOpacity = 0.35f;
        public float bypassOverlayOpacityMultiplier = 0.35f;
        public boolean renderLockedOverlayInFront = true;
        public boolean renderLockableHighlightInFront = true;
        public boolean renderUnlockableHighlightInFront = true;
    }

    public static class Feedback {
        public boolean showVisualFeedback = true;
        public boolean playSoundFeedback = true;
        public String feedbackSound = "minecraft:block.note_block.hat";
        public float feedbackVolume = 0.5f;
        public float feedbackPitch = 1.0f;
    }

    public static class Debug {
        public boolean enabled = false;
    }

    public General general = new General();
    public LockBehavior lockBehavior = new LockBehavior();
    public SlotBehavior slotBehavior = new SlotBehavior();
    public Overlay overlay = new Overlay();
    public Feedback feedback = new Feedback();
    public Debug debug = new Debug();

    public NeoFavoriteItemsConfig() {}
}
