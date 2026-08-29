package mycraft.yuyears.neofavoriteitems.render;

import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayTextureCatalogTest {
    @Test
    void exposesIndependentHeartAndArrowPresets() {
        assertFalse(OverlayTextureCatalog.presetIds().contains("preset:color_overlay"));
        assertFalse(OverlayTextureCatalog.presetIds().contains(OverlayTextureCatalog.NO_MATERIAL));
        assertTrue(OverlayTextureCatalog.presetIds().contains("preset:heart"));
        assertTrue(OverlayTextureCatalog.presetIds().contains("preset:arrow"));
        assertEquals(
            "neo_favorite_items:textures/heart.png",
            OverlayTextureCatalog.textureFor(NeoFavoriteItemsConfig.OverlayStyle.MARK, "preset:heart").toString()
        );
        assertEquals(
            "neo_favorite_items:textures/arrow_down.png",
            OverlayTextureCatalog.textureFor(NeoFavoriteItemsConfig.OverlayStyle.MARK, "preset:arrow").toString()
        );
        assertNull(OverlayTextureCatalog.presetStyle("preset:heart"));
        assertNull(OverlayTextureCatalog.presetStyle("preset:arrow"));
    }
}
