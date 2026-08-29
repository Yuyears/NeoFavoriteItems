package mycraft.yuyears.neofavoriteitems.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HudSlotLayoutsTest {
    @Test
    void neoForgeQuadMatchesRuntimeLayoutForAllRowCounts() {
        int width = 800;
        int height = 450;

        assertArrayEquals(new int[] {309, 428}, position(0, 1, width, height));
        assertArrayEquals(new int[] {218, 428}, position(0, 2, width, height));
        assertArrayEquals(new int[] {400, 428}, position(1, 2, width, height));
        assertArrayEquals(new int[] {218, 428}, position(0, 4, width, height));
        assertArrayEquals(new int[] {400, 428}, position(1, 4, width, height));
        assertArrayEquals(new int[] {218, 406}, position(2, 4, width, height));
        assertArrayEquals(new int[] {400, 406}, position(3, 4, width, height));
    }

    @Test
    void neoForgeDoubleSwapsOnlyAppliedColumn() {
        assertEquals(0, HudSlotLayouts.neoForgeDoublePhysicalIndex(0, 3, -1));
        assertEquals(27, HudSlotLayouts.neoForgeDoublePhysicalIndex(9, 3, -1));
        assertEquals(27, HudSlotLayouts.neoForgeDoublePhysicalIndex(0, 3, 0));
        assertEquals(0, HudSlotLayouts.neoForgeDoublePhysicalIndex(9, 3, 0));
        assertEquals(28, HudSlotLayouts.neoForgeDoublePhysicalIndex(10, 3, 0));
    }

    private static int[] position(int row, int rows, int width, int height) {
        return HudSlotLayouts.neoForgeQuadPosition(row, rows, width, height);
    }
}
