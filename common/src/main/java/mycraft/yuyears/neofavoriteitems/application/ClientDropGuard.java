package mycraft.yuyears.neofavoriteitems.application;

import mycraft.yuyears.neofavoriteitems.client.ClientInteractionFeedback;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;

public final class ClientDropGuard {
    private ClientDropGuard() {}

    public static boolean shouldBlockSelectedHotbarDrop(int selectedSlot, boolean hasSelectedItem, boolean bypassKeyHeld) {
        boolean denied = InteractionGuardService.getInstance()
            .evaluate(selectedSlot, InteractionType.DROP, bypassKeyHeld, hasSelectedItem)
            .denied();
        if (denied) ClientInteractionFeedback.playDeniedSound();
        return denied;
    }
}
