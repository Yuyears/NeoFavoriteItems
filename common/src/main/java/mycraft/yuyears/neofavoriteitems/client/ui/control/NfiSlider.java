package mycraft.yuyears.neofavoriteitems.client.ui.control;

import java.util.function.Function;
import mycraft.yuyears.neofavoriteitems.client.ui.NfiUiRenderer;
import mycraft.yuyears.neofavoriteitems.client.ui.binding.NfiValueBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class NfiSlider extends AbstractSliderButton {
    private final double minimum;
    private final double maximum;
    private final NfiValueBinding<Double> binding;
    private final Function<Double, Component> messageFactory;

    public NfiSlider(
        int x,
        int y,
        int width,
        int height,
        double minimum,
        double maximum,
        NfiValueBinding<Double> binding,
        Function<Double, Component> messageFactory
    ) {
        super(x, y, width, height, Component.empty(), normalize(binding.get(), minimum, maximum));
        this.minimum = minimum;
        this.maximum = maximum;
        this.binding = binding;
        this.messageFactory = messageFactory;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        if (messageFactory != null) {
            setMessage(messageFactory.apply(currentValue()));
        }
    }

    @Override
    protected void applyValue() {
        binding.set(currentValue());
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        NfiUiRenderer.slider(graphics, getX(), getY(), getWidth(), getHeight(), value, isHoveredOrFocused(), active);
        var font = Minecraft.getInstance().font;
        graphics.enableScissor(getX() + 2, getY(), getX() + getWidth() - 2, getY() + getHeight());
        NfiUiRenderer.centeredText(graphics, font, getMessage(), getX() + getWidth() / 2,
            getY() + (getHeight() - font.lineHeight) / 2 + 1, NfiUiRenderer.controlTextColor(active));
        graphics.disableScissor();
    }

    public void syncFromBinding() {
        value = normalize(binding.get(), minimum, maximum);
        updateMessage();
    }

    private double currentValue() {
        return minimum + value * (maximum - minimum);
    }

    private static double normalize(double value, double minimum, double maximum) {
        if (maximum <= minimum) {
            throw new IllegalArgumentException("maximum must be greater than minimum");
        }
        return Math.clamp((value - minimum) / (maximum - minimum), 0.0D, 1.0D);
    }
}
