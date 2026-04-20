package mycraft.yuyears.neofavoriteitems;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class DebugLogger {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DebugLogger() {}

    public static void debug(String message, Object... args) {
        if (ConfigManager.getInstance().getConfig().debug.enabled) {
            LOGGER.info("[NeoFavoriteItems Debug] " + message, args);
        }
    }
}
