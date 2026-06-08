package mycraft.yuyears.neofavoriteitems.forge;

import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

final class ForgeFavoriteItemsConfig {
    static final ForgeConfigSpec COMMON_SPEC;
    static final ForgeConfigSpec CLIENT_SPEC;

    private static final ForgeConfigSpec.BooleanValue AUTO_UNLOCK_EMPTY_SLOTS;
    private static final ForgeConfigSpec.BooleanValue LOCK_EMPTY_SLOTS;
    private static final ForgeConfigSpec.BooleanValue ALLOW_ITEMS_INTO_LOCKED_EMPTY_SLOTS;
    private static final ForgeConfigSpec.BooleanValue PREVENT_CLICK;
    private static final ForgeConfigSpec.BooleanValue PREVENT_DROP;
    private static final ForgeConfigSpec.BooleanValue PREVENT_QUICK_MOVE;
    private static final ForgeConfigSpec.BooleanValue PREVENT_SHIFT_CLICK;
    private static final ForgeConfigSpec.BooleanValue PREVENT_DRAG;
    private static final ForgeConfigSpec.BooleanValue PREVENT_SWAP;
    private static final ForgeConfigSpec.BooleanValue ALLOW_BYPASS_WITH_KEY;
    private static final ForgeConfigSpec.EnumValue<NeoFavoriteItemsConfig.SlotMoveBehavior> MOVE_BEHAVIOR;
    private static final ForgeConfigSpec.BooleanValue PRESERVE_LOCKED_SLOT_CONTENTS;
    private static final ForgeConfigSpec.BooleanValue DEBUG_ENABLED;

    private static final ForgeConfigSpec.EnumValue<NeoFavoriteItemsConfig.OverlayStyle> LOCKED_STYLE;
    private static final ForgeConfigSpec.EnumValue<NeoFavoriteItemsConfig.OverlayStyle> HOLDING_KEY_LOCKED_STYLE;
    private static final ForgeConfigSpec.EnumValue<NeoFavoriteItemsConfig.OverlayStyle> HIGHLIGHT_STYLE;
    private static final ForgeConfigSpec.ConfigValue<String> LOCKED_OVERLAY_COLOR;
    private static final ForgeConfigSpec.DoubleValue LOCKED_OVERLAY_OPACITY;
    private static final ForgeConfigSpec.ConfigValue<String> LOCKABLE_HIGHLIGHT_COLOR;
    private static final ForgeConfigSpec.DoubleValue LOCKABLE_HIGHLIGHT_OPACITY;
    private static final ForgeConfigSpec.ConfigValue<String> UNLOCKABLE_HIGHLIGHT_COLOR;
    private static final ForgeConfigSpec.DoubleValue UNLOCKABLE_HIGHLIGHT_OPACITY;
    private static final ForgeConfigSpec.DoubleValue COLOR_OVERLAY_OPACITY;
    private static final ForgeConfigSpec.DoubleValue BYPASS_OVERLAY_OPACITY_MULTIPLIER;
    private static final ForgeConfigSpec.BooleanValue RENDER_LOCKED_OVERLAY_IN_FRONT;
    private static final ForgeConfigSpec.BooleanValue RENDER_LOCKABLE_HIGHLIGHT_IN_FRONT;
    private static final ForgeConfigSpec.BooleanValue RENDER_UNLOCKABLE_HIGHLIGHT_IN_FRONT;
    private static final ForgeConfigSpec.BooleanValue SHOW_VISUAL_FEEDBACK;
    private static final ForgeConfigSpec.BooleanValue PLAY_SOUND_FEEDBACK;
    private static final ForgeConfigSpec.ConfigValue<String> FEEDBACK_SOUND;
    private static final ForgeConfigSpec.DoubleValue FEEDBACK_VOLUME;
    private static final ForgeConfigSpec.DoubleValue FEEDBACK_PITCH;

