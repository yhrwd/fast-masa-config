package fastui.yure.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IStringRepresentable;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import fastui.yure.FastMasaConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class MasaConfigProbe {
    private static final String[] KNOWN_CONFIG_CLASS_NAMES = {
            "fi.dy.masa.tweakeroo.config.Configs",
            "fi.dy.masa.tweakeroo.config.Hotkeys",
            "fi.dy.masa.tweakeroo.config.FeatureToggle",
            "fi.dy.masa.minihud.config.Configs",
            "fi.dy.masa.minihud.config.InfoToggle",
            "fi.dy.masa.minihud.config.RendererToggle",
            "fi.dy.masa.minihud.config.StructureToggle"
    };

    private MasaConfigProbe() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("fastmasaconfig")
                .then(literal("scan")
                        .executes(MasaConfigProbe::scan)));
    }

    private static int scan(CommandContext<FabricClientCommandSource> context) {
        Map<String, List<ConfigEntry>> guiEntries = scanRegisteredConfigScreens();
        Map<String, List<ConfigEntry>> reflectedEntries = scanKnownConfigClasses();

        FastMasaConfig.LOGGER.info("==== Fast Masa Config probe: registered config screens ====");
        logEntries(guiEntries);

        FastMasaConfig.LOGGER.info("==== Fast Masa Config probe: reflected config classes ====");
        logEntries(reflectedEntries);

        int guiCount = guiEntries.values().stream().mapToInt(List::size).sum();
        int reflectedCount = reflectedEntries.values().stream().mapToInt(List::size).sum();

        context.getSource().sendFeedback(Text.literal("Fast Masa Config scan complete. GUI entries: " + guiCount + ", reflected entries: " + reflectedCount + ". See client log."));
        return 1;
    }

    private static Map<String, List<ConfigEntry>> scanRegisteredConfigScreens() {
        Map<String, List<ConfigEntry>> result = new LinkedHashMap<>();

        for (ModInfo modInfo : Registry.CONFIG_SCREEN.getAllModsWithConfigScreens()) {
            List<ConfigEntry> entries = new ArrayList<>();

            try {
                GuiBase screen = modInfo.getConfigScreenSupplier() == null ? null : modInfo.getConfigScreenSupplier().get();

                if (screen instanceof IConfigGui configGui) {
                    for (GuiConfigsBase.ConfigOptionWrapper wrapper : configGui.getConfigs()) {
                        IConfigBase config = wrapper.getConfig();

                        if (config != null) {
                            entries.add(ConfigEntry.from(config, "gui:" + screen.getClass().getName()));
                        }
                    }
                }
            } catch (Exception e) {
                FastMasaConfig.LOGGER.warn("Failed to scan config screen for mod [{}]", modInfo.getModId(), e);
            }

            result.put(modInfo.getModId(), entries);
        }

        return result;
    }

    private static Map<String, List<ConfigEntry>> scanKnownConfigClasses() {
        Map<String, List<ConfigEntry>> result = new LinkedHashMap<>();

        for (String className : KNOWN_CONFIG_CLASS_NAMES) {
            try {
                Class<?> clazz = Class.forName(className);
                List<ConfigEntry> entries = new ArrayList<>();
                collectFromClass(clazz, entries, new LinkedHashSet<>());

                if (!entries.isEmpty()) {
                    result.put(className, entries);
                }
            } catch (ClassNotFoundException ignored) {
                FastMasaConfig.LOGGER.debug("Config class [{}] is not loaded because its mod is absent", className);
            } catch (Exception e) {
                FastMasaConfig.LOGGER.warn("Failed to reflect config class [{}]", className, e);
            }
        }

        return result;
    }

    private static void collectFromClass(Class<?> clazz, List<ConfigEntry> entries, Set<Object> seenObjects) throws IllegalAccessException {
        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);
            Object value = field.get(null);
            collectFromValue(value, entries, seenObjects, clazz.getName() + "#" + field.getName());
        }

        for (Class<?> nestedClass : clazz.getDeclaredClasses()) {
            collectFromClass(nestedClass, entries, seenObjects);
        }

        if (clazz.isEnum()) {
            Object[] enumConstants = clazz.getEnumConstants();

            if (enumConstants != null) {
                for (Object constant : enumConstants) {
                    collectFromValue(constant, entries, seenObjects, clazz.getName() + "#enum");
                }
            }
        }
    }

    private static void collectFromValue(Object value, List<ConfigEntry> entries, Set<Object> seenObjects, String source) {
        if (value == null || !seenObjects.add(value)) {
            return;
        }

        if (value instanceof IConfigBase config) {
            entries.add(ConfigEntry.from(config, source));
            return;
        }

        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectFromValue(item, entries, seenObjects, source);
            }

            return;
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);

            for (int index = 0; index < length; index++) {
                collectFromValue(Array.get(value, index), entries, seenObjects, source);
            }

            return;
        }

        collectConfigLikeMethod(value, entries, seenObjects, source, "getToggleOption");
        collectConfigLikeMethod(value, entries, seenObjects, source, "getHotkey");
        collectConfigLikeMethod(value, entries, seenObjects, source, "getColorMain");
        collectConfigLikeMethod(value, entries, seenObjects, source, "getColorComponents");
    }

    private static void collectConfigLikeMethod(Object value, List<ConfigEntry> entries, Set<Object> seenObjects, String source, String methodName) {
        try {
            Method method = value.getClass().getMethod(methodName);

            if (IConfigBase.class.isAssignableFrom(method.getReturnType())) {
                Object returnedValue = method.invoke(value);
                collectFromValue(returnedValue, entries, seenObjects, source + "." + methodName + "()");
            }
        } catch (ReflectiveOperationException ignored) {
            // 不是所有枚举都会暴露派生配置对象，这里只探测常见 MaLiLib 写法。
        }
    }

    private static void logEntries(Map<String, List<ConfigEntry>> groupedEntries) {
        for (Map.Entry<String, List<ConfigEntry>> group : groupedEntries.entrySet()) {
            FastMasaConfig.LOGGER.info("{}: {} config entries", group.getKey(), group.getValue().size());

            for (ConfigEntry entry : group.getValue()) {
                FastMasaConfig.LOGGER.info("  [{}] {} = {} ({})", entry.type(), entry.name(), entry.value(), entry.source());
            }
        }
    }

    private record ConfigEntry(String name, String type, String value, String source) {
        static ConfigEntry from(IConfigBase config, String source) {
            return new ConfigEntry(config.getName(), config.getType().name(), getValue(config), source);
        }

        private static String getValue(IConfigBase config) {
            if (config instanceof IStringRepresentable stringRepresentable) {
                return stringRepresentable.getStringValue();
            }

            if (config instanceof IHotkey hotkey) {
                return hotkey.getKeybind().getStringValue();
            }

            return "<not-string-representable>";
        }
    }
}
