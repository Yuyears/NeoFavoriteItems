package mycraft.yuyears.neofavoriteitems.client.ui.layout;

import net.minecraft.network.chat.Component;
import mycraft.yuyears.neofavoriteitems.client.ui.control.NfiButton;
import net.minecraft.client.gui.components.AbstractWidget;
import java.util.ArrayList;
import java.util.List;

public final class NfiConfigSection implements NfiConfigNode {
    private final Component title;
    private final String key;
    private final NfiButton header;
    private final int depth;
    private final List<NfiConfigRow> rows = new ArrayList<>();
    private final List<NfiConfigSection> childSections = new ArrayList<>();
    private boolean expanded;
    private Runnable changeListener = () -> {};
    private Runnable toggleListener = () -> {};
    private AbstractWidget trailingWidget;

    public NfiConfigSection(Component title, int x, int y) {
        this(title, title.getString(), 0, x, y);
    }

    public NfiConfigSection(Component title, String key, int x, int y) {
        this(title, key, 0, x, y);
    }

    public NfiConfigSection(Component title, String key, int depth, int x, int y) {
        this.title = title;
        this.key = key;
        this.depth = Math.max(0, depth);
        this.header = new NfiButton(x, y, 1, 20,
            Component.literal("▸ " + title.getString()), ignored -> setExpanded(!expanded));
        this.header.setSectionHeader(true);
    }

    public void setPosition(int x, int y) {
        header.setPosition(x, y);
    }

    public NfiButton header() { return header; }
    public AbstractWidget trailingWidget() { return trailingWidget; }
    public void setTrailingWidget(AbstractWidget widget) { trailingWidget = widget; }
    public void setChangeListener(Runnable listener) { changeListener = listener == null ? () -> {} : listener; }
    public void setToggleListener(Runnable listener) { toggleListener = listener == null ? () -> {} : listener; }
    public boolean expanded() { return expanded; }
    public void setExpanded(boolean value) {
        if (expanded == value) return;
        expanded = value;
        header.setMessage(Component.literal((value ? "▾ " : "▸ ") + title.getString()));
        changeListener.run();
        toggleListener.run();
    }

    /** Applies persisted state while building a tree, without firing UI callbacks. */
    public void setExpandedSilently(boolean value) {
        expanded = value;
        header.setMessage(Component.literal((value ? "▾ " : "▸ ") + title.getString()));
    }
    public void add(NfiConfigRow row) { rows.add(row); }
    public void addSection(NfiConfigSection section) { if (section != null) childSections.add(section); }
    public List<NfiConfigRow> rows() { return List.copyOf(rows); }
    public List<NfiConfigSection> childSections() { return List.copyOf(childSections); }

    @Override public String stableKey() { return key; }
    @Override public int depth() { return depth; }
    @Override public List<NfiConfigNode> children() {
        List<NfiConfigNode> children = new ArrayList<>();
        for (NfiConfigRow row : rows) {
            children.add(new NfiConfigRowNode(stableKey() + "." + row.hashCode(), row, depth + 1));
        }
        children.addAll(childSections);
        return List.copyOf(children);
    }
    @Override public int headerHeight() { return NfiConfigPanel.ROW_HEIGHT; }
    @Override public int contentHeight() {
        if (!expanded) return headerHeight();
        return headerHeight() + rows.size() * NfiConfigPanel.ROW_HEIGHT
            + childSections.stream().mapToInt(NfiConfigSection::contentHeight).sum();
    }
}
