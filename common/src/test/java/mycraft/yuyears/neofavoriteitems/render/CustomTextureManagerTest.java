package mycraft.yuyears.neofavoriteitems.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomTextureManagerTest {
    @Test
    void acceptsOnlyMatchingPngAndJpegSignatures() {
        assertTrue(CustomTextureManager.supportedSignature(
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, "png"
        ));
        assertTrue(CustomTextureManager.supportedSignature(
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, "jpg"
        ));
        assertFalse(CustomTextureManager.supportedSignature(new byte[]{0x50, 0x4B, 0x03}, "png"));
        assertFalse(CustomTextureManager.supportedSignature(
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, "jpg"
        ));
    }
}
