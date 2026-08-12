package fastui.yure.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickMessageStoreTest {
    @Test
    void startsEmptyAndManagesGroupsAndMessages() {
        QuickMessageStore.clear();
        assertTrue(QuickMessageStore.getGroups().isEmpty());

        QuickMessageGroup group = QuickMessageStore.createGroup("常用");
        QuickMessage message = QuickMessageStore.addMessage(group.id(), "回城", "/home");
        assertNotNull(message);
        assertEquals("回城", message.displayName());
        assertTrue(message.isCommand());
        assertEquals("你好", QuickMessageStore.addMessage(group.id(), "", "你好").displayName());
        assertTrue(QuickMessageStore.moveMessage(group.id(), 1, -1));
        assertEquals("你好", group.messages().getFirst().content());
        assertTrue(QuickMessageStore.removeMessage(group.id(), 1));
    }

    @Test
    void clearingAnEmptyStoreDoesNotInvalidateFloatingPanels() {
        QuickMessageStore.clear();
        long revision = QuickMessageStore.revision();
        long contentRevision = QuickMessageStore.contentRevision();

        QuickMessageStore.clear();

        assertEquals(revision, QuickMessageStore.revision());
        assertEquals(contentRevision, QuickMessageStore.contentRevision());
    }

    @Test
    void rejectsBlankNamesAndContent() {
        QuickMessageStore.clear();
        assertFalse(tryCreateGroup(null));
        assertFalse(tryCreateGroup(" "));
        QuickMessageGroup group = QuickMessageStore.createGroup("聊天");
        assertNull(QuickMessageStore.addMessage(group.id(), "备注", " "));
        assertNull(QuickMessageStore.addMessage(group.id(), "空指令", "/   "));
        assertNull(QuickMessageStore.addMessage("missing", "备注", "内容"));
    }

    @Test
    void updatesAndPersistsGroupState() {
        QuickMessageStore.clear();
        QuickMessageGroup group = QuickMessageStore.createGroup("聊天");
        QuickMessage message = QuickMessageStore.addMessage(group.id(), "旧备注", "旧内容");
        assertNotNull(message);
        assertTrue(QuickMessageStore.updateMessage(group.id(), message.id(), "新备注", "/spawn"));
        assertEquals("新备注", group.messages().getFirst().displayName());
        assertEquals("/spawn", group.messages().getFirst().content());
        assertTrue(QuickMessageStore.hideGroup(group.id(), true));
        assertTrue(QuickMessageStore.setWindowState(group.id(), true, 40, 60));

        JsonArray saved = QuickMessageStore.toJson();
        QuickMessageStore.clear();
        QuickMessageStore.fromJson(saved);
        QuickMessageGroup loaded = QuickMessageStore.getGroups().getFirst();
        assertTrue(loaded.hidden());
        assertTrue(loaded.collapsed());
        assertEquals(40, loaded.x());
        assertEquals(60, loaded.y());
        assertEquals("/spawn", loaded.messages().getFirst().content());
    }

    @Test
    void roundTripsAndSkipsMalformedData() {
        JsonArray groups = JsonParser.parseString("""
                [{"id":"7bbd91b5-31eb-4a44-a181-7698c7d4cb8e","name":"常用","x":30,"y":40,"messages":[
                  {"id":"1a3a9f80-9b32-4b6d-8b7d-b2cd7fba4d2f","label":"","content":"你好"},
                  {"id":"bad","content":"忽略"}, {"id":"2b3a9f80-9b32-4b6d-8b7d-b2cd7fba4d2f","content":""}]},
                {"id":"bad-group","name":"忽略"}]
                """).getAsJsonArray();

        QuickMessageStore.fromJson(groups);
        assertEquals(1, QuickMessageStore.getGroups().size());
        QuickMessageGroup group = QuickMessageStore.getGroups().getFirst();
        assertEquals(30, group.x());
        assertEquals("你好", group.messages().getFirst().displayName());

        JsonArray saved = QuickMessageStore.toJson();
        QuickMessageStore.clear();
        QuickMessageStore.fromJson(saved);
        assertEquals("你好", QuickMessageStore.getGroups().getFirst().messages().getFirst().content());
    }

    private static boolean tryCreateGroup(String name) {
        try {
            QuickMessageStore.createGroup(name);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
