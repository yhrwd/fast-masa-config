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
        List<ConfigIndexEntry> index = ConfigIndexService.scanSupportedConfigs();
        List<ResolvedShortcut> resolved = new ArrayList<>();

        for (ShortcutEntry shortcut : shortcuts) {
            find(index, shortcut).ifPresent(entry -> resolved.add(new ResolvedShortcut(shortcut, entry)));
        }

        return resolved;
    }

    public static Optional<ConfigIndexEntry> find(List<ConfigIndexEntry> index, ShortcutEntry shortcut) {
        return index.stream()
                .filter(entry -> entry.modId().equals(shortcut.modId()))
                .filter(entry -> shortcut.groupId().isBlank() || entry.groupId().equals(shortcut.groupId()))
                .filter(entry -> entry.configName().equals(shortcut.configName()))
                .findFirst();
    }

    public static Optional<ConfigIndexEntry> find(Map<ConfigIndexService.Target, ConfigIndexEntry> index,
            ShortcutEntry shortcut) {
        if (index == null) {
            return Optional.empty();
        }
        if (!shortcut.groupId().isBlank()) {
            return Optional.ofNullable(index.get(new ConfigIndexService.Target(shortcut.modId(), shortcut.groupId(),
                    shortcut.configName())));
        }
        return index.entrySet().stream()
                .filter(entry -> entry.getKey().modId().equals(shortcut.modId()))
                .filter(entry -> entry.getKey().configName().equals(shortcut.configName()))
                .map(Map.Entry::getValue)
                .findFirst();
    }
}
