package mycraft.yuyears.neofavoriteitems.fabric.mixin.compat;

import mycraft.yuyears.neofavoriteitems.application.ClientSortCompatService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "dev.terminalmc.clientsort.network.handler.StackFillHandler", remap = false)
public abstract class ClientSortStackFillHandlerCompatMixin {
    @ModifyVariable(method = "fillStacks", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int[] neoFavoriteItems$filterLockedSourceSlots(int[] srcSlotIds, MinecraftServer server, AbstractContainerMenu menu) {
        return ClientSortCompatService.filterStackFillSourceSlotIds(menu, srcSlotIds);
    }

    @ModifyVariable(method = "fillStacks", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static int[] neoFavoriteItems$filterLockedTargetSlots(int[] dstSlotIds, MinecraftServer server, AbstractContainerMenu menu) {
        return ClientSortCompatService.filterStackFillTargetSlotIds(menu, dstSlotIds);
    }
}
