package fastui.yure.client.shortcut;

import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.config.ConfigEditResult;
import fastui.yure.config.MasaConfigEditor;
import fastui.yure.config.ShortcutControlType;
import fastui.yure.config.ShortcutEntry;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.IConfigFloat;
import fi.dy.masa.malilib.config.IConfigInteger;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortcutControlTest {
    @Test
    void repeatedIntegerDragOnlyAssignsWhenTheSteppedValueChanges() {
        NumericConfig config = new NumericConfig(IConfigInteger.class, 0, 10, 0);
        ResolvedShortcut shortcut = config.shortcut(1.0, null, null);

        assertTrue(ShortcutControl.applySliderValue(shortcut, 0.54));
        assertEquals(5, config.value);
        assertFalse(ShortcutControl.applySliderValue(shortcut, 0.51));
        assertEquals(1, config.assignments);
        assertTrue(ShortcutControl.applySliderValue(shortcut, 2.0));
        assertEquals(10, config.value);
    }

    @Test
    void floatDragComparesTheAssignedFloatRatherThanIntermediateDouble() {
        NumericConfig config = new NumericConfig(IConfigFloat.class, 0, 1, 0);
        ResolvedShortcut shortcut = config.shortcut(1.0, null, null);

        assertTrue(ShortcutControl.applySliderValue(shortcut, 0.3));
        assertFalse(ShortcutControl.applySliderValue(shortcut, 0.31));
        assertEquals((double) 0.3f, config.value);
        assertEquals(1, config.assignments);
    }

    @Test
    void sliderRespectsOverridesAndRejectsNonFiniteRatios() {
        NumericConfig config = new NumericConfig(IConfigDouble.class, -10, 10, 0);
        ResolvedShortcut shortcut = config.shortcut(1.0, -2.0, 3.0);

        assertTrue(ShortcutControl.applySliderValue(shortcut, -1));
        assertEquals(-2, config.value);
        assertTrue(ShortcutControl.applySliderValue(shortcut, 2));
        assertEquals(3, config.value);
        assertFalse(ShortcutControl.applySliderValue(shortcut, Double.NaN));
        assertFalse(ShortcutControl.applySliderValue(shortcut, Double.POSITIVE_INFINITY));
        assertEquals(2, config.assignments);
    }

    @Test
    void numericEditorRejectsInvalidAndOutOfRangeValuesWithoutAssignment() {
        MasaConfigEditor editor = new MasaConfigEditor();
        NumericConfig config = new NumericConfig(IConfigDouble.class, -2, 3, 0);

        for (String invalid : new String[] { "bad", "NaN", "Infinity", "4" }) {
            ConfigEditResult result = editor.apply(config.config, invalid);
            assertFalse(result.success());
        }
        assertEquals(0, config.assignments);
        assertTrue(editor.apply(config.config, "2.5").success());
        assertEquals(2.5, config.value);
    }

    private static final class NumericConfig {
        private final IConfigBase config;
        private double value;
        private int assignments;

        private NumericConfig(Class<?> numericInterface, double min, double max, double initial) {
            this.value = initial;
            this.config = (IConfigBase) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { IConfigBase.class, numericInterface }, (proxy, method, args) -> {
                        String name = method.getName();
                        if (name.equals("getName")) return "value";
                        if (name.startsWith("set") && name.endsWith("Value")) {
                            this.value = ((Number) args[0]).doubleValue();
                            this.assignments++;
                            return null;
                        }
                        double result = name.startsWith("getMin") ? min : name.startsWith("getMax") ? max : this.value;
                        if (method.getReturnType() == int.class) return (int) result;
                        if (method.getReturnType() == float.class) return (float) result;
                        if (method.getReturnType() == double.class) return result;
                        if (method.getReturnType() == boolean.class) return false;
                        return null;
                    });
        }

        private ResolvedShortcut shortcut(double step, Double min, Double max) {
            ShortcutEntry shortcut = new ShortcutEntry("external", "group", "value", "",
                    ShortcutControlType.SLIDER, step, min, max);
            ConfigIndexEntry entry = new ConfigIndexEntry("external", "External", "group", "Group",
                    "value", "Value", this.config);
            return new ResolvedShortcut(shortcut, entry);
        }
    }
}
