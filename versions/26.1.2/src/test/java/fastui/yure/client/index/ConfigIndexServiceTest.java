package fastui.yure.client.index;

import fastui.yure.FastMasaConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigIndexServiceTest {
    @Test
    void skipsOwnConfigScreenToAvoidRecursiveIndexScan() {
        assertFalse(ConfigIndexService.shouldIndexMod(FastMasaConfig.MOD_ID));
        assertTrue(ConfigIndexService.shouldIndexMod("tweakeroo"));
    }

    @Test
    void distinguishesConfigsWithTheSameNameInDifferentGroups() {
        List<ConfigIndexEntry> entries = List.of(new ConfigIndexEntry(
                "tweakeroo", "Tweakeroo", "Generic", "Generic", "fastBlockPlacement", "Fast Block Placement", null));

        assertTrue(ConfigIndexService.containsIndexedTarget(entries, "tweakeroo", "Generic", "fastBlockPlacement"));
        assertFalse(ConfigIndexService.containsIndexedTarget(entries, "tweakeroo", "Hotkeys", "fastBlockPlacement"));
    }
}
