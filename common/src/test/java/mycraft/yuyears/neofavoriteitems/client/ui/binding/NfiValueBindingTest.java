package mycraft.yuyears.neofavoriteitems.client.ui.binding;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NfiValueBindingTest {
    @Test
    void acceptsChangesRejectsInvalidValuesAndResets() {
        AtomicReference<Integer> value = new AtomicReference<>(5);
        AtomicInteger changes = new AtomicInteger();
        var binding = new NfiValueBinding<>(
            value::get,
            value::set,
            () -> 5,
            candidate -> candidate >= 0
                ? NfiValidationResult.ok()
                : NfiValidationResult.error(Component.literal("negative")),
            changes::incrementAndGet
        );

        assertTrue(binding.isDefault());
        assertTrue(binding.set(7).valid());
        assertEquals(7, value.get());
        assertEquals(1, changes.get());

        assertFalse(binding.set(-1).valid());
        assertEquals(7, value.get());
        assertEquals(1, changes.get());

        binding.set(7);
        assertEquals(1, changes.get());
        binding.reset();
        assertEquals(5, value.get());
        assertEquals(2, changes.get());
    }
}
