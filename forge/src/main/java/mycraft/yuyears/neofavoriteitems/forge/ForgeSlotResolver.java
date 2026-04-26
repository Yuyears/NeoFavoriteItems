package mycraft.yuyears.neofavoriteitems.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.RangedWrapper;

import java.lang.reflect.Field;

public final class ForgeSlotResolver {
    private ForgeSlotResolver() {}

    public static boolean isPlayerInventorySlot(Slot slot) {
        var player = Minecraft.getInstance().player;
        return player != null && getPlayerInventoryIndex(slot) >= 0;
    }

    public static int getPlayerInventoryIndex(Slot slot) {
        var player = Minecraft.getInstance().player;
        if (player == null || slot == null) {
            return -1;
        }
        return resolvePlayerInventoryIndex(slot, player.getInventory());
    }

    public static boolean hasItem(Slot slot) {
        return slot.hasItem();
    }

    public static int resolvePlayerInventoryIndex(Slot slot, Inventory inventory) {
        Slot resolvedSlot = unwrapSlot(slot);
        if (resolvedSlot == null || inventory == null) {
            return -1;
        }
        if (resolvedSlot.container == inventory) {
            return resolvedSlot.getContainerSlot();
        }
        if (resolvedSlot instanceof SlotItemHandler itemHandlerSlot) {
            return resolveHandlerInventoryIndex(
                itemHandlerSlot.getItemHandler(),
                itemHandlerSlot.getContainerSlot(),
                inventory
            );
        }
        return -1;
    }

    private static int resolveHandlerInventoryIndex(IItemHandler handler, int slot, Inventory inventory) {
        if (handler instanceof InvWrapper invWrapper) {
            Container container = invWrapper.getInv();
            return container == inventory ? slot : -1;
        }
        if (handler instanceof RangedWrapper rangedWrapper) {
            Object compose = readField(rangedWrapper, "compose");
            Integer minSlot = readIntField(rangedWrapper, "minSlot");
            if (compose instanceof IItemHandler nestedHandler && minSlot != null) {
                return resolveHandlerInventoryIndex(nestedHandler, minSlot + slot, inventory);
            }
        }
        return -1;
    }

    private static Slot unwrapSlot(Slot slot) {
        if (slot == null) {
            return null;
        }

        Class<?> type = slot.getClass();
        while (type != null && type != Slot.class) {
            for (Field field : type.getDeclaredFields()) {
                if (!Slot.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Slot wrappedSlot = (Slot) field.get(slot);
                    if (wrappedSlot != null && wrappedSlot != slot) {
                        return wrappedSlot;
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return slot;
    }

    private static Object readField(Object target, String name) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer readIntField(Object target, String name) {
        Object value = readField(target, name);
        return value instanceof Integer integer ? integer : null;
    }
}
