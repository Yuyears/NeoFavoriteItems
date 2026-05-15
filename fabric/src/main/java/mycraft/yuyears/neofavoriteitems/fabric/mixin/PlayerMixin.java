package mycraft.yuyears.neofavoriteitems.fabric.mixin;

import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PlayerMixin {
    @Inject(method = "setItemInHand", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$rerouteLockedSelectedSlotHandSet(InteractionHand hand, ItemStack stack, CallbackInfo ci) {
        if ((Object) this instanceof Player player
            && ServerFavoriteService.shouldRerouteMainHandSet(player, hand, stack)) {
            ci.cancel();
        }
    }
}
