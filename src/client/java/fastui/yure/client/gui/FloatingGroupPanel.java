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

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** 分组浮动窗口。布局、绘制和命中测试共用同一份行模型。 */
public final class FloatingGroupPanel {
    private static final int TEXT = FastMasaMenuPalette.TEXT;
    private static final int MUTED = FastMasaMenuPalette.MUTED;
    private static final int BASE = FastMasaMenuPalette.SURFACE;
    private static final int TRACK = FastMasaMenuPalette.TRACK;
    private static final int ROW_HEIGHT = 25;
    private static final int EXPANDED_HEIGHT = 42;
    private static final int SLIDER_WIDTH = 88;

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
        return this.layout != null && this.layout.headerHeight() >= 18 && this.layout.width() >= 20 && GuiHitTest.isInside(mouseX, mouseY,
                this.layout.x() + this.layout.width() - 20, this.layout.y(), 20, this.layout.headerHeight());
    }

    public boolean isHideHit(int mouseX, int mouseY) {
        return this.layout != null && this.layout.headerHeight() >= 18 && this.layout.width() >= 40 && GuiHitTest.isInside(mouseX, mouseY,
                this.layout.x() + this.layout.width() - 40, this.layout.y(), 20, this.layout.headerHeight());
    }

    public boolean isFullConfigHit(int mouseX, int mouseY) {
        return this.layout != null && this.layout.headerHeight() >= 18 && hasSystemConfigEntry(this.groupId) && this.layout.width() >= 60
                && GuiHitTest.isInside(mouseX, mouseY,
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

        if (this.layout.width() <= 0 || this.layout.height() <= 0) {
            return;
        }

        int accent = FastMasaMenuPalette.ACCENT;
        RenderUtils.drawRect(context, this.layout.x(), this.layout.y(), this.layout.width(), this.layout.height(),
                HoloPanelVisuals.withAlpha(BASE, 0xE8));
        RenderUtils.drawRect(context, this.layout.x(), this.layout.y(), this.layout.width(), this.layout.headerHeight(),
                HoloPanelVisuals.withAlpha(accent, 0xD8));
        int headerControlsWidth = hasSystemConfigEntry(this.groupId) ? 60 : 40;
        boolean hasHeaderTextSpace = this.layout.headerHeight() >= 18;
        if (hasHeaderTextSpace && this.layout.width() > headerControlsWidth + 18) {
            context.drawString(this.font, fitText(group.name(), this.layout.width() - headerControlsWidth - 8),
                    this.layout.x() + 7, this.layout.y() + 6, TEXT, false);
        }
        if (hasHeaderTextSpace && hasSystemConfigEntry(this.groupId) && this.layout.width() >= 60) {
            context.drawString(this.font, "*", this.layout.x() + this.layout.width() - 54, this.layout.y() + 6, TEXT, false);
        }
        if (hasHeaderTextSpace && this.layout.width() >= 40) {
            context.drawString(this.font, StringUtils.translate("fast-masa-config.gui.floating.hide"),
                    this.layout.x() + this.layout.width() - 34, this.layout.y() + 5, TEXT, false);
        }
        if (hasHeaderTextSpace && this.layout.width() >= 20) {
            context.drawString(this.font, StringUtils.translate(group.collapsed() ? "fast-masa-config.gui.floating.expand"
                    : "fast-masa-config.gui.floating.collapse"), this.layout.x() + this.layout.width() - 14,
                    this.layout.y() + 5, TEXT, false);
        }

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
            boolean numeric = resolved != null && ShortcutControl.getControlType(resolved.configEntry().config())
                    != ShortcutControlType.TOGGLE;
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
            result.add(new GroupWindowHitTest.ItemControls(index, expand, slider));
        }
        return List.copyOf(result);
    }

    private void drawRow(GuiContext context, RowModel row, int x, int y, int width, int mouseX, int mouseY, int accent) {
        if (row.shortcut() == null) {
            if (!row.systemConfig()) {
                drawUnavailableRow(context, row, x, y, width);
                return;
            }
            RenderUtils.drawRect(context, x, y, width, ROW_HEIGHT, HoloPanelVisuals.withAlpha(FastMasaMenuPalette.SYSTEM_ROW, 0xD8));
            RenderUtils.drawRect(context, x, y, 2, ROW_HEIGHT, accent);
            context.drawString(this.font, fitText(StringUtils.translate("fast-masa-config.gui.system.open_full_config"), width - 12),
                    x + 6, y + 8, TEXT, false);
            return;
        }
        boolean hovered = GuiHitTest.isInside(mouseX, mouseY, x, y, width, row.height());
        boolean toggle = ShortcutControl.getControlType(row.shortcut().configEntry().config()) == ShortcutControlType.TOGGLE;
        boolean enabled = toggle && ShortcutControl.getBooleanValue(row.shortcut().configEntry().config());
        int rowColor = enabled ? accent : (hovered ? FastMasaMenuPalette.ROW_HOVER : FastMasaMenuPalette.ROW);
        RenderUtils.drawRect(context, x, y, width, row.height(), HoloPanelVisuals.withAlpha(rowColor, enabled ? 0xD8 : 0xC8));
        RenderUtils.drawRect(context, x, y, 2, row.height(), enabled ? accent
                : FastMasaMenuPalette.NEUTRAL);
        String label = row.shortcut().configEntry().displayName();
        context.drawString(this.font, fitText(label, width - 34), x + 6, y + 4, TEXT, false);
        if (!row.numeric()) {
            return;
        } else if (row.item().expanded()) {
            double ratio = ShortcutControl.getSliderRatio(row.shortcut().configEntry().config());
            GroupWindowHitTest.Bounds slider = sliderBounds(new GroupWindowLayout.Row(0, x, y, width, row.height()));
            int fillWidth = (int) Math.round(slider.width() * ratio);
            RenderUtils.drawRect(context, slider.x(), slider.y() + 3, slider.width(), 3, TRACK);
            RenderUtils.drawRect(context, slider.x(), slider.y() + 3, fillWidth, 3, accent);
            RenderUtils.drawRect(context, slider.x() + Math.max(0, fillWidth - 2), slider.y(), 4, slider.height(), TEXT);
            GroupWindowHitTest.Bounds value = valueBounds(new GroupWindowLayout.Row(0, x, y, width, row.height()));
            context.drawString(this.font, fitText(ShortcutControl.getValueText(row.shortcut().configEntry().config()),
                    value.width()), value.x(), value.y() + 1, MUTED, false);
            drawExpandButton(context, expandBounds(new GroupWindowLayout.Row(0, x, y, width, row.height())), true, accent);
        } else {
            drawExpandButton(context, expandBounds(new GroupWindowLayout.Row(0, x, y, width, row.height())), false, accent);
        }
    }

    private void drawUnavailableRow(GuiContext context, RowModel row, int x, int y, int width) {
        RenderUtils.drawRect(context, x, y, width, row.height(), HoloPanelVisuals.withAlpha(FastMasaMenuPalette.ROW, 0xC8));
        RenderUtils.drawRect(context, x, y, 2, row.height(), FastMasaMenuPalette.UNAVAILABLE_STRIP);
        context.drawString(this.font, fitText(row.item().configName(), width - 34), x + 6, y + 4,
                FastMasaMenuPalette.UNAVAILABLE_TEXT, false);
        context.drawString(this.font, StringUtils.translate("fast-masa-config.gui.floating.unavailable"), x + 6,
                y + 15, FastMasaMenuPalette.UNAVAILABLE_MUTED, false);
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

    static GroupWindowHitTest.Bounds expandBounds(GroupWindowLayout.Row row) {
        int width = Math.min(16, Math.max(0, row.width() - 8));
        return new GroupWindowHitTest.Bounds(row.x() + Math.max(0, row.width() - 22), row.y() + 2, width,
                Math.min(16, Math.max(0, row.height() - 4)));
    }

    static GroupWindowHitTest.Bounds sliderBounds(GroupWindowLayout.Row row) {
        int width = Math.min(SLIDER_WIDTH, Math.max(0, row.width() - 64));
        int x = row.x() + Math.max(0, row.width() - SLIDER_WIDTH - 64);
        return new GroupWindowHitTest.Bounds(x, row.y() + ROW_HEIGHT + 8, width,
                Math.min(10, Math.max(0, row.height() - ROW_HEIGHT - 8)));
    }

    static GroupWindowHitTest.Bounds valueBounds(GroupWindowLayout.Row row) {
        GroupWindowHitTest.Bounds slider = sliderBounds(row);
        int x = slider.x() + slider.width() + 6;
        int width = Math.max(0, expandBounds(row).x() - x - 4);
        return new GroupWindowHitTest.Bounds(x, slider.y(), width, slider.height());
    }

    private void drawExpandButton(GuiContext context, GroupWindowHitTest.Bounds bounds, boolean expanded, int accent) {
        RenderUtils.drawRect(context, bounds.x(), bounds.y(), bounds.width(), bounds.height(), expanded ? accent : TRACK);
        context.drawString(this.font, expanded ? "v" : ">", bounds.x() + 5, bounds.y() + 4, TEXT, false);
    }

    private record RowModel(GroupItem item, ResolvedShortcut shortcut, boolean systemConfig, int height) {
        boolean numeric() {
            return this.shortcut != null && ShortcutControl.getControlType(this.shortcut.configEntry().config())
                    != ShortcutControlType.TOGGLE;
        }
    }
}
