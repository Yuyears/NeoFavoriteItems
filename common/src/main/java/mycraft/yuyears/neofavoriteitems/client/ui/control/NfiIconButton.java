package mycraft.yuyears.neofavoriteitems.client.ui.control;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public final class NfiIconButton {
    private NfiIconButton() {}

    public static Button create(int x, int y, int size, Component icon, Component tooltip, Button.OnPress onPress) {
        Button button = new NfiButton(x, y, size, size, icon, onPress);
        button.setTooltip(Tooltip.create(tooltip));
        return button;
    }
}
