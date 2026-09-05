package mycraft.yuyears.neofavoriteitems.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotbarRowSwapCompatServiceTest {
    @Test
    void createsPairsOnlyForSelectedColumns() {
        var plan = HotbarRowSwapCompatService.createPlan(2, (1 << 0) | (1 << 4) | (1 << 8)).orElseThrow();
        assertArrayEquals(new int[]{0, 18, 4, 22, 8, 26}, plan.slotPairs());
    }

    @Test
    void rejectsInvalidRowsAndMasks() {
        assertTrue(HotbarRowSwapCompatService.createPlan(0, 1).isEmpty());
        assertTrue(HotbarRowSwapCompatService.createPlan(4, 1).isEmpty());
        assertTrue(HotbarRowSwapCompatService.createPlan(1, 0).isEmpty());
        assertTrue(HotbarRowSwapCompatService.createPlan(1, 1 << 9).isEmpty());
    }
}
