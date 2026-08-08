/*
 * Visual behavior adapted from Meteor Client's WMeteorWindow.java,
 * WMeteorModule.java, and WMeteorSlider.java.
 * Source: https://github.com/MeteorDevelopment/meteor-client
 * License: GPL-3.0
 */
package fastui.yure.client.gui;

import fastui.yure.FastMasaConfig;
import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.client.shortcut.ResolvedShortcut;
import fastui.yure.client.shortcut.ShortcutControl;
import fastui.yure.client.shortcut.ShortcutResolver;
import fastui.yure.config.ConfigGroup;
import fastui.yure.config.ConfigGroupStore;
import fastui.yure.config.FastMasaConfigs;
import fastui.yure.config.GroupItem;
import fastui.yure.config.ShortcutControlType;
import fastui.yure.config.ShortcutEntry;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.Font;
import fi.dy.masa.malilib.config.IConfigBase;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** 分组浮动窗口。布局、绘制和命中测试共用同一份行模型。 */
public final class FloatingGroupPanel {
    private static final int TEXT = FastMasaMenuPalette.TEXT;
    private static final int MUTED = FastMasaMenuPalette.MUTED;
    private static final int BASE = FastMasaMenuPalette.WINDOW_BACKGROUND;
    private static final int TRACK = FastMasaMenuPalette.TRACK;
    /**
     * 普通行总高。改大可增加所有非展开项的上下留白；展开项的标题区也固定使用此值。
     * 建议保留至少默认字体高度 9px，再额外保留约 4~5px 上下留白。
     */
    private static final int ROW_HEIGHT = 15;
    /**
     * 数值项展开后的总高。它由标题区、2px 间隔、滑条/数值控件区和底部间隔共同组成。
     * 改大时，sliderBounds() 会自动让滑条在新增空间内垂直居中。
     */
    private static final int EXPANDED_HEIGHT = 43;
    /** 窗口左右内边距，同时决定标签和滑条左起点。改大可增加留白，但会缩短滑条。 */
    private static final int ROW_PADDING = 5;
    /** 所有悬浮窗口的最小内容宽度。改大将同时加宽收起和展开状态。 */
    private static final int MIN_CONTENT_WIDTH = 56;
    /**
     * 长标题/选项可撑开的内容宽度上限。超过此宽度的标签交给跑马灯，窗口不再继续加宽。
     * 要整体加宽悬浮菜单优先调这里；窗口最终还会额外加 GroupWindowLayout.WINDOW_PADDING * 2。
     */
    private static final int EXPANDED_MAX_CONTENT_WIDTH = 92;
    /** 滑条右端与数值文本之间的空隙。改小可给滑条让出宽度，最小建议保留 2px。 */
    private static final int SLIDER_VALUE_GAP = 2;
    /**
     * 数值文本预留宽度。改小可加长滑条；应至少容纳当前常用数值格式。
     * 值显示被 fitText() 截断时应增大此值，而不是继续压缩滑条。
     */
    private static final int RESET_WIDTH = 12;
    /**
     * 跑马灯速度，单位 px/s。当前 30 约为每 33ms 前进 1px；调小更慢，调大更快。
     * GuiContext 文本坐标是整数像素，因此实际位置变化频率约等于该数值，不能独立强制提高帧率。
     */
    private static final double MARQUEE_PIXELS_PER_SECOND = 30.0;

    private final Font font;
    private final String groupId;
    private GroupWindowLayout layout;
    private List<RowModel> rows = List.of();
    private List<GroupWindowHitTest.ItemControls> controls = List.of();
    private int scrollOffset;
    private int editingItemIndex = -1;
    private String editingValue = "";
    private boolean editingValueSelected;

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

    public void beginEditingValue(int itemIndex, String value) {
        this.editingItemIndex = itemIndex;
        this.editingValue = value;
        this.editingValueSelected = true;
    }

    public void updateEditingValue(String value) {
        this.editingValue = value;
        this.editingValueSelected = false;
    }

