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

class InventorySortingCompatServiceTest {
    @BeforeEach
    void setUp() {
        FavoritesManager.getStateService().clearPlayer();
        FavoritesManager.getStateService().clearFavorites();
    }

    @Test
    void mergeKeepsExistingLocksAndAddsFavoriteSlotsInRange() {
        int[] merged = InventorySortingCompatService.mergeLockedSlots(Set.of(4, 9, 12, 36, 40), 9, 36, new int[] { 10, 12 });

        assertArrayEquals(new int[] { 9, 10, 12 }, merged);
    }

    @Test
    void mergeReturnsOriginalLocksWhenNoFavoritesExist() {
        int[] original = new int[] { 11 };

        assertSame(original, InventorySortingCompatService.mergeLockedSlots(Set.of(), 9, 36, original));
    }

    @Test
    void mergeReturnsNullWhenNoLocksApply() {
        assertNull(InventorySortingCompatService.mergeLockedSlots(Set.of(1, 40), 9, 36, null));
    }

    @Test
    void playerSortFavoriteSlotsReturnsEmptyArrayWhenNoLocksApply() {
        assertArrayEquals(new int[0], InventorySortingCompatService.favoriteSlotsForPlayerSort(null, 0, 36));
    }

    @Test
    void mergeSupportsMutableSortLockCollections() {
        int[] merged = InventorySortingCompatService.mergeLockedSlots(java.util.List.of(10, 11, 40), 9, 36, new int[] { 11, 12 });

        assertArrayEquals(new int[] { 10, 11, 12 }, merged);
    }

    @Test
    void quarkHotbarSwapMovesFavoriteStateWithItem() {
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(0), true);

        assertTrue(InventorySortingCompatService.swapFavoriteState(0, 9));

        assertFalse(FavoritesManager.getInstance().isSlotFavorite(0));
        assertTrue(FavoritesManager.getInstance().isSlotFavorite(9));
    }

    @Test
    void quarkHotbarSwapCanMoveFavoriteStateBackToHotbar() {
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(18), true);

        assertTrue(InventorySortingCompatService.swapFavoriteState(0, 18));

        assertTrue(FavoritesManager.getInstance().isSlotFavorite(0));
        assertFalse(FavoritesManager.getInstance().isSlotFavorite(18));
    }

    @Test
    void quarkHotbarSwapIgnoresUnchangedOrInvalidPairs() {
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(0), true);
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(9), true);

        assertFalse(InventorySortingCompatService.swapFavoriteState(0, 9));
        assertFalse(InventorySortingCompatService.swapFavoriteState(0, 99));
        assertFalse(InventorySortingCompatService.swapFavoriteState(4, 4));
    }

    @Test
    void quarkHotbarSlotPairOnlyMatchesHotbarRows() {
        assertTrue(InventorySortingCompatService.isQuarkHotbarSwapSlotPair(0, 9));
        assertTrue(InventorySortingCompatService.isQuarkHotbarSwapSlotPair(8, 35));
        assertFalse(InventorySortingCompatService.isQuarkHotbarSwapSlotPair(0, 10));
        assertFalse(InventorySortingCompatService.isQuarkHotbarSwapSlotPair(9, 18));
        assertFalse(InventorySortingCompatService.isQuarkHotbarSwapSlotPair(0, 36));
    }

    @Test
    void lockOperationFallbackDuplicateOnlyMatchesQuickMoveThenPickupOnSameSlot() {
        Object slot = new Object();

        assertTrue(InventorySortingCompatService.isLockOperationFallbackDuplicate(slot, slot, net.minecraft.world.inventory.ClickType.QUICK_MOVE, net.minecraft.world.inventory.ClickType.PICKUP));
        assertFalse(InventorySortingCompatService.isLockOperationFallbackDuplicate(slot, new Object(), net.minecraft.world.inventory.ClickType.QUICK_MOVE, net.minecraft.world.inventory.ClickType.PICKUP));
        assertFalse(InventorySortingCompatService.isLockOperationFallbackDuplicate(slot, slot, net.minecraft.world.inventory.ClickType.PICKUP, net.minecraft.world.inventory.ClickType.PICKUP));
        assertFalse(InventorySortingCompatService.isLockOperationFallbackDuplicate(slot, slot, net.minecraft.world.inventory.ClickType.QUICK_MOVE, net.minecraft.world.inventory.ClickType.QUICK_MOVE));
    }
}
