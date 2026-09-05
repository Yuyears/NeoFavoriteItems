package mycraft.yuyears.neofavoriteitems.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import mycraft.yuyears.neofavoriteitems.OverlayProfileConfig;

class OverlayLayerListTest {
    @Test
    void normalizesEmptyAndTruncatesToFour() {
        assertEquals(1, OverlayLayerList.normalize(List.of(), OverlayProfileConfig::defaultLocked).size());
        List<OverlayProfileConfig> source = new ArrayList<>();
        for (int i = 0; i < 6; i++) source.add(OverlayProfileConfig.defaultLocked());
        assertEquals(OverlayLayerList.MAX_LAYERS,
            OverlayLayerList.normalize(source, OverlayProfileConfig::defaultLocked).size());
    }

    @Test
    void normalizationCopiesLayerValues() {
        OverlayProfileConfig original = OverlayProfileConfig.defaultBypass();
        List<OverlayProfileConfig> normalized = OverlayLayerList.normalize(List.of(original), OverlayProfileConfig::defaultLocked);
        original.opacity = 0.0f;
        assertEquals(OverlayProfileConfig.DEFAULT_BYPASS_OPACITY, normalized.getFirst().opacity);
    }
}
