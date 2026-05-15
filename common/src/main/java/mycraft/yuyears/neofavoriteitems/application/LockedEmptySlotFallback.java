package mycraft.yuyears.neofavoriteitems.application;

import java.util.function.IntPredicate;

public final class LockedEmptySlotFallback {
    private LockedEmptySlotFallback() {}

    public static int findEmptyUnlockedFallbackSlot(IntPredicate isEmptySlot, IntPredicate isLockedTarget) {
        int hotbarSlot = findEmptyUnlockedSlot(0, 9, isEmptySlot, isLockedTarget);
        if (hotbarSlot >= 0) {
            return hotbarSlot;
        }
        return findEmptyUnlockedSlot(9, 36, isEmptySlot, isLockedTarget);
    }

    private static int findEmptyUnlockedSlot(int startInclusive, int endExclusive, IntPredicate isEmptySlot, IntPredicate isLockedTarget) {
        for (int slot = startInclusive; slot < endExclusive; slot++) {
            if (isEmptySlot.test(slot) && !isLockedTarget.test(slot)) {
                return slot;
            }
        }
        return -1;
    }
}
