package mycraft.yuyears.neofavoriteitems.forge.mixin.compat;

import mycraft.yuyears.neofavoriteitems.application.ClientSortCompatService;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "dev.terminalmc.clientsort.network.handler.SortHandler", remap = false)
public abstract class ClientSortSortHandlerCompatMixin {
    @ModifyVariable(method = "sort", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int[] neoFavoriteItems$stabilizeLockedSlots(int[] slotMapping, net.minecraft.server.MinecraftServer server, AbstractContainerMenu menu) {
        return ClientSortCompatService.stabilizeSortMapping(menu, slotMapping);
    }
}
