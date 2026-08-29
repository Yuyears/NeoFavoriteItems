package mycraft.yuyears.neofavoriteitems.render;

import java.util.Optional;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Resolves HUD item draw calls to player inventory slots without assuming HUD geometry. */
public final class HudSlotProbe {
    private static final ThreadLocal<Integer> GUI_SLOT_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final Set<Integer> OBSERVED_SLOTS = new HashSet<>();
    private static final Map<Integer, Long> OBSERVED_POSITIONS = new HashMap<>();

    private HudSlotProbe() {}

    public static Optional<LogicalSlotIndex> resolve(Player player, ItemStack renderedStack) {
        if (player == null || renderedStack == null || renderedStack.isEmpty()) return Optional.empty();
        return resolve(player.getInventory().items, renderedStack);
    }

    static Optional<LogicalSlotIndex> resolve(List<ItemStack> inventory, ItemStack renderedStack) {
        if (renderedStack == null || renderedStack.isEmpty()) return Optional.empty();
        int found = resolveIdentity(inventory, renderedStack, 36);
        return found < 0 ? Optional.empty() : Optional.of(LogicalSlotIndex.of(found));
    }

    static int resolveIdentity(List<?> inventory, Object renderedStack, int limit) {
        int found = -1;
        for (int slot = 0; slot < Math.min(limit, inventory.size()); slot++) {
            if (inventory.get(slot) != renderedStack) continue;
            if (found >= 0) return -1;
            found = slot;
        }
        return found;
    }

    public static void enterGuiSlot() {
        GUI_SLOT_DEPTH.set(GUI_SLOT_DEPTH.get() + 1);
    }

    public static void exitGuiSlot() {
        int depth = GUI_SLOT_DEPTH.get() - 1;
        if (depth <= 0) GUI_SLOT_DEPTH.remove();
        else GUI_SLOT_DEPTH.set(depth);
    }

    public static boolean insideGuiSlot() {
        return GUI_SLOT_DEPTH.get() > 0;
    }

    public static void markObserved(LogicalSlotIndex slot) {
        OBSERVED_SLOTS.add(slot.value());
    }

    public static void markObserved(LogicalSlotIndex slot, int x, int y) {
        OBSERVED_SLOTS.add(slot.value());
        OBSERVED_POSITIONS.put(slot.value(), position(x, y));
    }

    public static boolean wasObserved(LogicalSlotIndex slot) {
        return OBSERVED_SLOTS.contains(slot.value());
    }

    public static boolean wasObserved(LogicalSlotIndex slot, int x, int y) {
        return Long.valueOf(position(x, y)).equals(OBSERVED_POSITIONS.get(slot.value()));
    }

    public static void clearObserved() {
        OBSERVED_SLOTS.clear();
        OBSERVED_POSITIONS.clear();
    }

    private static long position(int x, int y) {
        return ((long) x << 32) ^ (y & 0xFFFFFFFFL);
    }
}
