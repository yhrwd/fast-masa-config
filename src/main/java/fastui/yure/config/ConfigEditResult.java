package fastui.yure.config;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.util.StringUtils;

import java.util.List;

/**
 * Carries a translation key with optional arguments instead of baked text,
 * so the failure message follows the active language when displayed.
 */
public record ConfigEditResult(boolean success, String configName, String messageKey, List<Object> messageArgs) {
    private static final List<Object> NO_ARGS = List.of();

    public static ConfigEditResult success(IConfigBase config) {
        return success(config.getName());
    }

    public static ConfigEditResult success(String configName) {
        return new ConfigEditResult(true, configName, "", NO_ARGS);
    }

    public static ConfigEditResult failure(IConfigBase config, String messageKey, Object... args) {
        return failure(config.getName(), messageKey, args);
    }

    public static ConfigEditResult failure(String configName, String messageKey, Object... args) {
        return new ConfigEditResult(false, configName, messageKey, List.of(args));
    }

    /** Resolves the message key against the active language; empty for successful edits. */
    public String translatedMessage() {
        return success || messageKey.isEmpty() ? "" : StringUtils.translate(messageKey, messageArgs.toArray());
    }
}
