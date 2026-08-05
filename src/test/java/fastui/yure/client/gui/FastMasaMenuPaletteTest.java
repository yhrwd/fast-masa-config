package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FastMasaMenuPaletteTest {
    @Test
    void ownsTheSharedPurpleAndDeepGrayMenuTokens() {
        assertEquals(0xFF913DE2, FastMasaMenuPalette.ACCENT);
        assertEquals(0xFF1C1C22, FastMasaMenuPalette.SURFACE);
        assertEquals(0xFF4B4850, FastMasaMenuPalette.NEUTRAL);
    }
}
