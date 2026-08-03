package fastui.yure.client.scan;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.gui.ButtonPressDirtyListenerSimple;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.gui.interfaces.IConfigInfoProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigGuiGroupScannerTest {
    @Test
    void scansEnumGroupsAndRestoresTheOriginalSelection() {
        FakeConfigScreen.tab = Tab.FIRST;

        List<ConfigGuiGroupScanner.Group> groups = ConfigGuiGroupScanner.collectGroups(new FakeConfigScreen(), new FakeConfigScreen());

        assertEquals(List.of("FIRST", "SECOND"), groups.stream().map(ConfigGuiGroupScanner.Group::id).toList());
        assertEquals(Tab.FIRST, FakeConfigScreen.tab);
    }

    private enum Tab {
        FIRST,
        SECOND
    }

    private static final class FakeConfigScreen implements IConfigGui {
        private static Tab tab = Tab.FIRST;

        @Override
        public String getModId() {
            return "test";
        }

        @Override
        public void clearOptions() {
        }

        @Override
        public List<GuiConfigsBase.ConfigOptionWrapper> getConfigs() {
            return List.of(new GuiConfigsBase.ConfigOptionWrapper(new FakeConfig(tab.name())));
        }

        @Override
        public ButtonPressDirtyListenerSimple getButtonPressListener() {
            return null;
        }

        @Override
        public IConfigInfoProvider getHoverInfoProvider() {
            return null;
        }
    }

    private record FakeConfig(String name) implements IConfigBase {
        @Override
        public ConfigType getType() {
            return ConfigType.BOOLEAN;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getComment() {
            return null;
        }

        @Override
        public String getTranslatedName() {
            return this.name;
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
