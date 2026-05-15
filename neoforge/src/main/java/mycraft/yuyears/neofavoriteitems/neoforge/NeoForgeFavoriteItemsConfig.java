package mycraft.yuyears.neofavoriteitems.neoforge;

import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

final class NeoForgeFavoriteItemsConfig {
    static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue AUTO_UNLOCK_EMPTY_SLOTS;
    private static final ModConfigSpec.BooleanValue LOCK_EMPTY_SLOTS;
    private static final ModConfigSpec.BooleanValue ALLOW_ITEMS_INTO_LOCKED_EMPTY_SLOTS;
    private static final ModConfigSpec.BooleanValue PREVENT_CLICK;
    private static final ModConfigSpec.BooleanValue PREVENT_DROP;
    private static final ModConfigSpec.BooleanValue PREVENT_QUICK_MOVE;
    private static final ModConfigSpec.BooleanValue PREVENT_SHIFT_CLICK;
    private static final ModConfigSpec.BooleanValue PREVENT_DRAG;
    private static final ModConfigSpec.BooleanValue PREVENT_SWAP;
    private static final ModConfigSpec.BooleanValue ALLOW_BYPASS_WITH_KEY;
    private static final ModConfigSpec.EnumValue<NeoFavoriteItemsConfig.SlotMoveBehavior> MOVE_BEHAVIOR;
    private static final ModConfigSpec.EnumValue<NeoFavoriteItemsConfig.OverlayStyle> LOCKED_STYLE;
    private static final ModConfigSpec.EnumValue<NeoFavoriteItemsConfig.OverlayStyle> HOLDING_KEY_LOCKED_STYLE;
    private static final ModConfigSpec.EnumValue<NeoFavoriteItemsConfig.OverlayStyle> HIGHLIGHT_STYLE;
    private static final ModConfigSpec.ConfigValue<String> LOCKED_OVERLAY_COLOR;
    private static final ModConfigSpec.DoubleValue LOCKED_OVERLAY_OPACITY;
    private static final ModConfigSpec.ConfigValue<String> LOCKABLE_HIGHLIGHT_COLOR;
    private static final ModConfigSpec.DoubleValue LOCKABLE_HIGHLIGHT_OPACITY;
    private static final ModConfigSpec.ConfigValue<String> UNLOCKABLE_HIGHLIGHT_COLOR;
    private static final ModConfigSpec.DoubleValue UNLOCKABLE_HIGHLIGHT_OPACITY;
    private static final ModConfigSpec.DoubleValue COLOR_OVERLAY_OPACITY;
    private static final ModConfigSpec.DoubleValue BYPASS_OVERLAY_OPACITY_MULTIPLIER;
    private static final ModConfigSpec.BooleanValue RENDER_LOCKED_OVERLAY_IN_FRONT;
    private static final ModConfigSpec.BooleanValue RENDER_LOCKABLE_HIGHLIGHT_IN_FRONT;
    private static final ModConfigSpec.BooleanValue RENDER_UNLOCKABLE_HIGHLIGHT_IN_FRONT;
    private static final ModConfigSpec.BooleanValue SHOW_VISUAL_FEEDBACK;
    private static final ModConfigSpec.BooleanValue PLAY_SOUND_FEEDBACK;
    private static final ModConfigSpec.ConfigValue<String> FEEDBACK_SOUND;
    private static final ModConfigSpec.DoubleValue FEEDBACK_VOLUME;
    private static final ModConfigSpec.DoubleValue FEEDBACK_PITCH;
    private static final ModConfigSpec.BooleanValue DEBUG_ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        AUTO_UNLOCK_EMPTY_SLOTS = builder.comment("Whether to automatically unlock slots when they become empty.")
            .define("autoUnlockEmptySlots", false);
        LOCK_EMPTY_SLOTS = builder.comment("Whether to allow locking empty slots.")
            .define("lockEmptySlots", true);
        ALLOW_ITEMS_INTO_LOCKED_EMPTY_SLOTS = builder.comment("Whether to allow items to be placed into locked empty slots.")
            .define("allowItemsIntoLockedEmptySlots", false);
        builder.pop();

