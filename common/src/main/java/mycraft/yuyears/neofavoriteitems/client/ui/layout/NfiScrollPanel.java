package mycraft.yuyears.neofavoriteitems.client.ui.layout;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

public final class NfiScrollPanel {
    private final NfiWidgetGroup content;
    private final int width;
    private final int height;
    private int x;
    private int y;
    private int contentHeight;
    private int scrollOffset;
    private AbstractWidget focused;

    public NfiScrollPanel(int x, int y, int width, int height, int contentHeight, NfiWidgetGroup content) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.content = content;
        setContentHeight(contentHeight);
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        applyLayout();
    }

    public void setContentHeight(int contentHeight) {
        this.contentHeight = Math.max(0, contentHeight);
        scrollOffset = Math.min(scrollOffset, maxScroll());
        applyLayout();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!contains(mouseX, mouseY) || maxScroll() == 0) return false;
        scrollOffset = Math.clamp(scrollOffset + (delta < 0 ? 18 : -18), 0, maxScroll());
        applyLayout();
        return true;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.enableScissor(x, y, x + width, y + height);
        content.widgets().forEach(widget -> widget.render(graphics, mouseX, mouseY, delta));
        graphics.disableScissor();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) return false;
        var widgets = content.widgets();
        for (int index = widgets.size() - 1; index >= 0; index--) {
            AbstractWidget widget = widgets.get(index);
            if (widget.mouseClicked(mouseX, mouseY, button)) {
                setFocused(widget);
                return true;
            }
        }
        setFocused(null);
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return focused != null && focused.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return focused != null && focused.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return focused != null && focused.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return focused != null && focused.charTyped(codePoint, modifiers);
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - height);
    }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void applyLayout() {
        content.setPosition(x, y - scrollOffset);
    }

    private void setFocused(AbstractWidget widget) {
        if (focused != null) focused.setFocused(false);
        focused = widget;
        if (focused != null) focused.setFocused(true);
    }
}
