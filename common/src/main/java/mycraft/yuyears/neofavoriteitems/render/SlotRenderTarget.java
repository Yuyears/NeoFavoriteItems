package mycraft.yuyears.neofavoriteitems.render;

import java.util.Objects;

import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;

/** Common slot geometry passed from platform screen adapters to render code. */
public record SlotRenderTarget(
    LogicalSlotIndex logicalSlot,
    boolean hasItem,
    int x,
    int y,
    int width,
    int height
) {
    public SlotRenderTarget {
        Objects.requireNonNull(logicalSlot, "logicalSlot");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Slot render target dimensions must be positive");
        }
    }

    public static SlotRenderTarget standard(LogicalSlotIndex logicalSlot, boolean hasItem, int x, int y) {
        return new SlotRenderTarget(logicalSlot, hasItem, x, y, 16, 16);
    }
}
