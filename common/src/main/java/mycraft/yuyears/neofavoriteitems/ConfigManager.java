
package mycraft.yuyears.neofavoriteitems;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import mycraft.yuyears.neofavoriteitems.render.OverlayMaterialMode;
import mycraft.yuyears.neofavoriteitems.render.OverlayTextureCatalog;
import mycraft.yuyears.neofavoriteitems.render.OverlayLayerList;

public class ConfigManager {
    private enum ConfigFileKind {
        SERVER,
        CLIENT_RENDERING,
        CLIENT_LOGIC,
        CLIENT_COMBINED,
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
    private Path clientLogicConfigPath;
    private Path legacyConfigPath;
    private Path legacyCommonConfigPath;
    private Path legacyClientConfigPath;
    private final List<String> loadIssues;
    private FileStamp commonStamp = FileStamp.MISSING;
    private FileStamp clientStamp = FileStamp.MISSING;
    private FileStamp clientLogicStamp = FileStamp.MISSING;
    private long lastChangeCheckNanos;
    private boolean draftDirty;
    private boolean externalChangeDetected;
    private boolean profileConfigSeen;
    private long revision;
    private int normalizationCount;
    private boolean layerListSeen;
    private boolean legacyProfileSeen;

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
        this.clientLogicConfigPath = configDir.resolve(NeoFavoriteItemsConstants.CLIENT_LOGIC_CONFIG_FILE_NAME);
        this.legacyConfigPath = configDir.resolve(NeoFavoriteItemsConstants.CONFIG_FILE_NAME);
        this.legacyCommonConfigPath = configDir.resolve(NeoFavoriteItemsConstants.LEGACY_COMMON_CONFIG_FILE_NAME);
        this.legacyClientConfigPath = configDir.resolve(NeoFavoriteItemsConstants.LEGACY_CLIENT_CONFIG_FILE_NAME);
        loadConfig();
    }

    /** Reload configuration immediately, for UI Reload/Apply actions. */
    public synchronized boolean reload() {
        if (commonConfigPath == null) {
            return false;
        }
        loadConfig();
        return true;
    }

    /** Poll config files at most twice per second and reload external edits. */
    public synchronized boolean reloadIfChanged() {
        if (commonConfigPath == null) {
            return false;
        }
        long now = System.nanoTime();
        if (now - lastChangeCheckNanos < 500_000_000L) {
            return false;
        }
        lastChangeCheckNanos = now;
        FileStamp currentCommon = FileStamp.read(commonConfigPath);
        FileStamp currentClient = FileStamp.read(clientConfigPath);
        FileStamp currentClientLogic = FileStamp.read(clientLogicConfigPath);
        if (currentCommon.equals(commonStamp) && currentClient.equals(clientStamp)
            && currentClientLogic.equals(clientLogicStamp)) {
            return false;
        }
        if (draftDirty) {
            externalChangeDetected = true;
            return false;
        }
        loadConfig();
        externalChangeDetected = false;
        DebugLogger.debug("Configuration reloaded after file change");
        return true;
    }

    public synchronized void setDraftDirty(boolean dirty) {
        draftDirty = dirty;
        if (!dirty) externalChangeDetected = false;
    }

    public synchronized boolean isExternalChangeDetected() {
        return externalChangeDetected;
    }

    public synchronized boolean isDraftDirty() {
        return draftDirty;
    }

    /** Accept current draft as authoritative without reloading changed files. */
    public synchronized void keepDraftAfterExternalChange() {
        commonStamp = FileStamp.read(commonConfigPath);
        clientStamp = FileStamp.read(clientConfigPath);
        clientLogicStamp = FileStamp.read(clientLogicConfigPath);
        externalChangeDetected = false;
    }

    public NeoFavoriteItemsConfig getConfig() {
        return config;
    }

    public void applyPlatformConfig(NeoFavoriteItemsConfig config) {
        this.config = config;
        loadIssues.clear();
        revision++;
    }

