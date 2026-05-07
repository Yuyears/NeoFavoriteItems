package mycraft.yuyears.neofavoriteitems.application;

import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FavoriteLockRulesTest {
    @Test
    void autoUnlockPreventsCreatingNewEmptySlotLocks() {
        NeoFavoriteItemsConfig config = new NeoFavoriteItemsConfig();
        config.general.lockEmptySlots = true;
        config.general.autoUnlockEmptySlots = true;

        assertFalse(FavoriteLockRules.canKeepEmptySlotLocked(config));
        assertFalse(FavoriteLockRules.canToggleFavorite(false, false, config));
    }

    @Test
    void existingFavoriteCanStillBeToggledOffWhenEmptySlotLocksAreNotKept() {
        NeoFavoriteItemsConfig config = new NeoFavoriteItemsConfig();
        config.general.lockEmptySlots = true;
        config.general.autoUnlockEmptySlots = true;

        assertTrue(FavoriteLockRules.canToggleFavorite(true, false, config));
    }

    @Test
    void emptySlotCanBeLockedOnlyWhenEmptyLocksAreEnabledAndNotAutoUnlocked() {
        NeoFavoriteItemsConfig config = new NeoFavoriteItemsConfig();
        config.general.lockEmptySlots = true;
        config.general.autoUnlockEmptySlots = false;

        assertTrue(FavoriteLockRules.canKeepEmptySlotLocked(config));
        assertTrue(FavoriteLockRules.canToggleFavorite(false, false, config));
    }

    @Test
    void itemSlotsCanAlwaysBeToggledRegardlessOfEmptySlotPolicy() {
        NeoFavoriteItemsConfig config = new NeoFavoriteItemsConfig();
        config.general.lockEmptySlots = false;
        config.general.autoUnlockEmptySlots = true;

        assertTrue(FavoriteLockRules.canToggleFavorite(false, true, config));
    }

    @Test
    void emptySlotPermissionDependsOnlyOnEmptySlotConfig() {
        NeoFavoriteItemsConfig config = new NeoFavoriteItemsConfig();
        config.general.lockEmptySlots = false;
        config.general.autoUnlockEmptySlots = false;

        assertFalse(FavoriteLockRules.canKeepEmptySlotLocked(config));
        assertFalse(FavoriteLockRules.canToggleFavorite(false, false, config));
    }
}
