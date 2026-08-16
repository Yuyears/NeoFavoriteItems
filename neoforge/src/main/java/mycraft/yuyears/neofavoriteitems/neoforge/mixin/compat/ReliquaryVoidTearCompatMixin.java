package mycraft.yuyears.neofavoriteitems.neoforge.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mycraft.yuyears.neofavoriteitems.application.ReliquaryCompatService;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "reliquary.item.VoidTearItem", remap = false)
public abstract class ReliquaryVoidTearCompatMixin {
    @ModifyExpressionValue(
        method = "fillTear",
        at = @At(
            value = "INVOKE",
            target = "Lreliquary/item/VoidTearItem;getKeepQuantity(Lnet/minecraft/world/item/ItemStack;)I"
        )
    )
    private int neoFavoriteItems$preserveLockedQuantity(
        int keep,
        ItemStack voidTear,
        Player player,
        ItemStack contents
    ) {
        Inventory inventory = player.getInventory();
        int total = 0;
        int unlockedMatching = 0;
        boolean hasLockedMatch = false;
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (!ItemStack.isSameItemSameComponents(contents, stack)) {
                continue;
            }
            total += stack.getCount();
            if (ServerFavoriteService.shouldPreventInventoryRemove(inventory, slot)) {
                hasLockedMatch = true;
            } else {
                unlockedMatching += stack.getCount();
            }
        }
        return hasLockedMatch
            ? ReliquaryCompatService.effectiveKeepQuantity(
                total,
                keep,
                unlockedMatching
            )
            : keep;
    }

    @WrapOperation(
        method = "fillTear",
        at = @At(
            value = "INVOKE",
            target = "Lreliquary/util/InventoryHelper;consumeItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;II)Z"
        )
    )
    private boolean neoFavoriteItems$consumeOnlyUnlockedItems(
        ItemStack target,
        Player player,
        int minCount,
        int requested,
        Operation<Boolean> original
    ) {
        if (player == null || player.isCreative() || !hasLockedMatch(player.getInventory(), target)) {
            return original.call(target, player, minCount, requested);
        }

        Inventory inventory = player.getInventory();
        int remaining = requested;
        for (int slot = 0; slot < inventory.items.size() && remaining > 0; slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (ItemStack.isSameItemSameComponents(target, stack)
                && !ServerFavoriteService.shouldPreventInventoryRemove(inventory, slot)) {
                int removed = Math.min(stack.getCount(), remaining);
                stack.shrink(removed);
                remaining -= removed;
            }
        }
        if (remaining != requested) {
            inventory.setChanged();
        }
        return remaining == 0;
    }

    private static boolean hasLockedMatch(Inventory inventory, ItemStack target) {
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (ItemStack.isSameItemSameComponents(target, inventory.items.get(slot))
                && ServerFavoriteService.shouldPreventInventoryRemove(inventory, slot)) {
                return true;
            }
        }
        return false;
    }
}
