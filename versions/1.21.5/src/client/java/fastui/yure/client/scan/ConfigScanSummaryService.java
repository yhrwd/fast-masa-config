package fastui.yure.client.scan;

import fastui.yure.FastMasaConfig;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;

import java.util.HashSet;
import java.util.Set;

public final class ConfigScanSummaryService {
    private ConfigScanSummaryService() {
    }

    public static ConfigScanSummary scanRegisteredScreens() {
        int modCount = 0;
        int configCount = 0;

        for (ConfigScreenSourceService.Source source : ConfigScreenSourceService.collectSources()) {
            try {
                modCount++;
                configCount += countUniqueConfigs(source);
            } catch (Exception e) {
                FastMasaConfig.LOGGER.warn("统计配置屏失败: {}", source.modId(), e);
            }
        }

        return new ConfigScanSummary(modCount, configCount);
    }

    private static int countUniqueConfigs(ConfigScreenSourceService.Source source) {
        Set<String> configNames = new HashSet<>();

        for (ConfigGuiGroupScanner.Group group : ConfigGuiGroupScanner.collectGroups(source.screen(), source.configGui())) {
            for (GuiConfigsBase.ConfigOptionWrapper wrapper : group.configs()) {
                IConfigBase config = wrapper.getConfig();

                if (config != null) {
                    configNames.add(config.getName());
                }
            }
        }

        return configNames.size();
    }
}
