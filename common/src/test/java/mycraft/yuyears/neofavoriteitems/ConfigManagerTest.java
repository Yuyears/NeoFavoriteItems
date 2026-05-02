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

        Path configFile = tempDir.resolve("config").resolve(NeoFavoriteItemsConstants.CONFIG_FILE_NAME);
        assertTrue(Files.exists(configFile));
        assertTrue(Files.readString(configFile, StandardCharsets.UTF_8).contains("锁定操作键"));
        assertTrue(manager.getConfig().general.lockEmptySlots);
        assertFalse(manager.getConfig().general.autoUnlockEmptySlots);
        assertTrue(manager.getConfig().slotBehavior.moveBehavior == NeoFavoriteItemsConfig.SlotMoveBehavior.STAY_AT_POSITION);
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
    }

    @Test
    void rewritesInvalidConfigWhilePreservingReadableValues() throws IOException {
        Path configDir = tempDir.resolve("config-repair");
        Path configFile = configDir.resolve(NeoFavoriteItemsConstants.CONFIG_FILE_NAME);
        Files.createDirectories(configDir);
        Files.writeString(
            configFile,
            """
            [general]
            autoUnlockEmptySlots = true
            lockEmptySlots = maybe

            [lockBehavior]
            preventDrop = false

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

        String repaired = Files.readString(configFile, StandardCharsets.UTF_8);
        assertFalse(manager.getLoadIssues().isEmpty());
        assertTrue(manager.getConfig().general.autoUnlockEmptySlots);
        assertTrue(manager.getConfig().general.lockEmptySlots);
        assertFalse(manager.getConfig().lockBehavior.preventDrop);
        assertEquals(NeoFavoriteItemsConfig.OverlayStyle.LOCK, manager.getConfig().overlay.lockedStyle);
        assertTrue(repaired.contains("autoUnlockEmptySlots = true"));
        assertTrue(repaired.contains("lockEmptySlots = true"));
        assertTrue(repaired.contains("preventDrop = false"));
        assertTrue(repaired.contains("lockedStyle = \"LOCK\""));
        assertTrue(repaired.contains("lockedOverlayOpacity = 0.7"));
        assertFalse(repaired.contains("unknownOverlayOption"));
        assertFalse(repaired.contains("broken line"));
    }
}
