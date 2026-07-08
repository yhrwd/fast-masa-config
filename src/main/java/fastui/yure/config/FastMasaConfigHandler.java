package fastui.yure.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fastui.yure.FastMasaConfig;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;

import java.nio.file.Files;
import java.nio.file.Path;

public final class FastMasaConfigHandler implements IConfigHandler {
    private static final String CONFIG_FILE_NAME = FastMasaConfig.MOD_ID + ".json";
    private static final int CONFIG_VERSION = 1;

    @Override
    public void load() {
        // 本 handler 只读取 fast-masa-config.json；首次安装时文件不存在就保持默认值，不碰其它 MaLiLib 模组配置。
        Path configFile = FileUtils.getConfigDirectory().resolve(CONFIG_FILE_NAME);

        if (Files.exists(configFile) && Files.isReadable(configFile)) {
            JsonElement element = JsonUtils.parseJsonFile(configFile);

            if (element != null && element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();
                ConfigUtils.readConfigBase(root, "Generic", FastMasaConfigs.Generic.OPTIONS);

                if (root.has("Shortcuts") && root.get("Shortcuts").isJsonArray()) {
                    ShortcutConfigStore.fromJson(root.getAsJsonArray("Shortcuts"));
                }
            } else {
                FastMasaConfig.LOGGER.error("无法读取配置文件: {}", configFile.toAbsolutePath());
            }
        }
    }

    @Override
    public void save() {
        // 保存也只写 fast-masa-config.json，不会覆盖 tweakeroo/minihud/malilib 自己的配置文件。
        Path dir = FileUtils.getConfigDirectory();

        if (!Files.exists(dir)) {
            FileUtils.createDirectoriesIfMissing(dir);
        }

        if (Files.isDirectory(dir)) {
            JsonObject root = new JsonObject();
            ConfigUtils.writeConfigBase(root, "Generic", FastMasaConfigs.Generic.OPTIONS);
            root.add("Shortcuts", ShortcutConfigStore.toJson());
            root.add("config_version", new JsonPrimitive(CONFIG_VERSION));
            JsonUtils.writeJsonToFile(root, dir.resolve(CONFIG_FILE_NAME));
        } else {
            FastMasaConfig.LOGGER.error("配置目录不存在: {}", dir.toAbsolutePath());
        }
    }
}
