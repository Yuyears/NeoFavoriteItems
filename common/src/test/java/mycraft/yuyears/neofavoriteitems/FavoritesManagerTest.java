package mycraft.yuyears.neofavoriteitems;

import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FavoritesManagerTest {
    @BeforeEach
    void setUp() {
        FavoritesManager.getStateService().clearPlayer();
        FavoritesManager.getStateService().clearFavorites();
    }

    @Test
    void serializeUsesUtf8AndDeserializeIgnoresMalformedEntries() {
        FavoritesManager.getStateService().setFavoriteSlots(Set.of(
            LogicalSlotIndex.of(0),
            LogicalSlotIndex.of(10)
        ));

        byte[] serialized = FavoritesManager.getCodec().serialize();
        Set<String> tokens = Arrays.stream(new String(serialized, StandardCharsets.UTF_8).split(","))
            .collect(Collectors.toSet());
        assertEquals(Set.of("0", "10"), tokens);

        FavoritesManager.getCodec().deserialize("1,abc,99".getBytes(StandardCharsets.UTF_8));
        assertEquals(Set.of(1), FavoritesManager.getStateService().getFavoriteSlots());
    }

    @Test
    void playerContextIsThreadLocal() throws Exception {
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread first = new Thread(() -> runFavoriteWrite(firstPlayer, 3, ready, release, failure), "favorites-first");
        Thread second = new Thread(() -> runFavoriteWrite(secondPlayer, 7, ready, release, failure), "favorites-second");
        first.start();
        second.start();

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        release.countDown();
        first.join();
        second.join();
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }

        FavoritesManager.getStateService().setPlayer(firstPlayer);
        assertEquals(Set.of(3), FavoritesManager.getStateService().getFavoriteSlots());
        FavoritesManager.getStateService().setPlayer(secondPlayer);
        assertEquals(Set.of(7), FavoritesManager.getStateService().getFavoriteSlots());
    }

    private void runFavoriteWrite(
        UUID playerId,
        int slot,
        CountDownLatch ready,
        CountDownLatch release,
        AtomicReference<Throwable> failure
    ) {
        try {
            FavoritesManager.getStateService().setPlayer(playerId);
            FavoritesManager.getStateService().setSlotFavorite(LogicalSlotIndex.of(slot), true);
            ready.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            assertEquals(Set.of(slot), FavoritesManager.getStateService().getFavoriteSlots());
        } catch (Throwable throwable) {
            failure.compareAndSet(null, throwable);
        } finally {
            FavoritesManager.getStateService().clearPlayer();
        }
    }
}
