package mycraft.yuyears.neofavoriteitems.render;

public enum OverlayRenderPhase {
    BELOW_ITEM,
    ABOVE_ITEM,
    ALL;

    public boolean includes(int zIndex) {
        return switch (this) {
            case BELOW_ITEM -> zIndex == OverlayZIndex.BELOW_ITEM;
            case ABOVE_ITEM -> zIndex >= OverlayZIndex.ABOVE_ITEM;
            case ALL -> zIndex != OverlayZIndex.ITEM;
        };
    }
}
