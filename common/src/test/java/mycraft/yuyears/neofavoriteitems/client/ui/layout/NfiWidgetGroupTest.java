package mycraft.yuyears.neofavoriteitems.client.ui.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NfiWidgetGroupTest {
    @Test
    void nestedRelativeGroupsMoveWhileAbsoluteGroupsStayPut() {
        var root = new NfiWidgetGroup(10, 20);
        var relative = new NfiWidgetGroup(0, 0);
        var nested = new NfiWidgetGroup(0, 0);
        var absolute = new NfiWidgetGroup(0, 0);

        relative.addRelative(nested, 3, 4);
        root.addRelative(relative, 5, 6);
        root.addAbsolute(absolute, 100, 200);

        assertEquals(18, nested.x());
        assertEquals(30, nested.y());
        root.setPosition(30, 40);
        assertEquals(38, nested.x());
        assertEquals(50, nested.y());
        assertEquals(100, absolute.x());
        assertEquals(200, absolute.y());
    }
}
