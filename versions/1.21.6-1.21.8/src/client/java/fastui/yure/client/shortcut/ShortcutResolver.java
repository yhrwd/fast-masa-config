package fastui.yure.client.shortcut;

import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.client.index.ConfigIndexService;
import fastui.yure.config.ShortcutEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        if (shortcut.groupId() == null || shortcut.groupId().isBlank()) {
            List<ConfigIndexEntry> matches = index.stream()
                    .filter(entry -> Objects.equals(entry.modId(), shortcut.modId()))
                    .filter(entry -> Objects.equals(entry.configName(), shortcut.configName()))
                    .limit(2)
                    .toList();
            return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
        }

        return index.stream()
                .filter(entry -> shortcut.isSameTarget(entry.modId(), entry.groupId(), entry.configName()))
                .findFirst();
    }
}
