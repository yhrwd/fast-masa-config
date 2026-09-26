package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoloPanelVisualsTest {
    @Test
    void easeOutQuartStaysInRangeAndReachesEndpoints() {
        assertEquals(0.0, HoloPanelVisuals.easeOutQuart(0.0));
        assertEquals(1.0, HoloPanelVisuals.easeOutQuart(1.0));

        double halfway = HoloPanelVisuals.easeOutQuart(0.5);

        assertTrue(halfway > 0.5);
        assertTrue(halfway < 1.0);
    }

    @Test
    void approachMovesTowardTargetWithoutOvershooting() {
        assertEquals(0.25, HoloPanelVisuals.approach(0.0, 1.0, 0.25));
        assertEquals(0.75, HoloPanelVisuals.approach(1.0, 0.0, 0.25));
        assertEquals(1.0, HoloPanelVisuals.approach(0.9, 1.0, 2.0));
    }

    @Test
    void appliesAlphaToRgbColor() {
        assertEquals(0x6600CCFF, HoloPanelVisuals.withAlpha(0xFF00CCFF, 0x66));
    }

    @Test
    void mixesRgbColors() {
        assertEquals(0xFF808080, HoloPanelVisuals.mixRgb(0xFF000000, 0xFFFFFFFF, 0.5));
    }

    @Test
    void openAnimationUsesFastEaseOut() {
        assertEquals(0.0, HoloPanelVisuals.openProgress(0L, 120L));
        assertTrue(HoloPanelVisuals.openProgress(40L, 120L) > 0.65);
        assertEquals(1.0, HoloPanelVisuals.openProgress(160L, 120L));
    }
}
