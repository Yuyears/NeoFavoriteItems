package mycraft.yuyears.neofavoriteitems.render;

import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.OverlayProfileConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class OverlayProfileCompilerTest {
    @Test
    void noMaterialCompilesToRenderableFillLayer() {
        var config = new OverlayProfileConfig(
            NeoFavoriteItemsConfig.OverlayStyle.MARK, 0xFFFF0000, 0.5f,
            OverlayProfileConfig.OpacityBehavior.FIXED, OverlayZIndex.ABOVE_ITEM
        );
        config.materialMode = OverlayMaterialMode.NO_MATERIAL;

        var profile = OverlayProfile.fromConfig(OverlayMode.LOCKED, config, 1.0f);

        assertEquals(OverlayLayerType.FILL, profile.layers().getFirst().type());
        assertEquals(OverlayColorMode.SOLID_ALPHA, profile.layers().getFirst().material().colorMode());
    }

    @Test
    void reusesUnchangedProfilesAndRecompilesChangedValues() {
        var overlay = new NeoFavoriteItemsConfig.Overlay();
        var compiler = new OverlayProfileCompiler();

        OverlayProfile first = compiler.resolve(overlay, OverlayMode.BYPASS_LOCKED, 1L);
        assertSame(first, compiler.resolve(overlay, OverlayMode.BYPASS_LOCKED, 1L));
        overlay.bypass.opacityBehavior = OverlayProfileConfig.OpacityBehavior.FIXED;
        overlay.bypass.opacity = 0.2f;
        OverlayProfile changed = compiler.resolve(overlay, OverlayMode.BYPASS_LOCKED, 2L);

        assertNotSame(first, changed);
        assertEquals(0.2f, changed.layers().getFirst().material().opacity());
    }
}
