package fastui.yure.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import java.nio.file.Path;

/**
 * MaLiLib 0.2x compatibility for the shared main sources: JsonUtils still
 * lives in fi.dy.masa.malilib.util, its file helpers take java.io.File, and
 * ConfigType renders through the yarn StringIdentifiable#asString.
 */
public final class MalilibCompat
{
    private MalilibCompat() {}

    public static JsonElement parseJsonFile(Path file)
    {
        return JsonUtils.parseJsonFile(file.toFile());
    }

    public static boolean writeJsonToFile(JsonElement root, Path file)
    {
        return JsonUtils.writeJsonToFile((JsonObject) root, file.toFile());
    }

    public static String getStringOrDefault(JsonObject object, String key, String defaultValue)
    {
        return JsonUtils.getStringOrDefault(object, key, defaultValue);
    }

    public static double getDoubleOrDefault(JsonObject object, String key, double defaultValue)
    {
        return JsonUtils.getDoubleOrDefault(object, key, defaultValue);
    }

    public static double getDouble(JsonObject object, String key)
    {
        return JsonUtils.getDouble(object, key);
    }

    public static boolean hasDouble(JsonObject object, String key)
    {
        return JsonUtils.hasDouble(object, key);
    }

    public static void createDirectoriesIfMissing(Path dir)
    {
        FileUtils.createDirectoriesIfMissing(dir);
    }

    public static Path getConfigDirectory()
    {
        return FileUtils.getConfigDirectory().toPath();
    }

    public static String configTypeString(fi.dy.masa.malilib.config.ConfigType type)
    {
        return type.asString();
    }
}
