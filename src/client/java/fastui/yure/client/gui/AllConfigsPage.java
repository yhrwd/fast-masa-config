package fastui.yure.client.gui;

import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.client.index.ConfigIndexService;
import fastui.yure.config.GroupItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/** 全部配置页复用的目标映射和文本筛选逻辑。 */
final class AllConfigsPage {
    private AllConfigsPage() {
    }

    static boolean matches(ConfigIndexEntry entry, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        filter = filter.toLowerCase(Locale.ROOT);
        String haystack = (entry.modId() + " " + entry.modName() + " " + entry.groupId() + " " + entry.groupName()
                + " " + entry.configName() + " " + entry.displayName()).toLowerCase(Locale.ROOT);
        return haystack.contains(filter);
    }

    static Map<ConfigIndexService.Target, Integer> buildGroupItemOrder(List<GroupItem> items) {
        Map<ConfigIndexService.Target, Integer> result = new HashMap<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            GroupItem item = items.get(index);
            result.putIfAbsent(new ConfigIndexService.Target(item.modId(), item.groupId(), item.configName()), index);
        }
        return Map.copyOf(result);
    }

    static ConfigIndexService.Target targetOf(ConfigIndexEntry entry) {
        return new ConfigIndexService.Target(entry.modId(), entry.groupId(), entry.configName());
    }
}
