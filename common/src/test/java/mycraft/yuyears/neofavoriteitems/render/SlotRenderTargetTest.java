package mycraft.yuyears.neofavoriteitems.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;

class SlotRenderTargetTest {
    @Test
    void standardTargetUsesMinecraftSlotSize() {
        var target = SlotRenderTarget.standard(LogicalSlotIndex.of(3), true, 10, 20);

        assertEquals(LogicalSlotIndex.of(3), target.logicalSlot());
        assertEquals(10, target.x());
        assertEquals(20, target.y());
        assertEquals(16, target.width());
        assertEquals(16, target.height());
    }

    @Test
    void rejectsInvalidDimensions() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SlotRenderTarget(LogicalSlotIndex.of(0), false, 0, 0, 0, 16)
        );
    }
}
