package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullConfigPageLayoutTest {
    @Test
    void keepsAllTabsInsideTheViewportAtNarrowWidths() {
        FullConfigPageLayout.TabStrip layout = FullConfigPageLayout.calculateTabStrip(260,
                new int[] { 64, 72, 84, 56 }, true);

        assertEquals(12, layout.xFor(0));
        assertEquals(12, layout.xFor(3));
        assertTrue(layout.xFor(3) + layout.buttonWidth(3) <= 260 - FullConfigListLayout.MARGIN);
        assertTrue(layout.yFor(3) < layout.contentTop());
        assertTrue(layout.usesCompactLabels());
    }

    @Test
    void keepsLabelsAtTheirPreferredWidthsOnWideScreens() {
        FullConfigPageLayout.TabStrip layout = FullConfigPageLayout.calculateTabStrip(800,
                new int[] { 92, 104, 120, 64 }, false);

        assertEquals(92, layout.buttonWidth(0));
        assertEquals(104, layout.buttonWidth(1));
        assertTrue(layout.yFor(3) == FullConfigPageLayout.TAB_Y);
        assertTrue(layout.contentTop() > FullConfigPageLayout.TAB_Y);
    }
}
