package fastui.yure.client.gui;

/** 全部分组隐藏时使用的紧凑恢复入口布局。 */
record RecoveryActionLayout(int x, int y, int width, int height) {
    private static final int MARGIN = 4;
    private static final int SIZE = 20;

    static RecoveryActionLayout calculate(int screenWidth, int screenHeight) {
        int width = Math.min(SIZE, Math.max(0, screenWidth));
        int height = Math.min(SIZE, Math.max(0, screenHeight));
        int x = Math.max(0, screenWidth - MARGIN - width);
        int y = Math.min(MARGIN, Math.max(0, screenHeight - height));
        return new RecoveryActionLayout(x, y, width, height);
    }
}
