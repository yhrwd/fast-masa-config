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

/** 快捷消息组和消息的持久化存储。不会创建默认组。 */
public final class QuickMessageStore {
    private static final List<QuickMessageGroup> GROUPS = new ArrayList<>();
    private static final Map<String, QuickMessageGroup> GROUPS_BY_ID = new HashMap<>();
    private static long revision;
    private static long contentRevision;

    private QuickMessageStore() {
    }

    public static QuickMessageGroup createGroup(String name) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("快捷消息组名称不能为空");
        }
        QuickMessageGroup group = new QuickMessageGroup(UUID.randomUUID().toString(), name.trim());
        int initialOffset = 20 + GROUPS.size() * 18;
        group.setWindowState(false, initialOffset, initialOffset);
        GROUPS.add(group);
        GROUPS_BY_ID.put(group.id(), group);
        revision++;
        contentRevision++;
        return group;
    }

    public static List<QuickMessageGroup> getGroups() {
        return Collections.unmodifiableList(GROUPS);
    }

    public static Optional<QuickMessageGroup> get(String id) {
        return Optional.ofNullable(GROUPS_BY_ID.get(id));
    }

    public static long revision() {
        return revision;
    }

    public static long contentRevision() {
        return contentRevision;
    }

    public static boolean renameGroup(String id, String name) {
        if (isBlank(name)) {
            return false;
        }
        QuickMessageGroup group = GROUPS_BY_ID.get(id);
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

    public static boolean removeGroup(String id) {
        QuickMessageGroup group = GROUPS_BY_ID.remove(id);
        if (group == null) {
            return false;
        }
        GROUPS.remove(group);
        revision++;
        contentRevision++;
        return true;
    }

    public static boolean hideGroup(String id, boolean hidden) {
        QuickMessageGroup group = GROUPS_BY_ID.get(id);
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
        contentRevision++;
        return true;
    }

    public static boolean setWindowState(String id, boolean collapsed, int x, int y) {
        QuickMessageGroup group = GROUPS_BY_ID.get(id);
        if (group == null) {
            return false;
        }
        if (group.collapsed() != collapsed || group.x() != x || group.y() != y) {
            group.setWindowState(collapsed, x, y);
        }
        return true;
    }

    public static QuickMessage addMessage(String groupId, String label, String content) {
        QuickMessageGroup group = GROUPS_BY_ID.get(groupId);
        if (group == null || isBlank(content)) {
            return null;
        }
        QuickMessage message;
        try {
            message = new QuickMessage(label, content);
        } catch (IllegalArgumentException exception) {
            return null;
        }
        if (!group.addMessage(message)) {
            return null;
        }
        revision++;
        contentRevision++;
        return message;
    }

    public static boolean updateMessage(String groupId, String messageId, String label, String content) {
        QuickMessageGroup group = GROUPS_BY_ID.get(groupId);
        if (group == null || isBlank(content) || !isUuid(messageId)) {
            return false;
        }
        QuickMessage message;
        try {
            message = new QuickMessage(messageId, label, content);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (!group.updateMessage(messageId, message)) {
            return false;
        }
        revision++;
        contentRevision++;
        return true;
    }

    public static boolean removeMessage(String groupId, int index) {
        QuickMessageGroup group = GROUPS_BY_ID.get(groupId);
        if (group == null || !group.removeMessage(index)) {
            return false;
        }
        revision++;
        contentRevision++;
        return true;
    }

    public static boolean moveMessage(String groupId, int index, int offset) {
        QuickMessageGroup group = GROUPS_BY_ID.get(groupId);
        if (group == null || !group.moveMessage(index, offset)) {
            return false;
        }
        revision++;
        contentRevision++;
        return true;
    }

    public static void clear() {
        if (GROUPS.isEmpty() && GROUPS_BY_ID.isEmpty()) {
            return;
        }
        GROUPS.clear();
        GROUPS_BY_ID.clear();
        revision++;
        contentRevision++;
    }

    public static JsonArray toJson() {
        JsonArray groups = new JsonArray();
        for (QuickMessageGroup group : GROUPS) {
            JsonObject object = new JsonObject();
            object.addProperty("id", group.id());
            object.addProperty("name", group.name());
            object.addProperty("hidden", group.hidden());
            object.addProperty("collapsed", group.collapsed());
            object.addProperty("x", group.x());
            object.addProperty("y", group.y());
            JsonArray messages = new JsonArray();
            for (QuickMessage message : group.messages()) {
                JsonObject messageObject = new JsonObject();
                messageObject.addProperty("id", message.id());
                messageObject.addProperty("label", message.label());
                messageObject.addProperty("content", message.content());
                messages.add(messageObject);
            }
            object.add("messages", messages);
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
            if (!isUuid(id) || isBlank(name) || GROUPS_BY_ID.containsKey(id)) {
                continue;
            }
            QuickMessageGroup group = new QuickMessageGroup(id, name.trim());
            group.setHidden(booleanValue(object, "hidden", false));
            group.setWindowState(booleanValue(object, "collapsed", false), intValue(object, "x", 0),
                    intValue(object, "y", 0));
            GROUPS.add(group);
            GROUPS_BY_ID.put(id, group);
            JsonElement messages = object.get("messages");
            if (messages != null && messages.isJsonArray()) {
                for (JsonElement messageElement : messages.getAsJsonArray()) {
                    if (!messageElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject messageObject = messageElement.getAsJsonObject();
                    String messageId = stringValue(messageObject, "id", "");
                    String content = stringValue(messageObject, "content", "");
                    if (!isUuid(messageId) || isBlank(content)) {
                        continue;
                    }
                    try {
                        group.addMessage(new QuickMessage(messageId, stringValue(messageObject, "label", ""), content));
                    } catch (IllegalArgumentException ignored) {
                        // 保留同组中的其它有效消息。
                    }
                }
            }
        }
        revision++;
        contentRevision++;
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
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String stringValue(JsonObject object, String key, String defaultValue) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : defaultValue;
    }

    private static int intValue(JsonObject object, String key, int defaultValue) {
        JsonElement value = object.get(key);
        try {
            return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                    ? value.getAsInt() : defaultValue;
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
