package fastui.yure.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ShortcutConfigStore {
    private static final List<ShortcutEntry> ENTRIES = new ArrayList<>();

    private ShortcutConfigStore() {
    }

    public static List<ShortcutEntry> getEntries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static boolean add(ShortcutEntry entry) {
        if (entry == null || !entry.hasValidTarget()) {
            return false;
        }

        if (containsTarget(entry.modId(), entry.groupId(), entry.configName())) {
            return false;
        }

        ENTRIES.add(entry);
        return true;
    }

    private static boolean containsTarget(String modId, String groupId, String configName) {
        return ENTRIES.stream().anyMatch(entry -> entry.isSameTarget(modId, groupId, configName));
    }

    public static void clear() {
        ENTRIES.clear();
    }

    public static JsonArray toJson() {
        JsonArray array = new JsonArray();

        for (ShortcutEntry entry : ENTRIES) {
            array.add(entry.toJson());
        }

        return array;
    }

    public static void fromJson(JsonArray array) {
        ENTRIES.clear();

        if (array == null) {
            return;
        }

        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                add(ShortcutEntry.fromJson(element.getAsJsonObject()));
            }
        }
    }
}
