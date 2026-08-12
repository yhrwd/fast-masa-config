package fastui.yure.client.gui;

import fi.dy.masa.malilib.config.IConfigBase;

import java.util.List;
import java.util.Locale;

/** 通用配置页的纯列表状态与筛选逻辑。绘制和控件生命周期仍由主 Screen 管理。 */
final class GenericConfigPage {
    private GenericConfigPage() {
    }

    static List<IConfigBase> filter(List<IConfigBase> configs, String filter) {
        return configs.stream().filter(config -> matches(config, filter)).toList();
    }

    static boolean matches(IConfigBase config, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        filter = filter.toLowerCase(Locale.ROOT);
        String haystack = (config.getName() + " " + config.getConfigGuiDisplayName() + " " + config.getComment())
                .toLowerCase(Locale.ROOT);
        return haystack.contains(filter);
    }
}
