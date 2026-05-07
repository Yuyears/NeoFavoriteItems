package mycraft.yuyears.neofavoriteitems.render;

import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;

public record OverlayRenderDescriptor(
    NeoFavoriteItemsConfig.OverlayStyle style,
    int color,
    float opacity,
    float multiplier,
    boolean renderInFront
) {
}
