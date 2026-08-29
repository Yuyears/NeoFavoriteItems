package mycraft.yuyears.neofavoriteitems.render;

import com.mojang.blaze3d.platform.NativeImage;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** Owns decoded user textures and their TextureManager registrations. */
public final class CustomTextureManager implements AutoCloseable {
    private final CustomAssetRegistry registry;
    private final Map<String, Loaded> loaded = new HashMap<>();
    private final Map<ResourceLocation, CustomAssetEntry> entriesByTexture = new HashMap<>();
    private ResourceManager resourceManager;
    private boolean initialized;

    public CustomTextureManager(CustomAssetRegistry registry) {
        this.registry = registry;
    }

    public synchronized RefreshResult refresh() {
        ResourceManager current = Minecraft.getInstance().getResourceManager();
        if (current != resourceManager) {
            releaseAll();
            resourceManager = current;
        }
        List<CustomAssetEntry> entries = registry.refresh();
        Map<String, CustomAssetEntry> desired = new HashMap<>();
        entries.forEach(entry -> desired.put(entry.id(), entry));

        List<String> removed = new ArrayList<>();
        for (String id : List.copyOf(loaded.keySet())) {
            Loaded old = loaded.get(id);
            CustomAssetEntry next = desired.get(id);
            if (next == null || !sameFile(old.entry, next)) {
                release(id);
                removed.add(id);
            }
        }

        List<String> loadedIds = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        for (CustomAssetEntry entry : entries) {
            if (loaded.containsKey(entry.id())) continue;
            if (load(entry)) loadedIds.add(entry.id());
            else rejected.add(entry.id());
        }
        initialized = true;
        return new RefreshResult(List.copyOf(entries), List.copyOf(loadedIds), List.copyOf(removed), List.copyOf(rejected));
    }

    public synchronized ResourceLocation resolve(String materialId) {
        ensureLoaded();
        Loaded texture = loaded.get(materialId);
        return texture == null ? null : texture.location;
    }

    public synchronized TextureRegistry.TextureMetadata metadata(ResourceLocation texture) {
        CustomAssetEntry entry = entriesByTexture.get(texture);
        if (entry == null) return null;
        Loaded loadedTexture = loaded.get(entry.id());
        return loadedTexture == null ? null : loadedTexture.metadata;
    }

    public synchronized NativeImage readSource(ResourceLocation texture) {
        CustomAssetEntry entry = entriesByTexture.get(texture);
        if (entry == null) return null;
        try {
            return readImage(entry);
        } catch (IOException | RuntimeException exception) {
            DebugLogger.warn("Custom texture reread failed: file={} error={}", entry.fileName(), exception.toString());
            return null;
        }
    }

    public synchronized boolean contains(String materialId) {
        ensureLoaded();
        return loaded.containsKey(materialId);
    }

    public synchronized int loadedCount() {
        return loaded.size();
    }

    @Override
    public synchronized void close() {
        releaseAll();
        initialized = false;
        resourceManager = null;
    }

    private void ensureLoaded() {
        if (!initialized || resourceManager != Minecraft.getInstance().getResourceManager()) refresh();
    }

    private boolean load(CustomAssetEntry entry) {
        NativeImage image = null;
        try {
            image = readImage(entry);
            int width = image.getWidth();
            int height = image.getHeight();
            if (width > TextureRegistry.MAX_TEXTURE_DIMENSION || height > TextureRegistry.MAX_TEXTURE_DIMENSION
                || (long) width * height > TextureRegistry.MAX_TEXTURE_PIXELS) {
                throw new IOException("image exceeds 1024px/1048576px limits: " + width + "x" + height);
            }
            ResourceLocation location = locationFor(entry.id());
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            image = null;
            Loaded loadedTexture = new Loaded(entry, location, new TextureRegistry.TextureMetadata(width, height));
            loaded.put(entry.id(), loadedTexture);
            entriesByTexture.put(location, entry);
            return true;
        } catch (IOException | RuntimeException exception) {
            if (image != null) image.close();
            DebugLogger.warn("Custom texture rejected: file={} error={}", entry.fileName(), exception.toString());
            return false;
        }
    }

    private static NativeImage readImage(CustomAssetEntry entry) throws IOException {
        try (InputStream stream = Files.newInputStream(entry.path())) {
            byte[] signature = stream.readNBytes(8);
            if (!supportedSignature(signature, entry.extension())) {
                throw new IOException("file signature does not match " + entry.extension());
            }
        }
        try (InputStream stream = Files.newInputStream(entry.path())) {
            return NativeImage.read(stream);
        }
    }

    static boolean supportedSignature(byte[] signature, String extension) {
        if ("png".equalsIgnoreCase(extension)) {
            byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            return java.util.Arrays.equals(signature, png);
        }
        return signature.length >= 3
            && (signature[0] & 0xFF) == 0xFF
            && (signature[1] & 0xFF) == 0xD8
            && (signature[2] & 0xFF) == 0xFF;
    }

    private void release(String id) {
        Loaded removed = loaded.remove(id);
        if (removed == null) return;
        entriesByTexture.remove(removed.location);
        Minecraft.getInstance().getTextureManager().release(removed.location);
    }

    private void releaseAll() {
        List.copyOf(loaded.keySet()).forEach(this::release);
    }

    private static boolean sameFile(CustomAssetEntry left, CustomAssetEntry right) {
        return left.size() == right.size() && left.modifiedMillis() == right.modifiedMillis();
    }

    private static ResourceLocation locationFor(String id) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(id.getBytes(StandardCharsets.UTF_8));
            return ResourceLocation.fromNamespaceAndPath(
                NeoFavoriteItemsMod.MOD_ID,
                "dynamic/custom/" + HexFormat.of().formatHex(hash)
            );
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public record RefreshResult(List<CustomAssetEntry> entries, List<String> loaded,
                                List<String> removed, List<String> rejected) {}

    private record Loaded(CustomAssetEntry entry, ResourceLocation location,
                          TextureRegistry.TextureMetadata metadata) {}
}
