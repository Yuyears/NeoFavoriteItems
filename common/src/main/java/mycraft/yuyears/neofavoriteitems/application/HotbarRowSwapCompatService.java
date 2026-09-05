package mycraft.yuyears.neofavoriteitems.application;

import mycraft.yuyears.neofavoriteitems.FavoritesManager;

import java.util.Optional;
import java.util.Set;

public final class HotbarRowSwapCompatService {
    public static final int ALL_COLUMNS_MASK = 0x1FF;

    private HotbarRowSwapCompatService() {}

    public static Optional<RowSwapPlan> createPlan(int rowIndex, int columnMask) {
        if (rowIndex < 1 || rowIndex > 3 || columnMask <= 0 || (columnMask & ~ALL_COLUMNS_MASK) != 0) {
            return Optional.empty();
        }

        int[] slotPairs = new int[Integer.bitCount(columnMask) * 2];
        int pairIndex = 0;
        for (int column = 0; column < 9; column++) {
            if ((columnMask & (1 << column)) == 0) {
                continue;
            }
            slotPairs[pairIndex++] = column;
            slotPairs[pairIndex++] = rowIndex * 9 + column;
        }
        return Optional.of(new RowSwapPlan(rowIndex, columnMask, slotPairs));
    }

    public static boolean moveClientFavoriteState(int rowIndex, int columnMask) {
        RowSwapPlan plan = createPlan(rowIndex, columnMask).orElse(null);
        if (plan == null) {
            return false;
        }

        FavoritesManager.getStateService().useClientState();
        Set<Integer> current = FavoritesManager.getStateService().getFavoriteSlots();
        Set<Integer> moved = FavoriteSlotTransferHelper.movePairs(current, plan.slotPairs());
        if (current.equals(moved)) {
            return false;
        }

        FavoritesManager favoritesManager = FavoritesManager.getInstance();
        for (int inventoryIndex : plan.slotPairs()) {
            favoritesManager.setSlotFavorite(inventoryIndex, moved.contains(inventoryIndex));
        }
        return true;
    }

    public record RowSwapPlan(int rowIndex, int columnMask, int[] slotPairs) {
        public RowSwapPlan {
            slotPairs = slotPairs.clone();
        }

        @Override
        public int[] slotPairs() {
            return slotPairs.clone();
        }
    }
}
