package mycraft.yuyears.neofavoriteitems.application;

import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;

public final class FavoriteLockRules {
    private FavoriteLockRules() {}

    public static boolean canKeepEmptySlotLocked(NeoFavoriteItemsConfig config) {
        return config.general.lockEmptySlots && !config.general.autoUnlockEmptySlots;
    }

    public static boolean canToggleFavorite(boolean isFavorite, boolean hasItem, NeoFavoriteItemsConfig config) {
        return isFavorite || hasItem || canKeepEmptySlotLocked(config);
    }
}
