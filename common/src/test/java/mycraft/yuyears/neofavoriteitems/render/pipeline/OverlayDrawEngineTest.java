package mycraft.yuyears.neofavoriteitems.render.pipeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverlayDrawEngineTest {
    @Test
    void capsEveryAboveItemBusinessLayerBelowTooltipDepth() {
        assertEquals(0.0f, OverlayDrawEngine.renderDepth(0));
        assertEquals(320.0f, OverlayDrawEngine.renderDepth(2));
        assertEquals(320.0f, OverlayDrawEngine.renderDepth(1000));
        assertEquals(320.0f, OverlayDrawEngine.renderDepth(Integer.MAX_VALUE));
    }
}
