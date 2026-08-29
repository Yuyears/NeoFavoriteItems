package mycraft.yuyears.neofavoriteitems.client.ui.layout;

import mycraft.yuyears.neofavoriteitems.client.ui.NfiUiRenderer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class NfiConfigPanel {
    private static final int ROW_HEIGHT = 24;
    private static final int CONTROL_LEFT_MARGIN = 6;
    private static final int CONTROL_RIGHT_MARGIN = 8;
    private final int width;
    private final int labelWidth;
    private final List<NfiConfigRow> rows = new ArrayList<>();
    private final NfiWidgetGroup group;
    private final int baseX;
    private final int baseY;
    private int viewportHeight = Integer.MAX_VALUE;
    private int scrollOffset;

    public NfiConfigPanel(int x, int y, int width, int labelWidth) {
        this.width = width;
        this.labelWidth = labelWidth;
        this.baseX = x;
        this.baseY = y;
        this.group = new NfiWidgetGroup(x, y);
    }

    public void add(NfiConfigRow row) {
        int rowY = rows.size() * ROW_HEIGHT;
        row.resize(controlWidth());
        group.addRelative(row.controls(), labelWidth + CONTROL_LEFT_MARGIN, rowY + 2);
        rows.add(row);
        refreshVisibility();
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

    public void renderLabels(GuiGraphics graphics, Font font, int color) {
        if (viewportHeight == Integer.MAX_VALUE) {
            for (int index = 0; index < rows.size(); index++) {
                rows.get(index).renderLabel(graphics, font, group.x(), group.y() + index * ROW_HEIGHT, color);
            }
            return;
        }
        graphics.enableScissor(group.x(), baseY, group.x() + width, baseY + viewportHeight);
        for (int index = 0; index < rows.size(); index++) rows.get(index).renderLabel(graphics, font, group.x(), group.y() + index * ROW_HEIGHT, color);
        renderScrollbar(graphics);
        graphics.disableScissor();
    }

    private int maxScroll() {
        return Math.max(0, rows.size() * ROW_HEIGHT - viewportHeight);
    }

    private void applyScroll() {
        group.setPosition(baseX, baseY - scrollOffset);
        refreshVisibility();
    }

    private void refreshVisibility() {
        for (int index = 0; index < rows.size(); index++) {
            int rowTop = baseY - scrollOffset + index * ROW_HEIGHT;
            boolean visible = rowTop >= baseY && rowTop + ROW_HEIGHT <= baseY + viewportHeight;
            rows.get(index).widgets().forEach(widget -> widget.visible = visible);
        }
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int maximum = maxScroll();
        if (maximum == 0) return;
        int trackX = group.x() + width - 3;
        graphics.fill(trackX, baseY, trackX + 2, baseY + viewportHeight, NfiUiRenderer.scrollbarTrackColor());
        int contentHeight = rows.size() * ROW_HEIGHT;
        int thumbHeight = Math.max(12, viewportHeight * viewportHeight / contentHeight);
        int travel = viewportHeight - thumbHeight;
        int thumbY = baseY + (int) Math.round((double) scrollOffset / maximum * travel);
        graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, NfiUiRenderer.scrollbarColor());
    }
}
