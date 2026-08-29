package mycraft.yuyears.neofavoriteitems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverlayProfileConfigTest {
    @Test
    void shippedProfileColorsMatchConfiguredDefaults() {
        assertEquals(0xFFFF5555, OverlayProfileConfig.defaultLocked().color);
        assertEquals(0xFFFF5555, OverlayProfileConfig.defaultBypass().color);
        assertEquals(0xB201960B, OverlayProfileConfig.defaultLockable().color);
        assertEquals(0xFFFFAA00, OverlayProfileConfig.defaultUnlockable().color);
    }
}
