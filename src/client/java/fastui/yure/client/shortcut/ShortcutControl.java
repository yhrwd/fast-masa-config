package fastui.yure.client.shortcut;

import fastui.yure.config.MaLiLibConfigCommitter;
import fastui.yure.config.MasaConfigEditor;
import fastui.yure.config.ShortcutControlType;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.IConfigFloat;
import fi.dy.masa.malilib.config.IConfigInteger;

public final class ShortcutControl {
    private static final MasaConfigEditor EDITOR = new MasaConfigEditor();
    private static final MaLiLibConfigCommitter COMMITTER = new MaLiLibConfigCommitter();

    private ShortcutControl() {
    }

    public static ShortcutControlType getControlType(IConfigBase config) {
        return config.getType() == ConfigType.BOOLEAN ? ShortcutControlType.TOGGLE : ShortcutControlType.SLIDER;
    }

    public static String getValueText(IConfigBase config) {
        return switch (config.getType()) {
            case BOOLEAN -> Boolean.toString(((IConfigBoolean) config).getBooleanValue());
            case INTEGER -> Integer.toString(((IConfigInteger) config).getIntegerValue());
            case FLOAT -> Float.toString(((IConfigFloat) config).getFloatValue());
            case DOUBLE -> Double.toString(((IConfigDouble) config).getDoubleValue());
            default -> "";
        };
    }

    public static boolean getBooleanValue(IConfigBase config) {
        return config instanceof IConfigBoolean booleanConfig && booleanConfig.getBooleanValue();
    }

    public static double getSliderRatio(IConfigBase config) {
        double min = getMin(config);
        double max = getMax(config);

        if (max <= min) {
            return 0.0;
        }

        return Math.max(0.0, Math.min(1.0, (getValue(config) - min) / (max - min)));
    }

    public static void toggle(ResolvedShortcut shortcut) {
        IConfigBase config = shortcut.configEntry().config();

        if (config instanceof IConfigBoolean booleanConfig) {
            // 直接写目标 mod 的 MaLiLib 配置对象，再用目标 modId 提交通知，保证目标配置页和配置文件同步。
            EDITOR.apply(config, Boolean.toString(booleanConfig.getBooleanValue() == false));
            EDITOR.commit(shortcut.configEntry().modId(), COMMITTER);
        }
    }

    public static void setSliderValue(ResolvedShortcut shortcut, double ratio) {
        IConfigBase config = shortcut.configEntry().config();
        double min = shortcut.shortcut().minOverride() == null ? getMin(config) : shortcut.shortcut().minOverride();
        double max = shortcut.shortcut().maxOverride() == null ? getMax(config) : shortcut.shortcut().maxOverride();
        double step = Math.max(0.000001, shortcut.shortcut().sliderStep());
        double rawValue = min + (Math.max(0.0, Math.min(1.0, ratio)) * (max - min));
        double steppedValue = Math.round(rawValue / step) * step;

        // 滑条同样提交到目标 modId，而不是 fast-masa-config 自己，避免各 mod 之间状态不同步。
        EDITOR.apply(config, formatValue(config, Math.max(min, Math.min(max, steppedValue))));
        EDITOR.commit(shortcut.configEntry().modId(), COMMITTER);
    }

    private static double getValue(IConfigBase config) {
        return switch (config.getType()) {
            case INTEGER -> ((IConfigInteger) config).getIntegerValue();
            case FLOAT -> ((IConfigFloat) config).getFloatValue();
            case DOUBLE -> ((IConfigDouble) config).getDoubleValue();
            default -> 0.0;
        };
    }

    private static double getMin(IConfigBase config) {
        return switch (config.getType()) {
            case INTEGER -> ((IConfigInteger) config).getMinIntegerValue();
            case FLOAT -> ((IConfigFloat) config).getMinFloatValue();
            case DOUBLE -> ((IConfigDouble) config).getMinDoubleValue();
            default -> 0.0;
        };
    }

    private static double getMax(IConfigBase config) {
        return switch (config.getType()) {
            case INTEGER -> ((IConfigInteger) config).getMaxIntegerValue();
            case FLOAT -> ((IConfigFloat) config).getMaxFloatValue();
            case DOUBLE -> ((IConfigDouble) config).getMaxDoubleValue();
            default -> 1.0;
        };
    }

    private static String formatValue(IConfigBase config, double value) {
        return config.getType() == ConfigType.INTEGER ? Integer.toString((int) Math.round(value)) : Double.toString(value);
    }
}
