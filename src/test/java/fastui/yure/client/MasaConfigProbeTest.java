package fastui.yure.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBase;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MasaConfigProbeTest {
    @Test
    void recordsUnknownTypeWhenConfigDoesNotReportMalilibType() throws Exception {
        Class<?> entryClass = Class.forName("fastui.yure.client.MasaConfigProbe$ConfigEntry");
        Method from = entryClass.getDeclaredMethod("from", IConfigBase.class, String.class);
        Method type = entryClass.getDeclaredMethod("type");
        from.setAccessible(true);
        type.setAccessible(true);

        Object entry = assertDoesNotThrow(() -> from.invoke(null, new NullTypeConfig(), "test"));

        assertEquals("UNKNOWN", type.invoke(entry));
    }

    private static final class NullTypeConfig implements IConfigBase {
        @Override
        public ConfigType getType() {
            return null;
        }

        @Override
        public String getName() {
            return "openNestedPage";
        }

        @Override
        public String getComment() {
            return null;
        }

        @Override
        public String getTranslatedName() {
            return "Open Nested Page";
        }

        @Override
        public void setPrettyName(String prettyName) {
        }

        @Override
        public void setTranslatedName(String translatedName) {
        }

        @Override
        public void setComment(String comment) {
        }

        @Override
        public void setValueFromJsonElement(JsonElement element) {
        }

        @Override
        public JsonElement getAsJsonElement() {
            return JsonNull.INSTANCE;
        }
    }
}
