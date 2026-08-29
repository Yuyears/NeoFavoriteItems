package mycraft.yuyears.neofavoriteitems.client;

import mycraft.yuyears.neofavoriteitems.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class ClientInteractionFeedback {
    private ClientInteractionFeedback() {}

    public static void playDeniedSound() {
        var feedback = ConfigManager.getInstance().getConfig().feedback;
        Minecraft minecraft = Minecraft.getInstance();
        if (!feedback.playSoundFeedback || minecraft == null) return;

        ResourceLocation id = ResourceLocation.tryParse(feedback.feedbackSound);
        if (id == null) return;

        BuiltInRegistries.SOUND_EVENT.getOptional(id).ifPresent(sound ->
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                sound,
                Math.clamp(feedback.feedbackPitch, 0.0f, 2.0f),
                Math.clamp(feedback.feedbackVolume, 0.0f, 1.0f)
            ))
        );
    }
}
