package fastui.yure.client.index;

import fastui.yure.FastMasaConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigIndexServiceTest {
    @Test
    void skipsOwnConfigScreenToAvoidRecursiveIndexScan() {
        assertFalse(ConfigIndexService.shouldIndexMod(FastMasaConfig.MOD_ID));
        assertTrue(ConfigIndexService.shouldIndexMod("tweakeroo"));
    }
}
