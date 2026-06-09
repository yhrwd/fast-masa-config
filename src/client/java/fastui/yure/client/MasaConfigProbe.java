package fastui.yure.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IStringRepresentable;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.util.StringUtils;
import fastui.yure.FastMasaConfig;
import fastui.yure.client.scan.ConfigGuiGroupScanner;
import fastui.yure.client.scan.ConfigScreenSourceService;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class MasaConfigProbe {
    private static final String[] FALLBACK_CONFIG_CLASS_NAMES = {
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
                        .executes(MasaConfigProbe::scanRegistered)
                        .then(literal("csv")
                                .executes(MasaConfigProbe::exportRegisteredCsv))
                        .then(literal("fallback")
                                .executes(MasaConfigProbe::scanFallback)
                                .then(literal("csv")
                                        .executes(MasaConfigProbe::exportFallbackCsv)))));
    }

    private static int scanRegistered(CommandContext<FabricClientCommandSource> context) {
        List<ModConfigScan> registeredScans = scanRegisteredConfigScreens();

        FastMasaConfig.LOGGER.info("==== Fast Masa Config probe: registered MaLiLib config screens ====");
        logRegisteredScans(registeredScans);

        int registeredCount = registeredScans.stream()
                .flatMap(scan -> scan.groups().stream())
                .mapToInt(group -> group.entries().size())
                .sum();

        context.getSource().sendFeedback(Text.literal("Fast Masa Config scan complete. Registered entries: " + registeredCount + ". Use /fastmasaconfig scan fallback for fallback reflection."));
        return 1;
    }

    private static int scanFallback(CommandContext<FabricClientCommandSource> context) {
        Map<String, List<ConfigEntry>> fallbackEntries = scanFallbackConfigClasses();

        FastMasaConfig.LOGGER.info("==== Fast Masa Config probe: fallback reflected config classes ====");
        logFallbackEntries(fallbackEntries);

        int fallbackCount = fallbackEntries.values().stream().mapToInt(List::size).sum();

        context.getSource().sendFeedback(Text.literal("Fast Masa Config fallback scan complete. Fallback entries: " + fallbackCount + ". See client log."));
        return 1;
    }

    private static int exportRegisteredCsv(CommandContext<FabricClientCommandSource> context) {
        try {
            Path outputPath = exportRegisteredCsvFile();
            context.getSource().sendFeedback(Text.literal("Fast Masa Config CSV exported: " + outputPath.toAbsolutePath()));
            return 1;
        } catch (Exception e) {
            FastMasaConfig.LOGGER.error("Failed to export registered config CSV", e);
            context.getSource().sendError(Text.literal("Fast Masa Config CSV export failed. See client log."));
            return 0;
        }
    }

    public static Path exportRegisteredCsvFile() throws Exception {
        List<ModConfigScan> registeredScans = scanRegisteredConfigScreens();
        Path outputPath = getRunDirectory().resolve("fast-masa-config-scan.csv");
        writeRegisteredCsv(outputPath, registeredScans);
        return outputPath;
    }

    private static int exportFallbackCsv(CommandContext<FabricClientCommandSource> context) {
        Map<String, List<ConfigEntry>> fallbackEntries = scanFallbackConfigClasses();
        Path outputPath = getRunDirectory().resolve("fast-masa-config-fallback-scan.csv");

        try {
            writeFallbackCsv(outputPath, fallbackEntries);
            context.getSource().sendFeedback(Text.literal("Fast Masa Config fallback CSV exported: " + outputPath.toAbsolutePath()));
            return 1;
        } catch (Exception e) {
            FastMasaConfig.LOGGER.error("Failed to export fallback config CSV", e);
            context.getSource().sendError(Text.literal("Fast Masa Config fallback CSV export failed. See client log."));
            return 0;
        }
    }

    private static List<ModConfigScan> scanRegisteredConfigScreens() {
        List<ModConfigScan> result = new ArrayList<>();

        for (ConfigScreenSourceService.Source source : ConfigScreenSourceService.collectSources()) {
            try {
                result.add(new ModConfigScan(source.modId(), source.modName(), collectGroups(source.screen(), source.configGui())));
            } catch (Exception e) {
                FastMasaConfig.LOGGER.warn("Failed to scan config screen for mod [{}]", source.modId(), e);
            }
        }

        return result;
    }

    private static List<ConfigGroup> collectGroups(Object screen, IConfigGui configGui) {
        List<ConfigGroup> groups = new ArrayList<>();
        String screenSource = "gui:" + screen.getClass().getName();

        for (ConfigGuiGroupScanner.Group group : ConfigGuiGroupScanner.collectGroups(screen, configGui)) {
            groups.add(new ConfigGroup(group.id(), group.displayName(), collectConfigEntries(group.configs(), screenSource + ":" + group.sourceId())));
        }

        return groups;
    }

    private static List<ConfigEntry> collectConfigEntries(List<GuiConfigsBase.ConfigOptionWrapper> wrappers, String source) {
        List<ConfigEntry> entries = new ArrayList<>();

        for (GuiConfigsBase.ConfigOptionWrapper wrapper : wrappers) {
            IConfigBase config = wrapper.getConfig();

            if (config != null) {
                entries.add(ConfigEntry.from(config, source));
            }
        }

        return entries;
    }

    private static Map<String, List<ConfigEntry>> scanFallbackConfigClasses() {
        Map<String, List<ConfigEntry>> result = new LinkedHashMap<>();

        for (String className : FALLBACK_CONFIG_CLASS_NAMES) {
            try {
                Class<?> clazz = Class.forName(className);
                List<ConfigEntry> entries = new ArrayList<>();
                collectFromClass(clazz, entries, new LinkedHashSet<>());

                if (entries.isEmpty() == false) {
                    result.put(className, entries);
                }
            } catch (ClassNotFoundException ignored) {
                FastMasaConfig.LOGGER.debug("Fallback config class [{}] is not loaded because its mod is absent", className);
            } catch (Exception e) {
                FastMasaConfig.LOGGER.warn("Failed to reflect fallback config class [{}]", className, e);
            }
        }

        return result;
    }

    private static void collectFromClass(Class<?> clazz, List<ConfigEntry> entries, Set<Object> seenObjects) throws IllegalAccessException {
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) == false) {
                continue;
            }

            field.setAccessible(true);
            collectFromValue(field.get(null), entries, seenObjects, clazz.getName() + "#" + field.getName());
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
        if (value == null || seenObjects.add(value) == false) {
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
                collectFromValue(method.invoke(value), entries, seenObjects, source + "." + methodName + "()");
            }
        } catch (ReflectiveOperationException ignored) {
            // 不是所有枚举都会暴露派生配置对象，这里只作为降级探测。
        }
    }

    private static void logRegisteredScans(List<ModConfigScan> scans) {
        for (ModConfigScan scan : scans) {
            int count = scan.groups().stream().mapToInt(group -> group.entries().size()).sum();
            FastMasaConfig.LOGGER.info("{} ({})：{} config entries", scan.modName(), scan.modId(), count);

            for (ConfigGroup group : scan.groups()) {
                FastMasaConfig.LOGGER.info("  [{}] {}：{} entries", group.id(), group.displayName(), group.entries().size());

                for (ConfigEntry entry : group.entries()) {
                    FastMasaConfig.LOGGER.info("    [{}] {} -> {} = {}", entry.type(), entry.name(), entry.displayName(), entry.value());
                }
            }
        }
    }

    private static void logFallbackEntries(Map<String, List<ConfigEntry>> groupedEntries) {
        for (Map.Entry<String, List<ConfigEntry>> group : groupedEntries.entrySet()) {
            FastMasaConfig.LOGGER.info("{}：{} config entries", group.getKey(), group.getValue().size());

            for (ConfigEntry entry : group.getValue()) {
                FastMasaConfig.LOGGER.info("  [{}] {} -> {} = {} ({})", entry.type(), entry.name(), entry.displayName(), entry.value(), entry.source());
            }
        }
    }

    private static void writeRegisteredCsv(Path outputPath, List<ModConfigScan> scans) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("kind,mod_id,mod_name,group_id,group_name,type,name,display_name,value,source");

        for (ModConfigScan scan : scans) {
            for (ConfigGroup group : scan.groups()) {
                for (ConfigEntry entry : group.entries()) {
                    lines.add(toCsvLine(
                            "registered",
                            scan.modId(),
                            scan.modName(),
                            group.id(),
                            group.displayName(),
                            entry.type(),
                            entry.name(),
                            entry.displayName(),
                            entry.value(),
                            entry.source()
                    ));
                }
            }
        }

        Files.write(outputPath, lines, StandardCharsets.UTF_8);
    }

    private static void writeFallbackCsv(Path outputPath, Map<String, List<ConfigEntry>> groupedEntries) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("kind,mod_id,mod_name,group_id,group_name,type,name,display_name,value,source");

        for (Map.Entry<String, List<ConfigEntry>> group : groupedEntries.entrySet()) {
            for (ConfigEntry entry : group.getValue()) {
                lines.add(toCsvLine(
                        "fallback",
                        "",
                        "",
                        group.getKey(),
                        group.getKey(),
                        entry.type(),
                        entry.name(),
                        entry.displayName(),
                        entry.value(),
                        entry.source()
                ));
            }
        }

        Files.write(outputPath, lines, StandardCharsets.UTF_8);
    }

    private static Path getRunDirectory() {
        return MinecraftClient.getInstance().runDirectory.toPath();
    }

    private static String toCsvLine(String... values) {
        List<String> escapedValues = new ArrayList<>(values.length);

        for (String value : values) {
            escapedValues.add(escapeCsv(value));
        }

        return String.join(",", escapedValues);
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        String escapedValue = value.replace("\"", "\"\"");

        if (escapedValue.contains(",") || escapedValue.contains("\"") || escapedValue.contains("\n") || escapedValue.contains("\r")) {
            return "\"" + escapedValue + "\"";
        }

        return escapedValue;
    }

    private record ModConfigScan(String modId, String modName, List<ConfigGroup> groups) {
    }

    private record ConfigGroup(String id, String displayName, List<ConfigEntry> entries) {
    }

    private record ConfigEntry(String name, String displayName, String type, String value, String source) {
        static ConfigEntry from(IConfigBase config, String source) {
            ConfigType type = config.getType();
            return new ConfigEntry(config.getName(), getDisplayName(config), type == null ? "UNKNOWN" : type.name(), getValue(config), source);
        }

        private static String getDisplayName(IConfigBase config) {
            String displayName = config.getConfigGuiDisplayName();

            if (displayName != null && displayName.isBlank() == false && displayName.equals(config.getName()) == false) {
                return displayName;
            }

            String translatedName = config.getTranslatedName();

            if (translatedName != null && translatedName.isBlank() == false && translatedName.equals(config.getName()) == false) {
                return translatedName;
            }

            return StringUtils.getTranslatedOrFallback(config.getName(), config.getName());
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
