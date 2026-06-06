package fastui.yure.client.index;

import fastui.yure.FastMasaConfig;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

        for (ModInfo modInfo : Registry.CONFIG_SCREEN.getAllModsWithConfigScreens()) {
            if (shouldIndexMod(modInfo.getModId()) == false) {
                continue;
            }

            try {
                GuiBase screen = modInfo.getConfigScreenSupplier() == null ? null : modInfo.getConfigScreenSupplier().get();

                if (screen instanceof IConfigGui configGui) {
                    collectScreenConfigs(result, modInfo, screen, configGui);
                }
            } catch (Exception e) {
                FastMasaConfig.LOGGER.warn("索引配置屏失败: {}", modInfo.getModId(), e);
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

    private static void collectScreenConfigs(List<ConfigIndexEntry> result, ModInfo modInfo, GuiBase screen, IConfigGui configGui) throws IllegalAccessException {
        Field tabField = findStaticTabField(screen.getClass());

        if (tabField == null) {
            collectConfigs(result, modInfo, "default", "Default", configGui.getConfigs());
            return;
        }

        tabField.setAccessible(true);
        Object originalTab = tabField.get(null);
        Object[] tabs = tabField.getType().getEnumConstants();

        if (tabs == null) {
            collectConfigs(result, modInfo, "default", "Default", configGui.getConfigs());
            return;
        }

        for (Object tab : tabs) {
            tabField.set(null, tab);
            collectConfigs(result, modInfo, getTabId(tab), getTabDisplayName(tab), configGui.getConfigs());
        }

        tabField.set(null, originalTab);
    }

    private static void collectConfigs(List<ConfigIndexEntry> result, ModInfo modInfo, String groupId, String groupName, List<GuiConfigsBase.ConfigOptionWrapper> wrappers) {
        for (GuiConfigsBase.ConfigOptionWrapper wrapper : wrappers) {
            IConfigBase config = wrapper.getConfig();

            if (config != null && isSupported(config)) {
                result.add(new ConfigIndexEntry(modInfo.getModId(), modInfo.getModName(), groupId, groupName, config.getName(), getDisplayName(config), config));
            }
        }
    }

    public static boolean isSupported(IConfigBase config) {
        ConfigType type = config.getType();
        return type == ConfigType.BOOLEAN || type == ConfigType.INTEGER || type == ConfigType.FLOAT || type == ConfigType.DOUBLE;
    }

    private static String getDisplayName(IConfigBase config) {
        String displayName = config.getConfigGuiDisplayName();
        return displayName == null || displayName.isBlank() ? config.getName() : displayName;
    }

    private static Field findStaticTabField(Class<?> screenClass) {
        for (Field field : screenClass.getDeclaredFields()) {
            int modifiers = field.getModifiers();

            if (Modifier.isStatic(modifiers) && field.getType().isEnum() && "tab".equals(field.getName())) {
                return field;
            }
        }

        return null;
    }

    private static String getTabId(Object tab) {
        return tab instanceof Enum<?> enumTab ? enumTab.name() : String.valueOf(tab);
    }

    private static String getTabDisplayName(Object tab) {
        try {
            Method method = tab.getClass().getMethod("getDisplayName");
            Object value = method.invoke(tab);

            if (value instanceof String stringValue && stringValue.isBlank() == false) {
                return stringValue;
            }
        } catch (ReflectiveOperationException ignored) {
            // 部分配置界面没有分组显示名方法，退回 enum 名即可。
        }

        return getTabId(tab);
    }
}
