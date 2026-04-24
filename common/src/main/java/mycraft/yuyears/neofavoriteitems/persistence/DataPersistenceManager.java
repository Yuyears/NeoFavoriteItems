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
        this.worldSaveDirectory = worldDirectory;
        this.isServerSide = isServerSide;
        this.clientStorageNamespace = NeoFavoriteItemsConstants.DEFAULT_SERVER_DIRECTORY;
        resetStorageContext();

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
        if (!Files.isDirectory(playerDirectory)) {
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

    public synchronized void saveData(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }

        refreshStorageContext();
        byte[] data = FavoritesManager.getCodec().serialize();
        cachedPlayerData.put(playerUUID, data);
        writeData(getSavePath(playerUUID), data);
    }

    public synchronized void loadData(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }

        refreshStorageContext();
        byte[] data = cachedPlayerData.get(playerUUID);
        if (data == null) {
            Path currentSavePath = getSavePath(playerUUID);
            data = readData(currentSavePath);
            if (data == null && worldSaveDirectory == null) {
                Path legacySavePath = getLegacyClientSavePath(playerUUID);
                data = readData(legacySavePath);
                if (data != null) {
                    writeData(currentSavePath, data);
                    deleteIfExists(legacySavePath);
                }
            }
            if (data != null) {
                cachedPlayerData.put(playerUUID, data);
            }
        }

        if (data != null) {
            FavoritesManager.getCodec().deserialize(data);
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
        }
    }

    public synchronized boolean hasData(UUID playerUUID) {
        if (playerUUID == null) {
            return false;
        }

        refreshStorageContext();
        return cachedPlayerData.containsKey(playerUUID)
            || Files.exists(getSavePath(playerUUID))
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
            }
        } catch (IllegalArgumentException exception) {
            DebugLogger.warn("Ignored favorite data file with invalid UUID name: {}", savePath);
        }
    }

    private void writeData(Path savePath, byte[] data) {
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
        } catch (IOException e) {
            DebugLogger.error("Failed to save favorite data: {}", savePath);
            DebugLogger.error("Favorite data save failure", e);
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
