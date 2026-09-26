package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickPanelLayoutTest {
    @Test
    void clampsHeightAndCalculatesVisibleRows() {
        QuickPanelLayout layout = QuickPanelLayout.calculate(800, 600, 260, 240, 1.0, 20);

        assertEquals(260, layout.width());
        assertEquals(240, layout.height());
        assertEquals(1, layout.columns());
        assertEquals(7, layout.visibleRows());
        assertEquals(13, layout.maxScrollOffset());
    }

    @Test
    void usesContentHeightWhenBelowMaxHeight() {
        QuickPanelLayout layout = QuickPanelLayout.calculate(800, 600, 260, 240, 1.0, 3);

        assertEquals(115, layout.height());
        assertEquals(3, layout.visibleRows());
        assertEquals(0, layout.maxScrollOffset());
    }

    @Test
    void rowHeightAllowsTwoLinesWithoutOverlap() {
        assertTrue(QuickPanelLayout.ROW_HEIGHT >= 25);
    }

    @Test
    void usesTwoOrThreeColumnsForWiderPanels() {
        QuickPanelLayout twoColumns = QuickPanelLayout.calculate(900, 600, 360, 240, 1.0, 20);
        QuickPanelLayout threeColumns = QuickPanelLayout.calculate(1200, 700, 520, 240, 1.0, 20);

        assertEquals(2, twoColumns.columns());
        assertEquals(14, twoColumns.visibleItemCount());
        assertEquals(6, twoColumns.maxScrollOffset());
        assertEquals(3, threeColumns.columns());
        assertEquals(20, threeColumns.visibleItemCount());
        assertEquals(0, threeColumns.maxScrollOffset());
    }

    @Test
    void clampsScrollOffsetToAvailableRows() {
        QuickPanelLayout layout = QuickPanelLayout.calculate(800, 600, 260, 240, 1.0, 20);

        assertEquals(0, layout.clampScrollOffset(-5));
        assertEquals(7, layout.clampScrollOffset(7));
        assertEquals(13, layout.clampScrollOffset(99));
    }

    @Test
    void scalesPanelWidthAndHeightWithinViewport() {
        QuickPanelLayout layout = QuickPanelLayout.calculate(800, 600, 260, 240, 2.5, 40);

        assertEquals(650, layout.width());
        assertEquals(560, layout.height());
        assertEquals(3, layout.columns());
        assertEquals(75, layout.x());
        assertEquals(20, layout.y());
    }

    @Test
    void clampsLargeScaleToSmallViewport() {
        QuickPanelLayout layout = QuickPanelLayout.calculate(320, 180, 260, 240, 2.5, 40);

        assertEquals(280, layout.width());
        assertEquals(140, layout.height());
        assertEquals(1, layout.columns());
        assertEquals(20, layout.x());
        assertEquals(20, layout.y());
        assertTrue(layout.x() + layout.width() <= 320);
        assertTrue(layout.y() + layout.height() <= 180);
    }

    @Test
    void usesViewportWidthToAvoidOvercrowdedColumns() {
        QuickPanelLayout layout = QuickPanelLayout.calculate(420, 300, 260, 240, 2.5, 12);

        assertEquals(380, layout.width());
        assertEquals(2, layout.columns());
        assertEquals(20, layout.x());
        assertTrue(layout.cellWidth() >= 162);
        assertTrue(layout.visibleItemCount() <= 12);
    }
}
