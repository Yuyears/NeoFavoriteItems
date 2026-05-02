package mycraft.yuyears.neofavoriteitems.persistence;

import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConstants;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataPersistenceManagerTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        FavoritesManager.getStateService().clearPlayer();
        FavoritesManager.getStateService().clearFavorites();
    }

    @Test
    void savesAndLoadsRoundTripWithoutLeavingTempFiles() throws IOException {
        UUID playerId = UUID.randomUUID();
        DataPersistenceManager manager = DataPersistenceManager.getInstance();
        manager.initialize(tempDir.resolve("game"), null, false);

        FavoritesManager.getStateService().setFavoriteSlots(Set.of(LogicalSlotIndex.of(5)));
        manager.saveData(playerId);
        FavoritesManager.getStateService().clearFavorites();
        manager.loadData(playerId);

        assertEquals(Set.of(5), FavoritesManager.getStateService().getFavoriteSlots());

        Path saveDirectory = manager.getSavePathForTesting(playerId).getParent();
        assertTrue(Files.list(saveDirectory).noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
    }

    @Test
    void storesClientOnlyDataUnderSanitizedServerAddress() {
        UUID playerId = UUID.randomUUID();
        DataPersistenceManager manager = DataPersistenceManager.getInstance();
        manager.initialize(tempDir.resolve("game-client"), null, false);
        manager.setClientStorageNamespace("example.com:25565");

        Path savePath = manager.getSavePathForTesting(playerId);

        assertTrue(savePath.toString().contains("favoriteitems"));
        assertTrue(savePath.toString().contains("example.com_25565"));
    }

    @Test
    void storesServerAuthoritativeDataInsideWorldDirectory() {
        UUID playerId = UUID.randomUUID();
        Path worldDir = tempDir.resolve("world");
        DataPersistenceManager manager = DataPersistenceManager.getInstance();
        manager.initialize(tempDir.resolve("game-server"), worldDir, true);

        Path savePath = manager.getSavePathForTesting(playerId);

        assertEquals(
            worldDir.resolve("data").resolve("neo_favorite_items").resolve("players").resolve(playerId + ".dat"),
            savePath
        );
    }

    @Test
    void migratesServerDataPreviouslyWrittenUnderGameDirectory() throws IOException {
        UUID playerId = UUID.randomUUID();
        Path gameDir = tempDir.resolve("game-server-legacy-root");
        Path worldDir = gameDir.resolve("saves").resolve("world");
        DataPersistenceManager manager = DataPersistenceManager.getInstance();
        manager.initialize(gameDir, worldDir, true);

        Path legacySavePath = gameDir.resolve("data")
            .resolve("neo_favorite_items")
            .resolve("players")
            .resolve(playerId + ".dat");
        Files.createDirectories(legacySavePath.getParent());
        byte[] payload = "12,40".getBytes(StandardCharsets.UTF_8);
        try (DataOutputStream outputStream = new DataOutputStream(Files.newOutputStream(legacySavePath))) {
            outputStream.writeInt(payload.length);
            outputStream.write(payload);
        }

        manager.loadData(playerId);

        assertEquals(Set.of(12, 40), FavoritesManager.getStateService().getFavoriteSlots());
        assertTrue(Files.notExists(legacySavePath));
        assertTrue(Files.exists(manager.getSavePathForTesting(playerId)));
    }

    @Test
    void clearingServerDataAlsoRemovesLegacyGameDirectoryData() throws IOException {
        UUID playerId = UUID.randomUUID();
        Path gameDir = tempDir.resolve("game-server-clear-legacy-root");
        Path worldDir = gameDir.resolve("saves").resolve("world");
        DataPersistenceManager manager = DataPersistenceManager.getInstance();
        manager.initialize(gameDir, worldDir, true);

        Path legacySavePath = gameDir.resolve("data")
            .resolve("neo_favorite_items")
            .resolve("players")
            .resolve(playerId + ".dat");
        Files.createDirectories(legacySavePath.getParent());
        byte[] payload = "4".getBytes(StandardCharsets.UTF_8);
        try (DataOutputStream outputStream = new DataOutputStream(Files.newOutputStream(legacySavePath))) {
            outputStream.writeInt(payload.length);
            outputStream.write(payload);
        }

        manager.clearData(playerId);

        assertTrue(Files.notExists(legacySavePath));
    }

    @Test
    void nonEmptyLegacyServerDataRepairsEmptyWorldDataThenDeletesLegacyFile() throws IOException {
        UUID playerId = UUID.randomUUID();
        Path gameDir = tempDir.resolve("game-server-repair-empty-world");
        Path worldDir = gameDir.resolve("saves").resolve("world");
        DataPersistenceManager manager = DataPersistenceManager.getInstance();
        manager.initialize(gameDir, worldDir, true);

        writeFavoritePayload(manager.getSavePathForTesting(playerId), "");
        Path legacySavePath = gameDir.resolve("data")
            .resolve("neo_favorite_items")
            .resolve("players")
            .resolve(playerId + ".dat");
        writeFavoritePayload(legacySavePath, "6,13");

        manager.loadData(playerId);

        assertEquals(Set.of(6, 13), FavoritesManager.getStateService().getFavoriteSlots());
        assertTrue(Files.notExists(legacySavePath));
    }

    @Test
    void existingWorldDataWinsOverLegacyServerDataAndDeletesLegacyFile() throws IOException {
        UUID playerId = UUID.randomUUID();
        Path gameDir = tempDir.resolve("game-server-world-wins");
        Path worldDir = gameDir.resolve("saves").resolve("world");
        DataPersistenceManager manager = DataPersistenceManager.getInstance();
        manager.initialize(gameDir, worldDir, true);

        writeFavoritePayload(manager.getSavePathForTesting(playerId), "2");
        Path legacySavePath = gameDir.resolve("data")
            .resolve("neo_favorite_items")
            .resolve("players")
            .resolve(playerId + ".dat");
        writeFavoritePayload(legacySavePath, "6,13");

        manager.loadData(playerId);

        assertEquals(Set.of(2), FavoritesManager.getStateService().getFavoriteSlots());
        assertTrue(Files.notExists(legacySavePath));
    }

    @Test
    void loadsLegacyClientDirectoryWhenNewDirectoryIsAbsent() throws IOException {
        UUID playerId = UUID.randomUUID();
        DataPersistenceManager manager = DataPersistenceManager.getInstance();
        Path gameDir = tempDir.resolve("game-legacy");
        manager.initialize(gameDir, null, false);
        manager.setClientStorageNamespace("legacy.server");

        Path legacySavePath = gameDir.resolve(NeoFavoriteItemsConstants.LEGACY_CLIENT_SAVE_DIRECTORY)
            .resolve("legacy.server")
            .resolve(NeoFavoriteItemsConstants.PLAYER_DATA_DIRECTORY)
            .resolve(playerId + ".dat");
        Files.createDirectories(legacySavePath.getParent());
        byte[] payload = "8".getBytes(StandardCharsets.UTF_8);
        try (DataOutputStream outputStream = new DataOutputStream(Files.newOutputStream(legacySavePath))) {
            outputStream.writeInt(payload.length);
            outputStream.write(payload);
        }

        manager.loadData(playerId);

        assertEquals(Set.of(8), FavoritesManager.getStateService().getFavoriteSlots());
        assertTrue(Files.notExists(legacySavePath));
        assertTrue(Files.exists(manager.getSavePathForTesting(playerId)));
    }

    @Test
    void preloadAndSaveAllRoundTripCachedPlayerData() throws IOException {
        UUID playerId = UUID.randomUUID();
        Path worldDir = tempDir.resolve("world-cache");
        DataPersistenceManager manager = DataPersistenceManager.getInstance();
        manager.initialize(tempDir.resolve("game-cache"), worldDir, true);

        Path savePath = manager.getSavePathForTesting(playerId);
        Files.createDirectories(savePath.getParent());
        byte[] initialPayload = "3,7".getBytes(StandardCharsets.UTF_8);
        try (DataOutputStream outputStream = new DataOutputStream(Files.newOutputStream(savePath))) {
            outputStream.writeInt(initialPayload.length);
            outputStream.write(initialPayload);
        }

        manager.loadAllData();
        FavoritesManager.getStateService().setPlayer(playerId);
        manager.loadData(playerId);
        assertEquals(Set.of(3, 7), FavoritesManager.getStateService().getFavoriteSlots());

        FavoritesManager.getStateService().setFavoriteSlots(Set.of(LogicalSlotIndex.of(9)));
        manager.saveData(playerId);
        manager.saveAllData();

        FavoritesManager.getStateService().clearFavorites();
        manager.loadData(playerId);
        assertEquals(Set.of(9), FavoritesManager.getStateService().getFavoriteSlots());
    }

    @Test
    void cacheDataUpdatesMemoryWithoutWritingPlayerFileUntilFlush() throws IOException {
        UUID playerId = UUID.randomUUID();
        Path worldDir = tempDir.resolve("world-cache-only");
        DataPersistenceManager manager = DataPersistenceManager.getInstance();
        manager.initialize(tempDir.resolve("game-cache-only"), worldDir, true);

        FavoritesManager.getStateService().setPlayer(playerId);
        FavoritesManager.getStateService().setFavoriteSlots(Set.of(LogicalSlotIndex.of(11)));
        Path savePath = manager.getSavePathForTesting(playerId);

        manager.cacheData(playerId);

        assertTrue(Files.notExists(savePath));
        FavoritesManager.getStateService().clearFavorites();
        manager.loadData(playerId);
        assertEquals(Set.of(11), FavoritesManager.getStateService().getFavoriteSlots());

        manager.saveAllData();

        assertTrue(Files.exists(savePath));
    }

    @Test
    void saveDataKeepsPlayerCacheForServerStopFlush() throws IOException {
        UUID playerId = UUID.randomUUID();
        Path worldDir = tempDir.resolve("world-save-keep-cache");
        DataPersistenceManager manager = DataPersistenceManager.getInstance();
        manager.initialize(tempDir.resolve("game-save-keep-cache"), worldDir, true);

        FavoritesManager.getStateService().setPlayer(playerId);
        FavoritesManager.getStateService().setFavoriteSlots(Set.of(LogicalSlotIndex.of(4)));
        Path savePath = manager.getSavePathForTesting(playerId);

        manager.saveData(playerId);
        writeFavoritePayload(savePath, "8");

        FavoritesManager.getStateService().clearFavorites();
        manager.loadData(playerId);

        assertEquals(Set.of(4), FavoritesManager.getStateService().getFavoriteSlots());
    }

    @Test
    void ignoresCorruptLengthHeaders() throws IOException {
        UUID playerId = UUID.randomUUID();
        DataPersistenceManager manager = DataPersistenceManager.getInstance();
        manager.initialize(tempDir.resolve("game-corrupt"), null, false);

        FavoritesManager.getStateService().setFavoriteSlots(Set.of(LogicalSlotIndex.of(9)));
        Path savePath = manager.getSavePathForTesting(playerId);
        Files.createDirectories(savePath.getParent());
        try (DataOutputStream outputStream = new DataOutputStream(Files.newOutputStream(savePath))) {
            outputStream.writeInt(9999);
            outputStream.writeByte(1);
        }

        manager.loadData(playerId);
        assertEquals(Set.of(9), FavoritesManager.getStateService().getFavoriteSlots());
    }

    private void writeFavoritePayload(Path savePath, String payloadText) throws IOException {
        Files.createDirectories(savePath.getParent());
        byte[] payload = payloadText.getBytes(StandardCharsets.UTF_8);
        try (DataOutputStream outputStream = new DataOutputStream(Files.newOutputStream(savePath))) {
            outputStream.writeInt(payload.length);
            outputStream.write(payload);
        }
    }
}
