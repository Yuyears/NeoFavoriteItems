package mycraft.yuyears.neofavoriteitems.client.ui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class NfiConfigLayoutTest {
    @Test
    void collapsedSectionRemovesDescendantHeight() {
        NfiConfigSectionNode section = new NfiConfigSectionNode("root", 0, true);
        section.add(new NfiConfigSectionNode("child", 1, true));
        assertEquals(48, section.contentHeight());
        section.setExpanded(false);
        assertEquals(24, section.contentHeight());
    }

    @Test
    void layoutVisibilityUsesViewport() {
        List<NfiConfigLayout.Entry> entries = NfiConfigLayout.layout(
            List.of(new NfiConfigSectionNode("first", 0, false), new NfiConfigSectionNode("second", 0, false)),
            10, 34, 0);
        assertTrue(entries.get(0).visible());
        assertTrue(!entries.get(1).visible());
    }

    @Test
    void parentViewportDoesNotHideExpandedChildStructure() {
        NfiConfigSectionNode parent = new NfiConfigSectionNode("parent", 0, true);
        parent.add(new NfiConfigSectionNode("child", 1, false));
        List<NfiConfigLayout.Entry> entries = NfiConfigLayout.layout(List.of(parent), 100, 124, 24);
        assertTrue(!entries.get(0).viewportVisible());
        assertTrue(entries.get(1).structuralVisible());
    }
}
