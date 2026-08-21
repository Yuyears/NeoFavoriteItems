package mycraft.yuyears.neofavoriteitems.application;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstantSwapCompatServiceTest {
    @Test
    void unlockedPairPasses() {
        assertEquals(
            InstantSwapCompatService.Decision.PASS,
            InstantSwapCompatService.decide(false, false, false, false)
        );
    }

    @Test
    void modifierModeUsesMostRecentlyPressedModifierWhenBothHeld() {
        assertEquals(InstantSwapCompatService.ModifierMode.MOVE_LOCKS,
            InstantSwapCompatService.resolveModifierMode(true, true, InstantSwapCompatService.ModifierMode.MOVE_LOCKS));
        assertEquals(InstantSwapCompatService.ModifierMode.BYPASS,
            InstantSwapCompatService.resolveModifierMode(true, true, InstantSwapCompatService.ModifierMode.BYPASS));
        assertEquals(InstantSwapCompatService.ModifierMode.BYPASS,
            InstantSwapCompatService.resolveModifierMode(true, false, InstantSwapCompatService.ModifierMode.MOVE_LOCKS));
        assertEquals(InstantSwapCompatService.ModifierMode.MOVE_LOCKS,
            InstantSwapCompatService.resolveModifierMode(false, true, InstantSwapCompatService.ModifierMode.BYPASS));
    }

    @Test
    void modifierReleaseDebounceHasExactBoundary() {
        long releasedAt = 1_000L;
        long debounce = 20L;
        assertTrue(InstantSwapCompatService.isModifierActive(false, 1_020L, releasedAt, debounce));
        assertFalse(InstantSwapCompatService.isModifierActive(false, 1_021L, releasedAt, debounce));
        assertFalse(InstantSwapCompatService.isModifierActive(false, 1_000L, 0L, debounce));
        assertTrue(InstantSwapCompatService.isModifierActive(true, 2_000L, 0L, debounce));
    }

    @Test
    void eitherLockedEndpointBlocksWithoutModifier() {
        assertEquals(
            InstantSwapCompatService.Decision.BLOCK,
            InstantSwapCompatService.decide(true, false, false, false)
        );
        assertEquals(
            InstantSwapCompatService.Decision.BLOCK,
            InstantSwapCompatService.decide(false, true, false, false)
        );
    }

    @Test
    void bypassAllowsContentOnlySwap() {
        assertEquals(
            InstantSwapCompatService.Decision.PASS,
            InstantSwapCompatService.decide(true, false, true, false)
        );
    }

    @Test
    void lockKeySwapsLockStateAndTakesPriorityOverBypass() {
        assertEquals(
            InstantSwapCompatService.Decision.SWAP_WITH_LOCKS,
            InstantSwapCompatService.decide(true, false, true, true)
        );
    }

    @Test
    void onlyHotbarToMainInventoryPairsUseLockFollowCompat() {
        assertTrue(InstantSwapCompatService.isHotbarMainInventoryPair(4, 22));
        assertTrue(InstantSwapCompatService.isHotbarMainInventoryPair(22, 4));
        assertFalse(InstantSwapCompatService.isHotbarMainInventoryPair(2, 8));
        assertFalse(InstantSwapCompatService.isHotbarMainInventoryPair(10, 22));
        assertFalse(InstantSwapCompatService.isHotbarMainInventoryPair(-1, 4));
        assertFalse(InstantSwapCompatService.isHotbarMainInventoryPair(4, 36));
    }

    @Test
    void favoriteStateFollowsTwoAndThreeSlotMoves() {
        assertEquals(
            Set.of(4, 30),
            InstantSwapCompatService.moveFavoriteSlots(Set.of(22, 30), 22, 4)
        );
        assertEquals(
            Set.of(7),
            InstantSwapCompatService.moveFavoriteSlots(Set.of(4), 22, 4, 7)
        );
        assertEquals(
            Set.of(),
            InstantSwapCompatService.moveFavoriteSlots(Set.of(4), -1, 4)
        );
    }

    @Test
    void favoriteStateFollowsIndependentCreativeRowPairs() {
        assertEquals(
            Set.of(0, 2, 10),
            InstantSwapCompatService.moveFavoritePairs(
                Set.of(1, 9, 11),
                9, 0,
                10, 1,
                11, 2
            )
        );
        assertEquals(
            Set.of(1, 9, 11),
            InstantSwapCompatService.moveFavoritePairs(Set.of(1, 9, 11), 9, 0, 10)
        );
        assertTrue(InstantSwapCompatService.areHotbarMainInventoryPairs(9, 0, 10, 1, 11, 2));
        assertFalse(InstantSwapCompatService.areHotbarMainInventoryPairs(9, 0, 10));
        assertFalse(InstantSwapCompatService.areHotbarMainInventoryPairs(9, 0, 10, 11));
    }

    @Test
    void pickupExchangeKeepsSusClickOrder() {
        assertArrayEquals(new int[]{12, 40}, InstantSwapCompatService.pickupExchangeSlots(12, 40, true, false));
        assertArrayEquals(new int[]{40, 12}, InstantSwapCompatService.pickupExchangeSlots(12, 40, false, true));
        assertArrayEquals(new int[]{12, 40, 12}, InstantSwapCompatService.pickupExchangeSlots(12, 40, true, true));
        assertArrayEquals(new int[0], InstantSwapCompatService.pickupExchangeSlots(12, 40, false, false));
    }

    @Test
    void transactionRequiresFavoritePlayerEndpoint() {
        assertTrue(InstantSwapCompatService.containsFavoriteSlot(Set.of(4), -1, 4));
        assertTrue(InstantSwapCompatService.containsFavoriteSlot(Set.of(22), 22, 4, 7));
        assertFalse(InstantSwapCompatService.containsFavoriteSlot(Set.of(4), -1, 7));
        assertFalse(InstantSwapCompatService.containsFavoriteSlot(Set.of(4), -1));
    }

    @Test
    void lockMoveRequiresContentToFollowCycle() {
        assertTrue(InstantSwapCompatService.contentFollowsCycle(
            List.of("target", "hotbar"),
            List.of("hotbar", "target"),
            Objects::equals
        ));
        assertTrue(InstantSwapCompatService.contentFollowsCycle(
            List.of("target", "hotbar", "empty"),
            List.of("empty", "target", "hotbar"),
            Objects::equals
        ));
        assertFalse(InstantSwapCompatService.contentFollowsCycle(
            List.of("target", "hotbar"),
            List.of("target", "hotbar"),
            Objects::equals
        ));
    }
}
