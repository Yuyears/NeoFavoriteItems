package mycraft.yuyears.neofavoriteitems.application;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;

import java.util.Set;
import java.util.stream.Collectors;

public final class ClientFavoriteSyncService {
    private static long lastRevision = -1L;

    private ClientFavoriteSyncService() {}

    public static void resetSession() {
        lastRevision = -1L;
        DebugLogger.debug("Client reset favorite sync session");
    }

    public static boolean applyFullSync(long revision, Set<Integer> favoriteSlots) {
        if (isStale(revision)) {
            DebugLogger.debug("Client ignored stale full sync: revision={} lastRevision={}", revision, lastRevision);
            return false;
        }

        Set<LogicalSlotIndex> logicalSlots = favoriteSlots.stream()
            .map(SlotMappingService::fromPlayerInventoryIndex)
            .flatMap(OptionalSlots::stream)
            .collect(Collectors.toSet());
        FavoritesManager.getInstance().setFavoriteSlots(logicalSlots);
        lastRevision = revision;
        DebugLogger.debug("Client applied favorite full sync: revision={} slots={}", revision, favoriteSlots);
        return true;
    }

    public static ApplyResult applyIncrementalSync(long revision, Set<Integer> addedSlots, Set<Integer> removedSlots) {
        if (isStale(revision)) {
            DebugLogger.debug("Client ignored stale incremental sync: revision={} lastRevision={}", revision, lastRevision);
            return ApplyResult.STALE;
        }
        if (revision != lastRevision + 1L) {
            DebugLogger.debug("Client detected favorite sync gap: revision={} expected={}", revision, lastRevision + 1L);
            return ApplyResult.GAP;
        }

        FavoritesManager favoritesManager = FavoritesManager.getInstance();
        for (int slot : removedSlots) {
            SlotMappingService.fromPlayerInventoryIndex(slot)
                .ifPresent(logicalSlot -> favoritesManager.setSlotFavorite(logicalSlot, false));
        }
        for (int slot : addedSlots) {
            SlotMappingService.fromPlayerInventoryIndex(slot)
                .ifPresent(logicalSlot -> favoritesManager.setSlotFavorite(logicalSlot, true));
        }
        lastRevision = revision;
        DebugLogger.debug("Client applied favorite incremental sync: revision={} added={} removed={}", revision, addedSlots, removedSlots);
        return ApplyResult.APPLIED;
    }

    private static boolean isStale(long revision) {
        return revision < lastRevision || revision < 0L;
    }

    public enum ApplyResult {
        APPLIED,
        STALE,
        GAP
    }

    private static final class OptionalSlots {
        private OptionalSlots() {}

        private static java.util.stream.Stream<LogicalSlotIndex> stream(java.util.Optional<LogicalSlotIndex> optional) {
            return optional.map(java.util.stream.Stream::of).orElseGet(java.util.stream.Stream::empty);
        }
    }
}
