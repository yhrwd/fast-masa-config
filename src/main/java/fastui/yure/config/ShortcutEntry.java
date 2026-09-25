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

    public boolean isSameTarget(String modId, String groupId, String configName) {
        return this.modId.equals(modId) && this.groupId.equals(groupId) && this.configName.equals(configName);
    }

    public boolean hasValidTarget() {
        return !this.modId.isBlank() && !this.configName.isBlank();
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
