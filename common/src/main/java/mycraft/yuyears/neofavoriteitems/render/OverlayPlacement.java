package mycraft.yuyears.neofavoriteitems.render;

public record OverlayPlacement(
    Anchor anchor,
    float offsetX,
    float offsetY,
    float width,
    float height,
    float scale,
    float rotationDegrees,
    boolean allowOverflow
) {
    public static final float MAX_RENDER_SIZE = 64.0f;

    public OverlayPlacement {
        if (anchor == null) {
            throw new NullPointerException("anchor");
        }
        if (!Float.isFinite(offsetX) || !Float.isFinite(offsetY)
            || !Float.isFinite(width) || !Float.isFinite(height) || !Float.isFinite(scale)
            || !Float.isFinite(rotationDegrees)
            || width <= 0.0f || height <= 0.0f || scale <= 0.0f) {
            throw new IllegalArgumentException("Overlay placement values must be finite and positive");
        }
    }

    public OverlayPlacement(Anchor anchor, float offsetX, float offsetY, float width, float height,
                            float scale, boolean allowOverflow) {
        this(anchor, offsetX, offsetY, width, height, scale, 0.0f, allowOverflow);
    }

    public static OverlayPlacement slot() {
        return new OverlayPlacement(Anchor.SLOT_TOP_LEFT, 0.0f, 0.0f, 16.0f, 16.0f, 1.0f, 0.0f, false);
    }

    public Resolved resolve(SlotRenderTarget target) {
        float renderWidth = Math.min(width * scale, MAX_RENDER_SIZE);
        float renderHeight = Math.min(height * scale, MAX_RENDER_SIZE);
        if (!allowOverflow) {
            renderWidth = Math.min(renderWidth, target.width());
            renderHeight = Math.min(renderHeight, target.height());
        }
        int resolvedWidth = Math.max(1, Math.round(renderWidth));
        int resolvedHeight = Math.max(1, Math.round(renderHeight));
        float baseX = switch (anchor) {
            case SLOT_TOP_LEFT, SLOT_CENTER_LEFT, SLOT_BOTTOM_LEFT -> target.x();
            case SLOT_CENTER, SLOT_TOP_CENTER, SLOT_BOTTOM_CENTER -> target.x() + (target.width() - resolvedWidth) / 2.0f;
            case SLOT_TOP_RIGHT, SLOT_CENTER_RIGHT, SLOT_BOTTOM_RIGHT -> target.x() + target.width() - resolvedWidth;
        };
        float baseY = switch (anchor) {
            case SLOT_TOP_LEFT, SLOT_TOP_CENTER, SLOT_TOP_RIGHT -> target.y();
            case SLOT_CENTER_LEFT, SLOT_CENTER, SLOT_CENTER_RIGHT -> target.y() + (target.height() - resolvedHeight) / 2.0f;
            case SLOT_BOTTOM_LEFT, SLOT_BOTTOM_CENTER, SLOT_BOTTOM_RIGHT -> target.y() + target.height() - resolvedHeight;
        };
        int resolvedX = Math.round(baseX + offsetX);
        int resolvedY = Math.round(baseY + offsetY);
        if (!allowOverflow) {
            resolvedX = Math.max(target.x(), Math.min(resolvedX, target.x() + target.width() - resolvedWidth));
            resolvedY = Math.max(target.y(), Math.min(resolvedY, target.y() + target.height() - resolvedHeight));
        }
        return new Resolved(resolvedX, resolvedY, resolvedWidth, resolvedHeight, rotationDegrees);
    }

    public enum Anchor {
        SLOT_TOP_LEFT,
        SLOT_TOP_CENTER,
        SLOT_TOP_RIGHT,
        SLOT_CENTER_LEFT,
        SLOT_CENTER,
        SLOT_CENTER_RIGHT,
        SLOT_BOTTOM_LEFT,
        SLOT_BOTTOM_CENTER,
        SLOT_BOTTOM_RIGHT
    }

    public record Resolved(int x, int y, int width, int height, float rotationDegrees) {}
}
