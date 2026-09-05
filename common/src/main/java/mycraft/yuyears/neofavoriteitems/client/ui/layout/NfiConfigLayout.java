package mycraft.yuyears.neofavoriteitems.client.ui.layout;

import java.util.ArrayList;
import java.util.List;

/** Single recursive layout pass source for viewport and hit-test rectangles. */
public final class NfiConfigLayout {
    public record Entry(NfiConfigNode node, int y, int height,
                        boolean structuralVisible, boolean viewportVisible) {
        public boolean visible() { return structuralVisible && viewportVisible; }
    }

    private NfiConfigLayout() {}

    public static List<Entry> layout(List<NfiConfigNode> roots, int viewportTop, int viewportBottom, int scrollOffset) {
        List<Entry> entries = new ArrayList<>();
        int y = viewportTop - scrollOffset;
        for (NfiConfigNode root : roots) y = append(root, y, viewportTop, viewportBottom, entries, true);
        return List.copyOf(entries);
    }

    private static int append(NfiConfigNode node, int y, int top, int bottom, List<Entry> entries, boolean parentVisible) {
        int height = node.contentHeight();
        // Rows need full visibility; Section headers are clipped separately by panel.
        boolean visible = node instanceof NfiConfigSection
            ? y + node.headerHeight() > top && y < bottom
            : y >= top && y + height <= bottom;
        boolean structuralVisible = parentVisible;
        entries.add(new Entry(node, y, height, structuralVisible, visible));
        int next = y + node.headerHeight();
        if (node.expanded()) {
            // Viewport clipping is local to each entry. A parent leaving the viewport must
            // not hide children that are still visible; collapsed parents are not traversed.
            for (NfiConfigNode child : node.children()) next = append(child, next, top, bottom, entries, structuralVisible);
        }
        return y + height;
    }
}
