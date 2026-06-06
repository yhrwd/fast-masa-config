package fastui.yure.config;

import fi.dy.masa.malilib.config.IConfigBase;

public record ConfigEditResult(boolean success, String configName, String message) {
    public static ConfigEditResult success(IConfigBase config) {
        return new ConfigEditResult(true, config.getName(), "");
    }

    public static ConfigEditResult success(String configName) {
        return new ConfigEditResult(true, configName, "");
    }

    public static ConfigEditResult failure(IConfigBase config, String message) {
        return new ConfigEditResult(false, config.getName(), message);
    }

    public static ConfigEditResult failure(String configName, String message) {
        return new ConfigEditResult(false, configName, message);
    }
}
