package mycraft.yuyears.neofavoriteitems.client.ui.control;

import java.util.ArrayList;
import java.util.List;
import mycraft.yuyears.neofavoriteitems.client.ui.NfiUiRenderer;
import mycraft.yuyears.neofavoriteitems.client.ui.binding.NfiValueBinding;
import mycraft.yuyears.neofavoriteitems.client.ui.layout.NfiWidgetGroup;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** RGBA/HSV picker. Popup edits local draft until Apply. */
public final class NfiColorPicker {
    private static final int POPUP_GAP = 5;
    private static final int POPUP_HEIGHT = 225;
    private final NfiValueBinding<Integer> binding;
    private final NfiColorButton colorButton;
    private final List<AbstractWidget> popupWidgets = new ArrayList<>();
    private final List<NfiSlider> sliders = new ArrayList<>();
    private final List<NfiEditBox> rgbFields = new ArrayList<>();
    private final NfiWidgetGroup group;
    private final NfiEditBox hexField;
    private final NfiButton applyButton;
    private final NfiButton cancelButton;
    private final NfiButton defaultButton;
    private final SvPicker svPicker;
    private boolean open;
    private boolean syncing;
    private int draftColor;
    private int originalColor;
    private final int defaultColor;
    private int popupTop;
    private AbstractWidget activePopupWidget;

    public NfiColorPicker(int x, int y, int width, NfiValueBinding<Integer> binding) {
        this(null, x, y, width, binding);
    }

    public NfiColorPicker(Font font, int x, int y, int width, NfiValueBinding<Integer> binding) {
        this.binding = binding;
        this.defaultColor = binding.defaultValue();
        this.group = new NfiWidgetGroup(x, y);
        colorButton = new NfiColorButton(x, y, width, 20, binding, () -> setOpen(!open));
        hexField = new NfiEditBox(font, x, y, width, 20, Component.translatable("screen.neo_favorite_items.color_hex"));
        hexField.setResponder(this::parseHex);
        applyButton = new NfiButton(x, y, 54, 20, Component.translatable("screen.neo_favorite_items.apply"), ignored -> commit());
        cancelButton = new NfiButton(x + 58, y, 54, 20, Component.translatable("screen.neo_favorite_items.cancel"), ignored -> cancel());
        defaultButton = new NfiButton(x + 116, y, 54, 20, Component.translatable("screen.neo_favorite_items.reset"), ignored -> setDraft(defaultColor));
        svPicker = new SvPicker(x, y, width, 76);
        group.addRelative(colorButton.widget(), 0, 0);
        for (int channel = 0; channel < 3; channel++) {
            final int c = channel;
            NfiEditBox field = new NfiEditBox(font, 0, 0, 40, 20, Component.literal("RGB"));
            field.setResponder(value -> parseRgbField(c, value));
            rgbFields.add(field);
        }
        sliders.add(hsvSlider(0, "H", 0, 360, true));
        sliders.add(hsvSlider(3, "A", 0, 255, true));
        popupWidgets.addAll(sliders);
        popupWidgets.addAll(rgbFields);
        popupWidgets.add(svPicker);
        popupWidgets.add(hexField);
        popupWidgets.add(applyButton);
        popupWidgets.add(cancelButton);
        popupWidgets.add(defaultButton);
        setOpen(false);
        syncFromBinding();
    }

    public Button button() { return colorButton.widget(); }
    /** Legacy RGB widgets; retained for callers that only render slider controls. */
    public List<? extends AbstractWidget> popupWidgets() { return rgbFields; }
    public List<? extends AbstractWidget> allPopupWidgets() { return popupWidgets; }
    public NfiWidgetGroup group() { return group; }
    public void setWidth(int width) { colorButton.widget().setWidth(width); hexField.setWidth(width); sliders.forEach(s -> s.setWidth(width)); }
    public boolean isOpen() { return open; }
    public AbstractWidget activePopupWidget() { return activePopupWidget; }
    public void close() { cancel(); }

