package mycraft.yuyears.neofavoriteitems.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;

class OverlayPlacementTest {
    @Test
    void disallowOverflowKeepsPlacementInsideSlot() {
        var target = SlotRenderTarget.standard(LogicalSlotIndex.of(0), true, 10, 20);
        var placement = new OverlayPlacement(
            OverlayPlacement.Anchor.SLOT_TOP_LEFT,
            -8.0f,
            -8.0f,
            80.0f,
            80.0f,
            1.0f,
            false
        );

        var resolved = placement.resolve(target);

        assertEquals(10, resolved.x());
        assertEquals(20, resolved.y());
        assertEquals(16, resolved.width());
        assertEquals(16, resolved.height());
    }

    @Test
    void allowOverflowPreservesCappedSizeAndOffset() {
        var target = SlotRenderTarget.standard(LogicalSlotIndex.of(0), true, 10, 20);
        var placement = new OverlayPlacement(
            OverlayPlacement.Anchor.SLOT_TOP_LEFT,
            -2.0f,
            3.0f,
            80.0f,
            20.0f,
            1.0f,
            true
        );

        var resolved = placement.resolve(target);

        assertEquals(8, resolved.x());
        assertEquals(23, resolved.y());
        assertEquals(64, resolved.width());
        assertEquals(20, resolved.height());
    }

    @Test
    void preservesRotationForCommandCompilation() {
        var target = SlotRenderTarget.standard(LogicalSlotIndex.of(0), true, 10, 20);
        var placement = new OverlayPlacement(
            OverlayPlacement.Anchor.SLOT_CENTER, 0.0f, 0.0f, 16.0f, 16.0f, 1.0f, 45.0f, false
        );

        assertEquals(45.0f, placement.resolve(target).rotationDegrees());
    }

    @Test
    void anchorsFinalRectangleThenAppliesOffset() {
        var target = SlotRenderTarget.standard(LogicalSlotIndex.of(0), true, 10, 20);

        var topLeft = new OverlayPlacement(
            OverlayPlacement.Anchor.SLOT_TOP_LEFT, 2.0f, -1.0f, 8.0f, 6.0f, 1.0f, true
        ).resolve(target);
        var bottomRight = new OverlayPlacement(
            OverlayPlacement.Anchor.SLOT_BOTTOM_RIGHT, 0.0f, 0.0f, 8.0f, 6.0f, 1.0f, true
        ).resolve(target);
        var center = new OverlayPlacement(
            OverlayPlacement.Anchor.SLOT_CENTER, 0.0f, 0.0f, 8.0f, 6.0f, 1.0f, true
        ).resolve(target);

        assertEquals(12, topLeft.x());
        assertEquals(19, topLeft.y());
        assertEquals(18, bottomRight.x());
        assertEquals(30, bottomRight.y());
        assertEquals(14, center.x());
        assertEquals(25, center.y());
    }

    @Test
    void rightAnchorUsesRoundedPixelWidth() {
        var target = SlotRenderTarget.standard(LogicalSlotIndex.of(0), true, 10, 20);
        var resolved = new OverlayPlacement(
            OverlayPlacement.Anchor.SLOT_TOP_RIGHT, 0.0f, 0.0f, 5.0f, 6.0f, 1.5f, true
        ).resolve(target);

        assertEquals(8, resolved.width());
        assertEquals(18, resolved.x());
        assertEquals(26, resolved.x() + resolved.width());
    }
}
