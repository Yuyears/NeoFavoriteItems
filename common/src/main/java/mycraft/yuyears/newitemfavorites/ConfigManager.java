
package mycraft.yuyears.newitemfavorites;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final String CONFIG_COMMENTS = """
        # New Item Favorites Mod Configuration
        # =====================================
        
        [general]
        # Whether to allow locking empty slots
        lockEmptySlots = false
        
        # Whether to automatically unlock slots when they become empty
        autoUnlockEmptySlots = true
        
        # Whether to allow items to be placed into locked empty slots
        allowItemsIntoLockedEmptySlots = false
        
        [lockBehavior]
        # Prevent clicking on locked slots
        preventClick = true
        
        # Prevent dropping items from locked slots
        preventDrop = true
        
        # Prevent quick moving items from locked slots
        preventQuickMove = true
        
        # Prevent shift-clicking items from locked slots
        preventShiftClick = true
        
        # Prevent dragging items over locked slots
        preventDrag = true
        
        # Prevent swapping items with locked slots
        preventSwap = true
        
        # Allow bypassing lock by holding a special key
        allowBypassWithKey = true
        
        [slotBehavior]
        # What happens when a favorite item is moved
        # FOLLOW_ITEM: The favorite status moves with the item
        # STAY_AT_POSITION: The favorite status stays at the slot position
        moveBehavior = "FOLLOW_ITEM"
        
        [overlay]
        # Overlay style for locked slots
        # Options: LOCK_ICON, BORDER_GLOW, COLOR_OVERLAY, CHECKMARK, STAR
        lockedStyle = "LOCK_ICON"
        
        # Overlay style for unlocked (favorite) slots
        # Options: LOCK_ICON, BORDER_GLOW, COLOR_OVERLAY, CHECKMARK, STAR
        unlockedStyle = "STAR"
        
        # Overlay style for locked slots when holding bypass key
        holdingKeyLockedStyle = "BORDER_GLOW"
        
        # Overlay style for unlocked slots when holding bypass key
        holdingKeyUnlockedStyle = "COLOR_OVERLAY"
        
        # Color for overlays (ARGB format)
        overlayColor = 0xFFFFD700
        
        # Opacity for overlays (0.0 - 1.0)
        overlayOpacity = 0.7
        
        [feedback]
        # Show visual feedback when trying to interact with locked slots
        showVisualFeedback = true
        
        # Play sound feedback when trying to interact with locked slots
        playSoundFeedback = true
        
        # Sound to play for feedback
        feedbackSound = "minecraft:block.note_block.hat"
        
        # Volume for feedback sound
        feedbackVolume = 0.5
        
        # Pitch for feedback sound
        feedbackPitch = 1.0
        
        [keybindings]
        # Key to toggle favorite status
        toggleFavoriteKey = "key.keyboard.f"
        
        # Key to bypass lock restrictions
        bypassLockKey = "key.keyboard.left.control"
        """;

    private static ConfigManager instance;
    private NewItemFavoritesConfig config;
    private Path configPath;

    private ConfigManager() {
        this.config = new NewItemFavoritesConfig();
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public void initialize(Path configDir) {
        this.configPath = configDir.resolve("new-item-favorites.toml");
        loadConfig();
    }

    public NewItemFavoritesConfig getConfig() {
        return config;
    }

    public void loadConfig() {
        if (Files.exists(configPath)) {
            try {
                String content = Files.readString(configPath);
                parseConfig(content);
            } catch (IOException e) {
                e.printStackTrace();
                saveDefaultConfig();
            }
        } else {
            saveDefaultConfig();
        }
    }

    private void parseConfig(String content) {
        String[] lines = content.split("\n");
        String currentSection = "";
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            
            if (line.startsWith("[")) {
                currentSection = line.substring(1, line.length() - 1);
                continue;
            }
            
            if (line.contains("=")) {
                String[] parts = line.split("=", 2);
                String key = parts[0].trim();
                String value = parts[1].trim();
                setConfigValue(currentSection, key, value);
            }
        }
    }

    private void setConfigValue(String section, String key, String value) {
        try {
            switch (section) {
                case "general" -> setGeneralValue(key, value);
                case "lockBehavior" -> setLockBehaviorValue(key, value);
                case "slotBehavior" -> setSlotBehaviorValue(key, value);
                case "overlay" -> setOverlayValue(key, value);
                case "feedback" -> setFeedbackValue(key, value);
                case "keybindings" -> setKeybindingValue(key, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setGeneralValue(String key, String value) {
        switch (key) {
            case "lockEmptySlots" -> config.general.lockEmptySlots = Boolean.parseBoolean(value);
            case "autoUnlockEmptySlots" -> config.general.autoUnlockEmptySlots = Boolean.parseBoolean(value);
            case "allowItemsIntoLockedEmptySlots" -> config.general.allowItemsIntoLockedEmptySlots = Boolean.parseBoolean(value);
        }
    }

    private void setLockBehaviorValue(String key, String value) {
        switch (key) {
            case "preventClick" -> config.lockBehavior.preventClick = Boolean.parseBoolean(value);
            case "preventDrop" -> config.lockBehavior.preventDrop = Boolean.parseBoolean(value);
            case "preventQuickMove" -> config.lockBehavior.preventQuickMove = Boolean.parseBoolean(value);
            case "preventShiftClick" -> config.lockBehavior.preventShiftClick = Boolean.parseBoolean(value);
            case "preventDrag" -> config.lockBehavior.preventDrag = Boolean.parseBoolean(value);
            case "preventSwap" -> config.lockBehavior.preventSwap = Boolean.parseBoolean(value);
            case "allowBypassWithKey" -> config.lockBehavior.allowBypassWithKey = Boolean.parseBoolean(value);
        }
    }

    private void setSlotBehaviorValue(String key, String value) {
        if (key.equals("moveBehavior")) {
            String cleanValue = value.replace("\"", "");
            try {
                config.slotBehavior.moveBehavior = NewItemFavoritesConfig.SlotMoveBehavior.valueOf(cleanValue);
            } catch (IllegalArgumentException e) {
                config.slotBehavior.moveBehavior = NewItemFavoritesConfig.SlotMoveBehavior.FOLLOW_ITEM;
            }
        }
    }

    private void setOverlayValue(String key, String value) {
        switch (key) {
            case "lockedStyle" -> config.overlay.lockedStyle = parseOverlayStyle(value);
            case "unlockedStyle" -> config.overlay.unlockedStyle = parseOverlayStyle(value);
            case "holdingKeyLockedStyle" -> config.overlay.holdingKeyLockedStyle = parseOverlayStyle(value);
            case "holdingKeyUnlockedStyle" -> config.overlay.holdingKeyUnlockedStyle = parseOverlayStyle(value);
            case "overlayColor" -> {
                if (value.startsWith("0x")) {
                    config.overlay.overlayColor = (int) Long.parseLong(value.substring(2), 16);
                } else {
                    config.overlay.overlayColor = Integer.parseInt(value);
                }
            }
            case "overlayOpacity" -> config.overlay.overlayOpacity = Float.parseFloat(value);
        }
    }

    private NewItemFavoritesConfig.OverlayStyle parseOverlayStyle(String value) {
        String cleanValue = value.replace("\"", "");
        try {
            return NewItemFavoritesConfig.OverlayStyle.valueOf(cleanValue);
        } catch (IllegalArgumentException e) {
            return NewItemFavoritesConfig.OverlayStyle.LOCK_ICON;
        }
    }

    private void setFeedbackValue(String key, String value) {
        switch (key) {
            case "showVisualFeedback" -> config.feedback.showVisualFeedback = Boolean.parseBoolean(value);
            case "playSoundFeedback" -> config.feedback.playSoundFeedback = Boolean.parseBoolean(value);
            case "feedbackSound" -> config.feedback.feedbackSound = value.replace("\"", "");
            case "feedbackVolume" -> config.feedback.feedbackVolume = Float.parseFloat(value);
            case "feedbackPitch" -> config.feedback.feedbackPitch = Float.parseFloat(value);
        }
    }

    private void setKeybindingValue(String key, String value) {
        switch (key) {
            case "toggleFavoriteKey" -> config.keybindings.toggleFavoriteKey = value.replace("\"", "");
            case "bypassLockKey" -> config.keybindings.bypassLockKey = value.replace("\"", "");
        }
    }

    public void saveDefaultConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, CONFIG_COMMENTS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        saveDefaultConfig();
    }
}