    /** Routes popup input before the parent screen can hit lower controls. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open) return false;
        activePopupWidget = null;
        if (svPicker.isMouseOver(mouseX, mouseY) && svPicker.mouseClicked(mouseX, mouseY, button)) {
            activePopupWidget = svPicker;
            return true;
        }
        for (int i = popupWidgets.size() - 1; i >= 0; i--) {
            AbstractWidget widget = popupWidgets.get(i);
            if (widget.visible && widget.mouseClicked(mouseX, mouseY, button)) { activePopupWidget = widget; return true; }
        }
        if (mouseX < group.x() - 10 || mouseX > group.x() + hexField.getWidth() + 10
            || mouseY < popupTop || mouseY > popupTop + POPUP_HEIGHT) cancel();
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!open) return false;
        if (activePopupWidget != null) { activePopupWidget.mouseReleased(mouseX, mouseY, button); activePopupWidget = null; return true; }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!open) return false;
        if (activePopupWidget != null) { activePopupWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY); return true; }
        return true;
    }

    public void syncFromBinding() {
        if (open) return;
        draftColor = originalColor = binding.get();
        colorButton.syncFromBinding();
        syncDraftControls();
    }

    public void layoutPopup(int screenHeight) {
        int below = group.y() + 20 + POPUP_GAP;
        popupTop = below + POPUP_HEIGHT <= screenHeight ? below
            : Math.clamp(group.y() - POPUP_GAP - POPUP_HEIGHT, 0, Math.max(0, screenHeight - POPUP_HEIGHT));
        for (int i = 0; i < 3; i++) { rgbFields.get(i).setX(group.x() + i * 42); rgbFields.get(i).setY(popupTop + 20); }
        svPicker.setX(group.x()); svPicker.setY(popupTop + 45); svPicker.setWidth(Math.max(80, hexField.getWidth()));
        sliders.get(0).setX(group.x()); sliders.get(0).setY(popupTop + 125);
        sliders.get(0).setWidth(Math.max(80, hexField.getWidth()));
        sliders.get(1).setX(group.x()); sliders.get(1).setY(popupTop + 145);
        sliders.get(1).setWidth(Math.max(80, hexField.getWidth()));
        hexField.setX(group.x()); hexField.setY(popupTop + 170);
        applyButton.setX(group.x()); applyButton.setY(popupTop + 195);
        cancelButton.setX(group.x() + 58); cancelButton.setY(popupTop + 195);
        defaultButton.setX(group.x() + 116); defaultButton.setY(popupTop + 195);
    }

    public void renderPopup(GuiGraphics graphics, int mouseX, int mouseY, float delta, int textColor) {
        if (!open) return;
        graphics.pose().pushPose(); graphics.pose().translate(0.0f, 0.0f, 500.0f);
        NfiUiRenderer.panel(graphics, group.x() - 10, popupTop, Math.max(170, hexField.getWidth() + 20), POPUP_HEIGHT);
        var font = net.minecraft.client.Minecraft.getInstance().font;
        NfiUiRenderer.text(graphics, font, Component.translatable("screen.neo_favorite_items.color_picker"), group.x(), popupTop + 3, textColor);
        svPicker.render(graphics, mouseX, mouseY, delta);
        rgbFields.forEach(f -> f.render(graphics, mouseX, mouseY, delta));
        int previewX = group.x() + 126;
        for (int px = 0; px < 40; px += 4) for (int py = 0; py < 20; py += 4) {
            int cell = (((px / 4) + (py / 4)) & 1) == 0 ? 0xFFD0D0D0 : 0xFF888888;
            graphics.fill(previewX + px, popupTop + 20 + py, previewX + px + 4, popupTop + 24 + py, cell);
        }
        graphics.fill(previewX, popupTop + 20, previewX + 40, popupTop + 40, draftColor);
        sliders.forEach(s -> s.render(graphics, mouseX, mouseY, delta));
        hexField.render(graphics, mouseX, mouseY, delta);
        applyButton.render(graphics, mouseX, mouseY, delta);
        cancelButton.render(graphics, mouseX, mouseY, delta);
        defaultButton.render(graphics, mouseX, mouseY, delta);
        graphics.pose().popPose();
    }

    private NfiSlider channelSlider(int channel, String label, double min, double max) {
        var b = NfiValueBinding.unchecked(() -> (double) component(draftColor, channel), v -> setDraft(withComponent(draftColor, channel, (int) Math.round(v))), () -> (double) component(draftColor, channel), this::syncDraftControls);
        return new NfiSlider(0, 0, 100, 18, min, max, b, v -> Component.literal(label + ": " + Math.round(v)));
    }
    private NfiSlider hsvSlider(int channel, String label, double min, double max, boolean gradient) {
        var b = NfiValueBinding.unchecked(() -> hsv(draftColor, channel), v -> setDraft(fromHsv(draftColor, channel, v)), () -> hsv(draftColor, channel), this::syncDraftControls);
        return gradient ? new GradientSlider(0, 0, 100, 18, min, max, b, label)
            : new NfiSlider(0, 0, 100, 18, min, max, b, v -> Component.literal(label + ": " + Math.round(v)));
    }
    private void setOpen(boolean value) { open = value; if (open) { originalColor = binding.get(); draftColor = originalColor; syncDraftControls(); } popupWidgets.forEach(w -> w.visible = open); }
    private void setDraft(int value) { draftColor = value; syncDraftControls(); }
    private void commit() { binding.set(draftColor); setOpen(false); colorButton.syncFromBinding(); }
    private void cancel() { if (open) { draftColor = originalColor; setOpen(false); } }
    private void parseHex(String text) {
        if (syncing) return; String value = text.trim(); if (value.startsWith("#")) value = value.substring(1);
        if ((value.length() == 6 || value.length() == 8) && value.matches("[0-9a-fA-F]+")) { int parsed = (int) Long.parseLong(value, 16); setDraft(value.length() == 6 ? 0xFF000000 | parsed : parsed); }
    }
    private void syncDraftControls() { syncing = true; sliders.forEach(NfiSlider::syncFromBinding); for (int i = 0; i < rgbFields.size(); i++) rgbFields.get(i).setValue(Integer.toString(component(draftColor, i))); hexField.setValue(String.format("#%08X", draftColor)); syncing = false; }
    private void parseRgbField(int channel, String text) { if (syncing) return; try { setDraft(withComponent(draftColor, channel, Integer.parseInt(text.trim()))); } catch (NumberFormatException ignored) {} }
    private static int component(int color, int channel) { int shift = switch (channel) { case 0 -> 16; case 1 -> 8; case 2 -> 0; case 3 -> 24; default -> 0; }; return (color >> shift) & 0xFF; }
    private static int withComponent(int color, int channel, int value) { int shift = switch (channel) { case 0 -> 16; case 1 -> 8; case 2 -> 0; case 3 -> 24; default -> 0; }; return (color & ~(0xFF << shift)) | (Math.clamp(value, 0, 255) << shift); }
    private static double hsv(int color, int channel) { if (channel == 3) return component(color, 3); float[] h = java.awt.Color.RGBtoHSB(component(color, 0), component(color, 1), component(color, 2), null); return channel == 0 ? h[0] * 360.0 : h[channel] * 100.0; }
    private static int fromHsv(int color, int channel, double value) { if (channel == 3) return withComponent(color, 3, (int) Math.round(value)); float[] h = java.awt.Color.RGBtoHSB(component(color, 0), component(color, 1), component(color, 2), null); if (channel == 0) h[0] = (float) (Math.clamp(value, 0.0, 360.0) / 360.0); else h[channel] = (float) (Math.clamp(value, 0.0, 100.0) / 100.0); return (color & 0xFF000000) | (java.awt.Color.HSBtoRGB(h[0], h[1], h[2]) & 0x00FFFFFF); }

    private final class GradientSlider extends NfiSlider {
        private final String label;
        GradientSlider(int x, int y, int width, int height, double min, double max, NfiValueBinding<Double> binding, String label) {
            super(x, y, width, height, min, max, binding, v -> Component.literal(label + ": " + Math.round(v)));
            this.label = label;
        }
        @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            super.renderWidget(graphics, mouseX, mouseY, delta);
            int w = getWidth();
            int baseRgb = java.awt.Color.HSBtoRGB((float) (hsv(draftColor, 0) / 360.0),
                (float) (hsv(draftColor, 1) / 100.0), (float) (hsv(draftColor, 2) / 100.0)) & 0x00FFFFFF;
            for (int x = 3; x < w - 3; x += 2) {
                float t = w <= 1 ? 0 : (float) x / (w - 1);
                if (label.equals("H")) {
                    int hueColor = java.awt.Color.HSBtoRGB(t, 1f, 1f) & 0x00FFFFFF;
                    graphics.fill(getX() + x, getY() + 3, getX() + x + 2, getY() + getHeight() - 3,
                        0xFF000000 | hueColor);
                } else {
                    for (int py = 3; py < getHeight() - 3; py += 2) {
                        int cell = (((x / 4) + (py / 4)) & 1) == 0 ? 0xFFD0D0D0 : 0xFF888888;
                        graphics.fill(getX() + x, getY() + py, getX() + x + 2, getY() + py + 2, cell);
                    }
                    int alpha = (int) Math.round(255 * t);
                    graphics.fill(getX() + x, getY() + 3, getX() + x + 2, getY() + getHeight() - 3, (alpha << 24) | baseRgb);
                }
            }
            int thumbX = getX() + 5 + (int) Math.round(value * Math.max(0, w - 16));
            NfiUiRenderer.control(graphics, thumbX, getY() + 5, 6, getHeight() - 10, isHoveredOrFocused(), active, false);
            if (label.equals("A")) {
                var font = net.minecraft.client.Minecraft.getInstance().font;
                NfiUiRenderer.centeredText(graphics, font,
                    Component.literal(Math.round(value * 100.0) + "%"),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - font.lineHeight) / 2 + 1,
                    NfiUiRenderer.controlTextColor(active));
            }
        }
    }

    private final class SvPicker extends AbstractWidget {
        private boolean dragging;
        SvPicker(int x, int y, int width, int height) { super(x, y, width, height, Component.empty()); visible = false; }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            float hue = (float) (hsv(draftColor, 0) / 360.0);
            for (int px = 0; px < getWidth(); px += 4) for (int py = 0; py < getHeight(); py += 4) {
                float s = getWidth() <= 1 ? 0 : (float) px / (getWidth() - 1);
                float v = getHeight() <= 1 ? 0 : 1.0f - (float) py / (getHeight() - 1);
                int rgb = java.awt.Color.HSBtoRGB(hue, s, v) & 0x00FFFFFF;
                graphics.fill(getX() + px, getY() + py, Math.min(getX() + getWidth(), getX() + px + 4),
                    Math.min(getY() + getHeight(), getY() + py + 4), 0xFF000000 | rgb);
            }
            int cursorX = getX() + (int) Math.round((hsv(draftColor, 1) / 100.0) * Math.max(0, getWidth() - 1));
            int cursorY = getY() + (int) Math.round((1.0 - hsv(draftColor, 2) / 100.0) * Math.max(0, getHeight() - 1));
            graphics.fill(cursorX - 2, cursorY, cursorX + 3, cursorY + 1, 0xFFFFFFFF);
            graphics.fill(cursorX, cursorY - 2, cursorX + 1, cursorY + 3, 0xFFFFFFFF);
        }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) { if (button != 0 || !isMouseOver(mouseX, mouseY)) return false; dragging = true; setSv(mouseX, mouseY); return true; }
        @Override public boolean mouseReleased(double mouseX, double mouseY, int button) { dragging = false; return true; }
        @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) { if (!dragging) return false; setSv(mouseX, mouseY); return true; }
        private void setSv(double mouseX, double mouseY) {
            double s = Math.clamp((mouseX - getX()) / Math.max(1, getWidth() - 1), 0.0, 1.0);
            double v = 1.0 - Math.clamp((mouseY - getY()) / Math.max(1, getHeight() - 1), 0.0, 1.0);
            float hue = (float) (hsv(draftColor, 0) / 360.0);
            int rgb = java.awt.Color.HSBtoRGB(hue, (float) s, (float) v) & 0x00FFFFFF;
            setDraft((draftColor & 0xFF000000) | rgb);
        }
        @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {}
    }
}
