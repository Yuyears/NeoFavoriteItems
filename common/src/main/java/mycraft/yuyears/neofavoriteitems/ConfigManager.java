
package mycraft.yuyears.neofavoriteitems;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private enum ConfigFileKind {
        COMMON,
        CLIENT,
        LEGACY
    }

    private static final String COMMON_CONFIG_COMMENTS = """
        # Neo Favorite Items Common Configuration
        # 新物品收藏模组通用配置
        # Server-authoritative or rule-affecting options
        # 服务端权威或影响规则的配置
        # =====================================
        
        [general]
        # Whether to automatically unlock slots when they become empty
        # 当槽位变为空时是否自动解锁
        # Parent option of lockEmptySlots and allowItemsIntoLockedEmptySlots
        # lockEmptySlots 与 allowItemsIntoLockedEmptySlots 的父级配置
        # If true, empty slots cannot keep a favorite lock
        # 如果为 true，空槽位不会保留收藏锁定
        autoUnlockEmptySlots = %s
        
        # Whether to allow locking empty slots
        # 是否允许锁定空槽位
        # Child option of autoUnlockEmptySlots=false
        # autoUnlockEmptySlots=false 时的子级配置
        lockEmptySlots = %s
        
        # Whether to allow items to be placed into locked empty slots
        # 是否允许物品放入已锁定的空槽位
        # Child option of autoUnlockEmptySlots=false + lockEmptySlots=true
        # autoUnlockEmptySlots=false 且 lockEmptySlots=true 时的子级配置
        # When false, picked-up ground items skip locked empty main-inventory slots and try the next valid slot before the stack is consumed
        # 为 false 时，拾取地面掉落物会在物品被消耗前跳过已锁定空主背包槽，并尝试放入下一个有效槽位
        allowItemsIntoLockedEmptySlots = %s
        
        [lockBehavior]
        # Prevent clicking on locked slots
        # 阻止点击已锁定槽位
        preventClick = %s
        
        # Prevent dropping items from locked slots
        # 阻止从已锁定槽位丢弃物品
        preventDrop = %s
        
        # Prevent quick moving items from locked slots
        # 阻止从已锁定槽位快速移动物品
        # Covers server quick-move paths such as container transfers and sorter-style inventory moves
        # 覆盖服务端快速移动路径，例如容器转移和整理类库存移动
        preventQuickMove = %s
        
        # Prevent shift-clicking items from locked slots
        # 阻止 Shift 点击已锁定槽位
        # Kept as the explicit Shift-click rule used by the interaction guard
        # 作为交互守卫使用的显式 Shift 点击规则保留
        preventShiftClick = %s
        
        # Prevent dragging items over locked slots
        # 阻止拖拽物品经过已锁定槽位
        preventDrag = %s
        
        # Prevent swapping items with locked slots
        # 阻止与已锁定槽位交换物品
        preventSwap = %s
        
        # Allow bypassing lock by holding the bypass key
        # 是否允许按住旁路键临时绕过锁定限制
        # Applies only to guarded operations that receive the synced bypass-key state
        # 仅对能收到同步旁路键状态的受守卫操作生效
        allowBypassWithKey = %s
        
        [slotBehavior]
        # What happens when a favorite item is moved
        # 当被锁定/收藏的物品移动时如何处理锁定状态
        # FOLLOW_ITEM: The favorite status moves with the item
        # FOLLOW_ITEM：锁定状态跟随物品移动
        # STAY_AT_POSITION: The favorite status stays at the slot position
        # STAY_AT_POSITION：锁定状态固定在槽位位置
        # This controls favorite-state movement only; death preservation is configured in [deathBehavior]
        # 该项只控制收藏状态如何移动；死亡保留由 [deathBehavior] 配置
        moveBehavior = "%s"

        [deathBehavior]
        # Whether locked slot contents survive death even when keepInventory is false
        # 是否在 keepInventory=false 时仍让已锁定槽位的内容随死亡重生保留
        # If true, locked player-inventory slots are skipped during death drops and restored to the respawned player
        # 如果为 true，死亡掉落会跳过已锁定玩家背包槽，并在重生后恢复到新玩家
        # This is independent from moveBehavior and combines with the runtime keepInventory gamerule
        # 该项与 moveBehavior 无关，并会和运行时 keepInventory 游戏规则综合判断
        preserveLockedSlotContents = %s
        
        [debug]
        # Enable extra diagnostic logs for key states, slot clicks, overlays and guard decisions
        # 是否启用额外诊断日志，用于排查按键状态、槽位点击、覆盖层渲染和交互拦截
        enabled = %s
        
        """;

    private static final String CLIENT_CONFIG_COMMENTS = """
        # Neo Favorite Items Client Configuration
        # 新物品收藏模组客户端配置
        # Visual, feedback, and client-preferred options
        # 视觉、反馈和客户端优先生效的配置
        # =====================================

        [overlay]
        # Overlay style for locked slots
        # 已锁定槽位的覆盖层样式
        # Options: BORDER, CLASSIC, FRAMEWORK, HIGHLIGHT, BRACKETS, LOCK, MARK, TAG, STAR, COLOR_OVERLAY
        # 可选值：BORDER, CLASSIC, FRAMEWORK, HIGHLIGHT, BRACKETS, LOCK, MARK, TAG, STAR, COLOR_OVERLAY
        lockedStyle = "%s"
        
        # Overlay style for locked slots when holding bypass key
        # 按住旁路键时已锁定槽位的覆盖层样式
        # Used only as a visual hint; server-side lock rules are still controlled by the common config
        # 仅作为视觉提示；服务端锁定规则仍由 common 配置控制
        holdingKeyLockedStyle = "%s"

        # Overlay style shown on lockable slots while holding the lock operation key
        # 按住锁定操作键时，可锁定槽位上显示的提示覆盖层样式
        highlightStyle = "%s"

        # Color for locked slot overlays when not holding the lock operation key
        # Supports "rgba(red, green, blue, alpha)", "rgb(red, green, blue)", "#RRGGBB", "#RRGGBBAA" or "luv(L, u, v, alpha)"
        # Alpha accepts 0.0 - 1.0 or 0 - 255
        # 未按住锁定操作键时，已锁定槽位覆盖层颜色
        # 支持 "rgba(红, 绿, 蓝, 透明度)"、"rgb(红, 绿, 蓝)"、"#RRGGBB"、"#RRGGBBAA" 或 "luv(L, u, v, alpha)"
        # 透明度支持 0.0 - 1.0 或 0 - 255
        lockedOverlayColor = "%s"

        # Opacity for locked slot overlays when not holding the lock operation key (0.0 - 1.0)
        # 未按住锁定操作键时，已锁定槽位覆盖层透明度，范围 0.0 - 1.0
        lockedOverlayOpacity = %s

        # Color for lockable slot highlight overlays while holding the lock operation key
        # Supports "rgba(red, green, blue, alpha)", "rgb(red, green, blue)", "#RRGGBB", "#RRGGBBAA" or "luv(L, u, v, alpha)"
        # Alpha accepts 0.0 - 1.0 or 0 - 255
        # 按住锁定操作键时，可收藏槽位提示覆盖层颜色
        # 支持 "rgba(红, 绿, 蓝, 透明度)"、"rgb(红, 绿, 蓝)"、"#RRGGBB"、"#RRGGBBAA" 或 "luv(L, u, v, alpha)"
        # 透明度支持 0.0 - 1.0 或 0 - 255
        lockableHighlightColor = "%s"

        # Opacity for lockable slot highlight overlays while holding the lock operation key (0.0 - 1.0)
        # 按住锁定操作键时，可收藏槽位提示覆盖层透明度，范围 0.0 - 1.0
        lockableHighlightOpacity = %s

        # Color for unlockable slot highlight overlays while holding the lock operation key
        # Supports "rgba(red, green, blue, alpha)", "rgb(red, green, blue)", "#RRGGBB", "#RRGGBBAA" or "luv(L, u, v, alpha)"
        # Alpha accepts 0.0 - 1.0 or 0 - 255
        # 按住锁定操作键时，可取消收藏槽位提示覆盖层颜色
        # 支持 "rgba(红, 绿, 蓝, 透明度)"、"rgb(红, 绿, 蓝)"、"#RRGGBB"、"#RRGGBBAA" 或 "luv(L, u, v, alpha)"
        # 透明度支持 0.0 - 1.0 或 0 - 255
        unlockableHighlightColor = "%s"

        # Opacity for unlockable slot highlight overlays while holding the lock operation key (0.0 - 1.0)
        # 按住锁定操作键时，可取消收藏槽位提示覆盖层透明度，范围 0.0 - 1.0
        unlockableHighlightOpacity = %s

        # Default opacity used by COLOR_OVERLAY pure-color style (0.0 - 1.0)
        # COLOR_OVERLAY 纯色覆盖层默认透明度，范围 0.0 - 1.0
        colorOverlayOpacity = %s

        # Opacity multiplier for locked overlays while holding the bypass key (0.0 - 1.0)
        # 按住旁路键时，已锁定覆盖层透明度乘数，范围 0.0 - 1.0
        bypassOverlayOpacityMultiplier = %s

        # Render locked overlays in front of item icons
        # 是否将已锁定槽位覆盖层渲染在物品图标前方
        # Disable this if a resource pack or UI mod should draw item icons above the lock mark
        # 如果资源包或 UI 模组需要让物品图标盖在锁定标记上方，可关闭该项
        renderLockedOverlayInFront = %s

        # Render lockable highlight overlays in front of item icons
        # 是否将可收藏提示覆盖层渲染在物品图标前方
        renderLockableHighlightInFront = %s

        # Render unlockable highlight overlays in front of item icons
        # 是否将可取消收藏提示覆盖层渲染在物品图标前方
        renderUnlockableHighlightInFront = %s
        
        [feedback]
        # Show visual feedback when trying to interact with locked slots
        # 尝试操作已锁定槽位时是否显示视觉反馈
        # Client-side presentation only; it does not decide whether the server allows an operation
        # 仅影响客户端表现；不会决定服务端是否允许某次操作
        showVisualFeedback = %s
        
        # Play sound feedback when trying to interact with locked slots
        # 尝试操作已锁定槽位时是否播放声音反馈
        playSoundFeedback = %s
        
        # Sound to play for feedback
        # 声音反馈使用的音效
        feedbackSound = "%s"
        
        # Volume for feedback sound
        # 声音反馈音量
        feedbackVolume = %s
        
        # Pitch for feedback sound
        # 声音反馈音高
        feedbackPitch = %s

        """;

    private static ConfigManager instance;
    private NeoFavoriteItemsConfig config;
    private Path commonConfigPath;
    private Path clientConfigPath;
    private Path legacyConfigPath;
    private final List<String> loadIssues;

    private ConfigManager() {
        this.config = new NeoFavoriteItemsConfig();
        this.loadIssues = new ArrayList<>();
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public void initialize(Path configDir) {
        this.commonConfigPath = configDir.resolve(NeoFavoriteItemsConstants.COMMON_CONFIG_FILE_NAME);
        this.clientConfigPath = configDir.resolve(NeoFavoriteItemsConstants.CLIENT_CONFIG_FILE_NAME);
        this.legacyConfigPath = configDir.resolve(NeoFavoriteItemsConstants.CONFIG_FILE_NAME);
        loadConfig();
    }

    public NeoFavoriteItemsConfig getConfig() {
        return config;
    }

    public void applyPlatformConfig(NeoFavoriteItemsConfig config) {
        this.config = config;
        loadIssues.clear();
    }

    public List<String> getLoadIssues() {
        return List.copyOf(loadIssues);
    }

    public void loadConfig() {
        config = new NeoFavoriteItemsConfig();
        loadIssues.clear();

        boolean commonNeedsRewrite = !Files.exists(commonConfigPath);
        boolean clientNeedsRewrite = !Files.exists(clientConfigPath);
        boolean legacyExists = Files.exists(legacyConfigPath);
        boolean legacyMigrationAttempted = legacyExists && (commonNeedsRewrite || clientNeedsRewrite);
        boolean legacyMigrationRead = false;

        if (legacyMigrationAttempted) {
            legacyMigrationRead = readLegacyConfigFile();
        }

        if (Files.exists(commonConfigPath)) {
            commonNeedsRewrite = readConfigFile(commonConfigPath, ConfigFileKind.COMMON);
        }
        if (Files.exists(clientConfigPath)) {
            clientNeedsRewrite = readConfigFile(clientConfigPath, ConfigFileKind.CLIENT);
        }

        boolean commonSaved = true;
        boolean clientSaved = true;
        if (commonNeedsRewrite) {
            commonSaved = saveCommonConfig();
        }
        if (clientNeedsRewrite) {
            clientSaved = saveClientConfig();
        }

        if (legacyExists && (!legacyMigrationAttempted || (legacyMigrationRead && commonSaved && clientSaved))) {
            deleteLegacyConfig();
        }

        if (!loadIssues.isEmpty()) {
            DebugLogger.warn("Config loaded with {} issue(s); defaults were kept for invalid entries", loadIssues.size());
        }
    }

    private boolean readLegacyConfigFile() {
        try {
            String content = Files.readString(legacyConfigPath, StandardCharsets.UTF_8);
            parseConfig(content, ConfigFileKind.LEGACY);
            return true;
        } catch (IOException e) {
            recordLoadIssue("Failed to read legacy config file " + legacyConfigPath + "; keeping it for manual recovery", e);
            return false;
        }
    }

    private boolean readConfigFile(Path path, ConfigFileKind kind) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            int issuesBeforeParse = loadIssues.size();
            parseConfig(content, kind);
            return loadIssues.size() > issuesBeforeParse || hasMissingConfigEntries(content, kind);
        } catch (IOException e) {
            recordLoadIssue("Failed to read config file " + path + "; regenerated readable defaults", e);
            return true;
        }
    }

    private void parseConfig(String content, ConfigFileKind kind) {
        String[] lines = content.split("\n");
        String currentSection = "";
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            if (line.startsWith("[")) {
                if (!line.endsWith("]")) {
                    recordLoadIssue("Malformed config section header: " + line, new IllegalArgumentException(line));
                    continue;
                }
                currentSection = line.substring(1, line.length() - 1);
                if (!isKnownSection(currentSection, kind)) {
                    recordLoadIssue("Unknown config section: " + currentSection, new IllegalArgumentException(currentSection));
                }
                continue;
            }
            
            if (line.contains("=")) {
                String[] parts = line.split("=", 2);
                String key = parts[0].trim();
                String value = parts[1].trim();
                setConfigValue(currentSection, key, value, kind);
            } else {
                recordLoadIssue("Malformed config entry: " + line, new IllegalArgumentException(line));
            }
        }
    }

    private void setConfigValue(String section, String key, String value, ConfigFileKind kind) {
        try {
            if (!isKnownConfigValue(section, key, kind)) {
                recordLoadIssue(
                    "Unknown config value [" + section + "] " + key + "; rewriting config without it",
                    new IllegalArgumentException(key)
                );
                return;
            }
            switch (section) {
                case "general" -> setGeneralValue(key, value);
                case "lockBehavior" -> setLockBehaviorValue(key, value);
                case "slotBehavior" -> setSlotBehaviorValue(key, value);
                case "deathBehavior" -> setDeathBehaviorValue(key, value);
                case "overlay" -> setOverlayValue(key, value);
                case "feedback" -> setFeedbackValue(key, value);
                case "debug" -> setDebugValue(key, value);
                case "keybindings" -> setKeybindingValue(key, value);
            }
        } catch (Exception e) {
            recordLoadIssue(
                "Invalid config value [" + section + "] " + key + "=" + value + "; keeping default",
                e
            );
        }
    }

    private void setGeneralValue(String key, String value) {
        switch (key) {
            case "lockEmptySlots" -> config.general.lockEmptySlots = parseBoolean(value);
            case "autoUnlockEmptySlots" -> config.general.autoUnlockEmptySlots = parseBoolean(value);
            case "allowItemsIntoLockedEmptySlots" -> config.general.allowItemsIntoLockedEmptySlots = parseBoolean(value);
        }
    }

    private void setLockBehaviorValue(String key, String value) {
        switch (key) {
            case "preventClick" -> config.lockBehavior.preventClick = parseBoolean(value);
            case "preventDrop" -> config.lockBehavior.preventDrop = parseBoolean(value);
            case "preventQuickMove" -> config.lockBehavior.preventQuickMove = parseBoolean(value);
            case "preventShiftClick" -> config.lockBehavior.preventShiftClick = parseBoolean(value);
            case "preventDrag" -> config.lockBehavior.preventDrag = parseBoolean(value);
            case "preventSwap" -> config.lockBehavior.preventSwap = parseBoolean(value);
            case "allowBypassWithKey" -> config.lockBehavior.allowBypassWithKey = parseBoolean(value);
        }
    }

    private void setSlotBehaviorValue(String key, String value) {
        if ("moveBehavior".equals(key)) {
            String cleanValue = value.replace("\"", "");
            try {
                config.slotBehavior.moveBehavior = NeoFavoriteItemsConfig.SlotMoveBehavior.valueOf(cleanValue);
            } catch (IllegalArgumentException e) {
                recordLoadIssue("Unknown slot move behavior: " + cleanValue + "; using default " + config.slotBehavior.moveBehavior, e);
            }
        }
    }

    private void setOverlayValue(String key, String value) {
        switch (key) {
            case "lockedStyle" -> config.overlay.lockedStyle = parseOverlayStyle(value);
            case "holdingKeyLockedStyle" -> config.overlay.holdingKeyLockedStyle = parseOverlayStyle(value);
            case "highlightStyle" -> config.overlay.highlightStyle = parseOverlayStyle(value);
            case "overlayColor" -> config.overlay.lockedOverlayColor = parseColorValue(value);
            case "overlayOpacity" -> config.overlay.lockedOverlayOpacity = Float.parseFloat(value);
            case "lockedOverlayColor" -> config.overlay.lockedOverlayColor = parseColorValue(value);
            case "lockedOverlayOpacity" -> config.overlay.lockedOverlayOpacity = Float.parseFloat(value);
            case "lockableHighlightColor" -> config.overlay.lockableHighlightColor = parseColorValue(value);
            case "lockableHighlightOpacity" -> config.overlay.lockableHighlightOpacity = Float.parseFloat(value);
            case "unlockableHighlightColor" -> config.overlay.unlockableHighlightColor = parseColorValue(value);
            case "unlockableHighlightOpacity" -> config.overlay.unlockableHighlightOpacity = Float.parseFloat(value);
            case "colorOverlayOpacity" -> config.overlay.colorOverlayOpacity = Float.parseFloat(value);
            case "bypassOverlayOpacityMultiplier" -> config.overlay.bypassOverlayOpacityMultiplier = Float.parseFloat(value);
            case "renderLockedOverlayInFront" -> config.overlay.renderLockedOverlayInFront = parseBoolean(value);
            case "renderLockableHighlightInFront" -> config.overlay.renderLockableHighlightInFront = parseBoolean(value);
            case "renderUnlockableHighlightInFront" -> config.overlay.renderUnlockableHighlightInFront = parseBoolean(value);
        }
    }

    private void setDeathBehaviorValue(String key, String value) {
        if ("preserveLockedSlotContents".equals(key)) {
            config.deathBehavior.preserveLockedSlotContents = parseBoolean(value);
        }
    }

    public static int parseColorValue(String value) {
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

    private static int parseRgbColor(String value) {
        String[] parts = value.substring(4, value.length() - 1).split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("RGB color must be rgb(red, green, blue): " + value);
        }
        return toArgb(1.0d, parseColorComponent(parts[0]), parseColorComponent(parts[1]), parseColorComponent(parts[2]));
    }

    private static int parseRgbaColor(String value) {
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

    private static int parseHexColor(String value, boolean legacyArgb) {
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

    private static int parseLuvColor(String value) {
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

    private static double parseDouble(String value) {
        return Double.parseDouble(value.trim());
    }

    private boolean parseBoolean(String value) {
        String cleanValue = value.trim();
        if ("true".equalsIgnoreCase(cleanValue)) {
            return true;
        }
        if ("false".equalsIgnoreCase(cleanValue)) {
            return false;
        }
        throw new IllegalArgumentException("Boolean value must be true or false: " + value);
    }

    private static double parseColorComponent(String value) {
        double component = parseDouble(value);
        if (component > 1.0d) {
            component /= 255.0d;
        }
        return clamp01(component);
    }

    private static double parseAlpha(String value) {
        double alpha = parseDouble(value);
        if (alpha > 1.0d) {
            alpha /= 255.0d;
        }
        return clamp01(alpha);
    }

    private static int luvToArgb(double l, double u, double v, double alpha) {
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

    private static int xyzToArgb(double alpha, double x, double y, double z) {
        x /= 100.0d;
        y /= 100.0d;
        z /= 100.0d;

        double r = 3.2406d * x - 1.5372d * y - 0.4986d * z;
        double g = -0.9689d * x + 1.8758d * y + 0.0415d * z;
        double b = 0.0557d * x - 0.2040d * y + 1.0570d * z;

        return toArgb(alpha, linearToSrgb(r), linearToSrgb(g), linearToSrgb(b));
    }

    private static double linearToSrgb(double value) {
        value = clamp01(value);
        if (value <= 0.0031308d) {
            return 12.92d * value;
        }
        return 1.055d * Math.pow(value, 1.0d / 2.4d) - 0.055d;
    }

    private static int toArgb(double alpha, double red, double green, double blue) {
        int a = toByte(alpha);
        int r = toByte(red);
        int g = toByte(green);
        int b = toByte(blue);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int toByte(double value) {
        return (int) Math.round(clamp01(value) * 255.0d);
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0d, 1.0d);
    }

    private static double clamp(double value, double min, double max) {
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
            recordLoadIssue("Unknown overlay style: " + cleanValue + "; using default " + config.overlay.lockedStyle, e);
            return config.overlay.lockedStyle;
        }
    }

    private void setFeedbackValue(String key, String value) {
        switch (key) {
            case "showVisualFeedback" -> config.feedback.showVisualFeedback = parseBoolean(value);
            case "playSoundFeedback" -> config.feedback.playSoundFeedback = parseBoolean(value);
            case "feedbackSound" -> config.feedback.feedbackSound = value.replace("\"", "");
            case "feedbackVolume" -> config.feedback.feedbackVolume = Float.parseFloat(value);
            case "feedbackPitch" -> config.feedback.feedbackPitch = Float.parseFloat(value);
        }
    }

    private void setDebugValue(String key, String value) {
        if ("enabled".equals(key)) {
            config.debug.enabled = parseBoolean(value);
        }
    }

    private void setKeybindingValue(String key, String value) {
        // Keybindings are registered in Minecraft's controls screen; legacy config keys are ignored.
    }

    public void saveDefaultConfig() {
        saveConfig();
    }

    public void saveConfig() {
        saveCommonConfig();
        saveClientConfig();
    }

    private boolean saveCommonConfig() {
        try {
            Files.createDirectories(commonConfigPath.getParent());
            Files.writeString(commonConfigPath, renderCommonConfig(config), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            DebugLogger.error("Failed to write common config file: {}", commonConfigPath);
            DebugLogger.error("Common config write failure", e);
            return false;
        }
    }

    private boolean saveClientConfig() {
        try {
            Files.createDirectories(clientConfigPath.getParent());
            Files.writeString(clientConfigPath, renderClientConfig(config), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            DebugLogger.error("Failed to write client config file: {}", clientConfigPath);
            DebugLogger.error("Client config write failure", e);
            return false;
        }
    }

    private void deleteLegacyConfig() {
        try {
            Files.deleteIfExists(legacyConfigPath);
            DebugLogger.debug("Deleted migrated legacy config file: {}", legacyConfigPath);
        } catch (IOException e) {
            recordLoadIssue("Failed to delete migrated legacy config file " + legacyConfigPath, e);
        }
    }

    private void recordLoadIssue(String message, Exception exception) {
        loadIssues.add(message);
        DebugLogger.warn("{} ({})", message, exception.toString());
    }

    private boolean hasMissingConfigEntries(String content, ConfigFileKind kind) {
        if (kind == ConfigFileKind.CLIENT) {
            return hasMissingClientConfigEntries(content);
        }
        if (kind == ConfigFileKind.COMMON) {
            return hasMissingCommonConfigEntries(content);
        }
        return hasMissingCommonConfigEntries(content) || hasMissingClientConfigEntries(content);
    }

    private boolean hasMissingCommonConfigEntries(String content) {
        return !content.contains("autoUnlockEmptySlots")
            || !content.contains("lockEmptySlots")
            || !content.contains("allowItemsIntoLockedEmptySlots")
            || !content.contains("preventClick")
            || !content.contains("preventDrop")
            || !content.contains("preventQuickMove")
            || !content.contains("preventShiftClick")
            || !content.contains("preventDrag")
            || !content.contains("preventSwap")
            || !content.contains("allowBypassWithKey")
            || !content.contains("moveBehavior")
            || !content.contains("[deathBehavior]")
            || !content.contains("preserveLockedSlotContents")
            || !content.contains("[debug]")
            || !content.contains("enabled");
    }

    private boolean hasMissingClientConfigEntries(String content) {
        return !content.contains("lockedStyle")
            || !content.contains("holdingKeyLockedStyle")
            || !content.contains("highlightStyle")
            || !(content.contains("lockedOverlayColor") || content.contains("overlayColor"))
            || !(content.contains("lockedOverlayOpacity") || content.contains("overlayOpacity"))
            || !content.contains("lockableHighlightColor")
            || !content.contains("lockableHighlightOpacity")
            || !content.contains("unlockableHighlightColor")
            || !content.contains("unlockableHighlightOpacity")
            || !content.contains("colorOverlayOpacity")
            || !content.contains("bypassOverlayOpacityMultiplier")
            || !content.contains("renderLockedOverlayInFront")
            || !content.contains("renderLockableHighlightInFront")
            || !content.contains("renderUnlockableHighlightInFront")
            || !content.contains("showVisualFeedback")
            || !content.contains("playSoundFeedback")
            || !content.contains("feedbackSound")
            || !content.contains("feedbackVolume")
            || !content.contains("feedbackPitch");
    }

    private boolean isKnownSection(String section, ConfigFileKind kind) {
        return switch (kind) {
            case COMMON -> isCommonSection(section);
            case CLIENT -> isClientSection(section);
            case LEGACY -> isCommonSection(section) || isClientSection(section) || "keybindings".equals(section);
        };
    }

    private boolean isKnownConfigValue(String section, String key, ConfigFileKind kind) {
        if (!isKnownSection(section, kind)) {
            return false;
        }
        return switch (section) {
            case "general" -> switch (key) {
                case "lockEmptySlots", "autoUnlockEmptySlots", "allowItemsIntoLockedEmptySlots" -> true;
                default -> false;
            };
            case "lockBehavior" -> switch (key) {
                case "preventClick", "preventDrop", "preventQuickMove", "preventShiftClick",
                     "preventDrag", "preventSwap", "allowBypassWithKey" -> true;
                default -> false;
            };
            case "slotBehavior" -> "moveBehavior".equals(key);
            case "deathBehavior" -> "preserveLockedSlotContents".equals(key);
            case "overlay" -> switch (key) {
                case "lockedStyle", "holdingKeyLockedStyle", "highlightStyle",
                     "overlayColor", "overlayOpacity", "lockedOverlayColor", "lockedOverlayOpacity",
                     "lockableHighlightColor", "lockableHighlightOpacity",
                     "unlockableHighlightColor", "unlockableHighlightOpacity",
                     "colorOverlayOpacity", "bypassOverlayOpacityMultiplier",
                     "renderLockedOverlayInFront", "renderLockableHighlightInFront",
                     "renderUnlockableHighlightInFront" -> true;
                default -> false;
            };
            case "feedback" -> switch (key) {
                case "showVisualFeedback", "playSoundFeedback", "feedbackSound", "feedbackVolume", "feedbackPitch" -> true;
                default -> false;
            };
            case "debug" -> "enabled".equals(key);
            case "keybindings" -> true;
            default -> false;
        };
    }

    private boolean isCommonSection(String section) {
        return switch (section) {
            case "general", "lockBehavior", "slotBehavior", "deathBehavior", "debug" -> true;
            default -> false;
        };
    }

    private boolean isClientSection(String section) {
        return switch (section) {
            case "overlay", "feedback" -> true;
            default -> false;
        };
    }

    private String renderCommonConfig(NeoFavoriteItemsConfig config) {
        return COMMON_CONFIG_COMMENTS.formatted(
            config.general.autoUnlockEmptySlots,
            config.general.lockEmptySlots,
            config.general.allowItemsIntoLockedEmptySlots,
            config.lockBehavior.preventClick,
            config.lockBehavior.preventDrop,
            config.lockBehavior.preventQuickMove,
            config.lockBehavior.preventShiftClick,
            config.lockBehavior.preventDrag,
            config.lockBehavior.preventSwap,
            config.lockBehavior.allowBypassWithKey,
            config.slotBehavior.moveBehavior.name(),
            config.deathBehavior.preserveLockedSlotContents,
            config.debug.enabled
        );
    }

    private String renderClientConfig(NeoFavoriteItemsConfig config) {
        return CLIENT_CONFIG_COMMENTS.formatted(
            config.overlay.lockedStyle.name(),
            config.overlay.holdingKeyLockedStyle.name(),
            config.overlay.highlightStyle.name(),
            colorToRgba(config.overlay.lockedOverlayColor),
            floatToConfig(config.overlay.lockedOverlayOpacity),
            colorToRgba(config.overlay.lockableHighlightColor),
            floatToConfig(config.overlay.lockableHighlightOpacity),
            colorToRgba(config.overlay.unlockableHighlightColor),
            floatToConfig(config.overlay.unlockableHighlightOpacity),
            floatToConfig(config.overlay.colorOverlayOpacity),
            floatToConfig(config.overlay.bypassOverlayOpacityMultiplier),
            config.overlay.renderLockedOverlayInFront,
            config.overlay.renderLockableHighlightInFront,
            config.overlay.renderUnlockableHighlightInFront,
            config.feedback.showVisualFeedback,
            config.feedback.playSoundFeedback,
            escapeConfigString(config.feedback.feedbackSound),
            floatToConfig(config.feedback.feedbackVolume),
            floatToConfig(config.feedback.feedbackPitch)
        );
    }

    private String colorToRgba(int color) {
        return "rgba("
            + ((color >> 16) & 0xFF)
            + ","
            + ((color >> 8) & 0xFF)
            + ","
            + (color & 0xFF)
            + ","
            + ((color >>> 24) & 0xFF)
            + ")";
    }

    private String floatToConfig(float value) {
        return Float.toString(value);
    }

    private String escapeConfigString(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
