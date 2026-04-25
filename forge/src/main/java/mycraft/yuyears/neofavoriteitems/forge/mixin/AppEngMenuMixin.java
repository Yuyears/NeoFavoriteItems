package mycraft.yuyears.neofavoriteitems.forge.mixin;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "appeng.menu.AEBaseMenu", remap = false)
public abstract class AppEngMenuMixin {
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void neoFavoriteItems$guardAppEngPlayerQuickMove(Player player, int slotId, CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (slotId < 0 || slotId >= menu.slots.size()) {
            return;
        }

        Slot slot = menu.getSlot(slotId);
        if (player != null && ServerFavoriteService.shouldPreventSlotPickup(slot, player)) {
            DebugLogger.debug(
                "AE2 quick move canceled at player slot source: player={} slotId={} inventoryIndex={}",
                player.getName().getString(),
                slotId,
                slot.getContainerSlot()
            );
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