    public synchronized long getRevision() {
        return revision;
    }

    public synchronized void markRuntimeConfigChanged() {
        revision++;
    }

    public List<String> getLoadIssues() {
        return List.copyOf(loadIssues);
    }

    public void loadConfig() {
        config = new NeoFavoriteItemsConfig();
        externalChangeDetected = false;
        loadIssues.clear();
        profileConfigSeen = false;
        normalizationCount = 0;
        layerListSeen = false;
        legacyProfileSeen = false;

        boolean commonNeedsRewrite = !Files.exists(commonConfigPath);
        boolean clientNeedsRewrite = !Files.exists(clientConfigPath);
        boolean clientLogicNeedsRewrite = !Files.exists(clientLogicConfigPath);
        boolean legacyExists = Files.exists(legacyConfigPath);
        boolean legacySplitExists = Files.exists(legacyCommonConfigPath) || Files.exists(legacyClientConfigPath);
        boolean legacyMigrationAttempted = legacyExists && (commonNeedsRewrite || clientNeedsRewrite);
        boolean legacyMigrationRead = false;

        if (legacyMigrationAttempted) {
            legacyMigrationRead = readLegacyConfigFile();
        }

        if (Files.exists(commonConfigPath)) {
            commonNeedsRewrite = readConfigFile(commonConfigPath, ConfigFileKind.SERVER);
        } else if (Files.exists(legacyCommonConfigPath)) {
            readConfigFile(legacyCommonConfigPath, ConfigFileKind.SERVER);
        }
        if (Files.exists(clientConfigPath)) {
            clientNeedsRewrite = readConfigFile(clientConfigPath, ConfigFileKind.CLIENT_RENDERING);
        } else if (Files.exists(legacyClientConfigPath)) {
            readConfigFile(legacyClientConfigPath, ConfigFileKind.CLIENT_COMBINED);
        }
        if (Files.exists(clientLogicConfigPath)) {
            clientLogicNeedsRewrite = readConfigFile(clientLogicConfigPath, ConfigFileKind.CLIENT_LOGIC);
        }

        if (!profileConfigSeen) {
            config.overlay.syncProfilesFromLegacy();
            clientNeedsRewrite = true;
        }
        if (!layerListSeen) {
            config.overlay.lockedLayers = List.of(config.overlay.locked.copy());
            config.overlay.bypassLayers = List.of(config.overlay.bypass.copy());
            config.overlay.lockableLayers = List.of(config.overlay.lockable.copy());
            config.overlay.unlockableLayers = List.of(config.overlay.unlockable.copy());
            clientNeedsRewrite = true;
        } else {
            config.overlay.lockedLayers = OverlayLayerList.normalize(config.overlay.lockedLayers, OverlayProfileConfig::defaultLocked);
            config.overlay.bypassLayers = OverlayLayerList.normalize(config.overlay.bypassLayers, OverlayProfileConfig::defaultBypass);
            config.overlay.lockableLayers = OverlayLayerList.normalize(config.overlay.lockableLayers, OverlayProfileConfig::defaultLockable);
            config.overlay.unlockableLayers = OverlayLayerList.normalize(config.overlay.unlockableLayers, OverlayProfileConfig::defaultUnlockable);
            if (legacyProfileSeen) syncFirstLayersFromLegacy();
            else syncLegacyProfilesFromLayers();
        }

        boolean commonSaved = true;
        boolean clientSaved = true;
        if (commonNeedsRewrite) {
            commonSaved = saveCommonConfig();
        }
        if (clientNeedsRewrite) {
            clientSaved = saveClientRenderingConfig();
        }
        boolean clientLogicSaved = true;
        if (clientLogicNeedsRewrite) {
            clientLogicSaved = saveClientLogicConfig();
        }

        if (legacyExists && (!legacyMigrationAttempted || (legacyMigrationRead && commonSaved && clientSaved))) {
            deleteLegacyConfig();
        }
        if (legacySplitExists && commonSaved && clientSaved && clientLogicSaved) {
            deleteMigratedConfig(legacyCommonConfigPath);
            deleteMigratedConfig(legacyClientConfigPath);
        }

        if (!loadIssues.isEmpty()) {
            DebugLogger.warn("Config loaded with {} issue(s); defaults were kept for invalid entries", loadIssues.size());
        }
        commonStamp = FileStamp.read(commonConfigPath);
        clientStamp = FileStamp.read(clientConfigPath);
        clientLogicStamp = FileStamp.read(clientLogicConfigPath);
        lastChangeCheckNanos = System.nanoTime();
        revision++;
    }