    public void clearEditingValue() {
        this.editingItemIndex = -1;
        this.editingValue = "";
        this.editingValueSelected = false;
    }

    public boolean isCollapseHit(int mouseX, int mouseY) {
        return this.layout != null && this.layout.headerHeight() >= 16 && this.layout.width() >= 20
                && GuiHitTest.isInside(mouseX, mouseY, this.layout.x() + this.layout.width() - 20,
                this.layout.y(), 20, this.layout.headerHeight());
    }

    public void toggleCollapsed() {
        ConfigGroupStore.get(this.groupId).ifPresent(group -> ConfigGroupStore.setWindowState(group.id(),
                !group.collapsed(), group.x(), group.y()));
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
        // 先建行模型、测内容宽度、再算窗口坐标，最后才生成命中矩形。
        // 这个顺序不能颠倒：命中区必须使用与渲染相同的最终 row 坐标。
        // 收起只隐藏内容，不改变参与测宽的行，保证窗口前后宽度一致。
        this.rows = buildRows(group, index);
        if (group.collapsed()) {
            this.controls = List.of();
            this.layout = GroupWindowLayout.calculate(screenWidth, screenHeight, group.x(), group.y(), true,
                    new int[0], measureContentWidth(group.name()));
        } else {
            int[] rowHeights = this.rows.stream().mapToInt(RowModel::height).toArray();
            this.layout = GroupWindowLayout.calculate(screenWidth, screenHeight, group.x(), group.y(), false,
                    rowHeights, measureContentWidth(group.name()));
            this.controls = buildControls();
        }
        this.scrollOffset = this.layout.clampScrollOffset(this.scrollOffset);

        if (this.layout.width() <= 0 || this.layout.height() <= 0) {
            return;
        }

        int accent = FastMasaMenuPalette.ACCENT;
        int backgroundAlpha = FastMasaConfigs.Generic.FLOATING_BACKGROUND_ALPHA.getIntegerValue();
        RenderUtils.drawRect(context, this.layout.x(), this.layout.y() + this.layout.headerHeight(), this.layout.width(),
                Math.max(0, this.layout.height() - this.layout.headerHeight()),
                HoloPanelVisuals.withAlpha(BASE, backgroundAlpha));
        RenderUtils.drawRect(context, this.layout.x(), this.layout.y(), this.layout.width(), this.layout.headerHeight(),
                HoloPanelVisuals.withAlpha(accent, backgroundAlpha));
        int headerControlsWidth = this.font.width("+") + ROW_PADDING * 2;
        boolean hasHeaderTextSpace = this.layout.headerHeight() >= 16;
        String title = fitText(group.name(), this.layout.width() - headerControlsWidth - ROW_PADDING * 2);
        if (hasHeaderTextSpace && this.layout.width() > headerControlsWidth + ROW_PADDING * 2) {
            context.drawString(this.font, title,
                    this.layout.x() + ROW_PADDING,
                    FloatingTextLayout.centeredTextY(this.layout.y(), this.layout.headerHeight(), this.font.lineHeight),
                    TEXT, false);
        }
        if (hasHeaderTextSpace && this.layout.width() >= 20) {
            String control = group.collapsed() ? "+" : "-";
            int controlX = this.layout.x() + this.layout.width() - headerControlsWidth + ROW_PADDING;
            context.drawString(this.font, control, FloatingTextLayout.centeredTextX(controlX,
                    headerControlsWidth - ROW_PADDING * 2, this.font.width(control)),
                    FloatingTextLayout.centeredTextY(this.layout.y(), this.layout.headerHeight(), this.font.lineHeight),
                    TEXT, false);
        }

        if (group.collapsed()) {
            return;
        }

        // 滚动只改变绘制位置；layout.rows() 始终保存未滚动坐标，供 hitTest 复用。
        for (int indexInRows = 0; indexInRows < this.rows.size(); indexInRows++) {
            GroupWindowLayout.Row row = this.layout.rows().get(indexInRows);
            if (!this.layout.isRowFullyVisible(row, this.scrollOffset)) {
                continue;
            }
            int y = row.y() - this.scrollOffset;
            drawRow(context, this.rows.get(indexInRows), indexInRows, row.x(), y, row.width(), mouseX, mouseY, accent);
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
        ResolvedShortcut shortcut = this.rows.get(itemIndex).shortcut();
        return isAvailableRow(shortcut) ? shortcut : null;
    }

    public boolean isSystemConfigRow(int itemIndex) {
        return isSystemConfigRow(this.groupId, itemIndex);
    }

    public int groupItemIndexAt(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= this.rows.size()) {
            return -1;
        }
        return groupItemIndexForRow(this.groupId, rowIndex);
    }

