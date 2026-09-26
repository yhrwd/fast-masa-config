package fastui.yure.config;

import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBase;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastMasaConfigsTest {
    @Test
    void exposesQuickOverlayOptionsForMalilibGui() {
        Map<String, IConfigBase> configs = FastMasaConfigs.Generic.OPTIONS.stream()
                .collect(Collectors.toMap(config -> config.getName(), config -> config));

        assertEquals(ConfigType.HOTKEY, configs.get("openQuickConfig").getType());
        assertEquals(ConfigType.INTEGER, configs.get("panelWidth").getType());
        assertEquals(ConfigType.INTEGER, configs.get("panelMaxHeight").getType());
        assertEquals(ConfigType.DOUBLE, configs.get("panelScale").getType());
        assertEquals(ConfigType.BOOLEAN, configs.get("showScanSummary").getType());
        assertEquals(ConfigType.BOOLEAN, configs.get("releaseToClose").getType());
        assertEquals(ConfigType.BOOLEAN, configs.get("closeOnInventoryKey").getType());
    }

    @Test
    void registersQuickOverlayHotkeyForKeybindManager() {
        assertTrue(FastMasaConfigs.Generic.HOTKEY_LIST.contains(FastMasaConfigs.Generic.OPEN_QUICK_CONFIG));
    }
}
