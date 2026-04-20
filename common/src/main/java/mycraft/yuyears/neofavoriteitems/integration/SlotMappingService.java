package mycraft.yuyears.neofavoriteitems.integration;

import java.util.Optional;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;

public final class SlotMappingService {
    private SlotMappingService() {}

    public static Optional<LogicalSlotIndex> fromPlayerInventoryIndex(int inventoryIndex) {
        if (!LogicalSlotIndex.isValid(inventoryIndex)) {
            return Optional.empty();
        }
        return Optional.of(LogicalSlotIndex.of(inventoryIndex));
    }

    public static int toInt(Optional<LogicalSlotIndex> logicalSlotIndex) {
        return logicalSlotIndex.map(LogicalSlotIndex::value).orElse(-1);
    }

    public static boolean isPlayerInventoryIndex(int inventoryIndex) {
        return LogicalSlotIndex.isValid(inventoryIndex);
    }
}
