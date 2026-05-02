package mycraft.yuyears.neofavoriteitems.persistence;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConstants;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class DataPersistenceManager {
    private static DataPersistenceManager instance;

    private final Map<UUID, byte[]> cachedPlayerData = new ConcurrentHashMap<>();
    private Path saveDirectory;
    private Path legacySaveDirectory;
    private Path legacyServerStorageRoot;
    private Path worldSaveDirectory;
    private boolean isServerSide;
    private String clientStorageNamespace = NeoFavoriteItemsConstants.DEFAULT_SERVER_DIRECTORY;
    private Path activeStorageRoot;

    private DataPersistenceManager() {}

    public static DataPersistenceManager getInstance() {
        if (instance == null) {
            instance = new DataPersistenceManager();
        }
        return instance;
    }

    public synchronized void initialize(Path gameDirectory, Path worldDirectory, boolean isServerSide) {
        this.saveDirectory = gameDirectory.resolve(NeoFavoriteItemsConstants.CLIENT_SAVE_DIRECTORY);
        this.legacySaveDirectory = gameDirectory.resolve(NeoFavoriteItemsConstants.LEGACY_CLIENT_SAVE_DIRECTORY);
        this.legacyServerStorageRoot = isServerSide && worldDirectory != null
            ? gameDirectory.resolve("data").resolve(NeoFavoriteItemsMod.MOD_ID)
            : null;
        this.worldSaveDirectory = worldDirectory;
        this.isServerSide = isServerSide;
        this.clientStorageNamespace = NeoFavoriteItemsConstants.DEFAULT_SERVER_DIRECTORY;
        resetStorageContext();

        DebugLogger.debug(
            "Persistence initialized: side={} gameDirectory={} worldDirectory={} storageRoot={} legacyServerRoot={}",
            isServerSide ? "server" : "client",
            gameDirectory,
            worldDirectory,
            resolveStorageRoot(),
            legacyServerStorageRoot
        );

        try {
            Files.createDirectories(saveDirectory);
        } catch (IOException e) {
            DebugLogger.error("Failed to create favorite save directory: {}", saveDirectory);
            DebugLogger.error("Favorite save directory initialization failure", e);
        }
    }

    public synchronized void loadAllData() {
        refreshStorageContext();
        cachedPlayerData.clear();

        Path playerDirectory = getPlayerDirectory();
        DebugLogger.debug("Persistence preload started: playerDirectory={}", playerDirectory);
        if (!Files.isDirectory(playerDirectory)) {
            DebugLogger.debug("Persistence preload skipped: playerDirectory_missing path={}", playerDirectory);
            return;
        }

        try (Stream<Path> playerFiles = Files.list(playerDirectory)) {
            playerFiles
                .filter(path -> path.getFileName().toString().endsWith(".dat"))
                .forEach(this::loadCachedEntry);
        } catch (IOException e) {
            DebugLogger.error("Failed to preload favorite data from directory: {}", playerDirectory);
            DebugLogger.error("Favorite data preload failure", e);
        }
    }

    public synchronized void saveAllData() {
        refreshStorageContext();
        for (Map.Entry<UUID, byte[]> entry : cachedPlayerData.entrySet()) {
            writeData(getSavePath(entry.getKey()), entry.getValue());
        }
    }

    public synchronized void cacheData(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }

        refreshStorageContext();
        byte[] data = FavoritesManager.getCodec().serialize();
        cachedPlayerData.put(playerUUID, data);
        DebugLogger.debug(
            "Persistence cache player: uuid={} slots={} payloadLength={}",
            playerUUID,
            describePayload(data),
            data.length
        );
    }

    public synchronized void saveData(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }

        refreshStorageContext();
        byte[] data = cacheCurrentData(playerUUID);
        Path savePath = getSavePath(playerUUID);
        logSave(playerUUID, savePath, data);
        writeData(savePath, data);
    }

    public synchronized void loadData(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }

        refreshStorageContext();
        byte[] data = cachedPlayerData.get(playerUUID);
        if (data == null) {
            Path currentSavePath = getSavePath(playerUUID);
            DebugLogger.debug("Persistence load player: uuid={} currentPath={}", playerUUID, currentSavePath);
            data = readData(currentSavePath);
            if (isServerSide && legacyServerStorageRoot != null) {
                Path legacyServerSavePath = getLegacyServerSavePath(playerUUID);
                byte[] legacyServerData = readData(legacyServerSavePath);
                if (legacyServerData != null) {
                    if (data == null || (isEmptyFavoriteData(data) && !isEmptyFavoriteData(legacyServerData))) {
                        data = legacyServerData;
                        writeData(currentSavePath, data);
                        DebugLogger.debug(
                            "Persistence migrated legacy server data: uuid={} legacyPath={} currentPath={} slots={}",
                            playerUUID,
                            legacyServerSavePath,
                            currentSavePath,
                            describePayload(data)
                        );
                    }
                    deleteIfExists(legacyServerSavePath);
                    DebugLogger.debug("Persistence removed legacy server data: uuid={} legacyPath={}", playerUUID, legacyServerSavePath);
                }
            }
            if (data == null && worldSaveDirectory == null) {
                Path legacySavePath = getLegacyClientSavePath(playerUUID);
                data = readData(legacySavePath);
                if (data != null) {
                    writeData(currentSavePath, data);
                    deleteIfExists(legacySavePath);
                    DebugLogger.debug(
                        "Persistence migrated legacy client data: uuid={} legacyPath={} currentPath={} slots={}",
                        playerUUID,
                        legacySavePath,
                        currentSavePath,
                        describePayload(data)
                    );
                }
            }
            if (data != null) {
                cachedPlayerData.put(playerUUID, data);
            }
        }

        if (data != null) {
            FavoritesManager.getCodec().deserialize(data);
            DebugLogger.debug(
                "Persistence loaded player: uuid={} slots={} payloadLength={}",
                playerUUID,
                FavoritesManager.getStateService().getFavoriteSlots(),
                data.length
            );
        } else {
            DebugLogger.debug("Persistence load missed: uuid={} storageRoot={}", playerUUID, resolveStorageRoot());
        }
    }

    public synchronized void clearData(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }

        refreshStorageContext();
        cachedPlayerData.remove(playerUUID);
        deleteIfExists(getSavePath(playerUUID));
        if (worldSaveDirectory == null) {
            deleteIfExists(getLegacyClientSavePath(playerUUID));
        } else if (isServerSide && legacyServerStorageRoot != null) {
            deleteIfExists(getLegacyServerSavePath(playerUUID));
        }
    }

    public synchronized boolean hasData(UUID playerUUID) {
        if (playerUUID == null) {
            return false;
        }

        refreshStorageContext();
        return cachedPlayerData.containsKey(playerUUID)
            || Files.exists(getSavePath(playerUUID))
            || (isServerSide && legacyServerStorageRoot != null && Files.exists(getLegacyServerSavePath(playerUUID)))
            || (worldSaveDirectory == null && Files.exists(getLegacyClientSavePath(playerUUID)));
    }

    public synchronized void setWorldSaveDirectory(Path worldDirectory) {
        this.worldSaveDirectory = worldDirectory;
        resetStorageContext();
    }

    public synchronized void setClientStorageNamespace(String namespace) {
        this.clientStorageNamespace = sanitizeClientStorageNamespace(namespace);
        resetStorageContext();
    }

    Path getSavePathForTesting(UUID playerUUID) {
        refreshStorageContext();
        return getSavePath(playerUUID);
    }

    String sanitizeClientStorageNamespaceForTesting(String namespace) {
        return sanitizeClientStorageNamespace(namespace);
    }

    private void loadCachedEntry(Path savePath) {
        String fileName = savePath.getFileName().toString();
        String uuidText = fileName.substring(0, fileName.length() - 4);
        try {
            UUID playerUUID = UUID.fromString(uuidText);
            byte[] data = readData(savePath);
            if (data != null) {
                cachedPlayerData.put(playerUUID, data);
                DebugLogger.debug("Persistence preloaded cached player: uuid={} path={} slots={}", playerUUID, savePath, describePayload(data));
            }
        } catch (IllegalArgumentException exception) {
            DebugLogger.warn("Ignored favorite data file with invalid UUID name: {}", savePath);
        }
    }

    private byte[] cacheCurrentData(UUID playerUUID) {
        byte[] data = FavoritesManager.getCodec().serialize();
        cachedPlayerData.put(playerUUID, data);
        return data;
    }

    private void logSave(UUID playerUUID, Path savePath, byte[] data) {
        DebugLogger.debug(
            "Persistence save player: uuid={} path={} slots={} payloadLength={}",
            playerUUID,
            savePath,
            describePayload(data),
            data.length
        );
    }

    private boolean writeData(Path savePath, byte[] data) {
        try {
            Files.createDirectories(savePath.getParent());

            Path tempPath = Files.createTempFile(savePath.getParent(), savePath.getFileName().toString(), ".tmp");
            try (OutputStream fileOutputStream = Files.newOutputStream(tempPath);
                 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                 DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream)) {

                dataOutputStream.writeInt(data.length);
                dataOutputStream.write(data);
                dataOutputStream.flush();
            } catch (IOException exception) {
                Files.deleteIfExists(tempPath);
                throw exception;
            }

            try {
                Files.move(tempPath, savePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveException) {
                Files.move(tempPath, savePath, StandardCopyOption.REPLACE_EXISTING);
                DebugLogger.warn("Atomic move unavailable for favorite data file: {}", savePath);
            }
            return true;
        } catch (IOException e) {
            DebugLogger.error("Failed to save favorite data: {}", savePath);
            DebugLogger.error("Favorite data save failure", e);
            return false;
        }
    }

    private byte[] readData(Path savePath) {
        if (!Files.exists(savePath)) {
            return null;
        }

        try (InputStream fileInputStream = Files.newInputStream(savePath);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
             DataInputStream dataInputStream = new DataInputStream(bufferedInputStream)) {

            int length = dataInputStream.readInt();
            long maxExpectedLength = Math.max(0L, Files.size(savePath) - Integer.BYTES);
            if (length < 0 || length > maxExpectedLength) {
                throw new IOException("Corrupt favorite data length: " + length + " for file " + savePath);
            }

            byte[] data = new byte[length];
            dataInputStream.readFully(data);
            return data;
        } catch (IOException e) {
            DebugLogger.error("Failed to load favorite data: {}", savePath);
            DebugLogger.error("Favorite data load failure", e);
            return null;
        }
    }

    private boolean isEmptyFavoriteData(byte[] data) {
        return data == null || data.length == 0;
    }

    private String describePayload(byte[] data) {
        if (data == null || data.length == 0) {
            return "[]";
        }
        return new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }

    private void deleteIfExists(Path savePath) {
        try {
            if (Files.exists(savePath)) {
                Files.delete(savePath);
            }
        } catch (IOException e) {
            DebugLogger.error("Failed to clear favorite data: {}", savePath);
            DebugLogger.error("Favorite data clear failure", e);
        }
    }

    private Path getSavePath(UUID playerUUID) {
        return getPlayerDirectory().resolve(playerUUID.toString() + ".dat");
    }

    private Path getLegacyClientSavePath(UUID playerUUID) {
        String namespace = sanitizeClientStorageNamespace(clientStorageNamespace);
        return legacySaveDirectory.resolve(namespace)
            .resolve(NeoFavoriteItemsConstants.PLAYER_DATA_DIRECTORY)
            .resolve(playerUUID.toString() + ".dat");
    }

    private Path getLegacyServerSavePath(UUID playerUUID) {
        return legacyServerStorageRoot
            .resolve(NeoFavoriteItemsConstants.PLAYER_DATA_DIRECTORY)
            .resolve(playerUUID.toString() + ".dat");
    }

    private Path getPlayerDirectory() {
        return resolveStorageRoot().resolve(NeoFavoriteItemsConstants.PLAYER_DATA_DIRECTORY);
    }

    private Path resolveStorageRoot() {
        if (worldSaveDirectory != null) {
            return worldSaveDirectory.resolve("data").resolve(NeoFavoriteItemsMod.MOD_ID);
        }

        return saveDirectory.resolve(sanitizeClientStorageNamespace(clientStorageNamespace));
    }

    private String sanitizeClientStorageNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return NeoFavoriteItemsConstants.DEFAULT_SERVER_DIRECTORY;
        }

        String sanitized = namespace.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        if (sanitized.isBlank()) {
            return NeoFavoriteItemsConstants.DEFAULT_SERVER_DIRECTORY;
        }
        return sanitized;
    }

    private void refreshStorageContext() {
        Path storageRoot = resolveStorageRoot();
        if (!storageRoot.equals(activeStorageRoot)) {
            activeStorageRoot = storageRoot;
            cachedPlayerData.clear();
        }
    }

    private void resetStorageContext() {
        activeStorageRoot = null;
        cachedPlayerData.clear();
    }
}
