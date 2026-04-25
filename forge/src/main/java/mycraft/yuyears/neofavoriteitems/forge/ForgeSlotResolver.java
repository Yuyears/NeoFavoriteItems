package mycraft.yuyears.neofavoriteitems.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;

public final class ForgeSlotResolver {
    private ForgeSlotResolver() {}

    public static boolean isPlayerInventorySlot(Slot slot) {
        var player = Minecraft.getInstance().player;
        return player != null && slot != null && slot.container == player.getInventory();
    }

    public static int getPlayerInventoryIndex(Slot slot) {
        return slot.getContainerSlot();
    }

    public static boolean hasItem(Slot slot) {
        return slot.hasItem();
    }
}
