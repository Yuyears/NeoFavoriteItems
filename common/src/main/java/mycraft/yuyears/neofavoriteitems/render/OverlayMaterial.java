package mycraft.yuyears.neofavoriteitems.render;

import net.minecraft.resources.ResourceLocation;

public record OverlayMaterial(
    ResourceLocation texture,
    int color,
    float opacity,
    OverlayColorMode colorMode
) {
    public OverlayMaterial {
        if (!Float.isFinite(opacity)) {
            throw new IllegalArgumentException("Overlay opacity must be finite");
        }
        opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        if (colorMode == null) {
            throw new NullPointerException("colorMode");
        }
    }
}
