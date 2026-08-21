package mycraft.yuyears.neofavoriteitems.application;

import net.minecraft.world.inventory.ClickType;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

public final class InstantSwapCompatService {
    private InstantSwapCompatService() {}

    public static ModifierMode resolveModifierMode(boolean bypassHeld, boolean lockHeld, ModifierMode lastPressed) {
        if (bypassHeld && lockHeld) {
            return lastPressed == null ? ModifierMode.BYPASS : lastPressed;
        }
        if (lockHeld) {
            return ModifierMode.MOVE_LOCKS;
        }
        if (bypassHeld) {
            return ModifierMode.BYPASS;
        }
        return ModifierMode.NONE;
    }

    public static boolean isModifierActive(boolean held, long nowNanos, long releasedAtNanos, long debounceNanos) {
        return held || releasedAtNanos > 0L
            && nowNanos >= releasedAtNanos
            && nowNanos - releasedAtNanos <= debounceNanos;
    }

    public static Decision decide(boolean firstLocked, boolean secondLocked, boolean bypassHeld, boolean lockKeyHeld) {
        if (lockKeyHeld) {
            return Decision.SWAP_WITH_LOCKS;
        }
        if (bypassHeld || (!firstLocked && !secondLocked)) {
            return Decision.PASS;
        }
        return Decision.BLOCK;
    }

    public static boolean isHotbarMainInventoryPair(int firstSlot, int secondSlot) {
        return isHotbarSlot(firstSlot) && isMainInventorySlot(secondSlot)
            || isHotbarSlot(secondSlot) && isMainInventorySlot(firstSlot);
    }

    public static Set<Integer> moveFavoriteSlots(Set<Integer> currentFavorites, int... slotCycle) {
        Set<Integer> result = new HashSet<>(currentFavorites);
        if (slotCycle == null || slotCycle.length < 2) {
            return result;
        }

        for (int slot : slotCycle) {
            if (slot >= 0) {
                result.remove(slot);
            }
        }
        for (int i = 0; i < slotCycle.length; i++) {
            int source = slotCycle[i];
            int target = slotCycle[(i + 1) % slotCycle.length];
            if (source >= 0 && target >= 0 && currentFavorites.contains(source)) {
                result.add(target);
            }
        }
        return result;
    }

    public static Set<Integer> moveFavoritePairs(Set<Integer> currentFavorites, int... slotPairs) {
        Set<Integer> result = new HashSet<>(currentFavorites);
        if (slotPairs == null || slotPairs.length % 2 != 0) {
            return result;
        }
        for (int i = 0; i < slotPairs.length; i += 2) {
            int first = slotPairs[i];
            int second = slotPairs[i + 1];
            if (first < 0 || second < 0 || first == second) {
                continue;
            }
            boolean firstFavorite = currentFavorites.contains(first);
            boolean secondFavorite = currentFavorites.contains(second);
            if (firstFavorite != secondFavorite) {
                if (firstFavorite) {
                    result.remove(first);
                    result.add(second);
                } else {
                    result.remove(second);
                    result.add(first);
                }
            }
        }
        return result;
    }

    public static boolean areHotbarMainInventoryPairs(int... slotPairs) {
        if (slotPairs == null || slotPairs.length == 0 || slotPairs.length > 18 || slotPairs.length % 2 != 0) {
            return false;
        }
        for (int i = 0; i < slotPairs.length; i += 2) {
            if (!isHotbarMainInventoryPair(slotPairs[i], slotPairs[i + 1])) {
                return false;
            }
        }
        return true;
    }

    static int[] pickupExchangeSlots(int targetSlot, int hotbarSlot, boolean targetHasItem, boolean hotbarHasItem) {
        if (targetHasItem && hotbarHasItem) {
            return new int[]{targetSlot, hotbarSlot, targetSlot};
        }
        if (targetHasItem) {
            return new int[]{targetSlot, hotbarSlot};
        }
        if (hotbarHasItem) {
            return new int[]{hotbarSlot, targetSlot};
        }
        return new int[0];
    }

