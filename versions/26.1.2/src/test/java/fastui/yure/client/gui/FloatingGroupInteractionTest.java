package fastui.yure.client.gui;

import fastui.yure.config.GroupItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloatingGroupInteractionTest {
    @Test
    void reservesDefaultRowZeroForTheFullConfigAction() {
        assertTrue(FloatingGroupPanel.isSystemConfigRow("default", 0));
        assertEquals(-1, FloatingGroupPanel.groupItemIndexForRow("default", 0));
        assertTrue(QuickConfigScreen.shouldOpenSystemConfigRow(GroupWindowHitTest.Target.ROW, true));
        assertFalse(QuickConfigScreen.shouldOpenSystemConfigRow(GroupWindowHitTest.Target.ROW, false));
    }

    @Test
    void mapsDefaultRenderRowNToGroupItemNMinusOne() {
        assertEquals(-1, FloatingGroupPanel.groupItemIndexForRow("default", 1));
        assertEquals(-1, FloatingGroupPanel.groupItemIndexForRow("default", 2));
        assertEquals(0, FloatingGroupPanel.groupItemIndexForRow("default", 3));
        assertEquals(1, FloatingGroupPanel.groupItemIndexForRow("default", 4));
        assertEquals(0, FloatingGroupPanel.groupItemIndexForRow("building", 0));
    }

    @Test
    void recoveryOpensTheDefaultGroupOnAllConfigs() {
        assertEquals("default", FastMasaConfigGui.recoveryTargetGroupId());
    }

    @Test
    void unresolvedRowsAreNotAvailableForMutation() {
        GroupItem staleItem = new GroupItem("missing-mod", "missing-group", "missing-config", false);

        assertFalse(FloatingGroupPanel.isInteractiveRow(staleItem, null));
    }

    @Test
    void rejectsSystemRowAndOtherNegativeIndicesBeforeToggleDispatch() {
        assertFalse(QuickConfigScreen.isValidToggleItemIndex(-1, 2));
        assertFalse(QuickConfigScreen.isValidToggleItemIndex(-1, 0));
        assertTrue(QuickConfigScreen.isValidToggleItemIndex(0, 2));
    }
}
