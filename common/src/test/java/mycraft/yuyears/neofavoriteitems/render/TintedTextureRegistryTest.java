package mycraft.yuyears.neofavoriteitems.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TintedTextureRegistryTest {
    @Test
    void tintTransfersSourceOklabLightnessAndKeepsTargetHue() {
        int blackSource = TintedTextureRegistry.transformPixel(0x7F000000, 0x006EFF, OverlayColorMode.TINT);
        int redSource = TintedTextureRegistry.transformPixel(0x7F0000FF, 0x006EFF, OverlayColorMode.TINT);
        int whiteSource = TintedTextureRegistry.transformPixel(0x7FFFFFFF, 0x006EFF, OverlayColorMode.TINT);

        assertEquals(0x7F000000, blackSource);
        assertEquals(0x7F000000, redSource & 0xFF000000);
        assertEquals(0x7F000000, whiteSource & 0xFF000000);
        double targetLightness = TintedTextureRegistry.oklabLightness(0xFFFF6E00);
        assertEquals(Math.sqrt(targetLightness), TintedTextureRegistry.oklabLightness(whiteSource), 0.01);
        assertEquals(
            Math.sqrt(targetLightness) * Math.sqrt(TintedTextureRegistry.oklabLightness(0x7F0000FF)),
            TintedTextureRegistry.oklabLightness(redSource),
            0.01
        );
        assertTrue(TintedTextureRegistry.oklabLightness(whiteSource)
            > TintedTextureRegistry.oklabLightness(redSource));
        assertTrue(((redSource >> 16) & 0xFF) > ((redSource >> 8) & 0xFF));
    }
}
