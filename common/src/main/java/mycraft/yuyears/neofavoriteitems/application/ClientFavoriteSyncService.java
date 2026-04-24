package mycraft.yuyears.neofavoriteitems.application;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public final class ClientFavoriteSyncService {
    private static final AtomicLong LAST_REVISION = new AtomicLong(-1L);

    private ClientFavoriteSyncService() {}

    public static synchronized void resetSession() {
        LAST_REVISION.set(-1L);
        DebugLogger.debug("Client reset favorite sync session");
    }

    public static synchronized boolean applyFullSync(long revision, Set<Integer> favoriteSlots) {
        long currentRevision = LAST_REVISION.get();
        if (isStale(revision, currentRevision)) {
            DebugLogger.debug("Client ignored stale full sync: revision={} lastRevision={}", revision, currentRevision);
            return false;
        }

        Set<LogicalSlotIndex> logicalSlots = favoriteSlots.stream()
            .map(SlotMappingService::fromPlayerInventoryIndex)
            .flatMap(OptionalSlots::stream)
            .collect(Collectors.toSet());
        FavoritesManager.getStateService().setFavoriteSlots(logicalSlots);
        LAST_REVISION.set(revision);
        DebugLogger.debug("Client applied favorite full sync: revision={} slots={}", revision, favoriteSlots);
        return true;
    }

    public static synchronized ApplyResult applyIncrementalSync(long revision, Set<Integer> addedSlots, Set<Integer> removedSlots) {
        long currentRevision = LAST_REVISION.get();
        if (isStale(revision, currentRevision)) {
            DebugLogger.debug("Client ignored stale incremental sync: revision={} lastRevision={}", revision, currentRevision);
            return ApplyResult.STALE;
        }
        if (revision != currentRevision + 1L) {
            DebugLogger.debug("Client detected favorite sync gap: revision={} expected={}", revision, currentRevision + 1L);
            return ApplyResult.GAP;
        }

        var favoriteStateService = FavoritesManager.getStateService();
        for (int slot : removedSlots) {
            SlotMappingService.fromPlayerInventoryIndex(slot)
                .ifPresent(logicalSlot -> favoriteStateService.setSlotFavorite(logicalSlot, false));
        }
        for (int slot : addedSlots) {
            SlotMappingService.fromPlayerInventoryIndex(slot)
                .ifPresent(logicalSlot -> favoriteStateService.setSlotFavorite(logicalSlot, true));
        }
        LAST_REVISION.set(revision);
        DebugLogger.debug("Client applied favorite incremental sync: revision={} added={} removed={}", revision, addedSlots, removedSlots);
        return ApplyResult.APPLIED;
    }

    static long lastRevisionForTesting() {
        return LAST_REVISION.get();
    }

    private static boolean isStale(long revision, long currentRevision) {
        return revision < currentRevision || revision < 0L;
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
