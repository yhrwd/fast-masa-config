package fastui.yure.client.gui;

import fastui.yure.client.shortcut.ShortcutControl;
import fastui.yure.config.FastMasaConfigs;
import fastui.yure.config.ShortcutControlType;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按住热键时显示的快捷配置面板。
 * 负责绘制半透明像素风面板、计算鼠标命中、处理列表滚动和滑条比例换算。
 */
public final class QuickConfigPanel {
    private static final long OPEN_ANIMATION_MS = 120L;
    private static final int BASE = 0xFF1A1A1D;
    private static final int ACCENT = 0xFFE6397C;
    private static final int TEXT = 0xFFFFEAF2;
    private static final int MUTED = 0xFFCFA4B7;
    private static final int TRACK = 0xFF5B3A48;
    private static final int TOGGLE_WIDTH = 30;
    private static final int TOGGLE_HEIGHT = 12;
    private static final int SLIDER_WIDTH = 48;
    private static final int MODE_TAB_WIDTH = 54;
    private final TextRenderer textRenderer;
    private final Map<String, Double> toggleAnimation = new HashMap<>();
    private int x;
    private int y;
    private int width;
    private int height;
    private int cellWidth;
    private int settingsButtonX;
    private int settingsButtonY;
    private int shortcutsTabX;
    private int enabledTabX;
    private int tabY;
    private int visibleRows = 1;
    private int columns = 1;
    private int maxScrollOffset;
    private int scrollOffset;
    private double settingsHoverProgress;
    private long openedAtMillis = -1L;

    /**
     * 保存 Minecraft 的文字渲染器，后续所有文本宽度计算和绘制都用同一个实例。
     */
    public QuickConfigPanel(MinecraftClient client) {
        this.textRenderer = client.textRenderer;
    }

    /**
     * 绘制快捷面板完整内容。
     * 每帧都会重新根据视窗、缩放和快捷项数量计算布局，确保窗口大小或 GUI scale 改变后不会重叠或出界。
     */
    public void render(DrawContext context, int screenWidth, int screenHeight, int mouseX, int mouseY, List<QuickPanelItem> shortcuts, int scrollOffset, PanelMode mode) {
        if (this.openedAtMillis < 0L) {
            this.openedAtMillis = System.currentTimeMillis();
        }

        double scale = FastMasaConfigs.Generic.PANEL_SCALE.getDoubleValue();
        QuickPanelLayout layout = QuickPanelLayout.calculate(screenWidth, screenHeight, FastMasaConfigs.Generic.PANEL_WIDTH.getIntegerValue(), FastMasaConfigs.Generic.PANEL_MAX_HEIGHT.getIntegerValue(), scale, shortcuts.size());
        double open = HoloPanelVisuals.openProgress(System.currentTimeMillis() - this.openedAtMillis, OPEN_ANIMATION_MS);
        int rise = (int) Math.round((1.0 - open) * 12.0);
        this.x = layout.x();
        this.y = layout.y();
        this.width = layout.width();
        this.height = layout.height();
        this.columns = layout.columns();
        this.cellWidth = layout.cellWidth() - (layout.maxScrollOffset() > 0 ? 6 / this.columns : 0);
        this.visibleRows = layout.visibleRows();
        this.maxScrollOffset = layout.maxScrollOffset();
        this.scrollOffset = layout.clampScrollOffset(scrollOffset);
        this.y += rise;

        drawPanelShell(context, open);
        context.drawText(this.textRenderer, "FAST", this.x + 9, this.y + 7, TEXT, false);
        context.drawText(this.textRenderer, "UI", this.x + 35, this.y + 7, ACCENT, false);
        drawSettingsButton(context, mouseX, mouseY);
        drawModeTabs(context, mouseX, mouseY, mode);

        if (shortcuts.isEmpty()) {
            String emptyKey = mode == PanelMode.ENABLED_BOOLEANS ? "fast-masa-config.gui.quick.empty_enabled" : "fast-masa-config.gui.quick.empty";
            context.drawText(this.textRenderer, fitText(StringUtils.translate(emptyKey), this.width - 22), this.x + 10, this.y + 31, TEXT, false);
            return;
        }

        int end = Math.min(shortcuts.size(), this.scrollOffset + this.visibleRows * this.columns);

        for (int index = this.scrollOffset; index < end; index++) {
            drawShortcut(context, shortcuts.get(index), index - this.scrollOffset, mouseX, mouseY);
        }

        if (this.maxScrollOffset > 0) {
            drawScrollIndicator(context);
        }
    }

