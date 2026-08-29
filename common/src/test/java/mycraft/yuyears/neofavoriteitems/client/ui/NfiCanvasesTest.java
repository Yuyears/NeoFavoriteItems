package mycraft.yuyears.neofavoriteitems.client.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NfiCanvasesTest {
    @Test
    void playTriangleUsesOddLogicalSize() {
        assertEquals(9, NfiCanvases.playSize(20, 20));
        assertEquals(5, NfiCanvases.playSize(9, 9));
    }
}
