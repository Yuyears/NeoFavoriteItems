package mycraft.yuyears.neofavoriteitems.fabric;

import mycraft.yuyears.neofavoriteitems.fabric.mixin.CreativeSlotWrapperAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;

public final class FabricSlotResolver {
    private FabricSlotResolver() {}

    public static Slot unwrapCreativeSlot(Slot slot) {
        if (slot instanceof CreativeSlotWrapperAccessor accessor) {
            return accessor.neoFavoriteItems$getTarget();
        }
        return slot;
    }

    public static boolean isPlayerInventorySlot(Slot slot) {
        var player = Minecraft.getInstance().player;
        return player != null && isPlayerInventorySlot(slot, player);
    }

    public static boolean isPlayerInventorySlot(Slot slot, net.minecraft.world.entity.player.Player player) {
        Slot resolvedSlot = unwrapCreativeSlot(slot);
        return resolvedSlot != null && resolvedSlot.container == player.getInventory();
    }

    public static int getPlayerInventoryIndex(Slot slot) {
        return unwrapCreativeSlot(slot).getContainerSlot();
    }

    public static boolean hasItem(Slot slot) {
        return unwrapCreativeSlot(slot).hasItem();
    }
}
