package fastui.yure.config;

import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortcutConfigStoreTest {
    @Test
    void parsesManualShortcutIdWithGroup() {
        ShortcutEntry entry = ShortcutEntry.fromManualId("tweakeroo/Generic/fastBlockPlacement");

        assertEquals("tweakeroo", entry.modId());
        assertEquals("Generic", entry.groupId());
        assertEquals("fastBlockPlacement", entry.configName());
    }

    @Test
    void parsesManualShortcutIdWithoutGroup() {
        ShortcutEntry entry = ShortcutEntry.fromManualId("minihud:fontScale");

        assertEquals("minihud", entry.modId());
        assertEquals("", entry.groupId());
        assertEquals("fontScale", entry.configName());
    }

    @Test
    void roundTripsShortcutJson() {
        ShortcutConfigStore.clear();
        ShortcutConfigStore.add(new ShortcutEntry("tweakeroo", "Generic", "fastBlockPlacement", "Fast Place", ShortcutControlType.TOGGLE, 1.0, null, null));

        JsonArray json = ShortcutConfigStore.toJson();

        ShortcutConfigStore.clear();
        ShortcutConfigStore.fromJson(json);

        assertEquals(1, ShortcutConfigStore.getEntries().size());
        assertTrue(ShortcutConfigStore.getEntries().getFirst().isSameTarget("tweakeroo", "Generic", "fastBlockPlacement"));
    }

    @Test
    void exposesEditableManualIdsForMalilibStringList() {
        ShortcutConfigStore.clear();
        ShortcutConfigStore.add(new ShortcutEntry("tweakeroo", "Generic", "fastBlockPlacement", "Fast Place", ShortcutControlType.TOGGLE, 1.0, null, null));
        ShortcutConfigStore.add(new ShortcutEntry("minihud", "Renderer", "overlayLightLevel", "", ShortcutControlType.SLIDER, 0.05, null, null));

        assertEquals(List.of("tweakeroo/Generic/fastBlockPlacement", "minihud/Renderer/overlayLightLevel"), ShortcutConfigStore.toManualIds());
    }

    @Test
    void replacesShortcutsFromMalilibStringListValues() {
        ShortcutConfigStore.clear();
        ShortcutConfigStore.add(new ShortcutEntry("tweakeroo", "Generic", "oldConfig", "", ShortcutControlType.TOGGLE, 1.0, null, null));

        ShortcutConfigStore.replaceWithManualIds(List.of("tweakeroo/Generic/fastBlockPlacement", "minihud:fontScale", " "));

        assertEquals(2, ShortcutConfigStore.getEntries().size());
        assertTrue(ShortcutConfigStore.getEntries().get(0).isSameTarget("tweakeroo", "Generic", "fastBlockPlacement"));
        assertTrue(ShortcutConfigStore.getEntries().get(1).isSameTarget("minihud", "", "fontScale"));
    }

    @Test
    void detectsAndRemovesShortcutTargets() {
        ShortcutConfigStore.clear();
        ShortcutConfigStore.add(new ShortcutEntry("tweakeroo", "Generic", "fastBlockPlacement", "", ShortcutControlType.TOGGLE, 1.0, null, null));
        ShortcutConfigStore.add(new ShortcutEntry("minihud", "Renderer", "overlayLightLevel", "", ShortcutControlType.SLIDER, 0.05, null, null));

        assertTrue(ShortcutConfigStore.containsTarget("tweakeroo", "Generic", "fastBlockPlacement"));

        ShortcutConfigStore.removeTarget("tweakeroo", "Generic", "fastBlockPlacement");

        assertEquals(1, ShortcutConfigStore.getEntries().size());
        assertTrue(ShortcutConfigStore.getEntries().getFirst().isSameTarget("minihud", "Renderer", "overlayLightLevel"));
    }

    @Test
    void ignoresDuplicateTargetsWhenAddingOrReplacing() {
        ShortcutConfigStore.clear();
        ShortcutEntry first = new ShortcutEntry("tweakeroo", "Generic", "fastBlockPlacement", "First", ShortcutControlType.TOGGLE, 1.0, null, null);
        ShortcutEntry duplicate = new ShortcutEntry("tweakeroo", "Hotkeys", "fastBlockPlacement", "Duplicate", ShortcutControlType.SLIDER, 0.05, null, null);

        ShortcutConfigStore.add(first);
        ShortcutConfigStore.add(duplicate);

        assertEquals(1, ShortcutConfigStore.getEntries().size());
        assertEquals("First", ShortcutConfigStore.getEntries().getFirst().labelOverride());

        ShortcutConfigStore.replaceWithManualIds(List.of("minihud:fontScale", "minihud:fontScale", "minihud/Renderer/fontScale"));

        assertEquals(List.of("minihud:fontScale"), ShortcutConfigStore.toManualIds());
    }

    @Test
    void movesShortcutsWithinCurrentOrder() {
        ShortcutConfigStore.clear();
        ShortcutConfigStore.add(new ShortcutEntry("tweakeroo", "Generic", "fastBlockPlacement", "", ShortcutControlType.TOGGLE, 1.0, null, null));
        ShortcutConfigStore.add(new ShortcutEntry("minihud", "Renderer", "overlayLightLevel", "", ShortcutControlType.SLIDER, 0.05, null, null));

        ShortcutConfigStore.move(1, -1);

        assertEquals(List.of("minihud/Renderer/overlayLightLevel", "tweakeroo/Generic/fastBlockPlacement"), ShortcutConfigStore.toManualIds());
    }
}
