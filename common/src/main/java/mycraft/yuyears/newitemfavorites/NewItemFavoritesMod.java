
package mycraft.yuyears.newitemfavorites;

public class NewItemFavoritesMod {
    public static final String MOD_ID = "new_item_favorites";
    public static final String MOD_NAME = "New Item Favorites";
    public static final String MOD_VERSION = "1.0.0";

    private static NewItemFavoritesMod instance;
    private boolean serverPresent;

    private NewItemFavoritesMod() {}

    public static NewItemFavoritesMod getInstance() {
        if (instance == null) {
            instance = new NewItemFavoritesMod();
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
