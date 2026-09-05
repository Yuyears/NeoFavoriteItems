package mycraft.yuyears.neofavoriteitems.render;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import com.mojang.blaze3d.platform.NativeImage;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Lazily creates color-transformed textures; no per-frame pixel work. */
public final class TintedTextureRegistry {
    private static final int MAX_CACHED_TEXTURES = 16;
    private static final int TINT_LUT_SIZE = 256;
    private static final int GAMUT_SEARCH_STEPS = 7;
    private static final double[] LINEAR_SRGB = createLinearSrgbLut();
    private final Map<Key, ResourceLocation> textures = new LinkedHashMap<>(16, 0.75f, true);
    private final Set<ResourceLocation> generatedTextures = new HashSet<>();
    private ResourceManager resourceManager;

    public ResourceLocation resolve(ResourceLocation source, int color) {
        return resolve(source, color, OverlayColorMode.TINT);
    }

    public ResourceLocation resolve(ResourceLocation source, int color, OverlayColorMode mode) {
        ResourceManager current = Minecraft.getInstance().getResourceManager();
        if (current != resourceManager) {
            clear();
            resourceManager = current;
        }
        Key key = new Key(source, color & 0x00FFFFFF, mode == null ? OverlayColorMode.TINT : mode);
        ResourceLocation texture = textures.get(key);
        if (texture == null) {
            texture = create(key);
            textures.put(key, texture);
            evictOldest();
        }
        return texture;
    }

    public void clear() {
        var textureManager = Minecraft.getInstance().getTextureManager();
        generatedTextures.forEach(textureManager::release);
        textures.clear();
        generatedTextures.clear();
        resourceManager = null;
    }