    static boolean hasSystemConfigEntry(String groupId) {
        return "default".equals(groupId);
    }

    static int systemRowCount(String groupId) {
        return hasSystemConfigEntry(groupId) ? 3 : 0;
    }

    static boolean isSystemConfigRow(String groupId, int rowIndex) {
        return hasSystemConfigEntry(groupId) && rowIndex == 0;
    }

    static int groupItemIndexForRow(String groupId, int rowIndex) {
        int itemIndex = rowIndex - systemRowCount(groupId);
        return itemIndex < 0 ? -1 : itemIndex;
    }

    static boolean isAvailableRow(ResolvedShortcut shortcut) {
        return shortcut != null;
    }

    static boolean isInteractiveRow(GroupItem item, ResolvedShortcut shortcut) {
        return item != null && shortcut != null;
    }

    /**
     * 将持久化的 GroupItem 解析为当前帧可绘制的行。
     * 系统行的 item 允许为 null，失效的外部配置则 shortcut 为 null；后续代码必须区分这两种情况。
     */
    private List<RowModel> buildRows(ConfigGroup group, List<ConfigIndexEntry> index) {
        List<RowModel> result = new ArrayList<>();
        if (hasSystemConfigEntry(group.id())) {
            result.add(new RowModel(null, null, true, ROW_HEIGHT));
            result.add(systemBooleanRow(FastMasaConfigs.Generic.RELEASE_TO_CLOSE));
            result.add(systemBooleanRow(FastMasaConfigs.Generic.CLOSE_ON_INVENTORY_KEY));
        }
        for (GroupItem item : group.items()) {
            ShortcutEntry shortcut = new ShortcutEntry(item.modId(), item.groupId(), item.configName(), "",
                    ShortcutControlType.SLIDER, 1.0, null, null);
            ResolvedShortcut resolved = ShortcutResolver.find(index, shortcut)
                    .map(entry -> new ResolvedShortcut(shortcut, entry)).orElse(null);
            boolean numeric = resolved != null && ShortcutControl.isNumeric(resolved.configEntry().config());
            result.add(new RowModel(item, resolved, false, numeric && item.expanded() ? EXPANDED_HEIGHT : ROW_HEIGHT));
        }
        return List.copyOf(result);
    }

    private RowModel systemBooleanRow(IConfigBase config) {
        ConfigIndexEntry entry = new ConfigIndexEntry(FastMasaConfig.MOD_ID, "Fast Masa Config",
                "generic", "Fast Masa Config", config.getName(), config.getConfigGuiDisplayName(), config);
        ShortcutEntry shortcut = new ShortcutEntry(FastMasaConfig.MOD_ID, "generic", config.getName(), "",
                ShortcutControlType.TOGGLE, 1.0, null, null);
        return new RowModel(null, new ResolvedShortcut(shortcut, entry), true, ROW_HEIGHT);
    }

