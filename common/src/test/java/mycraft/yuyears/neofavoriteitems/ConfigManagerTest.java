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
    @TempDir
    Path tempDir;

    @Test
    void writesDefaultConfigUsingUtf8AndAlignedDefaults() throws IOException {
        ConfigManager manager = ConfigManager.getInstance();
        manager.initialize(tempDir.resolve("config"));

        Path commonConfigFile = tempDir.resolve("config").resolve(NeoFavoriteItemsConstants.COMMON_CONFIG_FILE_NAME);
        Path clientConfigFile = tempDir.resolve("config").resolve(NeoFavoriteItemsConstants.CLIENT_CONFIG_FILE_NAME);
        Path legacyConfigFile = tempDir.resolve("config").resolve(NeoFavoriteItemsConstants.CONFIG_FILE_NAME);
        assertTrue(Files.exists(commonConfigFile));
        assertTrue(Files.exists(clientConfigFile));
        assertFalse(Files.exists(legacyConfigFile));
        assertTrue(Files.readString(commonConfigFile, StandardCharsets.UTF_8).contains("通用配置"));
        assertTrue(Files.readString(clientConfigFile, StandardCharsets.UTF_8).contains("客户端配置"));
        assertTrue(manager.getConfig().general.lockEmptySlots);
        assertFalse(manager.getConfig().general.autoUnlockEmptySlots);
        assertTrue(manager.getConfig().slotBehavior.moveBehavior == NeoFavoriteItemsConfig.SlotMoveBehavior.STAY_AT_POSITION);
        assertFalse(manager.getConfig().deathBehavior.preserveLockedSlotContents);
        assertFalse(Files.readString(clientConfigFile, StandardCharsets.UTF_8).contains("renderForegroundContrastBackdrop"));
        String commonConfig = Files.readString(commonConfigFile, StandardCharsets.UTF_8);
        String clientConfig = Files.readString(clientConfigFile, StandardCharsets.UTF_8);
        assertTrue(commonConfig.contains("[deathBehavior]"));
        assertTrue(commonConfig.contains("preserveLockedSlotContents = false"));
        assertTrue(commonConfig.contains("picked-up ground items skip locked empty main-inventory slots"));
        assertTrue(commonConfig.contains("拾取地面掉落物会在物品被消耗前跳过已锁定空主背包槽"));
        assertTrue(commonConfig.contains("death preservation is configured in [deathBehavior]"));
        assertTrue(commonConfig.contains("死亡保留由 [deathBehavior] 配置"));
        assertTrue(clientConfig.contains("server-side lock rules are still controlled by the common config"));
        assertTrue(clientConfig.contains("服务端锁定规则仍由 common 配置控制"));
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
        assertTrue(repairedClient.contains("lockedStyle = \"LOCK\""));
        assertTrue(repairedClient.contains("lockedOverlayOpacity = 0.7"));
        assertFalse(repairedClient.contains("renderForegroundContrastBackdrop"));
        assertFalse(repairedClient.contains("unknownOverlayOption"));
        assertFalse(repairedCommon.contains("broken line"));
        assertFalse(repairedClient.contains("broken line"));
    }

    @Test
    void deletesStaleLegacyConfigWhenSplitFilesAlreadyExist() throws IOException {
        Path configDir = tempDir.resolve("config-stale-legacy");
        Files.createDirectories(configDir);
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
            feedbackSound = "minecraft:block.note_block.hat"
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
