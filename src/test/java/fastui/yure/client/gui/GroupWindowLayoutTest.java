package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupWindowLayoutTest {
    @Test
    void clampsRequestedPositionAndUsesSafeWidth() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(800, 600, 900, 900, false, new int[]{25, 51});

        assertEquals(196, layout.width());
        assertEquals(584, layout.x());
        assertEquals(484, layout.y());
        assertEquals(20, layout.safeMargin());
    }

    @Test
    void usesMinimumSafeWidthWhenViewportAllowsIt() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(220, 160, 0, 0, false, new int[]{25});

        assertEquals(180, layout.width());
        assertEquals(20, layout.x());
        assertTrue(layout.x() + layout.width() <= 200);
    }

    @Test
    void arrangesCallerSuppliedRowHeightsBelowHeader() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(800, 600, 40, 50, false, new int[]{25, 51});

        assertEquals(20, layout.headerHeight());
        assertEquals(2, layout.rows().size());
        assertEquals(new GroupWindowLayout.Row(0, 40, 70, 196, 25), layout.rows().get(0));
        assertEquals(new GroupWindowLayout.Row(1, 40, 95, 196, 51), layout.rows().get(1));
        assertEquals(96, layout.height());
    }

    @Test
    void collapsedLayoutContainsOnlyHeader() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(800, 600, 40, 50, true, new int[]{25, 51});

        assertEquals(20, layout.height());
        assertEquals(20, layout.contentHeight());
        assertEquals(0, layout.rows().size());
        assertEquals(0, layout.maxScrollOffset());
    }

    @Test
    void onlyTreatsRowsFullyInsideTheScrolledContentViewportAsVisible() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(300, 100, 20, 20, false, new int[]{25, 25});

        assertFalse(layout.isRowFullyVisible(layout.rows().getFirst(), 10));
        assertTrue(layout.isRowFullyVisible(layout.rows().get(1), 10));
    }

    @Test
    void reportsHiddenContentAsPixelScrollRange() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(300, 100, 20, 20, false, new int[]{25, 51, 25});

        assertEquals(60, layout.height());
        assertEquals(121, layout.contentHeight());
        assertEquals(61, layout.maxScrollOffset());
        assertEquals(61, layout.clampScrollOffset(100));
    }

    @Test
    void keepsTinyViewportLayoutAndHeaderWithinScreenBounds() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(30, 35, -100, -100, false, new int[]{25});

        assertEquals(0, layout.width());
        assertEquals(0, layout.height());
        assertEquals(0, layout.headerHeight());
        assertEquals(0, layout.x());
        assertEquals(0, layout.y());
        assertTrue(layout.x() >= 0);
        assertTrue(layout.y() >= 0);
        assertTrue(layout.x() + layout.width() <= 30);
        assertTrue(layout.y() + layout.height() <= 35);
        assertTrue(layout.headerHeight() <= layout.height());
    }

    @Test
    void keepsNormalHeaderAndContentInsideTheirWindowOnAnOrdinaryViewport() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(800, 600, 40, 50, false, new int[]{25});

        assertEquals(196, layout.width());
        assertEquals(20, layout.headerHeight());
        assertTrue(layout.rows().getFirst().y() >= layout.y() + layout.headerHeight());
        assertTrue(layout.rows().getFirst().y() + layout.rows().getFirst().height() <= layout.y() + layout.height());
    }
}
