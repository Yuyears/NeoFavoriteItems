package mycraft.yuyears.neofavoriteitems.client.ui.control;

import java.util.concurrent.atomic.AtomicInteger;
import mycraft.yuyears.neofavoriteitems.client.ui.binding.NfiValueBinding;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NfiColorPickerTest {
    @Test
    void popupOpensAboveNearBottomEdge() {
        AtomicInteger color = new AtomicInteger(0xFFFFFFFF);
        var binding = NfiValueBinding.unchecked(color::get, color::set, () -> 0xFFFFFFFF, () -> {});
        var picker = new NfiColorPicker(10, 80, 100, binding);

        picker.layoutPopup(100);

        picker.popupWidgets().forEach(widget -> assertTrue(widget.getY() < picker.button().getY()));
    }

    @Test
    void popupStaysInsideShortScreen() {
        AtomicInteger color = new AtomicInteger(0xFFFFFFFF);
        var binding = NfiValueBinding.unchecked(color::get, color::set, () -> 0xFFFFFFFF, () -> {});
        var picker = new NfiColorPicker(10, 30, 100, binding);

        picker.layoutPopup(80);

        picker.popupWidgets().forEach(widget -> {
            assertTrue(widget.getY() >= 0);
            assertTrue(widget.getY() + widget.getHeight() <= 80);
        });
    }
}
