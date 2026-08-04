package fastui.yure.client.gui;

import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.config.ShortcutConfigStore;
import fastui.yure.config.ShortcutControlType;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortcutToggleConfigTest {
    @Test
    void ignoresEquivalentOpenQuickConfigSettingsSemantics() {
        KeybindSettings settings = createSettings(KeybindSettings.Context.INGAME, KeyAction.PRESS,
                false, true, true, false, true);
        KeybindSettings equivalentSettings = createSettings(KeybindSettings.Context.INGAME, KeyAction.PRESS,
                false, true, true, false, true);

        assertFalse(FastMasaConfigGui.hasOpenQuickConfigSettingsChanged(null, settings));
        assertFalse(FastMasaConfigGui.hasOpenQuickConfigSettingsChanged(settings, settings));
        assertFalse(FastMasaConfigGui.hasOpenQuickConfigSettingsChanged(settings, equivalentSettings));
    }

    @Test
    void commitsKeybindOnlyWhenCaptureEndsWithChangedValue() {
        assertFalse(FastMasaConfigGui.shouldCommitKeybindCapture("A", "A", true, true));
        assertTrue(FastMasaConfigGui.shouldCommitKeybindCapture("A", "B", false, true));
        assertFalse(FastMasaConfigGui.shouldCommitKeybindCapture("A", "B", true, false));
    }

    @Test
    void numericControlKeepsNormalGeometryAndFitsNarrowViewport() {
        FastMasaConfigGui.NumericControlLayout normal = FastMasaConfigGui.numericControlLayout(400);
        assertEquals(204, normal.controlX());
        assertEquals(68, normal.sliderWidth());
        assertEquals(130, normal.resetOffset());

        FastMasaConfigGui.NumericControlLayout narrow = FastMasaConfigGui.numericControlLayout(240);
        assertTrue(narrow.sliderWidth() < normal.sliderWidth());
        assertEquals(narrow.controlX() + narrow.valueWidth() + narrow.gap(), narrow.sliderX());
        assertEquals(narrow.sliderX() + narrow.sliderWidth() + narrow.gap(), narrow.resetX());
        assertTrue(narrow.valueWidth() > 0);
        assertTrue(narrow.sliderWidth() > 0);
        assertTrue(narrow.resetWidth() > 0);
        assertTrue(narrow.right() <= 240 - 12);
    }

    @Test
    void booleanControlKeepsToggleAndResetInsideNarrowViewport() {
        FastMasaConfigGui.BooleanControlLayout normal = FastMasaConfigGui.booleanControlLayout(400);
        assertEquals(64, normal.toggleWidth());
        assertEquals(54, normal.resetWidth());

        FastMasaConfigGui.BooleanControlLayout narrow = FastMasaConfigGui.booleanControlLayout(150);
        assertTrue(narrow.toggleWidth() > 0);
        assertTrue(narrow.resetWidth() > 0);
        assertEquals(narrow.controlX() + narrow.toggleWidth() + narrow.gap(), narrow.resetX());
        assertTrue(narrow.right() <= 150 - 12);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("settingsWithOneChangedSemanticField")
    void detectsEachChangedOpenQuickConfigSettingsSemantic(String field, KeybindSettings changedSettings) {
        KeybindSettings settings = createSettings(KeybindSettings.Context.INGAME, KeyAction.PRESS,
                false, true, true, false, true);

        assertTrue(FastMasaConfigGui.hasOpenQuickConfigSettingsChanged(settings, changedSettings));
    }

    private static Stream<Arguments> settingsWithOneChangedSemanticField() {
        return Stream.of(
                Arguments.of("context", createSettings(KeybindSettings.Context.GUI, KeyAction.PRESS,
                        false, true, true, false, true)),
                Arguments.of("activateOn", createSettings(KeybindSettings.Context.INGAME, KeyAction.RELEASE,
                        false, true, true, false, true)),
                Arguments.of("allowEmpty", createSettings(KeybindSettings.Context.INGAME, KeyAction.PRESS,
                        true, true, true, false, true)),
                Arguments.of("allowExtraKeys", createSettings(KeybindSettings.Context.INGAME, KeyAction.PRESS,
                        false, false, true, false, true)),
                Arguments.of("orderSensitive", createSettings(KeybindSettings.Context.INGAME, KeyAction.PRESS,
                        false, true, false, false, true)),
                Arguments.of("exclusive", createSettings(KeybindSettings.Context.INGAME, KeyAction.PRESS,
                        false, true, true, true, true)),
                Arguments.of("cancel", createSettings(KeybindSettings.Context.INGAME, KeyAction.PRESS,
                        false, true, true, false, false))
        );
    }

    private static KeybindSettings createSettings(KeybindSettings.Context context, KeyAction activateOn,
                                                   boolean allowEmpty, boolean allowExtraKeys, boolean orderSensitive,
                                                   boolean exclusive, boolean cancel) {
        return KeybindSettings.create(context, activateOn, allowExtraKeys, orderSensitive, exclusive, cancel, allowEmpty);
    }

    @Test
    void togglingBooleanConfigAddsAndRemovesShortcut() {
        ShortcutConfigStore.clear();
        ConfigIndexEntry entry = new ConfigIndexEntry("tweakeroo", "Tweakeroo", "Generic", "Generic", "fastBlockPlacement", "Fast Block Placement", new ConfigBoolean("fastBlockPlacement", false));
        ShortcutToggleConfig config = new ShortcutToggleConfig(entry, () -> { });

        assertFalse(config.getBooleanValue());

        config.setBooleanValue(true);

        assertTrue(ShortcutConfigStore.containsTarget("tweakeroo", "Generic", "fastBlockPlacement"));
        assertEquals(ShortcutControlType.TOGGLE, ShortcutConfigStore.getEntries().getFirst().controlType());

        config.setBooleanValue(false);

        assertFalse(ShortcutConfigStore.containsTarget("tweakeroo", "Generic", "fastBlockPlacement"));
    }

    @Test
    void numericConfigCreatesSliderShortcut() {
        ShortcutConfigStore.clear();
        ConfigIndexEntry entry = new ConfigIndexEntry("minihud", "MiniHUD", "Renderer", "Renderer", "fontScale", "Font Scale", new ConfigInteger("fontScale", 10, 1, 20));
        ShortcutToggleConfig config = new ShortcutToggleConfig(entry, () -> { });

        config.setBooleanValue(true);

        assertEquals(ShortcutControlType.SLIDER, ShortcutConfigStore.getEntries().getFirst().controlType());
        assertEquals(1.0, ShortcutConfigStore.getEntries().getFirst().sliderStep());
    }

    @Test
    void savingNonShortcutsTabDoesNotReplaceStoreWithEmptyEditorState() {
        ShortcutConfigStore.clear();
        ShortcutConfigStore.add(new fastui.yure.config.ShortcutEntry("minihud", "Renderer", "fontScale", "", ShortcutControlType.SLIDER, 1.0, null, null));

        FastMasaConfigGui.syncShortcutEditorToStore(false);

        assertTrue(ShortcutConfigStore.containsTarget("minihud", "Renderer", "fontScale"));
    }

    @Test
    void shortcutsFilterDoesNotExposeAddedState() {
        assertEquals(FastMasaConfigGui.FilterMode.ALL,
                FastMasaConfigGui.normalizeFilterMode(true, FastMasaConfigGui.FilterMode.ADDED));
        assertEquals(FastMasaConfigGui.FilterMode.MISSING,
                FastMasaConfigGui.nextFilterMode(true, FastMasaConfigGui.FilterMode.ALL));
        assertEquals(FastMasaConfigGui.FilterMode.ALL,
                FastMasaConfigGui.nextFilterMode(true, FastMasaConfigGui.FilterMode.MISSING));
    }

    @Test
    void fitDoesNotReturnEllipsisWhenNoRoomExists() {
        assertEquals("", FastMasaConfigGui.fitText("abcdef", 0, String::length));
        assertEquals("a", FastMasaConfigGui.fitText("abcdef", 1, String::length));
        assertEquals("a...", FastMasaConfigGui.fitText("abcdef", 4, String::length));
    }

    @Test
    void genericPageProvidesInlineBooleanAndNumericInteractionPaths() {
        var methodNames = Arrays.stream(FastMasaConfigGui.class.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(methodNames.contains("handleGenericClick"));
        assertTrue(methodNames.contains("mouseDragged"));
        assertTrue(methodNames.contains("mouseReleased"));
    }
}
