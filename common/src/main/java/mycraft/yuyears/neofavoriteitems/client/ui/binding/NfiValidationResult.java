package mycraft.yuyears.neofavoriteitems.client.ui.binding;

import net.minecraft.network.chat.Component;

public record NfiValidationResult(boolean valid, Component message) {
    private static final NfiValidationResult OK = new NfiValidationResult(true, Component.empty());

    public static NfiValidationResult ok() {
        return OK;
    }

    public static NfiValidationResult error(Component message) {
        return new NfiValidationResult(false, message);
    }
}
