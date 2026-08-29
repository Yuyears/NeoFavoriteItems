package mycraft.yuyears.neofavoriteitems;

import mycraft.yuyears.neofavoriteitems.render.OverlayPlacement;
import mycraft.yuyears.neofavoriteitems.render.OverlayColorMode;
import mycraft.yuyears.neofavoriteitems.render.OverlayMaterialMode;

/** Serializable, client-only source data compiled into an immutable render profile. */
public final class OverlayProfileConfig {
    public static final int DEFAULT_LOCKED_COLOR = 0xFFFF5555;
    public static final int DEFAULT_LOCKABLE_COLOR = 0xB201960B;
    public static final int DEFAULT_UNLOCKABLE_COLOR = 0xFFFFAA00;
    public static final float DEFAULT_LOCKED_OPACITY = 1.0f;
    public static final float DEFAULT_BYPASS_OPACITY = 0.346f;
    public enum OpacityBehavior {
        FIXED,
        MULTIPLY_LOCKED
    }

    public NeoFavoriteItemsConfig.OverlayStyle style;
    public OverlayMaterialMode materialMode;
    public String materialId;
    public int color;
    public float opacity;
    public OverlayColorMode colorMode = OverlayColorMode.MULTIPLY;
    public OpacityBehavior opacityBehavior;
    public OverlayPlacement.Anchor anchor = OverlayPlacement.Anchor.SLOT_TOP_LEFT;
    public float offsetX;
    public float offsetY;
    public float width = 16.0f;
    public float height = 16.0f;
    public float scale = 1.0f;
    public float rotationDegrees;
    public int zIndex;
    public boolean allowOverflow;
    public boolean clipToSlot;

    public OverlayProfileConfig(NeoFavoriteItemsConfig.OverlayStyle style, int color, float opacity,
                                OpacityBehavior opacityBehavior, int zIndex) {
        this.style = style;
        this.materialMode = style == NeoFavoriteItemsConfig.OverlayStyle.COLOR_OVERLAY ? OverlayMaterialMode.NO_MATERIAL : OverlayMaterialMode.MATERIAL;
        this.materialId = mycraft.yuyears.neofavoriteitems.render.OverlayTextureCatalog.presetId(style);
        this.color = color;
        this.opacity = opacity;
        this.opacityBehavior = opacityBehavior;
        this.zIndex = zIndex;
    }

    /** Single source for shipped defaults used by config creation and UI Reset. */
    public static OverlayProfileConfig defaultLocked() {
        OverlayProfileConfig p = new OverlayProfileConfig(NeoFavoriteItemsConfig.OverlayStyle.MARK,
            DEFAULT_LOCKED_COLOR, DEFAULT_LOCKED_OPACITY, OpacityBehavior.FIXED, 2);
        p.colorMode = OverlayColorMode.NATIVE;
        p.scale = 0.5f;
        return p;
    }

    public static OverlayProfileConfig defaultBypass() {
        OverlayProfileConfig p = new OverlayProfileConfig(NeoFavoriteItemsConfig.OverlayStyle.MARK,
            DEFAULT_LOCKED_COLOR, DEFAULT_BYPASS_OPACITY, OpacityBehavior.MULTIPLY_LOCKED, 2);
        p.colorMode = OverlayColorMode.NATIVE;
        p.scale = 0.5f;
        return p;
    }

    public static OverlayProfileConfig defaultLockable() {
        OverlayProfileConfig p = new OverlayProfileConfig(NeoFavoriteItemsConfig.OverlayStyle.BORDER,
            DEFAULT_LOCKABLE_COLOR, 1.0f, OpacityBehavior.FIXED, 0);
        p.colorMode = OverlayColorMode.TINT;
        return p;
    }

    public static OverlayProfileConfig defaultUnlockable() {
        OverlayProfileConfig p = new OverlayProfileConfig(NeoFavoriteItemsConfig.OverlayStyle.BORDER,
            DEFAULT_UNLOCKABLE_COLOR, 1.0f, OpacityBehavior.FIXED, 0);
        p.colorMode = OverlayColorMode.TINT;
        return p;
    }

    public OverlayProfileConfig copy() {
        OverlayProfileConfig copy = new OverlayProfileConfig(style, color, opacity, opacityBehavior, zIndex);
        copy.copyFrom(this);
        return copy;
    }

    public void copyFrom(OverlayProfileConfig source) {
        style = source.style;
        materialMode = source.materialMode;
        materialId = source.materialId;
        color = source.color;
        opacity = source.opacity;
        colorMode = source.colorMode;
        opacityBehavior = source.opacityBehavior;
        anchor = source.anchor;
        offsetX = source.offsetX;
        offsetY = source.offsetY;
        width = source.width;
        height = source.height;
        scale = source.scale;
        rotationDegrees = source.rotationDegrees;
        zIndex = source.zIndex;
        allowOverflow = source.allowOverflow;
        clipToSlot = source.clipToSlot;
    }

    public boolean sameValues(OverlayProfileConfig other) {
        return other != null
            && style == other.style
            && materialMode == other.materialMode
            && java.util.Objects.equals(materialId, other.materialId)
            && color == other.color
            && Float.compare(opacity, other.opacity) == 0
            && colorMode == other.colorMode
            && opacityBehavior == other.opacityBehavior
            && anchor == other.anchor
            && Float.compare(offsetX, other.offsetX) == 0
            && Float.compare(offsetY, other.offsetY) == 0
            && Float.compare(width, other.width) == 0
            && Float.compare(height, other.height) == 0
            && Float.compare(scale, other.scale) == 0
            && Float.compare(rotationDegrees, other.rotationDegrees) == 0
            && zIndex == other.zIndex
            && allowOverflow == other.allowOverflow
            && clipToSlot == other.clipToSlot;
    }
}
