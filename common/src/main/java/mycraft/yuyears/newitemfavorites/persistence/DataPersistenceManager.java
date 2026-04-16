
package mycraft.yuyears.newitemfavorites.persistence;

import mycraft.yuyears.newitemfavorites.FavoritesManager;
import mycraft.yuyears.newitemfavorites.NewItemFavoritesMod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class DataPersistenceManager {
    private static DataPersistenceManager instance;
    private Path saveDirectory;
    private Path worldSaveDirectory;
    private boolean isServerSide;

    private DataPersistenceManager() {}

    public static DataPersistenceManager getInstance() {
        if (instance == null) {
            instance = new DataPersistenceManager();
        }
        return instance;
    }

    public void initialize(Path gameDirectory, Path worldDirectory, boolean isServerSide) {
        this.saveDirectory = gameDirectory.resolve("itemfavorites");
        this.worldSaveDirectory = worldDirectory;
        this.isServerSide = isServerSide;
        
        try {
            Files.createDirectories(saveDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveData(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }

        Path savePath = getSavePath(playerUUID);
        
        try {
            Files.createDirectories(savePath.getParent());
            
            try (FileOutputStream fos = new FileOutputStream(savePath.toFile());
                 DataOutputStream dos = new DataOutputStream(fos)) {
                
                byte[] data = FavoritesManager.getInstance().serialize();
                dos.writeInt(data.length);
                dos.write(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadData(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }

        Path savePath = getSavePath(playerUUID);
        
        if (Files.exists(savePath)) {
            try (FileInputStream fis = new FileInputStream(savePath.toFile());
                 DataInputStream dis = new DataInputStream(fis)) {
                
                int length = dis.readInt();
                byte[] data = new byte[length];
                dis.readFully(data);
                
                FavoritesManager.getInstance().deserialize(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Path getSavePath(UUID playerUUID) {
        if (worldSaveDirectory != null && Files.exists(worldSaveDirectory)) {
            // 单人游戏或服务端模式：保存在存档文件夹中
            return worldSaveDirectory.resolve("data")
                .resolve(NewItemFavoritesMod.MOD_ID)
                .resolve("players")
                .resolve(playerUUID.toString() + ".dat");
        } else {
            // 仅客户端模式：保存在游戏根目录的itemfavorites文件夹中
            // 以服务器IP为区别（使用默认服务器标识）
            return saveDirectory.resolve("default_server")
                .resolve("players")
                .resolve(playerUUID.toString() + ".dat");
        }
    }

    public void setWorldSaveDirectory(Path worldDirectory) {
        this.worldSaveDirectory = worldDirectory;
    }

    public void clearData(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }

        Path savePath = getSavePath(playerUUID);
        
        try {
            if (Files.exists(savePath)) {
                Files.delete(savePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasData(UUID playerUUID) {
        if (playerUUID == null) {
            return false;
        }

        Path savePath = getSavePath(playerUUID);
        return Files.exists(savePath);
    }
}
