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
    public static final int SAFE_MARGIN = 20;
    public static final int DESIRED_WIDTH = 196;
    public static final int MIN_SAFE_WIDTH = 180;
    public static final int HEADER_HEIGHT = 20;

    public GroupWindowLayout {
        rows = List.copyOf(rows);
    }

    /**
     * 计算窗口及其未滚动的内容行。折叠窗口始终只包含标题栏。
     */
    public static GroupWindowLayout calculate(int screenWidth, int screenHeight, int requestedX, int requestedY,
                                              boolean collapsed, int[] rowHeights) {
        if (rowHeights == null) {
            throw new NullPointerException("rowHeights");
        }

        int viewportWidth = Math.max(0, screenWidth);
        int viewportHeight = Math.max(0, screenHeight);
        int availableWidth = Math.max(0, viewportWidth - SAFE_MARGIN * 2);
        int availableHeight = Math.max(0, viewportHeight - SAFE_MARGIN * 2);
        int width = availableWidth >= MIN_SAFE_WIDTH
                ? Math.min(DESIRED_WIDTH, availableWidth)
                : availableWidth;
        int headerHeight = Math.min(HEADER_HEIGHT, availableHeight);
        int contentHeight = headerHeight;
        List<Row> rows = new ArrayList<>();

        if (!collapsed) {
            for (int itemIndex = 0; itemIndex < rowHeights.length; itemIndex++) {
                int rowHeight = rowHeights[itemIndex];
                if (rowHeight <= 0) {
                    throw new IllegalArgumentException("row heights must be positive");
                }
                rows.add(new Row(itemIndex, 0, contentHeight, width, rowHeight));
                contentHeight += rowHeight;
            }
        }

        int height = Math.min(contentHeight, availableHeight);
        int x = clampPosition(requestedX, viewportWidth, width);
        int y = clampPosition(requestedY, viewportHeight, height);
        List<Row> positionedRows = rows.stream()
                .map(row -> new Row(row.itemIndex(), x, y + row.y(), width, row.height()))
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
