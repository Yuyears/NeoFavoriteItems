package mycraft.yuyears.neofavoriteitems.application;

import net.minecraft.world.inventory.ClickType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstantSwapOperationPlanTest {
    @Test
    void swapPlanUsesOneVanillaSwapClick() {
        var plan = InstantSwapCompatService.createPlan(
            InstantSwapCompatService.Operation.SWAP,
            12, 22, 40, 4, -1, -1, true, true
        ).orElseThrow();

        assertEquals(
            List.of(new InstantSwapCompatService.PlannedClick(12, 4, ClickType.SWAP)),
            plan.clicks()
        );
        assertEquals(List.of(22, 4), plan.favoriteCycle());
        assertEquals(List.of(12, 40), plan.menuSlotCycle());
    }

    @Test
    void pickupExchangePlanPreservesSusClickOrder() {
        var plan = InstantSwapCompatService.createPlan(
            InstantSwapCompatService.Operation.PICKUP_EXCHANGE,
            12, -1, 40, 4, -1, -1, true, true
        ).orElseThrow();

        assertEquals(
            List.of(
                new InstantSwapCompatService.PlannedClick(12, 0, ClickType.PICKUP),
                new InstantSwapCompatService.PlannedClick(40, 0, ClickType.PICKUP),
                new InstantSwapCompatService.PlannedClick(12, 0, ClickType.PICKUP)
            ),
            plan.clicks()
        );
        assertEquals(List.of(-1, 4), plan.favoriteCycle());
        assertEquals(List.of(12, 40), plan.menuSlotCycle());
    }

    @Test
    void hotbarStashPlanPreservesThreeSlotCycle() {
        var plan = InstantSwapCompatService.createPlan(
            InstantSwapCompatService.Operation.HOTBAR_STASH,
            12, 22, 40, 4, 43, 7, true, true
        ).orElseThrow();

        assertEquals(
            List.of(
                new InstantSwapCompatService.PlannedClick(40, 0, ClickType.PICKUP),
                new InstantSwapCompatService.PlannedClick(43, 0, ClickType.PICKUP),
                new InstantSwapCompatService.PlannedClick(12, 0, ClickType.PICKUP),
                new InstantSwapCompatService.PlannedClick(40, 0, ClickType.PICKUP)
            ),
            plan.clicks()
        );
        assertEquals(List.of(22, 4, 7), plan.favoriteCycle());
        assertEquals(List.of(12, 40, 43), plan.menuSlotCycle());
    }

    @Test
    void invalidOperationShapeHasNoPlan() {
        assertTrue(InstantSwapCompatService.createPlan(
            InstantSwapCompatService.Operation.PICKUP_EXCHANGE,
            12, 22, 40, 4, -1, -1, false, false
        ).isEmpty());
        assertTrue(InstantSwapCompatService.createPlan(
            InstantSwapCompatService.Operation.HOTBAR_STASH,
            12, 22, 40, 4, -1, -1, true, true
        ).isEmpty());
    }
}
