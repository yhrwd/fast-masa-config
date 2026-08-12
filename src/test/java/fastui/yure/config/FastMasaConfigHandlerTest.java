package fastui.yure.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastMasaConfigHandlerTest {
    @Test
    void missingQuickMessageGroupsClearsMessagesFromThePreviousLoad() {
        QuickMessageGroup group = QuickMessageStore.createGroup("临时");
        QuickMessageStore.addMessage(group.id(), "", "旧消息");

        FastMasaConfigHandler.loadQuickMessageGroups(new JsonObject());

        assertTrue(QuickMessageStore.getGroups().isEmpty());
    }

    @Test
    void malformedQuickMessageGroupsLoadsAsAnEmptyCollection() {
        QuickMessageStore.clear();
        JsonObject root = JsonParser.parseString("{\"QuickMessageGroups\": {\"invalid\": true}}")
                .getAsJsonObject();

        FastMasaConfigHandler.loadQuickMessageGroups(root);

        assertEquals(0, QuickMessageStore.getGroups().size());
    }
}
