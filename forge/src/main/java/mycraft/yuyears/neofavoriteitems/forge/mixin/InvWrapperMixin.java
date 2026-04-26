package mycraft.yuyears.neofavoriteitems.forge.mixin;

import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InvWrapper.class, remap = false)
public abstract class InvWrapperMixin {
    @Shadow public abstract Container getInv();

    @Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardLockedPlayerInventoryValidity(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Container container = getInv();
        if (container instanceof Inventory inventory
            && ServerFavoriteService.shouldPreventInventorySet(inventory, slot, stack)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardPlayerInventoryExtract(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        Container container = getInv();
        if (container instanceof Inventory inventory
            && ServerFavoriteService.shouldPreventInventoryRemove(inventory, slot)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardPlayerInventoryInsert(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        Container container = getInv();
        if (container instanceof Inventory inventory
            && ServerFavoriteService.shouldPreventInventorySet(inventory, slot, stack)) {
            cir.setReturnValue(stack);
        }
    }

    @Inject(method = "setStackInSlot", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardPlayerInventorySet(int slot, ItemStack stack, CallbackInfo ci) {
        Container container = getInv();
        if (container instanceof Inventory inventory
            && ServerFavoriteService.shouldPreventInventorySet(inventory, slot, stack)) {
            ci.cancel();
        }
    }
}
