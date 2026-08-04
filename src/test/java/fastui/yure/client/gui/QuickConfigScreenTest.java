package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickConfigScreenTest {
    @Test
    void doesNotTreatMouseHotkeyAsCloseWhenReleaseToCloseIsEnabled() {
        assertFalse(QuickConfigScreen.isOpeningMouseHotkeyPress(true, List.of(-99), 1));
    }

    @Test
    void treatsConfiguredMouseHotkeyAsCloseWhenReleaseToCloseIsDisabled() {
        assertTrue(QuickConfigScreen.isOpeningMouseHotkeyPress(false, List.of(-99), 1));
    }

    @Test
    void mapsMouseButtonToMalilibKeyCodeAndRejectsOtherButtons() {
        List<Integer> openingHotkeyCodes = List.of(-99);

        assertFalse(QuickConfigScreen.isOpeningMouseHotkeyPress(false, openingHotkeyCodes, 0));
        assertFalse(QuickConfigScreen.isOpeningMouseHotkeyPress(false, openingHotkeyCodes, 2));
    }
}
