package mycraft.yuyears.neofavoriteitems.neoforge.mixin.compat;

import it.unimi.dsi.fastutil.ints.IntList;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.application.InventorySortingCompatService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "invtweaks.util.Sorting", remap = false)
public abstract class InventoryTweaksSortingCompatMixin {
    @ModifyVariable(
        method = "playerSortServer",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0,
        remap = false
    )
    private static IntList neoFavoriteItems$lockFavoriteSlotsForPlayerSort(
        IntList lockedSlots,
        ServerPlayer player,
        @Coerce Object cats,
        @Coerce Object rules
    ) {
        if (player == null || lockedSlots == null) {
            return lockedSlots;
        }

        int originalLockCount = lockedSlots.size();
        int addedLockCount = 0;
        for (int favoriteSlot : InventorySortingCompatService.favoriteSlotsForPlayerSort(player.getInventory(), 0, player.getInventory().items.size())) {
            if (!lockedSlots.contains(favoriteSlot)) {
                lockedSlots.add(favoriteSlot);
                addedLockCount++;
            }
        }
        lockedSlots.sort(null);
        DebugLogger.debug(
            "Inventory Tweaks ReFoxed compat augmented player sort locks: player={} originalLocks={} addedFavoriteLocks={} totalLocks={}",
            player.getName().getString(),
            originalLockCount,
            addedLockCount,
            lockedSlots.size()
        );
        return lockedSlots;
    }
}
