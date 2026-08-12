package fastui.yure.client.shortcut;

import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.client.index.ConfigIndexService;
import fastui.yure.config.ShortcutEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ShortcutResolver {
    private ShortcutResolver() {
    }

    public static List<ResolvedShortcut> resolve(List<ShortcutEntry> shortcuts) {
        Map<ConfigIndexService.Target, ConfigIndexEntry> indexByTarget = ConfigIndexService.indexByTarget();
        List<ResolvedShortcut> resolved = new ArrayList<>();

        for (ShortcutEntry shortcut : shortcuts) {
            find(indexByTarget, shortcut).ifPresent(entry -> resolved.add(new ResolvedShortcut(shortcut, entry)));
        }

        return resolved;
    }

    public static Optional<ConfigIndexEntry> find(Map<ConfigIndexService.Target, ConfigIndexEntry> index,
            ShortcutEntry shortcut) {
        return Optional.ofNullable(index.get(new ConfigIndexService.Target(shortcut.modId(), shortcut.groupId(),
                shortcut.configName())));
    }
}
