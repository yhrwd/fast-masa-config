package fastui.yure.client.gui;

import fastui.yure.config.GroupItem;
import fastui.yure.config.QuickMessageStore;
import fastui.yure.client.index.ConfigIndexService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickConfigGroupGeometryTest {
    @Test
    void collapsedWindowHasTargetWidthAndNoInteractiveContent() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(800, 600, 100, 100, true,
                new int[]{0, -1});

        assertEquals(196, layout.width());
        assertEquals(0, layout.rows().size());
        assertEquals(layout.headerHeight(), layout.height());
        assertEquals(GroupWindowHitTest.Target.NONE, GroupWindowHitTest.hitTest(layout, 0, 120,
                layout.y() + layout.headerHeight(), null, java.util.List.of()).target());
    }

    @Test
    void dragClampUsesActualWindowDimensions() {
        assertArrayEquals(new int[]{320, 280}, FloatingGroupPanel.clampPosition(900, 900,
                600, 500, 260, 200, 20));
        assertArrayEquals(new int[]{20, 20}, FloatingGroupPanel.clampPosition(-10, -10,
                600, 500, 260, 200, 20));
    }

    @Test
    void onlyMarksDragWhenTheStoredPositionChanges() {
        assertFalse(FloatingGroupPanel.positionChanged(20, 20, 20, 20));
        assertTrue(FloatingGroupPanel.positionChanged(20, 20, 21, 20));
    }

    @Test
    void stationaryDefaultWindowDoesNotCountAsDragAfterLayoutClamp() {
        assertFalse(FloatingGroupPanel.positionChangedFromLayout(20, 20, 20, 20));
        assertTrue(FloatingGroupPanel.positionChangedFromLayout(20, 20, 21, 20));
    }

    @Test
    void interruptedDragOnlyFlushesWhenPositionChanged() {
        assertTrue(QuickConfigScreen.shouldFlushPendingDrag(true));
        assertFalse(QuickConfigScreen.shouldFlushPendingDrag(false));
    }

    @Test
    void normalizesMissingTargetToTheProtectedDefaultGroupBeforeControlsRender() {
        assertEquals("default", FastMasaConfigGui.normalizedTargetGroupId("", List.of("default", "building")));
        assertEquals("building", FastMasaConfigGui.normalizedTargetGroupId("building", List.of("default", "building")));
        assertEquals("", FastMasaConfigGui.normalizedTargetGroupId("missing", List.of("building")));
    }

    @Test
    void exposesTheSystemConfigEntryOnlyForTheProtectedDefaultGroup() {
        assertTrue(FloatingGroupPanel.hasSystemConfigEntry("default"));
        assertFalse(FloatingGroupPanel.hasSystemConfigEntry("building"));
    }

    @Test
    void keepsDefaultSystemRowsBeforeExternalItems() {
        assertEquals(3, FloatingGroupPanel.systemRowCount("default"));
        assertEquals(0, FloatingGroupPanel.groupItemIndexForRow("default", 3));
        assertEquals(-1, FloatingGroupPanel.groupItemIndexForRow("default", 0));
        assertEquals(-1, FloatingGroupPanel.groupItemIndexForRow("default", 2));
    }

    @Test
    void wrapsFilterControlsBelowTheNarrowBreakpoint() {
        assertFalse(FastMasaConfigGui.filterControlsWrap(290));
        assertTrue(FastMasaConfigGui.filterControlsWrap(289));
    }

    @Test
    void numericControlFitsBeforeScrollbarAt320Pixels() {
        FastMasaConfigGui.NumericControlLayout layout = FastMasaConfigGui.NumericControlLayout.calculate(320);

        assertTrue(layout.resetX() + layout.resetWidth() <= 320 - 12 - 3);
        assertTrue(layout.sliderX() >= layout.valueX() + layout.valueWidth());
        assertTrue(layout.resetX() >= layout.sliderX() + layout.sliderWidth());
    }

    @Test
    void indexesGroupItemsByTheirCanonicalTargetWithoutChangingFirstItemOrder() {
        GroupItem first = new GroupItem("tweakeroo", "Generic", "fastBlockPlacement", false);
        GroupItem duplicate = new GroupItem("tweakeroo", "Generic", "fastBlockPlacement", true);
        Map<ConfigIndexService.Target, Integer> order = FastMasaConfigGui.buildGroupItemOrder(List.of(first, duplicate));

        assertEquals(0, order.get(new ConfigIndexService.Target("tweakeroo", "Generic", "fastBlockPlacement")));
    }

    @Test
    void pageModelsKeepSearchMatchingCaseInsensitive() {
        QuickMessageStore.clear();
        try {
            var group = QuickMessageStore.createGroup("常用");
            QuickMessageStore.addMessage(group.id(), "回城", "/home");
            QuickMessageStore.addMessage(group.id(), "", "Hello world");

            assertEquals(1, QuickMessagesPage.filter(group, "HOME").size());
            assertEquals("Hello world", QuickMessagesPage.filter(group, "hello").getFirst().content());
        } finally {
            QuickMessageStore.clear();
        }
    }

    @Test
    void normalizesDuplicateAndUnboundMovementCodes() {
        assertEquals(Set.of(17, 30), QuickConfigScreen.normalizeMovementKeyCodes(Arrays.asList(17, null, 17, 30)));
    }

    @Test
    void keepsGroupActionsInsideA260PixelViewport() {
        FastMasaConfigGui.GroupActionLayout layout = FastMasaConfigGui.GroupActionLayout.calculate(260);

        assertTrue(layout.rightEdge() <= 260 - 12);
    }

    @Test
    void fullScreenListHitTestingUsesTheSameRowBoundsAsRendering() {
        assertEquals(2, FullConfigListLayout.visibleRows(180, 80));
        assertTrue(FullConfigListLayout.containsListPoint(12, 80, 300, 180, 80));
        assertFalse(FullConfigListLayout.containsListPoint(11, 80, 300, 180, 80));
        assertFalse(FullConfigListLayout.containsListPoint(20, 162, 300, 180, 80));
        assertEquals(1, FullConfigListLayout.rowIndexAt(20, 114, 300, 180, 80, 0, 3));
        assertEquals(-1, FullConfigListLayout.rowIndexAt(20, 110, 300, 180, 80, 0, 3));
        assertEquals(-1, FullConfigListLayout.rowIndexAt(20, 147, 300, 180, 80, 0, 3));
    }

    @Test
    void centersTextAgainstTheActualControlBounds() {
        assertEquals(15, FloatingTextLayout.centeredTextY(10, 18, 9));
        assertEquals(27, FloatingTextLayout.centeredTextY(20, 22, 9));
        assertEquals(5, FloatingTextLayout.centeredSymbolY(0, 16, 9));
        assertEquals(16, FloatingTextLayout.centeredTextX(10, 16, 4));
    }

}
