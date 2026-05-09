package mycraft.yuyears.neofavoriteitems.neoforge.mixin;

import mycraft.yuyears.neofavoriteitems.application.InventorySortingCompatService;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "org.violetmoon.quark.base.handler.SortingHandler", remap = false)
public abstract class QuarkSortingHandlerMixin {
    @ModifyVariable(
        method = "sortInventory(Lnet/minecraft/world/Container;II[I)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private static int[] neoFavoriteItems$lockFavoriteSlotsForQuarkSort(
        int[] lockedSlots,
        Container container,
        int startInclusive,
        int endExclusive
    ) {
        return InventorySortingCompatService.augmentLockedSlots(container, startInclusive, endExclusive, lockedSlots);
    }
}
