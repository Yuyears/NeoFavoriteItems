package mycraft.yuyears.neofavoriteitems.client.ui.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NfiScrollPanelTest {
    @Test
    void scrollsOnlyInsideViewportAndClampsToContent() {
        var content = new NfiWidgetGroup(0, 0);
        var panel = new NfiScrollPanel(10, 20, 100, 40, 100, content);

        assertFalse(panel.mouseScrolled(0, 0, -1));
        assertTrue(panel.mouseScrolled(20, 30, -1));
        assertEquals(18, panel.scrollOffset());
        for (int i = 0; i < 10; i++) panel.mouseScrolled(20, 30, -1);
        assertEquals(60, panel.scrollOffset());
        assertEquals(-40, content.y());
    }
}
