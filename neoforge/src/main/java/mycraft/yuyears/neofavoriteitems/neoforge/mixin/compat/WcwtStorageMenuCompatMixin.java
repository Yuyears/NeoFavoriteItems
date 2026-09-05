package mycraft.yuyears.neofavoriteitems.neoforge.mixin.compat;

import mycraft.yuyears.neofavoriteitems.application.ExternalInventoryGuard;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "appeng.menu.AEBaseMenu", remap = false)
public abstract class WcwtStorageMenuCompatMixin {
    @Inject(method = "isPlayerInventorySlotLocked(I)Z", at = @At("RETURN"), cancellable = true)
    private void neoFavoriteItems$includeFavoriteSlotLocks(int inventoryIndex, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || inventoryIndex < 0 || inventoryIndex >= 36) return;
        try {
            Object value = this.getClass().getMethod("getPlayer").invoke(this);
            if (value instanceof Player player) cir.setReturnValue(ExternalInventoryGuard.isFavoriteSlot(player, inventoryIndex));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Keep WCWT behavior unchanged when menu API differs.
        }
    }
}
