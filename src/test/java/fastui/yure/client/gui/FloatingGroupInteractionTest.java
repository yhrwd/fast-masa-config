package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FloatingGroupInteractionTest {
    @Test
    void mapsDefaultRenderRowNToGroupItemNMinusOne() {
        assertEquals(3, FloatingGroupPanel.systemRowCount("default"));
        assertEquals(-1, FloatingGroupPanel.groupItemIndexForRow("default", 1));
        assertEquals(-1, FloatingGroupPanel.groupItemIndexForRow("default", 2));
        assertEquals(0, FloatingGroupPanel.groupItemIndexForRow("default", 3));
        assertEquals(1, FloatingGroupPanel.groupItemIndexForRow("default", 4));
        assertEquals(0, FloatingGroupPanel.groupItemIndexForRow("building", 0));
    }
}
