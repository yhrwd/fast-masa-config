package fastui.yure.client.scan;

import fi.dy.masa.malilib.config.gui.ButtonPressDirtyListenerSimple;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.gui.interfaces.IConfigInfoProvider;
import net.minecraft.client.gui.screen.Screen;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
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
    void prefersDirectFactoryOverProvidedFactoryForTheSameMod() {
        List<ConfigScreenSourceService.Source> sources = ConfigScreenSourceService.collectModMenuSources(
                List.of(
                        new ConfigScreenSourceService.ModMenuEntrypoint("provider", "Provider",
                                new ProvidedFactoryApi("provided")),
                        new ConfigScreenSourceService.ModMenuEntrypoint("target", "Target",
                                new FakeModMenuApi("direct"))
                ),
                Set.of()
        );

        assertEquals(List.of("target", "other"), sources.stream()
                .map(ConfigScreenSourceService.Source::modId)
                .toList());
        assertEquals("target", sources.getFirst().modId());
        assertEquals("direct", sources.getFirst().configGui().getModId());
    }

    @Test
    void discoversProvidedConfigScreenFactories() {
        List<ConfigScreenSourceService.Source> sources = ConfigScreenSourceService.collectModMenuSources(
                List.of(new ConfigScreenSourceService.ModMenuEntrypoint("provider", "Provider",
                        new ProvidedFactoryApi("provided"))),
                Set.of()
        );

        assertEquals(Set.of("target", "other"), sources.stream()
                .map(ConfigScreenSourceService.Source::modId)
                .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void continuesAfterAProvidedFactoryThrows() {
        List<ConfigScreenSourceService.Source> sources = ConfigScreenSourceService.collectModMenuSources(
                List.of(new ConfigScreenSourceService.ModMenuEntrypoint("provider", "Provider",
                        new ThrowingProvidedFactoryApi())),
                Set.of()
        );

        assertEquals(List.of("valid"), sources.stream().map(ConfigScreenSourceService.Source::modId).toList());
    }

    @Test
    void reservesDirectModIdWhenItsFactoryThrows() {
        List<ConfigScreenSourceService.Source> sources = ConfigScreenSourceService.collectModMenuSources(
                List.of(
                        new ConfigScreenSourceService.ModMenuEntrypoint("target", "Target",
                                new ThrowingDirectWithProvidedFactoryApi())
                ),
                Set.of()
        );

        assertEquals(List.of("other"), sources.stream()
                .map(ConfigScreenSourceService.Source::modId)
                .toList());
    }

    @Test
    void continuesToDirectFactoryWhenProvidedFactoriesGetterThrows() {
        List<ConfigScreenSourceService.Source> sources = ConfigScreenSourceService.collectModMenuSources(
                List.of(new ConfigScreenSourceService.ModMenuEntrypoint("target", "Target",
                        new ThrowingProvidedGetterApi())),
                Set.of()
        );

        assertEquals(List.of("target"), sources.stream()
                .map(ConfigScreenSourceService.Source::modId)
                .toList());
    }

    @Test
    void reservesDirectModIdWhenDirectFactoryGetterThrows() {
        List<ConfigScreenSourceService.Source> sources = ConfigScreenSourceService.collectModMenuSources(
                List.of(new ConfigScreenSourceService.ModMenuEntrypoint("target", "Target",
                        new ThrowingDirectGetterApi())),
                Set.of()
        );

        assertEquals(List.of("other"), sources.stream()
                .map(ConfigScreenSourceService.Source::modId)
                .toList());
    }

    @Test
    void acceptsLaterProvidedFactoryWhenEarlierCandidateForTheSameModIsInvalid() {
        List<ConfigScreenSourceService.Source> sources = ConfigScreenSourceService.collectModMenuSources(
                List.of(
                        new ConfigScreenSourceService.ModMenuEntrypoint("invalid-provider", "Invalid Provider",
                                new SingleProvidedFactoryApi("target", new NullScreenFactory())),
                        new ConfigScreenSourceService.ModMenuEntrypoint("valid-provider", "Valid Provider",
                                new SingleProvidedFactoryApi("target", new FakeConfigScreenFactory("target")))
                ),
                Set.of()
        );

        assertEquals(List.of("target"), sources.stream()
                .map(ConfigScreenSourceService.Source::modId)
                .toList());
    }

    @Test
    void continuesAfterProvidedSourceMetadataLookupThrows() {
        List<ConfigScreenSourceService.Source> sources = ConfigScreenSourceService.collectModMenuSources(
                List.of(new ConfigScreenSourceService.ModMenuEntrypoint("provider", "Provider",
                        new TwoProvidedFactoriesApi())),
                Set.of(),
                modId -> {
                    if (modId.equals("broken")) {
                        throw new IllegalStateException("metadata lookup failed");
                    }
                    return modId;
                }
        );

        assertEquals(List.of("valid"), sources.stream()
                .map(ConfigScreenSourceService.Source::modId)
                .toList());
    }

    @Test
    void ignoresFactoryWithoutPublicScreenCreateContract() {
        List<ConfigScreenSourceService.Source> sources = ConfigScreenSourceService.collectModMenuSources(
                List.of(new ConfigScreenSourceService.ModMenuEntrypoint("provider", "Provider",
                        new SingleProvidedFactoryApi("target", new NonScreenFactory()))),
                Set.of()
        );

        assertEquals(List.of(), sources);
    }

    private record FakeModMenuApi(String modId) {
        @SuppressWarnings("unused")
        public FakeConfigScreenFactory getModConfigScreenFactory() {
            return new FakeConfigScreenFactory(this.modId);
        }
    }

    private record FakeConfigScreenFactory(String modId) {
        @SuppressWarnings("unused")
        public FakeConfigScreen create(Screen parent) {
            return new FakeConfigScreen(this.modId);
        }
    }

    private record ProvidedFactoryApi(String screenModId) {
        @SuppressWarnings("unused")
        public Map<String, FakeConfigScreenFactory> getProvidedConfigScreenFactories() {
            return Map.of(
                    "target", new FakeConfigScreenFactory(this.screenModId),
                    "other", new FakeConfigScreenFactory("other")
            );
        }
    }

    private record ThrowingDirectWithProvidedFactoryApi() {
        @SuppressWarnings("unused")
        public ThrowingFactory getModConfigScreenFactory() {
            return new ThrowingFactory();
        }

        @SuppressWarnings("unused")
        public Map<String, FakeConfigScreenFactory> getProvidedConfigScreenFactories() {
            return Map.of(
                    "target", new FakeConfigScreenFactory("provided-target"),
                    "other", new FakeConfigScreenFactory("other")
            );
        }
    }

    private record ThrowingProvidedGetterApi() {
        public Map<String, FakeConfigScreenFactory> getProvidedConfigScreenFactories() {
            throw new IllegalStateException("provided getter failed");
        }

        public FakeConfigScreenFactory getModConfigScreenFactory() {
            return new FakeConfigScreenFactory("direct");
        }
    }

    private record ThrowingDirectGetterApi() {
        public Map<String, FakeConfigScreenFactory> getProvidedConfigScreenFactories() {
            return Map.of(
                    "target", new FakeConfigScreenFactory("provided-target"),
                    "other", new FakeConfigScreenFactory("other")
            );
        }

        public FakeConfigScreenFactory getModConfigScreenFactory() {
            throw new IllegalStateException("direct getter failed");
        }
    }

    private record ThrowingProvidedFactoryApi() {
        @SuppressWarnings("unused")
        public Map<String, Object> getProvidedConfigScreenFactories() {
            Map<String, Object> factories = new LinkedHashMap<>();
            factories.put("broken", new ThrowingFactory());
            factories.put("valid", new FakeConfigScreenFactory("valid"));
            return factories;
        }
    }

    private record SingleProvidedFactoryApi(String modId, Object factory) {
        public Map<String, Object> getProvidedConfigScreenFactories() {
            return Map.of(this.modId, this.factory);
        }
    }

    private record TwoProvidedFactoriesApi() {
        public Map<String, FakeConfigScreenFactory> getProvidedConfigScreenFactories() {
            Map<String, FakeConfigScreenFactory> factories = new LinkedHashMap<>();
            factories.put("broken", new FakeConfigScreenFactory("broken"));
            factories.put("valid", new FakeConfigScreenFactory("valid"));
            return factories;
        }
    }

    private record NullScreenFactory() {
        public FakeConfigScreen create(Screen parent) {
            return null;
        }
    }

    private record NonScreenFactory() {
        public FakeConfigScreen create(String parent) {
            return new FakeConfigScreen("target");
        }
    }

    private record ThrowingFactory() {
        public FakeConfigScreen create(Screen parent) {
            throw new IllegalStateException("factory failed");
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
