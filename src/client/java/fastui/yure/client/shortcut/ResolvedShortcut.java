package fastui.yure.client.shortcut;

import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.config.ShortcutEntry;

public record ResolvedShortcut(ShortcutEntry shortcut, ConfigIndexEntry configEntry) {
}
