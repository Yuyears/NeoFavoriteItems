package mycraft.yuyears.neofavoriteitems.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;
import org.junit.jupiter.api.Test;

class QuarkSortingCompatServiceTest {
    @Test
    void mergeKeepsExistingLocksAndAddsFavoriteSlotsInRange() {
        int[] merged = QuarkSortingCompatService.mergeLockedSlots(Set.of(4, 9, 12, 36, 40), 9, 36, new int[] { 10, 12 });

        assertArrayEquals(new int[] { 9, 10, 12 }, merged);
    }

    @Test
    void mergeReturnsOriginalLocksWhenNoFavoritesExist() {
        int[] original = new int[] { 11 };

        assertSame(original, QuarkSortingCompatService.mergeLockedSlots(Set.of(), 9, 36, original));
    }

    @Test
    void mergeReturnsNullWhenNoLocksApply() {
        assertNull(QuarkSortingCompatService.mergeLockedSlots(Set.of(1, 40), 9, 36, null));
    }
}