    /**
     * 为每行生成交互矩形。绘制使用 expandBounds/sliderBounds，同一组函数也在这里使用，
     * 因此调整控件位置时只改 bounds 函数，不要在 QuickConfigScreen 里写第二套坐标。
     */
    private List<GroupWindowHitTest.ItemControls> buildControls() {
        if (this.layout == null) {
            return List.of();
        }
        List<GroupWindowHitTest.ItemControls> result = new ArrayList<>();
        for (int index = 0; index < this.rows.size(); index++) {
            GroupWindowLayout.Row row = this.layout.rows().get(index);
            RowModel model = this.rows.get(index);
            GroupWindowHitTest.Bounds expand = model.numeric()
                    ? expandBounds(row)
                    : null;
            GroupWindowHitTest.Bounds slider = model.shortcut() != null && model.numeric() && model.item().expanded()
                    ? sliderBounds(row)
                    : null;
            GroupWindowHitTest.Bounds value = slider == null ? null : valueBounds(row);
            GroupWindowHitTest.Bounds reset = slider == null ? null : resetBounds(row);
            result.add(new GroupWindowHitTest.ItemControls(index, expand, slider, value, reset));
        }
        return List.copyOf(result);
    }

    private void drawRow(GuiContext context, RowModel row, int rowIndex, int x, int y, int width, int mouseX,
            int mouseY, int accent) {
        int backgroundAlpha = FastMasaConfigs.Generic.FLOATING_BACKGROUND_ALPHA.getIntegerValue();
        if (row.shortcut() == null) {
            if (!row.systemConfig()) {
                drawUnavailableRow(context, row, rowIndex, x, y, width);
                return;
            }
            RenderUtils.drawRect(context, x, y, width, row.height(),
                    HoloPanelVisuals.withAlpha(FastMasaMenuPalette.SYSTEM_ROW, backgroundAlpha));
            drawMarqueeLabel(context, StringUtils.translate("fast-masa-config.gui.system.open_full_config"), x, y,
                    width, ROW_HEIGHT, width - 12, 0, TEXT);
            RenderUtils.drawRect(context, x, y, 2, ROW_HEIGHT, accent);
            return;
        }
        boolean hovered = GuiHitTest.isInside(mouseX, mouseY, x, y, width, row.height());
        boolean toggle = ShortcutControl.getControlType(row.shortcut().configEntry().config()) == ShortcutControlType.TOGGLE;
        boolean enabled = toggle && ShortcutControl.getBooleanValue(row.shortcut().configEntry().config());
        int rowColor = enabled ? FastMasaMenuPalette.MODULE_BACKGROUND
                : (hovered ? FastMasaMenuPalette.ROW_HOVER : FastMasaMenuPalette.ROW);
        // 非激活行也保留极轻的底色，避免文字与窗口背景融为一体；激活行再使用更强的模块色。
        RenderUtils.drawRect(context, x, y, width, row.height(),
                HoloPanelVisuals.withAlpha(rowColor, backgroundAlpha));
        String label = row.shortcut().configEntry().displayName();
        drawMarqueeLabel(context, label, x, y, width, ROW_HEIGHT, row.numeric() ? width - 28 : width - 8,
                rowIndex, TEXT);
        if (enabled) {
            RenderUtils.drawRect(context, x, y, 2, row.height(), accent);
        }
        if (!row.numeric()) {
            return;
        } else if (row.item().expanded()) {
            drawExpandedBorder(context, x, y, width, row.height());
            double ratio = ShortcutControl.getSliderRatio(row.shortcut());
            GroupWindowHitTest.Bounds slider = sliderBounds(new GroupWindowLayout.Row(0, x, y, width, row.height()));
            int fillWidth = (int) Math.round(slider.width() * ratio);
            int trackY = slider.y() + Math.max(0, (slider.height() - 3) / 2);
            RenderUtils.drawRect(context, slider.x(), trackY, slider.width(), 3, TRACK);
            RenderUtils.drawRect(context, slider.x(), trackY, fillWidth, 3, FastMasaMenuPalette.SLIDER_LEFT);
            int handleY = slider.y() + Math.max(0, (slider.height() - 4) / 2);
            drawSliderHandle(context, slider.x() + fillWidth - 2, handleY, TEXT);
            GroupWindowLayout.Row expandedRow = new GroupWindowLayout.Row(0, x, y, width, row.height());
            GroupWindowHitTest.Bounds value = valueBounds(expandedRow);
            boolean editing = rowIndex == this.editingItemIndex;
            String rawValue = editing ? this.editingValue : ShortcutControl.getValueText(row.shortcut().configEntry().config());
            boolean showCursor = editing && (System.currentTimeMillis() / 500L) % 2L == 0L;
            String valueText = fitText(rawValue + (showCursor ? "|" : ""), value.width() - 4);
            RenderUtils.drawRect(context, value.x(), value.y(), value.width(), value.height(),
                    editing ? FastMasaMenuPalette.ROW_HOVER : FastMasaMenuPalette.TRACK);
            if (editing && this.editingValueSelected) {
                RenderUtils.drawRect(context, value.x() + 2, value.y() + 2,
                        Math.max(0, Math.min(value.width() - 4, this.font.width(rawValue))),
                        Math.max(0, value.height() - 4), FastMasaMenuPalette.ACCENT);
            }
            RenderUtils.drawRect(context, value.x(), value.y(), value.width(), 1,
                    editing ? accent : FastMasaMenuPalette.NEUTRAL);
            RenderUtils.drawRect(context, value.x(), value.y() + Math.max(0, value.height() - 1), value.width(), 1,
                    editing ? accent : FastMasaMenuPalette.NEUTRAL);
            RenderUtils.drawRect(context, value.x(), value.y(), 1, value.height(), editing ? accent : FastMasaMenuPalette.NEUTRAL);
            RenderUtils.drawRect(context, value.x() + Math.max(0, value.width() - 1), value.y(), 1, value.height(),
                    editing ? accent : FastMasaMenuPalette.NEUTRAL);
            context.drawString(this.font, valueText,
                    value.x() + 2,
                    FloatingTextLayout.centeredTextY(value.y(), value.height(), this.font.lineHeight),
                    editing ? TEXT : MUTED, false);
            GroupWindowHitTest.Bounds reset = resetBounds(expandedRow);
            RenderUtils.drawRect(context, reset.x(), reset.y(), reset.width(), reset.height(), FastMasaMenuPalette.TRACK);
            RenderUtils.drawRect(context, reset.x(), reset.y(), reset.width(), 1, FastMasaMenuPalette.NEUTRAL);
            RenderUtils.drawRect(context, reset.x(), reset.y() + Math.max(0, reset.height() - 1), reset.width(), 1,
                    FastMasaMenuPalette.NEUTRAL);
            RenderUtils.drawRect(context, reset.x(), reset.y(), 1, reset.height(), FastMasaMenuPalette.NEUTRAL);
            RenderUtils.drawRect(context, reset.x() + Math.max(0, reset.width() - 1), reset.y(), 1, reset.height(),
                    FastMasaMenuPalette.NEUTRAL);
            String resetLabel = "R";
            context.drawString(this.font, resetLabel,
                    FloatingTextLayout.centeredTextX(reset.x(), reset.width(), this.font.width(resetLabel)),
                    FloatingTextLayout.centeredTextY(reset.y(), reset.height(), this.font.lineHeight), MUTED, false);
            drawExpandButton(context, expandBounds(new GroupWindowLayout.Row(0, x, y, width, row.height())),
                    true, accent);
        } else {
            drawExpandButton(context, expandBounds(new GroupWindowLayout.Row(0, x, y, width, row.height())),
                    false, accent);
        }
    }

