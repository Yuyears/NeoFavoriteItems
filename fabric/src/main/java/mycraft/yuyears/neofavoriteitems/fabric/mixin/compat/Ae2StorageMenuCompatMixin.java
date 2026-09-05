package mycraft.yuyears.neofavoriteitems.fabric.mixin.compat;

import mycraft.yuyears.neofavoriteitems.application.ExternalInventoryGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "appeng.menu.me.common.MEStorageMenu", remap = false)
public abstract class Ae2StorageMenuCompatMixin {
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

        ItemStack incomingStack = ExternalInventoryGuard.externalKeyStack(clickedKey);
        if (ExternalInventoryGuard.deniesMoveRegionTarget(player, (AbstractContainerMenu) (Object) this, incomingStack)) ci.cancel();
    }

    private static boolean isMoveRegion(Object action) {
        return action instanceof Enum<?> enumAction && "MOVE_REGION".equals(enumAction.name());
    }

}
