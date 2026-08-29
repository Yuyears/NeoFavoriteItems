package mycraft.yuyears.neofavoriteitems.render;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Caches resource dimensions once per client resource-manager generation. */
public final class TextureRegistry {
    public static final int MAX_TEXTURE_DIMENSION = 1_024;
    public static final int MAX_TEXTURE_PIXELS = 1_024 * 1_024;
    private static final TextureMetadata DEFAULT = new TextureMetadata(16, 16);

    private final Map<ResourceLocation, TextureMetadata> metadataByTexture = new HashMap<>();
    private ResourceManager resourceManager;

    public TextureMetadata get(ResourceLocation texture) {
        var customManager = mycraft.yuyears.neofavoriteitems.PlatformFavoriteSupport.getCustomTextureManager();
        TextureMetadata custom = customManager == null ? null : customManager.metadata(texture);
        if (custom != null) return custom;
        ResourceManager currentResourceManager = Minecraft.getInstance().getResourceManager();
        if (resourceManager != currentResourceManager) {
            resourceManager = currentResourceManager;
            metadataByTexture.clear();
        }
        return metadataByTexture.computeIfAbsent(texture, this::readMetadata);
    }

    public void clear() {
        metadataByTexture.clear();
        resourceManager = null;
    }

    private TextureMetadata readMetadata(ResourceLocation texture) {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isPresent()) {
                try (var stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
                    if (image.getWidth() > MAX_TEXTURE_DIMENSION || image.getHeight() > MAX_TEXTURE_DIMENSION
                        || image.getWidth() * image.getHeight() > MAX_TEXTURE_PIXELS) {
                        DebugLogger.warn(
                            "Overlay texture exceeds {}px limit: {} ({}x{})",
                            MAX_TEXTURE_DIMENSION,
                            texture,
                            image.getWidth(),
                            image.getHeight()
                        );
                        return DEFAULT;
                    }
                    return new TextureMetadata(image.getWidth(), image.getHeight());
                }
            }
        } catch (IOException | RuntimeException e) {
            DebugLogger.debug("Overlay texture metadata lookup failed: texture={} error={}", texture, e.toString());
        }
        return DEFAULT;
    }

    public record TextureMetadata(int width, int height) {
        public TextureMetadata {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Texture dimensions must be positive");
            }
        }
    }
}
