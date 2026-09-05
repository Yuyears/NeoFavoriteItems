package mycraft.yuyears.neofavoriteitems.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import mycraft.yuyears.neofavoriteitems.OverlayProfileConfig;

/** Bounded ordered layer collection. Codec/UI can use this before renderer migration. */
public final class OverlayLayerList {
    public static final int MAX_LAYERS = 4;

    private OverlayLayerList() {}

    public static List<OverlayProfileConfig> normalize(
            List<OverlayProfileConfig> source,
            Supplier<OverlayProfileConfig> standardFactory) {
        List<OverlayProfileConfig> result = new ArrayList<>();
        if (source != null) {
            for (OverlayProfileConfig layer : source) {
                if (layer == null) continue;
                result.add(layer.copy());
                if (result.size() == MAX_LAYERS) break;
            }
        }
        if (result.isEmpty()) result.add(standardFactory.get());
        return List.copyOf(result);
    }

    public static List<OverlayProfileConfig> copyOf(List<OverlayProfileConfig> source) {
        return normalize(source, () -> OverlayProfileConfig.defaultLocked());
    }
}
