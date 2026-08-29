package mycraft.yuyears.neofavoriteitems.render;

import java.util.EnumMap;
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
        OverlayProfileConfig config = configFor(overlay, mode);
        Cached cached = cache.get(mode);
        if (cached == null) {
            cached = new Cached(OverlayProfile.fromConfig(mode, config, overlay.locked.opacity));
            cache.put(mode, cached);
        }
        return cached.profile;
    }

    public void clear() {
        revision = Long.MIN_VALUE;
        cache.clear();
    }

    private static OverlayProfileConfig configFor(NeoFavoriteItemsConfig.Overlay overlay, OverlayMode mode) {
        return switch (mode) {
            case LOCKED -> overlay.locked;
            case BYPASS_LOCKED -> overlay.bypass;
            case LOCKABLE -> overlay.lockable;
            case UNLOCKABLE -> overlay.unlockable;
        };
    }

    private record Cached(OverlayProfile profile) {}
}