    /**
     * 根据鼠标坐标返回当前命中的快捷项索引。
     * 返回的是 shortcuts 列表中的真实索引，不是当前可见区域内的局部索引。
     */
    public int getShortcutIndexAt(int mouseX, int mouseY, int shortcutCount) {
        int end = Math.min(shortcutCount, this.scrollOffset + this.visibleRows * this.columns);

        for (int index = this.scrollOffset; index < end; index++) {
            int localIndex = index - this.scrollOffset;
            int cellX = getCellX(localIndex);
            int cellY = getCellY(localIndex);

            if (isInside(mouseX, mouseY, cellX, cellY, getCellWidth(), QuickPanelLayout.ROW_HEIGHT)) {
                return index;
            }
        }

        return -1;
    }

    /**
     * 根据鼠标滚轮方向计算新的滚动偏移。
     * verticalAmount 小于 0 表示向下滚，偏移增加；结果会被限制到当前最大滚动范围。
     */
    public int scroll(int scrollOffset, double verticalAmount) {
        int delta = verticalAmount < 0 ? 1 : -1;
        return Math.max(0, Math.min(this.maxScrollOffset, scrollOffset + delta));
    }

    /**
     * 将鼠标横坐标换算成滑条 0..1 比例。
     * index 是快捷项真实索引，方法会减去 scrollOffset 找到当前可见单元格位置。
     */
    public double getSliderRatioAt(int mouseX, int index) {
        int localIndex = index - this.scrollOffset;
        int cellX = getCellX(localIndex);
        int sliderX = getSliderX(cellX, getCellWidth());
        return Math.max(0.0, Math.min(1.0, (mouseX - sliderX) / (double) SLIDER_WIDTH));
    }

    /**
     * 判断鼠标是否悬停在右上角设置按钮上。
     * 按钮位置在 render() 中计算，因此调用前必须至少渲染过一帧。
     */
    public boolean isSettingsButtonHovered(int mouseX, int mouseY) {
        return isInside(mouseX, mouseY, this.settingsButtonX, this.settingsButtonY, 16, 16);
    }

