package mycraft.yuyears.neofavoriteitems.forge.mixin;

import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper", remap = false)
public abstract class SophisticatedCoreInventoryHelperMixin {
    @Inject(method = "mergeIntoPlayerInventory", at = @At("HEAD"), cancellable = true, remap = false)
    private static void neoFavoriteItems$mergeIntoPlayerInventorySkippingLockedSlots(
        Player player,
        ItemStack stack,
        int startSlot,
        CallbackInfoReturnable<ItemStack> cir
    ) {
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack remaining = stack.copy();
        List<Integer> emptySlots = new ArrayList<>();

        for (int slot = startSlot; slot < inventory.items.size(); slot++) {
            ItemStack slotStack = inventory.getItem(slot);
            if (slotStack.isEmpty()) {
                if (!ServerFavoriteService.shouldPreventInventoryReceive(inventory, slot, remaining)) {
                    emptySlots.add(slot);
                }
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(slotStack, remaining)) {
                continue;
            }

            int amountToMove = Math.min(slotStack.getMaxStackSize() - slotStack.getCount(), remaining.getCount());
            if (amountToMove <= 0) {
                continue;
            }

            ItemStack expectedStack = slotStack.copy();
            expectedStack.grow(amountToMove);
            if (ServerFavoriteService.shouldPreventInventorySet(inventory, slot, expectedStack)) {
                continue;
            }

            slotStack.grow(amountToMove);
            remaining.shrink(amountToMove);
            if (remaining.isEmpty()) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
        }

        for (int slot : emptySlots) {
            ItemStack insertedStack = remaining.copy();
            insertedStack.setCount(Math.min(insertedStack.getMaxStackSize(), remaining.getCount()));
            if (ServerFavoriteService.shouldPreventInventorySet(inventory, slot, insertedStack)) {
                continue;
            }

            inventory.setItem(slot, insertedStack);
            remaining.shrink(insertedStack.getCount());
            if (remaining.isEmpty()) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
        }

        cir.setReturnValue(remaining);
    }
}
