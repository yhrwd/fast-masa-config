package fastui.yure.client.scan;

import fi.dy.masa.malilib.config.gui.ButtonPressDirtyListenerSimple;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.gui.interfaces.IConfigInfoProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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

    @Test
    void keepsOnlyOneSourceWhenModMenuDeclaresTheSameModTwice() {
        List<ConfigScreenSourceService.Source> sources = ConfigScreenSourceService.collectModMenuSources(
                List.of(
                        new ConfigScreenSourceService.ModMenuEntrypoint("litematica", "Litematica", new FakeModMenuApi("litematica")),
                        new ConfigScreenSourceService.ModMenuEntrypoint("litematica", "Litematica", new FakeModMenuApi("litematica"))
                ),
                Set.of()
        );

        assertEquals(1, sources.size());
    }

    @Test
    void usesProvidedFactoryTargetsFromAnInterfaceDefaultMethod() {
        List<ConfigScreenSourceService.Source> sources = ConfigScreenSourceService.collectModMenuSources(
                List.of(new ConfigScreenSourceService.ModMenuEntrypoint("provider", "Provider", new ProvidedFactoriesApi())),
                Set.of("litematica")
        );

        assertEquals(1, sources.size());
        assertEquals("litematica-printer", sources.getFirst().modId());
        assertEquals("litematica-printer", sources.getFirst().configGui().getModId());
    }

    @Test
    void prefersAModOwnFactoryOverAProvidedFactoryForTheSameTarget() {
        List<ConfigScreenSourceService.Source> sources = ConfigScreenSourceService.collectModMenuSources(
                List.of(
                        new ConfigScreenSourceService.ModMenuEntrypoint("provider", "Provider", new CompetingProvidedFactoriesApi()),
                        new ConfigScreenSourceService.ModMenuEntrypoint("litematica", "Litematica", new FakeModMenuApi("own"))
                ),
                Set.of()
        );

        assertEquals(1, sources.size());
        assertEquals("own", sources.getFirst().configGui().getModId());
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

    private interface ProvidedFactories {
        default Map<String, FakeConfigScreenFactory> getProvidedConfigScreenFactories() {
            return Map.of(
                    "litematica", new FakeConfigScreenFactory("litematica"),
                    "litematica-printer", new FakeConfigScreenFactory("litematica-printer")
            );
        }
    }

    private static final class ProvidedFactoriesApi implements ProvidedFactories {
    }

    private interface CompetingProvidedFactories {
        default Map<String, FakeConfigScreenFactory> getProvidedConfigScreenFactories() {
            return Map.of("litematica", new FakeConfigScreenFactory("provided"));
        }
    }

    private static final class CompetingProvidedFactoriesApi implements CompetingProvidedFactories {
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
