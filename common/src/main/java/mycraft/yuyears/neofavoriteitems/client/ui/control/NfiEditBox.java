package mycraft.yuyears.neofavoriteitems.client.ui.control;

import mycraft.yuyears.neofavoriteitems.client.ui.NfiCanvas;
import mycraft.yuyears.neofavoriteitems.client.ui.NfiUiRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class NfiEditBox extends EditBox {
    private final Font font;
    private int displayPosition;
    private int highlightPosition;
    private long focusedAt;
    private NfiCanvas leadingCanvas;
    private int leadingCanvasWidth;

    public NfiEditBox(Font font, int x, int y, int width, int height, Component hint) {
        super(font, x, y, width, height, hint);
        this.font = font;
        setBordered(false);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        int textColor = NfiUiRenderer.controlTextColor(isActive());
        NfiUiRenderer.control(graphics, x, y, width, height, isHoveredOrFocused(), isActive(), false);
        if (leadingCanvas != null) {
            leadingCanvas.render(graphics, x + 2, y + 2, leadingCanvasWidth, height - 4, textColor);
        }

        int contentX = x + 4 + leadingCanvasWidth;
        int contentY = y + (height - font.lineHeight) / 2 + 1;
        int contentWidth = getInnerWidth();
        String value = getValue();
        displayPosition = Mth.clamp(displayPosition, 0, value.length());
        String visible = font.plainSubstrByWidth(value.substring(displayPosition), contentWidth);
        int visibleEnd = displayPosition + visible.length();

        graphics.enableScissor(contentX, y + 1, contentX + contentWidth, y + height - 1);
        renderSelection(graphics, contentX, contentY, visible, visibleEnd);
        if (!visible.isEmpty()) {
            NfiUiRenderer.text(graphics, font, Component.literal(visible), contentX, contentY, textColor);
        }
        renderCursor(graphics, contentX, contentY, visible, visibleEnd, textColor);
        graphics.disableScissor();
    }

    @Override
    public int getInnerWidth() {
        return Math.max(1, getWidth() - 8 - leadingCanvasWidth);
    }

    public void setLeadingCanvas(NfiCanvas canvas, int width) {
        leadingCanvas = canvas;
        leadingCanvasWidth = canvas == null ? 0 : Math.max(1, width);
    }

    @Override
    public void setCursorPosition(int position) {
        super.setCursorPosition(position);
        scrollTo(position);
    }

    @Override
    public void setHighlightPos(int position) {
        super.setHighlightPos(position);
        highlightPosition = Mth.clamp(position, 0, getValue().length());
        scrollTo(highlightPosition);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX - 4.0D - leadingCanvasWidth, mouseY);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) focusedAt = System.currentTimeMillis();
    }

    private void scrollTo(int position) {
        if (font == null) return;
        String value = getValue();
        displayPosition = Math.min(displayPosition, value.length());
        int width = getInnerWidth();
        String visible = font.plainSubstrByWidth(value.substring(displayPosition), width);
        int visibleEnd = displayPosition + visible.length();
        if (position == displayPosition) {
            displayPosition -= font.plainSubstrByWidth(value, width, true).length();
        } else if (position > visibleEnd) {
            displayPosition += position - visibleEnd;
        } else if (position <= displayPosition) {
            displayPosition -= displayPosition - position;
        }
        displayPosition = Mth.clamp(displayPosition, 0, value.length());
    }

    private void renderSelection(GuiGraphics graphics, int x, int y, String visible, int visibleEnd) {
        int start = Math.max(displayPosition, Math.min(getCursorPosition(), highlightPosition));
        int end = Math.min(visibleEnd, Math.max(getCursorPosition(), highlightPosition));
        if (start >= end) return;
        int startX = x + font.width(visible.substring(0, start - displayPosition));
        int endX = x + font.width(visible.substring(0, end - displayPosition));
        int color = (NfiUiRenderer.accentColor() & 0x00FFFFFF) | 0x66000000;
        graphics.fill(startX, y - 1, endX, y + font.lineHeight + 1, color);
    }

    private void renderCursor(GuiGraphics graphics, int x, int y, String visible, int visibleEnd, int color) {
        int cursor = getCursorPosition();
        if (!isFocused() || (System.currentTimeMillis() - focusedAt) / 300L % 2L != 0L
            || cursor < displayPosition || cursor > visibleEnd) return;
        int cursorX = x + font.width(visible.substring(0, cursor - displayPosition));
        graphics.fill(cursorX, y - 1, cursorX + 1, y + font.lineHeight + 1, color);
    }

}
