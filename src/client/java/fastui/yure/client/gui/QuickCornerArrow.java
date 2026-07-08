package fastui.yure.client.gui;

/**
 * 快捷面板角标箭头的像素坐标。
 */
record QuickCornerArrow(int tipX, int tipY, int midX, int midY, int baseX, int baseY) {
    /**
     * anchorX/anchorY 是箭头 10x10 命中盒的内侧起点，方向值指向面板内部，箭头尖端反向朝外。
     */
    static QuickCornerArrow calculate(int anchorX, int anchorY, int horizontalDirection, int verticalDirection) {
        int tipX = horizontalDirection > 0 ? anchorX - 2 : anchorX + 8;
        int tipY = verticalDirection > 0 ? anchorY - 2 : anchorY + 8;
        int midX = tipX + horizontalDirection * 2;
        int midY = tipY + verticalDirection * 2;
        int baseX = tipX + horizontalDirection * 4;
        int baseY = tipY + verticalDirection * 4;

        return new QuickCornerArrow(tipX, tipY, midX, midY, baseX, baseY);
    }
}
