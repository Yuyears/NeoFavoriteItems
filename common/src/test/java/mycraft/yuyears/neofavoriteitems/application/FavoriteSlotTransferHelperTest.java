package mycraft.yuyears.neofavoriteitems.application;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FavoriteSlotTransferHelperTest {
    @Test
    void movesIndependentPairsWithoutTouchingOtherSlots() {
        assertEquals(
            Set.of(0, 2, 30),
            FavoriteSlotTransferHelper.movePairs(Set.of(9, 2, 30), 0, 9, 1, 10)
        );
    }

    @Test
    void rejectsIncompletePairInputWithoutChangingState() {
        assertEquals(Set.of(1, 9), FavoriteSlotTransferHelper.movePairs(Set.of(1, 9), 0, 9, 1));
    }
}
