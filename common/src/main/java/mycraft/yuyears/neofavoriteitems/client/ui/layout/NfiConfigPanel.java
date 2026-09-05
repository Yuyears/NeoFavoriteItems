package mycraft.yuyears.neofavoriteitems.client.ui.layout;

import mycraft.yuyears.neofavoriteitems.client.ui.NfiUiRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class NfiConfigPanel {
    static final int ROW_HEIGHT = 24;
    private static final int CONTROL_LEFT_MARGIN = 6;
    private static final int CONTROL_RIGHT_MARGIN = 8;
    private final int width;
    private final int labelWidth;
    private final List<NfiConfigRow> rows = new ArrayList<>();
    private final List<NfiConfigSection> sections = new ArrayList<>();
    /** Root nodes in visual insertion order. Child rows/sections never enter this list. */
    private final List<NfiConfigNode> rootNodes = new ArrayList<>();
    private final Set<NfiConfigRow> leftAlignedRows = new HashSet<>();
    private NfiConfigSection activeSection;
    private final List<NfiConfigSection> sectionStack = new ArrayList<>();
    private final NfiWidgetGroup group;
    private final int baseX;
    private final int baseY;
    private final Map<String, Boolean> expansionState;
    private int viewportHeight = Integer.MAX_VALUE;
    private int scrollOffset;

    public NfiConfigPanel(int x, int y, int width, int labelWidth, Map<String, Boolean> expansionState) {
        this.width = width;
        this.labelWidth = labelWidth;
        this.baseX = x;
        this.baseY = y;
        this.expansionState = expansionState;
        this.group = new NfiWidgetGroup(x, y);
    }

    public void add(NfiConfigRow row) {
        row.resize(controlWidth());
        rows.add(row);
        if (activeSection != null) activeSection.add(row);
        else rootNodes.add(new NfiConfigRowNode("row." + row.hashCode(), row, 0));
        reflow();
        refreshVisibility();
    }

    public void addLeftWidget(net.minecraft.client.gui.components.AbstractWidget widget) {
        NfiConfigRow row = new NfiConfigRow(Component.empty(), widget);
        row.resize(controlWidth());
        rows.add(row);
        leftAlignedRows.add(row);
        rootNodes.add(new NfiConfigRowNode("row." + row.hashCode(), row, 0));
        reflow();
        refreshVisibility();
    }

    public NfiConfigSection beginSection(Component title) {
        String prefix = sectionStack.isEmpty() ? "" : sectionStack.getLast().stableKey() + ".";
        return beginSection(title, prefix + title.getString());
    }

    public NfiConfigSection beginSection(Component title, String stableKey) {
        NfiConfigSection section = new NfiConfigSection(title, stableKey, sectionStack.size(), baseX, baseY);
        activeSection = section;
        section.header().setWidth(Math.max(1, width - 4 - section.depth() * 12));
        section.setChangeListener(() -> {
            expansionState.put(stableKey, section.expanded());
            refreshLayout();
        });
        if (Boolean.TRUE.equals(expansionState.get(stableKey))) section.setExpandedSilently(true);
        if (sectionStack.isEmpty()) {
            sections.add(section);
            rootNodes.add(section);
        }
        else sectionStack.getLast().addSection(section);
        sectionStack.add(section);
        reflow();
        return activeSection;
    }

    /** Starts a root section without relying on the mutable active-section stack. */
    public NfiConfigSectionHandle section(Component title, String stableKey) {
        return new NfiConfigSectionHandle(this, createSection(null, title, stableKey));
    }

    NfiConfigSectionHandle beginHandleChild(NfiConfigSection parent, Component title, String stableKey) {
        return new NfiConfigSectionHandle(this, createSection(parent, title, stableKey));
    }

    void addToSection(NfiConfigSection section, NfiConfigRow row) {
        row.resize(controlWidth());
        rows.add(row);
        section.add(row);
        reflow();
        refreshVisibility();
    }

    private NfiConfigSection createSection(NfiConfigSection parent, Component title, String stableKey) {
        int depth = parent == null ? 0 : parent.depth() + 1;
        NfiConfigSection section = new NfiConfigSection(title, stableKey, depth, baseX, baseY);
        section.header().setWidth(Math.max(1, width - 4 - depth * 12));
        section.setChangeListener(() -> {
            expansionState.put(stableKey, section.expanded());
            refreshLayout();
        });
        if (Boolean.TRUE.equals(expansionState.get(stableKey))) section.setExpandedSilently(true);
        if (parent == null) {
            sections.add(section);
            rootNodes.add(section);
        } else {
            parent.addSection(section);
        }
        reflow();
        return section;
    }

    public void endSection() {
        if (!sectionStack.isEmpty()) sectionStack.removeLast();
        activeSection = sectionStack.isEmpty() ? null : sectionStack.getLast();
    }
    public List<net.minecraft.client.gui.components.AbstractWidget> sectionHeaders() {
        List<net.minecraft.client.gui.components.AbstractWidget> result = new ArrayList<>();
        java.util.Set<net.minecraft.client.gui.components.AbstractWidget> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        sections.forEach(section -> collectHeaders(section, result, seen));
        return result;
    }

    /** Completes initial tree layout before Screen registers widgets. */
    public void finalizeLayout() {
        reflow();
        refreshVisibility();
    }

    private static void collectHeaders(NfiConfigSection section, List<net.minecraft.client.gui.components.AbstractWidget> result,
                                       java.util.Set<net.minecraft.client.gui.components.AbstractWidget> seen) {
        if (seen.add(section.header())) result.add(section.header());
        if (section.trailingWidget() != null && seen.add(section.trailingWidget())) result.add(section.trailingWidget());
        section.childSections().forEach(child -> collectHeaders(child, result, seen));
    }

    public void hideAllSectionWidgets() {
        // Screen may still be traversing previous widget list during a deferred rebuild.
        // Hide every widget in old tree, not only section headers, to prevent one-frame ghost tree.
        for (NfiConfigRow row : rows) {
            for (var widget : row.widgets()) {
                widget.visible = false;
            }
        }
        sections.forEach(NfiConfigPanel::hideSectionHeaders);
    }

    /** Temporary diagnostic snapshot; removed after ghost-widget root cause is confirmed. */
    public String diagnosticSnapshot() {
        long visibleHeaders = sectionHeaders().stream().filter(w -> w.visible).count();
        long visibleRows = rows.stream().flatMap(r -> r.widgets().stream()).filter(w -> w.visible).count();
        var entries = layoutEntries().stream()
            .filter(NfiConfigLayout.Entry::visible)
            .map(e -> (e.node() instanceof NfiConfigSection s ? "S:" + s.stableKey() : "R")
                + "@" + e.y() + ":" + e.height())
            .toList();
        return "panel=" + System.identityHashCode(this) + " headers=" + visibleHeaders
            + " rows=" + visibleRows + " entries=" + entries;
    }

    public int controlWidth() {
        return width - labelWidth - CONTROL_LEFT_MARGIN - CONTROL_RIGHT_MARGIN;
    }

    public void setPosition(int x, int y) {
        group.setPosition(x, y);
    }

    public void setViewportHeight(int viewportHeight) {
        this.viewportHeight = Math.max(1, viewportHeight);
        scrollOffset = Math.min(scrollOffset, maxScroll());
        applyScroll();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (rows.isEmpty() || mouseX < group.x() || mouseX >= group.x() + width
            || mouseY < baseY || (viewportHeight != Integer.MAX_VALUE && mouseY >= baseY + viewportHeight)
            || maxScroll() == 0) return false;
        scrollOffset = Math.clamp(scrollOffset + (delta < 0 ? ROW_HEIGHT : -ROW_HEIGHT), 0, maxScroll());
        applyScroll();
        return true;
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    /** Handles section chrome before Screen widget dispatch, preventing first-click focus swallowing. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Use current layout entries only. Header widget rectangles may belong to a
        // previous scroll pass and can otherwise swallow clicks for a different section.
        for (NfiConfigLayout.Entry entry : layoutEntries()) {
            if (!(entry.node() instanceof NfiConfigSection section) || !entry.visible()) continue;
            int x = baseX + section.depth() * 12;
            int right = x + section.header().getWidth();
            if (mouseX >= x && mouseX < right && mouseY >= entry.y() && mouseY < entry.y() + section.headerHeight()) {
                section.setExpanded(!section.expanded());
                return true;
            }
            if (section.trailingWidget() != null) {
                var trailing = section.trailingWidget();
                if (mouseX >= trailing.getX() && mouseX < trailing.getX() + trailing.getWidth()
                    && mouseY >= trailing.getY() && mouseY < trailing.getY() + trailing.getHeight()) {
                    trailing.mouseClicked(mouseX, mouseY, button);
                    return true;
                }
            }
        }
        return false;
    }

    public void renderLabels(GuiGraphics graphics, Font font, int color) {
        if (viewportHeight == Integer.MAX_VALUE) {
            renderAllLabels(graphics, font, color);
            return;
        }
        graphics.enableScissor(group.x(), baseY, group.x() + width, baseY + viewportHeight);
        renderAllLabels(graphics, font, color);
        renderScrollbar(graphics);
        graphics.disableScissor();
    }

    private int maxScroll() {
        int contentHeight = rootNodes.stream().mapToInt(NfiConfigNode::contentHeight).sum();
        int overflow = contentHeight - viewportHeight;
        if (overflow <= 0) return 0;
        // Keep row-sized wheel steps, but round final bound upward so last content row
        // can always be reached when viewport height is not a row multiple.
        return ((overflow + ROW_HEIGHT - 1) / ROW_HEIGHT) * ROW_HEIGHT;
    }

    private void renderAllLabels(GuiGraphics graphics, Font font, int color) {
        for (NfiConfigLayout.Entry entry : layoutEntries()) {
            if (!entry.visible()) continue;
            if (entry.node() instanceof NfiConfigRowNode row) {
                row.row().renderLabel(graphics, font, group.x() + row.depth() * 12, entry.y(), color);
            }
        }
    }

    private void applyScroll() {
        group.setPosition(baseX, baseY - scrollOffset);
        reflow();
        refreshVisibility();
    }

    private void refreshLayout() {
        scrollOffset = Math.min(scrollOffset, maxScroll());
        reflow();
        refreshVisibility();
    }

    private void reflow() {
        group.clear();
        group.setPosition(baseX, baseY - scrollOffset);
        for (NfiConfigLayout.Entry entry : layoutEntries()) {
            if (entry.node() instanceof NfiConfigSection section) {
                section.header().setWidth(Math.max(1, width - 4 - entry.node().depth() * 12
                    - (section.trailingWidget() == null ? 0 : 28)));
                section.setPosition(baseX + entry.node().depth() * 12, entry.y());
                if (section.trailingWidget() != null) {
                    section.trailingWidget().setPosition(baseX + width - 26 - entry.node().depth() * 12, entry.y() + 2);
                }
            } else if (entry.node() instanceof NfiConfigRowNode row) {
                row.row().fitIndentedWidth(controlWidth(), row.depth());
                int x = leftAlignedRows.contains(row.row()) ? baseX : baseX + labelWidth + CONTROL_LEFT_MARGIN + row.depth() * 12;
                group.addAbsolute(row.row().controls(), x, entry.y() + 2);
                row.row().compactWidgets(x, entry.y() + 2);
            }
        }
    }

    private void refreshVisibility() {
        // First hide every widget; collapsed ancestors are intentionally absent from layout entries.
        rows.forEach(row -> row.widgets().forEach(widget -> widget.visible = false));
        rootNodes.forEach(node -> hideSectionHeaders(node));
        for (NfiConfigLayout.Entry entry : layoutEntries()) {
            if (entry.node() instanceof NfiConfigSection section) {
                // Entry visibility is authoritative: includes viewport and collapsed ancestors.
                boolean headerVisible = entry.visible();
                section.header().visible = headerVisible;
                if (section.trailingWidget() != null) section.trailingWidget().visible = headerVisible;
            } else if (entry.node() instanceof NfiConfigRowNode row) {
                row.row().widgets().forEach(widget -> widget.visible = entry.visible());
            }
        }
    }

    private static void hideSectionHeaders(NfiConfigNode node) {
        if (node instanceof NfiConfigSection section) {
            section.header().visible = false;
            if (section.trailingWidget() != null) section.trailingWidget().visible = false;
            section.childSections().forEach(NfiConfigPanel::hideSectionHeaders);
        }
    }

    private boolean sectionHeaderVisible(int y, int height) {
        if (viewportHeight == Integer.MAX_VALUE) return true;
        // Header and row share same clipping semantics: fully inside viewport only.
        return y >= baseY && y + height <= baseY + viewportHeight;
    }

    private List<NfiConfigLayout.Entry> layoutEntries() {
        return NfiConfigLayout.layout(rootNodes, baseY, baseY + viewportHeight, scrollOffset);
    }

    private boolean inViewport(int top) {
        return viewportHeight == Integer.MAX_VALUE
            || (top >= baseY && top + ROW_HEIGHT <= baseY + viewportHeight);
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int maximum = maxScroll();
        if (maximum == 0) return;
        int trackX = group.x() + width - 3;
        graphics.fill(trackX, baseY, trackX + 2, baseY + viewportHeight, NfiUiRenderer.scrollbarTrackColor());
        int contentHeight = maxScroll() + viewportHeight;
        int thumbHeight = Math.max(12, viewportHeight * viewportHeight / contentHeight);
        int travel = viewportHeight - thumbHeight;
        int thumbY = baseY + (int) Math.round((double) scrollOffset / maximum * travel);
        graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, NfiUiRenderer.scrollbarColor());
    }
}
