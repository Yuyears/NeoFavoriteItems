package mycraft.yuyears.newitemfavorites.application;

import mycraft.yuyears.newitemfavorites.ConfigManager;
import mycraft.yuyears.newitemfavorites.FavoritesManager;
import mycraft.yuyears.newitemfavorites.NewItemFavoritesConfig;
import mycraft.yuyears.newitemfavorites.domain.InteractionDecision;
import mycraft.yuyears.newitemfavorites.domain.InteractionType;
import mycraft.yuyears.newitemfavorites.domain.LogicalSlotIndex;
import mycraft.yuyears.newitemfavorites.integration.SlotMappingService;

public class InteractionGuardService {
    private static InteractionGuardService instance;

    private final FavoritesManager favoritesManager;
    private final ConfigManager configManager;

    private InteractionGuardService(FavoritesManager favoritesManager, ConfigManager configManager) {
        this.favoritesManager = favoritesManager;
        this.configManager = configManager;
    }

    public static InteractionGuardService getInstance() {
        if (instance == null) {
            instance = new InteractionGuardService(FavoritesManager.getInstance(), ConfigManager.getInstance());
        }
        return instance;
    }

    public InteractionDecision evaluate(int slot, InteractionType interactionType, boolean holdingBypassKey) {
        return SlotMappingService.fromPlayerInventoryIndex(slot)
            .map(logicalSlot -> evaluate(logicalSlot, interactionType, holdingBypassKey))
            .orElseGet(InteractionDecision::allow);
    }

    public InteractionDecision evaluate(LogicalSlotIndex slot, InteractionType interactionType, boolean holdingBypassKey) {
        if (!favoritesManager.isSlotFavorite(slot)) {
            return InteractionDecision.allow();
        }

        NewItemFavoritesConfig config = configManager.getConfig();
        if (holdingBypassKey && config.lockBehavior.allowBypassWithKey) {
            return InteractionDecision.allowBypass();
        }

        if (shouldBlockByConfig(config, interactionType)) {
            return InteractionDecision.deny("favorite_slot_" + interactionType.name().toLowerCase());
        }

        return InteractionDecision.allow();
    }

    private boolean shouldBlockByConfig(NewItemFavoritesConfig config, InteractionType interactionType) {
        return switch (interactionType) {
            case CLICK -> config.lockBehavior.preventClick;
            case DROP -> config.lockBehavior.preventDrop;
            case QUICK_MOVE -> config.lockBehavior.preventQuickMove;
            case SHIFT_CLICK -> config.lockBehavior.preventShiftClick;
            case DRAG -> config.lockBehavior.preventDrag;
            case SWAP -> config.lockBehavior.preventSwap;
            case USE_ITEM, PLACE_BLOCK, CONSUME_ITEM, USE_TOOL_OR_WEAPON -> true;
            case UNKNOWN -> true;
        };
    }
}
