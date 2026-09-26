package fastui.yure.client.scan;

import fastui.yure.FastMasaConfig;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import net.minecraft.client.gui.screen.Screen;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public final class ConfigScreenSourceService {
    private ConfigScreenSourceService() {
    }

    public static List<Source> collectSources() {
        List<Source> sources = new ArrayList<>();
        Set<String> registryModIds = new HashSet<>();

        collectRegistrySources(sources, registryModIds);
        sources.addAll(collectModMenuSources(collectModMenuEntrypoints(), registryModIds));

        return sources;
    }

    private static void collectRegistrySources(List<Source> sources, Set<String> registryModIds) {
        for (ModInfo modInfo : Registry.CONFIG_SCREEN.getAllModsWithConfigScreens()) {
            try {
                var screenSupplier = modInfo.getConfigScreenSupplier();
                Object screen = screenSupplier == null ? null : screenSupplier.get();

                if (screen instanceof IConfigGui configGui) {
                    sources.add(new Source(modInfo.getModId(), modInfo.getModName(), screen, configGui));
                    registryModIds.add(modInfo.getModId());
                }
            } catch (Exception e) {
                FastMasaConfig.LOGGER.warn("Failed to create registered config screen for mod [{}]", modInfo.getModId(),
                        e);
            }
        }
    }

    private static List<ModMenuEntrypoint> collectModMenuEntrypoints() {
        if (FabricLoader.getInstance().isModLoaded("modmenu") == false) {
            return List.of();
        }

        List<ModMenuEntrypoint> entrypoints = new ArrayList<>();

        for (EntrypointContainer<Object> container : FabricLoader.getInstance().getEntrypointContainers("modmenu",
                Object.class)) {
            ModContainer provider = container.getProvider();
            String modId = provider.getMetadata().getId();
            String modName = provider.getMetadata().getName();

            try {
                entrypoints.add(new ModMenuEntrypoint(modId, modName, container.getEntrypoint()));
            } catch (Exception e) {
                FastMasaConfig.LOGGER.warn("Failed to create ModMenu entrypoint for mod [{}]", modId, e);
            }
        }

        return entrypoints;
    }

    static List<Source> collectModMenuSources(List<ModMenuEntrypoint> entrypoints, Set<String> registryModIds) {
        return collectModMenuSources(entrypoints, registryModIds, ConfigScreenSourceService::getTargetModName);
    }

    static List<Source> collectModMenuSources(List<ModMenuEntrypoint> entrypoints, Set<String> registryModIds,
            Function<String, String> targetModNameResolver) {
        List<Source> sources = new ArrayList<>();
        Set<String> sourceModIds = new HashSet<>(registryModIds);
        List<ModMenuSource> directSources = new ArrayList<>();
        List<ModMenuSource> providedSources = new ArrayList<>();

        for (ModMenuEntrypoint entrypoint : entrypoints) {
            try {
                for (ModMenuScreen screen : createModMenuScreens(entrypoint.api(), entrypoint.modId())) {
                    List<ModMenuSource> target = screen.provided() ? providedSources : directSources;
                    target.add(new ModMenuSource(screen.modId(), entrypoint.modName(), screen.screen(), screen.provided()));
                }
            } catch (Exception e) {
                FastMasaConfig.LOGGER.warn("Failed to create ModMenu config screen for mod [{}]", entrypoint.modId(),
                        e);
            }
        }

        // 先预留并提交 ModMenu 自有来源，再处理提供来源，确保自有工厂失败时仍能占用其 ID。
        addModMenuSources(sources, sourceModIds, directSources, targetModNameResolver);
        addModMenuSources(sources, sourceModIds, providedSources, targetModNameResolver);
        return sources;
    }

    private static void addModMenuSources(List<Source> sources, Set<String> sourceModIds,
            List<ModMenuSource> candidates, Function<String, String> targetModNameResolver) {
        for (ModMenuSource candidate : candidates) {
            if (sourceModIds.contains(candidate.modId())) {
                continue;
            }

            if ((candidate.screen() instanceof IConfigGui) == false) {
                if (candidate.provided() == false) {
                    sourceModIds.add(candidate.modId());
                }
                continue;
            }

            try {
                IConfigGui configGui = (IConfigGui) candidate.screen();
                String modName = candidate.provided() ? targetModNameResolver.apply(candidate.modId())
                        : candidate.modName();
                sources.add(new Source(candidate.modId(), modName, candidate.screen(), configGui));
                sourceModIds.add(candidate.modId());
            } catch (RuntimeException e) {
                FastMasaConfig.LOGGER.warn("Failed to create provided ModMenu config source for mod [{}]",
                        candidate.modId(), e);
            }
        }
    }

    private static List<ModMenuScreen> createModMenuScreens(Object api, String directModId)
            throws ReflectiveOperationException {
        List<ModMenuScreen> screens = new ArrayList<>();
        Method providedFactoriesMethod = findNoArgMethod(api.getClass(), "getProvidedConfigScreenFactories");

        if (providedFactoriesMethod != null) {
            try {
                setAccessibleIfNeeded(providedFactoriesMethod);
                Object providedFactories = providedFactoriesMethod.invoke(api);

                if (providedFactories instanceof Map<?, ?> factories) {
                    for (Map.Entry<?, ?> entry : factories.entrySet()) {
                        if (entry.getKey() instanceof String modId && entry.getValue() != null) {
                            createModMenuScreen(entry.getValue(), modId, true, screens);
                        }
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                FastMasaConfig.LOGGER.warn("Failed to get provided ModMenu config screen factories", e);
            }
        }

        Method factoryMethod = findNoArgMethod(api.getClass(), "getModConfigScreenFactory");

        if (factoryMethod != null) {
            try {
                setAccessibleIfNeeded(factoryMethod);
                Object factory = factoryMethod.invoke(api);

                if (factory != null) {
                    createModMenuScreen(factory, directModId, false, screens);
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                FastMasaConfig.LOGGER.warn("Failed to get ModMenu config screen factory for mod [{}]", directModId, e);
                screens.add(new ModMenuScreen(directModId, null, false));
            }
        }

        return screens;
    }

    private static void createModMenuScreen(Object factory, String modId, boolean provided,
            List<ModMenuScreen> screens) {
        try {
            screens.add(new ModMenuScreen(modId, createModMenuScreen(factory).orElse(null), provided));
        } catch (ReflectiveOperationException | RuntimeException e) {
            FastMasaConfig.LOGGER.warn("Failed to create {}ModMenu config screen for mod [{}]",
                    provided ? "provided " : "", modId, e);
            screens.add(new ModMenuScreen(modId, null, provided));
        }
    }

    private static Optional<Object> createModMenuScreen(Object factory) throws ReflectiveOperationException {
        Method createMethod = findCreateMethod(factory.getClass());

        if (createMethod == null) {
            return Optional.empty();
        }

        setAccessibleIfNeeded(createMethod);
        return Optional.ofNullable(createMethod.invoke(factory, new Object[] { null }));
    }

    private static Method findNoArgMethod(Class<?> type, String methodName) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
                return method;
            }
        }

        Class<?> currentClass = type;

        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredMethod(methodName);
            } catch (NoSuchMethodException ignored) {
                currentClass = currentClass.getSuperclass();
            }
        }

        return null;
    }

    private static String getTargetModName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getName())
                .filter(name -> name.isBlank() == false)
                .orElse(modId);
    }

    private static Method findCreateMethod(Class<?> type) {
        for (Method method : type.getMethods()) {
            if ("create".equals(method.getName())
                    && Modifier.isPublic(method.getModifiers())
                    && method.getParameterCount() == 1
                    && Screen.class.isAssignableFrom(method.getParameterTypes()[0])
                    && (Screen.class.isAssignableFrom(method.getReturnType())
                            || IConfigGui.class.isAssignableFrom(method.getReturnType()))) {
                return method;
            }
        }

        return null;
    }

    private static void setAccessibleIfNeeded(Method method) {
        if (Modifier.isPublic(method.getModifiers()) == false
                || Modifier.isPublic(method.getDeclaringClass().getModifiers()) == false) {
            method.setAccessible(true);
        }
    }

    public record Source(String modId, String modName, Object screen, IConfigGui configGui) {
    }

    record ModMenuEntrypoint(String modId, String modName, Object api) {
    }

    private record ModMenuScreen(String modId, Object screen, boolean provided) {
    }

    private record ModMenuSource(String modId, String modName, Object screen, boolean provided) {
    }
}
