package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupWindowResizeTest {
    private static final GroupWindowResize.Edge LEFT = GroupWindowResize.Edge.LEFT;
    private static final GroupWindowResize.Edge RIGHT = GroupWindowResize.Edge.RIGHT;

    @Test
    void leftDragKeepsRightEdgeFixedAndRightDragKeepsLeftEdgeFixed() {
        assertEquals(new GroupWindowResize.Bounds(70, 130), GroupWindowResize.resize(100, 100, -30, LEFT, 800));
        assertEquals(new GroupWindowResize.Bounds(130, 70), GroupWindowResize.resize(100, 100, 30, LEFT, 800));
        assertEquals(new GroupWindowResize.Bounds(100, 130), GroupWindowResize.resize(100, 100, 30, RIGHT, 800));
        assertEquals(new GroupWindowResize.Bounds(100, 70), GroupWindowResize.resize(100, 100, -30, RIGHT, 800));
    }

    @Test
    void stopsAtMinimumWidthWithoutMovingOppositeEdge() {
        assertEquals(new GroupWindowResize.Bounds(140, 60), GroupWindowResize.resize(100, 100, 500, LEFT, 800));
        assertEquals(new GroupWindowResize.Bounds(100, 60), GroupWindowResize.resize(100, 100, -500, RIGHT, 800));
    }

    @Test
    void clampsToScreenMarginsAndRecoversFromDraggingPastTheScreen() {
        assertEquals(new GroupWindowResize.Bounds(10, 190), GroupWindowResize.resize(100, 100, -900, LEFT, 800));
        assertEquals(new GroupWindowResize.Bounds(100, 690), GroupWindowResize.resize(100, 100, 900, RIGHT, 800));
        // Each event uses the original anchor; reversing an overshoot must not accumulate drift.
        assertEquals(new GroupWindowResize.Bounds(90, 110), GroupWindowResize.resize(100, 100, -10, LEFT, 800));
        assertEquals(new GroupWindowResize.Bounds(100, 110), GroupWindowResize.resize(100, 100, 10, RIGHT, 800));
    }

    @Test
    void fitsTinyScreensEvenWhenThereIsNoRoomForTheMinimumWidth() {
        for (int screenWidth : new int[]{0, 12, 30, 70}) {
            for (GroupWindowResize.Edge edge : new GroupWindowResize.Edge[]{LEFT, RIGHT}) {
                GroupWindowResize.Bounds bounds = GroupWindowResize.resize(10, 20, 500, edge, screenWidth);
                assertTrue(bounds.width() >= 0);
                assertTrue(bounds.x() >= 0);
                assertTrue(bounds.x() + bounds.width() <= screenWidth);
            }
        }
    }

    @Test
    void edgesAreEasyToHitFromOutsideWithoutTakingRowControlsOrHeaderCenter() {
        GroupWindowLayout layout = GroupWindowLayout.calculate(800, 600, 100, 80, false, new int[]{43}, 96);
        int right = layout.x() + layout.width();
        assertEquals(LEFT, GroupWindowResize.edgeAt(layout, 97, 90));
        assertEquals(LEFT, GroupWindowResize.edgeAt(layout, 101, 90));
        assertEquals(RIGHT, GroupWindowResize.edgeAt(layout, right + 2, 90));
        assertEquals(RIGHT, GroupWindowResize.edgeAt(layout, right - 1, 90));
        assertEquals(GroupWindowResize.Edge.NONE, GroupWindowResize.edgeAt(layout, 110, 90));
        assertEquals(GroupWindowResize.Edge.NONE, GroupWindowResize.edgeAt(layout, right - 7, 105));
        assertEquals(GroupWindowResize.Edge.NONE, GroupWindowResize.edgeAt(layout, 100, 79));
        assertEquals(GroupWindowResize.Edge.NONE, GroupWindowResize.edgeAt(layout, 100, layout.y() + layout.height()));
        assertEquals(GroupWindowResize.Edge.NONE, GroupWindowResize.edgeAt(null, 100, 80));
    }

}
