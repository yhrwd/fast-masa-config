package fastui.yure.client.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * 分组窗口的纯布局结果。所有尺寸和滚动偏移均以屏幕像素为单位。
 */
public record GroupWindowLayout(
        int x,
        int y,
        int width,
        int height,
        int safeMargin,
        int headerHeight,
        int contentHeight,
        int maxScrollOffset,
        List<Row> rows
) {
    /** 拖动和首次显示时，窗口与屏幕边缘保持的最小距离。 */
    public static final int SAFE_MARGIN = 10;
    /** 旧固定宽度布局的目标值；运行时悬浮菜单不再走这一条重载。 */
    public static final int DESIRED_WIDTH = 196;
    public static final int MIN_SAFE_WIDTH = 180;
    /** Meteor 窗口标题栏高度，同时也是折叠窗口的完整高度。 */
    public static final int HEADER_HEIGHT = 16;
    /** 内容面与每个配置行之间的水平、首尾垂直内边距。 */
    public static final int WINDOW_PADDING = 2;
    /** 内容行之间的垂直空白，改它时无需改命中测试，Row 已包含最终坐标。 */
    public static final int ROW_SPACING = 0;

    public GroupWindowLayout {
        rows = List.copyOf(rows);
    }

    /**
     * 计算窗口及其未滚动的内容行。折叠窗口始终只包含标题栏。
     */
    public static GroupWindowLayout calculate(int screenWidth, int screenHeight, int requestedX, int requestedY,
                                              boolean collapsed, int[] rowHeights) {
        return calculate(screenWidth, screenHeight, requestedX, requestedY, collapsed, rowHeights, null);
    }

    /**
     * 按 Meteor 的窗口内边距和垂直列表间距计算运行时悬浮菜单。
     *
     * <p>{@code contentWidth} 是 {@link FloatingGroupPanel} 根据标题和行文本测得的内容宽度，
     * 这里统一补上左右窗口内边距并按屏幕可用宽度裁剪。不要在绘制代码里再额外偏移行坐标，
     * 否则渲染矩形与 {@link GroupWindowHitTest} 会不一致。</p>
     */
    public static GroupWindowLayout calculate(int screenWidth, int screenHeight, int requestedX, int requestedY,
                                              boolean collapsed, int[] rowHeights, int contentWidth) {
        if (rowHeights == null) {
            throw new NullPointerException("rowHeights");
        }
        return calculate(screenWidth, screenHeight, requestedX, requestedY, collapsed, rowHeights,
                Integer.valueOf(contentWidth));
    }

    private static GroupWindowLayout calculate(int screenWidth, int screenHeight, int requestedX, int requestedY,
                                               boolean collapsed, int[] rowHeights, Integer measuredContentWidth) {
        if (rowHeights == null) {
            throw new NullPointerException("rowHeights");
        }

        int viewportWidth = Math.max(0, screenWidth);
        int viewportHeight = Math.max(0, screenHeight);
        int availableWidth = Math.max(0, viewportWidth - SAFE_MARGIN * 2);
        int availableHeight = Math.max(0, viewportHeight - SAFE_MARGIN * 2);
        int width = measuredContentWidth == null
                ? (availableWidth >= MIN_SAFE_WIDTH ? Math.min(DESIRED_WIDTH, availableWidth) : availableWidth)
                : Math.min(Math.max(0, measuredContentWidth) + WINDOW_PADDING * 2, availableWidth);
        int headerHeight = Math.min(HEADER_HEIGHT, availableHeight);
        int contentHeight = headerHeight;
        List<Row> rows = new ArrayList<>();

        if (!collapsed) {
            if (measuredContentWidth != null) {
                // 标题栏下方先留出内边距；每个 Row 保存的是未滚动的绝对屏幕坐标。
                contentHeight += WINDOW_PADDING;
            }
            for (int itemIndex = 0; itemIndex < rowHeights.length; itemIndex++) {
                int rowHeight = rowHeights[itemIndex];
                if (rowHeight <= 0) {
                    throw new IllegalArgumentException("row heights must be positive");
                }
                if (measuredContentWidth != null && !rows.isEmpty()) {
                    contentHeight += ROW_SPACING;
                }
                int rowX = measuredContentWidth == null ? 0 : WINDOW_PADDING;
                int rowWidth = measuredContentWidth == null ? width : Math.max(0, width - WINDOW_PADDING * 2);
                rows.add(new Row(itemIndex, rowX, contentHeight, rowWidth, rowHeight));
                contentHeight += rowHeight;
            }
            if (measuredContentWidth != null) {
                contentHeight += WINDOW_PADDING;
            }
        }

        int height = Math.min(contentHeight, availableHeight);
        return finishLayout(screenWidth, screenHeight, requestedX, requestedY, width, height, headerHeight,
                contentHeight, rows, measuredContentWidth != null);
    }

    private static GroupWindowLayout finishLayout(int screenWidth, int screenHeight, int requestedX, int requestedY,
                                                  int width, int height, int headerHeight, int contentHeight,
                                                  List<Row> rows, boolean rowsHaveRelativeX) {
        int viewportWidth = Math.max(0, screenWidth);
        int viewportHeight = Math.max(0, screenHeight);
        int x = clampPosition(requestedX, viewportWidth, width);
        int y = clampPosition(requestedY, viewportHeight, height);
        List<Row> positionedRows = rows.stream()
                .map(row -> rowsHaveRelativeX
                        ? new Row(row.itemIndex(), x + row.x(), y + row.y(), row.width(), row.height())
                        : new Row(row.itemIndex(), x, y + row.y(), width, row.height()))
                .toList();

        return new GroupWindowLayout(x, y, width, height, SAFE_MARGIN, headerHeight, contentHeight,
                Math.max(0, contentHeight - height), positionedRows);
    }

    public int clampScrollOffset(int scrollOffset) {
        return clamp(scrollOffset, 0, this.maxScrollOffset);
    }

    public boolean isRowFullyVisible(Row row, int scrollOffset) {
        int contentTop = this.y + this.headerHeight;
        int contentBottom = this.y + this.height;
        int rowTop = row.y() - this.clampScrollOffset(scrollOffset);
        return rowTop >= contentTop && rowTop + row.height() <= contentBottom;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static int clampPosition(int requestedPosition, int viewportSize, int windowSize) {
        int maximumPosition = Math.max(0, viewportSize - windowSize);
        if (viewportSize >= windowSize + SAFE_MARGIN * 2) {
            return clamp(requestedPosition, SAFE_MARGIN, viewportSize - SAFE_MARGIN - windowSize);
        }
        return clamp(requestedPosition, 0, maximumPosition);
    }

    public record Row(int itemIndex, int x, int y, int width, int height) {
    }
}
