package fastui.yure.client.gui;

import fastui.yure.config.FastMasaConfigs;
import fastui.yure.config.QuickMessage;
import fastui.yure.config.QuickMessageGroup;
import fastui.yure.config.QuickMessageStore;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.gui.Font;

import java.util.List;

/** 快捷消息专用悬浮窗口。消息行仅负责发送，不参与配置项的控件逻辑。 */
public final class FloatingMessagePanel {
    private static final int ROW_HEIGHT = 15;
    private static final int ROW_PADDING = 5;
    private static final int MIN_CONTENT_WIDTH = 56;
    private static final int MAX_CONTENT_WIDTH = 92;
    private static final int[] EMPTY_ROW_HEIGHTS = new int[0];

    private final Font font;
    private final String groupId;
    private GroupWindowLayout layout;
    private long contentRevision = -1;
    private int contentWidth;
    private int[] rowHeights = EMPTY_ROW_HEIGHTS;
    private List<String> renderedNames = List.of();
    private String renderedTitle = "";
    private long textRevision = -1;
    private int textWidth = -1;
    private boolean textCollapsed;
    private int layoutScreenWidth = -1;
    private int layoutScreenHeight = -1;
    private int layoutRequestedX;
    private int layoutRequestedY;
    private int layoutContentWidth = -1;
    private boolean layoutCollapsed;
    private int scrollOffset;

    public FloatingMessagePanel(Font font, String groupId) {
        this.font = font;
        this.groupId = groupId;
    }

    public String groupId() {
        return this.groupId;
    }

    public int x() {
        return this.layout == null ? 0 : this.layout.x();
    }

    public int y() {
        return this.layout == null ? 0 : this.layout.y();
    }

    public boolean isCollapseHit(int mouseX, int mouseY) {
        return this.layout != null && this.layout.headerHeight() >= 16 && this.layout.width() >= 20
                && GuiHitTest.isInside(mouseX, mouseY, this.layout.x() + this.layout.width() - 20,
                this.layout.y(), 20, this.layout.headerHeight());
    }

    public void toggleCollapsed() {
        QuickMessageStore.get(this.groupId).ifPresent(group -> QuickMessageStore.setWindowState(group.id(),
                !group.collapsed(), group.x(), group.y()));
    }

    public boolean moveTo(int requestedX, int requestedY, int screenWidth, int screenHeight) {
        if (this.layout == null) {
            return false;
        }
        int[] position = FloatingGroupPanel.clampPosition(requestedX, requestedY, screenWidth, screenHeight,
                this.layout.width(), this.layout.height(), this.layout.safeMargin());
        return QuickMessageStore.get(this.groupId).map(group -> QuickMessageStore.setWindowState(group.id(),
                group.collapsed(), position[0], position[1])).filter(updated ->
                position[0] != this.layout.x() || position[1] != this.layout.y()).orElse(false);
    }

    public void render(GuiContext context, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        QuickMessageGroup group = QuickMessageStore.get(this.groupId).orElse(null);
        if (group == null) {
            this.layout = null;
            return;
        }
        this.refreshContent(group);
        this.refreshLayout(group, screenWidth, screenHeight);
        this.refreshText(group);
        this.scrollOffset = this.layout.clampScrollOffset(this.scrollOffset);
        if (this.layout.width() <= 0 || this.layout.height() <= 0) {
            return;
        }

        int alpha = FastMasaConfigs.Generic.FLOATING_BACKGROUND_ALPHA.getIntegerValue();
        int headerControlsWidth = this.font.width("+") + ROW_PADDING * 2;
        RenderUtils.drawRect(context, this.layout.x(), this.layout.y() + this.layout.headerHeight(), this.layout.width(),
                Math.max(0, this.layout.height() - this.layout.headerHeight()),
                HoloPanelVisuals.withAlpha(FastMasaMenuPalette.WINDOW_BACKGROUND, alpha));
        RenderUtils.drawRect(context, this.layout.x(), this.layout.y(), this.layout.width(), this.layout.headerHeight(),
                HoloPanelVisuals.withAlpha(FastMasaMenuPalette.accent(), alpha));
        if (this.layout.headerHeight() >= 16 && this.layout.width() > headerControlsWidth + ROW_PADDING * 2) {
            context.drawString(this.font, this.renderedTitle, this.layout.x() + ROW_PADDING,
                    FloatingTextLayout.centeredTextY(this.layout.y(), this.layout.headerHeight(), this.font.lineHeight),
                    FastMasaMenuPalette.TEXT, false);
        }
        String control = group.collapsed() ? "+" : "-";
        if (this.layout.headerHeight() >= 16 && this.layout.width() >= 20) {
            int controlX = this.layout.x() + this.layout.width() - headerControlsWidth + ROW_PADDING;
            context.drawString(this.font, control,
                    FloatingTextLayout.centeredTextX(controlX, headerControlsWidth - ROW_PADDING * 2,
                            this.font.width(control)),
                    FloatingTextLayout.centeredTextY(this.layout.y(), this.layout.headerHeight(), this.font.lineHeight),
                    FastMasaMenuPalette.TEXT, false);
        }
        if (group.collapsed()) {
            return;
        }

        List<QuickMessage> messages = group.messages();
        for (GroupWindowLayout.Row row : this.layout.rows()) {
            if (!this.layout.isRowFullyVisible(row, this.scrollOffset) || row.itemIndex() >= messages.size()) {
                continue;
            }
            int y = row.y() - this.scrollOffset;
            boolean hovered = GuiHitTest.isInside(mouseX, mouseY, row.x(), y, row.width(), row.height());
            QuickMessage message = messages.get(row.itemIndex());
            int color = hovered ? FastMasaMenuPalette.ROW_HOVER : FastMasaMenuPalette.ROW;
            RenderUtils.drawRect(context, row.x(), y, row.width(), row.height(), HoloPanelVisuals.withAlpha(color, alpha));
            RenderUtils.drawRect(context, row.x(), y, 2, row.height(), message.isCommand()
                    ? FastMasaMenuPalette.accent() : FastMasaMenuPalette.NEUTRAL);
            context.drawString(this.font, this.renderedNames.get(row.itemIndex()),
                    row.x() + ROW_PADDING,
                    FloatingTextLayout.centeredTextY(y, row.height(), this.font.lineHeight), FastMasaMenuPalette.TEXT,
                    false);
        }

        if (this.layout.maxScrollOffset() > 0) {
            int trackY = this.layout.y() + this.layout.headerHeight();
            int trackHeight = this.layout.height() - this.layout.headerHeight();
            int thumbHeight = Math.max(12,
                    trackHeight * this.layout.height() / Math.max(trackHeight, this.layout.contentHeight()));
            int travel = Math.max(1, trackHeight - thumbHeight);
            int thumbY = trackY + (int) Math.round(
                    travel * (this.scrollOffset / (double) this.layout.maxScrollOffset()));
            RenderUtils.drawRect(context, this.layout.x() + this.layout.width() - 4, trackY, 2, trackHeight,
                    0x661A1A1D);
            RenderUtils.drawRect(context, this.layout.x() + this.layout.width() - 4, thumbY, 2, thumbHeight,
                    FastMasaMenuPalette.accent());
        }
    }

