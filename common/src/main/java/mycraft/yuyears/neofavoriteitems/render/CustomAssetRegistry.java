package mycraft.yuyears.neofavoriteitems.render;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConstants;

/** Discovers user images without coupling asset storage to a platform UI. */
public final class CustomAssetRegistry {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    private final Path directory;
    private List<CustomAssetEntry> entries = List.of();

    public CustomAssetRegistry(Path gameDirectory) {
        directory = gameDirectory.resolve(NeoFavoriteItemsConstants.CUSTOM_ASSETS_DIRECTORY);
    }

    public Path directory() {
        return directory;
    }

    public synchronized List<CustomAssetEntry> refresh() {
        try {
            Files.createDirectories(directory);
            try (var files = Files.list(directory)) {
                entries = files
                    .filter(Files::isRegularFile)
                    .map(this::toEntry)
                    .filter(java.util.Objects::nonNull)
                    .sorted(Comparator.comparing(CustomAssetEntry::id, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CustomAssetEntry::id))
                    .toList();
            }
        } catch (IOException | RuntimeException e) {
            DebugLogger.warn("Custom asset scan failed: directory={} error={}", directory, e.toString());
            entries = List.of();
        }
        return entries;
    }

    public synchronized List<CustomAssetEntry> entries() {
        return entries;
    }

    /** Returns requested ID when present, otherwise stable fallback ID. */
    public synchronized String resolveOrDefault(String requestedId, String defaultId) {
        return find(requestedId) == null ? defaultId : requestedId;
    }

    public synchronized CustomAssetEntry find(String id) {
        if (id == null) {
            return null;
        }
        return entries.stream().filter(entry -> entry.id().equals(id)).findFirst().orElse(null);
    }

    private CustomAssetEntry toEntry(Path path) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            return null;
        }
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) return null;
        try {
            return new CustomAssetEntry(
                "custom:" + fileName,
                fileName,
                extension,
                path,
                Files.size(path),
                Files.getLastModifiedTime(path).toMillis()
            );
        } catch (IOException exception) {
            DebugLogger.warn("Custom asset metadata read failed: path={} error={}", path, exception.toString());
            return null;
        }
    }
}
