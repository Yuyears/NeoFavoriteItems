package mycraft.yuyears.neofavoriteitems.application;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public final class ClientSortCompatService {
    private ClientSortCompatService() {}

    public static int[] filterCollectSlotIds(AbstractContainerMenu menu, ServerPlayer player, int[] slotIds) {
        if (slotIds == null || slotIds.length == 0) {
            return slotIds;
        }
        return filterCollectSlotIds(menu, slotIds);
    }

    public static int[] filterCollectSlotIds(AbstractContainerMenu menu, int[] slotIds) {
        return filterMoveSlotIds(menu, slotIds, "collect", "scope");
    }

    public static int[] filterTransferSourceSlotIds(AbstractContainerMenu menu, int[] slotIds) {
        return filterMoveSlotIds(menu, slotIds, "transfer", "source");
    }

    public static int[] filterTransferTargetSlotIds(AbstractContainerMenu menu, int[] slotIds) {
        return filterMoveSlotIds(menu, slotIds, "transfer", "target");
    }

    public static int[] filterStackFillSourceSlotIds(AbstractContainerMenu menu, int[] slotIds) {
        return filterMoveSlotIds(menu, slotIds, "stack_fill", "source");
    }

    public static int[] filterStackFillTargetSlotIds(AbstractContainerMenu menu, int[] slotIds) {
        return filterMoveSlotIds(menu, slotIds, "stack_fill", "target");
    }

    private static int[] filterMoveSlotIds(AbstractContainerMenu menu, int[] slotIds, String operation, String role) {
        if (slotIds == null || slotIds.length == 0) {
            return slotIds;
        }
        int[] filtered = filterCollectSlotIds(slotIds, slotId -> isProtectedPlayerMenuSlot(menu, slotId));
        if (filtered != slotIds) {
            DebugLogger.debug(
                "ClientSort {} filtered locked {} slots: before={} after={}",
                operation,
                role,
                Arrays.toString(slotIds),
                Arrays.toString(filtered)
            );
        }
        return filtered;
    }

    public static int[] stabilizeSortMapping(AbstractContainerMenu menu, ServerPlayer player, int[] slotMapping) {
        if (slotMapping == null || slotMapping.length < 2) {
            return slotMapping;
        }
        return stabilizeSortMapping(menu, slotMapping);
    }

    public static int[] stabilizeSortMapping(AbstractContainerMenu menu, int[] slotMapping) {
        if (slotMapping == null || slotMapping.length < 2) {
            return slotMapping;
        }
        int[] stabilized = stabilizeSortMapping(slotMapping, slotId -> isProtectedPlayerMenuSlot(menu, slotId));
        if (stabilized != slotMapping) {
            DebugLogger.debug(
                "ClientSort sort stabilized locked slots: before={} after={}",
                Arrays.toString(slotMapping),
                Arrays.toString(stabilized)
            );
        }
        return stabilized;
    }

    static int[] filterCollectSlotIds(int[] slotIds, IntPredicate protectedSlot) {
        int[] filtered = Arrays.stream(slotIds)
            .filter(slotId -> !protectedSlot.test(slotId))
            .toArray();
        return filtered.length == slotIds.length ? slotIds : filtered;
    }

    static int[] stabilizeSortMapping(int[] slotMapping, IntPredicate protectedSlot) {
        Set<Integer> protectedSlots = protectedSlots(slotMapping, protectedSlot);
        if (protectedSlots.isEmpty()) {
            return slotMapping;
        }

        int pairCount = slotMapping.length / 2;
        int[] unlockedSources = new int[pairCount];
        int[] unlockedDestinations = new int[pairCount];
        int unlockedSourceCount = 0;
        int unlockedDestinationCount = 0;
        for (int i = 0; i < slotMapping.length - 1; i += 2) {
            int source = slotMapping[i];
            int destination = slotMapping[i + 1];
            if (!protectedSlots.contains(source)) {
                unlockedSources[unlockedSourceCount++] = source;
            }
            if (!protectedSlots.contains(destination)) {
                unlockedDestinations[unlockedDestinationCount++] = destination;
            }
        }

        if (unlockedSourceCount != unlockedDestinationCount) {
            DebugLogger.debug(
                "ClientSort sort stabilization skipped: reason=unbalanced_unlocked_slots sources={} destinations={}",
                unlockedSourceCount,
                unlockedDestinationCount
            );
            return slotMapping;
        }

        int[] stabilized = slotMapping.clone();
        int unlockedIndex = 0;
        for (int i = 0; i < stabilized.length - 1; i += 2) {
            int source = stabilized[i];
            if (protectedSlots.contains(source)) {
                stabilized[i + 1] = source;
                continue;
            }
            stabilized[i] = unlockedSources[unlockedIndex];
            stabilized[i + 1] = unlockedDestinations[unlockedIndex];
            unlockedIndex++;
        }
        return stabilized;
    }

    private static Set<Integer> protectedSlots(int[] slotMapping, IntPredicate protectedSlot) {
        Set<Integer> protectedSlots = new HashSet<>();
        for (int i = 0; i < slotMapping.length - 1; i += 2) {
            int source = slotMapping[i];
            int destination = slotMapping[i + 1];
            if (protectedSlot.test(source)) {
                protectedSlots.add(source);
            }
            if (protectedSlot.test(destination)) {
                protectedSlots.add(destination);
            }
        }
        return protectedSlots;
    }

    private static boolean isProtectedPlayerMenuSlot(AbstractContainerMenu menu, int menuSlotId) {
        if (menu == null || menuSlotId < 0 || menuSlotId >= menu.slots.size()) {
            return false;
        }

        Slot slot = menu.slots.get(menuSlotId);
        if (!(slot.container instanceof Inventory inventory)) {
            return false;
        }
        return ServerFavoriteService.shouldProtectInventorySlotForExternalMove(inventory, slot.getContainerSlot());
    }
}
