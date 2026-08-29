package mycraft.yuyears.neofavoriteitems;

import net.minecraft.network.FriendlyByteBuf;

public record ServerRulesSnapshot(
    boolean autoUnlockEmptySlots,
    boolean lockEmptySlots,
    boolean allowItemsIntoLockedEmptySlots,
    boolean preventClick,
    boolean preventDrop,
    boolean preventQuickMove,
    boolean preventShiftClick,
    boolean preventDrag,
    boolean preventSwap,
    boolean allowBypassWithKey,
    NeoFavoriteItemsConfig.SlotMoveBehavior moveBehavior,
    boolean preserveLockedSlotContents,
    boolean debugEnabled
) {
    public static ServerRulesSnapshot from(NeoFavoriteItemsConfig config) {
        return new ServerRulesSnapshot(
            config.general.autoUnlockEmptySlots,
            config.general.lockEmptySlots,
            config.general.allowItemsIntoLockedEmptySlots,
            config.lockBehavior.preventClick,
            config.lockBehavior.preventDrop,
            config.lockBehavior.preventQuickMove,
            config.lockBehavior.preventShiftClick,
            config.lockBehavior.preventDrag,
            config.lockBehavior.preventSwap,
            config.lockBehavior.allowBypassWithKey,
            config.slotBehavior.moveBehavior,
            config.deathBehavior.preserveLockedSlotContents,
            config.debug.enabled
        );
    }

    public void applyTo(NeoFavoriteItemsConfig config) {
        config.general.autoUnlockEmptySlots = autoUnlockEmptySlots;
        config.general.lockEmptySlots = lockEmptySlots;
        config.general.allowItemsIntoLockedEmptySlots = allowItemsIntoLockedEmptySlots;
        config.lockBehavior.preventClick = preventClick;
        config.lockBehavior.preventDrop = preventDrop;
        config.lockBehavior.preventQuickMove = preventQuickMove;
        config.lockBehavior.preventShiftClick = preventShiftClick;
        config.lockBehavior.preventDrag = preventDrag;
        config.lockBehavior.preventSwap = preventSwap;
        config.lockBehavior.allowBypassWithKey = allowBypassWithKey;
        config.slotBehavior.moveBehavior = moveBehavior;
        config.deathBehavior.preserveLockedSlotContents = preserveLockedSlotContents;
        config.debug.enabled = debugEnabled;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(autoUnlockEmptySlots);
        buffer.writeBoolean(lockEmptySlots);
        buffer.writeBoolean(allowItemsIntoLockedEmptySlots);
        buffer.writeBoolean(preventClick);
        buffer.writeBoolean(preventDrop);
        buffer.writeBoolean(preventQuickMove);
        buffer.writeBoolean(preventShiftClick);
        buffer.writeBoolean(preventDrag);
        buffer.writeBoolean(preventSwap);
        buffer.writeBoolean(allowBypassWithKey);
        buffer.writeEnum(moveBehavior);
        buffer.writeBoolean(preserveLockedSlotContents);
        buffer.writeBoolean(debugEnabled);
    }

    public static ServerRulesSnapshot read(FriendlyByteBuf buffer) {
        return new ServerRulesSnapshot(
            buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
            buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
            buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
            buffer.readBoolean(), buffer.readEnum(NeoFavoriteItemsConfig.SlotMoveBehavior.class),
            buffer.readBoolean(), buffer.readBoolean()
        );
    }
}
