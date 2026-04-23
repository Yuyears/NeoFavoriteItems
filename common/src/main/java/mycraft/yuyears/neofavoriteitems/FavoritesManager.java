
package mycraft.yuyears.neofavoriteitems;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import mycraft.yuyears.neofavoriteitems.application.InteractionGuardService;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;

public class FavoritesManager {
    private static FavoritesManager instance;
    private final Set<LogicalSlotIndex> clientFavoriteSlots;
    private final Map<UUID, Set<LogicalSlotIndex>> favoriteSlotsByPlayer;
    private UUID currentPlayerUUID;

    private FavoritesManager() {
        this.clientFavoriteSlots = new HashSet<>();
        this.favoriteSlotsByPlayer = new HashMap<>();
    }

    public static FavoritesManager getInstance() {
        if (instance == null) {
            instance = new FavoritesManager();
        }
        return instance;
    }

    public void setPlayer(UUID playerUUID) {
        this.currentPlayerUUID = playerUUID;
        if (playerUUID != null) {
            this.favoriteSlotsByPlayer.computeIfAbsent(playerUUID, ignored -> new HashSet<>());
        }
    }

    public void clearPlayer() {
        this.currentPlayerUUID = null;
        this.clientFavoriteSlots.clear();
    }

    public boolean isSlotFavorite(int slot) {
        return SlotMappingService.fromPlayerInventoryIndex(slot)
            .map(activeSlots()::contains)
            .orElse(false);
    }

    public boolean isSlotFavorite(LogicalSlotIndex slot) {
        return activeSlots().contains(slot);
    }

    public void toggleSlotFavorite(int slot) {
        SlotMappingService.fromPlayerInventoryIndex(slot).ifPresent(this::toggleSlotFavorite);
    }

    public void toggleSlotFavorite(LogicalSlotIndex slot) {
        Set<LogicalSlotIndex> favoriteSlots = activeSlots();
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
        Set<LogicalSlotIndex> favoriteSlots = activeSlots();
        if (favorite) {
            favoriteSlots.add(slot);
        } else {
            favoriteSlots.remove(slot);
        }
    }

    public Set<Integer> getFavoriteSlots() {
        return activeSlots().stream()
            .map(LogicalSlotIndex::value)
            .collect(Collectors.toCollection(HashSet::new));
    }

    public Set<LogicalSlotIndex> getFavoriteLogicalSlots() {
        return new HashSet<>(activeSlots());
    }

    public void clearFavorites() {
        activeSlots().clear();
    }

    public void setFavoriteSlots(Set<LogicalSlotIndex> slots) {
        Set<LogicalSlotIndex> favoriteSlots = activeSlots();
        favoriteSlots.clear();
        favoriteSlots.addAll(slots);
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
        for (LogicalSlotIndex slot : activeSlots()) {
            sb.append(slot.value()).append(",");
        }
        return sb.toString().getBytes();
    }

    public void deserialize(byte[] data) {
        Set<LogicalSlotIndex> favoriteSlots = activeSlots();
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

    private Set<LogicalSlotIndex> activeSlots() {
        if (currentPlayerUUID == null) {
            return clientFavoriteSlots;
        }
        return favoriteSlotsByPlayer.computeIfAbsent(currentPlayerUUID, ignored -> new HashSet<>());
    }
}