    private record FileStamp(boolean exists, long modifiedMillis, long size) {
        private static final FileStamp MISSING = new FileStamp(false, 0L, 0L);

        private static FileStamp read(Path path) {
            try {
                return Files.exists(path)
                    ? new FileStamp(true, Files.getLastModifiedTime(path).toMillis(), Files.size(path))
                    : MISSING;
            } catch (IOException | RuntimeException e) {
                return MISSING;
            }
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
            int normalizationsBeforeParse = normalizationCount;
            parseConfig(content, kind);
            return loadIssues.size() > issuesBeforeParse
                || normalizationCount > normalizationsBeforeParse
                || hasMissingConfigEntries(content, kind);
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
            
            if (line.startsWith("[[")) {
                if (!line.endsWith("]]")) {
                    recordLoadIssue("Malformed array config section header: " + line, new IllegalArgumentException(line));
                    continue;
                }
                currentSection = line.substring(2, line.length() - 2) + ".__new__";
                if (currentSection.startsWith("profile.") && currentSection.endsWith(".layers.__new__")) {
                    beginProfileLayer(currentSection.substring(0, currentSection.length() - ".__new__".length()));
                }
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
            if (section.endsWith(".__new__")) {
                setProfileLayerValue(section.substring(0, section.length() - ".__new__".length()), key, value);
                return;
            }
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
                default -> {
                    if (section.startsWith("profile.")) setProfileValue(section, key, value);
                }
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
        String componentText = value.trim();
        double component = parseDouble(componentText);
        if (componentText.matches("[+-]?\\d+") || component > 1.0d) {
            component /= 255.0d;
        }
        return clamp01(component);
    }

    private static double parseAlpha(String value) {
        return parseColorComponent(value);
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
        return parseOverlayStyle(value, config.overlay.lockedStyle);
    }

    private NeoFavoriteItemsConfig.OverlayStyle parseOverlayStyle(
        String value,
        NeoFavoriteItemsConfig.OverlayStyle fallback
    ) {
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
            recordLoadIssue("Unknown overlay style: " + cleanValue + "; using default " + fallback, e);
            return fallback;
        }
    }

