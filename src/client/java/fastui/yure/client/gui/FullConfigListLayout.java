package fastui.yure.client.gui;

/** 全屏配置页共用的列表行、滚动与命中几何。 */
final class FullConfigListLayout {
    static final int MARGIN = 12;
    static final int ROW_HEIGHT = 30;
    static final int ROW_GAP = 3;

    private FullConfigListLayout() {
    }

    static int visibleRows(int screenHeight, int listTop) {
        return Math.max(1, (screenHeight - 18 - listTop) / (ROW_HEIGHT + ROW_GAP));
    }

    static boolean containsListPoint(int mouseX, int mouseY, int screenWidth, int screenHeight, int listTop) {
        int bottom = screenHeightBottom(screenHeight);
        return GuiHitTest.isInside(mouseX, mouseY, MARGIN, listTop, screenWidth - MARGIN * 2, bottom - listTop);
    }

    static int rowIndexAt(int mouseX, int mouseY, int screenWidth, int screenHeight, int listTop, int scrollOffset,
            int rowCount) {
        if (!containsListPoint(mouseX, mouseY, screenWidth, screenHeight, listTop)) {
            return -1;
        }
        int visibleIndex = (mouseY - listTop) / (ROW_HEIGHT + ROW_GAP);
        if (visibleIndex >= visibleRows(screenHeight, listTop)) {
            return -1;
        }
        int index = scrollOffset + visibleIndex;
        int rowY = listTop + visibleIndex * (ROW_HEIGHT + ROW_GAP);
        return mouseY >= rowY + ROW_HEIGHT || index < 0 || index >= rowCount ? -1 : index;
    }

    private static int screenHeightBottom(int screenHeight) {
        return screenHeight - 18;
    }
}
