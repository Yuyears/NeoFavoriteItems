package mycraft.yuyears.neofavoriteitems.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClientSortCompatServiceTest {
    @Test
    void collectFiltersProtectedSlots() {
        int[] original = new int[] { 0, 1, 2, 3, 4 };

        int[] filtered = ClientSortCompatService.filterCollectSlotIds(original, Set.of(1, 3)::contains);

        assertArrayEquals(new int[] { 0, 2, 4 }, filtered);
    }

    @Test
    void collectReturnsOriginalArrayWhenNoSlotsAreProtected() {
        int[] original = new int[] { 0, 1, 2 };

        int[] filtered = ClientSortCompatService.filterCollectSlotIds(original, slot -> false);

        assertSame(original, filtered);
    }

    @Test
    void sortMakesTwoSlotSwapNoOpWhenEitherSlotIsProtected() {
        int[] original = new int[] { 4, 10, 10, 4 };

        int[] stabilized = ClientSortCompatService.stabilizeSortMapping(original, slot -> slot == 4);

        assertArrayEquals(new int[] { 4, 4, 10, 10 }, stabilized);
    }

    @Test
    void sortKeepsUnlockedPartOfCycleMovingWhenProtectedSlotIsFixed() {
        int[] original = new int[] { 1, 2, 2, 3, 3, 1 };

        int[] stabilized = ClientSortCompatService.stabilizeSortMapping(original, slot -> slot == 2);

        assertArrayEquals(new int[] { 1, 3, 2, 2, 3, 1 }, stabilized);
        assertTrue(isValidSlotMapping(stabilized));
    }

    @Test
    void sortOnlyStabilizesCycleContainingProtectedSlot() {
        int[] original = new int[] { 1, 2, 2, 1, 8, 9, 9, 8 };

        int[] stabilized = ClientSortCompatService.stabilizeSortMapping(original, slot -> slot == 1);

        assertArrayEquals(new int[] { 1, 1, 2, 2, 8, 9, 9, 8 }, stabilized);
        assertTrue(isValidSlotMapping(stabilized));
    }

    @Test
    void sortRemovesMultipleProtectedSlotsFromLargeCycleWithoutFreezingUnlockedSlots() {
        int[] original = new int[] { 31, 9, 21, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17, 17, 18, 18, 19, 19, 20, 20, 22, 21, 23, 22, 24, 23, 25, 24, 26, 25, 27, 26, 28, 27, 29, 28, 30, 29, 32, 30, 33, 31, 34, 32, 35, 33, 9, 34, 10, 35 };

        int[] stabilized = ClientSortCompatService.stabilizeSortMapping(original, Set.of(14, 23, 25)::contains);

        assertArrayEquals(
            new int[] { 31, 9, 21, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17, 17, 18, 18, 19, 19, 20, 20, 22, 21, 23, 23, 24, 22, 25, 25, 26, 24, 27, 26, 28, 27, 29, 28, 30, 29, 32, 30, 33, 31, 34, 32, 35, 33, 9, 34, 10, 35 },
            stabilized
        );
        assertTrue(isValidSlotMapping(stabilized));
    }

    @Test
    void sortUsesClientSortTargetOrderWhenProtectedSlotWouldWrapCycle() {
        int[] original = new int[] { 4, 1, 1, 2, 2, 3, 3, 4 };

        int[] stabilized = ClientSortCompatService.stabilizeSortMapping(original, slot -> slot == 4);

        assertArrayEquals(new int[] { 4, 4, 1, 1, 2, 2, 3, 3 }, stabilized);
        assertTrue(isValidSlotMapping(stabilized));
    }

    @Test
    void sortReturnsOriginalArrayWhenNoSlotsAreProtected() {
        int[] original = new int[] { 1, 2, 2, 3, 3, 1 };

        int[] stabilized = ClientSortCompatService.stabilizeSortMapping(original, slot -> false);

        assertSame(original, stabilized);
    }

    private static boolean isValidSlotMapping(int[] slotMapping) {
        Set<Integer> sources = new HashSet<>();
        Set<Integer> destinations = new HashSet<>();
        for (int i = 0; i < slotMapping.length - 1; i += 2) {
            if (!sources.add(slotMapping[i]) || !destinations.add(slotMapping[i + 1])) {
                return false;
            }
        }
        return sources.equals(destinations);
    }
}
