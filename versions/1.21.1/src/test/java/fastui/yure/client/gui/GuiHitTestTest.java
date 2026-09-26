package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiHitTestTest {
    @Test
    void containsIncludesTopLeftAndExcludesBottomRightEdges() {
        assertTrue(GuiHitTest.contains(10, 10, 20, 20, 10, 10));
        assertTrue(GuiHitTest.contains(10, 10, 20, 20, 29, 29));
        assertFalse(GuiHitTest.contains(10, 10, 20, 20, 30, 29));
        assertFalse(GuiHitTest.contains(10, 10, 20, 20, 29, 30));
    }

    @Test
    void containsIncludesLastRowPixelAndExcludesFirstPixelBelowRect() {
        assertTrue(GuiHitTest.contains(12, 80, 300, 30, 12, 109));
        assertFalse(GuiHitTest.contains(12, 80, 300, 30, 12, 110));
    }
}
