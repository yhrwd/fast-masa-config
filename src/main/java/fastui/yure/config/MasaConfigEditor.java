package fastui.yure.config;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigColor;
import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.IConfigFloat;
import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.IConfigOptionList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.IConfigResettable;
import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.config.IStringRepresentable;
import fi.dy.masa.malilib.hotkeys.IHotkey;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class MasaConfigEditor {
    private static final Pattern COLOR_PATTERN = Pattern.compile("#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})");

    private static final String ERR_UNSUPPORTED_TYPE = "fast-masa-config.edit.error.unsupported_type";
    private static final String ERR_RESET_UNSUPPORTED = "fast-masa-config.edit.error.reset_unsupported";
    private static final String ERR_BOOLEAN_INVALID = "fast-masa-config.edit.error.boolean_invalid";
    private static final String ERR_INTEGER_INVALID = "fast-masa-config.edit.error.integer_invalid";
    private static final String ERR_INTEGER_RANGE = "fast-masa-config.edit.error.integer_range";
    private static final String ERR_NUMBER_INVALID = "fast-masa-config.edit.error.number_invalid";
    private static final String ERR_NUMBER_NOT_FINITE = "fast-masa-config.edit.error.number_not_finite";
    private static final String ERR_NUMBER_RANGE = "fast-masa-config.edit.error.number_range";
    private static final String ERR_COLOR_INVALID = "fast-masa-config.edit.error.color_invalid";
    private static final String ERR_OPTION_UNKNOWN = "fast-masa-config.edit.error.option_unknown";

    public ConfigEditResult apply(IConfigBase config, String rawValue) {
        if (config instanceof IConfigBoolean) return applyBoolean(config, rawValue);
        if (config instanceof IConfigInteger) return applyInteger(config, rawValue);
        if (config instanceof IConfigDouble) return applyDouble(config, rawValue);
        if (config instanceof IConfigFloat) return applyFloat(config, rawValue);

        return switch (config.getType()) {
            case BOOLEAN -> applyBoolean(config, rawValue);
            case INTEGER -> applyInteger(config, rawValue);
            case DOUBLE -> applyDouble(config, rawValue);
            case FLOAT -> applyFloat(config, rawValue);
            case COLOR -> applyColor(config, rawValue);
            case STRING -> applyString(config, rawValue);
            case OPTION_LIST -> applyOptionList(config, rawValue);
            case HOTKEY -> applyHotkey(config, rawValue);
            case STRING_LIST -> applyStringList(config, rawValue);
            default -> ConfigEditResult.failure(config, ERR_UNSUPPORTED_TYPE,
                    MalilibCompat.configTypeString(config.getType()));
        };
    }

    public ConfigEditResult reset(IConfigBase config) {
        if (config instanceof IConfigResettable resettable) {
            resettable.resetToDefault();
            return ConfigEditResult.success(config);
        }

        if (config instanceof IConfigStringList stringList) {
            stringList.setStrings(stringList.getDefaultStrings());
            return ConfigEditResult.success(config);
        }

        return ConfigEditResult.failure(config, ERR_RESET_UNSUPPORTED);
    }

    private ConfigEditResult applyBoolean(IConfigBase config, String rawValue) {
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);

        if ("true".equals(normalized) == false && "false".equals(normalized) == false) {
            return ConfigEditResult.failure(config, ERR_BOOLEAN_INVALID);
        }

        ((IConfigBoolean) config).setBooleanValue(Boolean.parseBoolean(normalized));
        return ConfigEditResult.success(config);
    }

    private ConfigEditResult applyInteger(IConfigBase config, String rawValue) {
        IConfigInteger integerConfig = (IConfigInteger) config;
        int value;

        try {
            value = Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException e) {
            return ConfigEditResult.failure(config, ERR_INTEGER_INVALID);
        }

        if (value < integerConfig.getMinIntegerValue() || value > integerConfig.getMaxIntegerValue()) {
            return ConfigEditResult.failure(config, ERR_INTEGER_RANGE,
                    integerConfig.getMinIntegerValue(), integerConfig.getMaxIntegerValue());
        }

        integerConfig.setIntegerValue(value);
        return ConfigEditResult.success(config);
    }

    private ConfigEditResult applyDouble(IConfigBase config, String rawValue) {
        IConfigDouble doubleConfig = (IConfigDouble) config;
        double value;

        try {
            value = Double.parseDouble(rawValue.trim());
        } catch (NumberFormatException e) {
            return ConfigEditResult.failure(config, ERR_NUMBER_INVALID);
        }

        if (Double.isFinite(value) == false) {
            return ConfigEditResult.failure(config, ERR_NUMBER_NOT_FINITE);
        }

        if (value < doubleConfig.getMinDoubleValue() || value > doubleConfig.getMaxDoubleValue()) {
            return ConfigEditResult.failure(config, ERR_NUMBER_RANGE,
                    doubleConfig.getMinDoubleValue(), doubleConfig.getMaxDoubleValue());
        }

        doubleConfig.setDoubleValue(value);
        return ConfigEditResult.success(config);
    }

    private ConfigEditResult applyFloat(IConfigBase config, String rawValue) {
        IConfigFloat floatConfig = (IConfigFloat) config;
        float value;

        try {
            value = Float.parseFloat(rawValue.trim());
        } catch (NumberFormatException e) {
            return ConfigEditResult.failure(config, ERR_NUMBER_INVALID);
        }

        if (Float.isFinite(value) == false) {
            return ConfigEditResult.failure(config, ERR_NUMBER_NOT_FINITE);
        }

        if (value < floatConfig.getMinFloatValue() || value > floatConfig.getMaxFloatValue()) {
            return ConfigEditResult.failure(config, ERR_NUMBER_RANGE,
                    floatConfig.getMinFloatValue(), floatConfig.getMaxFloatValue());
        }

        floatConfig.setFloatValue(value);
        return ConfigEditResult.success(config);
    }

    private ConfigEditResult applyColor(IConfigBase config, String rawValue) {
        String value = rawValue.trim();

        if (COLOR_PATTERN.matcher(value).matches() == false) {
            return ConfigEditResult.failure(config, ERR_COLOR_INVALID);
        }

        ((IConfigColor) config).setValueFromString(value);
        return ConfigEditResult.success(config);
    }

    private ConfigEditResult applyString(IConfigBase config, String rawValue) {
        ((IStringRepresentable) config).setValueFromString(rawValue);
        return ConfigEditResult.success(config);
    }

    private ConfigEditResult applyOptionList(IConfigBase config, String rawValue) {
        IConfigOptionList optionConfig = (IConfigOptionList) config;
        String value = rawValue.trim();
        IConfigOptionListEntry currentValue = optionConfig.getOptionListValue();
        IConfigOptionListEntry nextValue = currentValue.fromString(value);

        if (nextValue == currentValue && currentValue.getStringValue().equals(value) == false) {
            return ConfigEditResult.failure(config, ERR_OPTION_UNKNOWN, value);
        }

        optionConfig.setOptionListValue(nextValue);
        return ConfigEditResult.success(config);
    }

    private ConfigEditResult applyHotkey(IConfigBase config, String rawValue) {
        ((IHotkey) config).getKeybind().setValueFromString(rawValue.trim());
        return ConfigEditResult.success(config);
    }

    private ConfigEditResult applyStringList(IConfigBase config, String rawValue) {
        List<String> values = rawValue.lines().toList();
        ((IConfigStringList) config).setStrings(values);
        return ConfigEditResult.success(config);
    }
}