    public GroupWindowHitTest.Result hitTest(int mouseX, int mouseY) {
        if (this.layout == null) {
            return new GroupWindowHitTest.Result(GroupWindowHitTest.Target.NONE, -1);
        }
        GroupWindowHitTest.Bounds scrollbar = this.layout.maxScrollOffset() > 0
                ? new GroupWindowHitTest.Bounds(this.layout.x() + this.layout.width() - 7,
                this.layout.y() + this.layout.headerHeight(), 8, this.layout.height() - this.layout.headerHeight()) : null;
        return GroupWindowHitTest.hitTest(this.layout, this.scrollOffset, mouseX, mouseY, scrollbar, List.of());
    }

    public QuickMessage messageAt(int index) {
        return QuickMessageStore.get(this.groupId).filter(group -> index >= 0 && index < group.messages().size())
                .map(group -> group.messages().get(index)).orElse(null);
    }

    public void scroll(double verticalAmount) {
        if (this.layout != null) {
            this.scrollOffset = this.layout.clampScrollOffset(this.scrollOffset + (verticalAmount < 0 ? ROW_HEIGHT : -ROW_HEIGHT));
        }
    }

    private void refreshContent(QuickMessageGroup group) {
        if (this.contentRevision == QuickMessageStore.contentRevision()) {
            return;
        }
        this.rowHeights = new int[group.messages().size()];
        java.util.Arrays.fill(this.rowHeights, ROW_HEIGHT);
        int width = this.font.width(group.name()) + 22;
        for (QuickMessage message : group.messages()) {
            width = Math.max(width, this.font.width(message.displayName()) + ROW_PADDING * 2);
        }
        this.contentWidth = Math.max(MIN_CONTENT_WIDTH, Math.min(MAX_CONTENT_WIDTH, width));
        this.contentRevision = QuickMessageStore.contentRevision();
        this.layout = null;
        this.textRevision = -1;
    }

    private void refreshLayout(QuickMessageGroup group, int screenWidth, int screenHeight) {
        if (this.layout != null && this.layoutScreenWidth == screenWidth && this.layoutScreenHeight == screenHeight
                && this.layoutRequestedX == group.x() && this.layoutRequestedY == group.y()
                && this.layoutCollapsed == group.collapsed() && this.layoutContentWidth == this.contentWidth) {
            return;
        }
        this.layout = GroupWindowLayout.calculate(screenWidth, screenHeight, group.x(), group.y(), group.collapsed(),
                group.collapsed() ? EMPTY_ROW_HEIGHTS : this.rowHeights, this.contentWidth);
        this.layoutScreenWidth = screenWidth;
        this.layoutScreenHeight = screenHeight;
        this.layoutRequestedX = group.x();
        this.layoutRequestedY = group.y();
        this.layoutCollapsed = group.collapsed();
        this.layoutContentWidth = this.contentWidth;
    }

    private void refreshText(QuickMessageGroup group) {
        int width = this.layout == null ? 0 : this.layout.width();
        boolean collapsed = group.collapsed();
        if (this.textRevision == this.contentRevision && this.textWidth == width && this.textCollapsed == collapsed) {
            return;
        }
        int headerControlsWidth = this.font.width("+") + ROW_PADDING * 2;
        this.renderedTitle = fitText(group.name(), width - headerControlsWidth - ROW_PADDING * 2);
        if (collapsed || this.layout == null) {
            this.renderedNames = List.of();
        } else {
            List<String> names = new java.util.ArrayList<>(group.messages().size());
            for (int index = 0; index < group.messages().size(); index++) {
                GroupWindowLayout.Row row = this.layout.rows().get(index);
                names.add(fitText(group.messages().get(index).displayName(), row.width() - ROW_PADDING * 2));
            }
            this.renderedNames = List.copyOf(names);
        }
        this.textRevision = this.contentRevision;
        this.textWidth = width;
        this.textCollapsed = collapsed;
    }

    private String fitText(String text, int maxWidth) {
        return FloatingGroupPanel.fitText(text, maxWidth, this.font::width);
    }
}
