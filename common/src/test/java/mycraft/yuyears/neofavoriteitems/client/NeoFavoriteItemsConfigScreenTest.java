package mycraft.yuyears.neofavoriteitems.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoFavoriteItemsConfigScreenTest {
    @Test
    void soundPreviewRequiresTwoHundredMillisecondCooldown() {
        assertFalse(NeoFavoriteItemsConfigScreen.soundPreviewReady(1_000L, 1_199L));
        assertTrue(NeoFavoriteItemsConfigScreen.soundPreviewReady(1_000L, 1_200L));
    }
}
