package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortcutToggleConfigTest {
    @Test
    void floatingVisualsKeepAnimationValuesBounded() {
        assertEquals(0.0, HoloPanelVisuals.easeOutQuart(0.0));
        assertEquals(1.0, HoloPanelVisuals.easeOutQuart(1.0));
        assertTrue(HoloPanelVisuals.easeOutQuart(0.5) > 0.5);
        assertEquals(0.25, HoloPanelVisuals.approach(0.0, 1.0, 0.25));
        assertEquals(0xFF808080, HoloPanelVisuals.mixRgb(0xFF000000, 0xFFFFFFFF, 0.5));
        assertTrue(HoloPanelVisuals.openProgress(40L, 120L) > 0.65);
    }

    @Test
    void floatingScreenExposesNumericCommitAndDragGuards() {
        assertTrue(QuickConfigScreen.shouldFlushPendingDrag(true));
        assertEquals(false, QuickConfigScreen.shouldFlushPendingDrag(false));
        assertTrue(QuickConfigScreen.isValidToggleItemIndex(0, 1));
        assertEquals(false, QuickConfigScreen.isValidToggleItemIndex(1, 1));
    }
}
