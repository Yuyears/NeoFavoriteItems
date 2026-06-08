package mycraft.yuyears.neofavoriteitems.forge.mixin;

import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Unique
    private Map<Integer, ItemStack> neoFavoriteItems$preservedDeathDropStacks;

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
        Inventory inventory = (Inventory) (Object) this;
        if (ServerFavoriteService.shouldRerouteInventorySet(inventory, inventoryIndex, stack)
            || ServerFavoriteService.shouldPreventInventorySet(inventory, inventoryIndex, stack)) {
            ci.cancel();
        }
    }

    @Inject(method = "getFreeSlot", at = @At("RETURN"), cancellable = true)
    private void neoFavoriteItems$skipLockedEmptySlotsForIncomingItems(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ServerFavoriteService.resolveFreeSlotForIncomingItem((Inventory) (Object) this, cir.getReturnValue()));
    }

    @Inject(method = "dropAll", at = @At("HEAD"))
    private void neoFavoriteItems$temporarilyRemoveLockedSlotsDuringPreservedDeath(CallbackInfo ci) {
        Inventory inventory = (Inventory) (Object) this;
        Map<Integer, ItemStack> preservedStacks = new HashMap<>();
        for (int inventoryIndex = 0; inventoryIndex < inventory.getContainerSize(); inventoryIndex++) {
            if (ServerFavoriteService.shouldPreserveInventorySlotOnDeath(inventory, inventoryIndex)) {
                preservedStacks.put(inventoryIndex, inventory.getItem(inventoryIndex).copy());
            }
        }
        if (preservedStacks.isEmpty()) {
            return;
        }

        neoFavoriteItems$preservedDeathDropStacks = preservedStacks;
        ServerFavoriteService.runWithInventoryGuardsBypassed(inventory.player, () -> preservedStacks.keySet()
            .forEach(inventoryIndex -> inventory.setItem(inventoryIndex, ItemStack.EMPTY)));
    }

    @Inject(method = "dropAll", at = @At("RETURN"))
    private void neoFavoriteItems$restoreLockedSlotsAfterPreservedDeathDrop(CallbackInfo ci) {
        Map<Integer, ItemStack> preservedStacks = neoFavoriteItems$preservedDeathDropStacks;
        if (preservedStacks == null || preservedStacks.isEmpty()) {
            return;
        }

        neoFavoriteItems$preservedDeathDropStacks = null;
        Inventory inventory = (Inventory) (Object) this;
        ServerFavoriteService.runWithInventoryGuardsBypassed(inventory.player, () -> preservedStacks
            .forEach((inventoryIndex, stack) -> inventory.setItem(inventoryIndex, stack.copy())));
    }
}
