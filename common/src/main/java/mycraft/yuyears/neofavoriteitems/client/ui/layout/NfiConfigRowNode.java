package mycraft.yuyears.neofavoriteitems.client.ui.layout;

import java.util.List;

/** Leaf node wrapping existing config row; keeps widget implementation out of tree layout. */
public record NfiConfigRowNode(String stableKey, NfiConfigRow row, int depth) implements NfiConfigNode {
    public NfiConfigRowNode {
        if (stableKey == null || row == null) throw new NullPointerException("row node fields");
        depth = Math.max(0, depth);
    }

    @Override public List<NfiConfigNode> children() { return List.of(); }
    @Override public int headerHeight() { return 0; }
    @Override public int contentHeight() { return NfiConfigPanel.ROW_HEIGHT; }
    @Override public boolean expanded() { return true; }
}
