package mycraft.yuyears.neofavoriteitems.application;

import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionGuardServiceTest {
    @BeforeEach
    void setUp() {
        FavoritesManager.getStateService().clearPlayer();
        FavoritesManager.getStateService().clearFavorites();

        NeoFavoriteItemsConfig config = ConfigManager.getInstance().getConfig();
        config.general.lockEmptySlots = true;
        config.general.autoUnlockEmptySlots = false;
        config.general.allowItemsIntoLockedEmptySlots = true;
        config.lockBehavior.preventClick = true;
        config.lockBehavior.preventDrop = true;
        config.lockBehavior.preventQuickMove = true;
        config.lockBehavior.preventShiftClick = true;
        config.lockBehavior.preventDrag = true;
        config.lockBehavior.preventSwap = true;
        config.lockBehavior.allowBypassWithKey = true;
    }

    @Test
    void lockedEmptyOffhandRejectsIncomingSwapItem() {
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(40), true);

        assertTrue(InteractionGuardService.getInstance()
            .evaluateIncomingItem(40, InteractionType.SWAP, false, true)
            .denied());
    }

    @Test
    void bypassAllowsIncomingItemIntoLockedTarget() {
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(40), true);

        assertFalse(InteractionGuardService.getInstance()
            .evaluateIncomingItem(40, InteractionType.SWAP, true, true)
            .denied());
    }

    @Test
    void lockedEmptyArmorRejectsQuickMoveEquipTarget() {
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(39), true);

        assertTrue(InteractionGuardService.getInstance()
            .evaluateIncomingItem(39, InteractionType.QUICK_MOVE, false, true)
            .denied());
    }

    @Test
    void lockedSourceRejectsQuickMoveRemoval() {
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(9), true);

        assertTrue(InteractionGuardService.getInstance()
            .evaluate(9, InteractionType.QUICK_MOVE, false, true)
            .denied());
    }

    @Test
    void lockedSourceQuickMoveAllowsBypass() {
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(9), true);

        assertFalse(InteractionGuardService.getInstance()
            .evaluate(9, InteractionType.QUICK_MOVE, true, true)
            .denied());
    }

    @Test
    void incomingCheckIgnoresUnlockedTargetAndEmptyIncomingStack() {
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(38), true);

        assertFalse(InteractionGuardService.getInstance()
            .evaluateIncomingItem(37, InteractionType.QUICK_MOVE, false, true)
            .denied());
        assertFalse(InteractionGuardService.getInstance()
            .evaluateIncomingItem(38, InteractionType.QUICK_MOVE, false, false)
            .denied());
    }
}
