package mycraft.yuyears.neofavoriteitems;

import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;

import java.util.Set;
import java.util.UUID;

public interface FavoriteStateService {
    void setPlayer(UUID playerUUID);

    void clearPlayer();

    void removePlayer(UUID playerUUID);

    boolean isSlotFavorite(LogicalSlotIndex slot);

    void setSlotFavorite(LogicalSlotIndex slot, boolean favorite);

    Set<Integer> getFavoriteSlots();

    Set<LogicalSlotIndex> getFavoriteLogicalSlots();

    void clearFavorites();

    void setFavoriteSlots(Set<LogicalSlotIndex> slots);
}
