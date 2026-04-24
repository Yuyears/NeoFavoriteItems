package mycraft.yuyears.neofavoriteitems.application;

import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientDropGuardTest {
    @BeforeEach
    void setUp() {
        FavoritesManager.getStateService().clearPlayer();
        FavoritesManager.getStateService().clearFavorites();
        ConfigManager.getInstance().getConfig().lockBehavior.preventDrop = true;
        ConfigManager.getInstance().getConfig().lockBehavior.allowBypassWithKey = true;
        ConfigManager.getInstance().getConfig().general.autoUnlockEmptySlots = false;
    }

    @Test
    void blocksDropForLockedHotbarSlot() {
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(2), true);
        assertTrue(ClientDropGuard.shouldBlockSelectedHotbarDrop(2, true, false));
    }

    @Test
    void allowsDropWhenBypassKeyIsHeld() {
        FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(2), true);
        assertFalse(ClientDropGuard.shouldBlockSelectedHotbarDrop(2, true, true));
    }
}
