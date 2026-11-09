package fastui.yure.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityRenderFilterTest {
    private static final List<String> IDS = List.of("minecraft:item", "minecraft:zombie");

    @Test
    void appliesTheExpectedTruthTable() {
        assertTrue(EntityRenderFilter.shouldRender(false, false, IDS, "minecraft:item"));
        assertTrue(EntityRenderFilter.shouldRender(false, false, IDS, "minecraft:cow"));
        assertFalse(EntityRenderFilter.shouldRender(true, false, IDS, "minecraft:item"));
        assertTrue(EntityRenderFilter.shouldRender(true, false, IDS, "minecraft:cow"));
        assertTrue(EntityRenderFilter.shouldRender(true, true, IDS, "minecraft:item"));
        assertFalse(EntityRenderFilter.shouldRender(true, true, IDS, "minecraft:cow"));
    }

    @Test
    void whitelistOffHidesOnlyConfiguredEntityIds() {
        List<String> ids = List.of(" minecraft:item ", "minecraft:zombie");

        assertFalse(EntityRenderFilter.shouldRender(true, false, ids, "minecraft:item"));
        assertTrue(EntityRenderFilter.shouldRender(true, false, ids, "minecraft:cow"));
    }

    @Test
    void whitelistOnRendersOnlyConfiguredEntityIds() {
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
    void emptyListHasExplicitModeSemantics() {
        assertTrue(EntityRenderFilter.shouldRender(false, false, List.of(), "minecraft:cow"));
        assertTrue(EntityRenderFilter.shouldRender(false, true, List.of(), "minecraft:cow"));
        assertTrue(EntityRenderFilter.shouldRender(true, false, List.of(), "minecraft:cow"));
        assertFalse(EntityRenderFilter.shouldRender(true, true, List.of(), "minecraft:cow"));
    }

    @Test
    void cachedStateReusesTheSnapshotWhileInputsStayEqual() {
        EntityRenderFilter.State first = EntityRenderFilter.cachedState(true, false, IDS);
        assertSame(first, EntityRenderFilter.cachedState(true, false, List.of("minecraft:item", "minecraft:zombie")));
        assertSame(first, EntityRenderFilter.cachedState(true, false, IDS));
    }

    @Test
    void cachedStateRebuildsWhenAnyInputChanges() {
        EntityRenderFilter.State first = EntityRenderFilter.cachedState(true, false, IDS);
        assertNotSame(first, EntityRenderFilter.cachedState(true, true, IDS));
        assertNotSame(first, EntityRenderFilter.cachedState(false, false, IDS));
        assertNotSame(first, EntityRenderFilter.cachedState(true, false, List.of("minecraft:cow")));
    }

    @Test
    void cachedStateDetectsInPlaceChangesToTheInputList() {
        List<String> ids = new java.util.ArrayList<>(List.of("minecraft:item"));
        EntityRenderFilter.State first = EntityRenderFilter.cachedState(true, false, ids);
        ids.set(0, "minecraft:cow");

        EntityRenderFilter.State updated = EntityRenderFilter.cachedState(true, false, ids);

        assertNotSame(first, updated);
        assertTrue(updated.shouldRender("minecraft:item"));
        assertFalse(updated.shouldRender("minecraft:cow"));
    }

    @Test
    void cachedStateTreatsNullListAsEmpty() {
        EntityRenderFilter.State state = EntityRenderFilter.cachedState(true, false, null);

        assertTrue(state.shouldRender("minecraft:cow"));
    }
}
