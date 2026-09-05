package mycraft.yuyears.neofavoriteitems.render;

import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OverlayTextureCatalogTest {
    @Test
    void exposesEveryRenderingPresetInCatalogOrder() {
        assertEquals(List.of(
            "preset:border", "preset:brackets", "preset:classic", "preset:framework",
            "preset:heart_0", "preset:heart_1", "preset:heart_2", "preset:heart",
            "preset:highlight", "preset:lock_0", "preset:lock_1", "preset:lock_2",
            "preset:lock", "preset:lock_flat", "preset:mark", "preset:star",
            "preset:tag", "preset:arrow_down"
        ), OverlayTextureCatalog.presetIds());
        assertEquals(
            "neo_favorite_items:textures/heart.png",
            OverlayTextureCatalog.textureFor(NeoFavoriteItemsConfig.OverlayStyle.MARK, "preset:heart").toString()
        );
        assertEquals(
            "neo_favorite_items:textures/arrow_down.png",
            OverlayTextureCatalog.textureFor(NeoFavoriteItemsConfig.OverlayStyle.MARK, "preset:arrow_down").toString()
        );
        assertEquals(
            "neo_favorite_items:textures/arrow_down.png",
            OverlayTextureCatalog.textureFor(NeoFavoriteItemsConfig.OverlayStyle.MARK, "preset:arrow").toString()
        );
        assertEquals("preset:arrow_down", OverlayTextureCatalog.canonicalPresetId("preset:arrow"));
        assertNull(OverlayTextureCatalog.presetStyle("preset:heart"));
        assertNull(OverlayTextureCatalog.presetStyle("preset:arrow_down"));
    }
}
