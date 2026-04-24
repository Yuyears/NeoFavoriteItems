package mycraft.yuyears.neofavoriteitems;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class DebugLogger {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DEBUG_PREFIX = "[NeoFavoriteItems Debug] ";
    private static final String LOG_PREFIX = "[NeoFavoriteItems] ";

    private DebugLogger() {}

    public static void debug(String message, Object... args) {
        if (ConfigManager.getInstance().getConfig().debug.enabled) {
            LOGGER.debug(DEBUG_PREFIX + message, args);
        }
    }

    public static void warn(String message, Object... args) {
        LOGGER.warn(LOG_PREFIX + message, args);
    }

    public static void error(String message, Object... args) {
        LOGGER.error(LOG_PREFIX + message, args);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.error(LOG_PREFIX + message, throwable);
    }
}
