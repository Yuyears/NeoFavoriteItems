package mycraft.yuyears.neofavoriteitems.neoforge.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "dev.polaris_light.wrenchfinder.logic.ItemLookupService", remap = false)
public abstract class WrenchFinderDirectMatchCompatMixin {
    @WrapOperation(
        method = "findDirectMatch",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private static ItemStack neoFavoriteItems$hideLockedInventoryCandidate(
        Inventory inventory,
        int slot,
        Operation<ItemStack> original
    ) {
        ItemStack candidate = original.call(inventory, slot);
        return ServerFavoriteService.shouldProtectInventorySlotForExternalMove(inventory, slot)
            ? ItemStack.EMPTY
            : candidate;
    }

    @WrapOperation(
        method = "findDirectMatch",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private static ItemStack neoFavoriteItems$hideLockedOffhandCandidate(
        ServerPlayer player,
        Operation<ItemStack> original
    ) {
        ItemStack candidate = original.call(player);
        return ServerFavoriteService.shouldProtectInventorySlotForExternalMove(
            player.getInventory(),
            Inventory.SLOT_OFFHAND
        ) ? ItemStack.EMPTY : candidate;
    }
}
