package mycraft.yuyears.neofavoriteitems.application;

import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.domain.InteractionDecision;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;

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

    public InteractionDecision evaluate(int slot, InteractionType interactionType, boolean holdingBypassKey, boolean hasItem) {
        return SlotMappingService.fromPlayerInventoryIndex(slot)
            .map(logicalSlot -> evaluate(logicalSlot, interactionType, holdingBypassKey, hasItem))
            .orElseGet(InteractionDecision::allow);
    }

    public InteractionDecision evaluate(LogicalSlotIndex slot, InteractionType interactionType, boolean holdingBypassKey) {
        return evaluate(slot, interactionType, holdingBypassKey, true);
    }

    public InteractionDecision evaluate(LogicalSlotIndex slot, InteractionType interactionType, boolean holdingBypassKey, boolean hasItem) {
        if (!favoritesManager.isSlotFavorite(slot)) {
            DebugLogger.debug("Interaction allowed: slot={} type={} reason=not_locked", slot.value(), interactionType);
            return InteractionDecision.allow();
        }

        NeoFavoriteItemsConfig config = configManager.getConfig();
        if (!hasItem) {
            if (!config.general.lockEmptySlots || config.general.autoUnlockEmptySlots) {
                favoritesManager.setSlotFavorite(slot, false);
                DebugLogger.debug("Interaction allowed: slot={} type={} reason=auto_unlock_empty_slot", slot.value(), interactionType);
                return InteractionDecision.allow();
            }
            if (config.general.allowItemsIntoLockedEmptySlots) {
                DebugLogger.debug("Interaction allowed: slot={} type={} reason=locked_empty_slot_allows_insert", slot.value(), interactionType);
                return InteractionDecision.allow();
            }
        }

        if (holdingBypassKey && config.lockBehavior.allowBypassWithKey) {
            DebugLogger.debug("Interaction bypassed: slot={} type={} reason=bypass_key", slot.value(), interactionType);
            return InteractionDecision.allowBypass();
        }

        if (shouldBlockByConfig(config, interactionType)) {
            DebugLogger.debug("Interaction denied: slot={} type={} reason=config", slot.value(), interactionType);
            return InteractionDecision.deny("favorite_slot_" + interactionType.name().toLowerCase());
        }

        DebugLogger.debug("Interaction allowed: slot={} type={} reason=config_allows", slot.value(), interactionType);
        return InteractionDecision.allow();
    }

    private boolean shouldBlockByConfig(NeoFavoriteItemsConfig config, InteractionType interactionType) {
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
