package mycraft.yuyears.neofavoriteitems.neoforge.mixin.compat;

import mycraft.yuyears.neofavoriteitems.application.ClientSortCompatService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "dev.terminalmc.clientsort.network.handler.validate.SchemaValidator", remap = false)
public abstract class ClientSortSchemaValidatorCompatMixin {
    @ModifyVariable(method = "validateSlotArray", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int[] neoFavoriteItems$filterCollectLockedSlots(int[] slotIds, ServerPlayer player, AbstractContainerMenu menu) {
        return ClientSortCompatService.filterCollectSlotIds(menu, player, slotIds);
    }

    @ModifyVariable(method = "validateSlotMapping", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int[] neoFavoriteItems$stabilizeSortLockedSlots(int[] slotMapping, ServerPlayer player, AbstractContainerMenu menu) {
        return ClientSortCompatService.stabilizeSortMapping(menu, player, slotMapping);
    }
}