    static boolean containsFavoriteSlot(Set<Integer> favoriteSlots, int... slots) {
        for (int slot : slots) {
            if (slot >= 0 && favoriteSlots.contains(slot)) {
                return true;
            }
        }
        return false;
    }

    static Optional<OperationPlan> createPlan(
        Operation operation,
        int targetMenuSlot,
        int targetInventorySlot,
        int hotbarMenuSlot,
        int hotbarInventorySlot,
        int auxiliaryMenuSlot,
        int auxiliaryInventorySlot,
        boolean targetHasItem,
        boolean hotbarHasItem
    ) {
        if (operation == null || targetMenuSlot < 0 || hotbarMenuSlot < 0
            || hotbarInventorySlot < 0 || hotbarInventorySlot > 8) {
            return Optional.empty();
        }

        return switch (operation) {
            case SWAP -> Optional.of(new OperationPlan(
                List.of(new PlannedClick(targetMenuSlot, hotbarInventorySlot, ClickType.SWAP)),
                List.of(targetInventorySlot, hotbarInventorySlot),
                List.of(targetMenuSlot, hotbarMenuSlot)
            ));
            case PICKUP_EXCHANGE -> {
                int[] slots = pickupExchangeSlots(targetMenuSlot, hotbarMenuSlot, targetHasItem, hotbarHasItem);
                if (slots.length == 0) {
                    yield Optional.empty();
                }
                yield Optional.of(new OperationPlan(
                    java.util.Arrays.stream(slots)
                        .mapToObj(slot -> new PlannedClick(slot, 0, ClickType.PICKUP))
                        .toList(),
                    List.of(targetInventorySlot, hotbarInventorySlot),
                    List.of(targetMenuSlot, hotbarMenuSlot)
                ));
            }
            case HOTBAR_STASH -> {
                if (auxiliaryMenuSlot < 0 || auxiliaryInventorySlot < 0 || auxiliaryInventorySlot > 8
                    || auxiliaryInventorySlot == hotbarInventorySlot || !targetHasItem || !hotbarHasItem) {
                    yield Optional.empty();
                }
                yield Optional.of(new OperationPlan(
                    List.of(
                        new PlannedClick(hotbarMenuSlot, 0, ClickType.PICKUP),
                        new PlannedClick(auxiliaryMenuSlot, 0, ClickType.PICKUP),
                        new PlannedClick(targetMenuSlot, 0, ClickType.PICKUP),
                        new PlannedClick(hotbarMenuSlot, 0, ClickType.PICKUP)
                    ),
                    List.of(targetInventorySlot, hotbarInventorySlot, auxiliaryInventorySlot),
                    List.of(targetMenuSlot, hotbarMenuSlot, auxiliaryMenuSlot)
                ));
            }
        };
    }

    public static <T> boolean contentFollowsCycle(List<T> before, List<T> after, BiPredicate<T, T> matches) {
        if (before == null || after == null || matches == null || before.size() < 2 || before.size() != after.size()) {
            return false;
        }
        for (int source = 0; source < before.size(); source++) {
            int target = (source + 1) % before.size();
            if (!matches.test(before.get(source), after.get(target))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot < 9;
    }

    private static boolean isMainInventorySlot(int slot) {
        return slot >= 9 && slot < 36;
    }

    public enum Decision {
        PASS,
        BLOCK,
        SWAP_WITH_LOCKS,
        SERVER_HANDLED
    }

    public enum ModifierMode {
        NONE,
        BYPASS,
        MOVE_LOCKS
    }

    public enum Operation {
        SWAP,
        PICKUP_EXCHANGE,
        HOTBAR_STASH
    }

    public record PlannedClick(int slotId, int button, ClickType clickType) {
        boolean matches(int actualSlotId, int actualButton, ClickType actualClickType) {
            return slotId == actualSlotId && button == actualButton && clickType == actualClickType;
        }
    }

    public record OperationPlan(
        List<PlannedClick> clicks,
        List<Integer> favoriteCycle,
        List<Integer> menuSlotCycle
    ) {
        public OperationPlan {
            clicks = List.copyOf(clicks);
            favoriteCycle = List.copyOf(favoriteCycle);
            menuSlotCycle = List.copyOf(menuSlotCycle);
        }
    }
}
