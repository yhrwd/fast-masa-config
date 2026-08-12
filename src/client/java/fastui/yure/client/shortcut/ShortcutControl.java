package fastui.yure.client.shortcut;

import fastui.yure.FastMasaConfig;
import fastui.yure.config.MasaConfigEditor;
import fastui.yure.config.ShortcutControlType;
import fi.dy.masa.malilib.config.ConfigManager;
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
        return config instanceof IConfigBoolean ? ShortcutControlType.TOGGLE : ShortcutControlType.SLIDER;
    }

    public static String getValueText(IConfigBase config) {
        if (config instanceof IConfigBoolean value) return Boolean.toString(value.getBooleanValue());
        if (config instanceof IConfigInteger value) return Integer.toString(value.getIntegerValue());
        if (config instanceof IConfigFloat value) return Float.toString(value.getFloatValue());
        if (config instanceof IConfigDouble value) return Double.toString(value.getDoubleValue());
        return "";
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
            persistOwnConfigIfChanged(shortcut,
                    EDITOR.apply(config, Boolean.toString(booleanConfig.getBooleanValue() == false)).success());
        }
    }

    public static void setSliderValue(ResolvedShortcut shortcut, double ratio) {
        IConfigBase config = shortcut.configEntry().config();
        if (!isNumeric(config)) {
            return;
        }

        NumericRange range = rangeFor(shortcut, config);
        double step = config instanceof IConfigInteger ? Math.max(1.0, shortcut.shortcut().sliderStep()) : 0.1;
        double rawValue = range.min() + (clampRatio(ratio) * range.width());
        double steppedValue = Math.round(rawValue / step) * step;

        // Avoid ConfigManager.onConfigsChanged(): its default handler saves and reloads the target mod's config.
        persistOwnConfigIfChanged(shortcut, EDITOR.apply(config, formatValue(config, range.clamp(steppedValue))).success());
    }

    public static boolean setTypedValue(ResolvedShortcut shortcut, String rawValue) {
        IConfigBase config = shortcut.configEntry().config();
        if (!isNumeric(config)) {
            return false;
        }

        double value;
        try {
            value = Double.parseDouble(rawValue.trim().replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return false;
        }
        if (!Double.isFinite(value)) {
            return false;
        }

        NumericRange range = rangeFor(shortcut, config);
        value = range.clamp(value);
        String normalized;
        if (config instanceof IConfigInteger) {
            normalized = Integer.toString((int) value);
        } else if (config instanceof IConfigFloat) {
            normalized = Float.toString((float) value);
        } else {
            normalized = Double.toString(value);
        }
        boolean changed = EDITOR.apply(config, normalized).success();
        persistOwnConfigIfChanged(shortcut, changed);
        return changed;
    }

    public static boolean reset(ResolvedShortcut shortcut) {
        boolean changed = EDITOR.reset(shortcut.configEntry().config()).success();
        persistOwnConfigIfChanged(shortcut, changed);
        return changed;
    }

    private static double getValue(IConfigBase config) {
        if (config instanceof IConfigInteger value) return value.getIntegerValue();
        if (config instanceof IConfigFloat value) return value.getFloatValue();
        if (config instanceof IConfigDouble value) return value.getDoubleValue();
        return 0.0;
    }

    private static double getMin(IConfigBase config) {
        if (config instanceof IConfigInteger value) return value.getMinIntegerValue();
        if (config instanceof IConfigFloat value) return value.getMinFloatValue();
        if (config instanceof IConfigDouble value) return value.getMinDoubleValue();
        return 0.0;
    }

    private static double getMax(IConfigBase config) {
        if (config instanceof IConfigInteger value) return value.getMaxIntegerValue();
        if (config instanceof IConfigFloat value) return value.getMaxFloatValue();
        if (config instanceof IConfigDouble value) return value.getMaxDoubleValue();
        return 1.0;
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

    public static boolean isNumeric(IConfigBase config) {
        return config instanceof IConfigInteger || config instanceof IConfigFloat || config instanceof IConfigDouble;
    }

    private static double clampRatio(double ratio) {
        return Math.max(0.0, Math.min(1.0, ratio));
    }

    private static String formatValue(IConfigBase config, double value) {
        return config instanceof IConfigInteger ? Integer.toString((int) Math.round(value)) : Double.toString(value);
    }

    private static void persistOwnConfigIfChanged(ResolvedShortcut shortcut, boolean changed) {
        if (changed && FastMasaConfig.MOD_ID.equals(shortcut.configEntry().modId())) {
            ConfigManager.getInstance().onConfigsChanged(FastMasaConfig.MOD_ID);
        }
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
