
package mycraft.yuyears.neofavoriteitems;

public class NeoFavoriteItemsMod {
    public static final String MOD_ID = "neo_favorite_items";
    public static final String MOD_NAME = "Neo Favorite Items";
    public static final String MOD_VERSION = "0.0.1-alpha";

    private static NeoFavoriteItemsMod instance;
    private boolean serverPresent;

    private NeoFavoriteItemsMod() {}

    public static NeoFavoriteItemsMod getInstance() {
        if (instance == null) {
            instance = new NeoFavoriteItemsMod();
        }
        return instance;
    }

    public void initialize() {
        serverPresent = false;
    }

    public void onClientInitialize() {
    }

    public void onServerInitialize() {
        serverPresent = true;
    }

    public boolean isServerPresent() {
        return serverPresent;
    }

    public void setServerPresent(boolean serverPresent) {
        this.serverPresent = serverPresent;
    }
}
