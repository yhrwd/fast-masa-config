package fastui.yure.client.index;

import fastui.yure.FastMasaConfig;
import fastui.yure.client.scan.ConfigGuiGroupScanner;
import fastui.yure.client.scan.ConfigScreenSourceService;
import fastui.yure.config.FastMasaConfigs;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConfigIndexService {
    private static List<ConfigIndexEntry> cachedEntries;
    private static Map<Target, ConfigIndexEntry> cachedEntriesByTarget = Map.of();
    private static volatile long generation;

    private ConfigIndexService() {
    }

    public static synchronized List<ConfigIndexEntry> scanSupportedConfigs() {
        if (cachedEntries != null) {
            return cachedEntries;
        }

        List<ConfigIndexEntry> result = new ArrayList<>();
        Set<Target> indexedTargets = new HashSet<>();

        collectTaggedOwnConfigs(result, indexedTargets);

        for (ConfigScreenSourceService.Source source : ConfigScreenSourceService.collectSources()) {
            if (shouldIndexMod(source.modId()) == false) {
                continue;
            }

            try {
                collectScreenConfigs(result, indexedTargets, source);
            } catch (Exception e) {
                FastMasaConfig.LOGGER.warn("索引配置屏失败: {}", source.modId(), e);
            }
        }

        cachedEntries = List.copyOf(result);
        Map<Target, ConfigIndexEntry> entriesByTarget = new HashMap<>(result.size());
        for (ConfigIndexEntry entry : result) {
            entriesByTarget.put(new Target(entry.modId(), entry.groupId(), entry.configName()), entry);
            if (FastMasaConfig.MOD_ID.equals(entry.modId()) && "Generic".equals(entry.groupId())) {
                entriesByTarget.put(new Target(entry.modId(), FastMasaConfigs.QuickPanel.GROUP_ID,
                        entry.configName()), entry);
            }
        }
        cachedEntriesByTarget = Map.copyOf(entriesByTarget);
        generation++;
        return cachedEntries;
    }

    public static synchronized void invalidate() {
        cachedEntries = null;
        cachedEntriesByTarget = Map.of();
        generation++;
    }

    /** 索引重建或失效时递增，供长驻界面刷新本地引用。 */
    public static long generation() {
        return generation;
    }

    public static Map<Target, ConfigIndexEntry> indexByTarget() {
        scanSupportedConfigs();
        return cachedEntriesByTarget;
    }

    public static boolean shouldIndexMod(String modId) {
        return FastMasaConfig.MOD_ID.equals(modId) == false;
    }

    private static void collectScreenConfigs(List<ConfigIndexEntry> result, Set<Target> indexedTargets,
            ConfigScreenSourceService.Source source) {
        for (ConfigGuiGroupScanner.Group group : ConfigGuiGroupScanner.collectGroups(source.screen(), source.configGui())) {
            collectConfigs(result, indexedTargets, source, group.id(), group.displayName(), group.configs());
        }
    }

    private static void collectTaggedOwnConfigs(List<ConfigIndexEntry> result, Set<Target> indexedTargets) {
        for (IConfigBase config : FastMasaConfigs.QuickPanel.TAGGED_OPTIONS) {
            Target target = new Target(FastMasaConfig.MOD_ID, "Generic", config.getName());
            if (isSupported(config) && indexedTargets.add(target)) {
                result.add(new ConfigIndexEntry(FastMasaConfig.MOD_ID, "Fast Masa Config",
                        "Generic", "Fast Masa Config",
                        config.getName(), getDisplayName(config), config));
            }
        }
    }

    private static void collectConfigs(List<ConfigIndexEntry> result, Set<Target> indexedTargets,
            ConfigScreenSourceService.Source source, String groupId, String groupName,
            List<GuiConfigsBase.ConfigOptionWrapper> wrappers) {
        for (GuiConfigsBase.ConfigOptionWrapper wrapper : wrappers) {
            IConfigBase config = wrapper.getConfig();
            if (config != null && isSupported(config)
                    && indexedTargets.add(new Target(source.modId(), groupId, config.getName()))) {
                result.add(new ConfigIndexEntry(source.modId(), source.modName(), groupId, groupName, config.getName(), getDisplayName(config), config));
            }
        }
    }

    public static boolean isSupported(IConfigBase config) {
        return config instanceof fi.dy.masa.malilib.config.IConfigBoolean
                || config instanceof fi.dy.masa.malilib.config.IConfigInteger
                || config instanceof fi.dy.masa.malilib.config.IConfigFloat
                || config instanceof fi.dy.masa.malilib.config.IConfigDouble;
    }

    private static String getDisplayName(IConfigBase config) {
        String displayName = config.getConfigGuiDisplayName();
        return displayName == null || displayName.isBlank() ? config.getName() : displayName;
    }

    public record Target(String modId, String groupId, String configName) {
    }

}
