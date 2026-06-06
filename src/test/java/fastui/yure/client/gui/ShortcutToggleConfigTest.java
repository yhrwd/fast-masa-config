package fastui.yure.client.gui;

import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.config.ShortcutConfigStore;
import fastui.yure.config.ShortcutControlType;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortcutToggleConfigTest {
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
}
