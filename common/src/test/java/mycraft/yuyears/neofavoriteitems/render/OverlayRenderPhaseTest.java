package mycraft.yuyears.neofavoriteitems.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayRenderPhaseTest {
    @Test
    void reservesOneForItemsAndSplitsNfiLayersAroundIt() {
        assertTrue(OverlayRenderPhase.BELOW_ITEM.includes(0));
        assertFalse(OverlayRenderPhase.BELOW_ITEM.includes(1));
        assertFalse(OverlayRenderPhase.ABOVE_ITEM.includes(1));
        assertTrue(OverlayRenderPhase.ABOVE_ITEM.includes(2));
        assertTrue(OverlayRenderPhase.ABOVE_ITEM.includes(200));
    }
}
