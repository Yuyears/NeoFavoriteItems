package mycraft.yuyears.neofavoriteitems.forge.mixin;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.application.ClientDropGuard;
import mycraft.yuyears.neofavoriteitems.forge.ForgeSlotInteractionHandler;
import mycraft.yuyears.neofavoriteitems.forge.NeoFavoriteItemsForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
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

    @Redirect(
        method = "handleKeybinds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V")
    )
    private void neoFavoriteItems$blockLockedOffhandSwap(
        ClientPacketListener listener,
        Packet<?> packet
    ) {
        if (packet instanceof ServerboundPlayerActionPacket actionPacket
            && actionPacket.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND
            && ForgeSlotInteractionHandler.shouldCancelOffhandSwap(Minecraft.getInstance().player.getInventory().selected)) {
            DebugLogger.debug("Forge blocked offhand swap before client packet: selectedSlot={}", Minecraft.getInstance().player.getInventory().selected);
            return;
        }
        listener.send(packet);
    }
}
