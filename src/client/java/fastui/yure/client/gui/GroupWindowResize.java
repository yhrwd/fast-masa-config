package fastui.yure.client.gui;

/** Pure geometry for horizontal resizing. A drag always keeps the opposite edge fixed. */
public final class GroupWindowResize {
    public static final int MIN_WIDTH = 60;
    private static final int EDGE_OUTSET = 3;
    private static final int EDGE_WIDTH = 5;

    private GroupWindowResize() {
    }

    public enum Edge { NONE, LEFT, RIGHT }

    public record Bounds(int x, int width) {
    }

    public static boolean contains(GroupWindowLayout layout, int mouseX, int mouseY) {
        return layout != null && GuiHitTest.isInside(mouseX, mouseY,
                layout.x(), layout.y(), layout.width(), layout.height());
    }

    public static GroupWindowHitTest.Bounds edgeBounds(GroupWindowLayout layout, Edge edge) {
        int x = edge == Edge.LEFT ? layout.x() - EDGE_OUTSET : layout.x() + layout.width() - 2;
        return new GroupWindowHitTest.Bounds(x, layout.y(), EDGE_WIDTH, layout.height());
    }

    public static Edge edgeAt(GroupWindowLayout layout, int mouseX, int mouseY) {
        if (layout == null || layout.width() <= 0 || layout.height() <= 0) {
            return Edge.NONE;
        }
        if (edgeBounds(layout, Edge.LEFT).contains(mouseX, mouseY)) {
            return Edge.LEFT;
        }
        if (edgeBounds(layout, Edge.RIGHT).contains(mouseX, mouseY)) {
            return Edge.RIGHT;
        }
        return Edge.NONE;
    }

    public static Bounds resize(int startX, int startWidth, int deltaX, Edge edge, int screenWidth) {
        int margin = Math.min(GroupWindowLayout.SAFE_MARGIN, Math.max(0, screenWidth) / 2);
        int screenRight = Math.max(margin, screenWidth - margin);
        if (edge == Edge.LEFT) {
            int right = (int) Math.clamp((long) startX + startWidth, margin, screenRight);
            int maxWidth = right - margin;
            int width = (int) Math.clamp((long) startWidth - deltaX, Math.min(MIN_WIDTH, maxWidth), maxWidth);
            return new Bounds(right - width, width);
        }
        int left = Math.clamp(startX, margin, screenRight);
        int maxWidth = screenRight - left;
        int width = (int) Math.clamp((long) startWidth + deltaX, Math.min(MIN_WIDTH, maxWidth), maxWidth);
        return new Bounds(left, width);
    }
}