    private void setFeedbackValue(String key, String value) {
        switch (key) {
            case "showVisualFeedback" -> config.feedback.showVisualFeedback = parseBoolean(value);
            case "playSoundFeedback" -> config.feedback.playSoundFeedback = parseBoolean(value);
            case "feedbackSound" -> config.feedback.feedbackSound = parseConfigString(value);
            case "feedbackVolume" -> config.feedback.feedbackVolume = Float.parseFloat(value);
            case "feedbackPitch" -> config.feedback.feedbackPitch = Float.parseFloat(value);
            case "uiTheme" -> config.feedback.uiTheme = parseConfigString(value).toUpperCase(java.util.Locale.ROOT);
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
        saveClientRenderingConfig();
        saveClientLogicConfig();
        revision++;
    }

    private boolean saveCommonConfig() {
        try {
            writeConfigAtomically(commonConfigPath, renderCommonConfig(config));
            return true;
        } catch (IOException e) {
            DebugLogger.error("Failed to write common config file: {}", commonConfigPath);
            DebugLogger.error("Common config write failure", e);
            return false;
        }
    }

    private boolean saveClientRenderingConfig() {
        try {
            writeConfigAtomically(clientConfigPath, renderClientRenderingConfig(config));
            return true;
        } catch (IOException e) {
            DebugLogger.error("Failed to write client config file: {}", clientConfigPath);
            DebugLogger.error("Client config write failure", e);
            return false;
        }
    }

    private mycraft.yuyears.neofavoriteitems.render.OverlayMaterialMode parseMaterialMode(String value, mycraft.yuyears.neofavoriteitems.render.OverlayMaterialMode fallback) {
        try {
            return mycraft.yuyears.neofavoriteitems.render.OverlayMaterialMode.valueOf(value.replace("\"", "").trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    public synchronized void saveServerConfig() {
        if (saveCommonConfig()) commonStamp = FileStamp.read(commonConfigPath);
        revision++;
    }

    public synchronized void saveClientConfig() {
        if (saveClientRenderingConfig()) clientStamp = FileStamp.read(clientConfigPath);
        if (saveClientLogicConfig()) clientLogicStamp = FileStamp.read(clientLogicConfigPath);
        revision++;
    }

    private void setProfileValue(String section, String key, String value) {
        OverlayProfileConfig profile = switch (section) {
            case "profile.locked" -> config.overlay.locked;
            case "profile.bypass" -> config.overlay.bypass;
            case "profile.lockable" -> config.overlay.lockable;
            case "profile.unlockable" -> config.overlay.unlockable;
            default -> throw new IllegalArgumentException(section);
        };
        switch (key) {
            case "style" -> profile.style = parseOverlayStyle(value, profile.style);
            case "materialMode" -> profile.materialMode = parseMaterialMode(value, profile.materialMode);
            case "materialId" -> {
                profile.materialId = OverlayTextureCatalog.canonicalPresetId(parseConfigString(value));
                if (OverlayTextureCatalog.NO_MATERIAL.equals(profile.materialId)
                    || OverlayTextureCatalog.presetStyle(profile.materialId) == NeoFavoriteItemsConfig.OverlayStyle.COLOR_OVERLAY) {
                    profile.materialMode = OverlayMaterialMode.NO_MATERIAL;
                    profile.materialId = OverlayTextureCatalog.NO_MATERIAL;
                }
            }
            case "color" -> profile.color = parseColorValue(value);
            case "opacity" -> profile.opacity = parseClampedProfileFloat(value, 0.0f, 1.0f);
            case "colorMode" -> profile.colorMode = mycraft.yuyears.neofavoriteitems.render.OverlayColorMode.valueOf(value.replace("\"", ""));
            case "opacityBehavior" -> profile.opacityBehavior = OverlayProfileConfig.OpacityBehavior.valueOf(value.replace("\"", ""));
            case "anchor" -> profile.anchor = mycraft.yuyears.neofavoriteitems.render.OverlayPlacement.Anchor.valueOf(value.replace("\"", ""));
            case "offsetX" -> profile.offsetX = parseFiniteProfileFloat(value);
            case "offsetY" -> profile.offsetY = parseFiniteProfileFloat(value);
            case "width" -> profile.width = parsePositiveProfileFloat(value);
            case "height" -> profile.height = parsePositiveProfileFloat(value);
            case "scale" -> profile.scale = parsePositiveProfileFloat(value);
            case "rotationDegrees" -> profile.rotationDegrees = parseFiniteProfileFloat(value);
            case "zIndex" -> profile.zIndex = parseOverlayZIndex(value);
            case "allowOverflow" -> profile.allowOverflow = parseBoolean(value);
            case "clipToSlot" -> profile.clipToSlot = parseBoolean(value);
        }
        profileConfigSeen = true;
        legacyProfileSeen = true;
    }

    private void beginProfileLayer(String section) {
        layerListSeen = true;
        List<OverlayProfileConfig> layers = switch (section) {
            case "profile.locked.layers" -> config.overlay.lockedLayers;
            case "profile.bypass.layers" -> config.overlay.bypassLayers;
            case "profile.lockable.layers" -> config.overlay.lockableLayers;
            case "profile.unlockable.layers" -> config.overlay.unlockableLayers;
            default -> null;
        };
        if (layers == null) return;
        if (layers.size() == 1 && isStandardLayer(layers.getFirst(), section)) {
            layers = new ArrayList<>();
        } else {
            layers = new ArrayList<>(layers);
        }
        layers.add(defaultLayerFor(section));
        switch (section) {
            case "profile.locked.layers" -> config.overlay.lockedLayers = layers;
            case "profile.bypass.layers" -> config.overlay.bypassLayers = layers;
            case "profile.lockable.layers" -> config.overlay.lockableLayers = layers;
            case "profile.unlockable.layers" -> config.overlay.unlockableLayers = layers;
        }
    }

    private void setProfileLayerValue(String section, String key, String value) {
        List<OverlayProfileConfig> layers = switch (section) {
            case "profile.locked.layers" -> config.overlay.lockedLayers;
            case "profile.bypass.layers" -> config.overlay.bypassLayers;
            case "profile.lockable.layers" -> config.overlay.lockableLayers;
            case "profile.unlockable.layers" -> config.overlay.unlockableLayers;
            default -> List.of();
        };
        if (layers.isEmpty()) return;
        setProfileValueOn(layers.getLast(), key, value);
        profileConfigSeen = true;
    }

    private void setProfileValueOn(OverlayProfileConfig profile, String key, String value) {
        switch (key) {
            case "style" -> profile.style = parseOverlayStyle(value, profile.style);
            case "materialMode" -> profile.materialMode = parseMaterialMode(value, profile.materialMode);
            case "materialId" -> {
                profile.materialId = OverlayTextureCatalog.canonicalPresetId(parseConfigString(value));
                if (OverlayTextureCatalog.NO_MATERIAL.equals(profile.materialId)
                    || OverlayTextureCatalog.presetStyle(profile.materialId) == NeoFavoriteItemsConfig.OverlayStyle.COLOR_OVERLAY) {
                    profile.materialMode = OverlayMaterialMode.NO_MATERIAL;
                    profile.materialId = OverlayTextureCatalog.NO_MATERIAL;
                }
            }
            case "color" -> profile.color = parseColorValue(value);
            case "opacity" -> profile.opacity = parseClampedProfileFloat(value, 0.0f, 1.0f);
            case "colorMode" -> profile.colorMode = mycraft.yuyears.neofavoriteitems.render.OverlayColorMode.valueOf(value.replace("\"", ""));
            case "opacityBehavior" -> profile.opacityBehavior = OverlayProfileConfig.OpacityBehavior.valueOf(value.replace("\"", ""));
            case "anchor" -> profile.anchor = mycraft.yuyears.neofavoriteitems.render.OverlayPlacement.Anchor.valueOf(value.replace("\"", ""));
            case "offsetX" -> profile.offsetX = parseFiniteProfileFloat(value);
            case "offsetY" -> profile.offsetY = parseFiniteProfileFloat(value);
            case "width" -> profile.width = parsePositiveProfileFloat(value);
            case "height" -> profile.height = parsePositiveProfileFloat(value);
            case "scale" -> profile.scale = parsePositiveProfileFloat(value);
            case "rotationDegrees" -> profile.rotationDegrees = parseFiniteProfileFloat(value);
            case "zIndex" -> profile.zIndex = parseOverlayZIndex(value);
            case "allowOverflow" -> profile.allowOverflow = parseBoolean(value);
            case "clipToSlot" -> profile.clipToSlot = parseBoolean(value);
        }
    }

    private static OverlayProfileConfig defaultLayerFor(String section) {
        return switch (section) {
            case "profile.bypass.layers" -> OverlayProfileConfig.defaultBypass();
            case "profile.lockable.layers" -> OverlayProfileConfig.defaultLockable();
            case "profile.unlockable.layers" -> OverlayProfileConfig.defaultUnlockable();
            default -> OverlayProfileConfig.defaultLocked();
        };
    }

    private static boolean isStandardLayer(OverlayProfileConfig p, String section) {
        return p.sameValues(defaultLayerFor(section));
    }

    private void syncLegacyProfilesFromLayers() {
        if (!config.overlay.lockedLayers.isEmpty()) config.overlay.locked.copyFrom(config.overlay.lockedLayers.getFirst());
        if (!config.overlay.bypassLayers.isEmpty()) config.overlay.bypass.copyFrom(config.overlay.bypassLayers.getFirst());
        if (!config.overlay.lockableLayers.isEmpty()) config.overlay.lockable.copyFrom(config.overlay.lockableLayers.getFirst());
        if (!config.overlay.unlockableLayers.isEmpty()) config.overlay.unlockable.copyFrom(config.overlay.unlockableLayers.getFirst());
    }

    private void syncFirstLayersFromLegacy() {
        config.overlay.lockedLayers = replaceFirst(config.overlay.lockedLayers, config.overlay.locked);
        config.overlay.bypassLayers = replaceFirst(config.overlay.bypassLayers, config.overlay.bypass);
        config.overlay.lockableLayers = replaceFirst(config.overlay.lockableLayers, config.overlay.lockable);
        config.overlay.unlockableLayers = replaceFirst(config.overlay.unlockableLayers, config.overlay.unlockable);
    }

    private float parseFiniteProfileFloat(String value) {
        float parsed = Float.parseFloat(value);
        if (!Float.isFinite(parsed)) throw new IllegalArgumentException("Profile value must be finite");
        return parsed;
    }

    private float parsePositiveProfileFloat(String value) {
        float parsed = parseFiniteProfileFloat(value);
        if (parsed <= 0.0f) throw new IllegalArgumentException("Profile value must be positive");
        return parsed;
    }

    private float parseClampedProfileFloat(String value, float min, float max) {
        float parsed = parseFiniteProfileFloat(value);
        float normalized = Math.clamp(parsed, min, max);
        if (Float.compare(parsed, normalized) != 0) normalizationCount++;
        return normalized;
    }

    private int parseOverlayZIndex(String value) {
        double parsed = Double.parseDouble(value);
        int normalized = normalizeOverlayZIndex(parsed);
        if (Double.compare(parsed, normalized) != 0) normalizationCount++;
        return normalized;
    }

    static int normalizeOverlayZIndex(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("zIndex must be finite");
        int zIndex = (int) Math.min(Math.max(value, 0.0D), 1000.0D);
        return zIndex == mycraft.yuyears.neofavoriteitems.render.OverlayZIndex.ITEM
            ? mycraft.yuyears.neofavoriteitems.render.OverlayZIndex.ABOVE_ITEM
            : zIndex;
    }

    private boolean saveClientLogicConfig() {
        try {
            writeConfigAtomically(clientLogicConfigPath, renderClientLogicConfig(config));
            return true;
        } catch (IOException e) {
            DebugLogger.error("Failed to write client logic config file: {}", clientLogicConfigPath);
            DebugLogger.error("Client logic config write failure", e);
            return false;
        }
    }

    private static void writeConfigAtomically(Path target, String content) throws IOException {
        Path parent = target.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void deleteLegacyConfig() {
        deleteMigratedConfig(legacyConfigPath);
    }

    private void deleteMigratedConfig(Path path) {
        try {
            Files.deleteIfExists(path);
            DebugLogger.debug("Deleted migrated legacy config file: {}", path);
        } catch (IOException e) {
            recordLoadIssue("Failed to delete migrated legacy config file " + path, e);
        }
    }

    private void recordLoadIssue(String message, Exception exception) {
        loadIssues.add(message);
        DebugLogger.warn("{} ({})", message, exception.toString());
    }

    private boolean hasMissingConfigEntries(String content, ConfigFileKind kind) {
        if (kind == ConfigFileKind.CLIENT_RENDERING) {
            return hasMissingRenderingConfigEntries(content);
        }
        if (kind == ConfigFileKind.CLIENT_LOGIC) {
            return hasMissingClientLogicConfigEntries(content);
        }
        if (kind == ConfigFileKind.CLIENT_COMBINED) {
            return hasMissingClientConfigEntries(content);
        }
        if (kind == ConfigFileKind.SERVER) {
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
        return hasMissingRenderingConfigEntries(content) || hasMissingClientLogicConfigEntries(content);
    }

    private boolean hasMissingRenderingConfigEntries(String content) {
        return content.contains("[overlay]")
            || !content.contains("[profile.locked]")
            || !content.contains("[profile.bypass]")
            || !content.contains("[profile.lockable]")
            || !content.contains("[profile.unlockable]")
            || !content.contains("materialMode")
            || !content.contains("colorMode")
            || !content.contains("materialId")
            || !content.contains("rotationDegrees")
            || !content.contains("zIndex");
    }

    private boolean hasMissingClientLogicConfigEntries(String content) {
        return !content.contains("showVisualFeedback")
            || !content.contains("playSoundFeedback")
            || !content.contains("feedbackSound")
            || !content.contains("feedbackVolume")
            || !content.contains("feedbackPitch")
            || !content.contains("uiTheme");
    }

    private boolean isKnownSection(String section, ConfigFileKind kind) {
        boolean profileSection = section.equals("profile.locked") || section.equals("profile.bypass")
            || section.equals("profile.lockable") || section.equals("profile.unlockable");
        return switch (kind) {
            case SERVER -> isCommonSection(section);
            case CLIENT_RENDERING -> "overlay".equals(section) || profileSection;
            case CLIENT_LOGIC -> "feedback".equals(section);
            case CLIENT_COMBINED -> isClientSection(section) || profileSection;
            case LEGACY -> isCommonSection(section) || isClientSection(section) || profileSection || "keybindings".equals(section);
        };
    }

    private boolean isKnownConfigValue(String section, String key, ConfigFileKind kind) {
        if (!isKnownSection(section, kind)) {
            return false;
        }
        return switch (section) {
            case "profile.locked", "profile.bypass", "profile.lockable", "profile.unlockable" -> switch (key) {
                case "style", "materialMode", "materialId", "color", "opacity", "colorMode", "opacityBehavior", "anchor", "offsetX", "offsetY",
                     "width", "height", "scale", "rotationDegrees", "zIndex", "allowOverflow", "clipToSlot" -> true;
                default -> false;
            };
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

    private String renderClientRenderingConfig(NeoFavoriteItemsConfig config) {
        return "# Neo Favorite Items Client Rendering Configuration\n"
            + "# 新物品收藏模组客户端配置：渲染\n"
            + "# Rendering is client-only; server-side lock rules are still controlled by the common config\n"
            + "# 渲染仅在客户端生效；服务端锁定规则仍由 common 配置控制\n\n"
            + renderProfile("locked", config.overlay.locked)
            + renderProfile("bypass", config.overlay.bypass)
            + renderProfile("lockable", config.overlay.lockable)
            + renderProfile("unlockable", config.overlay.unlockable)
            + renderProfileLayers("locked", config.overlay.lockedLayers)
            + renderProfileLayers("bypass", config.overlay.bypassLayers)
            + renderProfileLayers("lockable", config.overlay.lockableLayers)
            + renderProfileLayers("unlockable", config.overlay.unlockableLayers);
    }

    private static List<OverlayProfileConfig> replaceFirst(List<OverlayProfileConfig> layers, OverlayProfileConfig first) {
        List<OverlayProfileConfig> result = new ArrayList<>(OverlayLayerList.normalize(layers, first::copy));
        result.set(0, first.copy());
        return List.copyOf(result);
    }

    private String renderProfileLayers(String name, List<OverlayProfileConfig> layers) {
        StringBuilder result = new StringBuilder();
        List<OverlayProfileConfig> normalized = OverlayLayerList.normalize(layers, OverlayProfileConfig::defaultLocked);
        for (OverlayProfileConfig profile : normalized) {
            result.append("[[profile.").append(name).append(".layers]]\n")
                .append(renderProfileFields(profile)).append("\n");
        }
        return result.toString();
    }

    private String renderProfileFields(OverlayProfileConfig profile) {
        return "style = \"" + profile.style.name() + "\"\n"
            + "materialMode = \"" + profile.materialMode.name() + "\"\n"
            + "materialId = \"" + escapeConfigString(profile.materialId) + "\"\n"
            + "color = \"" + colorToRgba(profile.color) + "\"\n"
            + "opacity = " + floatToConfig(profile.opacity) + "\n"
            + "colorMode = \"" + profile.colorMode.name() + "\"\n"
            + "opacityBehavior = \"" + profile.opacityBehavior.name() + "\"\n"
            + "anchor = \"" + profile.anchor.name() + "\"\n"
            + "offsetX = " + floatToConfig(profile.offsetX) + "\n"
            + "offsetY = " + floatToConfig(profile.offsetY) + "\n"
            + "width = " + floatToConfig(profile.width) + "\n"
            + "height = " + floatToConfig(profile.height) + "\n"
            + "scale = " + floatToConfig(profile.scale) + "\n"
            + "rotationDegrees = " + floatToConfig(profile.rotationDegrees) + "\n"
            + "zIndex = " + profile.zIndex + "\n"
            + "allowOverflow = " + profile.allowOverflow + "\n"
            + "clipToSlot = " + profile.clipToSlot + "\n";
    }

    private String renderProfile(String name, OverlayProfileConfig profile) {
        return "[profile." + name + "]\n"
            + "style = \"" + profile.style.name() + "\"\n"
            + "materialMode = \"" + profile.materialMode.name() + "\"\n"
            + "materialId = \"" + escapeConfigString(profile.materialId) + "\"\n"
            + "color = \"" + colorToRgba(profile.color) + "\"\n"
            + "opacity = " + floatToConfig(profile.opacity) + "\n"
            + "colorMode = \"" + profile.colorMode.name() + "\"\n"
            + "opacityBehavior = \"" + profile.opacityBehavior.name() + "\"\n"
            + "anchor = \"" + profile.anchor.name() + "\"\n"
            + "offsetX = " + floatToConfig(profile.offsetX) + "\n"
            + "offsetY = " + floatToConfig(profile.offsetY) + "\n"
            + "width = " + floatToConfig(profile.width) + "\n"
            + "height = " + floatToConfig(profile.height) + "\n"
            + "scale = " + floatToConfig(profile.scale) + "\n"
            + "rotationDegrees = " + floatToConfig(profile.rotationDegrees) + "\n"
            + "zIndex = " + profile.zIndex + "\n"
            + "allowOverflow = " + profile.allowOverflow + "\n"
            + "clipToSlot = " + profile.clipToSlot + "\n\n";
    }

    private String renderClientLogicConfig(NeoFavoriteItemsConfig config) {
        String combined = renderClientConfig(config);
        int feedbackSection = combined.indexOf("[feedback]");
        return "# Neo Favorite Items Client Logic Configuration\n"
            + "# 客户端本地反馈和行为配置\n\n"
            + combined.substring(feedbackSection)
            + "uiTheme = \"" + escapeConfigString(config.feedback.uiTheme) + "\"\n";
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

    private String parseConfigString(String value) {
        String clean = value.trim();
        if (clean.length() >= 2 && clean.startsWith("\"") && clean.endsWith("\"")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        return clean.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
