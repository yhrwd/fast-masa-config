package fastui.yure.client.shortcut;

import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.config.ShortcutControlType;
import fastui.yure.config.ShortcutEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortcutResolverTest {
    @Test
    void resolvesSameConfigNameFromRequestedGroup() {
        ConfigIndexEntry generic = entry("generic");
        ConfigIndexEntry placement = entry("placement");
        ShortcutEntry shortcut = shortcut("placement");

        assertEquals(placement, ShortcutResolver.find(List.of(generic, placement), shortcut).orElseThrow());
    }

    @Test
    void resolvesLegacyShortcutWhenConfigIdentityIsUnique() {
        ConfigIndexEntry grouped = entry("generic");
        ShortcutEntry shortcut = ShortcutEntry.fromManualId("tweakeroo:enabled");

        assertEquals(grouped, ShortcutResolver.find(List.of(grouped), shortcut).orElseThrow());
    }

    @Test
    void doesNotGuessWhenLegacyShortcutMatchesMultipleGroups() {
        ConfigIndexEntry generic = entry("generic");
        ConfigIndexEntry placement = entry("placement");
        ShortcutEntry shortcut = ShortcutEntry.fromManualId("tweakeroo:enabled");

        assertTrue(ShortcutResolver.find(List.of(generic, placement), shortcut).isEmpty());
    }

    @Test
    void handlesNullIdentityFieldsWithoutThrowing() {
        ConfigIndexEntry nullIdentity = new ConfigIndexEntry(null, "Tweakeroo", null, "Generic", null, "Enabled", null);
        ShortcutEntry shortcut = new ShortcutEntry(null, null, null, "", ShortcutControlType.TOGGLE, 1.0, null, null);

        assertDoesNotThrow(() -> ShortcutResolver.find(List.of(nullIdentity), shortcut));
        assertEquals(nullIdentity, ShortcutResolver.find(List.of(nullIdentity), shortcut).orElseThrow());
    }

    private static ConfigIndexEntry entry(String groupId) {
        return new ConfigIndexEntry("tweakeroo", "Tweakeroo", groupId, groupId, "enabled", "Enabled", null);
    }

    private static ShortcutEntry shortcut(String groupId) {
        return new ShortcutEntry("tweakeroo", groupId, "enabled", "", ShortcutControlType.TOGGLE, 1.0, null, null);
    }
}