    public PanelMode getModeAt(int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, this.shortcutsTabX, this.tabY, MODE_TAB_WIDTH, 14)) {
            return PanelMode.SHORTCUTS;
        }

        if (isInside(mouseX, mouseY, this.enabledTabX, this.tabY, MODE_TAB_WIDTH, 14)) {
            return PanelMode.ENABLED_BOOLEANS;
        }

        return null;
    }

    /**
     * 绘制面板外壳、半透明背景、标题分隔线和像素角标。
     * open 是打开动画进度，会同时影响透明度和外观层级。
     */
    private void drawPanelShell(DrawContext context, double open) {
        int alpha = getPanelAlpha(open);
        RenderUtils.drawRect(context, this.x, this.y, this.width, this.height, HoloPanelVisuals.withAlpha(BASE, alpha));
        RenderUtils.drawRect(context, this.x + 1, this.y + 1, this.width - 2, this.height - 2, HoloPanelVisuals.withAlpha(BASE, Math.max(0x24, alpha - 0x24)));
        RenderUtils.drawRect(context, this.x + 6, this.y + QuickPanelLayout.HEADER_HEIGHT - 3, this.width - 12, 2, ACCENT);
        drawCornerArrow(context, this.x + 2, this.y + 2, 1, 1, ACCENT);
        drawCornerArrow(context, this.x + this.width - 10, this.y + 2, -1, 1, ACCENT);
        drawCornerArrow(context, this.x + 2, this.y + this.height - 10, 1, -1, ACCENT);
        drawCornerArrow(context, this.x + this.width - 10, this.y + this.height - 10, -1, -1, ACCENT);
    }

    /**
     * 绘制朝向对应角落外侧的像素箭头。
     * horizontalDirection/verticalDirection 决定箭头分别朝左上、右上、左下、右下四个方向。
     */
    private void drawCornerArrow(DrawContext context, int x, int y, int horizontalDirection, int verticalDirection, int color) {
        QuickCornerArrow arrow = QuickCornerArrow.calculate(x, y, horizontalDirection, verticalDirection);

        RenderUtils.drawRect(context, arrow.tipX(), arrow.tipY(), 2, 2, color);
        RenderUtils.drawRect(context, arrow.midX(), arrow.tipY(), 2, 2, color);
        RenderUtils.drawRect(context, arrow.tipX(), arrow.midY(), 2, 2, color);
        RenderUtils.drawRect(context, arrow.baseX(), arrow.tipY(), 2, 2, color);
        RenderUtils.drawRect(context, arrow.tipX(), arrow.baseY(), 2, 2, color);
    }

    /**
     * 绘制设置按钮并更新 hover 动画进度。
     * 按钮命中框也在这里同步，供 QuickConfigScreen 点击时判断是否进入全屏 UI。
     */
    private void drawSettingsButton(DrawContext context, int mouseX, int mouseY) {
        this.settingsButtonX = this.x + this.width - 24;
        this.settingsButtonY = this.y + 5;
        boolean hovered = isSettingsButtonHovered(mouseX, mouseY);
        this.settingsHoverProgress = HoloPanelVisuals.approach(this.settingsHoverProgress, hovered ? 1.0 : 0.0, 0.22);
        int bgColor = HoloPanelVisuals.withAlpha(HoloPanelVisuals.mixRgb(BASE, ACCENT, this.settingsHoverProgress), hovered ? 0xE8 : 0xB8);
        RenderUtils.drawRect(context, this.settingsButtonX, this.settingsButtonY, 16, 16, bgColor);
        drawSettingsIcon(context, this.settingsButtonX + 3, this.settingsButtonY + 3, hovered ? TEXT : ACCENT);
    }

    /**
     * 用矩形绘制小齿轮，避免额外纹理资源在多版本加载路径上的差异。
     */
    private void drawSettingsIcon(DrawContext context, int x, int y, int color) {
        RenderUtils.drawRect(context, x + 4, y, 2, 10, color);
        RenderUtils.drawRect(context, x, y + 4, 10, 2, color);
        RenderUtils.drawRect(context, x + 2, y + 2, 6, 6, color);
        RenderUtils.drawRect(context, x + 4, y + 4, 2, 2, BASE);
        RenderUtils.drawRect(context, x + 1, y + 1, 2, 2, color);
        RenderUtils.drawRect(context, x + 7, y + 1, 2, 2, color);
        RenderUtils.drawRect(context, x + 1, y + 7, 2, 2, color);
        RenderUtils.drawRect(context, x + 7, y + 7, 2, 2, color);
    }

    private void drawModeTabs(DrawContext context, int mouseX, int mouseY, PanelMode mode) {
        this.tabY = this.y + 5;
        this.enabledTabX = this.settingsButtonX - MODE_TAB_WIDTH - 4;
        this.shortcutsTabX = this.enabledTabX - MODE_TAB_WIDTH - 2;
        drawModeTab(context, this.shortcutsTabX, this.tabY, MODE_TAB_WIDTH, StringUtils.translate("fast-masa-config.gui.quick.tab.shortcuts"), mode == PanelMode.SHORTCUTS, mouseX, mouseY);
        drawModeTab(context, this.enabledTabX, this.tabY, MODE_TAB_WIDTH, StringUtils.translate("fast-masa-config.gui.quick.tab.enabled"), mode == PanelMode.ENABLED_BOOLEANS, mouseX, mouseY);
    }

    private void drawModeTab(DrawContext context, int x, int y, int width, String label, boolean active, int mouseX, int mouseY) {
        boolean hovered = isInside(mouseX, mouseY, x, y, width, 14);
        int bg = active ? HoloPanelVisuals.withAlpha(ACCENT, 0xE8) : HoloPanelVisuals.withAlpha(BASE, hovered ? 0xE0 : 0x78);
        int fg = active ? TEXT : (hovered ? TEXT : MUTED);
        RenderUtils.drawRect(context, x, y, width, 14, bg);
        RenderUtils.drawRect(context, x, y, width, 1, active || hovered ? TEXT : 0x885A3040);
        RenderUtils.drawRect(context, x, y + 13, width, 1, active ? ACCENT : 0x66302028);
        String text = fitText(label, width - 6);
        context.drawText(this.textRenderer, text, x + (width - this.textRenderer.getWidth(text)) / 2, y + 3, fg, false);
    }

    /**
     * 绘制单个快捷项。
     * 根据配置类型自动选择 toggle 或 slider，并为右侧控件预留宽度，左侧文本超出时省略。
     */
    private void drawShortcut(DrawContext context, QuickPanelItem shortcut, int index, int mouseX, int mouseY) {
        int cellX = getCellX(index);
        int cellY = getCellY(index);
        int cellWidth = getCellWidth();
        boolean hovered = isInside(mouseX, mouseY, cellX, cellY, cellWidth, QuickPanelLayout.ROW_HEIGHT);
        int itemAlpha = getItemAlpha(hovered);
        RenderUtils.drawRect(context, cellX, cellY, cellWidth, QuickPanelLayout.ROW_HEIGHT, HoloPanelVisuals.withAlpha(hovered ? 0xFF34202A : 0xFF211820, itemAlpha));
        RenderUtils.drawRect(context, cellX, cellY, 2, QuickPanelLayout.ROW_HEIGHT, hovered ? ACCENT : 0x885A3040);

        String label = shortcut.shortcut().labelOverride().isBlank() ? shortcut.configEntry().displayName() : shortcut.shortcut().labelOverride();
        String meta = shortcut.configEntry().modName() + " / " + shortcut.configEntry().groupName();
        int rightReserved = ShortcutControl.getControlType(shortcut.configEntry().config()) == ShortcutControlType.TOGGLE ? 42 : 82;
        context.drawText(this.textRenderer, fitText(label, cellWidth - rightReserved - 12), cellX + 6, cellY + 4, hovered ? TEXT : MUTED, false);
        context.drawText(this.textRenderer, fitText(meta, cellWidth - rightReserved - 12), cellX + 6, cellY + 15, 0xAA8F6676, false);

        if (ShortcutControl.getControlType(shortcut.configEntry().config()) == ShortcutControlType.TOGGLE) {
            drawToggle(context, shortcut, cellX + cellWidth - TOGGLE_WIDTH - 6, cellY + 7, hovered);
            return;
        }

        int sliderX = getSliderX(cellX, cellWidth);
        int controlCenterY = cellY + QuickPanelLayout.ROW_HEIGHT / 2 + 3;
        int sliderY = controlCenterY - 1;
        int valueY = controlCenterY - this.textRenderer.fontHeight / 2;
        double ratio = ShortcutControl.getSliderRatio(shortcut.configEntry().config());
        RenderUtils.drawRect(context, sliderX, sliderY, SLIDER_WIDTH, 3, TRACK);
        RenderUtils.drawRect(context, sliderX, sliderY, (int) Math.round(SLIDER_WIDTH * ratio), 3, ACCENT);
        RenderUtils.drawRect(context, sliderX + (int) Math.round(SLIDER_WIDTH * ratio) - 1, sliderY - 2, 3, 7, TEXT);
        context.drawText(this.textRenderer, fitText(ShortcutControl.getValueText(shortcut.configEntry().config()), 28), sliderX - 32, valueY, MUTED, false);
    }

    /**
     * 绘制布尔配置的开关控件。
     * toggleAnimation 按快捷项 manualId 缓存动画进度，避免开关状态变化时突兀跳动。
     */
    private void drawToggle(DrawContext context, QuickPanelItem shortcut, int x, int y, boolean hovered) {
        boolean enabled = ShortcutControl.getBooleanValue(shortcut.configEntry().config());
        String key = shortcut.shortcut().manualId();
        double current = this.toggleAnimation.getOrDefault(key, enabled ? 1.0 : 0.0);
        double target = enabled ? 1.0 : 0.0;
        current = HoloPanelVisuals.approach(current, target, 0.24);
        this.toggleAnimation.put(key, current);

        double eased = HoloPanelVisuals.easeOutQuart(current);
        int activeColor = ACCENT;
        int inactiveColor = 0xFF4A303A;
        int trackColor = HoloPanelVisuals.withAlpha(HoloPanelVisuals.mixRgb(inactiveColor, activeColor, eased), 0xE8);
        int knobX = x + 2 + (int) Math.round((TOGGLE_WIDTH - TOGGLE_HEIGHT) * eased);

        RenderUtils.drawRect(context, x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT, ACCENT);
        RenderUtils.drawRect(context, x + 2, y + 2, TOGGLE_WIDTH - 4, TOGGLE_HEIGHT - 4, trackColor);
        RenderUtils.drawRect(context, knobX, y + 2, TOGGLE_HEIGHT - 4, TOGGLE_HEIGHT - 4, TEXT);
        RenderUtils.drawRect(context, knobX + 2, y + 4, TOGGLE_HEIGHT - 8, TOGGLE_HEIGHT - 8, enabled ? ACCENT : BASE);
    }

    /**
     * 绘制右侧滚动指示条。
     * 只有内容超出可见区域时才调用，thumb 位置由当前 scrollOffset 占最大 offset 的比例决定。
     */
    private void drawScrollIndicator(DrawContext context) {
        int trackX = this.x + this.width - 5;
        int trackY = this.y + QuickPanelLayout.HEADER_HEIGHT + QuickPanelLayout.LIST_TOP_GAP;
        int trackHeight = this.height - QuickPanelLayout.HEADER_HEIGHT - QuickPanelLayout.LIST_TOP_GAP - QuickPanelLayout.PANEL_PADDING;
        int thumbHeight = Math.max(14, trackHeight * this.visibleRows / (this.visibleRows + this.maxScrollOffset));
        int thumbTravel = Math.max(1, trackHeight - thumbHeight);
        int thumbY = trackY + (int) Math.round(thumbTravel * (this.scrollOffset / (double) this.maxScrollOffset));

        RenderUtils.drawRect(context, trackX, trackY, 2, trackHeight, 0x661A1A1D);
        RenderUtils.drawRect(context, trackX - 1, thumbY, 4, thumbHeight, ACCENT);
    }

    /**
     * 计算局部索引所在单元格的左上角 X 坐标。
     * localIndex 是当前可见窗口内的索引，不包含 scrollOffset。
     */
    private int getCellX(int localIndex) {
        int column = localIndex % this.columns;
        return this.x + QuickPanelLayout.PANEL_PADDING + column * (getCellWidth() + QuickPanelLayout.GAP);
    }

    /**
     * 计算局部索引所在单元格的左上角 Y 坐标。
     * 行号由 localIndex / columns 得到，确保多列布局按行填充。
     */
    private int getCellY(int localIndex) {
        int index = localIndex / this.columns;
        return this.y + QuickPanelLayout.HEADER_HEIGHT + QuickPanelLayout.LIST_TOP_GAP + index * (QuickPanelLayout.ROW_HEIGHT + QuickPanelLayout.GAP);
    }

    /**
     * 返回当前帧计算出的单元格宽度。
     * 该值已扣除滚动条预留空间，绘制和命中测试必须共用它。
     */
    private int getCellWidth() {
        return this.cellWidth;
    }

    /**
     * 计算滑条左侧 X 坐标。
     * 滑条始终贴近单元格右侧，给左侧标签和值文本留下稳定空间。
     */
    private int getSliderX(int cellX, int cellWidth) {
        return cellX + cellWidth - SLIDER_WIDTH - 6;
    }

    /**
     * 根据用户配置和打开动画计算面板背景 alpha。
     * 配置值只影响背景层，文字、边框和控件仍保持稳定可读。
     */
    private int getPanelAlpha(double open) {
        double opacity = FastMasaConfigs.Generic.PANEL_OPACITY.getDoubleValue();
        return (int) Math.round(0xFF * opacity * open);
    }

    /**
     * 根据用户配置计算快捷项背景 alpha。
     * 悬停项略高亮，但仍跟随整体透明度，保持面板视觉统一。
     */
    private int getItemAlpha(boolean hovered) {
        double opacity = FastMasaConfigs.Generic.PANEL_OPACITY.getDoubleValue();
        int baseAlpha = hovered ? 0xA8 : 0x78;
        return Math.max(0x18, (int) Math.round(baseAlpha * opacity));
    }

    /**
     * 将文本裁剪到指定像素宽度以内。
     * Minecraft 文本宽度不是等宽字符数，必须用 TextRenderer 逐步测量并补上省略号。
     */
    private String fitText(String text, int maxWidth) {
        if (this.textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int end = text.length();

        while (end > 0 && this.textRenderer.getWidth(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }

        return text.substring(0, Math.max(0, end)) + ellipsis;
    }

    /**
     * 通用矩形命中测试。
     * 右边界和下边界使用开区间，避免相邻单元格边界重叠时同时命中两个元素。
     */
    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public enum PanelMode {
        SHORTCUTS,
        ENABLED_BOOLEANS
    }
}
