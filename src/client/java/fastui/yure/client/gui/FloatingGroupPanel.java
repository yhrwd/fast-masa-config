package fastui.yure.client.gui;

import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.client.shortcut.ResolvedShortcut;
import fastui.yure.client.shortcut.ShortcutControl;
import fastui.yure.client.shortcut.ShortcutResolver;
import fastui.yure.config.ConfigGroup;
import fastui.yure.config.ConfigGroupStore;
import fastui.yure.config.GroupItem;
import fastui.yure.config.ShortcutControlType;
import fastui.yure.config.ShortcutEntry;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;

/** 分组浮动窗口。布局、绘制和命中测试共用同一份行模型。 */
public final class FloatingGroupPanel {
    private static final int TEXT = 0xFFFFEAF2;
    private static final int MUTED = 0xFFCFA4B7;
    private static final int BASE = 0xFF1A1A1D;
    private static final int TRACK = 0xFF5B3A48;
    private static final int ROW_HEIGHT = 25;
    private static final int EXPANDED_HEIGHT = 42;
    private static final int SLIDER_WIDTH = 76;

    private final Font font;
    private final String groupId;
    private GroupWindowLayout layout;
    private List<RowModel> rows = List.of();
    private List<GroupWindowHitTest.ItemControls> controls = List.of();
    private int scrollOffset;

    public FloatingGroupPanel(Font font, String groupId) {
        this.font = font;
        this.groupId = groupId;
    }

    public String groupId() {
        return this.groupId;
    }

    public int x() { return this.layout == null ? 0 : this.layout.x(); }
    public int y() { return this.layout == null ? 0 : this.layout.y(); }
    public int width() { return this.layout == null ? 0 : this.layout.width(); }

    public boolean isCollapseHit(int mouseX, int mouseY) {
        return this.layout != null && GuiHitTest.isInside(mouseX, mouseY,
                this.layout.x() + this.layout.width() - 20, this.layout.y(), 20, this.layout.headerHeight());
    }

    public boolean isHideHit(int mouseX, int mouseY) {
        return this.layout != null && GuiHitTest.isInside(mouseX, mouseY,
                this.layout.x() + this.layout.width() - 40, this.layout.y(), 20, this.layout.headerHeight());
    }

    public boolean isFullConfigHit(int mouseX, int mouseY) {
        return this.layout != null && GuiHitTest.isInside(mouseX, mouseY,
                this.layout.x() + this.layout.width() - 60, this.layout.y(), 20, this.layout.headerHeight());
    }

    public void toggleCollapsed() {
        ConfigGroupStore.get(this.groupId).ifPresent(group -> ConfigGroupStore.setWindowState(group.id(),
                !group.collapsed(), group.x(), group.y()));
    }

    public boolean hide() {
        return ConfigGroupStore.hide(this.groupId, true);
    }

    public boolean moveTo(int requestedX, int requestedY, int screenWidth, int screenHeight) {
        if (this.layout == null) {
            return false;
        }
        int[] position = clampPosition(requestedX, requestedY, screenWidth, screenHeight, this.layout.width(),
                this.layout.height(), this.layout.safeMargin());
        int clampedX = position[0];
        int clampedY = position[1];
        return ConfigGroupStore.get(this.groupId).map(group -> {
            if (!positionChangedFromLayout(this.layout.x(), this.layout.y(), clampedX, clampedY)) {
                return false;
            }
            return ConfigGroupStore.setWindowState(group.id(), group.collapsed(), clampedX, clampedY);
        }).orElse(false);
    }

    static boolean positionChanged(int storedX, int storedY, int candidateX, int candidateY) {
        return storedX != candidateX || storedY != candidateY;
    }

    static boolean positionChangedFromLayout(int renderedX, int renderedY, int candidateX, int candidateY) {
        return positionChanged(renderedX, renderedY, candidateX, candidateY);
    }

    static int[] clampPosition(int requestedX, int requestedY, int screenWidth, int screenHeight,
            int windowWidth, int windowHeight, int safeMargin) {
        int maxX = Math.max(safeMargin, screenWidth - safeMargin - windowWidth);
        int maxY = Math.max(safeMargin, screenHeight - safeMargin - windowHeight);
        int clampedX = screenWidth >= windowWidth + safeMargin * 2
                ? Math.max(safeMargin, Math.min(maxX, requestedX))
                : Math.max(0, Math.min(Math.max(0, screenWidth - windowWidth), requestedX));
        int clampedY = screenHeight >= windowHeight + safeMargin * 2
                ? Math.max(safeMargin, Math.min(maxY, requestedY))
                : Math.max(0, Math.min(Math.max(0, screenHeight - windowHeight), requestedY));
        return new int[]{clampedX, clampedY};
    }

