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

    @Test
    void treatsSameConfigNameInDifferentGroupsAsDifferentEntries() {
        ConfigIndexEntry generic = new ConfigIndexEntry("tweakeroo", "Tweakeroo", "generic", "Generic",
                "enabled", "Enabled", null);

        assertTrue(ConfigIndexService.hasSameConfigIdentity(generic, "tweakeroo", "generic", "enabled"));
        assertFalse(ConfigIndexService.hasSameConfigIdentity(generic, "tweakeroo", "placement", "enabled"));
    }

    @Test
    void comparesNullConfigIdentityValuesSafely() {
        ConfigIndexEntry nullIdentity = new ConfigIndexEntry(null, "Tweakeroo", null, "Generic", null, "Enabled", null);

        assertTrue(ConfigIndexService.hasSameConfigIdentity(nullIdentity, null, null, null));
        assertFalse(ConfigIndexService.hasSameConfigIdentity(nullIdentity, "tweakeroo", null, null));
        assertFalse(ConfigIndexService.hasSameConfigIdentity(null, null, null, null));
    }
}