    static {
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();

        commonBuilder.push("general");
        AUTO_UNLOCK_EMPTY_SLOTS = commonBuilder.comment("Whether to automatically unlock slots when they become empty.")
            .define("autoUnlockEmptySlots", false);
        LOCK_EMPTY_SLOTS = commonBuilder.comment("Whether to allow locking empty slots.")
            .define("lockEmptySlots", true);
        ALLOW_ITEMS_INTO_LOCKED_EMPTY_SLOTS = commonBuilder.comment("Whether to allow items to be placed into locked empty slots.")
            .define("allowItemsIntoLockedEmptySlots", false);
        commonBuilder.pop();

        commonBuilder.push("lockBehavior");
        PREVENT_CLICK = commonBuilder.define("preventClick", true);
        PREVENT_DROP = commonBuilder.define("preventDrop", true);
        PREVENT_QUICK_MOVE = commonBuilder.define("preventQuickMove", true);
        PREVENT_SHIFT_CLICK = commonBuilder.define("preventShiftClick", true);
        PREVENT_DRAG = commonBuilder.define("preventDrag", true);
        PREVENT_SWAP = commonBuilder.define("preventSwap", true);
        ALLOW_BYPASS_WITH_KEY = commonBuilder.define("allowBypassWithKey", true);
        commonBuilder.pop();

        commonBuilder.push("slotBehavior");
        MOVE_BEHAVIOR = commonBuilder.defineEnum("moveBehavior", NeoFavoriteItemsConfig.SlotMoveBehavior.STAY_AT_POSITION);
        commonBuilder.pop();

        commonBuilder.push("deathBehavior");
        PRESERVE_LOCKED_SLOT_CONTENTS = commonBuilder.comment("Whether locked slot contents survive death even when keepInventory is false.")
            .define("preserveLockedSlotContents", false);
        commonBuilder.pop();

        commonBuilder.push("debug");
        DEBUG_ENABLED = commonBuilder.define("enabled", false);
        commonBuilder.pop();

        COMMON_SPEC = commonBuilder.build();

        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();

        clientBuilder.push("overlay");
        LOCKED_STYLE = clientBuilder.defineEnum("lockedStyle", NeoFavoriteItemsConfig.OverlayStyle.MARK);
        HOLDING_KEY_LOCKED_STYLE = clientBuilder.defineEnum("holdingKeyLockedStyle", NeoFavoriteItemsConfig.OverlayStyle.MARK);
        HIGHLIGHT_STYLE = clientBuilder.defineEnum("highlightStyle", NeoFavoriteItemsConfig.OverlayStyle.BORDER);
        LOCKED_OVERLAY_COLOR = clientBuilder.comment("Color as rgba(...), rgb(...), #RRGGBB, #RRGGBBAA, 0xAARRGGBB, or luv(...).")
            .define("lockedOverlayColor", "#FF413CFA");
        LOCKED_OVERLAY_OPACITY = clientBuilder.defineInRange("lockedOverlayOpacity", 0.7d, 0.0d, 1.0d);
        LOCKABLE_HIGHLIGHT_COLOR = clientBuilder.define("lockableHighlightColor", "#23E600C8");
        LOCKABLE_HIGHLIGHT_OPACITY = clientBuilder.defineInRange("lockableHighlightOpacity", 0.55d, 0.0d, 1.0d);
        UNLOCKABLE_HIGHLIGHT_COLOR = clientBuilder.define("unlockableHighlightColor", "#FFC335B4");
        UNLOCKABLE_HIGHLIGHT_OPACITY = clientBuilder.defineInRange("unlockableHighlightOpacity", 0.65d, 0.0d, 1.0d);
        COLOR_OVERLAY_OPACITY = clientBuilder.defineInRange("colorOverlayOpacity", 0.35d, 0.0d, 1.0d);
        BYPASS_OVERLAY_OPACITY_MULTIPLIER = clientBuilder.defineInRange("bypassOverlayOpacityMultiplier", 0.35d, 0.0d, 1.0d);
        RENDER_LOCKED_OVERLAY_IN_FRONT = clientBuilder.define("renderLockedOverlayInFront", true);
        RENDER_LOCKABLE_HIGHLIGHT_IN_FRONT = clientBuilder.define("renderLockableHighlightInFront", true);
        RENDER_UNLOCKABLE_HIGHLIGHT_IN_FRONT = clientBuilder.define("renderUnlockableHighlightInFront", true);
        clientBuilder.pop();

        clientBuilder.push("feedback");
        SHOW_VISUAL_FEEDBACK = clientBuilder.define("showVisualFeedback", true);
        PLAY_SOUND_FEEDBACK = clientBuilder.define("playSoundFeedback", true);
        FEEDBACK_SOUND = clientBuilder.define("feedbackSound", "minecraft:block.note_block.hat");
        FEEDBACK_VOLUME = clientBuilder.defineInRange("feedbackVolume", 0.5d, 0.0d, 1.0d);
        FEEDBACK_PITCH = clientBuilder.defineInRange("feedbackPitch", 1.0d, 0.0d, 2.0d);
        clientBuilder.pop();

        CLIENT_SPEC = clientBuilder.build();
    }

    private ForgeFavoriteItemsConfig() {}

    static void onModConfig(ModConfigEvent event) {
        if (!NeoFavoriteItemsMod.MOD_ID.equals(event.getConfig().getModId())) {
            return;
        }
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            syncCommonToCommonConfig();
        } else if (event.getConfig().getType() == ModConfig.Type.CLIENT) {
            syncClientToCommonConfig();
        }
    }

    private static void syncCommonToCommonConfig() {
        NeoFavoriteItemsConfig config = ConfigManager.getInstance().getConfig();
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
        config.deathBehavior.preserveLockedSlotContents = PRESERVE_LOCKED_SLOT_CONTENTS.get();
        config.debug.enabled = DEBUG_ENABLED.get();
    }

    private static void syncClientToCommonConfig() {
        NeoFavoriteItemsConfig config = ConfigManager.getInstance().getConfig();
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
    }

    private static int parseColor(String value, String fallback) {
        try {
            return ConfigManager.parseColorValue(value);
        } catch (RuntimeException e) {
            DebugLogger.warn("Invalid Forge config color '{}'; using default {}", value, fallback);
            return ConfigManager.parseColorValue(fallback);
        }
    }
}
