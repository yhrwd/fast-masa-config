package fastui.yure.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fastui.yure.FastMasaConfig;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigHandler;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public final class FastMasaConfigHandler implements IConfigHandler {
    private static final String CONFIG_FILE_NAME = FastMasaConfig.MOD_ID + ".json";
    private static final int CONFIG_VERSION = 4;
    private static final DateTimeFormatter BROKEN_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /**
     * load() 发现现有配置文件无法解析（或不可读）时置位：save() 拒绝覆写，
     * 直到下次 load() 成功解析为止，避免用默认值摧毁用户的原始数据。
     */
    private static volatile boolean protectUnparsedFile;

    @Override
    public void load() {
        // 本 handler 只读取 fast-masa-config.json；首次安装时文件不存在就保持默认值，不碰其它 MaLiLib 模组配置。
        loadFromDirectory(MalilibCompat.getConfigDirectory());
    }

    static void loadFromDirectory(Path dir) {
        loadFromDirectory(dir, MalilibCompat::parseJsonFile);
    }

    /** parseFile 参数隔离 MaLiLib 的 JsonUtils（其静态初始化依赖游戏环境），单测可注入替身。 */
    static void loadFromDirectory(Path dir, Function<Path, JsonElement> parseFile) {
        loadFromDirectory(dir, parseFile, FastMasaConfigHandler::applyConfigRoot);
    }

    static void loadFromDirectory(Path dir, Function<Path, JsonElement> parseFile, Consumer<JsonObject> applyRoot) {
        ConfigGroupStore.clear();
        ShortcutConfigStore.clear();
        QuickMessageStore.clear();
        protectUnparsedFile = true;
        Path configFile = dir.resolve(CONFIG_FILE_NAME);

        try {
            if (Files.notExists(configFile)) {
                protectUnparsedFile = false;
            } else if (Files.isRegularFile(configFile) == false || Files.isReadable(configFile) == false) {
                FastMasaConfig.LOGGER.error("配置文件不可读，已禁止覆盖: {}", configFile.toAbsolutePath());
            } else {
                JsonElement element = parseFile.apply(configFile);
                if (element == null || element.isJsonObject() == false) {
                    throw new IllegalArgumentException("Config root must be a JSON object");
                }
                JsonObject root = element.getAsJsonObject();
                validateRoot(root);
                applyRoot.accept(root);
                protectUnparsedFile = false;
            }
        } catch (RuntimeException e) {
            FastMasaConfig.LOGGER.error("无法加载配置，已禁止覆盖: {}", configFile.toAbsolutePath(), e);
            backUpBrokenConfig(configFile);
        } finally {
            ConfigGroupStore.ensureDefaultGroup();
        }
    }

    private static void validateRoot(JsonObject root) {
        for (String section : List.of("Generic", "Tools")) {
            if (root.has(section) && !root.get(section).isJsonObject()) {
                throw new IllegalArgumentException("Expected object section: " + section);
            }
        }
        for (String section : List.of("Groups", "Shortcuts", "QuickMessageGroups")) {
            if (root.has(section) && !root.get(section).isJsonArray()) {
                throw new IllegalArgumentException("Expected array section: " + section);
            }
        }
    }

    static void applyConfigRoot(JsonObject root) {
        ConfigUtils.readConfigBase(root, "Generic", FastMasaConfigs.Generic.PERSISTED_OPTIONS);
        ConfigUtils.readConfigBase(root, "Tools", FastMasaConfigs.Tools.OPTIONS);

        loadShortcuts(root);
        loadGroups(root, ShortcutConfigStore.getEntries());
        loadQuickMessageGroups(root);
    }

    /** 把无法解析的配置文件复制为带时间戳的备份，给用户留一条手工恢复的路。 */
    static void backUpBrokenConfig(Path configFile) {
        String timestamp = LocalDateTime.now().format(BROKEN_TIMESTAMP);
        Path backup = configFile.resolveSibling(CONFIG_FILE_NAME + ".broken-" + timestamp + "-" + UUID.randomUUID() + ".json");

        try {
            Files.copy(configFile, backup);
            FastMasaConfig.LOGGER.error("已将无法解析的配置文件备份到 {}", backup.toAbsolutePath());
        } catch (IOException | RuntimeException e) {
            FastMasaConfig.LOGGER.error("备份无法解析的配置文件失败: {}", configFile.toAbsolutePath(), e);
        }
    }

    static boolean isProtectingUnparsedFile() {
        return protectUnparsedFile;
    }

    @Override
    public void save() {
        // 保存也只写 fast-masa-config.json，不会覆盖 tweakeroo/minihud/malilib 自己的配置文件。
        Path dir = MalilibCompat.getConfigDirectory();

        if (!Files.exists(dir)) {
            MalilibCompat.createDirectoriesIfMissing(dir);
        }

        if (Files.isDirectory(dir) == false) {
            FastMasaConfig.LOGGER.error("配置目录不存在: {}", dir.toAbsolutePath());
            return;
        }

        saveTo(dir.resolve(CONFIG_FILE_NAME));
    }

    static void saveTo(Path configFile) {
        saveTo(configFile, root -> MalilibCompat.writeJsonToFile(root, configFile));
    }

    /** writeFile 参数与 parseFile 对称：单测用它拦截真实落盘并断言保护逻辑。 */
    static void saveTo(Path configFile, Consumer<JsonObject> writeFile) {
        if (protectUnparsedFile) {
            FastMasaConfig.LOGGER.error(
                    "配置文件此前无法解析，已跳过保存以保护原始数据；请先手工修复或删除 {} 后重启",
                    configFile.toAbsolutePath());
            return;
        }

        writeFile.accept(buildRoot());
    }

    static JsonObject buildRoot() {
        JsonObject root = new JsonObject();
        ConfigUtils.writeConfigBase(root, "Generic", FastMasaConfigs.Generic.OPTIONS);
        ConfigUtils.writeConfigBase(root, "Tools", FastMasaConfigs.Tools.OPTIONS);
        root.add("Shortcuts", ShortcutConfigStore.toJson());
        root.add("Groups", ConfigGroupStore.toJson());
        root.add("QuickMessageGroups", QuickMessageStore.toJson());
        root.add("config_version", new JsonPrimitive(CONFIG_VERSION));
        return root;
    }

    static void loadQuickMessageGroups(JsonObject root) {
        JsonElement groups = root == null ? null : root.get("QuickMessageGroups");
        QuickMessageStore.fromJson(groups != null && groups.isJsonArray() ? groups.getAsJsonArray() : null);
    }

    static void loadShortcuts(JsonObject root) {
        ShortcutConfigStore.clear();
        JsonElement shortcuts = root == null ? null : root.get("Shortcuts");
        if (shortcuts != null && shortcuts.isJsonArray()) {
            ShortcutConfigStore.fromJson(shortcuts.getAsJsonArray());
        }
    }

    static void loadGroups(JsonObject root, List<ShortcutEntry> shortcuts) {
        JsonElement groups = root == null ? null : root.get("Groups");
        if (groups != null && groups.isJsonArray()) {
            ConfigGroupStore.fromJson(groups.getAsJsonArray());
        }

        // An explicitly empty Groups array is authoritative, even if legacy Shortcuts remain.
        if (groups == null || !groups.isJsonArray()
                || (groups.getAsJsonArray().size() > 0 && ConfigGroupStore.getGroups().isEmpty())) {
            ConfigGroupStore.migrateShortcutsIfEmpty(shortcuts);
        }
    }
}
