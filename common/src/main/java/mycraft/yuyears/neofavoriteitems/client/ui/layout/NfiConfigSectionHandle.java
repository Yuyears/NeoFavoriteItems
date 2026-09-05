package mycraft.yuyears.neofavoriteitems.client.ui.layout;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/** Explicit parent handle for building nested configuration sections. */
public final class NfiConfigSectionHandle {
    private final NfiConfigPanel panel;
    private final NfiConfigSection section;

    NfiConfigSectionHandle(NfiConfigPanel panel, NfiConfigSection section) {
        this.panel = panel;
        this.section = section;
    }

    public NfiConfigSectionHandle add(Component label, AbstractWidget widget) {
        panel.addToSection(section, new NfiConfigRow(label, widget));
        return this;
    }

    public NfiConfigSectionHandle add(Component label, NfiWidgetGroup group,
                                      java.util.List<AbstractWidget> widgets) {
        panel.addToSection(section, new NfiConfigRow(label, group, widgets));
        return this;
    }

    public NfiConfigSectionHandle child(Component title, String stableKey) {
        return panel.beginHandleChild(section, title, stableKey);
    }

    public NfiConfigSection section() { return section; }
}
