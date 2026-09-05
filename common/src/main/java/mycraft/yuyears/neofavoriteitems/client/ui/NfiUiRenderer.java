package mycraft.yuyears.neofavoriteitems.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** Small shared renderer for the compact, framed NFI configuration surfaces. */
public final class NfiUiRenderer {
    private static final float SHADOW_OFFSET = 0.6F;
    private static NfiUiTheme theme = NfiUiTheme.DEFAULT;
    private static final int PANEL_BORDER = 0xD8D8D8D8;
    private static final int DIVIDER = 0x55909090;
    private static final int CONTROL_BORDER = 0xFF777777;
    private static final int CONTROL_BORDER_HOVER = 0xFFFFFFFF;
    private static final int CONTROL_BORDER_DISABLED = 0xFF424242;
    private static final int CONTROL_FILL = 0xE61B1B1B;
    private static final int CONTROL_FILL_HOVER = 0xF02D2D2D;
    private static final int CONTROL_FILL_DISABLED = 0xD8141414;
    private static final int CONTROL_INNER_SHADOW = 0x33404040;
    private static final int SELECTED = 0xFFFFC94A;
    private static final int SEGMENT_BORDER = 0xFFFFFFFF;
    // Matches framed control shadow while remaining visible on the white segment fill.
    private static final int SEGMENT_TOP_LINE = 0x88606060;

    private NfiUiRenderer() {}

    public static void setTheme(NfiUiTheme value) {
        theme = value == null ? NfiUiTheme.DEFAULT : value;
    }

    public static int controlTextColor(boolean active) {
        return active ? theme.text : (theme.text & 0x00FFFFFF) | 0x88000000;
    }

    public static int wellColor() {
        return theme.well;
    }

    public static int accentColor() {
        return theme.accent;
    }

    public static int scrollbarColor() {
        return hoverFill();
    }

    public static int scrollbarTrackColor() {
        int neutral = (theme.background & 0xFF000000)
            | (theme == NfiUiTheme.DARK ? 0x00FFFFFF : 0x00000000);
        return mixRgb(theme.background, neutral, 0.85F);
    }

    private static int hoverFill() {
        return mixRgb(theme.control, theme.accent, 0.7F);
    }

    private static int mixRgb(int first, int second, float firstWeight) {
        float secondWeight = 1.0F - firstWeight;
        int r = Math.round(((first >> 16) & 0xFF) * firstWeight + ((second >> 16) & 0xFF) * secondWeight);
        int g = Math.round(((first >> 8) & 0xFF) * firstWeight + ((second >> 8) & 0xFF) * secondWeight);
        int b = Math.round((first & 0xFF) * firstWeight + (second & 0xFF) * secondWeight);
        int alpha = Math.max(first >>> 24, second >>> 24);
        return alpha << 24 | r << 16 | g << 8 | b;
    }

    public static int shadowColor() {
        return theme.shadow;
    }

    public static float shadowOffset() {
        return SHADOW_OFFSET;
    }

    public static void text(GuiGraphics graphics, Font font, Component value, int x, int y, int color) {
        drawText(graphics, font, value, x, y, color);
    }

    private static void drawText(GuiGraphics graphics, Font font, Component value, int x, int y, int color) {
        offsetText(graphics, font, value, x, y, theme.shadow, SHADOW_OFFSET, SHADOW_OFFSET);
        graphics.drawString(font, value, x, y, color, false);
    }

    private static void offsetText(GuiGraphics graphics, Font font, Component value, int x, int y,
                                   int color, float offsetX, float offsetY) {
        graphics.pose().pushPose();
        graphics.pose().translate(offsetX, offsetY, 0.0F);
        graphics.drawString(font, value, x, y, color, false);
        graphics.pose().popPose();
    }

    public static void centeredText(GuiGraphics graphics, Font font, Component value,
                                    int centerX, int y, int color) {
        drawText(graphics, font, value, centerX - font.width(value) / 2, y, color);
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        panel(graphics, x, y, width, height, theme.background);
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        if (width <= 1 || height <= 1) return;
        graphics.fill(x, y, x + width, y + 1, PANEL_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_BORDER);
        graphics.fill(x, y + 1, x + 1, y + height - 1, PANEL_BORDER);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, PANEL_BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
    }

