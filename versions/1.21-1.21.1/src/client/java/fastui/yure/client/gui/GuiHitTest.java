package fastui.yure.client.gui;

final class GuiHitTest {
    private GuiHitTest() {
    }

    static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
