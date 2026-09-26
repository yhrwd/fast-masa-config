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

    public ConfigEditResult apply(IConfigBase config, String rawValue) {
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
            default -> ConfigEditResult.failure(config, "暂不支持修改该配置类型: " + config.getType().asString());
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

        return ConfigEditResult.failure(config, "该配置不支持重置");
    }

    public ConfigEditResult commit(String modId, ConfigCommitter committer) {
        if (modId == null || modId.isBlank()) {
            return ConfigEditResult.failure("<commit>", "modId 不能为空");
        }

        committer.commit(modId);
        return ConfigEditResult.success("<commit>");
    }

    private ConfigEditResult applyBoolean(IConfigBase config, String rawValue) {
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);

        if ("true".equals(normalized) == false && "false".equals(normalized) == false) {
            return ConfigEditResult.failure(config, "布尔值只能是 true 或 false");
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
            return ConfigEditResult.failure(config, "请输入整数");
        }

        if (value < integerConfig.getMinIntegerValue() || value > integerConfig.getMaxIntegerValue()) {
            return ConfigEditResult.failure(config, "整数超出范围: " + integerConfig.getMinIntegerValue() + " ~ " + integerConfig.getMaxIntegerValue());
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
            return ConfigEditResult.failure(config, "请输入数字");
        }

        if (Double.isFinite(value) == false) {
            return ConfigEditResult.failure(config, "数字不能是 NaN 或 Infinity");
        }

        if (value < doubleConfig.getMinDoubleValue() || value > doubleConfig.getMaxDoubleValue()) {
            return ConfigEditResult.failure(config, "数字超出范围: " + doubleConfig.getMinDoubleValue() + " ~ " + doubleConfig.getMaxDoubleValue());
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
            return ConfigEditResult.failure(config, "请输入数字");
        }

        if (Float.isFinite(value) == false) {
            return ConfigEditResult.failure(config, "数字不能是 NaN 或 Infinity");
        }

        if (value < floatConfig.getMinFloatValue() || value > floatConfig.getMaxFloatValue()) {
            return ConfigEditResult.failure(config, "数字超出范围: " + floatConfig.getMinFloatValue() + " ~ " + floatConfig.getMaxFloatValue());
        }

        floatConfig.setFloatValue(value);
        return ConfigEditResult.success(config);
    }

    private ConfigEditResult applyColor(IConfigBase config, String rawValue) {
        String value = rawValue.trim();

        if (COLOR_PATTERN.matcher(value).matches() == false) {
            return ConfigEditResult.failure(config, "颜色必须是 #RRGGBB 或 #AARRGGBB");
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
            return ConfigEditResult.failure(config, "未知选项: " + value);
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