    private void drawUnavailableRow(GuiContext context, RowModel row, int rowIndex, int x, int y, int width) {
        int backgroundAlpha = FastMasaConfigs.Generic.FLOATING_BACKGROUND_ALPHA.getIntegerValue();
        RenderUtils.drawRect(context, x, y, width, row.height(),
                HoloPanelVisuals.withAlpha(FastMasaMenuPalette.ROW, backgroundAlpha));
        drawMarqueeLabel(context, row.item().configName(), x, y, width, ROW_HEIGHT, width - 8, rowIndex,
                FastMasaMenuPalette.UNAVAILABLE_TEXT);
    }

    private String fitText(String text, int maxWidth) {
        return fitText(text, maxWidth, this.font::width);
    }

    static String fitText(String text, int maxWidth, ToIntFunction<String> width) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        if (width.applyAsInt(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        if (width.applyAsInt(ellipsis) > maxWidth) {
            return fitPrefix(text, maxWidth, width);
        }
        int end = text.length();
        while (end > 0 && width.applyAsInt(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    private static String fitPrefix(String text, int maxWidth, ToIntFunction<String> width) {
        int end = text.length();
        while (end > 0 && width.applyAsInt(text.substring(0, end)) > maxWidth) {
            end--;
        }
        return text.substring(0, end);
    }

    /**
     * 数值行右侧 +/- 的绘制和点击区域。16px 是按钮目标宽高，右侧 2px、上下 1px 是窗口留白。
     * 改这里会同时影响 drawExpandButton() 和鼠标命中，不需要到 QuickConfigScreen 再改一份坐标。
     */
    static GroupWindowHitTest.Bounds expandBounds(GroupWindowLayout.Row row) {
        int width = Math.min(16, Math.max(0, row.width() - 4));
        return new GroupWindowHitTest.Bounds(row.x() + Math.max(0, row.width() - width - 2), row.y() + 1, width,
                Math.min(16, Math.max(0, row.height() - 2)));
    }

    /** The slider occupies its own line beneath the title. */
    static GroupWindowHitTest.Bounds sliderBounds(GroupWindowLayout.Row row) {
        int detailTop = row.y() + ROW_HEIGHT + 2;
        int x = row.x() + ROW_PADDING;
        int end = row.x() + row.width() - ROW_PADDING;
        return new GroupWindowHitTest.Bounds(x, detailTop, Math.max(0, end - x), 8);
    }

    /** The editable value field occupies the third line of an expanded numeric row. */
    static GroupWindowHitTest.Bounds valueBounds(GroupWindowLayout.Row row) {
        int x = row.x() + ROW_PADDING;
        int end = row.x() + row.width() - ROW_PADDING;
        return new GroupWindowHitTest.Bounds(x, row.y() + ROW_HEIGHT + 13,
                Math.max(0, end - x - SLIDER_VALUE_GAP - RESET_WIDTH), 10);
    }

    static GroupWindowHitTest.Bounds resetBounds(GroupWindowLayout.Row row) {
        GroupWindowHitTest.Bounds value = valueBounds(row);
        return new GroupWindowHitTest.Bounds(value.x() + value.width() + SLIDER_VALUE_GAP, value.y(), RESET_WIDTH,
                value.height());
    }

    private void drawExpandButton(GuiContext context, GroupWindowHitTest.Bounds bounds, boolean expanded, int accent) {
        String control = expanded ? "-" : "+";
        context.drawString(this.font, control,
                FloatingTextLayout.centeredTextX(bounds.x(), bounds.width(), this.font.width(control)),
                // +/- 的字形重心比普通文字高，centeredSymbolY() 已额外向下补 1px。
                FloatingTextLayout.centeredSymbolY(bounds.y(), bounds.height(), this.font.lineHeight),
                expanded ? accent : MUTED,
                false);
    }

    /**
     * 展开区边框。起点的 2px 和总高度公式须与 sliderBounds() 保持一致，否则滑条会跑出边框。
     * 若只想加大边框内部高度，优先调 EXPANDED_HEIGHT。
     */
    private void drawExpandedBorder(GuiContext context, int x, int y, int width, int height) {
        int top = y + ROW_HEIGHT + 2;
        int borderHeight = Math.max(0, height - ROW_HEIGHT - 4);
        if (borderHeight <= 1 || width <= 1) {
            return;
        }
        RenderUtils.drawRect(context, x, top, width, 1, FastMasaMenuPalette.NEUTRAL);
        RenderUtils.drawRect(context, x, top + borderHeight - 1, width, 1, FastMasaMenuPalette.NEUTRAL);
        RenderUtils.drawRect(context, x, top, 1, borderHeight, FastMasaMenuPalette.NEUTRAL);
        RenderUtils.drawRect(context, x + width - 1, top, 1, borderHeight, FastMasaMenuPalette.NEUTRAL);
    }


    /** 4x4px 菱形滑块把手。改尺寸时也要同步调整 drawRow() 中的 handleY 和横向 -2px 偏移。 */
    private void drawSliderHandle(GuiContext context, int x, int y, int color) {
        RenderUtils.drawRect(context, x + 1, y, 2, 1, color);
        RenderUtils.drawRect(context, x, y + 1, 4, 2, color);
        RenderUtils.drawRect(context, x + 1, y + 3, 2, 1, color);
    }

    /**
     * 这是悬浮窗口宽度的唯一测量入口。改最大宽度、标题栏按钮或行右侧控件时，
     * 必须同步调整这里的预留宽度，否则文本会在渲染时被意外截断。
     */
    private int measureContentWidth(String groupName) {
        int collapseControlWidth = this.font.width("+");
        int width = this.font.width(groupName) + collapseControlWidth + ROW_PADDING * 2 - 10;
        for (RowModel row : this.rows) {
            String label = measureRowLabel(row);
            // 数值项额外预留 56px 给 +/-、数值和间距；布尔项只预留右侧呼吸空间 12px。
            int rowWidth = this.font.width(label) + ROW_PADDING * 2 + (row.numeric() ? 72 : 12);
            // 长选项不撑大窗口，交给 drawMarqueeLabel 在固定文本区内滚动；标题则始终优先完整显示。
            width = Math.max(width, Math.min(EXPANDED_MAX_CONTENT_WIDTH, rowWidth));
        }
        return Math.max(MIN_CONTENT_WIDTH, width);
    }

    /**
     * 按像素偏移绘制完整文本，并用 GuiContext scissor 裁掉标签区域外的内容。
     * 这条路径不调用 fitText，因此长名称不会出现省略号。
     */
    private void drawMarqueeLabel(GuiContext context, String text, int rowX, int rowY, int rowWidth,
            int labelHeight, int maxWidth, int rowIndex, int textColor) {
        int labelX = rowX + ROW_PADDING;
        int labelWidth = Math.max(0, Math.min(maxWidth, rowWidth - ROW_PADDING));
        int baseline = FloatingTextLayout.centeredTextY(rowY, labelHeight, this.font.lineHeight);
        if (text == null || labelWidth <= 0) {
            return;
        }

        int textWidth = this.font.width(text);
        String renderedText = text;
        int drawX = labelX;
        if (textWidth > labelWidth) {
            // 三个空格决定两轮文本之间的停顿距离；调大可增加分隔空白。
            String gap = "   ";
            int cycleWidth = textWidth + this.font.width(gap);
            double elapsedSeconds = System.nanoTime() / 1_000_000_000.0;
            // 17px 只是让每行错开起始相位；调为 0 可让所有跑马灯完全同步。
            double offset = (elapsedSeconds * MARQUEE_PIXELS_PER_SECOND + rowIndex * 17.0) % cycleWidth;
            renderedText = text + gap + text;
            int pixelOffset = (int) Math.floor(offset);
            drawX -= pixelOffset;
        }

        context.pushScissor(new ScreenRectangle(labelX, rowY, labelWidth, labelHeight));
        try {
            context.drawString(this.font, renderedText, drawX, baseline, textColor, false);
        } finally {
            context.popScissor();
        }
    }

    private String measureRowLabel(RowModel row) {
        if (row.systemConfig()) {
            return row.shortcut() == null
                    ? StringUtils.translate("fast-masa-config.gui.system.open_full_config")
                    : row.shortcut().configEntry().displayName();
        }
        if (row.shortcut() != null) {
            return row.shortcut().configEntry().displayName();
        }
        return row.item().configName();
    }

    private record RowModel(GroupItem item, ResolvedShortcut shortcut, boolean systemConfig, int height) {
        boolean numeric() {
            return this.shortcut != null && ShortcutControl.isNumeric(this.shortcut.configEntry().config());
        }
    }
}
