package mycraft.yuyears.neofavoriteitems.mixin;

import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$serverGuardFavoriteSlot(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if (player == null) {
            return;
        }
        ServerFavoriteService.beginAuthorizedInstantSwapClick(
            (AbstractContainerMenu) (Object) this,
            player,
            slotId,
            button,
            clickType
        );
        if (ServerFavoriteService.shouldCancelMenuClick((AbstractContainerMenu) (Object) this, player, slotId, button, clickType)) {
            ci.cancel();
        }
    }

    @Inject(method = "clicked", at = @At("RETURN"))
    private void neoFavoriteItems$finishAuthorizedInstantSwapClick(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        ServerFavoriteService.finishAuthorizedInstantSwapClick(player);
    }

    @Redirect(
        method = "moveItemStackTo",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I",
            ordinal = 0
        )
    )
    private int neoFavoriteItems$skipLockedOccupiedMergeTarget(Slot slot, ItemStack incomingStack) {
        return ServerFavoriteService.shouldPreventInPlaceSlotMerge(slot, incomingStack)
            ? slot.getItem().getCount()
            : slot.getMaxStackSize(incomingStack);
    }
}
