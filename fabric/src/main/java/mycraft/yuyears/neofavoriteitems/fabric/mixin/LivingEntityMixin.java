package mycraft.yuyears.neofavoriteitems.fabric.mixin;

import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "dropEquipment", at = @At("HEAD"))
    private void neoFavoriteItems$beginDeathDropPreservation(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer serverPlayer) {
            ServerFavoriteService.beginDeathDropPreservation(serverPlayer);
        }
    }

    @Inject(method = "dropEquipment", at = @At("RETURN"))
    private void neoFavoriteItems$endDeathDropPreservation(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer serverPlayer) {
            ServerFavoriteService.endDeathDropPreservation(serverPlayer);
        }
    }
}
