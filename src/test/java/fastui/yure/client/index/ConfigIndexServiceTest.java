package fastui.yure.client.index;

import fastui.yure.FastMasaConfig;
import fastui.yure.config.QuickPanelConfigTags;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigIndexServiceTest {
    @Test
    void skipsOwnConfigScreenToAvoidRecursiveIndexScan() {
        assertFalse(ConfigIndexService.shouldIndexMod(FastMasaConfig.MOD_ID));
        assertTrue(ConfigIndexService.shouldIndexMod("tweakeroo"));
    }

    @Test
    void exposesOnlyExplicitlyTaggedOwnConfigsForShortcutSelection() {
        assertTrue(QuickPanelConfigTags.contains("entityRenderFilter"));
        assertTrue(QuickPanelConfigTags.contains("blockBreakIndicator"));
        assertFalse(QuickPanelConfigTags.contains("releaseToClose"));
    }
}
