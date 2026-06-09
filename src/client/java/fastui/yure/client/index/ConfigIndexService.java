package fastui.yure.client.index;

import fastui.yure.FastMasaConfig;
import fastui.yure.client.scan.ConfigGuiGroupScanner;
import fastui.yure.client.scan.ConfigScreenSourceService;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;

import java.util.ArrayList;
import java.util.List;

public final class ConfigIndexService {
    private static List<ConfigIndexEntry> cachedEntries;

    private ConfigIndexService() {
    }

    public static List<ConfigIndexEntry> scanSupportedConfigs() {
        if (cachedEntries != null) {
            return cachedEntries;
        }

        List<ConfigIndexEntry> result = new ArrayList<>();

        for (ConfigScreenSourceService.Source source : ConfigScreenSourceService.collectSources()) {
            if (shouldIndexMod(source.modId()) == false) {
                continue;
            }

            try {
                collectScreenConfigs(result, source);
            } catch (Exception e) {
                FastMasaConfig.LOGGER.warn("索引配置屏失败: {}", source.modId(), e);
            }
        }

        cachedEntries = List.copyOf(result);
        return cachedEntries;
    }

    public static void invalidate() {
        cachedEntries = null;
    }

    public static boolean shouldIndexMod(String modId) {
        return FastMasaConfig.MOD_ID.equals(modId) == false;
    }

    private static void collectScreenConfigs(List<ConfigIndexEntry> result, ConfigScreenSourceService.Source source) {
        for (ConfigGuiGroupScanner.Group group : ConfigGuiGroupScanner.collectGroups(source.screen(), source.configGui())) {
            collectConfigs(result, source, group.id(), group.displayName(), group.configs());
        }
    }

    private static void collectConfigs(List<ConfigIndexEntry> result, ConfigScreenSourceService.Source source, String groupId, String groupName, List<GuiConfigsBase.ConfigOptionWrapper> wrappers) {
        for (GuiConfigsBase.ConfigOptionWrapper wrapper : wrappers) {
            IConfigBase config = wrapper.getConfig();

            if (config != null && isSupported(config) && containsIndexedTarget(result, source.modId(), config.getName()) == false) {
                result.add(new ConfigIndexEntry(source.modId(), source.modName(), groupId, groupName, config.getName(), getDisplayName(config), config));
            }
        }
    }

    private static boolean containsIndexedTarget(List<ConfigIndexEntry> result, String modId, String configName) {
        return result.stream().anyMatch(entry -> entry.modId().equals(modId) && entry.configName().equals(configName));
    }

    public static boolean isSupported(IConfigBase config) {
        ConfigType type = config.getType();
        return type == ConfigType.BOOLEAN || type == ConfigType.INTEGER || type == ConfigType.FLOAT || type == ConfigType.DOUBLE;
    }

    private static String getDisplayName(IConfigBase config) {
        String displayName = config.getConfigGuiDisplayName();
        return displayName == null || displayName.isBlank() ? config.getName() : displayName;
    }

}
