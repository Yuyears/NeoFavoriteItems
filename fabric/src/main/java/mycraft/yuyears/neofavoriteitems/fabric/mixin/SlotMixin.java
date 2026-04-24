package mycraft.yuyears.neofavoriteitems.fabric.mixin;

import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {
    @Shadow @Final public Container container;

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardLockedSlotPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player == null) {
            return;
        }
        if (ServerFavoriteService.shouldPreventSlotPickup((Slot) (Object) this, player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardLockedSlotPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (container instanceof Inventory inventory
            && inventory.player != null
            && ServerFavoriteService.shouldPreventSlotPlace((Slot) (Object) this, inventory.player)) {
            cir.setReturnValue(false);
        }
    }
}
