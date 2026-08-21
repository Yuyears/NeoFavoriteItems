package mycraft.yuyears.neofavoriteitems.application;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;

public final class InventorySortingCompatService {
    private InventorySortingCompatService() {}

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

    public static int[] favoriteSlotsForPlayerSort(Inventory inventory, int startInclusive, int endExclusive) {
        if (inventory == null || inventory.player == null) {
            return new int[0];
        }

        FavoritesManager.getStateService().setPlayer(inventory.player.getUUID());
        int[] favoriteLocks = mergeLockedSlots(
            FavoritesManager.getStateService().getFavoriteSlots(),
            startInclusive,
            endExclusive,
            null
        );
        return favoriteLocks == null ? new int[0] : favoriteLocks;
    }

    public static boolean applyHotbarSwapFavoriteState(Inventory inventory, int slot1, int slot2) {
        if (inventory == null || inventory.player == null) {
            return false;
        }
        if (!isQuarkHotbarSwapSlotPair(slot1, slot2)) {
            return false;
        }

        FavoritesManager.getStateService().setPlayer(inventory.player.getUUID());
        if (!swapFavoriteState(slot1, slot2)) {
            return false;
        }

        if (inventory.player instanceof ServerPlayer serverPlayer) {
            ServerFavoriteService.markFavoriteStateChanged(serverPlayer, "quark_hotbar_swap");
        }
        return true;
    }

    public static boolean swapFavoriteState(int slot1, int slot2) {
        if (!SlotMappingService.isPlayerInventoryIndex(slot1)
            || !SlotMappingService.isPlayerInventoryIndex(slot2)
            || slot1 == slot2) {
            return false;
        }

        FavoritesManager favoritesManager = FavoritesManager.getInstance();
        boolean slot1Favorite = favoritesManager.isSlotFavorite(slot1);
        boolean slot2Favorite = favoritesManager.isSlotFavorite(slot2);
        if (slot1Favorite == slot2Favorite) {
            return false;
        }

        favoritesManager.setSlotFavorite(slot1, slot2Favorite);
        favoritesManager.setSlotFavorite(slot2, slot1Favorite);
        return true;
    }

    static boolean isQuarkHotbarSwapSlotPair(int slot1, int slot2) {
        return slot1 >= 0
            && slot1 < 9
            && slot2 >= 9
            && slot2 < 36
            && (slot2 - slot1) % 9 == 0;
    }

    public static boolean isLockOperationFallbackDuplicate(Object currentSlotIdentity, Object previousSlotIdentity, ClickType previousClickType, ClickType currentClickType) {
        return currentSlotIdentity != null
            && currentSlotIdentity == previousSlotIdentity
            && previousClickType == ClickType.QUICK_MOVE
            && currentClickType == ClickType.PICKUP;
    }

    static int[] mergeLockedSlots(Collection<Integer> favoriteSlots, int startInclusive, int endExclusive, int[] lockedSlots) {
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
