
package mycraft.yuyears.newitemfavorites;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FavoritesManager {
    private static FavoritesManager instance;
    private final Set&lt;Integer&gt; favoriteSlots;
    private UUID currentPlayerUUID;

    private FavoritesManager() {
        this.favoriteSlots = new HashSet&lt;&gt;();
    }

    public static FavoritesManager getInstance() {
        if (instance == null) {
            instance = new FavoritesManager();
        }
        return instance;
    }

    public void setPlayer(UUID playerUUID) {
        if (this.currentPlayerUUID == null || !this.currentPlayerUUID.equals(playerUUID)) {
            this.currentPlayerUUID = playerUUID;
            this.favoriteSlots.clear();
        }
    }

    public void clearPlayer() {
        this.currentPlayerUUID = null;
        this.favoriteSlots.clear();
    }

    public boolean isSlotFavorite(int slot) {
        return favoriteSlots.contains(slot);
    }

    public void toggleSlotFavorite(int slot) {
        if (favoriteSlots.contains(slot)) {
            favoriteSlots.remove(slot);
        } else {
            favoriteSlots.add(slot);
        }
    }

    public void setSlotFavorite(int slot, boolean favorite) {
        if (favorite) {
            favoriteSlots.add(slot);
        } else {
            favoriteSlots.remove(slot);
        }
    }

    public Set&lt;Integer&gt; getFavoriteSlots() {
        return new HashSet&lt;&gt;(favoriteSlots);
    }

    public void clearFavorites() {
        favoriteSlots.clear();
    }

    public void handleSlotSwap(int slot1, int slot2) {
        NewItemFavoritesConfig config = ConfigManager.getInstance().getConfig();
        if (config.slotBehavior.moveBehavior == NewItemFavoritesConfig.SlotMoveBehavior.FOLLOW_ITEM) {
            boolean slot1Fav = isSlotFavorite(slot1);
            boolean slot2Fav = isSlotFavorite(slot2);
            setSlotFavorite(slot1, slot2Fav);
            setSlotFavorite(slot2, slot1Fav);
        }
    }

    public void handleItemMove(int fromSlot, int toSlot) {
        NewItemFavoritesConfig config = ConfigManager.getInstance().getConfig();
        if (config.slotBehavior.moveBehavior == NewItemFavoritesConfig.SlotMoveBehavior.FOLLOW_ITEM) {
            boolean fromFav = isSlotFavorite(fromSlot);
            boolean toFav = isSlotFavorite(toSlot);
            setSlotFavorite(fromSlot, toFav);
            setSlotFavorite(toSlot, fromFav);
        }
    }

    public void handleSlotEmpty(int slot) {
        NewItemFavoritesConfig config = ConfigManager.getInstance().getConfig();
        if (config.general.autoUnlockEmptySlots) {
            setSlotFavorite(slot, false);
        }
    }

    public boolean shouldPreventInteraction(int slot, boolean isHoldingBypassKey) {
        if (!isSlotFavorite(slot)) {
            return false;
        }

        NewItemFavoritesConfig config = ConfigManager.getInstance().getConfig();
        
        if (isHoldingBypassKey &amp;&amp; config.lockBehavior.allowBypassWithKey) {
            return false;
        }

        return true;
    }

    public byte[] serialize() {
        StringBuilder sb = new StringBuilder();
        for (int slot : favoriteSlots) {
            sb.append(slot).append(",");
        }
        return sb.toString().getBytes();
    }

    public void deserialize(byte[] data) {
        favoriteSlots.clear();
        String str = new String(data);
        if (!str.isEmpty()) {
            String[] slots = str.split(",");
            for (String slotStr : slots) {
                if (!slotStr.isEmpty()) {
                    try {
                        favoriteSlots.add(Integer.parseInt(slotStr));
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
    }
}
