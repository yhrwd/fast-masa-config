package fastui.yure.config;

import com.google.gson.JsonObject;
import fi.dy.masa.malilib.util.data.json.JsonUtils;

public record ShortcutEntry(
        String modId,
        String groupId,
        String configName,
        String labelOverride,
        ShortcutControlType controlType,
        double sliderStep,
        Double minOverride,
        Double maxOverride) {
    private static final double DEFAULT_SLIDER_STEP = 1.0;

    public ShortcutEntry {
        modId = normalizedText(modId);
        groupId = normalizedText(groupId);
        configName = normalizedText(configName);
        labelOverride = normalizedText(labelOverride);
        controlType = controlType == null ? ShortcutControlType.TOGGLE : controlType;
        sliderStep = Double.isFinite(sliderStep) && sliderStep > 0.0 ? sliderStep : DEFAULT_SLIDER_STEP;
        minOverride = finiteOrNull(minOverride);
        maxOverride = finiteOrNull(maxOverride);
    }

    public static ShortcutEntry fromManualId(String rawId) {
        if (rawId == null) {
            throw new IllegalArgumentException("快捷方式 ID 不能为空");
        }

        String value = rawId.trim();

        if (value.contains("/")) {
            String[] parts = value.split("/", 3);

            if (parts.length == 3 && !parts[0].isBlank() && !parts[1].isBlank() && !parts[2].isBlank()) {
                return new ShortcutEntry(parts[0], parts[1], parts[2], "", ShortcutControlType.TOGGLE, 1.0, null, null);
            }
        }

        if (value.contains(":")) {
            String[] parts = value.split(":", 2);

            if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                return new ShortcutEntry(parts[0], "", parts[1], "", ShortcutControlType.TOGGLE, 1.0, null, null);
            }
        }

        throw new IllegalArgumentException("快捷方式 ID 必须是 modId/groupId/configName 或 modId:configName");
    }

    public boolean isSameTarget(String modId, String groupId, String configName) {
        return this.modId.equals(modId) && this.groupId.equals(groupId) && this.configName.equals(configName);
    }

    public boolean hasValidTarget() {
        return !this.modId.isBlank() && !this.configName.isBlank();
    }

    public String manualId() {
        return this.groupId.isBlank() ? this.modId + ":" + this.configName
                : this.modId + "/" + this.groupId + "/" + this.configName;
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("modId", this.modId);
        object.addProperty("groupId", this.groupId);
        object.addProperty("configName", this.configName);
        object.addProperty("labelOverride", this.labelOverride);
        object.addProperty("controlType", this.controlType.id());
        object.addProperty("sliderStep", this.sliderStep);

        if (this.minOverride != null) {
            object.addProperty("minOverride", this.minOverride);
        }

        if (this.maxOverride != null) {
            object.addProperty("maxOverride", this.maxOverride);
        }

        return object;
    }

    public static ShortcutEntry fromJson(JsonObject object) {
        return new ShortcutEntry(
                JsonUtils.getStringOrDefault(object, "modId", ""),
                JsonUtils.getStringOrDefault(object, "groupId", ""),
                JsonUtils.getStringOrDefault(object, "configName", ""),
                JsonUtils.getStringOrDefault(object, "labelOverride", ""),
                ShortcutControlType.fromId(JsonUtils.getStringOrDefault(object, "controlType", "toggle")),
                JsonUtils.getDoubleOrDefault(object, "sliderStep", 1.0),
                JsonUtils.hasDouble(object, "minOverride") ? JsonUtils.getDouble(object, "minOverride") : null,
                JsonUtils.hasDouble(object, "maxOverride") ? JsonUtils.getDouble(object, "maxOverride") : null);
    }

    private static String normalizedText(String value) {
        return value == null ? "" : value.trim();
    }

    private static Double finiteOrNull(Double value) {
        return value != null && Double.isFinite(value) ? value : null;
    }
}
