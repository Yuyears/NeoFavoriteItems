package mycraft.yuyears.neofavoriteitems.client.ui.control;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import mycraft.yuyears.neofavoriteitems.client.ui.binding.NfiValueBinding;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NfiDropdownTest {
    @Test
    void keyboardSelectionAndCloseUpdateBinding() {
        AtomicReference<String> value = new AtomicReference<>("a");
        var binding = NfiValueBinding.unchecked(value::get, value::set, () -> "a", () -> {});
        var dropdown = new NfiDropdown<>(0, 0, 100, 2, binding, List.of("a", "b", "c"), Component::literal);

        dropdown.open();
        assertTrue(dropdown.keyPressed(264));
        assertEquals("b", value.get());
        assertTrue(dropdown.isOpen());
        assertTrue(dropdown.keyPressed(257));
        assertFalse(dropdown.isOpen());
    }

    @Test
    void popupUsesInsetRowsAndOpensAboveNearBottomEdge() {
        AtomicReference<String> value = new AtomicReference<>("a");
        var binding = NfiValueBinding.unchecked(value::get, value::set, () -> "a", () -> {});
        var dropdown = new NfiDropdown<>(10, 80, 100, 2, binding, List.of("a", "b"), Component::literal);

        dropdown.layoutPopup(100);

        var widgets = dropdown.group().widgets();
        assertEquals(14, widgets.get(1).getX());
        assertEquals(92, widgets.get(1).getWidth());
        assertTrue(widgets.get(1).getY() < dropdown.button().getY());
        assertTrue(widgets.get(1).getY() < widgets.get(2).getY());
    }

    @Test
    void popupShrinksRowsToFitScaledScreen() {
        AtomicReference<String> value = new AtomicReference<>("a");
        var binding = NfiValueBinding.unchecked(value::get, value::set, () -> "a", () -> {});
        var dropdown = new NfiDropdown<>(10, 50, 100, 10, binding,
            List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"), Component::literal);

        dropdown.open();
        dropdown.layoutPopup(80);

        assertEquals(2, dropdown.visibleRowCount());
        assertTrue(dropdown.group().widgets().get(1).getY() >= 0);
        assertTrue(dropdown.group().widgets().get(2).getY() + 20 <= 80);
    }

    @Test
    void scrollbarTrackMovesVisibleWindow() {
        AtomicReference<String> value = new AtomicReference<>("a");
        var binding = NfiValueBinding.unchecked(value::get, value::set, () -> "a", () -> {});
        var dropdown = new NfiDropdown<>(10, 10, 100, 2, binding, List.of("a", "b", "c", "d"), Component::literal);
        dropdown.layoutPopup(200);
        dropdown.open();

        assertTrue(dropdown.mouseClicked(108, 69, 0));
        assertEquals(2, dropdown.scrollPosition());
    }

    @Test
    void openDropdownConsumesOutsideClickBeforeLowerControls() {
        AtomicReference<String> value = new AtomicReference<>("a");
        var binding = NfiValueBinding.unchecked(value::get, value::set, () -> "a", () -> {});
        var dropdown = new NfiDropdown<>(10, 10, 100, 2, binding, List.of("a", "b"), Component::literal);

        dropdown.open();

        assertTrue(dropdown.mouseClicked(0, 0, 0));
        assertFalse(dropdown.isOpen());
    }

    @Test
    void clickOnPopupPaddingIsConsumedWithoutClosing() {
        AtomicReference<String> value = new AtomicReference<>("a");
        var binding = NfiValueBinding.unchecked(value::get, value::set, () -> "a", () -> {});
        var dropdown = new NfiDropdown<>(10, 10, 100, 2, binding, List.of("a", "b"), Component::literal);
        dropdown.open();
        dropdown.layoutPopup(200);

        assertTrue(dropdown.mouseClicked(11, 35, 0));
        assertTrue(dropdown.isOpen());
        assertEquals("a", value.get());
    }
}
