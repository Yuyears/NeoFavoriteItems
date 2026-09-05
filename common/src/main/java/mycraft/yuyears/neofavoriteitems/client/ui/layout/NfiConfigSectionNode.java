package mycraft.yuyears.neofavoriteitems.client.ui.layout;

import java.util.ArrayList;
import java.util.List;

/** Nested section node. Expansion state belongs to caller-owned session map. */
public final class NfiConfigSectionNode implements NfiConfigNode {
    private final String stableKey;
    private final List<NfiConfigNode> children = new ArrayList<>();
    private final int depth;
    private boolean expanded;

    public NfiConfigSectionNode(String stableKey, int depth, boolean expanded) {
        this.stableKey = stableKey;
        this.depth = Math.max(0, depth);
        this.expanded = expanded;
    }

    public NfiConfigSectionNode add(NfiConfigNode child) {
        if (child != null) children.add(child);
        return this;
    }

    public void setExpanded(boolean expanded) { this.expanded = expanded; }
    @Override public String stableKey() { return stableKey; }
    @Override public int depth() { return depth; }
    @Override public List<NfiConfigNode> children() { return List.copyOf(children); }
    @Override public int headerHeight() { return NfiConfigPanel.ROW_HEIGHT; }
    @Override public int contentHeight() {
        if (!expanded) return headerHeight();
        return headerHeight() + children.stream().mapToInt(NfiConfigNode::contentHeight).sum();
    }
    @Override public boolean expanded() { return expanded; }
}
