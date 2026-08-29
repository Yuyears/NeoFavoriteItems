package mycraft.yuyears.neofavoriteitems.render;

import java.util.List;

import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.OverlayProfileConfig;

public record OverlayProfile(OverlayMode mode, List<OverlayLayer> layers) {
    public OverlayProfile {
        if (mode == null || layers == null) {
            throw new NullPointerException("Overlay profile fields must not be null");
        }
        layers = List.copyOf(layers);
    }

    public static OverlayProfile fromConfig(OverlayMode mode, OverlayProfileConfig config, float lockedOpacity) {
        float configuredOpacity = finiteOr(config.opacity, 1.0f);
        float effectiveOpacity = config.opacityBehavior == OverlayProfileConfig.OpacityBehavior.MULTIPLY_LOCKED
            ? configuredOpacity * finiteOr(lockedOpacity, 1.0f)
            : configuredOpacity;
        OverlayLayerType type = config.materialMode == OverlayMaterialMode.NO_MATERIAL
            ? OverlayLayerType.FILL
            : OverlayLayerType.TEXTURE;
        OverlayMaterial material = new OverlayMaterial(
            OverlayTextureCatalog.textureFor(config.style, config.materialId),
            config.color,
            effectiveOpacity,
            type == OverlayLayerType.FILL
                ? OverlayColorMode.SOLID_ALPHA
                : normalizeColorMode(config.colorMode)
        );
        OverlayPlacement placement = new OverlayPlacement(
            config.anchor == null ? OverlayPlacement.Anchor.SLOT_TOP_LEFT : config.anchor,
            finiteOr(config.offsetX, 0.0f),
            finiteOr(config.offsetY, 0.0f),
            positiveOr(config.width, 16.0f),
            positiveOr(config.height, 16.0f),
            positiveOr(config.scale, 1.0f),
            finiteOr(config.rotationDegrees, 0.0f),
            config.allowOverflow
        );
        return new OverlayProfile(mode, List.of(new OverlayLayer(
            type, validZIndex(config.zIndex), placement, material, true, config.clipToSlot
        )));
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private static OverlayColorMode normalizeColorMode(OverlayColorMode mode) {
        if (mode == null) return OverlayColorMode.MULTIPLY;
        if (mode == OverlayColorMode.ORIGINAL) return OverlayColorMode.NATIVE;
        return mode;
    }

    private static int validZIndex(int zIndex) {
        if (zIndex < 0) return OverlayZIndex.BELOW_ITEM;
        if (zIndex == OverlayZIndex.ITEM) return OverlayZIndex.ABOVE_ITEM;
        return Math.min(zIndex, 1000);
    }
}
