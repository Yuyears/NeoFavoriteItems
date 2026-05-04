package mycraft.yuyears.neofavoriteitems.application;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;

public final class QuarkSortingCompatService {
    private QuarkSortingCompatService() {}

    public static int[] augmentLockedSlots(Container container, int startInclusive, int endExclusive, int[] lockedSlots) {
        if (!(container instanceof Inventory inventory) || inventory.player == null) {
            return lockedSlots;
        }

        FavoritesManager.getStateService().setPlayer(inventory.player.getUUID());
        return mergeLockedSlots(
            FavoritesManager.getStateService().getFavoriteSlots(),
            startInclusive,
            endExclusive,
            lockedSlots
        );
    }

    static int[] mergeLockedSlots(Set<Integer> favoriteSlots, int startInclusive, int endExclusive, int[] lockedSlots) {
        if (favoriteSlots == null || favoriteSlots.isEmpty()) {
            return lockedSlots;
        }

        TreeSet<Integer> merged = new TreeSet<>();
        if (lockedSlots != null) {
            Arrays.stream(lockedSlots).forEach(merged::add);
        }

        for (int slot : favoriteSlots) {
            if (slot >= startInclusive && slot < endExclusive && SlotMappingService.isPlayerInventoryIndex(slot)) {
                merged.add(slot);
            }
        }

        if (lockedSlots == null && merged.isEmpty()) {
            return null;
        }
        return merged.stream().mapToInt(Integer::intValue).toArray();
    }
}
