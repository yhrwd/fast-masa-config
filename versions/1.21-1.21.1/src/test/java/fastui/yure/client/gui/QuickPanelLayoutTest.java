package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickPanelLayoutTest {
    @Test
    void collapsedWindowHasHeaderOnlyAndNoRows() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(800, 600, 100, 100, true,
                new int[]{24, 30});

        assertEquals(196, layout.width());
        assertEquals(0, layout.rows().size());
        assertEquals(layout.headerHeight(), layout.height());
    }

    @Test
    void contentRowsAndScrollOffsetAreComputedFromMeasuredHeight() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(320, 240, 10, 10, false,
                new int[]{24, 24, 24, 24}, 180);

        assertTrue(layout.height() <= 220);
        assertTrue(layout.contentHeight() >= layout.height());
        assertTrue(layout.maxScrollOffset() >= 0);
        assertEquals(layout.maxScrollOffset(), layout.clampScrollOffset(Integer.MAX_VALUE));
    }

    @Test
    void hitTestUsesTheSameRowBoundsAsLayout() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(640, 480, 20, 20, false,
                new int[]{24}, 180);
        GroupWindowLayout.Row row = layout.rows().getFirst();

        GroupWindowHitTest.Result hit = GroupWindowHitTest.hitTest(layout, 0,
                row.x() + 2, row.y() + 2, null, java.util.List.of());
        assertEquals(GroupWindowHitTest.Target.ROW, hit.target());
        assertEquals(0, hit.itemIndex());
    }

    @Test
    void movementCodesAreNormalizedBeforePassingToScreen() {
        assertEquals(java.util.Set.of(17, 30),
                QuickConfigScreen.normalizeMovementKeyCodes(java.util.Arrays.asList(17, null, 17, 30)));
    }
}
