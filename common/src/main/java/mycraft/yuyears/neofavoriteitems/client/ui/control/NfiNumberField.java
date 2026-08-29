package mycraft.yuyears.neofavoriteitems.client.ui.control;

import java.util.function.Function;
import mycraft.yuyears.neofavoriteitems.client.ui.binding.NfiValidationResult;
import mycraft.yuyears.neofavoriteitems.client.ui.binding.NfiValueBinding;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public final class NfiNumberField<T extends Number> {
    private final NfiValueBinding<T> binding;
    private final Function<String, T> parser;
    private final Function<T, String> formatter;
    private final NfiEditBox field;
    private NfiValidationResult validation = NfiValidationResult.ok();
    private boolean syncing;

    public NfiNumberField(Font font, int x, int y, int width, int height, Component hint,
                          NfiValueBinding<T> binding, Function<String, T> parser, Function<T, String> formatter) {
        this.binding = binding;
        this.parser = parser;
        this.formatter = formatter;
        this.field = new NfiEditBox(font, x, y, width, height, hint);
        this.field.setResponder(this::acceptText);
        syncFromBinding();
    }

    public NfiEditBox widget() {
        return field;
    }

    public NfiValidationResult validation() {
        return validation;
    }

    public void syncFromBinding() {
        syncing = true;
        field.setValue(formatter.apply(binding.get()));
        syncing = false;
        validation = NfiValidationResult.ok();
    }

    private void acceptText(String text) {
        if (syncing) return;
        try {
            validation = binding.set(parser.apply(text));
        } catch (NumberFormatException exception) {
            validation = NfiValidationResult.error(Component.translatable("screen.neo_favorite_items.invalid_number"));
        }
    }
}
