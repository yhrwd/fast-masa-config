package fastui.yure.config;

import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortcutConfigStoreTest {
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
    void keepsTheFirstEntryWhenADuplicateTargetIsAdded() {
        ShortcutConfigStore.clear();
        ShortcutEntry first = new ShortcutEntry("tweakeroo", "Generic", "fastBlockPlacement", "First", ShortcutControlType.TOGGLE, 1.0, null, null);
        ShortcutEntry duplicate = new ShortcutEntry("tweakeroo", "Generic", "fastBlockPlacement", "Duplicate", ShortcutControlType.SLIDER, 0.05, null, null);

        ShortcutConfigStore.add(first);
        ShortcutConfigStore.add(duplicate);

        assertEquals(1, ShortcutConfigStore.getEntries().size());
        assertEquals("First", ShortcutConfigStore.getEntries().getFirst().labelOverride());
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
}
