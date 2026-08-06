package fastui.yure.client.gui;

/** 悬浮菜单文本和小控件共用的像素级对齐计算。 */
final class FloatingTextLayout {
    private FloatingTextLayout() {
    }

    static int centeredTextY(int top, int height, int lineHeight) {
        // Font.drawString 的 y 是字形顶部落点；剩余奇数像素向下分配，避免文字整体偏上。
        return top + Math.max(0, (height - lineHeight + 1) / 2);
    }

    static int centeredSymbolY(int top, int height, int lineHeight) {
        // +/- 与标签使用同一视觉基线，并额外下移一个像素修正符号的字形重心。
        return centeredTextY(top, height, lineHeight) + 1;
    }

    static int centeredTextX(int left, int width, int textWidth) {
        return left + Math.max(0, (width - textWidth) / 2);
    }
}
