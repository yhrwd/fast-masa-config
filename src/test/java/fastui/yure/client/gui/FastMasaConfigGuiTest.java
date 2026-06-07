package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastMasaConfigGuiTest {
    @Test
    void placesStatusToastInTitleGapAwayFromFilterControls() {
        FastMasaConfigGui.StatusToastPlacement placement = FastMasaConfigGui.getStatusToastPlacement(854, 96);

        assertEquals(10, placement.y());
        assertTrue(placement.x() >= 144);
        assertTrue(placement.x() + placement.textWidth() <= 854 - 258);
    }

    @Test
    void limitsStatusToastWidthToSafeTitleGap() {
        FastMasaConfigGui.StatusToastPlacement placement = FastMasaConfigGui.getStatusToastPlacement(420, 480);

        assertEquals(10, placement.y());
        assertTrue(placement.textWidth() <= 420 - 144 - 258);
    }
}