    public static void divider(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, DIVIDER);
    }

    public static void sectionHeader(GuiGraphics graphics, Font font, Component value,
                                     int x, int y, int width, int height, boolean hovered, boolean active) {
        int textColor = controlTextColor(active);
        text(graphics, font, value, x + 4, y + (height - font.lineHeight) / 2, textColor);
        int lineX = x + 6 + font.width(value);
        int lineColor = hovered && active ? CONTROL_BORDER_HOVER : DIVIDER;
        if (lineX < x + width - 4) graphics.fill(lineX, y + height / 2, x + width - 4, y + height / 2 + 1, lineColor);
    }

    public static void control(GuiGraphics graphics, int x, int y, int width, int height,
                               boolean hovered, boolean active, boolean selected) {
        int border = !active ? CONTROL_BORDER_DISABLED : hovered ? CONTROL_BORDER_HOVER : CONTROL_BORDER;
        int fill = !active ? CONTROL_FILL_DISABLED : hovered ? theme.accent : theme.control;
        graphics.fill(x, y, x + width, y + height, border);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 3, CONTROL_INNER_SHADOW);
        if (selected) graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, theme.accent);
    }

    /** Right half of a joined control. Its left edge is the single shared divider. */
    public static void dropdownTrigger(GuiGraphics graphics, int x, int y, int width, int height,
                                       boolean hovered, boolean active) {
        int border = !active ? CONTROL_BORDER_DISABLED : hovered ? CONTROL_BORDER_HOVER : CONTROL_BORDER;
        int fill = !active ? CONTROL_FILL_DISABLED : hovered ? theme.accent : theme.control;
        graphics.fill(x, y, x + width, y + height, border);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        graphics.fill(x + 1, y + 2, x + width - 2, y + 3, CONTROL_INNER_SHADOW);
    }

    public static void primaryTab(GuiGraphics graphics, int x, int y, int width, int height,
                                  boolean hovered, boolean active, boolean selected,
        boolean first, boolean last, int groupWidth) {
        if (width <= 0 || height <= 0) return;
        int lineColor = !active ? CONTROL_BORDER_DISABLED : selected ? theme.accent : theme.shadow;
        graphics.fill(x, y + height - 1, x + width, y + height, lineColor);
        if (selected) {
            graphics.fill(x, y + height - 2, x + width, y + height - 1, theme.accent);
        } else if (hovered && active) {
            graphics.fill(x, y + height - 2, x + width, y + height - 1, theme.control);
        }
    }

    public static void segmentedTab(GuiGraphics graphics, int x, int y, int width, int height,
                                    boolean hovered, boolean active, boolean selected,
        boolean first, boolean last, int groupWidth) {
        if (width <= 0 || height <= 0) return;
        int fill = selected ? theme.accent : hovered && active ? hoverFill() : theme.control;
        int border = CONTROL_BORDER;
        int left = x + (first ? 1 : 0);
        int right = x + width - (last ? 1 : 0);
        graphics.fill(x, y, x + width, y + 1, border);
        graphics.fill(x, y + height - 1, x + width, y + height, border);
        graphics.fill(left, y + 1, right, y + height - 1, fill);
        if (first) graphics.fill(x, y + 1, x + 1, y + height - 1, border);
        if (last) graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, border);
        int shadowLeft = first ? x + 2 : x;
        int shadowRight = last ? x + width - 2 : x + width;
        if (shadowRight > shadowLeft) {
            graphics.fill(shadowLeft, y + 2, shadowRight, y + 3, CONTROL_INNER_SHADOW);
        }
        if (selected) {
            int lineWidth = Math.max(6, width / 3);
            int lineLeft = x + (width - lineWidth) / 2;
            graphics.fill(lineLeft, y + height - 4, lineLeft + lineWidth, y + height - 3, SEGMENT_BORDER);
        }
    }

    public static void valueWell(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, CONTROL_BORDER_DISABLED);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, theme.well);
    }

    public static void slider(GuiGraphics graphics, int x, int y, int width, int height,
                              double value, boolean hovered, boolean active) {
        control(graphics, x, y, width, height, hovered, active, false);
        valueWell(graphics, x + 3, y + 3, width - 6, height - 6);
        int thumbWidth = hovered ? 6 : 4;
        int travel = Math.max(0, width - 10 - thumbWidth);
        int thumbX = x + 5 + (int) Math.round(Math.clamp(value, 0.0D, 1.0D) * travel);
        control(graphics, thumbX, y + 5, thumbWidth, height - 10, hovered, active, false);
    }
}
