package mycraft.yuyears.neofavoriteitems;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {
    @Test
    void rgbaIntegerOneIsOneByteNotNormalizedWhite() {
        assertEquals(0xB201960B, ConfigManager.parseColorValue("rgba(1,150,11,178)"));
        assertEquals(0x80FF0000, ConfigManager.parseColorValue("rgba(1.0,0,0,0.5)"));
    }

    @TempDir
    Path tempDir;

    @Test
    void dirtyDraftOnlyReportsExternalChangeWhenFilesActuallyChanged() throws Exception {
        Path configDir = tempDir.resolve("config-dirty-draft");
        ConfigManager manager = ConfigManager.getInstance();
        manager.initialize(configDir);
        manager.setDraftDirty(true);

        Thread.sleep(550L);
        assertFalse(manager.reloadIfChanged());
        assertFalse(manager.isExternalChangeDetected());

        Path rendering = configDir.resolve(NeoFavoriteItemsConstants.CLIENT_CONFIG_FILE_NAME);
        Files.writeString(rendering, "\n# external edit\n", StandardCharsets.UTF_8,
            java.nio.file.StandardOpenOption.APPEND);
        Thread.sleep(550L);
        assertFalse(manager.reloadIfChanged());
        assertTrue(manager.isExternalChangeDetected());
        manager.keepDraftAfterExternalChange();
        assertFalse(manager.isExternalChangeDetected());
        assertTrue(manager.isDraftDirty());
        manager.setDraftDirty(false);
    }

    @Test
    void normalizesOverlayZIndexWithoutRounding() {
        assertEquals(0, ConfigManager.normalizeOverlayZIndex(-4.2));
        assertEquals(0, ConfigManager.normalizeOverlayZIndex(0.9));
        assertEquals(2, ConfigManager.normalizeOverlayZIndex(1.9));
        assertEquals(8, ConfigManager.normalizeOverlayZIndex(8.99));
        assertEquals(1000, ConfigManager.normalizeOverlayZIndex(5000.0));
    }

    @Test
    void rewritesNormalizedOverlayZIndexAtConfigBoundary() throws IOException {
        Path configDir = tempDir.resolve("config-z-index");
        ConfigManager manager = ConfigManager.getInstance();
        manager.initialize(configDir);
        Path rendering = configDir.resolve(NeoFavoriteItemsConstants.CLIENT_CONFIG_FILE_NAME);
        String content = Files.readString(rendering, StandardCharsets.UTF_8)
            .replaceFirst("zIndex = 2", "zIndex = -4.2");
        Files.writeString(rendering, content, StandardCharsets.UTF_8);

        manager.reload();

        assertEquals(0, manager.getConfig().overlay.locked.zIndex);
        assertTrue(Files.readString(rendering, StandardCharsets.UTF_8).contains("zIndex = 0"));
    }

    @Test
    void writesDefaultConfigUsingUtf8AndAlignedDefaults() throws IOException {
        ConfigManager manager = ConfigManager.getInstance();
        manager.initialize(tempDir.resolve("config"));

        Path commonConfigFile = tempDir.resolve("config").resolve(NeoFavoriteItemsConstants.COMMON_CONFIG_FILE_NAME);
        Path clientConfigFile = tempDir.resolve("config").resolve(NeoFavoriteItemsConstants.CLIENT_CONFIG_FILE_NAME);
        Path clientLogicConfigFile = tempDir.resolve("config").resolve(NeoFavoriteItemsConstants.CLIENT_LOGIC_CONFIG_FILE_NAME);
        Path legacyConfigFile = tempDir.resolve("config").resolve(NeoFavoriteItemsConstants.CONFIG_FILE_NAME);
        assertTrue(Files.exists(commonConfigFile));
        assertTrue(Files.exists(clientConfigFile));
        assertTrue(Files.exists(clientLogicConfigFile));
        assertFalse(Files.exists(legacyConfigFile));
        try (var files = Files.list(clientConfigFile.getParent())) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
        assertTrue(Files.readString(commonConfigFile, StandardCharsets.UTF_8).contains("通用配置"));
        assertTrue(Files.readString(clientConfigFile, StandardCharsets.UTF_8).contains("客户端配置"));
        assertTrue(manager.getConfig().general.lockEmptySlots);
        assertFalse(manager.getConfig().general.autoUnlockEmptySlots);
        assertEquals("minecraft:block.chain.break", manager.getConfig().feedback.feedbackSound);
        assertEquals(0.25f, manager.getConfig().feedback.feedbackVolume);
        assertEquals(1.5f, manager.getConfig().feedback.feedbackPitch);
        assertTrue(manager.getConfig().slotBehavior.moveBehavior == NeoFavoriteItemsConfig.SlotMoveBehavior.STAY_AT_POSITION);
        assertFalse(manager.getConfig().deathBehavior.preserveLockedSlotContents);
        assertFalse(Files.readString(clientConfigFile, StandardCharsets.UTF_8).contains("renderForegroundContrastBackdrop"));
        String commonConfig = Files.readString(commonConfigFile, StandardCharsets.UTF_8);
        String clientConfig = Files.readString(clientConfigFile, StandardCharsets.UTF_8);
        String clientLogicConfig = Files.readString(clientLogicConfigFile, StandardCharsets.UTF_8);
        assertTrue(commonConfig.contains("[deathBehavior]"));
        assertTrue(commonConfig.contains("preserveLockedSlotContents = false"));
        assertTrue(commonConfig.contains("picked-up ground items skip locked empty main-inventory slots"));
        assertTrue(commonConfig.contains("拾取地面掉落物会在物品被消耗前跳过已锁定空主背包槽"));
        assertTrue(commonConfig.contains("death preservation is configured in [deathBehavior]"));
        assertTrue(commonConfig.contains("死亡保留由 [deathBehavior] 配置"));
        assertTrue(clientConfig.contains("server-side lock rules are still controlled by the common config"));
        assertTrue(clientConfig.contains("服务端锁定规则仍由 common 配置控制"));
        assertTrue(clientConfig.contains("[profile.locked]"));
        assertTrue(clientConfig.contains("colorMode = \"NATIVE\""));
        assertTrue(clientConfig.contains("scale = 0.5"));
        assertFalse(clientConfig.contains("[overlay]"));
        assertFalse(clientConfig.contains("lockedStyle"));
        assertFalse(clientConfig.contains("[feedback]"));
        assertTrue(clientLogicConfig.contains("[feedback]"));
        assertFalse(clientLogicConfig.contains("[overlay]"));
    }

    @Test
    void roundTripsIndependentProfileRenderingValues() {
        Path configDir = tempDir.resolve("config-profiles");
        ConfigManager manager = ConfigManager.getInstance();
        manager.initialize(configDir);
        var bypass = manager.getConfig().overlay.bypass;
        bypass.colorMode = mycraft.yuyears.neofavoriteitems.render.OverlayColorMode.NATIVE;
        bypass.materialMode = mycraft.yuyears.neofavoriteitems.render.OverlayMaterialMode.NO_MATERIAL;
        bypass.materialId = mycraft.yuyears.neofavoriteitems.render.OverlayTextureCatalog.NO_MATERIAL;
        bypass.materialId = "custom:wide image.png";
        bypass.opacityBehavior = OverlayProfileConfig.OpacityBehavior.FIXED;
        bypass.opacity = 0.42f;
        bypass.offsetX = -2.5f;
        bypass.width = 23.0f;
        bypass.height = 11.0f;
        bypass.scale = 1.25f;
        bypass.rotationDegrees = 37.0f;
        bypass.zIndex = 125;
        bypass.allowOverflow = true;
        manager.saveConfig();

        manager.reload();
        bypass = manager.getConfig().overlay.bypass;
        assertEquals(mycraft.yuyears.neofavoriteitems.render.OverlayColorMode.NATIVE, bypass.colorMode);
        assertEquals(mycraft.yuyears.neofavoriteitems.render.OverlayMaterialMode.NO_MATERIAL, bypass.materialMode);
        assertEquals("custom:wide image.png", bypass.materialId);
        assertEquals(OverlayProfileConfig.OpacityBehavior.FIXED, bypass.opacityBehavior);
        assertEquals(0.42f, bypass.opacity);
        assertEquals(-2.5f, bypass.offsetX);
        assertEquals(23.0f, bypass.width);
        assertEquals(11.0f, bypass.height);
        assertEquals(1.25f, bypass.scale);
        assertEquals(37.0f, bypass.rotationDegrees);
        assertEquals(125, bypass.zIndex);
        assertTrue(bypass.allowOverflow);
    }

    @Test
    void normalizesInvalidProfileGeometryAtConfigBoundary() throws IOException {
        Path configDir = tempDir.resolve("config-profile-normalization");
        ConfigManager manager = ConfigManager.getInstance();
        manager.initialize(configDir);
        Path rendering = configDir.resolve(NeoFavoriteItemsConstants.CLIENT_CONFIG_FILE_NAME);
        String content = Files.readString(rendering, StandardCharsets.UTF_8)
            .replaceFirst("opacity = 0.7", "opacity = 3.5")
            .replaceFirst("width = 16.0", "width = -2.0")
            .replaceFirst("offsetX = 0.0", "offsetX = NaN");
        Files.writeString(rendering, content, StandardCharsets.UTF_8);

        manager.reload();

        assertEquals(1.0f, manager.getConfig().overlay.locked.opacity);
        assertEquals(16.0f, manager.getConfig().overlay.locked.width);
        assertEquals(0.0f, manager.getConfig().overlay.locked.offsetX);
        String normalized = Files.readString(rendering, StandardCharsets.UTF_8);
        assertTrue(normalized.contains("opacity = 1.0"));
        assertTrue(normalized.contains("width = 16.0"));
        assertTrue(normalized.contains("offsetX = 0.0"));
    }

    @Test
    void recordsInvalidEntriesAndKeepsDefaults() throws IOException {
        Path configDir = tempDir.resolve("config-invalid");
        Files.createDirectories(configDir);
        Files.writeString(
            configDir.resolve(NeoFavoriteItemsConstants.CONFIG_FILE_NAME),
            """
            [slotBehavior]
            moveBehavior = "NOT_A_REAL_MODE"

            [overlay]
            lockedStyle = "BAD_STYLE"
            lockedOverlayOpacity = nope
            """,
            StandardCharsets.UTF_8
        );

        ConfigManager manager = ConfigManager.getInstance();
        manager.initialize(configDir);

        assertFalse(manager.getLoadIssues().isEmpty());
        assertTrue(manager.getConfig().slotBehavior.moveBehavior == NeoFavoriteItemsConfig.SlotMoveBehavior.STAY_AT_POSITION);
        assertTrue(manager.getConfig().overlay.lockedStyle == NeoFavoriteItemsConfig.OverlayStyle.MARK);
        assertFalse(Files.exists(configDir.resolve(NeoFavoriteItemsConstants.CONFIG_FILE_NAME)));
    }

    @Test
    void migratesLegacyConfigIntoSplitFilesWhilePreservingReadableValues() throws IOException {
        Path configDir = tempDir.resolve("config-repair");
        Path legacyConfigFile = configDir.resolve(NeoFavoriteItemsConstants.CONFIG_FILE_NAME);
        Path commonConfigFile = configDir.resolve(NeoFavoriteItemsConstants.COMMON_CONFIG_FILE_NAME);
        Path clientConfigFile = configDir.resolve(NeoFavoriteItemsConstants.CLIENT_CONFIG_FILE_NAME);
        Files.createDirectories(configDir);
        Files.writeString(
            legacyConfigFile,
            """
            [general]
            autoUnlockEmptySlots = true
            lockEmptySlots = maybe

            [lockBehavior]
            preventDrop = false

            [deathBehavior]
            preserveLockedSlotContents = true

            [overlay]
            lockedStyle = "LOCK"
            lockedOverlayOpacity = nope
            unknownOverlayOption = 1

            broken line
            """,
            StandardCharsets.UTF_8
        );

        ConfigManager manager = ConfigManager.getInstance();
        manager.initialize(configDir);

        String repairedCommon = Files.readString(commonConfigFile, StandardCharsets.UTF_8);
        String repairedClient = Files.readString(clientConfigFile, StandardCharsets.UTF_8);
        assertFalse(manager.getLoadIssues().isEmpty());
        assertTrue(manager.getConfig().general.autoUnlockEmptySlots);
        assertTrue(manager.getConfig().general.lockEmptySlots);
        assertFalse(manager.getConfig().lockBehavior.preventDrop);
        assertTrue(manager.getConfig().deathBehavior.preserveLockedSlotContents);
        assertEquals(NeoFavoriteItemsConfig.OverlayStyle.LOCK, manager.getConfig().overlay.lockedStyle);
        assertFalse(Files.exists(legacyConfigFile));
        assertTrue(repairedCommon.contains("autoUnlockEmptySlots = true"));
        assertTrue(repairedCommon.contains("lockEmptySlots = true"));
        assertTrue(repairedCommon.contains("preventDrop = false"));
        assertTrue(repairedCommon.contains("preserveLockedSlotContents = true"));
        assertTrue(repairedClient.contains("[profile.locked]"));
        assertTrue(repairedClient.contains("style = \"LOCK\""));
        assertTrue(repairedClient.contains("opacity = 1.0"));
        assertFalse(repairedClient.contains("renderForegroundContrastBackdrop"));
        assertFalse(repairedClient.contains("unknownOverlayOption"));
        assertFalse(repairedCommon.contains("broken line"));
        assertFalse(repairedClient.contains("broken line"));
    }

    @Test
    void deletesStaleLegacyConfigWhenSplitFilesAlreadyExist() throws IOException {
        Path configDir = tempDir.resolve("config-stale-legacy");
        Files.createDirectories(configDir.resolve(NeoFavoriteItemsConstants.COMMON_CONFIG_FILE_NAME).getParent());
        Files.createDirectories(configDir.resolve(NeoFavoriteItemsConstants.CLIENT_CONFIG_FILE_NAME).getParent());
        Files.writeString(
            configDir.resolve(NeoFavoriteItemsConstants.COMMON_CONFIG_FILE_NAME),
            """
            [general]
            autoUnlockEmptySlots = false
            lockEmptySlots = true
            allowItemsIntoLockedEmptySlots = false

            [lockBehavior]
            preventClick = true
            preventDrop = true
            preventQuickMove = true
            preventShiftClick = true
            preventDrag = true
            preventSwap = true
            allowBypassWithKey = true

            [slotBehavior]
            moveBehavior = "STAY_AT_POSITION"

            [deathBehavior]
            preserveLockedSlotContents = false

            [debug]
            enabled = false
            """,
            StandardCharsets.UTF_8
        );
        Files.writeString(
            configDir.resolve(NeoFavoriteItemsConstants.CLIENT_CONFIG_FILE_NAME),
            """
            [overlay]
            lockedStyle = "MARK"
            holdingKeyLockedStyle = "MARK"
            highlightStyle = "BORDER"
            lockedOverlayColor = "rgba(255,65,60,250)"
            lockedOverlayOpacity = 0.7
            lockableHighlightColor = "rgba(35,230,0,200)"
            lockableHighlightOpacity = 0.55
            unlockableHighlightColor = "rgba(255,195,53,180)"
            unlockableHighlightOpacity = 0.65
            colorOverlayOpacity = 0.35
            bypassOverlayOpacityMultiplier = 0.35
            renderLockedOverlayInFront = true
            renderLockableHighlightInFront = true
            renderUnlockableHighlightInFront = true

            [feedback]
            showVisualFeedback = true
            playSoundFeedback = true
            feedbackSound = "minecraft:block.chain.break"
            feedbackVolume = 0.5
            feedbackPitch = 1.0
            """,
            StandardCharsets.UTF_8
        );
        Files.writeString(
            configDir.resolve(NeoFavoriteItemsConstants.CONFIG_FILE_NAME),
            """
            [general]
            autoUnlockEmptySlots = true
            """,
            StandardCharsets.UTF_8
        );

        ConfigManager manager = ConfigManager.getInstance();
        manager.initialize(configDir);

        assertFalse(manager.getConfig().general.autoUnlockEmptySlots);
        assertFalse(Files.exists(configDir.resolve(NeoFavoriteItemsConstants.CONFIG_FILE_NAME)));
    }
}