        builder.push("lockBehavior");
        PREVENT_CLICK = builder.define("preventClick", true);
        PREVENT_DROP = builder.define("preventDrop", true);
        PREVENT_QUICK_MOVE = builder.define("preventQuickMove", true);
        PREVENT_SHIFT_CLICK = builder.define("preventShiftClick", true);
        PREVENT_DRAG = builder.define("preventDrag", true);
        PREVENT_SWAP = builder.define("preventSwap", true);
        ALLOW_BYPASS_WITH_KEY = builder.define("allowBypassWithKey", true);
        builder.pop();

        builder.push("slotBehavior");
        MOVE_BEHAVIOR = builder.defineEnum("moveBehavior", NeoFavoriteItemsConfig.SlotMoveBehavior.STAY_AT_POSITION);
        builder.pop();

        builder.push("overlay");
        LOCKED_STYLE = builder.defineEnum("lockedStyle", NeoFavoriteItemsConfig.OverlayStyle.MARK);
        HOLDING_KEY_LOCKED_STYLE = builder.defineEnum("holdingKeyLockedStyle", NeoFavoriteItemsConfig.OverlayStyle.MARK);
        HIGHLIGHT_STYLE = builder.defineEnum("highlightStyle", NeoFavoriteItemsConfig.OverlayStyle.BORDER);
        LOCKED_OVERLAY_COLOR = builder.comment("Color as rgba(...), rgb(...), #RRGGBB, #RRGGBBAA, 0xAARRGGBB, or luv(...).")
            .define("lockedOverlayColor", "#FF413CFA");
        LOCKED_OVERLAY_OPACITY = builder.defineInRange("lockedOverlayOpacity", 0.7d, 0.0d, 1.0d);
        LOCKABLE_HIGHLIGHT_COLOR = builder.define("lockableHighlightColor", "#23E600C8");
        LOCKABLE_HIGHLIGHT_OPACITY = builder.defineInRange("lockableHighlightOpacity", 0.55d, 0.0d, 1.0d);
        UNLOCKABLE_HIGHLIGHT_COLOR = builder.define("unlockableHighlightColor", "#FFC335B4");
        UNLOCKABLE_HIGHLIGHT_OPACITY = builder.defineInRange("unlockableHighlightOpacity", 0.65d, 0.0d, 1.0d);
        COLOR_OVERLAY_OPACITY = builder.defineInRange("colorOverlayOpacity", 0.35d, 0.0d, 1.0d);
        BYPASS_OVERLAY_OPACITY_MULTIPLIER = builder.defineInRange("bypassOverlayOpacityMultiplier", 0.35d, 0.0d, 1.0d);
        RENDER_LOCKED_OVERLAY_IN_FRONT = builder.define("renderLockedOverlayInFront", true);
        RENDER_LOCKABLE_HIGHLIGHT_IN_FRONT = builder.define("renderLockableHighlightInFront", true);
        RENDER_UNLOCKABLE_HIGHLIGHT_IN_FRONT = builder.define("renderUnlockableHighlightInFront", true);
        builder.pop();

        builder.push("feedback");
        SHOW_VISUAL_FEEDBACK = builder.define("showVisualFeedback", true);
        PLAY_SOUND_FEEDBACK = builder.define("playSoundFeedback", true);
        FEEDBACK_SOUND = builder.define("feedbackSound", "minecraft:block.note_block.hat");
        FEEDBACK_VOLUME = builder.defineInRange("feedbackVolume", 0.5d, 0.0d, 1.0d);
        FEEDBACK_PITCH = builder.defineInRange("feedbackPitch", 1.0d, 0.0d, 2.0d);
        builder.pop();

        builder.push("debug");
        DEBUG_ENABLED = builder.define("enabled", false);
        builder.pop();

