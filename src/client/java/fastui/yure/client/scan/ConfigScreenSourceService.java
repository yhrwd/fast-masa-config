package fastui.yure.client.scan;

import fastui.yure.FastMasaConfig;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;

import java.util.ArrayList;
import java.util.List;

public final class ConfigScreenSourceService {
    private ConfigScreenSourceService() {
    }

    public static List<Source> collectSources() {
        List<Source> sources = new ArrayList<>();
        collectRegistrySources(sources);

        return sources;
    }

    private static void collectRegistrySources(List<Source> sources) {
        for (ModInfo modInfo : Registry.CONFIG_SCREEN.getAllModsWithConfigScreens()) {
            try {
                var screenSupplier = modInfo.configScreenSupplier();
                Object screen = screenSupplier == null ? null : screenSupplier.get();

                if (screen instanceof IConfigGui configGui) {
                    sources.add(new Source(modInfo.modId(), modInfo.modName(), screen, configGui));
                }
            } catch (Exception | LinkageError e) {
                FastMasaConfig.LOGGER.warn("Failed to create registered config screen for mod [{}]", modInfo.modId(),
                        e);
            }
        }
    }

    public record Source(String modId, String modName, Object screen, IConfigGui configGui) {
    }
}
