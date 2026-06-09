package fastui.yure.client.scan;

import fi.dy.masa.malilib.config.gui.ButtonPressDirtyListenerSimple;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.gui.interfaces.IConfigInfoProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigScreenSourceServiceTest {
    @Test
    void addsModMenuConfigScreenWhenRegistryDidNotProvideThatMod() {
        List<ConfigScreenSourceService.Source> sources = ConfigScreenSourceService.collectModMenuSources(
                List.of(
                        new ConfigScreenSourceService.ModMenuEntrypoint("litematica", "Litematica", new FakeModMenuApi("litematica")),
                        new ConfigScreenSourceService.ModMenuEntrypoint("litematica-printer", "Litematica-Printer", new FakeModMenuApi("litematica-printer"))
                ),
                Set.of("litematica")
        );

        assertEquals(1, sources.size());
        assertEquals("litematica-printer", sources.getFirst().modId());
        assertEquals("litematica-printer", sources.getFirst().configGui().getModId());
    }

    private record FakeModMenuApi(String modId) {
        @SuppressWarnings("unused")
        public FakeConfigScreenFactory getModConfigScreenFactory() {
            return new FakeConfigScreenFactory(this.modId);
        }
    }

    private record FakeConfigScreenFactory(String modId) {
        @SuppressWarnings("unused")
        public Object create(Object parent) {
            return new FakeConfigScreen(this.modId);
        }
    }

    private record FakeConfigScreen(String modId) implements IConfigGui {
        @Override
        public String getModId() {
            return this.modId;
        }

        @Override
        public void clearOptions() {
        }

        @Override
        public List<GuiConfigsBase.ConfigOptionWrapper> getConfigs() {
            return List.of();
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
}
