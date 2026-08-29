package mycraft.yuyears.neofavoriteitems.render;

import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudSlotProbeTest {
    @Test
    void resolvesExactInventoryStackIdentityOnly() {
        Object empty = new Object();
        var inventory = new ArrayList<Object>(Collections.nCopies(36, empty));
        Object stack = new Object();
        inventory.set(22, stack);

        assertEquals(22, HudSlotProbe.resolveIdentity(inventory, stack, 36));
        assertEquals(-1, HudSlotProbe.resolveIdentity(inventory, new Object(), 36));
        inventory.set(3, stack);
        assertEquals(-1, HudSlotProbe.resolveIdentity(inventory, stack, 36));
    }

    @Test
    void clearsObservedSlotsAtFrameEnd() {
        var slot = LogicalSlotIndex.of(17);

        HudSlotProbe.clearObserved();
        HudSlotProbe.markObserved(slot);
        assertTrue(HudSlotProbe.wasObserved(slot));
        HudSlotProbe.markObserved(slot, 100, 200);
        assertTrue(HudSlotProbe.wasObserved(slot, 100, 200));
        assertFalse(HudSlotProbe.wasObserved(slot, 120, 200));

        HudSlotProbe.clearObserved();
        assertFalse(HudSlotProbe.wasObserved(slot));
    }
}
