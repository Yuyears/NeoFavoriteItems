package mycraft.yuyears.neofavoriteitems.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HotbarSlotLayoutTest {
    @Test
    void mapsOnlyVanillaMainHotbarItemCoordinates() {
        int width = 320;
        int height = 240;
        for (int slot = 0; slot < 9; slot++) {
            assertEquals(slot, HotbarSlotLayout.slotAt(
                width, height, HotbarSlotLayout.x(width, slot), HotbarSlotLayout.y(height)
            ));
        }
        assertEquals(-1, HotbarSlotLayout.slotAt(width, height, HotbarSlotLayout.x(width, 0) + 1, HotbarSlotLayout.y(height)));
        assertEquals(-1, HotbarSlotLayout.slotAt(width, height, HotbarSlotLayout.x(width, 0), HotbarSlotLayout.y(height) - 1));
        assertEquals(-1, HotbarSlotLayout.slotAt(width, height, HotbarSlotLayout.x(width, 8) + 20, HotbarSlotLayout.y(height)));
    }
}