        SPEC = builder.build();
    }

    private NeoForgeFavoriteItemsConfig() {}

    static void onModConfig(ModConfigEvent event) {
        if (!NeoFavoriteItemsMod.MOD_ID.equals(event.getConfig().getModId())
            || event.getConfig().getType() != ModConfig.Type.COMMON) {
            return;
        }
        syncToCommonConfig();
    }

    private static void syncToCommonConfig() {
        NeoFavoriteItemsConfig config = new NeoFavoriteItemsConfig();
        config.general.autoUnlockEmptySlots = AUTO_UNLOCK_EMPTY_SLOTS.get();
        config.general.lockEmptySlots = LOCK_EMPTY_SLOTS.get();
        config.general.allowItemsIntoLockedEmptySlots = ALLOW_ITEMS_INTO_LOCKED_EMPTY_SLOTS.get();
        config.lockBehavior.preventClick = PREVENT_CLICK.get();
        config.lockBehavior.preventDrop = PREVENT_DROP.get();
        config.lockBehavior.preventQuickMove = PREVENT_QUICK_MOVE.get();
        config.lockBehavior.preventShiftClick = PREVENT_SHIFT_CLICK.get();
        config.lockBehavior.preventDrag = PREVENT_DRAG.get();
        config.lockBehavior.preventSwap = PREVENT_SWAP.get();
        config.lockBehavior.allowBypassWithKey = ALLOW_BYPASS_WITH_KEY.get();
        config.slotBehavior.moveBehavior = MOVE_BEHAVIOR.get();
        config.overlay.lockedStyle = LOCKED_STYLE.get();
        config.overlay.holdingKeyLockedStyle = HOLDING_KEY_LOCKED_STYLE.get();
        config.overlay.highlightStyle = HIGHLIGHT_STYLE.get();
        config.overlay.lockedOverlayColor = parseColor(LOCKED_OVERLAY_COLOR.get(), "#FF413CFA");
        config.overlay.lockedOverlayOpacity = LOCKED_OVERLAY_OPACITY.get().floatValue();
        config.overlay.lockableHighlightColor = parseColor(LOCKABLE_HIGHLIGHT_COLOR.get(), "#23E600C8");
        config.overlay.lockableHighlightOpacity = LOCKABLE_HIGHLIGHT_OPACITY.get().floatValue();
        config.overlay.unlockableHighlightColor = parseColor(UNLOCKABLE_HIGHLIGHT_COLOR.get(), "#FFC335B4");
        config.overlay.unlockableHighlightOpacity = UNLOCKABLE_HIGHLIGHT_OPACITY.get().floatValue();
        config.overlay.colorOverlayOpacity = COLOR_OVERLAY_OPACITY.get().floatValue();
        config.overlay.bypassOverlayOpacityMultiplier = BYPASS_OVERLAY_OPACITY_MULTIPLIER.get().floatValue();
        config.overlay.renderLockedOverlayInFront = RENDER_LOCKED_OVERLAY_IN_FRONT.get();
        config.overlay.renderLockableHighlightInFront = RENDER_LOCKABLE_HIGHLIGHT_IN_FRONT.get();
        config.overlay.renderUnlockableHighlightInFront = RENDER_UNLOCKABLE_HIGHLIGHT_IN_FRONT.get();
        config.feedback.showVisualFeedback = SHOW_VISUAL_FEEDBACK.get();
        config.feedback.playSoundFeedback = PLAY_SOUND_FEEDBACK.get();
        config.feedback.feedbackSound = FEEDBACK_SOUND.get();
        config.feedback.feedbackVolume = FEEDBACK_VOLUME.get().floatValue();
        config.feedback.feedbackPitch = FEEDBACK_PITCH.get().floatValue();
        config.debug.enabled = DEBUG_ENABLED.get();
        ConfigManager.getInstance().applyPlatformConfig(config);
    }

    private static int parseColor(String value, String fallback) {
        try {
            return ConfigManager.parseColorValue(value);
        } catch (RuntimeException e) {
            DebugLogger.warn("Invalid NeoForge config color '{}'; using default {}", value, fallback);
            return ConfigManager.parseColorValue(fallback);
        }
    }
}
