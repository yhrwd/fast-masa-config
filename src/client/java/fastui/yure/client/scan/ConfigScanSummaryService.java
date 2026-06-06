package fastui.yure.client.scan;

import fastui.yure.FastMasaConfig;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;

public final class ConfigScanSummaryService {
    private ConfigScanSummaryService() {
    }

    public static ConfigScanSummary scanRegisteredScreens() {
        int modCount = 0;
        int configCount = 0;

        for (ModInfo modInfo : Registry.CONFIG_SCREEN.getAllModsWithConfigScreens()) {
            try {
                GuiBase screen = modInfo.getConfigScreenSupplier() == null ? null : modInfo.getConfigScreenSupplier().get();

                if (screen instanceof IConfigGui configGui) {
                    modCount++;
                    configCount += configGui.getConfigs().size();
                }
            } catch (Exception e) {
                FastMasaConfig.LOGGER.warn("统计配置屏失败: {}", modInfo.getModId(), e);
            }
        }

        return new ConfigScanSummary(modCount, configCount);
    }
}
