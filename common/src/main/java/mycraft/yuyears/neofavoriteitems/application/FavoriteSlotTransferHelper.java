package mycraft.yuyears.neofavoriteitems.application;

import java.util.HashSet;
import java.util.Set;

public final class FavoriteSlotTransferHelper {
    private FavoriteSlotTransferHelper() {}

    public static Set<Integer> moveCycle(Set<Integer> currentFavorites, int... slotCycle) {
        Set<Integer> result = new HashSet<>(currentFavorites);
        if (slotCycle == null || slotCycle.length < 2) {
            return result;
        }

        for (int slot : slotCycle) {
            if (slot >= 0) {
                result.remove(slot);
            }
        }
        for (int i = 0; i < slotCycle.length; i++) {
            int source = slotCycle[i];
            int target = slotCycle[(i + 1) % slotCycle.length];
            if (source >= 0 && target >= 0 && currentFavorites.contains(source)) {
                result.add(target);
            }
        }
        return result;
    }

    public static Set<Integer> movePairs(Set<Integer> currentFavorites, int... slotPairs) {
        Set<Integer> result = new HashSet<>(currentFavorites);
        if (slotPairs == null || slotPairs.length % 2 != 0) {
            return result;
        }

        for (int i = 0; i < slotPairs.length; i += 2) {
            int first = slotPairs[i];
            int second = slotPairs[i + 1];
            if (first < 0 || second < 0 || first == second) {
                continue;
            }
            boolean firstFavorite = currentFavorites.contains(first);
            boolean secondFavorite = currentFavorites.contains(second);
            if (firstFavorite != secondFavorite) {
                result.remove(firstFavorite ? first : second);
                result.add(firstFavorite ? second : first);
            }
        }
        return result;
    }
}
