package mycraft.yuyears.neofavoriteitems.application;

import mycraft.yuyears.neofavoriteitems.domain.InteractionType;

public final class ClientDropGuard {
    private ClientDropGuard() {}

    public static boolean shouldBlockSelectedHotbarDrop(int selectedSlot, boolean hasSelectedItem, boolean bypassKeyHeld) {
        return InteractionGuardService.getInstance()
            .evaluate(selectedSlot, InteractionType.DROP, bypassKeyHeld, hasSelectedItem)
            .denied();
    }
}
