package mycraft.yuyears.neofavoriteitems.forge.mixin;

import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerDropEquipmentMixin {
    @Inject(method = "dropEquipment", at = @At("HEAD"))
    private void neoFavoriteItems$beginPlayerDeathDropPreservation(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer serverPlayer) {
            ServerFavoriteService.beginDeathDropPreservation(serverPlayer);
        }
    }

    @Inject(method = "dropEquipment", at = @At("RETURN"))
    private void neoFavoriteItems$endPlayerDeathDropPreservation(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer serverPlayer) {
            ServerFavoriteService.endDeathDropPreservation(serverPlayer);
        }
    }
}
