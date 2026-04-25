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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

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
            && ServerFavoriteService.shouldPreventSlotPlace((Slot) (Object) this, inventory.player, stack)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardLockedSlotRemove(int amount, CallbackInfoReturnable<ItemStack> cir) {
        if (shouldPreventSlotRemoval()) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "tryRemove", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardLockedSlotTryRemove(int count, int decrement, Player player, CallbackInfoReturnable<Optional<ItemStack>> cir) {
        if (player != null && ServerFavoriteService.shouldPreventSlotPickup((Slot) (Object) this, player)) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Inject(method = "safeTake", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardLockedSlotSafeTake(int count, int decrement, Player player, CallbackInfoReturnable<ItemStack> cir) {
        if (player != null && ServerFavoriteService.shouldPreventSlotPickup((Slot) (Object) this, player)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "safeInsert(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardLockedSlotSafeInsert(ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (shouldPreventSlotWrite(stack)) {
            cir.setReturnValue(stack);
        }
    }

    @Inject(method = "safeInsert(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardLockedSlotSafeInsertWithLimit(ItemStack stack, int increment, CallbackInfoReturnable<ItemStack> cir) {
        if (shouldPreventSlotWrite(stack)) {
            cir.setReturnValue(stack);
        }
    }

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardLockedSlotSet(ItemStack stack, CallbackInfo ci) {
        if (shouldPreventSlotWrite(stack)) {
            ci.cancel();
        }
    }

    @Inject(method = "setByPlayer(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardLockedSlotSetByPlayer(ItemStack stack, CallbackInfo ci) {
        if (shouldPreventSlotWrite(stack)) {
            ci.cancel();
        }
    }

    @Inject(method = "setByPlayer(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardLockedSlotSetByPlayerWithPrevious(ItemStack stack, ItemStack previousStack, CallbackInfo ci) {
        if (shouldPreventSlotWrite(stack)) {
            ci.cancel();
        }
    }

    private boolean shouldPreventSlotRemoval() {
        return container instanceof Inventory inventory
            && ServerFavoriteService.shouldPreventInventoryRemove(inventory, ((Slot) (Object) this).getContainerSlot());
    }

    private boolean shouldPreventSlotWrite(ItemStack stack) {
        return container instanceof Inventory inventory
            && ServerFavoriteService.shouldPreventInventorySet(inventory, ((Slot) (Object) this).getContainerSlot(), stack);
    }
}
