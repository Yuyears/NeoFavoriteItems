package mycraft.yuyears.neofavoriteitems.client.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class NfiCanvasTest {
    @Test
    void reusesCanvasForSameDrawingId() {
        NfiCanvas first = NfiCanvas.cached("test-shared", (graphics, x, y, width, height, color) -> {});
        NfiCanvas second = NfiCanvas.cached("test-shared", (graphics, x, y, width, height, color) -> {});

        assertSame(first, second);
    }
}
