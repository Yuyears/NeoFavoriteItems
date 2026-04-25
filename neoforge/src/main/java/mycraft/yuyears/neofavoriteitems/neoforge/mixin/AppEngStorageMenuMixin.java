package mycraft.yuyears.neofavoriteitems.neoforge.mixin;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import mycraft.yuyears.neofavoriteitems.common.util.ReflectionHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "appeng.menu.me.common.MEStorageMenu", remap = false)
public abstract class AppEngStorageMenuMixin {
    @Inject(method = "handleNetworkInteraction", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardAppEngMoveRegionTargets(
        ServerPlayer player,
        @Coerce Object clickedKey,
        @Coerce Object action,
        CallbackInfo ci
    ) {
        if (player == null || !isMoveRegion(action)) {
            return;
        }

        ItemStack incomingStack = toItemStack(clickedKey);
        if (incomingStack.isEmpty()) {
            return;
        }

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        for (Slot slot : menu.slots) {
            if (isBlockedPlayerTarget(player, slot, incomingStack)) {
                DebugLogger.debug(
                    "AE2 network move-region canceled at locked target: player={} inventoryIndex={} item={}",
                    player.getName().getString(),
                    slot.getContainerSlot(),
                    incomingStack.getHoverName().getString()
                );
                ci.cancel();
                return;
            }
        }
    }

    private static boolean isMoveRegion(Object action) {
        return action instanceof Enum<?> enumAction && "MOVE_REGION".equals(enumAction.name());
    }

    private static ItemStack toItemStack(Object clickedKey) {
        if (clickedKey == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = ReflectionHelper.invokeMethod(clickedKey, "toStack", ItemStack.class);
        if (stack != null) {
            return stack;
        }

        stack = ReflectionHelper.invokeMethod(clickedKey, "getReadOnlyStack", ItemStack.class);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    private static boolean isBlockedPlayerTarget(ServerPlayer player, Slot slot, ItemStack incomingStack) {
        if (!(slot.container instanceof Inventory inventory) || inventory != player.getInventory()) {
            return false;
        }
        if (!canStackIntoSlotIgnoringMayPlace(slot, incomingStack)) {
            return false;
        }
        return ServerFavoriteService.shouldPreventInventoryReceive(inventory, slot.getContainerSlot(), incomingStack);
    }

    private static boolean canStackIntoSlotIgnoringMayPlace(Slot slot, ItemStack incomingStack) {
        ItemStack currentStack = slot.getItem();
        return currentStack.isEmpty()
            || ItemStack.isSameItemSameComponents(currentStack, incomingStack)
                && currentStack.getCount() < slot.getMaxStackSize(currentStack);
    }
}
