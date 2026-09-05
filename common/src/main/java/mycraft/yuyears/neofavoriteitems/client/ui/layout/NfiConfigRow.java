package mycraft.yuyears.neofavoriteitems.client.ui.layout;

import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import mycraft.yuyears.neofavoriteitems.client.ui.NfiUiRenderer;

public final class NfiConfigRow {
    private final Component label;
    private final NfiWidgetGroup controls;
    private final net.minecraft.client.gui.components.AbstractWidget primaryControl;
    private final List<net.minecraft.client.gui.components.AbstractWidget> rowWidgets;
    private final Map<net.minecraft.client.gui.components.AbstractWidget, Integer> baseWidths = new IdentityHashMap<>();
    private int widgetGap = 8;

    public NfiConfigRow(Component label, net.minecraft.client.gui.components.AbstractWidget control) {
        this.label = label;
        this.primaryControl = control;
        this.rowWidgets = List.of(control);
        this.controls = new NfiWidgetGroup(0, 0);
        this.controls.addRelative(control, 0, 0);
    }

    public NfiConfigRow(Component label, NfiWidgetGroup controls) {
        this(label, controls, controls.widgets());
    }

    public NfiConfigRow(Component label, NfiWidgetGroup controls,
                        List<net.minecraft.client.gui.components.AbstractWidget> rowWidgets) {
        this.label = label;
        this.controls = controls;
        this.primaryControl = null;
        this.rowWidgets = List.copyOf(rowWidgets);
    }

    void resize(int controlWidth) {
        if (primaryControl != null) {
            primaryControl.setWidth(controlWidth);
        }
    }

    NfiWidgetGroup controls() {
        return controls;
    }

    List<net.minecraft.client.gui.components.AbstractWidget> widgets() {
        // Use live group contents so composite rows restore every child after scrolling.
        if (primaryControl != null) return controls.widgets();
        return controls.widgets().isEmpty() ? rowWidgets : controls.widgets();
    }

    public void setWidgetGap(int gap) { widgetGap = Math.max(0, gap); }

    void fitIndentedWidth(int baseWidth, int depth) {
        int count = Math.max(1, rowWidgets.size());
        int reduction = Math.max(0, depth) * 12;
        int perWidget = reduction / count;
        for (var widget : rowWidgets) {
            int original = baseWidths.computeIfAbsent(widget, ignored -> widget.getWidth());
            widget.setWidth(Math.max(1, original - perWidget));
        }
    }

    void compactWidgets(int x, int y) {
        int cursor = x;
        for (var widget : rowWidgets) {
            widget.setX(cursor);
            widget.setY(y);
            cursor += widget.getWidth() + widgetGap;
        }
    }

    void renderLabel(GuiGraphics graphics, net.minecraft.client.gui.Font font, int x, int y, int color) {
        int effectiveColor = rowWidgets.stream().anyMatch(widget -> widget.active) ? color : 0xFF808080;
        NfiUiRenderer.text(graphics, font, label, x, y + 8, effectiveColor);
    }
}
