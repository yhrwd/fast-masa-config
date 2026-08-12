package fastui.yure.client.gui;

/** Pure geometry for the full-screen configuration page header and tab strip. */
final class FullConfigPageLayout {
    static final int TAB_Y = 28;
    static final int BUTTON_HEIGHT = 20;
    static final int GAP = 4;
    private FullConfigPageLayout() {
    }

    static TabStrip calculateTabStrip(int screenWidth, int[] buttonWidths, boolean compactLabels) {
        if (buttonWidths.length == 0) {
            return new TabStrip(new int[0], new int[0], new int[0], TAB_Y + GAP, compactLabels);
        }
        int availableWidth = Math.max(1, screenWidth - FullConfigListLayout.MARGIN * 2);
        int[] x = new int[buttonWidths.length];
        int[] y = new int[buttonWidths.length];
        int rowX = FullConfigListLayout.MARGIN;
        int rowY = TAB_Y;
        for (int index = 0; index < buttonWidths.length; index++) {
            int width = Math.min(availableWidth, Math.max(1, buttonWidths[index]));
            if (rowX > FullConfigListLayout.MARGIN && rowX + width > screenWidth - FullConfigListLayout.MARGIN) {
                rowX = FullConfigListLayout.MARGIN;
                rowY += BUTTON_HEIGHT + GAP;
            }
            x[index] = rowX;
            y[index] = rowY;
            rowX += width + GAP;
        }
        return new TabStrip(buttonWidths.clone(), x, y, rowY + BUTTON_HEIGHT + 6, compactLabels);
    }

    static record TabStrip(int[] buttonWidths, int[] xPositions, int[] yPositions, int contentTop,
            boolean usesCompactLabels) {
        int xFor(int index) {
            return xPositions[index];
        }

        int yFor(int index) {
            return yPositions[index];
        }

        int buttonWidth(int index) {
            return buttonWidths[index];
        }
    }
}
