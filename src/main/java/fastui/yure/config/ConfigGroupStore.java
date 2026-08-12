package fastui.yure.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ConfigGroupStore {
    private static final String DEFAULT_GROUP_ID = "default";
    private static final String DEFAULT_GROUP_NAME = "FastUI";
    private static final List<ConfigGroup> GROUPS = new ArrayList<>();
    private static final Map<String, ConfigGroup> GROUPS_BY_ID = new HashMap<>();
    private static long revision;
    private static long contentRevision;

    private ConfigGroupStore() {
    }

    public static ConfigGroup create(String name) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("分组名称不能为空");
        }

        ConfigGroup group = new ConfigGroup(UUID.randomUUID().toString(), name.trim(), false);
        GROUPS.add(group);
        GROUPS_BY_ID.put(group.id(), group);
        revision++;
        contentRevision++;
        return group;
    }

    public static ConfigGroup ensureDefaultGroup() {
        Optional<ConfigGroup> existing = get(DEFAULT_GROUP_ID);
        if (existing.isPresent()) {
            ConfigGroup group = existing.get();
            if (!DEFAULT_GROUP_NAME.equals(group.name()) || !group.system()) {
                group.rename(DEFAULT_GROUP_NAME);
                group.setSystem(true);
                revision++;
                contentRevision++;
            }
            return group;
        }

        ConfigGroup group = new ConfigGroup(DEFAULT_GROUP_ID, DEFAULT_GROUP_NAME, true);
        GROUPS.add(group);
        GROUPS_BY_ID.put(group.id(), group);
        revision++;
        contentRevision++;
        return group;
    }

    public static List<ConfigGroup> getGroups() {
        return Collections.unmodifiableList(GROUPS);
    }

    public static Optional<ConfigGroup> get(String id) {
        return Optional.ofNullable(GROUPS_BY_ID.get(id));
    }

    /** 组集合、可见性或排序发生变化时递增，供客户端避免空闲帧重复同步。 */
    public static long revision() {
        return revision;
    }

    /** 仅在名称或条目发生变化时递增，不包含拖动和隐藏等窗口状态。 */
    public static long contentRevision() {
        return contentRevision;
    }

    public static boolean rename(String id, String name) {
        if (name == null || name.isBlank()) {
            return false;
        }

        ConfigGroup group = GROUPS_BY_ID.get(id);
        if (group == null) {
            return false;
        }
        String normalized = name.trim();
        if (!normalized.equals(group.name())) {
            group.rename(normalized);
            revision++;
            contentRevision++;
        }
        return true;
    }

    public static boolean remove(String id) {
        ConfigGroup group = GROUPS_BY_ID.get(id);
        if (group == null || group.system()) {
            return false;
        }
        GROUPS.remove(group);
        GROUPS_BY_ID.remove(id);
        revision++;
        contentRevision++;
        return true;
    }

    public static boolean hide(String id, boolean hidden) {
        ConfigGroup group = GROUPS_BY_ID.get(id);
        if (group == null) {
            return false;
        }
        if (group.hidden() != hidden) {
            group.setHidden(hidden);
            revision++;
        }
        return true;
    }

    public static boolean moveGroup(String id, int offset) {
        int index = indexOf(id);
        int target = index + offset;

        if (index < 0 || target < 0 || target >= GROUPS.size()) {
            return false;
        }

        Collections.swap(GROUPS, index, target);
        revision++;
        return true;
    }

    public static boolean addItem(String groupId, GroupItem item) {
        if (item == null || isBlank(item.modId()) || isBlank(item.configName())) {
            return false;
        }

        GroupItem normalized = item.groupId() == null
                ? new GroupItem(item.modId(), "", item.configName(), item.expanded())
                : item;
        ConfigGroup group = GROUPS_BY_ID.get(groupId);
        if (group == null || !group.addItem(normalized)) {
            return false;
        }
        revision++;
        contentRevision++;
        return true;
    }

    public static boolean removeItem(String groupId, int index) {
        ConfigGroup group = GROUPS_BY_ID.get(groupId);
        if (group == null || !group.removeItem(index)) {
            return false;
        }
        revision++;
        contentRevision++;
        return true;
    }

    public static boolean moveItem(String groupId, int index, int offset) {
        ConfigGroup group = GROUPS_BY_ID.get(groupId);
        if (group == null || !group.moveItem(index, offset)) {
            return false;
        }
        revision++;
        contentRevision++;
        return true;
    }

    public static boolean setItemExpanded(String groupId, int index, boolean expanded) {
        ConfigGroup group = GROUPS_BY_ID.get(groupId);
        if (group == null || index < 0 || index >= group.items().size()) {
            return false;
        }
        if (group.items().get(index).expanded() != expanded) {
            group.setItemExpanded(index, expanded);
            revision++;
            contentRevision++;
        }
        return true;
    }

    public static boolean setWindowState(String groupId, boolean collapsed, int x, int y) {
        ConfigGroup group = GROUPS_BY_ID.get(groupId);
        if (group == null) {
            return false;
        }
        if (group.collapsed() != collapsed || group.x() != x || group.y() != y) {
            group.setWindowState(collapsed, x, y);
        }
        return true;
    }

    public static void clear() {
        if (GROUPS.isEmpty()) {
            return;
        }
        GROUPS.clear();
        GROUPS_BY_ID.clear();
        revision++;
        contentRevision++;
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
        clear();
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
            ConfigGroup group = new ConfigGroup(id, defaultGroup ? DEFAULT_GROUP_NAME : name.trim(), defaultGroup);
            group.setHidden(booleanValue(object, "hidden", false));
            group.setWindowState(booleanValue(object, "collapsed", false), intValue(object, "x", 0),
                    intValue(object, "y", 0));
            GROUPS.add(group);
            GROUPS_BY_ID.put(group.id(), group);
            revision++;
            contentRevision++;

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
