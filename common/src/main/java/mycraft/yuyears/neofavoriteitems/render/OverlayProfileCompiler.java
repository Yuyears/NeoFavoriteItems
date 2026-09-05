package mycraft.yuyears.neofavoriteitems.render;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.OverlayProfileConfig;

/** Keeps mutable config at the boundary and immutable profiles in the render hot path. */
public final class OverlayProfileCompiler {
    private final EnumMap<OverlayMode, Cached> cache = new EnumMap<>(OverlayMode.class);
    private long revision = Long.MIN_VALUE;

    public OverlayProfile resolve(NeoFavoriteItemsConfig.Overlay overlay, OverlayMode mode, long revision) {
        if (this.revision != revision) {
            this.revision = revision;
            cache.clear();
        }
        Cached cached = cache.get(mode);
        if (cached == null) {
            List<OverlayProfileConfig> configs = new ArrayList<>();
            configs.add(legacyConfig(overlay, mode));
            List<OverlayProfileConfig> configured = overlay.layers(mode);
            if (configured.size() > 1) configs.addAll(configured.subList(1, configured.size()));
            List<OverlayLayer> layers = new ArrayList<>();
            for (OverlayProfileConfig config : configs) {
                layers.addAll(OverlayProfile.fromConfig(mode, config, overlay.locked.opacity).layers());
            }
            layers.sort(Comparator.comparingInt(OverlayLayer::zIndex));
            cached = new Cached(new OverlayProfile(mode, layers));
            cache.put(mode, cached);
        }
        return cached.profile;
    }

    private static OverlayProfileConfig legacyConfig(NeoFavoriteItemsConfig.Overlay overlay, OverlayMode mode) {
        return switch (mode) {
            case LOCKED -> overlay.locked;
            case BYPASS_LOCKED -> overlay.bypass;
            case LOCKABLE -> overlay.lockable;
            case UNLOCKABLE -> overlay.unlockable;
        };
    }

    public void clear() {
        revision = Long.MIN_VALUE;
        cache.clear();
    }

    private record Cached(OverlayProfile profile) {}
}
