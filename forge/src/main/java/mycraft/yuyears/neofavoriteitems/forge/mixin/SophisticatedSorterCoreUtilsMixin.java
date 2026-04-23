package mycraft.yuyears.neofavoriteitems.forge.mixin;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(targets = "com.sighs.sophisticatedsorter.utils.CoreUtils", remap = false)
public abstract class SophisticatedSorterCoreUtilsMixin {
    @Inject(method = "sortInventory", at = @At("HEAD"), cancellable = true, remap = false)
    private static void neoFavoriteItems$sortInventorySkippingLockedSlots(ServerPlayer player, @Coerce Object sortBy, boolean usePinyin, CallbackInfo ci) {
        Inventory inventory = player.getInventory();
        List<Integer> sortableSlots = new ArrayList<>();
        for (int inventoryIndex = 9; inventoryIndex < 36; inventoryIndex++) {
            if (!ServerFavoriteService.shouldProtectInventorySlotForExternalMove(inventory, inventoryIndex)) {
                sortableSlots.add(inventoryIndex);
            }
        }

        ItemStackHandler sortableHandler = new ItemStackHandler(sortableSlots.size());
        for (int i = 0; i < sortableSlots.size(); i++) {
            sortableHandler.setStackInSlot(i, inventory.getItem(sortableSlots.get(i)).copy());
        }

        if (!sortHandler(sortableHandler, sortBy, usePinyin)) {
            return;
        }

        for (int i = 0; i < sortableSlots.size(); i++) {
            inventory.setItem(sortableSlots.get(i), sortableHandler.getStackInSlot(i));
        }
        DebugLogger.debug("SophisticatedSorter compat sorted inventory while preserving locked slots: player={} sortableSlots={}", player.getName().getString(), sortableSlots.size());
        ci.cancel();
    }

    @Inject(method = "transfer", at = @At("HEAD"), cancellable = true, remap = false)
    private static void neoFavoriteItems$transferUsingMenuSlotIds(Player player, boolean transferToContainer, boolean filter, CallbackInfo ci) {
        AbstractContainerMenu menu = player.containerMenu;
        List<Slot> sourceSlots = new ArrayList<>();
        Set<Item> targetItems = new HashSet<>();

        for (Slot slot : menu.slots) {
            boolean playerInventorySlot = slot.container instanceof Inventory && slot.container == player.getInventory();
            if (transferToContainer == playerInventorySlot) {
                sourceSlots.add(slot);
            } else if (slot.hasItem()) {
                targetItems.add(slot.getItem().getItem());
            }
        }

        if (transferToContainer && sourceSlots.size() > 9) {
            sourceSlots.subList(sourceSlots.size() - 9, sourceSlots.size()).clear();
        }

        for (Slot slot : sourceSlots) {
            if (!slot.hasItem()) {
                continue;
            }
            if (filter && !targetItems.contains(slot.getItem().getItem())) {
                continue;
            }
            if (slot.container instanceof Inventory inventory
                && ServerFavoriteService.shouldProtectInventorySlotForExternalMove(inventory, slot.getContainerSlot())) {
                continue;
            }

            int menuSlotId = menu.slots.indexOf(slot);
            if (menuSlotId >= 0) {
                menu.quickMoveStack(player, menuSlotId);
            }
        }

        DebugLogger.debug("SophisticatedSorter compat transferred items while preserving locked slots: player={} transferToContainer={} sources={}", player.getName().getString(), transferToContainer, sourceSlots.size());
        ci.cancel();
    }

    private static boolean sortHandler(IItemHandlerModifiable handler, Object sortBy, boolean usePinyin) {
        try {
            Class<?> coreUtilsClass = Class.forName("com.sighs.sophisticatedsorter.utils.CoreUtils");
            Method getComparator = coreUtilsClass.getMethod("getComparator", sortBy.getClass(), boolean.class);
            Comparator<?> comparator = (Comparator<?>) getComparator.invoke(null, sortBy, usePinyin);
            Class<?> inventorySorterClass = Class.forName("net.p3pp3rf1y.sophisticatedcore.util.InventorySorter");
            Method sortHandler = inventorySorterClass.getMethod("sortHandler", IItemHandlerModifiable.class, Comparator.class, Set.class);
            sortHandler.invoke(null, handler, comparator, Set.of());
            return true;
        } catch (ReflectiveOperationException exception) {
            DebugLogger.debug("SophisticatedSorter compat failed to sort inventory: {}", exception.toString());
            return false;
        }
    }
}
