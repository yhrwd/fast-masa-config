package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryActionLayoutTest {
    @Test
    void placesRecoveryActionInTheTopRightCornerWithinTheViewport() {
        RecoveryActionLayout layout = RecoveryActionLayout.calculate(100, 60);

        assertEquals(76, layout.x());
        assertEquals(4, layout.y());
        assertEquals(20, layout.width());
        assertEquals(20, layout.height());
        assertTrue(layout.x() + layout.width() <= 100);
        assertTrue(layout.y() + layout.height() <= 60);
    }

    @Test
    void keepsRecoveryActionWithinATinyViewport() {
        RecoveryActionLayout layout = RecoveryActionLayout.calculate(10, 8);

        assertEquals(0, layout.x());
        assertEquals(0, layout.y());
        assertEquals(10, layout.width());
        assertEquals(8, layout.height());
    }
}
