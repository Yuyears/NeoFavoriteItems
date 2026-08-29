package mycraft.yuyears.neofavoriteitems.client.ui.control;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import mycraft.yuyears.neofavoriteitems.client.ui.binding.NfiValueBinding;
import net.minecraft.network.chat.Component;

public final class NfiTabs<T> {
    private final NfiValueBinding<T> binding;
    private final List<T> values;
    private final List<NfiButton> buttons;

    public NfiTabs(
        int x,
        int y,
        int width,
        int height,
        NfiValueBinding<T> binding,
        List<T> values,
        Function<T, Component> labelFactory
    ) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("values must not be empty");
        }
        this.binding = binding;
        this.values = List.copyOf(values);
        this.buttons = new ArrayList<>(values.size());
        int tabWidth = width / values.size();
        for (int index = 0; index < values.size(); index++) {
            T tab = values.get(index);
            int buttonX = x + index * tabWidth;
            int buttonWidth = index == values.size() - 1 ? width - index * tabWidth : tabWidth;
            buttons.add(new NfiButton(buttonX, y, buttonWidth, height, labelFactory.apply(tab), ignored -> select(tab)));
        }
        syncFromBinding();
    }

    public List<NfiButton> widgets() {
        return buttons;
    }

    public void syncFromBinding() {
        T selected = binding.get();
        for (int index = 0; index < buttons.size(); index++) {
            buttons.get(index).setSelected(values.get(index).equals(selected));
        }
    }

    public void setPrimaryStyle(boolean primary) {
        for (int index = 0; index < buttons.size(); index++) {
            buttons.get(index).setPrimaryTab(primary, index == 0, index == buttons.size() - 1, width());
        }
    }

    public void setSegmentedStyle(boolean segmented) {
        for (int index = 0; index < buttons.size(); index++) {
            buttons.get(index).setSegmentedTab(segmented, index == 0, index == buttons.size() - 1, width());
        }
    }

    public void setEnabled(T value, boolean enabled) {
        int index = values.indexOf(value);
        if (index >= 0) buttons.get(index).active = enabled;
    }

    private void select(T value) {
        binding.set(value);
        syncFromBinding();
    }

    private int width() {
        NfiButton first = buttons.getFirst();
        NfiButton last = buttons.getLast();
        return last.getX() + last.getWidth() - first.getX();
    }
}
