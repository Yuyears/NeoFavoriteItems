package mycraft.yuyears.neofavoriteitems.client.ui.control;

import java.util.List;
import java.util.function.Function;
import mycraft.yuyears.neofavoriteitems.client.ui.binding.NfiValueBinding;
import net.minecraft.network.chat.Component;

public final class NfiCycleButton<T> {
    private final NfiValueBinding<T> binding;
    private final List<T> values;
    private final Function<T, Component> messageFactory;
    private final NfiButton button;

    public NfiCycleButton(
        int x,
        int y,
        int width,
        int height,
        NfiValueBinding<T> binding,
        List<T> values,
        Function<T, Component> messageFactory
    ) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("values must not be empty");
        }
        this.binding = binding;
        this.values = List.copyOf(values);
        this.messageFactory = messageFactory;
        this.button = new NfiButton(x, y, width, height, Component.empty(), ignored -> selectNext());
        syncFromBinding();
    }

    public NfiButton widget() {
        return button;
    }

    public void syncFromBinding() {
        button.setMessage(messageFactory.apply(binding.get()));
        if (tooltipFactory != null) button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltipFactory.apply(binding.get())));
    }

    private Function<T, Component> tooltipFactory;
    public void setTooltipFactory(Function<T, Component> factory) { tooltipFactory = factory; syncFromBinding(); }

    private void selectNext() {
        int current = values.indexOf(binding.get());
        binding.set(values.get((current + 1) % values.size()));
        syncFromBinding();
    }
}
