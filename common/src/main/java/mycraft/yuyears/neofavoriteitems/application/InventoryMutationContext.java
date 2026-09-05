package mycraft.yuyears.neofavoriteitems.application;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Loader-neutral input for optional inventory compatibility hooks. */
public record InventoryMutationContext(
    ServerPlayer player,
    int inventoryIndex,
    ItemStack stack,
    Direction direction,
    Source source
) {
    public enum Direction { EXTRACT, INSERT }
    public enum Source { EXTERNAL_TRANSFER, RECIPE_TRANSFER, AUTOMATION }
}
