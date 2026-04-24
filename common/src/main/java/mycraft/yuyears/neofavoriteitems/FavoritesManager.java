
package mycraft.yuyears.neofavoriteitems;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import mycraft.yuyears.neofavoriteitems.application.InteractionGuardService;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import mycraft.yuyears.neofavoriteitems.persistence.FavoriteSlotCodec;

public class FavoritesManager implements FavoriteStateService, FavoriteSlotCodec {
    private static final FavoritesManager INSTANCE = new FavoritesManager();

    private final Set<LogicalSlotIndex> clientFavoriteSlots;
    private final Map<UUID, Set<LogicalSlotIndex>> favoriteSlotsByPlayer;
    private final ThreadLocal<UUID> currentPlayerUUID;

    private FavoritesManager() {
        this.clientFavoriteSlots = ConcurrentHashMap.newKeySet();
        this.favoriteSlotsByPlayer = new ConcurrentHashMap<>();
        this.currentPlayerUUID = new ThreadLocal<>();
    }

    public static FavoritesManager getInstance() {
        return INSTANCE;
    }

    public static FavoriteStateService getStateService() {
        return INSTANCE;
    }

    public static FavoriteSlotCodec getCodec() {
        return INSTANCE;
    }

    @Override
    public void setPlayer(UUID playerUUID) {
        currentPlayerUUID.set(playerUUID);
        if (playerUUID != null) {
            favoriteSlotsByPlayer.computeIfAbsent(playerUUID, ignored -> ConcurrentHashMap.newKeySet());
        }
    }

    @Override
    public void clearPlayer() {
        currentPlayerUUID.remove();
        clientFavoriteSlots.clear();
    }

    @Override
    public void removePlayer(UUID playerUUID) {
        if (playerUUID != null) {
            favoriteSlotsByPlayer.remove(playerUUID);
        }
    }

    public boolean isSlotFavorite(int slot) {
        return SlotMappingService.fromPlayerInventoryIndex(slot)
            .map(activeSlots()::contains)
            .orElse(false);
    }

    @Override
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

    @Override
    public void setSlotFavorite(LogicalSlotIndex slot, boolean favorite) {
        Set<LogicalSlotIndex> favoriteSlots = activeSlots();
        if (favorite) {
            favoriteSlots.add(slot);
        } else {
            favoriteSlots.remove(slot);
        }
    }

    @Override
    public Set<Integer> getFavoriteSlots() {
        return activeSlots().stream()
            .map(LogicalSlotIndex::value)
            .collect(Collectors.toCollection(java.util.HashSet::new));
    }

    @Override
    public Set<LogicalSlotIndex> getFavoriteLogicalSlots() {
        return new java.util.HashSet<>(activeSlots());
    }

    @Override
    public void clearFavorites() {
        activeSlots().clear();
    }

    @Override
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

    @Override
    public byte[] serialize() {
        Set<LogicalSlotIndex> favoriteSlots = activeSlots();
        StringBuilder serialized = new StringBuilder(Math.max(16, favoriteSlots.size() * 3));
        Iterator<LogicalSlotIndex> iterator = favoriteSlots.iterator();
        while (iterator.hasNext()) {
            serialized.append(iterator.next().value());
            if (iterator.hasNext()) {
                serialized.append(',');
            }
        }
        return serialized.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void deserialize(byte[] data) {
        Set<LogicalSlotIndex> favoriteSlots = activeSlots();
        favoriteSlots.clear();

        if (data == null || data.length == 0) {
            return;
        }

        String serialized = new String(data, StandardCharsets.UTF_8);
        if (serialized.isEmpty()) {
            return;
        }

        String[] slots = serialized.split(",");
        for (String slotText : slots) {
            if (slotText.isEmpty()) {
                continue;
            }

            try {
                int slotIndex = Integer.parseInt(slotText);
                if (SlotMappingService.fromPlayerInventoryIndex(slotIndex).map(favoriteSlots::add).isEmpty()) {
                    DebugLogger.warn("Ignored favorite slot outside supported range during deserialize: slot={}", slotIndex);
                }
            } catch (NumberFormatException exception) {
                DebugLogger.warn("Ignored malformed favorite slot entry during deserialize: value={}", slotText);
            }
        }
    }

    private Set<LogicalSlotIndex> activeSlots() {
        UUID playerUUID = currentPlayerUUID.get();
        if (playerUUID == null) {
            return clientFavoriteSlots;
        }
        return favoriteSlotsByPlayer.computeIfAbsent(playerUUID, ignored -> ConcurrentHashMap.newKeySet());
    }
}
