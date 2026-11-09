package fastui.yure.client.shortcut;

import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.client.index.ConfigIndexService;
import fastui.yure.config.ShortcutEntry;

import java.util.Map;
import java.util.Optional;

public final class ShortcutResolver {
    private ShortcutResolver() {
    }

    public static Optional<ConfigIndexEntry> find(Map<ConfigIndexService.Target, ConfigIndexEntry> index,
            ShortcutEntry shortcut) {
        return Optional.ofNullable(index.get(new ConfigIndexService.Target(shortcut.modId(), shortcut.groupId(),
                shortcut.configName())));
    }
}
