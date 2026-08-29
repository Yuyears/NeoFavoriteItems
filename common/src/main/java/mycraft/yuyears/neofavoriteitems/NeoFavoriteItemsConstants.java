package mycraft.yuyears.neofavoriteitems;

public final class NeoFavoriteItemsConstants {
    public static final String COMMON_CONFIG_FILE_NAME = "NFI/server/server-rules.toml";
    public static final String CLIENT_CONFIG_FILE_NAME = "NFI/client/rendering.toml";
    public static final String CLIENT_LOGIC_CONFIG_FILE_NAME = "NFI/client/logic.toml";
    public static final String LEGACY_COMMON_CONFIG_FILE_NAME = "neo-favorite-items-common.toml";
    public static final String LEGACY_CLIENT_CONFIG_FILE_NAME = "neo-favorite-items-client.toml";
    public static final String CONFIG_FILE_NAME = "neo-favorite-items.toml";
    public static final String KEY_CATEGORY = "category.neo_favorite_items";
    public static final String LOCK_OPERATION_KEY_ID = "key.neo_favorite_items.lock_operation";
    public static final String BYPASS_LOCK_KEY_ID = "key.neo_favorite_items.bypass_lock";
    public static final String CONFIG_UI_KEY_ID = "key.neo_favorite_items.config_ui";
    public static final int DEFAULT_LOCK_OPERATION_KEY_CODE = 342; // GLFW_KEY_LEFT_ALT
    public static final int DEFAULT_BYPASS_LOCK_KEY_CODE = 341; // GLFW_KEY_LEFT_CONTROL
    public static final int DEFAULT_CONFIG_UI_KEY_CODE = 79; // GLFW_KEY_O
    public static final int NETWORK_PROTOCOL_VERSION = 1;
    public static final String NETWORK_PROTOCOL_VERSION_STRING = "1";
    public static final String PLAYER_DATA_DIRECTORY = "players";
    public static final String CLIENT_SAVE_DIRECTORY = "favoriteitems";
    public static final String LEGACY_CLIENT_SAVE_DIRECTORY = "itemfavorites";
    public static final String DEFAULT_SERVER_DIRECTORY = "default_server";
    public static final String CUSTOM_ASSETS_DIRECTORY = "NFI Assets";

    private NeoFavoriteItemsConstants() {}
}
