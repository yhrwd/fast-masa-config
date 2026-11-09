package fastui.yure.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void unparsableConfigFileIsBackedUpAndProtectedFromOverwrite(@TempDir Path dir) throws IOException {
        Path configFile = dir.resolve("fast-masa-config.json");
        Files.writeString(configFile, "{\"Groups\": [broken");
        List<JsonObject> attemptedWrites = new ArrayList<>();

        FastMasaConfigHandler.loadFromDirectory(dir, file -> null);

        assertTrue(FastMasaConfigHandler.isProtectingUnparsedFile());
        assertEquals("{\"Groups\": [broken", Files.readString(configFile));
        try (Stream<Path> entries = Files.list(dir)) {
            assertTrue(entries.anyMatch(p -> p.getFileName().toString().startsWith("fast-masa-config.json.broken-")));
        }

        FastMasaConfigHandler.saveTo(configFile, attemptedWrites::add);
        assertTrue(attemptedWrites.isEmpty());
    }

    @Test
    void nextSuccessfulLoadReenablesSaving(@TempDir Path dir) throws IOException {
        Path configFile = dir.resolve("fast-masa-config.json");
        Files.writeString(configFile, "not json at all");
        FastMasaConfigHandler.loadFromDirectory(dir, file -> null);
        assertTrue(FastMasaConfigHandler.isProtectingUnparsedFile());

        // applyRoot 用不含 MaLiLib 内部初始化的最小路径，验证“解析成功即解除保护”。
        FastMasaConfigHandler.loadFromDirectory(dir, file -> new JsonObject(),
                FastMasaConfigHandler::loadQuickMessageGroups);

        assertFalse(FastMasaConfigHandler.isProtectingUnparsedFile());
    }

    @Test
    void parseOrApplyExceptionsKeepTheOriginalFileProtected(@TempDir Path dir) throws IOException {
        Path configFile = dir.resolve("fast-masa-config.json");
        Files.writeString(configFile, "{}");
        List<JsonObject> attemptedWrites = new ArrayList<>();

        FastMasaConfigHandler.loadFromDirectory(dir, file -> {
            throw new IllegalStateException("parse failed");
        });
        assertTrue(FastMasaConfigHandler.isProtectingUnparsedFile());
        FastMasaConfigHandler.saveTo(configFile, attemptedWrites::add);
        assertTrue(attemptedWrites.isEmpty());

        FastMasaConfigHandler.loadFromDirectory(dir, file -> new JsonObject(), root -> {
            throw new IllegalArgumentException("invalid section");
        });
        assertTrue(FastMasaConfigHandler.isProtectingUnparsedFile());
        FastMasaConfigHandler.saveTo(configFile, attemptedWrites::add);
        assertTrue(attemptedWrites.isEmpty());
        assertEquals("{}", Files.readString(configFile));
        try (Stream<Path> entries = Files.list(dir)) {
            assertEquals(2, entries.filter(p -> p.getFileName().toString().contains(".broken-")).count());
        }
    }

    @Test
    void malformedSectionTypesAreProtectedBeforeApplyingAnyData(@TempDir Path dir) throws IOException {
        Path configFile = dir.resolve("fast-masa-config.json");
        String malformed = "{\"Groups\": {\"unexpected\": true}}";
        Files.writeString(configFile, malformed);
        List<JsonObject> appliedRoots = new ArrayList<>();

        FastMasaConfigHandler.loadFromDirectory(dir, file -> JsonParser.parseString(malformed), appliedRoots::add);

        assertTrue(FastMasaConfigHandler.isProtectingUnparsedFile());
        assertTrue(appliedRoots.isEmpty());
        assertEquals(malformed, Files.readString(configFile));
    }

    @Test
    void backupFailureDoesNotDisableOverwriteProtection(@TempDir Path dir) throws IOException {
        Path configFile = dir.resolve("fast-masa-config.json");
        Files.writeString(configFile, "broken");
        List<JsonObject> attemptedWrites = new ArrayList<>();

        FastMasaConfigHandler.loadFromDirectory(dir, file -> {
            try {
                Files.delete(file);
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            return null;
        });
        FastMasaConfigHandler.saveTo(configFile, attemptedWrites::add);

        assertTrue(FastMasaConfigHandler.isProtectingUnparsedFile());
        assertTrue(attemptedWrites.isEmpty());
        assertFalse(Files.exists(configFile));
    }

    @Test
    void emptyGroupsDoesNotResurrectLegacyShortcuts() {
        ConfigGroupStore.clear();
        JsonObject root = JsonParser.parseString("{\"Groups\": []}").getAsJsonObject();
        List<ShortcutEntry> shortcuts = List.of(new ShortcutEntry("mod", "group", "option", "",
                ShortcutControlType.TOGGLE, 1.0, null, null));

        FastMasaConfigHandler.loadGroups(root, shortcuts);
        ConfigGroupStore.ensureDefaultGroup();

        assertTrue(ConfigGroupStore.getGroups().getFirst().items().isEmpty());
    }

    @Test
    void missingConfigFileLeavesSavesEnabled(@TempDir Path dir) {
        FastMasaConfigHandler.loadFromDirectory(dir, file -> null);

        assertFalse(FastMasaConfigHandler.isProtectingUnparsedFile());
    }
}
