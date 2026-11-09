package fastui.yure.client.gui;

import fastui.yure.config.GroupItem;
import fastui.yure.config.QuickMessageStore;
import fastui.yure.client.index.ConfigIndexService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickConfigGroupGeometryTest {
    @Test
    void dragClampUsesActualWindowDimensions() {
        assertArrayEquals(new int[]{320, 280}, FloatingGroupPanel.clampPosition(900, 900,
                600, 500, 260, 200, 20));
        assertArrayEquals(new int[]{20, 20}, FloatingGroupPanel.clampPosition(-10, -10,
                600, 500, 260, 200, 20));
    }

    @Test
    void normalizesMissingTargetToTheProtectedDefaultGroupBeforeControlsRender() {
        assertEquals("default", FastMasaConfigGui.normalizedTargetGroupId("", List.of("default", "building")));
        assertEquals("building", FastMasaConfigGui.normalizedTargetGroupId("building", List.of("default", "building")));
        assertEquals("", FastMasaConfigGui.normalizedTargetGroupId("missing", List.of("building")));
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
    void keepsGroupActionsInsideA260PixelViewport() {
        FastMasaConfigGui.GroupActionLayout layout = FastMasaConfigGui.GroupActionLayout.calculate(260);

        assertTrue(layout.rightEdge() <= 260 - 12);
    }
}
