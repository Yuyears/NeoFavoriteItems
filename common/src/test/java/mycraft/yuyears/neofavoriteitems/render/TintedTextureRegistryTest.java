package mycraft.yuyears.neofavoriteitems.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TintedTextureRegistryTest {
    @Test
    void tintTransfersSourceOklabLightnessAndKeepsTargetHue() {
        int redSource = TintedTextureRegistry.transformPixel(0x7F0000FF, 0x006EFF, OverlayColorMode.TINT);
        int whiteSource = TintedTextureRegistry.transformPixel(0x7FFFFFFF, 0x006EFF, OverlayColorMode.TINT);

        assertEquals(0x7F000000, redSource & 0xFF000000);
        assertEquals(0x7F000000, whiteSource & 0xFF000000);
        assertEquals(0x7FFF6E00, whiteSource);
        assertTrue(TintedTextureRegistry.oklabLightness(whiteSource)
            > TintedTextureRegistry.oklabLightness(redSource));
        assertTrue(((redSource >> 16) & 0xFF) > ((redSource >> 8) & 0xFF));
    }
}
