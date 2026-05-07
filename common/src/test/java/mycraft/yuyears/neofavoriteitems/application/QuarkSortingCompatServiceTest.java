package mycraft.yuyears.neofavoriteitems.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuarkSortingCompatServiceTest {
    @BeforeEach
    void setUp() {
        FavoritesManager.getStateService().clearPlayer();
        FavoritesManager.getStateService().clearFavorites();
    }

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

    @Test
    void quarkHotbarSwapMovesFavoriteStateWithItem() {
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(0), true);

        assertTrue(QuarkSortingCompatService.swapFavoriteState(0, 9));

        assertFalse(FavoritesManager.getInstance().isSlotFavorite(0));
        assertTrue(FavoritesManager.getInstance().isSlotFavorite(9));
    }

    @Test
    void quarkHotbarSwapCanMoveFavoriteStateBackToHotbar() {
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(18), true);

        assertTrue(QuarkSortingCompatService.swapFavoriteState(0, 18));

        assertTrue(FavoritesManager.getInstance().isSlotFavorite(0));
        assertFalse(FavoritesManager.getInstance().isSlotFavorite(18));
    }

    @Test
    void quarkHotbarSwapIgnoresUnchangedOrInvalidPairs() {
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(0), true);
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(9), true);

        assertFalse(QuarkSortingCompatService.swapFavoriteState(0, 9));
        assertFalse(QuarkSortingCompatService.swapFavoriteState(0, 99));
        assertFalse(QuarkSortingCompatService.swapFavoriteState(4, 4));
    }

    @Test
    void quarkHotbarSlotPairOnlyMatchesHotbarRows() {
        assertTrue(QuarkSortingCompatService.isQuarkHotbarSwapSlotPair(0, 9));
        assertTrue(QuarkSortingCompatService.isQuarkHotbarSwapSlotPair(8, 35));
        assertFalse(QuarkSortingCompatService.isQuarkHotbarSwapSlotPair(0, 10));
        assertFalse(QuarkSortingCompatService.isQuarkHotbarSwapSlotPair(9, 18));
        assertFalse(QuarkSortingCompatService.isQuarkHotbarSwapSlotPair(0, 36));
    }

    @Test
    void lockOperationFallbackDuplicateOnlyMatchesQuickMoveThenPickupOnSameSlot() {
        Object slot = new Object();

        assertTrue(QuarkSortingCompatService.isLockOperationFallbackDuplicate(slot, slot, net.minecraft.world.inventory.ClickType.QUICK_MOVE, net.minecraft.world.inventory.ClickType.PICKUP));
        assertFalse(QuarkSortingCompatService.isLockOperationFallbackDuplicate(slot, new Object(), net.minecraft.world.inventory.ClickType.QUICK_MOVE, net.minecraft.world.inventory.ClickType.PICKUP));
        assertFalse(QuarkSortingCompatService.isLockOperationFallbackDuplicate(slot, slot, net.minecraft.world.inventory.ClickType.PICKUP, net.minecraft.world.inventory.ClickType.PICKUP));
        assertFalse(QuarkSortingCompatService.isLockOperationFallbackDuplicate(slot, slot, net.minecraft.world.inventory.ClickType.QUICK_MOVE, net.minecraft.world.inventory.ClickType.QUICK_MOVE));
    }
}
