package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("keybindCaptureDecisions")
    void decidesKeybindCaptureCommitAtSelectionBoundary(String name, String initialValue, String currentValue,
            boolean captureEnded, boolean expected) {
        assertEquals(expected, FastMasaConfigGui.shouldCommitKeybindCapture(initialValue, currentValue, captureEnded));
    }

    private static Stream<Arguments> keybindCaptureDecisions() {
        return Stream.of(
                Arguments.of("no-op", "LEFT_ALT,C", "LEFT_ALT,C", true, false),
                Arguments.of("changed and completed", "LEFT_ALT,C", "LEFT_ALT,V", true, true),
                Arguments.of("changed then reverted", "LEFT_ALT,C", "LEFT_ALT,C", true, false),
                Arguments.of("changed but cancelled", "LEFT_ALT,C", "LEFT_ALT,V", false, false),
                Arguments.of("completion without change", "LEFT_ALT,C", "LEFT_ALT,C", true, false));
    }

    @ParameterizedTest(name = "semantic field: {0}")
    @MethodSource("semanticKeybindSettingsChanges")
    void detectsOneChangedSemanticKeybindSettingsField(String field, KeybindSettings previousSettings,
            KeybindSettings currentSettings) {
        assertTrue(FastMasaConfigGui.hasOpenQuickConfigSettingsChanged(previousSettings, currentSettings), field);
    }

    private static Stream<Arguments> semanticKeybindSettingsChanges() {
        KeybindSettings baseline = KeybindSettings.create(KeybindSettings.Context.INGAME, KeyAction.PRESS,
                true, true, false, true, false);

        return Stream.of(
                Arguments.of("context", baseline,
                        KeybindSettings.create(KeybindSettings.Context.GUI, KeyAction.PRESS, true, true, false, true,
                                false)),
                Arguments.of("activation", baseline,
                        KeybindSettings.create(KeybindSettings.Context.INGAME, KeyAction.RELEASE, true, true, false,
                                true, false)),
                Arguments.of("allow extra keys", baseline,
                        KeybindSettings.create(KeybindSettings.Context.INGAME, KeyAction.PRESS, false, true, false, true,
                                false)),
                Arguments.of("order sensitive", baseline,
                        KeybindSettings.create(KeybindSettings.Context.INGAME, KeyAction.PRESS, true, false, false, true,
                                false)),
                Arguments.of("exclusive", baseline,
                        KeybindSettings.create(KeybindSettings.Context.INGAME, KeyAction.PRESS, true, true, true, true,
                                false)),
                Arguments.of("cancel", baseline,
                        KeybindSettings.create(KeybindSettings.Context.INGAME, KeyAction.PRESS, true, true, false, false,
                                false)),
                Arguments.of("allow empty", baseline,
                        KeybindSettings.create(KeybindSettings.Context.INGAME, KeyAction.PRESS, true, true, false, true,
                                true)));
    }

    @Test
    void unchangedSemanticKeybindSettingsDoNotTriggerRefresh() {
        KeybindSettings settings = KeybindSettings.create(KeybindSettings.Context.INGAME, KeyAction.PRESS,
                true, true, false, true, false);

        assertFalse(FastMasaConfigGui.hasOpenQuickConfigSettingsChanged(settings, settings));
    }
}
