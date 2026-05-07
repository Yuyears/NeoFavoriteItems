package mycraft.yuyears.neofavoriteitems.neoforge.mixin;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoFavoriteItemsNeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.violetmoon.quark.content.management.module.EasyTransferringModule$Client", remap = false)
public abstract class QuarkEasyTransferringClientMixin {
    @Inject(method = "hasShiftDown(Z)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private static void neoFavoriteItems$letLockOperationOwnShiftSemantics(boolean original, CallbackInfoReturnable<Boolean> cir) {
        if (NeoFavoriteItemsNeoForge.isLockOperationKeyHeld()) {
            DebugLogger.debug("NeoForge Quark easy-transferring shift override suppressed during lock operation");
            cir.setReturnValue(false);
        }
    }
}
