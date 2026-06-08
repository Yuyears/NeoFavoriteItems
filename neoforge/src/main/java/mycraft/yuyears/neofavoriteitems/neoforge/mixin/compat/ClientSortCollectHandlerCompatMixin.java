package mycraft.yuyears.neofavoriteitems.neoforge.mixin.compat;

import mycraft.yuyears.neofavoriteitems.application.ClientSortCompatService;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "dev.terminalmc.clientsort.network.handler.CollectHandler", remap = false)
public abstract class ClientSortCollectHandlerCompatMixin {
    @ModifyVariable(method = "collect", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int[] neoFavoriteItems$filterLockedSlots(int[] slotIds, net.minecraft.server.MinecraftServer server, AbstractContainerMenu menu) {
        return ClientSortCompatService.filterCollectSlotIds(menu, slotIds);
    }
}
