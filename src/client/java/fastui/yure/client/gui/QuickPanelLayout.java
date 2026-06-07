package fastui.yure.client.gui;

/**
 * 快捷面板的纯布局结果。
 * 这里不依赖 Minecraft 渲染类，方便单独计算尺寸、列数、可见行数和滚动范围。
 */
public record QuickPanelLayout(
        int x,
        int y,
        int width,
        int height,
        int columns,
        int visibleRows,
        int visibleItemCount,
        int maxScrollOffset
) {
    private static final int SAFE_MARGIN = 20;
    private static final int MIN_WIDTH = 214;
    private static final int MIN_HEIGHT = 54;
    private static final int MIN_CELL_WIDTH = 162;
    public static final int HEADER_HEIGHT = 25;
    public static final int ROW_HEIGHT = 25;
    public static final int GAP = 2;
    public static final int LIST_TOP_GAP = 4;
    public static final int PANEL_PADDING = 7;

    /**
     * 根据当前视窗、用户配置和快捷项数量计算面板最终布局。
     * configuredWidth/configuredMaxHeight 是配置里的基础尺寸，scale 会放大/缩小它们，
     * 但最终结果必须被限制在当前视窗安全边距内，避免 GUI scale 或小窗口导致面板溢出。
     */
    public static QuickPanelLayout calculate(int screenWidth, int screenHeight, int configuredWidth, int configuredMaxHeight, double scale, int rowCount) {
        // 缩放值可以很大，实际尺寸必须先收敛到当前视窗内，避免小窗口下遮挡原版 UI。
        int availableWidth = Math.max(MIN_WIDTH, screenWidth - SAFE_MARGIN * 2);
        int availableHeight = Math.max(MIN_HEIGHT, screenHeight - SAFE_MARGIN * 2);
        int scaledWidth = Math.max(MIN_WIDTH, (int) Math.round(configuredWidth * scale));
        int width = Math.min(scaledWidth, availableWidth);
        int columns = getColumnCount(width);
        int rows = Math.max(1, (int) Math.ceil(Math.max(1, rowCount) / (double) columns));
        int contentHeight = HEADER_HEIGHT + LIST_TOP_GAP + rows * ROW_HEIGHT + (rows - 1) * GAP + PANEL_PADDING;
        int scaledContentHeight = Math.max(MIN_HEIGHT, (int) Math.round(contentHeight * scale));
        int scaledMaxHeight = Math.max(MIN_HEIGHT, (int) Math.round(configuredMaxHeight * scale));
        int maxHeight = Math.max(MIN_HEIGHT, Math.min(scaledMaxHeight, availableHeight));
        int height = Math.min(scaledContentHeight, maxHeight);
        int availableRowsHeight = Math.max(ROW_HEIGHT, height - HEADER_HEIGHT - LIST_TOP_GAP - PANEL_PADDING);
        int visibleRows = Math.max(1, Math.min(rows, (availableRowsHeight + GAP) / (ROW_HEIGHT + GAP)));
        int visibleItemCount = Math.min(Math.max(1, rowCount), visibleRows * columns);
        int maxScrollOffset = Math.max(0, rowCount - visibleItemCount);

        return new QuickPanelLayout((screenWidth - width) / 2, (screenHeight - height) / 2, width, height, columns, visibleRows, visibleItemCount, maxScrollOffset);
    }

    /**
     * 按实际面板宽度选择 1/2/3 列。
     * 优先使用更多列提升紧凑度，但每列必须保留足够宽度给文字、开关、数值和滑条。
     */
    private static int getColumnCount(int width) {
        if (canFitColumns(width, 3)) {
            return 3;
        }

        if (canFitColumns(width, 2)) {
            return 2;
        }

        return 1;
    }

    /**
     * 判断指定列数是否能在当前面板宽度中安全摆下。
     * MIN_CELL_WIDTH 是视觉和交互的下限，小于它会出现文字截断过度或控件互相挤压。
     */
    private static boolean canFitColumns(int width, int columns) {
        // 列宽低于控件预留宽度时强制降列，避免文字、滑条和滚动条互相压住。
        int usableWidth = width - PANEL_PADDING * 2 - (columns - 1) * GAP;
        return usableWidth / columns >= MIN_CELL_WIDTH;
    }

    /**
     * 将外部传入的滚动偏移限制到当前布局允许范围内。
     * 快捷项数量变化或面板高度变化时，旧 offset 可能越界，需要在渲染和滚轮处理中统一收敛。
     */
    public int clampScrollOffset(int scrollOffset) {
        return Math.max(0, Math.min(this.maxScrollOffset, scrollOffset));
    }

    /**
     * 计算单个快捷项单元格宽度。
     * 渲染、鼠标命中和测试都用同一个公式，避免“看起来的位置”和“点中的位置”不一致。
     */
    public int cellWidth() {
        return (this.width - PANEL_PADDING * 2 - (this.columns - 1) * GAP) / this.columns;
    }
}
