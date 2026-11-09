package fastui.yure.client.scan;

import fastui.yure.FastMasaConfig;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public final class ConfigScreenSourceService {
    private ConfigScreenSourceService() {
    }

    public static List<Source> collectSources() {
        ConfigScreenDiscovery discovery = new ConfigScreenDiscovery(Screen.class, IConfigGui.class,
                ConfigScreenSourceService::getTargetModName,
                (message, failure) -> FastMasaConfig.LOGGER.warn(message, failure));
        List<ConfigScreenDiscovery.RegisteredScreen> registry = new ArrayList<>();
        for (ModInfo modInfo : Registry.CONFIG_SCREEN.getAllModsWithConfigScreens()) {
            registry.add(new ConfigScreenDiscovery.RegisteredScreen(modInfo.modId(), modInfo.modName(), () -> {
                var supplier = modInfo.configScreenSupplier();
                return supplier == null ? null : supplier.get();
            }));
        }

        List<Source> sources = new ArrayList<>();
        for (ConfigScreenDiscovery.Source source : discovery.collectSources(registry,
                ConfigScreenSourceService::collectModMenuEntrypoints)) {
            if (source.screen() instanceof IConfigGui configGui) {
                sources.add(new Source(source.modId(), source.modName(), source.screen(), configGui));
            }
        }
        return sources;
    }

    private static List<ConfigScreenDiscovery.ModMenuEntrypoint> collectModMenuEntrypoints() {
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("modmenu") == false) {
            return List.of();
        }

        List<ConfigScreenDiscovery.ModMenuEntrypoint> entrypoints = new ArrayList<>();
        // Object.class avoids linking the optional ModMenu API when ModMenu is absent.
        for (EntrypointContainer<Object> container : loader.getEntrypointContainers("modmenu", Object.class)) {
            ModContainer provider = container.getProvider();
            entrypoints.add(new ConfigScreenDiscovery.ModMenuEntrypoint(provider.getMetadata().getId(),
                    provider.getMetadata().getName(), container::getEntrypoint));
        }
        return entrypoints;
    }

    private static String getTargetModName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getName())
                .filter(name -> name.isBlank() == false)
                .orElse(modId);
    }

    public record Source(String modId, String modName, Object screen, IConfigGui configGui) {
    }
}
