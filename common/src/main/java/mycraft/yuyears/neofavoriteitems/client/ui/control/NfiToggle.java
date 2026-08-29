package mycraft.yuyears.neofavoriteitems.client.ui.control;

import java.util.function.Function;
import mycraft.yuyears.neofavoriteitems.client.ui.binding.NfiValueBinding;
import net.minecraft.network.chat.Component;

public final class NfiToggle {
    private final NfiValueBinding<Boolean> binding;
    private final Function<Boolean, Component> messageFactory;
    private final NfiButton button;

    public NfiToggle(int x, int y, int width, int height, NfiValueBinding<Boolean> binding,
                     Function<Boolean, Component> messageFactory) {
        this.binding = binding;
        this.messageFactory = messageFactory;
        this.button = new NfiButton(x, y, width, height, Component.empty(), ignored -> toggle());
        syncFromBinding();
    }

    public NfiButton widget() {
        return button;
    }

    public void syncFromBinding() {
        button.setMessage(messageFactory.apply(binding.get()));
        button.setSelected(binding.get());
    }

    private void toggle() {
        binding.set(!binding.get());
        syncFromBinding();
    }
}
