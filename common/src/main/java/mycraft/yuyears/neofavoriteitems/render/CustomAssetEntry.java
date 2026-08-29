package mycraft.yuyears.neofavoriteitems.render;

import java.nio.file.Path;

/** One user-provided image discovered in the NFI Assets directory. */
public record CustomAssetEntry(String id, String fileName, String extension, Path path, long size, long modifiedMillis) {
    public CustomAssetEntry {
        if (id == null || id.isBlank() || fileName == null || fileName.isBlank()
            || extension == null || extension.isBlank() || path == null || size < 0 || modifiedMillis < 0) {
            throw new IllegalArgumentException("Custom asset fields must be present");
        }
    }
}
