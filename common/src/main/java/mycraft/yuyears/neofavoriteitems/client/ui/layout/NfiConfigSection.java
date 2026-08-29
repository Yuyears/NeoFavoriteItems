package mycraft.yuyears.neofavoriteitems.client.ui.layout;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import mycraft.yuyears.neofavoriteitems.client.ui.NfiUiRenderer;

public final class NfiConfigSection {
    private final Component title;
    private final NfiWidgetGroup content;

    public NfiConfigSection(Component title, int x, int y) {
        this.title = title;
        this.content = new NfiWidgetGroup(x, y + 14);
    }

    public NfiWidgetGroup content() {
        return content;
    }

    public void setPosition(int x, int y) {
        content.setPosition(x, y + 14);
    }

    public void renderTitle(GuiGraphics graphics, Font font, int color) {
        NfiUiRenderer.text(graphics, font, title, content.x(), content.y() - 14, color);
    }
}
