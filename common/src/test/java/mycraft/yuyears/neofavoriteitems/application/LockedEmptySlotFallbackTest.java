package mycraft.yuyears.neofavoriteitems.application;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LockedEmptySlotFallbackTest {
    @Test
    void prefersHotbarEmptyUnlockedSlotBeforeMainInventory() {
        Set<Integer> emptySlots = Set.of(2, 12);
        Set<Integer> lockedTargets = Set.of();

        assertEquals(2, LockedEmptySlotFallback.findEmptyUnlockedFallbackSlot(
            emptySlots::contains,
            lockedTargets::contains
        ));
    }

    @Test
    void skipsLockedHotbarSlotAndUsesNextUnlockedHotbarSlot() {
        Set<Integer> emptySlots = Set.of(1, 4, 10);
        Set<Integer> lockedTargets = Set.of(1);

        assertEquals(4, LockedEmptySlotFallback.findEmptyUnlockedFallbackSlot(
            emptySlots::contains,
            lockedTargets::contains
        ));
    }

    @Test
    void fallsBackToMainInventoryWhenHotbarHasNoUsableEmptySlot() {
        Set<Integer> emptySlots = Set.of(0, 14, 20);
        Set<Integer> lockedTargets = Set.of(0, 14);

        assertEquals(20, LockedEmptySlotFallback.findEmptyUnlockedFallbackSlot(
            emptySlots::contains,
            lockedTargets::contains
        ));
    }

    @Test
    void returnsMinusOneWhenNoEmptyUnlockedInventorySlotExists() {
        IntPredicate noEmptySlots = slot -> false;
        IntPredicate noLockedTargets = slot -> false;

        assertEquals(-1, LockedEmptySlotFallback.findEmptyUnlockedFallbackSlot(
            noEmptySlots,
            noLockedTargets
        ));
    }
}
