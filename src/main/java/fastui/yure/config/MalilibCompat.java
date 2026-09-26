package fastui.yure.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import java.nio.file.Path;



/**
 * Isolates the MaLiLib JSON helper API surface so the version-independent
 * main sources compile for every target. Targets whose MaLiLib line keeps
 * JsonUtils in another package override this class.
 */
public final class MalilibCompat
{
    private MalilibCompat() {}

    public static JsonElement parseJsonFile(Path file)
    {
        return JsonUtils.parseJsonFile(file);
    }

    public static boolean writeJsonToFile(JsonElement root, Path file)
    {
        return JsonUtils.writeJsonToFile(root, file);
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

    public static void createDirectoriesIfMissing(java.nio.file.Path dir)
    {
        fi.dy.masa.malilib.util.FileUtils.createDirectoriesIfMissing(dir);
    }

    public static Path getConfigDirectory()
    {
        return fi.dy.masa.malilib.util.FileUtils.getConfigDirectory();
    }

    public static String configTypeString(fi.dy.masa.malilib.config.ConfigType type)
    {
        return type.getSerializedName();
    }
}
