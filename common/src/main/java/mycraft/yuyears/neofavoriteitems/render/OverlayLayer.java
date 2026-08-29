package mycraft.yuyears.neofavoriteitems.render;

public record OverlayLayer(
    OverlayLayerType type,
    int zIndex,
    OverlayPlacement placement,
    OverlayMaterial material,
    boolean enabled,
    boolean clipToSlot
) {
    public OverlayLayer {
        if (type == null || placement == null || material == null) {
            throw new NullPointerException("Overlay layer fields must not be null");
        }
    }
}