    public void render(GuiContext context, int screenWidth, int screenHeight, int mouseX, int mouseY,
            List<ConfigIndexEntry> index) {
        ConfigGroup group = ConfigGroupStore.get(this.groupId).orElse(null);
        if (group == null) {
            this.layout = null;
            return;
        }

        if (group.collapsed()) {
            this.rows = List.of();
            this.controls = List.of();
            this.layout = GroupWindowLayout.calculate(screenWidth, screenHeight, group.x(), group.y(), true,
                    new int[0]);
        } else {
            this.rows = buildRows(group, index);
            int[] rowHeights = this.rows.stream().mapToInt(RowModel::height).toArray();
            this.layout = GroupWindowLayout.calculate(screenWidth, screenHeight, group.x(), group.y(), false,
                    rowHeights);
            this.controls = buildControls();
        }
        this.scrollOffset = this.layout.clampScrollOffset(this.scrollOffset);

        int accent = group.color();
        RenderUtils.drawRect(context, this.layout.x(), this.layout.y(), this.layout.width(), this.layout.height(),
                HoloPanelVisuals.withAlpha(BASE, 0xE8));
        RenderUtils.drawRect(context, this.layout.x(), this.layout.y(), this.layout.width(), this.layout.headerHeight(),
                HoloPanelVisuals.withAlpha(accent, 0xD8));
        context.drawString(this.font, fitText(group.name(), this.layout.width() - 54), this.layout.x() + 7,
                this.layout.y() + 6, TEXT, false);
        context.drawString(this.font, StringUtils.translate("fast-masa-config.gui.floating.full_config"),
                this.layout.x() + this.layout.width() - 57, this.layout.y() + 5, TEXT, false);
        context.drawString(this.font, StringUtils.translate("fast-masa-config.gui.floating.hide"),
                this.layout.x() + this.layout.width() - 34, this.layout.y() + 5, TEXT, false);
        context.drawString(this.font, StringUtils.translate(group.collapsed() ? "fast-masa-config.gui.floating.expand"
                : "fast-masa-config.gui.floating.collapse"), this.layout.x() + this.layout.width() - 14,
                this.layout.y() + 5, TEXT, false);

        if (group.collapsed()) {
            return;
        }

        for (int indexInRows = 0; indexInRows < this.rows.size(); indexInRows++) {
            GroupWindowLayout.Row row = this.layout.rows().get(indexInRows);
            if (!this.layout.isRowFullyVisible(row, this.scrollOffset)) {
                continue;
            }
            int y = row.y() - this.scrollOffset;
            drawRow(context, this.rows.get(indexInRows), row.x(), y, row.width(), mouseX, mouseY, accent);
        }

        if (this.layout.maxScrollOffset() > 0) {
            int trackY = this.layout.y() + this.layout.headerHeight();
            int trackHeight = this.layout.height() - this.layout.headerHeight();
            int thumbHeight = Math.max(12, trackHeight * this.layout.height() / Math.max(trackHeight, this.layout.contentHeight()));
            int travel = Math.max(1, trackHeight - thumbHeight);
            int thumbY = trackY + (int) Math.round(travel * (this.scrollOffset / (double) this.layout.maxScrollOffset()));
            RenderUtils.drawRect(context, this.layout.x() + this.layout.width() - 4, trackY, 2, trackHeight, 0x661A1A1D);
            RenderUtils.drawRect(context, this.layout.x() + this.layout.width() - 5, thumbY, 4, thumbHeight, accent);
        }
    }

    public GroupWindowHitTest.Result hitTest(int mouseX, int mouseY) {
        if (this.layout == null) {
            return new GroupWindowHitTest.Result(GroupWindowHitTest.Target.NONE, -1);
        }
        GroupWindowHitTest.Bounds scrollbar = this.layout.maxScrollOffset() > 0
                ? new GroupWindowHitTest.Bounds(this.layout.x() + this.layout.width() - 7,
                        this.layout.y() + this.layout.headerHeight(), 8,
                        this.layout.height() - this.layout.headerHeight())
                : null;
        return GroupWindowHitTest.hitTest(this.layout, this.scrollOffset, mouseX, mouseY, scrollbar, this.controls);
    }

    public int scroll(double verticalAmount) {
        if (this.layout == null) {
            return this.scrollOffset;
        }
        int delta = verticalAmount < 0 ? ROW_HEIGHT : -ROW_HEIGHT;
        this.scrollOffset = this.layout.clampScrollOffset(this.scrollOffset + delta);
        return this.scrollOffset;
    }

