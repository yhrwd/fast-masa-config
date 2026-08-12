package fastui.yure.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityRenderFilterTest {
    @Test
    void blacklistHidesOnlyConfiguredEntityIds() {
        List<String> ids = List.of(" minecraft:item ", "minecraft:zombie");

        assertFalse(EntityRenderFilter.shouldRender(true, false, ids, "minecraft:item"));
        assertTrue(EntityRenderFilter.shouldRender(true, false, ids, "minecraft:cow"));
    }

    @Test
    void whitelistRendersOnlyConfiguredEntityIds() {
        List<String> ids = List.of("minecraft:item", "invalid");

        assertTrue(EntityRenderFilter.shouldRender(true, true, ids, "MINECRAFT:ITEM"));
        assertFalse(EntityRenderFilter.shouldRender(true, true, ids, "minecraft:cow"));
        assertTrue(EntityRenderFilter.shouldRender(false, true, ids, "minecraft:cow"));
    }

    @Test
    void disabledFilterDoesNotReadTheConfiguredList() {
        assertTrue(EntityRenderFilter.shouldRender(false, false, null, "minecraft:player"));
    }

    @Test
    void snapshotKeepsDisabledFilterPassThrough() {
        EntityRenderFilter.State state = EntityRenderFilter.State.from(false, true, null);

        assertTrue(state.shouldRender("minecraft:item"));
        assertTrue(state.shouldRender("minecraft:player"));
    }

    @Test
    void disabledFilterNeverTurnsBlacklistIntoAnImplicitWhitelist() {
        EntityRenderFilter.State state = EntityRenderFilter.State.from(false, false,
                List.of("minecraft:item", "minecraft:zombie"));

        assertTrue(state.shouldRender("minecraft:item"));
        assertTrue(state.shouldRender("minecraft:zombie"));
        assertTrue(state.shouldRender("minecraft:cow"));
    }
}
