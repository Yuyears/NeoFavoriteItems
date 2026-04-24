package mycraft.yuyears.neofavoriteitems.application;

import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientFavoriteSyncServiceTest {
    @BeforeEach
    void setUp() {
        FavoritesManager.getStateService().clearPlayer();
        FavoritesManager.getStateService().clearFavorites();
        ClientFavoriteSyncService.resetSession();
    }

    @Test
    void detectsRevisionGaps() {
        assertTrue(ClientFavoriteSyncService.applyFullSync(5L, Set.of(0)));
        assertEquals(
            ClientFavoriteSyncService.ApplyResult.GAP,
            ClientFavoriteSyncService.applyIncrementalSync(7L, Set.of(1), Set.of())
        );
    }

    @Test
    void concurrentFullSyncKeepsHighestRevision() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        for (int revision = 1; revision <= 50; revision++) {
            final int currentRevision = revision;
            executorService.submit(() ->
                ClientFavoriteSyncService.applyFullSync(currentRevision, Set.of(currentRevision % 41))
            );
        }

        executorService.shutdown();
        assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(50L, ClientFavoriteSyncService.lastRevisionForTesting());
        assertEquals(Set.of(50 % 41), FavoritesManager.getStateService().getFavoriteSlots());
    }
}
