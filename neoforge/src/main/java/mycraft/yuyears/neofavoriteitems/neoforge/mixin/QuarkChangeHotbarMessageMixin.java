package mycraft.yuyears.neofavoriteitems.neoforge.mixin;

import mycraft.yuyears.neofavoriteitems.application.InventorySortingCompatService;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeFavoriteNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "org.violetmoon.quark.base.network.message.ChangeHotbarMessage", remap = false)
public abstract class QuarkChangeHotbarMessageMixin {
    @Inject(
        method = "swap(Lnet/minecraft/world/Container;II)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void neoFavoriteItems$swapQuarkHotbarWithFavoriteState(
        Container container,
        int slot1,
        int slot2,
        CallbackInfo ci
    ) {
        if (!(container instanceof Inventory inventory) || !(inventory.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ServerFavoriteService.runWithInventoryGuardsBypassed(serverPlayer, () -> {
            ItemStack stack1 = inventory.getItem(slot1);
            ItemStack stack2 = inventory.getItem(slot2);
            inventory.setItem(slot2, stack1);
            inventory.setItem(slot1, stack2);
        });

        if (InventorySortingCompatService.applyHotbarSwapFavoriteState(inventory, slot1, slot2)) {
            NeoForgeFavoriteNetworking.sendFullSync(serverPlayer);
        }
        ci.cancel();
    }
}
