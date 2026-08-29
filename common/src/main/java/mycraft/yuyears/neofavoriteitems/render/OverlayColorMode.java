package mycraft.yuyears.neofavoriteitems.render;

public enum OverlayColorMode {
    NATIVE,
    /** Legacy name for NATIVE. */
    ORIGINAL,
    TINT,
    MULTIPLY,
    SOLID_ALPHA,
    /** Reserved grayscale transform; unsupported backends fall back to native. */
    GRAYSCALE,
    /** Replace source RGB with configured color while preserving source alpha. */
    REPLACE
}
