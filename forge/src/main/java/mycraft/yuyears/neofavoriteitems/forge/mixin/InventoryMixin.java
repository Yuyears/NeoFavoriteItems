package mycraft.yuyears.neofavoriteitems.forge.mixin;

import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Inject(method = "removeItem(II)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardLockedSlotRemove(int inventoryIndex, int count, CallbackInfoReturnable<ItemStack> cir) {
        if (ServerFavoriteService.shouldPreventInventoryRemove((Inventory) (Object) this, inventoryIndex)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "removeItemNoUpdate", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardLockedSlotRemoveNoUpdate(int inventoryIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (ServerFavoriteService.shouldPreventInventoryRemove((Inventory) (Object) this, inventoryIndex)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardLockedSlotSet(int inventoryIndex, ItemStack stack, CallbackInfo ci) {
        if (ServerFavoriteService.shouldPreventInventorySet((Inventory) (Object) this, inventoryIndex, stack)) {
            ci.cancel();
        }
    }
}
