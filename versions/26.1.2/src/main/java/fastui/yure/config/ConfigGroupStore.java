package fastui.yure.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ConfigGroupStore {
    private static final String DEFAULT_GROUP_ID = "default";
    private static final String DEFAULT_GROUP_NAME = "FastUI";
    private static final List<ConfigGroup> GROUPS = new ArrayList<>();

    private ConfigGroupStore() {
    }

    public static ConfigGroup create(String name) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("分组名称不能为空");
        }

        ConfigGroup group = new ConfigGroup(UUID.randomUUID().toString(), name, false);
        GROUPS.add(group);
        return group;
    }

    public static ConfigGroup ensureDefaultGroup() {
        Optional<ConfigGroup> existing = get(DEFAULT_GROUP_ID);
        if (existing.isPresent()) {
            ConfigGroup group = existing.get();
            group.rename(DEFAULT_GROUP_NAME);
            group.setSystem(true);
            return group;
        }

        ConfigGroup group = new ConfigGroup(DEFAULT_GROUP_ID, DEFAULT_GROUP_NAME, true);
        GROUPS.add(group);
        return group;
    }

    public static List<ConfigGroup> getGroups() {
        return Collections.unmodifiableList(GROUPS);
    }

    public static Optional<ConfigGroup> get(String id) {
        return GROUPS.stream().filter(group -> group.id().equals(id)).findFirst();
    }

    public static boolean rename(String id, String name) {
        if (name == null || name.isBlank()) {
            return false;
        }

        Optional<ConfigGroup> group = get(id);
        group.ifPresent(value -> value.rename(name));
        return group.isPresent();
    }

    public static boolean remove(String id) {
        return GROUPS.removeIf(group -> group.id().equals(id) && !group.system());
    }

    public static boolean hide(String id, boolean hidden) {
        Optional<ConfigGroup> group = get(id);
        group.ifPresent(value -> value.setHidden(hidden));
        return group.isPresent();
    }

    public static boolean moveGroup(String id, int offset) {
        int index = indexOf(id);
        int target = index + offset;

        if (index < 0 || target < 0 || target >= GROUPS.size()) {
            return false;
        }

        Collections.swap(GROUPS, index, target);
        return true;
    }

    public static boolean addItem(String groupId, GroupItem item) {
        if (item == null || isBlank(item.modId()) || isBlank(item.configName())) {
            return false;
        }

        GroupItem normalized = item.groupId() == null
                ? new GroupItem(item.modId(), "", item.configName(), item.expanded())
                : item;
        return get(groupId).map(group -> group.addItem(normalized)).orElse(false);
    }

    public static boolean removeItem(String groupId, int index) {
        return get(groupId).map(group -> group.removeItem(index)).orElse(false);
    }

    public static boolean moveItem(String groupId, int index, int offset) {
        return get(groupId).map(group -> group.moveItem(index, offset)).orElse(false);
    }

    public static boolean setItemExpanded(String groupId, int index, boolean expanded) {
        return get(groupId).map(group -> group.setItemExpanded(index, expanded)).orElse(false);
    }

    public static boolean setWindowState(String groupId, boolean collapsed, int x, int y) {
        Optional<ConfigGroup> group = get(groupId);
        group.ifPresent(value -> value.setWindowState(collapsed, x, y));
        return group.isPresent();
    }

    public static void clear() {
        GROUPS.clear();
    }

    public static JsonArray toJson() {
        JsonArray groups = new JsonArray();

        for (ConfigGroup group : GROUPS) {
            JsonObject object = new JsonObject();
            object.addProperty("id", group.id());
            object.addProperty("name", group.name());
            object.addProperty("system", group.system());
            object.addProperty("hidden", group.hidden());
            object.addProperty("collapsed", group.collapsed());
            object.addProperty("x", group.x());
            object.addProperty("y", group.y());

            JsonArray items = new JsonArray();
            for (GroupItem item : group.items()) {
                JsonObject itemObject = new JsonObject();
                itemObject.addProperty("modId", item.modId());
                itemObject.addProperty("groupId", item.groupId());
                itemObject.addProperty("configName", item.configName());
                itemObject.addProperty("expanded", item.expanded());
                items.add(itemObject);
            }
            object.add("items", items);
            groups.add(object);
        }

        return groups;
    }

    public static void fromJson(JsonArray groups) {
        GROUPS.clear();
        if (groups == null) {
            return;
        }

        for (JsonElement element : groups) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject object = element.getAsJsonObject();
            String id = stringValue(object, "id", "");
            String name = stringValue(object, "name", "");
            if ((!DEFAULT_GROUP_ID.equals(id) && !isUuid(id))
                    || (!DEFAULT_GROUP_ID.equals(id) && isBlank(name)) || get(id).isPresent()) {
                continue;
            }

            boolean defaultGroup = DEFAULT_GROUP_ID.equals(id);
            ConfigGroup group = new ConfigGroup(id, defaultGroup ? DEFAULT_GROUP_NAME : name, defaultGroup);
            group.setHidden(booleanValue(object, "hidden", false));
            group.setWindowState(booleanValue(object, "collapsed", false), intValue(object, "x", 0),
                    intValue(object, "y", 0));
            GROUPS.add(group);

            JsonElement items = object.get("items");
            if (items != null && items.isJsonArray()) {
                for (JsonElement itemElement : items.getAsJsonArray()) {
                    if (!itemElement.isJsonObject()) {
                        continue;
                    }

                    JsonObject itemObject = itemElement.getAsJsonObject();
                    addItem(id, new GroupItem(stringValue(itemObject, "modId", ""),
                            stringValue(itemObject, "groupId", ""), stringValue(itemObject, "configName", ""),
                            booleanValue(itemObject, "expanded", false)));
                }
            }
        }
    }

    public static void migrateShortcutsIfEmpty(List<ShortcutEntry> shortcuts) {
        ConfigGroup group = ensureDefaultGroup();
        if (shortcuts == null || shortcuts.isEmpty()) {
            return;
        }

        for (ShortcutEntry shortcut : shortcuts) {
            if (shortcut != null && !isBlank(shortcut.modId()) && !isBlank(shortcut.configName())) {
                addItem(group.id(), new GroupItem(shortcut.modId(), shortcut.groupId(), shortcut.configName(), false));
            }
        }
    }

    private static int indexOf(String id) {
        for (int index = 0; index < GROUPS.size(); index++) {
            if (GROUPS.get(index).id().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String stringValue(JsonObject object, String key, String defaultValue) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return defaultValue;
        }
        return value.getAsString();
    }

    private static int intValue(JsonObject object, String key, int defaultValue) {
        JsonElement value = object.get(key);
        try {
            return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? value.getAsInt() : defaultValue;
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static boolean booleanValue(JsonObject object, String key, boolean defaultValue) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                ? value.getAsBoolean() : defaultValue;
    }
}
