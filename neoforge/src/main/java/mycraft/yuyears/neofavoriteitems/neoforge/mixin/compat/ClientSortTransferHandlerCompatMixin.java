package mycraft.yuyears.neofavoriteitems.neoforge.mixin.compat;

import mycraft.yuyears.neofavoriteitems.application.ClientSortCompatService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "dev.terminalmc.clientsort.network.handler.TransferHandler", remap = false)
public abstract class ClientSortTransferHandlerCompatMixin {
    @ModifyVariable(method = "transfer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int[] neoFavoriteItems$filterLockedSourceSlots(int[] srcSlotIds, MinecraftServer server, AbstractContainerMenu menu) {
        return ClientSortCompatService.filterTransferSourceSlotIds(menu, srcSlotIds);
    }

    @ModifyVariable(method = "transfer", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static int[] neoFavoriteItems$filterLockedTargetSlots(int[] dstSlotIds, MinecraftServer server, AbstractContainerMenu menu) {
        return ClientSortCompatService.filterTransferTargetSlotIds(menu, dstSlotIds);
    }
}
