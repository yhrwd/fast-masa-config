package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickConfigGroupGeometryTest {
    @Test
    void selectorOnlyHitsVisibleMenuRows() {
        assertEquals(1, QuickConfigPanel.selectorIndexAt(20, 40, 124, 18, 3, 30, 59));
        assertEquals(-1, QuickConfigPanel.selectorIndexAt(20, 40, 124, 18, 3, 30, 94));
        assertEquals(-1, QuickConfigPanel.selectorIndexAt(20, 40, 124, 18, 3, 144, 59));
    }

    @Test
    void dragClampUsesActualWindowDimensions() {
        assertArrayEquals(new int[]{320, 280}, FloatingGroupPanel.clampPosition(900, 900,
                600, 500, 260, 200, 20));
        assertArrayEquals(new int[]{20, 20}, FloatingGroupPanel.clampPosition(-10, -10,
                600, 500, 260, 200, 20));
    }

    @Test
    void usesSingleColumnGroupsLayoutAtAndBelowTheNarrowThreshold() {
        assertEquals(true, FastMasaConfigGui.isNarrowGroupsLayout(520));
        assertEquals(false, FastMasaConfigGui.isNarrowGroupsLayout(521));
    }

    @Test
    void onlyMarksDragWhenTheStoredPositionChanges() {
        assertFalse(FloatingGroupPanel.positionChanged(20, 20, 20, 20));
        assertTrue(FloatingGroupPanel.positionChanged(20, 20, 21, 20));
    }

    @Test
    void stationaryDefaultWindowDoesNotCountAsDragAfterLayoutClamp() {
        assertFalse(FloatingGroupPanel.positionChangedFromLayout(20, 20, 20, 20));
        assertTrue(FloatingGroupPanel.positionChangedFromLayout(20, 20, 21, 20));
    }

    @Test
    void groupConfigIndexKeyDistinguishesSameNameConfigsFromDifferentGroups() {
        assertFalse(FastMasaConfigGui.getGroupItemKey("tweakeroo", "Generic", "fastBlockPlacement")
                .equals(FastMasaConfigGui.getGroupItemKey("tweakeroo", "Hotkeys", "fastBlockPlacement")));
    }
}
