package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuickPanelLayoutTest {
    @Test
    void exposesQuickAndEnabledPanelModes() {
        assertEquals(QuickConfigPanel.PanelMode.SHORTCUTS, QuickConfigPanel.PanelMode.SHORTCUTS);
        assertEquals(QuickConfigPanel.PanelMode.ENABLED_BOOLEANS, QuickConfigPanel.PanelMode.ENABLED_BOOLEANS);
    }
}
