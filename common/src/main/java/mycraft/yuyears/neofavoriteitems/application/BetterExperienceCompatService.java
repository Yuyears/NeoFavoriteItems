package mycraft.yuyears.neofavoriteitems.application;

import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class BetterExperienceCompatService {
    private BetterExperienceCompatService() {}

    public static boolean shouldSkipPlayerInventorySource(Inventory inventory, ItemStack sourceStack) {
        if (inventory == null || sourceStack == null || sourceStack.isEmpty()) {
            return false;
        }

        int inventoryIndex = findIdentityIndex(inventory.items, sourceStack);
        return inventoryIndex >= 0
            && ServerFavoriteService.shouldProtectInventorySlotForExternalMove(inventory, inventoryIndex);
    }

    static <T> int findIdentityIndex(List<T> values, T candidate) {
        if (values == null || candidate == null) {
            return -1;
        }

        for (int index = 0; index < values.size(); index++) {
            if (values.get(index) == candidate) {
                return index;
            }
        }
        return -1;
    }
}
