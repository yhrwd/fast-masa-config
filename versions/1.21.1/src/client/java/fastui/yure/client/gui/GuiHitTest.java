package fastui.yure.client.gui;

final class GuiHitTest {
    private GuiHitTest() {
    }

    static boolean contains(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
