package fastui.yure.client.shortcut;

import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.client.index.ConfigIndexService;
import fastui.yure.config.ShortcutEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShortcutResolverTest {
    @Test
    void resolvesRepeatedNamesFromThePrebuiltTargetIndex() {
        List<ConfigIndexEntry> index = List.of(
                new ConfigIndexEntry("tweakeroo", "Tweakeroo", "Generic", "Generic",
                        "fastBlockPlacement", "Generic value", null),
                new ConfigIndexEntry("tweakeroo", "Tweakeroo", "Hotkeys", "Hotkeys",
                        "fastBlockPlacement", "Hotkey value", null));
        ShortcutEntry shortcut = ShortcutEntry.fromManualId("tweakeroo/Hotkeys/fastBlockPlacement");

        Map<ConfigIndexService.Target, ConfigIndexEntry> indexByTarget = Map.of(
                new ConfigIndexService.Target("tweakeroo", "Generic", "fastBlockPlacement"), index.get(0),
                new ConfigIndexService.Target("tweakeroo", "Hotkeys", "fastBlockPlacement"), index.get(1));
        ConfigIndexEntry resolved = ShortcutResolver.find(indexByTarget, shortcut)
                .orElseThrow();

        assertEquals("Hotkeys", resolved.groupId());
        assertEquals("Hotkey value", resolved.displayName());
    }
}
