package fastui.yure.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigGroupStoreTest {
    @Test
    void managesGroupMetadataAndOrder() {
        ConfigGroupStore.clear();
        ConfigGroup first = ConfigGroupStore.create("建筑");
        ConfigGroup second = ConfigGroupStore.create("渲染");

        assertFalse(first.id().isBlank());
        assertTrue(ConfigGroupStore.rename(first.id(), "建造"));
        assertEquals("建造", first.name());
        assertTrue(ConfigGroupStore.moveGroup(second.id(), -1));
        assertEquals(List.of(second.id(), first.id()), ConfigGroupStore.getGroups().stream().map(ConfigGroup::id).toList());
        assertTrue(ConfigGroupStore.remove(first.id()));
        assertTrue(ConfigGroupStore.get(first.id()).isEmpty());
    }

    @Test
    void rejectsBlankGroupNames() {
        ConfigGroupStore.clear();

        assertThrows(IllegalArgumentException.class, () -> ConfigGroupStore.create(null));
        assertThrows(IllegalArgumentException.class, () -> ConfigGroupStore.create(" "));

        ConfigGroup group = ConfigGroupStore.create("建筑");
        assertFalse(ConfigGroupStore.rename(group.id(), null));
        assertFalse(ConfigGroupStore.rename(group.id(), " "));
        assertEquals("建筑", group.name());
    }

    @Test
    void addsTargetsOnceWithinTheSameGroupAndPreservesOrder() {
        ConfigGroupStore.clear();
        ConfigGroup group = ConfigGroupStore.create("建筑");

        assertTrue(ConfigGroupStore.addItem(group.id(), new GroupItem("tweakeroo", "Generic", "fastBlockPlacement", false)));
        assertTrue(ConfigGroupStore.addItem(group.id(), new GroupItem("tweakeroo", "Hotkeys", "fastBlockPlacement", false)));
        assertFalse(ConfigGroupStore.addItem(group.id(), new GroupItem("tweakeroo", "Generic", "fastBlockPlacement", false)));
        assertTrue(ConfigGroupStore.addItem(group.id(), new GroupItem("minihud", "Renderer", "overlayLightLevel", false)));
        assertTrue(ConfigGroupStore.moveItem(group.id(), 2, -1));
        assertTrue(ConfigGroupStore.setItemExpanded(group.id(), 1, true));
        assertTrue(ConfigGroupStore.removeItem(group.id(), 2));

        ConfigGroup loaded = ConfigGroupStore.get(group.id()).orElseThrow();
        assertEquals("Generic", loaded.items().getFirst().groupId());
        assertEquals("overlayLightLevel", loaded.items().get(1).configName());
        assertTrue(loaded.items().get(1).expanded());
        assertEquals(2, loaded.items().size());
    }

    @Test
    void roundTripsWindowStateWithoutPersistingObsoleteFloatingFlag() {
        ConfigGroupStore.clear();
        ConfigGroup group = ConfigGroupStore.create("建筑");
        ConfigGroupStore.setWindowState(group.id(), false, 40, 60);
        ConfigGroupStore.addItem(group.id(), new GroupItem("tweakeroo", "Generic", "fastBlockPlacement", true));

        JsonArray saved = ConfigGroupStore.toJson();
        ConfigGroupStore.clear();
        ConfigGroupStore.fromJson(saved);

        ConfigGroup loaded = ConfigGroupStore.get(group.id()).orElseThrow();
        assertFalse(loaded.collapsed());
        assertEquals(40, loaded.x());
        assertEquals(60, loaded.y());
        assertTrue(loaded.items().getFirst().expanded());
        assertEquals("Generic", loaded.items().getFirst().groupId());
    }

    @Test
    void ignoresLegacyFloatingFlagWhenLoadingGroups() {
        ConfigGroupStore.fromJson(JsonParser.parseString("""
                [{"id":"default","name":"Fast Masa Config","color":123,"floating":false,"collapsed":true,"x":40,"y":60}]
                """).getAsJsonArray());

        JsonObject saved = ConfigGroupStore.toJson().get(0).getAsJsonObject();
        ConfigGroup loaded = ConfigGroupStore.get("default").orElseThrow();

        assertFalse(saved.has("floating"));
        assertFalse(saved.has("color"));
        assertEquals("FastUI", loaded.name());
        assertTrue(loaded.collapsed());
        assertEquals(40, loaded.x());
        assertEquals(60, loaded.y());
    }

    @Test
    void persistsSystemAndHiddenStateForTheDefaultGroup() {
        ConfigGroupStore.fromJson(JsonParser.parseString("""
                [{"id":"default","name":"Fast Masa Config","system":true,"hidden":true,"items":[]}]
                """).getAsJsonArray());

        JsonArray saved = ConfigGroupStore.toJson();

        assertEquals(1, saved.size());
        assertEquals("default", saved.get(0).getAsJsonObject().get("id").getAsString());
        assertTrue(saved.get(0).getAsJsonObject().get("system").getAsBoolean());
        assertTrue(saved.get(0).getAsJsonObject().get("hidden").getAsBoolean());
        assertFalse(ConfigGroupStore.remove("default"));
    }

    @Test
    void hidesGroupsWithoutChangingExternalUserGroupDeletion() {
        ConfigGroupStore.clear();
        ConfigGroup defaultGroup = ConfigGroupStore.ensureDefaultGroup();
        ConfigGroup userGroup = ConfigGroupStore.create("建筑");

        assertTrue(ConfigGroupStore.hide(defaultGroup.id(), true));
        assertTrue(defaultGroup.hidden());
        assertTrue(ConfigGroupStore.hide(defaultGroup.id(), false));
        assertFalse(defaultGroup.hidden());
        assertTrue(ConfigGroupStore.remove(userGroup.id()));
        assertFalse(ConfigGroupStore.remove(defaultGroup.id()));
    }

    @Test
    void normalizesNullItemGroupIdBeforeJsonRoundTrip() {
        ConfigGroupStore.clear();
        ConfigGroup group = ConfigGroupStore.create("建筑");

        assertTrue(ConfigGroupStore.addItem(group.id(), new GroupItem("tweakeroo", null, "fastBlockPlacement", false)));
        assertEquals("", ConfigGroupStore.get(group.id()).orElseThrow().items().getFirst().groupId());

        JsonArray saved = ConfigGroupStore.toJson();
        ConfigGroupStore.clear();
        ConfigGroupStore.fromJson(saved);

        assertEquals("", ConfigGroupStore.get(group.id()).orElseThrow().items().getFirst().groupId());
    }

    @Test
    void skipsMalformedGroupsAndItems() {
        JsonArray groups = JsonParser.parseString("""
                [{"id":"","name":"invalid"},{"id":"7bbd91b5-31eb-4a44-a181-7698c7d4cb8e","name":"建筑","items":[{"modId":"","configName":"x"},{"modId":"tweakeroo","configName":"valid"},null]}]
                """).getAsJsonArray();

        ConfigGroupStore.fromJson(groups);

        assertEquals(1, ConfigGroupStore.getGroups().size());
        assertEquals(1, ConfigGroupStore.getGroups().getFirst().items().size());
        assertEquals("valid", ConfigGroupStore.getGroups().getFirst().items().getFirst().configName());
    }

    @Test
    void migratesShortcutsToAnIdempotentProtectedDefaultGroup() {
        ConfigGroupStore.clear();

        ConfigGroupStore.migrateShortcutsIfEmpty(java.util.Collections.singletonList(null));

        assertEquals(1, ConfigGroupStore.getGroups().size());
        ConfigGroup defaultGroup = ConfigGroupStore.getGroups().getFirst();
        assertEquals("default", defaultGroup.id());
        assertEquals("FastUI", defaultGroup.name());
        assertFalse(ConfigGroupStore.remove(defaultGroup.id()));

        List<ShortcutEntry> shortcuts = List.of(
                ShortcutEntry.fromManualId("tweakeroo/Generic/fastBlockPlacement"),
                ShortcutEntry.fromManualId("tweakeroo/Hotkeys/fastBlockPlacement"));

        ConfigGroupStore.migrateShortcutsIfEmpty(shortcuts);

        assertEquals(1, ConfigGroupStore.getGroups().size());
        assertEquals(2, defaultGroup.items().size());

        ConfigGroupStore.migrateShortcutsIfEmpty(List.of(ShortcutEntry.fromManualId("minihud:fontScale")));

        assertEquals(1, ConfigGroupStore.getGroups().size());
        assertEquals(3, defaultGroup.items().size());
    }

    @Test
    void migratesShortcutsWhenGroupsIsMalformedOrNotAnArray() {
        ConfigGroupStore.clear();
        JsonObject root = new JsonObject();
        root.addProperty("Groups", "invalid");
        List<ShortcutEntry> shortcuts = List.of(ShortcutEntry.fromManualId("minihud:fontScale"));

        FastMasaConfigHandler.loadGroups(root, shortcuts);

        assertEquals(1, ConfigGroupStore.getGroups().size());
        assertEquals(1, ConfigGroupStore.getGroups().getFirst().items().size());
    }

    @Test
    void migratesShortcutsWhenGroupsArrayHasNoValidGroups() {
        ConfigGroupStore.clear();
        JsonObject root = new JsonObject();
        root.add("Groups", JsonParser.parseString("[null,{\"id\":\"bad\"}]").getAsJsonArray());

        FastMasaConfigHandler.loadGroups(root, List.of(ShortcutEntry.fromManualId("minihud:fontScale")));

        assertEquals(1, ConfigGroupStore.getGroups().size());
        assertEquals(1, ConfigGroupStore.getGroups().getFirst().items().size());
    }

    @Test
    void doesNotMigrateShortcutsWhenGroupsContainsAValidGroup() {
        ConfigGroupStore.clear();
        JsonObject root = new JsonObject();
        root.add("Groups", JsonParser.parseString("[{\"id\":\"7bbd91b5-31eb-4a44-a181-7698c7d4cb8e\",\"name\":\"建筑\"}]").getAsJsonArray());

        FastMasaConfigHandler.loadGroups(root, List.of(ShortcutEntry.fromManualId("minihud:fontScale")));

        assertEquals(1, ConfigGroupStore.getGroups().size());
        assertTrue(ConfigGroupStore.getGroups().getFirst().items().isEmpty());
    }

    @Test
    void persistedSystemFlagCannotProtectAUserUuidGroup() {
        ConfigGroupStore.fromJson(JsonParser.parseString("""
                [{"id":"7bbd91b5-31eb-4a44-a181-7698c7d4cb8e","name":"建筑","system":true}]
                """).getAsJsonArray());

        assertTrue(ConfigGroupStore.remove("7bbd91b5-31eb-4a44-a181-7698c7d4cb8e"));
    }

    @Test
    void clearsStaleShortcutsBeforeLoadingMissingOrMalformedShortcutData() {
        ShortcutConfigStore.clear();
        ShortcutConfigStore.add(ShortcutEntry.fromManualId("minihud:fontScale"));
        JsonObject root = new JsonObject();
        root.addProperty("Shortcuts", "invalid");

        FastMasaConfigHandler.loadShortcuts(root);
        ConfigGroupStore.clear();
        FastMasaConfigHandler.loadGroups(root, ShortcutConfigStore.getEntries());

        assertTrue(ShortcutConfigStore.getEntries().isEmpty());
        assertTrue(ConfigGroupStore.getGroups().getFirst().items().isEmpty());
    }

}
