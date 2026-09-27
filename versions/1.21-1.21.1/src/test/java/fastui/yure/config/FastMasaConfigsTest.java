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
    void exposes5Point3ConfigOptions() {
        Map<String, IConfigBase> configs = FastMasaConfigs.Generic.OPTIONS.stream()
                .collect(Collectors.toMap(IConfigBase::getName, config -> config));

        assertEquals(ConfigType.HOTKEY, configs.get("openQuickConfig").getType());
        assertEquals(ConfigType.BOOLEAN, configs.get("releaseToClose").getType());
        assertEquals(ConfigType.BOOLEAN, configs.get("closeOnInventoryKey").getType());
        assertTrue(FastMasaConfigs.Tools.OPTIONS.stream()
                .anyMatch(config -> config.getName().equals("entityRenderFilter")));
    }

    @Test
    void registersQuickOverlayHotkeyForKeybindManager() {
        assertTrue(FastMasaConfigs.Generic.HOTKEY_LIST.contains(FastMasaConfigs.Generic.OPEN_QUICK_CONFIG));
    }
}
