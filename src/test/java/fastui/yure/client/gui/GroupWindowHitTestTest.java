package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupWindowHitTestTest {
    @Test
    void prioritizesHeaderOverAllOtherTargets() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(400, 300, 40, 40, false, new int[]{51});

        GroupWindowHitTest.Result result = GroupWindowHitTest.hitTest(layout, 0, 45, 45,
                new GroupWindowHitTest.Bounds(40, 40, 20, 20),
                List.of(new GroupWindowHitTest.ItemControls(0,
                        new GroupWindowHitTest.Bounds(40, 40, 20, 20),
                        new GroupWindowHitTest.Bounds(40, 40, 20, 20))));

        assertEquals(new GroupWindowHitTest.Result(GroupWindowHitTest.Target.HEADER, -1), result);
    }

    @Test
    void sliderWinsOverExpandedRowAndExpandAffordanceWinsOverRow() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(400, 300, 40, 40, false, new int[]{51});
        GroupWindowHitTest.ItemControls controls = new GroupWindowHitTest.ItemControls(0,
                new GroupWindowHitTest.Bounds(50, 65, 20, 20),
                new GroupWindowHitTest.Bounds(100, 70, 60, 15));

        assertEquals(new GroupWindowHitTest.Result(GroupWindowHitTest.Target.SLIDER, 0),
                GroupWindowHitTest.hitTest(layout, 0, 110, 75, null, List.of(controls)));
        assertEquals(new GroupWindowHitTest.Result(GroupWindowHitTest.Target.EXPAND, 0),
                GroupWindowHitTest.hitTest(layout, 0, 55, 70, null, List.of(controls)));
        assertEquals(new GroupWindowHitTest.Result(GroupWindowHitTest.Target.ROW, 0),
                GroupWindowHitTest.hitTest(layout, 0, 45, 100, null, List.of(controls)));
    }

    @Test
    void recognizesScrollbarAndExcludesRightAndBottomBounds() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(400, 300, 40, 40, false, new int[]{25});
        GroupWindowHitTest.Bounds scrollbar = new GroupWindowHitTest.Bounds(294, 60, 6, 25);

        assertEquals(new GroupWindowHitTest.Result(GroupWindowHitTest.Target.SCROLLBAR, -1),
                GroupWindowHitTest.hitTest(layout, 0, 294, 60, scrollbar, List.of()));
        assertEquals(new GroupWindowHitTest.Result(GroupWindowHitTest.Target.NONE, -1),
                GroupWindowHitTest.hitTest(layout, 0, 300, 85, scrollbar, List.of()));
        assertEquals(new GroupWindowHitTest.Result(GroupWindowHitTest.Target.NONE, -1),
                GroupWindowHitTest.hitTest(layout, 0, 20, 20, scrollbar, List.of()));
    }

    @Test
    void resolvesTheCorrectRowAfterScrolling() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(300, 100, 20, 20, false, new int[]{25, 51});

        assertEquals(new GroupWindowHitTest.Result(GroupWindowHitTest.Target.ROW, 1),
                GroupWindowHitTest.hitTest(layout, 25, 30, 50, null, List.of()));
    }

    @Test
    void clipsRowsAndControlsToTheContentViewport() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(300, 100, 20, 20, false, new int[]{25, 51});
        GroupWindowHitTest.ItemControls controls = new GroupWindowHitTest.ItemControls(1,
                new GroupWindowHitTest.Bounds(50, 75, 20, 20),
                new GroupWindowHitTest.Bounds(100, 75, 60, 20));

        assertEquals(new GroupWindowHitTest.Result(GroupWindowHitTest.Target.NONE, -1),
                GroupWindowHitTest.hitTest(layout, 0, 30, 90, null, List.of(controls)));
        assertEquals(new GroupWindowHitTest.Result(GroupWindowHitTest.Target.NONE, -1),
                GroupWindowHitTest.hitTest(layout, 0, 110, 85, null, List.of(controls)));
    }

    @Test
    void scrollbarWinsOverRowAtTheRightEdge() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(400, 300, 40, 40, false, new int[]{25});
        GroupWindowHitTest.Bounds scrollbar = new GroupWindowHitTest.Bounds(294, 60, 6, 25);

        assertEquals(new GroupWindowHitTest.Result(GroupWindowHitTest.Target.SCROLLBAR, -1),
                GroupWindowHitTest.hitTest(layout, 0, 294, 60, scrollbar, List.of()));
    }

    @Test
    void scrollbarWinsOverOverlappingExpandedRowControls() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(400, 300, 40, 40, false, new int[]{51});
        GroupWindowHitTest.Bounds scrollbar = new GroupWindowHitTest.Bounds(294, 60, 6, 25);
        GroupWindowHitTest.ItemControls controls = new GroupWindowHitTest.ItemControls(0,
                new GroupWindowHitTest.Bounds(290, 65, 10, 20),
                new GroupWindowHitTest.Bounds(290, 65, 10, 20));

        assertEquals(new GroupWindowHitTest.Result(GroupWindowHitTest.Target.SCROLLBAR, -1),
                GroupWindowHitTest.hitTest(layout, 0, 294, 70, scrollbar, List.of(controls)));
    }
}
