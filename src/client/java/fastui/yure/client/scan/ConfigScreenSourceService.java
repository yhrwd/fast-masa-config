package fastui.yure.client.scan;

import fastui.yure.FastMasaConfig;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
            if (registryModIds.contains(modInfo.getModId())) {
                continue;
            }

            try {
                GuiBase screen = modInfo.getConfigScreenSupplier() == null ? null : modInfo.getConfigScreenSupplier().get();

                if (screen instanceof IConfigGui configGui) {
                    sources.add(new Source(modInfo.getModId(), modInfo.getModName(), screen, configGui));
                    registryModIds.add(modInfo.getModId());
                }
            } catch (Exception e) {
                FastMasaConfig.LOGGER.warn("Failed to create registered config screen for mod [{}]", modInfo.getModId(), e);
            }
        }
    }

    private static List<ModMenuEntrypoint> collectModMenuEntrypoints() {
        if (FabricLoader.getInstance().isModLoaded("modmenu") == false) {
            return List.of();
        }

        List<ModMenuEntrypoint> entrypoints = new ArrayList<>();

        for (EntrypointContainer<Object> container : FabricLoader.getInstance().getEntrypointContainers("modmenu", Object.class)) {
            ModContainer provider = container.getProvider();
            String modId = provider.getMetadata().getId();

            try {
                entrypoints.add(new ModMenuEntrypoint(modId, provider.getMetadata().getName(), container.getEntrypoint()));
            } catch (Exception e) {
                FastMasaConfig.LOGGER.warn("Failed to create ModMenu entrypoint for mod [{}]", modId, e);
            }
        }

        return entrypoints;
    }

    static List<Source> collectModMenuSources(List<ModMenuEntrypoint> entrypoints, Set<String> registryModIds) {
        List<Source> sources = new ArrayList<>();
        Set<String> sourceModIds = new HashSet<>(registryModIds);

        for (ModMenuEntrypoint entrypoint : entrypoints) {
            if (sourceModIds.contains(entrypoint.modId())) {
                continue;
            }

            try {
                Optional<Object> screen = createModMenuScreen(entrypoint.api());

                if (screen.isPresent() && screen.get() instanceof IConfigGui configGui) {
                    sources.add(new Source(entrypoint.modId(), entrypoint.modName(), screen.get(), configGui));
                    sourceModIds.add(entrypoint.modId());
                }
            } catch (Exception e) {
                FastMasaConfig.LOGGER.warn("Failed to create ModMenu config screen for mod [{}]", entrypoint.modId(), e);
            }
        }

        return sources;
    }

    private static Optional<Object> createModMenuScreen(Object api) throws ReflectiveOperationException {
        Method factoryMethod = findNoArgMethod(api.getClass(), "getModConfigScreenFactory");

        if (factoryMethod == null) {
            return Optional.empty();
        }

        factoryMethod.setAccessible(true);
        Object factory = factoryMethod.invoke(api);

        if (factory == null) {
            return Optional.empty();
        }

        for (Method method : factory.getClass().getMethods()) {
            if ("create".equals(method.getName()) && method.getParameterCount() == 1) {
                method.setAccessible(true);
                return Optional.ofNullable(method.invoke(factory, new Object[] {null}));
            }
        }

        return Optional.empty();
    }

    private static Method findNoArgMethod(Class<?> type, String name) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
                // 继续查找父类，兼容 ModMenu API 的实现类。
            }
        }

        return null;
    }

    public record Source(String modId, String modName, Object screen, IConfigGui configGui) {
    }

    record ModMenuEntrypoint(String modId, String modName, Object api) {
    }
}
