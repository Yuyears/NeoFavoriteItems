package mycraft.yuyears.neofavoriteitems.application;

import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.common.util.ReflectionHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Common policy boundary used by small, optional third-party compatibility hooks. */
public final class ExternalInventoryGuard {
    private ExternalInventoryGuard() {}

    public static MutationDecision beforeMutation(InventoryMutationContext context) {
        if (context == null || context.player() == null || context.inventoryIndex() < 0
            || context.inventoryIndex() >= context.player().getInventory().getContainerSize()) {
            return MutationDecision.ALLOW;
        }
        Inventory inventory = context.player().getInventory();
        boolean denied = context.direction() == InventoryMutationContext.Direction.EXTRACT
            ? ServerFavoriteService.shouldPreventInventoryRemove(inventory, context.inventoryIndex())
            : ServerFavoriteService.shouldPreventInventoryReceive(inventory, context.inventoryIndex(), context.stack());
        return denied ? MutationDecision.DENY : MutationDecision.ALLOW;
    }

    public static boolean isFavoriteSlot(Player player, int inventoryIndex) {
        if (player == null || inventoryIndex < 0 || inventoryIndex >= player.getInventory().getContainerSize()) return false;
        FavoritesManager.getStateService().setPlayer(player.getUUID());
        return FavoritesManager.getInstance().isSlotFavorite(inventoryIndex);
    }

    public static boolean deniesMoveRegionTarget(ServerPlayer player, AbstractContainerMenu menu, ItemStack incoming) {
        if (player == null || menu == null || incoming == null || incoming.isEmpty()) return false;
        for (Slot slot : menu.slots) {
            if (slot.container != player.getInventory() || !canReceive(slot, incoming)) continue;
            InventoryMutationContext context = new InventoryMutationContext(
                player, slot.getContainerSlot(), incoming,
                InventoryMutationContext.Direction.INSERT,
                InventoryMutationContext.Source.EXTERNAL_TRANSFER
            );
            if (beforeMutation(context) == MutationDecision.DENY) return true;
        }
        return false;
    }

    public static ItemStack externalKeyStack(Object key) {
        if (key == null) return ItemStack.EMPTY;
        ItemStack stack = ReflectionHelper.invokeMethod(key, "toStack", ItemStack.class);
        if (stack == null) stack = ReflectionHelper.invokeMethod(key, "getReadOnlyStack", ItemStack.class);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    private static boolean canReceive(Slot slot, ItemStack incoming) {
        ItemStack current = slot.getItem();
        return current.isEmpty() || ItemStack.isSameItemSameComponents(current, incoming)
            && current.getCount() < slot.getMaxStackSize(current);
    }
}
