
package mycraft.yuyears.neofavoriteitems;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import mycraft.yuyears.neofavoriteitems.application.InteractionGuardService;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;

public class FavoritesManager {
    private static FavoritesManager instance;
    private final Set<LogicalSlotIndex> favoriteSlots;
    private UUID currentPlayerUUID;

    private FavoritesManager() {
        this.favoriteSlots = new HashSet<>();
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
        return SlotMappingService.fromPlayerInventoryIndex(slot)
            .map(favoriteSlots::contains)
            .orElse(false);
    }

    public boolean isSlotFavorite(LogicalSlotIndex slot) {
        return favoriteSlots.contains(slot);
    }

    public void toggleSlotFavorite(int slot) {
        SlotMappingService.fromPlayerInventoryIndex(slot).ifPresent(this::toggleSlotFavorite);
    }

    public void toggleSlotFavorite(LogicalSlotIndex slot) {
        if (favoriteSlots.contains(slot)) {
            favoriteSlots.remove(slot);
            return;
        }
        favoriteSlots.add(slot);
    }

    public void setSlotFavorite(int slot, boolean favorite) {
        SlotMappingService.fromPlayerInventoryIndex(slot).ifPresent(logicalSlot -> setSlotFavorite(logicalSlot, favorite));
    }

    public void setSlotFavorite(LogicalSlotIndex slot, boolean favorite) {
        if (favorite) {
            favoriteSlots.add(slot);
        } else {
            favoriteSlots.remove(slot);
        }
    }

    public Set<Integer> getFavoriteSlots() {
        return favoriteSlots.stream()
            .map(LogicalSlotIndex::value)
            .collect(Collectors.toCollection(HashSet::new));
    }

    public Set<LogicalSlotIndex> getFavoriteLogicalSlots() {
        return new HashSet<>(favoriteSlots);
    }

    public void clearFavorites() {
        favoriteSlots.clear();
    }

    public void handleSlotSwap(int slot1, int slot2) {
        NeoFavoriteItemsConfig config = ConfigManager.getInstance().getConfig();
        if (config.slotBehavior.moveBehavior == NeoFavoriteItemsConfig.SlotMoveBehavior.FOLLOW_ITEM) {
            boolean slot1Fav = isSlotFavorite(slot1);
            boolean slot2Fav = isSlotFavorite(slot2);
            setSlotFavorite(slot1, slot2Fav);
            setSlotFavorite(slot2, slot1Fav);
        }
    }

    public void handleItemMove(int fromSlot, int toSlot) {
        NeoFavoriteItemsConfig config = ConfigManager.getInstance().getConfig();
        if (config.slotBehavior.moveBehavior == NeoFavoriteItemsConfig.SlotMoveBehavior.FOLLOW_ITEM) {
            boolean fromFav = isSlotFavorite(fromSlot);
            boolean toFav = isSlotFavorite(toSlot);
            setSlotFavorite(fromSlot, toFav);
            setSlotFavorite(toSlot, fromFav);
        }
    }

    public void handleSlotEmpty(int slot) {
        NeoFavoriteItemsConfig config = ConfigManager.getInstance().getConfig();
        if (config.general.autoUnlockEmptySlots) {
            setSlotFavorite(slot, false);
        }
    }

    public boolean shouldPreventInteraction(int slot, boolean isHoldingBypassKey) {
        return InteractionGuardService.getInstance()
            .evaluate(slot, InteractionType.CLICK, isHoldingBypassKey)
            .denied();
    }

    public byte[] serialize() {
        StringBuilder sb = new StringBuilder();
        for (LogicalSlotIndex slot : favoriteSlots) {
            sb.append(slot.value()).append(",");
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
                        SlotMappingService.fromPlayerInventoryIndex(Integer.parseInt(slotStr))
                            .ifPresent(favoriteSlots::add);
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
    }
}