    private ResourceLocation create(Key key) {
        NativeImage customSource = null;
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(key.source);
            var customManager = mycraft.yuyears.neofavoriteitems.PlatformFavoriteSupport.getCustomTextureManager();
            customSource = resource.isEmpty() && customManager != null ? customManager.readSource(key.source) : null;
            if (resource.isEmpty() && customSource == null) return key.source;
            try (var stream = resource.isPresent() ? resource.get().open() : null;
                 NativeImage source = customSource != null ? customSource : NativeImage.read(stream)) {
                customSource = null;
                if (source.getWidth() > TextureRegistry.MAX_TEXTURE_DIMENSION
                    || source.getHeight() > TextureRegistry.MAX_TEXTURE_DIMENSION
                    || source.getWidth() * source.getHeight() > TextureRegistry.MAX_TEXTURE_PIXELS) {
                    DebugLogger.warn("Tint source exceeds texture limits: {} ({}x{})", key.source,
                        source.getWidth(), source.getHeight());
                    return key.source;
                }
                NativeImage tinted = new NativeImage(source.getWidth(), source.getHeight(), false);
                int[] tintLut = key.mode == OverlayColorMode.TINT ? createTintLut(key.rgb) : null;
                for (int y = 0; y < source.getHeight(); y++) {
                    for (int x = 0; x < source.getWidth(); x++) {
                        int pixel = source.getPixelRGBA(x, y);
                        tinted.setPixelRGBA(x, y, transformPixel(pixel, key.rgb, key.mode, tintLut));
                    }
                }
                ResourceLocation generated = Minecraft.getInstance().getTextureManager().register(
                    "nfi_tint_" + Integer.toUnsignedString(key.hashCode(), 36),
                    new DynamicTexture(tinted)
                );
                generatedTextures.add(generated);
                return generated;
            }
        } catch (IOException | RuntimeException exception) {
            if (customSource != null) customSource.close();
            DebugLogger.warn("Failed to tint overlay texture {}: {}", key.source, exception.toString());
            return key.source;
        }
    }

    private void evictOldest() {
        if (textures.size() <= MAX_CACHED_TEXTURES) return;
        var iterator = textures.entrySet().iterator();
        ResourceLocation texture = iterator.next().getValue();
        iterator.remove();
        if (generatedTextures.remove(texture)) {
            Minecraft.getInstance().getTextureManager().release(texture);
        }
    }

    static int transformPixel(int abgr, int rgb, OverlayColorMode mode) {
        return transformPixel(abgr, rgb, mode, mode == OverlayColorMode.TINT ? createTintLut(rgb) : null);
    }

    private static int transformPixel(int abgr, int rgb, OverlayColorMode mode, int[] tintLut) {
        if (mode == OverlayColorMode.GRAYSCALE) {
            int luminance = Math.round(
                (abgr & 0xFF) * 0.2126f
                    + ((abgr >> 8) & 0xFF) * 0.7152f
                    + ((abgr >> 16) & 0xFF) * 0.0722f
            );
            return (abgr & 0xFF000000) | (luminance << 16) | (luminance << 8) | luminance;
        }
        if (mode == OverlayColorMode.REPLACE) {
            return (abgr & 0xFF000000) | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF);
        }
        if (mode == OverlayColorMode.TINT && tintLut != null) {
            int index = (int) Math.round(oklabLightness(abgr) * (TINT_LUT_SIZE - 1));
            return (abgr & 0xFF000000) | tintLut[Math.clamp(index, 0, TINT_LUT_SIZE - 1)];
        }
        return abgr;
    }

    private static int[] createTintLut(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        double[] target = toOklab(red, green, blue);
        double targetChroma = Math.hypot(target[1], target[2]);
        double hue = Math.atan2(target[2], target[1]);
        int[] lut = new int[TINT_LUT_SIZE];
        for (int index = 0; index < lut.length; index++) {
            double sourceLightness = index / (double) (lut.length - 1);
            double transfer = Math.sqrt(sourceLightness);
            double lightness = Math.sqrt(target[0]) * transfer;
            double chroma = targetChroma * transfer;
            double usableChroma = inGamut(lightness, chroma, hue)
                ? chroma
                : maxInGamutChroma(lightness, chroma, hue);
            lut[index] = oklchToRgb(lightness, usableChroma, hue);
        }
        return lut;
    }

    static double oklabLightness(int abgr) {
        double r = LINEAR_SRGB[abgr & 0xFF];
        double g = LINEAR_SRGB[(abgr >> 8) & 0xFF];
        double b = LINEAR_SRGB[(abgr >> 16) & 0xFF];
        double l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
        double m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
        double s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
        return 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s;
    }

    private static double[] toOklab(int red, int green, int blue) {
        double r = LINEAR_SRGB[red];
        double g = LINEAR_SRGB[green];
        double b = LINEAR_SRGB[blue];
        double l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
        double m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
        double s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
        return new double[] {
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        };
    }

    private static double srgbToLinear(double value) {
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static double[] createLinearSrgbLut() {
        double[] lut = new double[256];
        for (int index = 0; index < lut.length; index++) {
            lut[index] = srgbToLinear(index / 255.0);
        }
        return lut;
    }

    private static double maxInGamutChroma(double lightness, double chroma, double hue) {
        double low = 0.0;
        double high = chroma;
        for (int step = 0; step < GAMUT_SEARCH_STEPS; step++) {
            double middle = (low + high) * 0.5;
            if (inGamut(lightness, middle, hue)) low = middle;
            else high = middle;
        }
        return low;
    }

    private static boolean inGamut(double lightness, double chroma, double hue) {
        double[] linear = oklchToLinearRgb(lightness, chroma, hue);
        return linear[0] >= 0.0 && linear[0] <= 1.0
            && linear[1] >= 0.0 && linear[1] <= 1.0
            && linear[2] >= 0.0 && linear[2] <= 1.0;
    }

    private static int oklchToRgb(double lightness, double chroma, double hue) {
        double[] linear = oklchToLinearRgb(lightness, chroma, hue);
        int red = linearToSrgb8(linear[0]);
        int green = linearToSrgb8(linear[1]);
        int blue = linearToSrgb8(linear[2]);
        return (blue << 16) | (green << 8) | red;
    }

    private static double[] oklchToLinearRgb(double lightness, double chroma, double hue) {
        double a = chroma * Math.cos(hue);
        double b = chroma * Math.sin(hue);
        double l = Math.pow(lightness + 0.3963377774 * a + 0.2158037573 * b, 3.0);
        double m = Math.pow(lightness - 0.1055613458 * a - 0.0638541728 * b, 3.0);
        double s = Math.pow(lightness - 0.0894841775 * a - 1.2914855480 * b, 3.0);
        return new double[] {
            4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
        };
    }

    private static int linearToSrgb8(double value) {
        value = Math.clamp(value, 0.0, 1.0);
        double srgb = value <= 0.0031308
            ? 12.92 * value
            : 1.055 * Math.pow(value, 1.0 / 2.4) - 0.055;
        return (int) Math.round(srgb * 255.0);
    }

    private record Key(ResourceLocation source, int rgb, OverlayColorMode mode) {}
}
