package fastui.yure.client.shortcut;

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
        return getSliderRatio(config, rangeFor(config));
    }

    public static double getSliderRatio(ResolvedShortcut shortcut) {
        IConfigBase config = shortcut.configEntry().config();
        return getSliderRatio(config, rangeFor(shortcut, config));
    }

    public static void toggle(ResolvedShortcut shortcut) {
        IConfigBase config = shortcut.configEntry().config();

        if (config instanceof IConfigBoolean booleanConfig) {
            // Keep the same runtime semantics as MaLiLib's native toggle hotkeys.
            EDITOR.apply(config, Boolean.toString(booleanConfig.getBooleanValue() == false));
        }
    }

    public static void setSliderValue(ResolvedShortcut shortcut, double ratio) {
        IConfigBase config = shortcut.configEntry().config();
        if (!isNumeric(config)) {
            return;
        }

        NumericRange range = rangeFor(shortcut, config);
        double step = Math.max(0.000001, shortcut.shortcut().sliderStep());
        double rawValue = range.min() + (clampRatio(ratio) * range.width());
        double steppedValue = Math.round(rawValue / step) * step;

        // Avoid ConfigManager.onConfigsChanged(): its default handler saves and reloads the target mod's config.
        EDITOR.apply(config, formatValue(config, range.clamp(steppedValue)));
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

    private static NumericRange rangeFor(IConfigBase config) {
        return new NumericRange(getMin(config), getMax(config));
    }

    private static NumericRange rangeFor(ResolvedShortcut shortcut, IConfigBase config) {
        NumericRange configRange = rangeFor(config);
        Double minOverride = shortcut.shortcut().minOverride();
        Double maxOverride = shortcut.shortcut().maxOverride();
        double min = minOverride == null ? configRange.min() : Math.max(configRange.min(), minOverride);
        double max = maxOverride == null ? configRange.max() : Math.min(configRange.max(), maxOverride);

        return min <= max ? new NumericRange(min, max) : configRange;
    }

    private static double getSliderRatio(IConfigBase config, NumericRange range) {
        return range.width() <= 0.0 ? 0.0 : clampRatio((getValue(config) - range.min()) / range.width());
    }

    private static boolean isNumeric(IConfigBase config) {
        return config.getType() == ConfigType.INTEGER || config.getType() == ConfigType.FLOAT
                || config.getType() == ConfigType.DOUBLE;
    }

    private static double clampRatio(double ratio) {
        return Math.max(0.0, Math.min(1.0, ratio));
    }

    private static String formatValue(IConfigBase config, double value) {
        return config.getType() == ConfigType.INTEGER ? Integer.toString((int) Math.round(value)) : Double.toString(value);
    }

    private record NumericRange(double min, double max) {
        double width() {
            return this.max - this.min;
        }

        double clamp(double value) {
            return Math.max(this.min, Math.min(this.max, value));
        }
    }
}
