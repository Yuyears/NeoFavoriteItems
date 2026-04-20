package mycraft.yuyears.newitemfavorites.domain;

import java.util.Objects;

public final class LogicalSlotIndex {
    public static final int MIN_INDEX = 0;
    public static final int MAX_INDEX = 40;

    private final int value;

    private LogicalSlotIndex(int value) {
        this.value = value;
    }

    public static LogicalSlotIndex of(int value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid logical slot index: " + value);
        }
        return new LogicalSlotIndex(value);
    }

    public static boolean isValid(int value) {
        return value >= MIN_INDEX && value <= MAX_INDEX;
    }

    public int value() {
        return value;
    }

    public boolean isHotbar() {
        return value >= 0 && value <= 8;
    }

    public boolean isMainInventory() {
        return value >= 9 && value <= 35;
    }

    public boolean isArmor() {
        return value >= 36 && value <= 39;
    }

    public boolean isOffhand() {
        return value == 40;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LogicalSlotIndex other)) {
            return false;
        }
        return value == other.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "LogicalSlotIndex[" + value + "]";
    }
}
