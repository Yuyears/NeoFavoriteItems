
package mycraft.yuyears.newitemfavorites;

public class NewItemFavoritesConfig {
    public static class General {
        public boolean lockEmptySlots = false;
        public boolean autoUnlockEmptySlots = true;
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
        public SlotMoveBehavior moveBehavior = SlotMoveBehavior.FOLLOW_ITEM;
    }

    public static enum OverlayStyle {
        LOCK_ICON,
        BORDER_GLOW,
        COLOR_OVERLAY,
        CHECKMARK,
        STAR
    }

    public static class Overlay {
        public OverlayStyle lockedStyle = OverlayStyle.LOCK_ICON;
        public OverlayStyle unlockedStyle = OverlayStyle.STAR;
        public OverlayStyle holdingKeyLockedStyle = OverlayStyle.BORDER_GLOW;
        public OverlayStyle holdingKeyUnlockedStyle = OverlayStyle.COLOR_OVERLAY;
        public int overlayColor = 0xFFFFD700;
        public float overlayOpacity = 0.7f;
    }

    public static class Feedback {
        public boolean showVisualFeedback = true;
        public boolean playSoundFeedback = true;
        public String feedbackSound = "minecraft:block.note_block.hat";
        public float feedbackVolume = 0.5f;
        public float feedbackPitch = 1.0f;
    }

    public static class Keybindings {
        public String toggleFavoriteKey = "key.keyboard.f";
        public String bypassLockKey = "key.keyboard.left.control";
    }

    public General general = new General();
    public LockBehavior lockBehavior = new LockBehavior();
    public SlotBehavior slotBehavior = new SlotBehavior();
    public Overlay overlay = new Overlay();
    public Feedback feedback = new Feedback();
    public Keybindings keybindings = new Keybindings();

    public NewItemFavoritesConfig() {}
}
