package fastui.yure.client.gui;

import java.util.List;

/**
 * 分组窗口的纯鼠标命中测试。控件边界由调用方按未滚动的内容坐标提供。
 */
public final class GroupWindowHitTest {
    private static final int NO_ITEM = -1;

    private GroupWindowHitTest() {
    }

    public static Result hitTest(GroupWindowLayout layout, int scrollOffset, int mouseX, int mouseY, Bounds scrollbar,
                                 List<ItemControls> itemControls) {
        if (GuiHitTest.isInside(mouseX, mouseY, layout.x(), layout.y(), layout.width(), layout.headerHeight())) {
            return new Result(Target.HEADER, NO_ITEM);
        }

        int clampedScrollOffset = layout.clampScrollOffset(scrollOffset);
        Bounds contentViewport = new Bounds(layout.x(), layout.y() + layout.headerHeight(), layout.width(),
                layout.height() - layout.headerHeight());

        if (scrollbar != null && contentViewport.contains(mouseX, mouseY) && scrollbar.contains(mouseX, mouseY)) {
            return new Result(Target.SCROLLBAR, NO_ITEM);
        }

        for (ItemControls controls : itemControls) {
            if (isInsideContent(mouseX, mouseY, contentViewport, controls.slider(), clampedScrollOffset)) {
                return new Result(Target.SLIDER, controls.itemIndex());
            }
        }

        for (ItemControls controls : itemControls) {
            if (isInsideContent(mouseX, mouseY, contentViewport, controls.expand(), clampedScrollOffset)) {
                return new Result(Target.EXPAND, controls.itemIndex());
            }
        }

        for (GroupWindowLayout.Row row : layout.rows()) {
            if (isFullyInsideContent(contentViewport, row.x(), row.y() - clampedScrollOffset, row.width(), row.height())
                    && contentViewport.contains(mouseX, mouseY)
                    && GuiHitTest.isInside(mouseX, mouseY, row.x(), row.y() - clampedScrollOffset,
                    row.width(), row.height())) {
                return new Result(Target.ROW, row.itemIndex());
            }
        }

        return new Result(Target.NONE, NO_ITEM);
    }

    private static boolean isInsideContent(int mouseX, int mouseY, Bounds contentViewport, Bounds bounds,
                                            int scrollOffset) {
        return bounds != null && isFullyInsideContent(contentViewport, bounds.x(), bounds.y() - scrollOffset,
                bounds.width(), bounds.height()) && contentViewport.contains(mouseX, mouseY)
                && GuiHitTest.isInside(mouseX, mouseY, bounds.x(), bounds.y() - scrollOffset,
                bounds.width(), bounds.height());
    }

    private static boolean isFullyInsideContent(Bounds viewport, int x, int y, int width, int height) {
        return x >= viewport.x() && y >= viewport.y() && x + width <= viewport.x() + viewport.width()
                && y + height <= viewport.y() + viewport.height();
    }

    public enum Target {
        NONE,
        HEADER,
        ROW,
        EXPAND,
        SLIDER,
        SCROLLBAR
    }

    public record Result(Target target, int itemIndex) {
    }

    public record Bounds(int x, int y, int width, int height) {
        boolean contains(int mouseX, int mouseY) {
            return GuiHitTest.isInside(mouseX, mouseY, this.x, this.y, this.width, this.height);
        }
    }

    public record ItemControls(int itemIndex, Bounds expand, Bounds slider) {
    }
}