    public double sliderRatioAt(int itemIndex, int mouseX) {
        if (itemIndex < 0 || itemIndex >= this.controls.size()) {
            return 0.0;
        }
        GroupWindowHitTest.Bounds slider = this.controls.get(itemIndex).slider();
        if (slider == null) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (mouseX - slider.x()) / (double) slider.width()));
    }

    public ResolvedShortcut shortcutAt(int itemIndex) {
        if (itemIndex < 0 || itemIndex >= this.rows.size()) {
            return null;
        }
        return this.rows.get(itemIndex).shortcut();
    }

    private List<RowModel> buildRows(ConfigGroup group, List<ConfigIndexEntry> index) {
        List<RowModel> result = new ArrayList<>();
        for (GroupItem item : group.items()) {
            ShortcutEntry shortcut = new ShortcutEntry(item.modId(), item.groupId(), item.configName(), "",
                    ShortcutControlType.SLIDER, 1.0, null, null);
            ResolvedShortcut resolved = ShortcutResolver.find(index, shortcut)
                    .map(entry -> new ResolvedShortcut(shortcut, entry)).orElse(null);
            boolean numeric = resolved != null && ShortcutControl.getControlType(resolved.configEntry().config())
                    != ShortcutControlType.TOGGLE;
            result.add(new RowModel(item, resolved, numeric && item.expanded() ? EXPANDED_HEIGHT : ROW_HEIGHT));
        }
        return List.copyOf(result);
    }

    private List<GroupWindowHitTest.ItemControls> buildControls() {
        if (this.layout == null) {
            return List.of();
        }
        List<GroupWindowHitTest.ItemControls> result = new ArrayList<>();
        for (int index = 0; index < this.rows.size(); index++) {
            GroupWindowLayout.Row row = this.layout.rows().get(index);
            RowModel model = this.rows.get(index);
            GroupWindowHitTest.Bounds expand = model.numeric()
                    ? new GroupWindowHitTest.Bounds(row.x() + row.width() - 22, row.y(), 18, ROW_HEIGHT)
                    : null;
            GroupWindowHitTest.Bounds slider = model.shortcut() != null && model.numeric() && model.item().expanded()
                    ? new GroupWindowHitTest.Bounds(row.x() + row.width() - SLIDER_WIDTH - 8,
                            row.y() + ROW_HEIGHT + 8, SLIDER_WIDTH, 8)
                    : null;
            result.add(new GroupWindowHitTest.ItemControls(index, expand, slider));
        }
        return List.copyOf(result);
    }

    private void drawRow(GuiContext context, RowModel row, int x, int y, int width, int mouseX, int mouseY, int accent) {
        boolean active = row.shortcut() != null;
        boolean hovered = active && GuiHitTest.isInside(mouseX, mouseY, x, y, width, row.height());
        int rowColor = hovered ? 0xFF34202A : 0xFF211820;
        RenderUtils.drawRect(context, x, y, width, row.height(), HoloPanelVisuals.withAlpha(rowColor, active ? 0xC8 : 0x68));
        RenderUtils.drawRect(context, x, y, 2, row.height(), active ? accent : 0x664A303A);
        String label = active ? row.shortcut().configEntry().displayName() : row.item().configName();
        context.drawString(this.font, fitText(label, width - 34), x + 6, y + 4, active ? TEXT : 0x887F6C75, false);
        if (!active) {
            context.drawString(this.font, StringUtils.translate("fast-masa-config.gui.floating.unavailable"), x + 6,
                    y + 15, 0x776F5B64, false);
            return;
        }
        if (!row.numeric()) {
            drawToggle(context, row.shortcut(), x + width - 36, y + 7, accent);
        } else if (row.item().expanded()) {
            double ratio = ShortcutControl.getSliderRatio(row.shortcut().configEntry().config());
            int sliderX = x + width - SLIDER_WIDTH - 8;
            RenderUtils.drawRect(context, sliderX, y + ROW_HEIGHT + 10, SLIDER_WIDTH, 3, TRACK);
            RenderUtils.drawRect(context, sliderX, y + ROW_HEIGHT + 10, (int) Math.round(SLIDER_WIDTH * ratio), 3, accent);
            context.drawString(this.font, ShortcutControl.getValueText(row.shortcut().configEntry().config()), x + 6,
                    y + ROW_HEIGHT + 7, MUTED, false);
            context.drawString(this.font, StringUtils.translate("fast-masa-config.gui.floating.collapse"), x + width - 17,
                    y + 6, MUTED, false);
        } else {
            context.drawString(this.font, StringUtils.translate("fast-masa-config.gui.floating.expand"), x + width - 17,
                    y + 6, MUTED, false);
        }
    }

    private void drawToggle(GuiContext context, ResolvedShortcut shortcut, int x, int y, int accent) {
        int color = ShortcutControl.getBooleanValue(shortcut.configEntry().config()) ? accent : 0xFF4A303A;
        RenderUtils.drawRect(context, x, y, 30, 12, color);
        RenderUtils.drawRect(context, x + (ShortcutControl.getBooleanValue(shortcut.configEntry().config()) ? 18 : 2), y + 2,
                8, 8, TEXT);
    }

    private String fitText(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end) + "...") > maxWidth) {
            end--;
        }
        return text.substring(0, Math.max(0, end)) + "...";
    }

    private record RowModel(GroupItem item, ResolvedShortcut shortcut, int height) {
        boolean numeric() {
            return this.shortcut != null && ShortcutControl.getControlType(this.shortcut.configEntry().config())
                    != ShortcutControlType.TOGGLE;
        }
    }
}
