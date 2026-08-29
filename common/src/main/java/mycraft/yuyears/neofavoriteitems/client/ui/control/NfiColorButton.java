package mycraft.yuyears.neofavoriteitems.client.ui.control;

import mycraft.yuyears.neofavoriteitems.client.ui.binding.NfiValueBinding;
import net.minecraft.network.chat.Component;

public final class NfiColorButton {
    private final NfiValueBinding<Integer> binding;
    private final NfiButton button;

    public NfiColorButton(int x, int y, int width, int height, NfiValueBinding<Integer> binding, Runnable onPress) {
        this.binding = binding;
        this.button = new NfiButton(x, y, width, height, Component.empty(), ignored -> onPress.run());
        syncFromBinding();
    }

    public NfiButton widget() {
        return button;
    }

    public void syncFromBinding() {
        button.setMessage(Component.translatable(
            "screen.neo_favorite_items.color",
            String.format("#%08X", binding.get())
        ));
    }
}
