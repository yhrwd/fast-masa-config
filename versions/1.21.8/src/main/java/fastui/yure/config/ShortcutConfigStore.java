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
        if (containsTarget(entry.modId(), entry.groupId(), entry.configName())) {
            return false;
        }

        ENTRIES.add(entry);
        return true;
    }

    public static void remove(int index) {
        if (index >= 0 && index < ENTRIES.size()) {
            ENTRIES.remove(index);
        }
    }

    public static boolean move(int index, int offset) {
        int target = index + offset;

        if (index >= 0 && index < ENTRIES.size() && target >= 0 && target < ENTRIES.size()) {
            Collections.swap(ENTRIES, index, target);
            return true;
        }

        return false;
    }

    public static boolean containsTarget(String modId, String groupId, String configName) {
        return ENTRIES.stream().anyMatch(entry -> entry.isSameTarget(modId, groupId, configName));
    }

    public static void removeTarget(String modId, String groupId, String configName) {
        ENTRIES.removeIf(entry -> entry.isSameTarget(modId, groupId, configName));
    }

    public static List<String> toManualIds() {
        List<String> ids = new ArrayList<>();

        for (ShortcutEntry entry : ENTRIES) {
            ids.add(entry.manualId());
        }

        return ids;
    }

    public static void replaceWithManualIds(List<String> manualIds) {
        ENTRIES.clear();

        for (String manualId : manualIds) {
            if (manualId == null || manualId.isBlank()) {
                continue;
            }

            add(ShortcutEntry.fromManualId(manualId));
        }
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

        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                add(ShortcutEntry.fromJson(element.getAsJsonObject()));
            }
        }
    }
}
