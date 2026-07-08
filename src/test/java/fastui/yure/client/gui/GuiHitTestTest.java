package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiHitTestTest {
    @Test
    void matchesLeftAndTopEdgesButExcludesRightAndBottomEdges() {
        assertTrue(GuiHitTest.isInside(10, 20, 10, 20, 30, 40));
        assertTrue(GuiHitTest.isInside(39, 59, 10, 20, 30, 40));

        assertFalse(GuiHitTest.isInside(40, 59, 10, 20, 30, 40));
        assertFalse(GuiHitTest.isInside(39, 60, 10, 20, 30, 40));
    }
}
