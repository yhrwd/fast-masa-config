package fastui.yure.client.scan;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigScreenDiscoveryTest {
    private final List<Failure> failures = new ArrayList<>();
    private final ConfigScreenDiscovery discovery = new ConfigScreenDiscovery(ParentScreen.class, ConfigGui.class,
            modId -> "target-" + modId, (message, cause) -> this.failures.add(new Failure(message, cause)));

    @Test
    void keepsRegistryPrecedenceAndDeduplicatesByModIdWithoutCreatingLosingScreens() {
        FakeScreen registryScreen = new FakeScreen();
        AtomicInteger losingCalls = new AtomicInteger();
        Supplier<Object> losingRegistry = () -> {
            losingCalls.incrementAndGet();
            return new FakeScreen();
        };
        Factory losingFactory = parent -> {
            losingCalls.incrementAndGet();
            return new FakeScreen();
        };

        List<ConfigScreenDiscovery.Source> sources = this.discovery.collectSources(List.of(
                registered("registered", () -> registryScreen), registered("registered", losingRegistry)),
                () -> List.of(own("registered", losingFactory), provided("provider", Map.of("registered", losingFactory)),
                        own("extra", new ScreenFactory(new FakeScreen()))));

        assertEquals(List.of("registered", "extra"), ids(sources));
        assertSame(registryScreen, sources.getFirst().screen());
        assertEquals("name-registered", sources.getFirst().modName());
        assertEquals(0, losingCalls.get());
        assertTrue(this.failures.isEmpty());
    }

    @Test
    void letsModMenuSupplyAModWhoseRegistryScreenIsUnscannable() {
        FakeScreen screen = new FakeScreen();
        List<ConfigScreenDiscovery.Source> sources = this.discovery.collectSources(
                List.of(registered("mod", ParentScreen::new)),
                () -> List.of(own("mod", new ScreenFactory(screen))));

        assertEquals(List.of("mod"), ids(sources));
        assertSame(screen, sources.getFirst().screen());
    }

    @Test
    void skipsNullRegistrySuppliersAndScreens() {
        List<ConfigScreenDiscovery.Source> sources = this.discovery.collectSources(List.of(
                registered("no-supplier", null), registered("no-screen", () -> null)), List::of);

        assertEquals(List.of(), sources);
        assertTrue(this.failures.isEmpty());
    }

    @Test
    void continuesAfterRegistrySupplierRuntimeAndLinkageFailures() {
        IllegalStateException runtimeFailure = new IllegalStateException("broken supplier");
        NoClassDefFoundError linkageFailure = new NoClassDefFoundError("optional dependency");
        List<ConfigScreenDiscovery.Source> sources = this.discovery.collectSources(List.of(
                registered("runtime", () -> { throw runtimeFailure; }),
                registered("linkage", () -> { throw linkageFailure; }),
                registered("valid", FakeScreen::new)), List::of);

        assertEquals(List.of("valid"), ids(sources));
        assertEquals(2, this.failures.size());
        assertSame(runtimeFailure, this.failures.getFirst().cause());
        assertSame(linkageFailure, this.failures.get(1).cause());
    }

    @Test
    void ignoresAbsentModMenuAndEntrypointsWithoutApiMethods() {
        assertEquals(List.of(), this.discovery.collectSources(List.of(), List::of));
        assertEquals(List.of(), collect(new ConfigScreenDiscovery.ModMenuEntrypoint("unrelated", "Unrelated", Object::new)));
        assertTrue(this.failures.isEmpty());
    }

    @Test
    void keepsRegistrySourcesWhenOptionalEntrypointDiscoveryCannotLink() {
        NoClassDefFoundError failure = new NoClassDefFoundError("modmenu optional dependency");
        List<ConfigScreenDiscovery.Source> sources = this.discovery.collectSources(
                List.of(registered("registered", FakeScreen::new)), () -> { throw failure; });

        assertEquals(List.of("registered"), ids(sources));
        assertEquals(1, this.failures.size());
        assertSame(failure, this.failures.getFirst().cause());
    }

    @Test
    void continuesAfterOneEntrypointCannotBeLoaded() {
        NoClassDefFoundError failure = new NoClassDefFoundError("missing integration");
        List<ConfigScreenDiscovery.Source> sources = collect(
                new ConfigScreenDiscovery.ModMenuEntrypoint("broken", "Broken", () -> { throw failure; }),
                own("valid", new ScreenFactory(new FakeScreen())));

        assertEquals(List.of("valid"), ids(sources));
        assertEquals(1, this.failures.size());
        assertTrue(this.failures.getFirst().message().contains("broken"));
        assertSame(failure, this.failures.getFirst().cause());
    }

    @Test
    void readsProvidedFactoriesFromAnInheritedInterfaceDefaultAndUsesTargetMetadata() {
        List<ConfigScreenDiscovery.Source> sources = collect(
                new ConfigScreenDiscovery.ModMenuEntrypoint("provider", "Provider", DefaultApi::new));

        assertEquals(List.of("provided"), ids(sources));
        assertEquals("target-provided", sources.getFirst().modName());
        assertTrue(this.failures.isEmpty());
    }

    @Test
    void prefersAnOwnFactoryOverAnEarlierProvidedFactory() {
        FakeScreen ownScreen = new FakeScreen();
        AtomicInteger providedCalls = new AtomicInteger();
        Factory providedFactory = parent -> {
            providedCalls.incrementAndGet();
            return new FakeScreen();
        };
        List<ConfigScreenDiscovery.Source> sources = collect(
                provided("provider", Map.of("mod", providedFactory)), own("mod", new ScreenFactory(ownScreen)),
                own("mod", new ScreenFactory(new FakeScreen())));

        assertEquals(List.of("mod"), ids(sources));
        assertEquals("name-mod", sources.getFirst().modName());
        assertSame(ownScreen, sources.getFirst().screen());
        assertEquals(0, providedCalls.get());
    }

    @Test
    void reservesNullAndNonConfigOwnScreensBeforeProvidedFactories() {
        for (ParentScreen ownScreen : new ParentScreen[] { null, new ParentScreen() }) {
            List<ConfigScreenDiscovery.Source> sources = collect(
                    provided("provider", Map.of("mod", new ScreenFactory(new FakeScreen()))),
                    own("mod", new ScreenFactory(ownScreen)));
            assertEquals(List.of(), sources);
        }
        assertTrue(this.failures.isEmpty());
    }

    @Test
    void reservesAnOwnFactoryThatThrowsAndLogsItsActualCause() {
        NoClassDefFoundError failure = new NoClassDefFoundError("missing screen dependency");
        Factory throwing = parent -> { throw failure; };
        List<ConfigScreenDiscovery.Source> sources = collect(
                provided("provider", Map.of("broken", new ScreenFactory(new FakeScreen()))),
                own("broken", throwing), own("valid", new ScreenFactory(new FakeScreen())));

        assertEquals(List.of("valid"), ids(sources));
        assertEquals(1, this.failures.size());
        assertSame(failure, this.failures.getFirst().cause());
    }

    @Test
    void isolatesProvidedGetterFailureFromTheSameEntrypointsOwnFactory() {
        IllegalStateException failure = new IllegalStateException("provided factories failed");
        Object api = new Object() {
            public Object getModConfigScreenFactory() {
                return new ScreenFactory(new FakeScreen());
            }

            public Map<String, Object> getProvidedConfigScreenFactories() {
                throw failure;
            }
        };
        List<ConfigScreenDiscovery.Source> sources = collect(
                new ConfigScreenDiscovery.ModMenuEntrypoint("own", "Own", () -> api));

        assertEquals(List.of("own"), ids(sources));
        assertEquals(1, this.failures.size());
        assertSame(failure, this.failures.getFirst().cause());
    }

    @Test
    void reservesAnOwnGetterThatThrowsWithoutLosingItsProvidedFactories() {
        IllegalStateException failure = new IllegalStateException("own factory failed");
        Object api = new Object() {
            public Object getModConfigScreenFactory() {
                throw failure;
            }

            public Map<String, Object> getProvidedConfigScreenFactories() {
                return Map.of("other", new ScreenFactory(new FakeScreen()));
            }
        };
        List<ConfigScreenDiscovery.Source> sources = collect(
                provided("provider", Map.of("broken", new ScreenFactory(new FakeScreen()))),
                new ConfigScreenDiscovery.ModMenuEntrypoint("broken", "Broken", () -> api));

        assertEquals(List.of("other"), ids(sources));
        assertEquals(1, this.failures.size());
        assertSame(failure, this.failures.getFirst().cause());
    }

    @Test
    void continuesAfterAProvidedFactoryThrowsAndCanUseALaterValidProvider() {
        IllegalStateException failure = new IllegalStateException("provided screen failed");
        Factory broken = parent -> { throw failure; };
        Map<String, Object> factories = new LinkedHashMap<>();
        factories.put("broken", broken);
        factories.put("valid", new ScreenFactory(new FakeScreen()));
        List<ConfigScreenDiscovery.Source> sources = collect(provided("provider", factories),
                provided("fallback", Map.of("broken", new ScreenFactory(new FakeScreen()))));

        assertEquals(List.of("valid", "broken"), ids(sources));
        assertEquals(1, this.failures.size());
        assertSame(failure, this.failures.getFirst().cause());
        assertTrue(this.failures.getFirst().message().contains("provided ModMenu"));
    }

    @Test
    void ignoresUnscannableAndMalformedProvidedEntriesWithoutReservingTheirIds() {
        Map<Object, Object> factories = new LinkedHashMap<>();
        factories.put(123, new ScreenFactory(new FakeScreen()));
        factories.put("null-factory", null);
        factories.put("null-screen", new ScreenFactory(null));
        factories.put("plain-screen", new ScreenFactory(new ParentScreen()));
        List<ConfigScreenDiscovery.Source> sources = collect(provided("first", factories),
                provided("second", Map.of("null-screen", new ScreenFactory(new FakeScreen()))));

        assertEquals(List.of("null-screen"), ids(sources));
        assertTrue(this.failures.isEmpty());
    }

    @Test
    void choosesScreenCreateMethodInsteadOfUnrelatedOverloads() {
        FakeScreen screen = new FakeScreen();
        List<ConfigScreenDiscovery.Source> sources = collect(own("mod", new OverloadedFactory(screen)));

        assertEquals(List.of("mod"), ids(sources));
        assertSame(screen, sources.getFirst().screen());
        assertTrue(this.failures.isEmpty());
    }

    @Test
    void readsANonPublicFactoryGetterFromTheSuperclass() {
        List<ConfigScreenDiscovery.Source> sources = collect(
                new ConfigScreenDiscovery.ModMenuEntrypoint("mod", "Mod", InheritedApi::new));

        assertEquals(List.of("mod"), ids(sources));
        assertTrue(this.failures.isEmpty());
    }

    @Test
    void continuesAfterProvidedTargetNameResolutionFails() {
        IllegalStateException failure = new IllegalStateException("metadata failed");
        ConfigScreenDiscovery nameFailingDiscovery = new ConfigScreenDiscovery(ParentScreen.class, ConfigGui.class,
                modId -> { throw failure; }, (message, cause) -> this.failures.add(new Failure(message, cause)));
        List<ConfigScreenDiscovery.Source> sources = nameFailingDiscovery.collectSources(List.of(),
                () -> List.of(provided("provider", Map.of("provided", new ScreenFactory(new FakeScreen()))),
                        own("own", new ScreenFactory(new FakeScreen()))));

        assertEquals(List.of("own"), ids(sources));
        assertEquals(1, this.failures.size());
        assertSame(failure, this.failures.getFirst().cause());
    }

    private List<ConfigScreenDiscovery.Source> collect(ConfigScreenDiscovery.ModMenuEntrypoint... entrypoints) {
        return this.discovery.collectSources(List.of(), () -> List.of(entrypoints));
    }

    private static ConfigScreenDiscovery.RegisteredScreen registered(String modId, Supplier<?> supplier) {
        return new ConfigScreenDiscovery.RegisteredScreen(modId, "name-" + modId, supplier);
    }

    private static ConfigScreenDiscovery.ModMenuEntrypoint own(String modId, Object factory) {
        return new ConfigScreenDiscovery.ModMenuEntrypoint(modId, "name-" + modId, () -> new OwnApi(factory));
    }

    private static ConfigScreenDiscovery.ModMenuEntrypoint provided(String modId, Map<?, ?> factories) {
        return new ConfigScreenDiscovery.ModMenuEntrypoint(modId, "name-" + modId, () -> new ProvidedApi(factories));
    }

    private static List<String> ids(List<ConfigScreenDiscovery.Source> sources) {
        return sources.stream().map(ConfigScreenDiscovery.Source::modId).toList();
    }

    private static class ParentScreen {
    }

    private interface ConfigGui {
    }

    private static final class FakeScreen extends ParentScreen implements ConfigGui {
    }

    private interface Factory {
        ParentScreen create(ParentScreen parent);
    }

    private record ScreenFactory(ParentScreen screen) implements Factory {
        @Override
        public ParentScreen create(ParentScreen parent) {
            assertNull(parent);
            return this.screen;
        }
    }

    private record OwnApi(Object factory) {
        public Object getModConfigScreenFactory() {
            return this.factory;
        }
    }

    private record ProvidedApi(Map<?, ?> factories) {
        public Map<?, ?> getProvidedConfigScreenFactories() {
            return this.factories;
        }
    }

    private interface ProvidedDefaults {
        default Map<String, ScreenFactory> getProvidedConfigScreenFactories() {
            return Map.of("provided", new ScreenFactory(new FakeScreen()));
        }
    }

    private static final class DefaultApi implements ProvidedDefaults {
    }

    private static class BaseApi {
        @SuppressWarnings("unused")
        private Object getModConfigScreenFactory() {
            return new ScreenFactory(new FakeScreen());
        }
    }

    private static final class InheritedApi extends BaseApi {
    }

    private record OverloadedFactory(FakeScreen screen) {
        public FakeScreen create(String unrelated) {
            throw new AssertionError("Unrelated overload invoked");
        }

        public FakeScreen create(NarrowScreen unrelated) {
            throw new AssertionError("Narrow screen overload invoked");
        }

        public FakeScreen create(ParentScreen parent) {
            assertNull(parent);
            return this.screen;
        }
    }

    private static final class NarrowScreen extends ParentScreen {
    }

    private record Failure(String message, Throwable cause) {
    }
}
