package mycraft.yuyears.neofavoriteitems.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import java.util.Comparator;
import java.util.UUID;

class CustomAssetRegistryTest {
    @Test
    void refreshFiltersAndSortsSupportedFiles() throws Exception {
        var gameDirectory = java.nio.file.Path.of("build", "custom-asset-test-" + UUID.randomUUID());
        try {
            Files.createDirectories(gameDirectory.resolve("NFI Assets"));
            Files.createFile(gameDirectory.resolve("NFI Assets/Z.jpg"));
            Files.createFile(gameDirectory.resolve("NFI Assets/a.PNG"));
            Files.createFile(gameDirectory.resolve("NFI Assets/no.txt"));

            var registry = new CustomAssetRegistry(gameDirectory);
            var entries = registry.refresh();

            assertEquals(2, entries.size());
            assertEquals("custom:a.PNG", entries.get(0).id());
            assertEquals("custom:Z.jpg", entries.get(1).id());
            assertEquals("builtin", registry.resolveOrDefault("missing.png", "builtin"));
            assertTrue(registry.directory().endsWith("NFI Assets"));
        } finally {
            if (Files.exists(gameDirectory)) {
                try (var paths = Files.walk(gameDirectory)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                    });
                }
            }
        }
    }
}
