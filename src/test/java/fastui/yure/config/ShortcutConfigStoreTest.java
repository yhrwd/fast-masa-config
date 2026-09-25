package fastui.yure.config;

import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void keepsTargetsWithTheSameNameInDifferentGroups() {
        ShortcutConfigStore.clear();
        ShortcutEntry first = new ShortcutEntry("tweakeroo", "Generic", "fastBlockPlacement", "First", ShortcutControlType.TOGGLE, 1.0, null, null);
        ShortcutEntry duplicate = new ShortcutEntry("tweakeroo", "Hotkeys", "fastBlockPlacement", "Duplicate", ShortcutControlType.SLIDER, 0.05, null, null);

        ShortcutConfigStore.add(first);
        ShortcutConfigStore.add(duplicate);

        assertEquals(2, ShortcutConfigStore.getEntries().size());
        assertEquals("First", ShortcutConfigStore.getEntries().getFirst().labelOverride());
        assertEquals("Duplicate", ShortcutConfigStore.getEntries().get(1).labelOverride());

    }

    @Test
    void rejectsInvalidShortcutEntries() {
        ShortcutConfigStore.clear();

        assertFalse(ShortcutConfigStore.add(null));
        assertFalse(ShortcutConfigStore.add(new ShortcutEntry(null, null, null, null, null,
                Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)));
    }

    @Test
    void normalizesOptionalShortcutMetadata() {
        ShortcutEntry entry = new ShortcutEntry(" mod ", null, " config ", null, null,
                Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        assertEquals("mod", entry.modId());
        assertEquals("", entry.groupId());
        assertEquals("config", entry.configName());
        assertEquals(ShortcutControlType.TOGGLE, entry.controlType());
        assertEquals(1.0, entry.sliderStep());
        assertNull(entry.minOverride());
        assertNull(entry.maxOverride());
    }
}
