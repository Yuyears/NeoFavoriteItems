package mycraft.yuyears.neofavoriteitems.forge.mixin;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.application.ClientDropGuard;
import mycraft.yuyears.neofavoriteitems.forge.NeoFavoriteItemsForge;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.client.Minecraft.class)
public abstract class MinecraftMixin {
    @Redirect(
        method = "handleKeybinds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;drop(Z)Z")
    )
    private boolean neoFavoriteItems$blockLockedHotbarDrop(LocalPlayer player, boolean dropAll) {
        int selectedSlot = player.getInventory().selected;
        boolean hasSelectedItem = !player.getInventory().getSelected().isEmpty();
        if (ClientDropGuard.shouldBlockSelectedHotbarDrop(selectedSlot, hasSelectedItem, NeoFavoriteItemsForge.isBypassKeyHeld())) {
            DebugLogger.debug("Forge blocked selected hotbar drop before client animation: slot={} dropAll={}", selectedSlot, dropAll);
            return false;
        }
        return player.drop(dropAll);
    }
}
