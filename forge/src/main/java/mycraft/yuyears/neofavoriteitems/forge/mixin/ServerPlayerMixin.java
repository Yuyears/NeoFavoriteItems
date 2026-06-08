package mycraft.yuyears.neofavoriteitems.forge.mixin;

import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void neoFavoriteItems$beginRespawnInventoryRestoreBypass(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        ServerFavoriteService.beginRespawnInventoryRestoreBypass((ServerPlayer) (Object) this, keepEverything);
    }

    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void neoFavoriteItems$endRespawnInventoryRestoreBypass(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        ServerFavoriteService.endRespawnInventoryRestoreBypass((ServerPlayer) (Object) this, keepEverything);
    }
}
