package mycraft.yuyears.neofavoriteitems.client.ui.control;

import mycraft.yuyears.neofavoriteitems.client.ui.NfiCanvas;
import mycraft.yuyears.neofavoriteitems.client.ui.NfiCanvases;
import mycraft.yuyears.neofavoriteitems.client.ui.NfiUiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class NfiButton extends Button {
    private static final int DROPDOWN_TRIGGER_WIDTH = 24;
    private boolean selected;
    private boolean dropdown;
    private boolean primaryTab;
    private boolean segmentedTab;
    private boolean firstSegment;
    private boolean lastSegment;
    private int tabGroupWidth;
    private ResourceLocation icon;
    private NfiCanvas canvas;
    private int contentRightInset;
    private boolean embeddedDropdownTrigger;
    private boolean silent;
    private boolean sectionHeader;
    private boolean plain;

    public NfiButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setDropdown(boolean dropdown) {
        this.dropdown = dropdown;
    }

    public void setIcon(ResourceLocation icon) { this.icon = icon; }
    public void setCanvas(NfiCanvas canvas) { this.canvas = canvas; }
    public void setContentRightInset(int inset) { contentRightInset = Math.max(0, inset); }
    public void setEmbeddedDropdownTrigger(boolean embedded) { embeddedDropdownTrigger = embedded; }
    public void setSilent(boolean silent) { this.silent = silent; }
    public void setSectionHeader(boolean value) { sectionHeader = value; }
    public void setPlain(boolean value) { plain = value; }

    @Override
    public void playDownSound(SoundManager soundManager) {
        if (!silent) super.playDownSound(soundManager);
    }

    public void setPrimaryTab(boolean primaryTab, boolean first, boolean last, int groupWidth) {
        this.primaryTab = primaryTab;
        this.firstSegment = first;
        this.lastSegment = last;
        this.tabGroupWidth = groupWidth;
    }

    public void setSegmentedTab(boolean segmentedTab, boolean firstSegment, boolean lastSegment, int groupWidth) {
        this.segmentedTab = segmentedTab;
        this.firstSegment = firstSegment;
        this.lastSegment = lastSegment;
        this.tabGroupWidth = groupWidth;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (plain) {
            NfiUiRenderer.centeredText(graphics, Minecraft.getInstance().font, getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - Minecraft.getInstance().font.lineHeight) / 2 + 1,
                NfiUiRenderer.controlTextColor(active || isHoveredOrFocused()));
            return;
        } else if (sectionHeader) {
            NfiUiRenderer.sectionHeader(graphics, Minecraft.getInstance().font, getMessage(),
                getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), active);
            return;
        } else if (embeddedDropdownTrigger) {
            NfiUiRenderer.dropdownTrigger(graphics, getX(), getY(), getWidth(), getHeight(),
                isHoveredOrFocused(), active);
            if (canvas == null) {
                NfiCanvases.DROPDOWN.render(graphics, getX(), getY(), getWidth(), getHeight(),
                    NfiUiRenderer.controlTextColor(active));
            } else {
                canvas.render(graphics, getX() + 2, getY() + 2, getWidth() - 4, getHeight() - 4,
                    NfiUiRenderer.controlTextColor(active));
            }
            return;
        } else if (primaryTab) {
            NfiUiRenderer.primaryTab(graphics, getX(), getY(), getWidth(), getHeight(),
                isHoveredOrFocused(), active, selected, firstSegment, lastSegment, tabGroupWidth);
        } else if (segmentedTab) {
            NfiUiRenderer.segmentedTab(graphics, getX(), getY(), getWidth(), getHeight(),
                isHoveredOrFocused(), active, selected, firstSegment, lastSegment, tabGroupWidth);
        } else if (dropdown) {
            boolean triggerHovered = active && mouseX >= getX() + getWidth() - DROPDOWN_TRIGGER_WIDTH
                && mouseX < getX() + getWidth() && mouseY >= getY() && mouseY < getY() + getHeight();
            NfiUiRenderer.control(graphics, getX(), getY(), getWidth(), getHeight(),
                isHoveredOrFocused() && !triggerHovered, active, selected);
            NfiUiRenderer.dropdownTrigger(graphics, getX() + getWidth() - DROPDOWN_TRIGGER_WIDTH,
                getY(), DROPDOWN_TRIGGER_WIDTH, getHeight(), triggerHovered, active);
        } else {
            NfiUiRenderer.control(graphics, getX(), getY(), getWidth(), getHeight(),
                isHoveredOrFocused(), active, selected);
        }
        var font = Minecraft.getInstance().font;
        int textRight = getX() + getWidth() - Math.max(dropdown ? DROPDOWN_TRIGGER_WIDTH : 0, contentRightInset);
        graphics.enableScissor(getX() + 2, getY(), textRight - 2, getY() + getHeight());
        int contentColor = NfiUiRenderer.controlTextColor(active || selected);
        if (canvas == null) {
            NfiUiRenderer.centeredText(graphics, font, getMessage(), (getX() + textRight) / 2,
                getY() + (getHeight() - font.lineHeight) / 2 + 1, contentColor);
        } else {
            canvas.render(graphics, getX() + 2, getY() + 2,
                Math.max(1, textRight - getX() - 4), getHeight() - 4, contentColor);
        }
        graphics.disableScissor();
        if (icon != null) graphics.blit(icon, getX() + (getWidth() - 16) / 2, getY() + 2, 0, 0, 16, 16, 16, 16);
        if (dropdown) {
            NfiCanvases.DROPDOWN.render(graphics, getX() + getWidth() - DROPDOWN_TRIGGER_WIDTH,
                getY(), DROPDOWN_TRIGGER_WIDTH, getHeight(), NfiUiRenderer.controlTextColor(active));
        }
    }
}
