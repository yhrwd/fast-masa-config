package fastui.yure.config;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasaConfigEditorTest {
    private final MasaConfigEditor editor = new MasaConfigEditor();

    @Test
    void rejectsInvalidBooleanWithoutChangingValue() {
        ConfigBoolean config = new ConfigBoolean("enabled", true);

        ConfigEditResult result = editor.apply(config, "abc");

        assertFalse(result.success());
        assertEquals("enabled", result.configName());
        assertEquals("true", config.getStringValue());
    }

    @Test
    void appliesValidBoolean() {
        ConfigBoolean config = new ConfigBoolean("enabled", true);

        ConfigEditResult result = editor.apply(config, "false");

        assertTrue(result.success());
        assertEquals("false", config.getStringValue());
    }

    @Test
    void rejectsOutOfRangeIntegerWithoutClamping() {
        ConfigInteger config = new ConfigInteger("count", 3, 1, 5);

        ConfigEditResult result = editor.apply(config, "8");

        assertFalse(result.success());
        assertEquals("3", config.getStringValue());
    }

    @Test
    void appliesValidInteger() {
        ConfigInteger config = new ConfigInteger("count", 3, 1, 5);

        ConfigEditResult result = editor.apply(config, "5");

        assertTrue(result.success());
        assertEquals("5", config.getStringValue());
    }

    @Test
    void rejectsUnknownOptionListValue() {
        ConfigOptionList config = new ConfigOptionList("mode", TestMode.FIRST);

        ConfigEditResult result = editor.apply(config, "missing");

        assertFalse(result.success());
        assertEquals("first", config.getStringValue());
    }

    @Test
    void appliesKnownOptionListValue() {
        ConfigOptionList config = new ConfigOptionList("mode", TestMode.FIRST);

        ConfigEditResult result = editor.apply(config, "second");

        assertTrue(result.success());
        assertEquals("second", config.getStringValue());
    }

    @Test
    void commitsChangedModId() {
        RecordingConfigCommitter committer = new RecordingConfigCommitter();

        ConfigEditResult result = editor.commit("tweakeroo", committer);

        assertTrue(result.success());
        assertEquals("tweakeroo", committer.modId);
    }

    private enum TestMode implements IConfigOptionListEntry {
        FIRST("first"),
        SECOND("second");

        private final String value;

        TestMode(String value) {
            this.value = value;
        }

        @Override
        public String getStringValue() {
            return this.value;
        }

        @Override
        public String getDisplayName() {
            return this.value;
        }

        @Override
        public IConfigOptionListEntry cycle(boolean forward) {
            return forward ? SECOND : FIRST;
        }

        @Override
        public IConfigOptionListEntry fromString(String name) {
            for (TestMode mode : values()) {
                if (mode.value.equals(name)) {
                    return mode;
                }
            }

            return this;
        }
    }

    private static class RecordingConfigCommitter implements ConfigCommitter {
        private String modId;

        @Override
        public void commit(String modId) {
            this.modId = modId;
        }
    }
}
