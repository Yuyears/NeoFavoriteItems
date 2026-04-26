package mycraft.yuyears.neofavoriteitems.forge.mixin;

import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RangedWrapper.class, remap = false)
public abstract class RangedWrapperMixin {
    @Shadow @Final private IItemHandlerModifiable compose;
    @Shadow @Final private int minSlot;

    @Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardLockedPlayerInventoryValidity(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Inventory inventory = getPlayerInventory();
        if (inventory != null && ServerFavoriteService.shouldPreventInventorySet(inventory, minSlot + slot, stack)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardPlayerInventoryExtract(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        Inventory inventory = getPlayerInventory();
        if (inventory != null && ServerFavoriteService.shouldPreventInventoryRemove(inventory, minSlot + slot)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardPlayerInventoryInsert(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        Inventory inventory = getPlayerInventory();
        if (inventory != null && ServerFavoriteService.shouldPreventInventorySet(inventory, minSlot + slot, stack)) {
            cir.setReturnValue(stack);
        }
    }

    @Inject(method = "setStackInSlot", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardPlayerInventorySet(int slot, ItemStack stack, CallbackInfo ci) {
        Inventory inventory = getPlayerInventory();
        if (inventory != null && ServerFavoriteService.shouldPreventInventorySet(inventory, minSlot + slot, stack)) {
            ci.cancel();
        }
    }

    private Inventory getPlayerInventory() {
        if (compose instanceof InvWrapper invWrapper) {
            Container container = invWrapper.getInv();
            if (container instanceof Inventory inventory) {
                return inventory;
            }
        }
        return null;
    }
}
