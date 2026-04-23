
package mycraft.yuyears.neofavoriteitems;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final String CONFIG_COMMENTS = """
        # New Item Favorites Mod Configuration
        # 新物品收藏模组配置
        # =====================================
        
        [general]
        # Whether to allow locking empty slots
        # 是否允许锁定空槽位
        # Parent option for keeping empty slots locked after they become empty
        # 是否允许空槽位保持收藏状态的父级配置
        lockEmptySlots = true
        
        # Whether to automatically unlock slots when they become empty
        # 当槽位变为空时是否自动解锁
        # If true, allowItemsIntoLockedEmptySlots below has no effect because locked empty slots are removed
        # 如果为 true，下方 allowItemsIntoLockedEmptySlots 不生效，因为空槽位会自动解除收藏
        autoUnlockEmptySlots = false
        
        # Whether to allow items to be placed into locked empty slots
        # 是否允许物品放入已锁定的空槽位
        # Child option of lockEmptySlots + autoUnlockEmptySlots=false
        # lockEmptySlots 与 autoUnlockEmptySlots=false 的子级配置
        allowItemsIntoLockedEmptySlots = false
        
        [lockBehavior]
        # Prevent clicking on locked slots
        # 阻止点击已锁定槽位
        preventClick = true
        
        # Prevent dropping items from locked slots
        # 阻止从已锁定槽位丢弃物品
        preventDrop = true
        
        # Prevent quick moving items from locked slots
        # 阻止从已锁定槽位快速移动物品
        preventQuickMove = true
        
        # Prevent shift-clicking items from locked slots
        # 阻止 Shift 点击已锁定槽位
        preventShiftClick = true
        
        # Prevent dragging items over locked slots
        # 阻止拖拽物品经过已锁定槽位
        preventDrag = true
        
        # Prevent swapping items with locked slots
        # 阻止与已锁定槽位交换物品
        preventSwap = true
        
        # Allow bypassing lock by holding the bypass key
        # 是否允许按住旁路键临时绕过锁定限制
        allowBypassWithKey = true
        
        [slotBehavior]
        # What happens when a favorite item is moved
        # 当被锁定/收藏的物品移动时如何处理锁定状态
        # FOLLOW_ITEM: The favorite status moves with the item
        # FOLLOW_ITEM：锁定状态跟随物品移动
        # STAY_AT_POSITION: The favorite status stays at the slot position
        # STAY_AT_POSITION：锁定状态固定在槽位位置
        moveBehavior = "STAY_AT_POSITION"
        
        [overlay]
        # Overlay style for locked slots
        # 已锁定槽位的覆盖层样式
        # Options: BORDER, CLASSIC, FRAMEWORK, HIGHLIGHT, BRACKETS, LOCK, MARK, TAG, STAR, COLOR_OVERLAY
        # 可选值：BORDER, CLASSIC, FRAMEWORK, HIGHLIGHT, BRACKETS, LOCK, MARK, TAG, STAR, COLOR_OVERLAY
        lockedStyle = "MARK"
        
        # Overlay style for locked slots when holding bypass key
        # 按住旁路键时已锁定槽位的覆盖层样式
        holdingKeyLockedStyle = "MARK"

        # Overlay style shown on lockable slots while holding the lock operation key
        # 按住锁定操作键时，可锁定槽位上显示的提示覆盖层样式
        highlightStyle = "BORDER"

        # Color for locked slot overlays when not holding the lock operation key
        # Supports "rgba(red, green, blue, alpha)", "rgb(red, green, blue)", "#RRGGBB", "#RRGGBBAA" or "luv(L, u, v, alpha)"
        # Alpha accepts 0.0 - 1.0 or 0 - 255
        # 未按住锁定操作键时，已锁定槽位覆盖层颜色
        # 支持 "rgba(红, 绿, 蓝, 透明度)"、"rgb(红, 绿, 蓝)"、"#RRGGBB"、"#RRGGBBAA" 或 "luv(L, u, v, alpha)"
        # 透明度支持 0.0 - 1.0 或 0 - 255
        lockedOverlayColor = "rgba(255,65,60,250)"

        # Opacity for locked slot overlays when not holding the lock operation key (0.0 - 1.0)
        # 未按住锁定操作键时，已锁定槽位覆盖层透明度，范围 0.0 - 1.0
        lockedOverlayOpacity = 0.7

        # Color for lockable slot highlight overlays while holding the lock operation key
        # Supports "rgba(red, green, blue, alpha)", "rgb(red, green, blue)", "#RRGGBB", "#RRGGBBAA" or "luv(L, u, v, alpha)"
        # Alpha accepts 0.0 - 1.0 or 0 - 255
        # 按住锁定操作键时，可收藏槽位提示覆盖层颜色
        # 支持 "rgba(红, 绿, 蓝, 透明度)"、"rgb(红, 绿, 蓝)"、"#RRGGBB"、"#RRGGBBAA" 或 "luv(L, u, v, alpha)"
        # 透明度支持 0.0 - 1.0 或 0 - 255
        lockableHighlightColor = "rgba(35,230,0,200)"

        # Opacity for lockable slot highlight overlays while holding the lock operation key (0.0 - 1.0)
        # 按住锁定操作键时，可收藏槽位提示覆盖层透明度，范围 0.0 - 1.0
        lockableHighlightOpacity = 0.55

        # Color for unlockable slot highlight overlays while holding the lock operation key
        # Supports "rgba(red, green, blue, alpha)", "rgb(red, green, blue)", "#RRGGBB", "#RRGGBBAA" or "luv(L, u, v, alpha)"
        # Alpha accepts 0.0 - 1.0 or 0 - 255
        # 按住锁定操作键时，可取消收藏槽位提示覆盖层颜色
        # 支持 "rgba(红, 绿, 蓝, 透明度)"、"rgb(红, 绿, 蓝)"、"#RRGGBB"、"#RRGGBBAA" 或 "luv(L, u, v, alpha)"
        # 透明度支持 0.0 - 1.0 或 0 - 255
        unlockableHighlightColor = "rgba(255, 195, 53, 180)"

        # Opacity for unlockable slot highlight overlays while holding the lock operation key (0.0 - 1.0)
        # 按住锁定操作键时，可取消收藏槽位提示覆盖层透明度，范围 0.0 - 1.0
        unlockableHighlightOpacity = 0.65

        # Default opacity used by COLOR_OVERLAY pure-color style (0.0 - 1.0)
        # COLOR_OVERLAY 纯色覆盖层默认透明度，范围 0.0 - 1.0
        colorOverlayOpacity = 0.35

        # Opacity multiplier for locked overlays while holding the bypass key (0.0 - 1.0)
        # 按住旁路键时，已锁定覆盖层透明度乘数，范围 0.0 - 1.0
        bypassOverlayOpacityMultiplier = 0.35

        # Render locked overlays in front of item icons
        # 是否将已锁定槽位覆盖层渲染在物品图标前方
        renderLockedOverlayInFront = true

        # Render lockable highlight overlays in front of item icons
        # 是否将可收藏提示覆盖层渲染在物品图标前方
        renderLockableHighlightInFront = true

        # Render unlockable highlight overlays in front of item icons
        # 是否将可取消收藏提示覆盖层渲染在物品图标前方
        renderUnlockableHighlightInFront = true
        
        [feedback]
        # Show visual feedback when trying to interact with locked slots
        # 尝试操作已锁定槽位时是否显示视觉反馈
        showVisualFeedback = true
        
        # Play sound feedback when trying to interact with locked slots
        # 尝试操作已锁定槽位时是否播放声音反馈
        playSoundFeedback = true
        
        # Sound to play for feedback
        # 声音反馈使用的音效
        feedbackSound = "minecraft:block.note_block.hat"
        
        # Volume for feedback sound
        # 声音反馈音量
        feedbackVolume = 0.5
        
        # Pitch for feedback sound
        # 声音反馈音高
        feedbackPitch = 1.0

        [debug]
        # Enable extra diagnostic logs for key states, slot clicks, overlays and guard decisions
        # 是否启用额外诊断日志，用于排查按键状态、槽位点击、覆盖层渲染和交互拦截
        enabled = false
        
        """;

    private static ConfigManager instance;
    private NeoFavoriteItemsConfig config;
    private Path configPath;

    private ConfigManager() {
        this.config = new NeoFavoriteItemsConfig();
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public void initialize(Path configDir) {
        this.configPath = configDir.resolve("neo-favorite-items.toml");
        loadConfig();
    }

    public NeoFavoriteItemsConfig getConfig() {
        return config;
    }

    public void loadConfig() {
        if (Files.exists(configPath)) {
            try {
                String content = Files.readString(configPath);
                parseConfig(content);
                appendMissingConfigEntries(content);
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
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
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

    private void appendMissingConfigEntries(String content) throws IOException {
        StringBuilder additions = new StringBuilder();
        if (!content.contains("[debug]")) {
            additions.append(System.lineSeparator())
                .append("[debug]").append(System.lineSeparator())
                .append("# Enable extra diagnostic logs for key states, slot clicks, overlays and guard decisions").append(System.lineSeparator())
                .append("# 是否启用额外诊断日志，用于排查按键状态、槽位点击、覆盖层渲染和交互拦截").append(System.lineSeparator())
                .append("enabled = false").append(System.lineSeparator());
        }
        if (content.contains("[overlay]") && !content.contains("lockedOverlayColor")) {
            additions.append(System.lineSeparator())
                .append("[overlay]").append(System.lineSeparator())
                .append("# Additional overlay color controls added in 0.0.1-alpha").append(System.lineSeparator())
                .append("# 0.0.1-alpha 新增的覆盖层颜色控制项").append(System.lineSeparator())
                .append("# lockedOverlayColor / lockedOverlayOpacity apply to locked slots when not holding the lock operation key").append(System.lineSeparator())
                .append("# lockedOverlayColor / lockedOverlayOpacity 用于未按住锁定操作键时的已锁定槽位").append(System.lineSeparator())
                .append("lockedOverlayColor = \"rgba(255, 215, 0, 1.0)\"").append(System.lineSeparator())
                .append("lockedOverlayOpacity = 0.7").append(System.lineSeparator())
                .append("# lockableHighlightColor applies to non-favorite player inventory slots with items while holding the lock operation key").append(System.lineSeparator())
                .append("# lockableHighlightColor 用于按住锁定操作键时，带有物品且尚未收藏的玩家背包槽位").append(System.lineSeparator())
                .append("lockableHighlightColor = \"rgba(102, 204, 255, 1.0)\"").append(System.lineSeparator())
                .append("lockableHighlightOpacity = 0.55").append(System.lineSeparator())
                .append("# unlockableHighlightColor applies to favorite slots while holding the lock operation key").append(System.lineSeparator())
                .append("# unlockableHighlightColor 用于按住锁定操作键时，已收藏且可取消收藏的槽位").append(System.lineSeparator())
                .append("unlockableHighlightColor = \"rgba(255, 170, 51, 1.0)\"").append(System.lineSeparator())
                .append("unlockableHighlightOpacity = 0.65").append(System.lineSeparator())
                .append("# colorOverlayOpacity controls the base opacity of the COLOR_OVERLAY pure-color style").append(System.lineSeparator())
                .append("# colorOverlayOpacity 控制 COLOR_OVERLAY 纯色覆盖层样式的基础透明度").append(System.lineSeparator())
                .append("colorOverlayOpacity = 0.35").append(System.lineSeparator())
                .append("# bypassOverlayOpacityMultiplier fades locked overlays while holding the bypass key").append(System.lineSeparator())
                .append("# bypassOverlayOpacityMultiplier 用于按住旁路键时淡化已锁定槽位覆盖层").append(System.lineSeparator())
                .append("bypassOverlayOpacityMultiplier = 0.35").append(System.lineSeparator())
                .append("# Render overlay layers in front of item icons").append(System.lineSeparator())
                .append("# 是否将各类覆盖层渲染在物品图标前方").append(System.lineSeparator())
                .append("renderLockedOverlayInFront = true").append(System.lineSeparator())
                .append("renderLockableHighlightInFront = true").append(System.lineSeparator())
                .append("renderUnlockableHighlightInFront = true").append(System.lineSeparator());
        } else if (content.contains("[overlay]") && !content.contains("renderLockedOverlayInFront")) {
            additions.append(System.lineSeparator())
                .append("[overlay]").append(System.lineSeparator())
                .append("# Render locked overlays in front of item icons").append(System.lineSeparator())
                .append("# 是否将已锁定槽位覆盖层渲染在物品图标前方").append(System.lineSeparator())
                .append("renderLockedOverlayInFront = true").append(System.lineSeparator())
                .append("# Render lockable highlight overlays in front of item icons").append(System.lineSeparator())
                .append("# 是否将可收藏提示覆盖层渲染在物品图标前方").append(System.lineSeparator())
                .append("renderLockableHighlightInFront = true").append(System.lineSeparator())
                .append("# Render unlockable highlight overlays in front of item icons").append(System.lineSeparator())
                .append("# 是否将可取消收藏提示覆盖层渲染在物品图标前方").append(System.lineSeparator())
                .append("renderUnlockableHighlightInFront = true").append(System.lineSeparator());
        }
        if (content.contains("[overlay]") && !content.contains("colorOverlayOpacity") && additions.indexOf("colorOverlayOpacity") < 0) {
            additions.append(System.lineSeparator())
                .append("[overlay]").append(System.lineSeparator())
                .append("# Default opacity used by COLOR_OVERLAY pure-color style (0.0 - 1.0)").append(System.lineSeparator())
                .append("# COLOR_OVERLAY 纯色覆盖层默认透明度，范围 0.0 - 1.0").append(System.lineSeparator())
                .append("colorOverlayOpacity = 0.35").append(System.lineSeparator());
        }

        if (!additions.isEmpty()) {
            Files.writeString(configPath, content.stripTrailing() + System.lineSeparator() + additions);
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
                case "debug" -> setDebugValue(key, value);
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
        if ("moveBehavior".equals(key)) {
            String cleanValue = value.replace("\"", "");
            try {
                config.slotBehavior.moveBehavior = NeoFavoriteItemsConfig.SlotMoveBehavior.valueOf(cleanValue);
            } catch (IllegalArgumentException e) {
                config.slotBehavior.moveBehavior = NeoFavoriteItemsConfig.SlotMoveBehavior.FOLLOW_ITEM;
            }
        }
    }

    private void setOverlayValue(String key, String value) {
        switch (key) {
            case "lockedStyle" -> config.overlay.lockedStyle = parseOverlayStyle(value);
            case "holdingKeyLockedStyle" -> config.overlay.holdingKeyLockedStyle = parseOverlayStyle(value);
            case "highlightStyle" -> config.overlay.highlightStyle = parseOverlayStyle(value);
            case "overlayColor" -> config.overlay.lockedOverlayColor = parseColor(value);
            case "overlayOpacity" -> config.overlay.lockedOverlayOpacity = Float.parseFloat(value);
            case "lockedOverlayColor" -> config.overlay.lockedOverlayColor = parseColor(value);
            case "lockedOverlayOpacity" -> config.overlay.lockedOverlayOpacity = Float.parseFloat(value);
            case "lockableHighlightColor" -> config.overlay.lockableHighlightColor = parseColor(value);
            case "lockableHighlightOpacity" -> config.overlay.lockableHighlightOpacity = Float.parseFloat(value);
            case "unlockableHighlightColor" -> config.overlay.unlockableHighlightColor = parseColor(value);
            case "unlockableHighlightOpacity" -> config.overlay.unlockableHighlightOpacity = Float.parseFloat(value);
            case "colorOverlayOpacity" -> config.overlay.colorOverlayOpacity = Float.parseFloat(value);
            case "bypassOverlayOpacityMultiplier" -> config.overlay.bypassOverlayOpacityMultiplier = Float.parseFloat(value);
            case "renderLockedOverlayInFront" -> config.overlay.renderLockedOverlayInFront = Boolean.parseBoolean(value);
            case "renderLockableHighlightInFront" -> config.overlay.renderLockableHighlightInFront = Boolean.parseBoolean(value);
            case "renderUnlockableHighlightInFront" -> config.overlay.renderUnlockableHighlightInFront = Boolean.parseBoolean(value);
        }
    }

    private int parseColor(String value) {
        String cleanValue = value.replace("\"", "").trim();
        if (cleanValue.regionMatches(true, 0, "rgba(", 0, 5) && cleanValue.endsWith(")")) {
            return parseRgbaColor(cleanValue);
        }
        if (cleanValue.regionMatches(true, 0, "rgb(", 0, 4) && cleanValue.endsWith(")")) {
            return parseRgbColor(cleanValue);
        }
        if (cleanValue.regionMatches(true, 0, "luv(", 0, 4) && cleanValue.endsWith(")")) {
            return parseLuvColor(cleanValue);
        }
        if (cleanValue.startsWith("#")) {
            return parseHexColor(cleanValue.substring(1), false);
        }
        if (cleanValue.startsWith("0x") || cleanValue.startsWith("0X")) {
            return parseHexColor(cleanValue.substring(2), true);
        }
        int parsed = Integer.parseInt(cleanValue);
        return parsed <= 0x00FFFFFF ? 0xFF000000 | parsed : parsed;
    }

    private int parseRgbColor(String value) {
        String[] parts = value.substring(4, value.length() - 1).split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("RGB color must be rgb(red, green, blue): " + value);
        }
        return toArgb(1.0d, parseColorComponent(parts[0]), parseColorComponent(parts[1]), parseColorComponent(parts[2]));
    }

    private int parseRgbaColor(String value) {
        String[] parts = value.substring(5, value.length() - 1).split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException("RGBA color must be rgba(red, green, blue, alpha): " + value);
        }
        return toArgb(
            parseAlpha(parts[3]),
            parseColorComponent(parts[0]),
            parseColorComponent(parts[1]),
            parseColorComponent(parts[2])
        );
    }

    private int parseHexColor(String value, boolean legacyArgb) {
        String hex = value.trim();
        if (hex.length() == 6) {
            int rgb = (int) Long.parseLong(hex, 16);
            return 0xFF000000 | rgb;
        }
        if (hex.length() == 8) {
            long parsed = Long.parseLong(hex, 16);
            if (legacyArgb) {
                return (int) parsed;
            }
            int red = (int) ((parsed >> 24) & 0xFF);
            int green = (int) ((parsed >> 16) & 0xFF);
            int blue = (int) ((parsed >> 8) & 0xFF);
            int alpha = (int) (parsed & 0xFF);
            return (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
        throw new IllegalArgumentException("Hex color must be #RRGGBB or #RRGGBBAA: " + value);
    }

    private int parseLuvColor(String value) {
        String[] parts = value.substring(4, value.length() - 1).split(",");
        if (parts.length != 3 && parts.length != 4) {
            throw new IllegalArgumentException("Luv color must be luv(L, u, v) or luv(L, u, v, alpha): " + value);
        }

        double l = parseDouble(parts[0]);
        double u = parseDouble(parts[1]);
        double v = parseDouble(parts[2]);
        double alpha = parts.length == 4 ? parseAlpha(parts[3]) : 1.0d;
        return luvToArgb(l, u, v, alpha);
    }

    private double parseDouble(String value) {
        return Double.parseDouble(value.trim());
    }

    private double parseColorComponent(String value) {
        double component = parseDouble(value);
        if (component > 1.0d) {
            component /= 255.0d;
        }
        return clamp01(component);
    }

    private double parseAlpha(String value) {
        double alpha = parseDouble(value);
        if (alpha > 1.0d) {
            alpha /= 255.0d;
        }
        return clamp01(alpha);
    }

    private int luvToArgb(double l, double u, double v, double alpha) {
        l = clamp(l, 0.0d, 100.0d);
        if (l <= 0.0d) {
            return toArgb(alpha, 0.0d, 0.0d, 0.0d);
        }

        double refX = 95.047d;
        double refY = 100.000d;
        double refZ = 108.883d;
        double refDenominator = refX + 15.0d * refY + 3.0d * refZ;
        double refU = 4.0d * refX / refDenominator;
        double refV = 9.0d * refY / refDenominator;

        double targetU = u / (13.0d * l) + refU;
        double targetV = v / (13.0d * l) + refV;
        if (targetV == 0.0d) {
            return toArgb(alpha, 0.0d, 0.0d, 0.0d);
        }

        double y = l > 8.0d
            ? refY * Math.pow((l + 16.0d) / 116.0d, 3.0d)
            : refY * l / 903.3d;
        double x = y * 9.0d * targetU / (4.0d * targetV);
        double z = y * (12.0d - 3.0d * targetU - 20.0d * targetV) / (4.0d * targetV);

        return xyzToArgb(alpha, x, y, z);
    }

    private int xyzToArgb(double alpha, double x, double y, double z) {
        x /= 100.0d;
        y /= 100.0d;
        z /= 100.0d;

        double r = 3.2406d * x - 1.5372d * y - 0.4986d * z;
        double g = -0.9689d * x + 1.8758d * y + 0.0415d * z;
        double b = 0.0557d * x - 0.2040d * y + 1.0570d * z;

        return toArgb(alpha, linearToSrgb(r), linearToSrgb(g), linearToSrgb(b));
    }

    private double linearToSrgb(double value) {
        value = clamp01(value);
        if (value <= 0.0031308d) {
            return 12.92d * value;
        }
        return 1.055d * Math.pow(value, 1.0d / 2.4d) - 0.055d;
    }

    private int toArgb(double alpha, double red, double green, double blue) {
        int a = toByte(alpha);
        int r = toByte(red);
        int g = toByte(green);
        int b = toByte(blue);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int toByte(double value) {
        return (int) Math.round(clamp01(value) * 255.0d);
    }

    private double clamp01(double value) {
        return clamp(value, 0.0d, 1.0d);
    }

    private double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private NeoFavoriteItemsConfig.OverlayStyle parseOverlayStyle(String value) {
        String cleanValue = value.replace("\"", "");
        switch (cleanValue) {
            case "LOCK_ICON" -> {
                return NeoFavoriteItemsConfig.OverlayStyle.LOCK;
            }
            case "BORDER_GLOW" -> {
                return NeoFavoriteItemsConfig.OverlayStyle.BORDER;
            }
            case "CHECKMARK" -> {
                return NeoFavoriteItemsConfig.OverlayStyle.MARK;
            }
        }
        try {
            return NeoFavoriteItemsConfig.OverlayStyle.valueOf(cleanValue);
        } catch (IllegalArgumentException e) {
            return NeoFavoriteItemsConfig.OverlayStyle.LOCK;
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

    private void setDebugValue(String key, String value) {
        if ("enabled".equals(key)) {
            config.debug.enabled = Boolean.parseBoolean(value);
        }
    }

    private void setKeybindingValue(String key, String value) {
        // Keybindings are registered in Minecraft's controls screen; legacy config keys are ignored.
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
