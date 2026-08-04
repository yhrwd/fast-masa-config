package fastui.yure.client.shortcut;

import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.config.ShortcutEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortcutResolverTest {
    @Test
    void doesNotResolveAConfigWithTheSameNameFromAnotherGroup() {
        List<ConfigIndexEntry> index = List.of(new ConfigIndexEntry(
                "tweakeroo", "Tweakeroo", "Generic", "Generic", "fastBlockPlacement", "Fast Block Placement", null));
        ShortcutEntry shortcut = ShortcutEntry.fromManualId("tweakeroo/Hotkeys/fastBlockPlacement");

        assertTrue(ShortcutResolver.find(index, shortcut).isEmpty());
    }
}
