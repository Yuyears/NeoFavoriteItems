package mycraft.yuyears.neofavoriteitems.render;

/** Vanilla hotbar item coordinates used by both HUD render phases. */
public final class HotbarSlotLayout {
    private static final int SLOT_COUNT = 9;
    private static final int SLOT_SPACING = 20;

    private HotbarSlotLayout() {}

    public static int x(int guiWidth, int slotIndex) {
        return guiWidth / 2 - 88 + slotIndex * SLOT_SPACING;
    }

    public static int y(int guiHeight) {
        return guiHeight - 19;
    }

    public static int slotAt(int guiWidth, int guiHeight, int x, int y) {
        if (y != y(guiHeight)) return -1;
        int delta = x - x(guiWidth, 0);
        if (delta < 0 || delta % SLOT_SPACING != 0) return -1;
        int slotIndex = delta / SLOT_SPACING;
        return slotIndex < SLOT_COUNT ? slotIndex : -1;
    }
}
