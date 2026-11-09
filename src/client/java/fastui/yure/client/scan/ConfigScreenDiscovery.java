package fastui.yure.client.scan;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Discovery and precedence rules without loading Minecraft, MaLiLib or ModMenu classes. */
final class ConfigScreenDiscovery {
    private final Class<?> screenType;
    private final Class<?> configGuiType;
    private final Function<String, String> modNameResolver;
    private final BiConsumer<String, Throwable> failureLogger;

    ConfigScreenDiscovery(Class<?> screenType, Class<?> configGuiType,
            Function<String, String> modNameResolver, BiConsumer<String, Throwable> failureLogger) {
        this.screenType = screenType;
        this.configGuiType = configGuiType;
        this.modNameResolver = modNameResolver;
        this.failureLogger = failureLogger;
    }

    List<Source> collectSources(Iterable<RegisteredScreen> registry,
            Supplier<? extends Iterable<ModMenuEntrypoint>> entrypointSupplier) {
        List<Source> sources = new ArrayList<>();
        Set<String> sourceModIds = new HashSet<>();

        for (RegisteredScreen registered : registry) {
            if (sourceModIds.contains(registered.modId())) {
                continue;
            }

            try {
                Supplier<?> supplier = registered.screenSupplier();
                Object screen = supplier == null ? null : supplier.get();
                if (this.configGuiType.isInstance(screen)) {
                    sources.add(new Source(registered.modId(), registered.modName(), screen));
                    sourceModIds.add(registered.modId());
                }
            } catch (RuntimeException | LinkageError e) {
                logFailure("create registered config screen", registered.modId(), e);
            }
        }

        Iterable<ModMenuEntrypoint> entrypoints;
        try {
            entrypoints = entrypointSupplier.get();
        } catch (RuntimeException | LinkageError e) {
            this.failureLogger.accept("Failed to discover ModMenu entrypoints", e);
            return sources;
        }

        List<Factory> directFactories = new ArrayList<>();
        List<Factory> providedFactories = new ArrayList<>();
        for (ModMenuEntrypoint entrypoint : entrypoints) {
            Object api;
            try {
                api = entrypoint.apiSupplier().get();
            } catch (RuntimeException | LinkageError e) {
                logFailure("create ModMenu entrypoint", entrypoint.modId(), e);
                continue;
            }
            if (api != null) {
                collectDirectFactory(api, entrypoint, directFactories);
                collectProvidedFactories(api, entrypoint.modId(), providedFactories);
            }
        }

        // Direct factories reserve their IDs even if the screen cannot be scanned,
        // matching ModMenu's preference for a mod's own factory over provided ones.
        addSources(sources, sourceModIds, directFactories, false);
        addSources(sources, sourceModIds, providedFactories, true);
        return sources;
    }

    private void collectDirectFactory(Object api, ModMenuEntrypoint entrypoint, List<Factory> factories) {
        try {
            Method method = findNoArgMethod(api.getClass(), "getModConfigScreenFactory");
            if (method != null) {
                Object factory = invoke(method, api);
                if (factory != null) {
                    factories.add(new Factory(entrypoint.modId(), entrypoint.modName(), factory));
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            logFailure("get ModMenu config screen factory", entrypoint.modId(), e);
            factories.add(new Factory(entrypoint.modId(), entrypoint.modName(), null));
        }
    }

    private void collectProvidedFactories(Object api, String providerModId, List<Factory> factories) {
        try {
            Method method = findNoArgMethod(api.getClass(), "getProvidedConfigScreenFactories");
            Object result = method == null ? null : invoke(method, api);
            if (result instanceof Map<?, ?> provided) {
                for (Map.Entry<?, ?> entry : provided.entrySet()) {
                    if (entry.getKey() instanceof String modId && entry.getValue() != null) {
                        factories.add(new Factory(modId, null, entry.getValue()));
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            logFailure("get provided ModMenu config screen factories", providerModId, e);
        }
    }

    private void addSources(List<Source> sources, Set<String> sourceModIds, List<Factory> factories, boolean provided) {
        for (Factory factory : factories) {
            if (sourceModIds.contains(factory.modId())) {
                continue;
            }
            if (provided == false) {
                sourceModIds.add(factory.modId());
            }

            try {
                Object screen = createScreen(factory.factory());
                if (this.configGuiType.isInstance(screen)) {
                    String modName = provided ? this.modNameResolver.apply(factory.modId()) : factory.modName();
                    sources.add(new Source(factory.modId(), modName, screen));
                    sourceModIds.add(factory.modId());
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                logFailure("create " + (provided ? "provided " : "") + "ModMenu config screen", factory.modId(), e);
            }
        }
    }

    private Object createScreen(Object factory) throws ReflectiveOperationException {
        if (factory == null) {
            return null;
        }
        for (Method method : factory.getClass().getMethods()) {
            if (method.getName().equals("create")
                    && Modifier.isStatic(method.getModifiers()) == false
                    && method.getParameterCount() == 1
                    && method.getParameterTypes()[0] == this.screenType
                    && (this.screenType.isAssignableFrom(method.getReturnType())
                            || this.configGuiType.isAssignableFrom(method.getReturnType()))) {
                return invoke(method, factory, new Object[] { null });
            }
        }
        return null;
    }

    private static Method findNoArgMethod(Class<?> type, String name) {
        // getMethods includes inherited interface defaults used for provided factories.
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 0) {
                return method;
            }
        }
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
                // Some integrations implement the API methods on a non-public superclass.
            }
        }
        return null;
    }

    private static Object invoke(Method method, Object owner, Object... arguments) throws ReflectiveOperationException {
        if (method.canAccess(owner) == false) {
            method.setAccessible(true);
        }
        return method.invoke(owner, arguments);
    }

    private void logFailure(String action, String modId, Throwable failure) {
        if (failure instanceof InvocationTargetException e && e.getCause() != null) {
            failure = e.getCause();
        }
        this.failureLogger.accept("Failed to " + action + " for mod [" + modId + "]", failure);
    }

    record RegisteredScreen(String modId, String modName, Supplier<?> screenSupplier) {
    }

    record ModMenuEntrypoint(String modId, String modName, Supplier<?> apiSupplier) {
    }

    record Source(String modId, String modName, Object screen) {
    }

    private record Factory(String modId, String modName, Object factory) {
    }
}
