package fastui.yure.client.gui;

import fastui.yure.FastMasaConfig;
import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.client.index.ConfigIndexService;
import fastui.yure.client.input.HeldKeyInputSuppressor;
import fastui.yure.config.FastMasaConfigs;
import fastui.yure.config.ConfigGroup;
import fastui.yure.config.ConfigGroupStore;
import fastui.yure.config.GroupItem;
import fi.dy.masa.malilib.MaLiLib;
import fi.dy.masa.malilib.MaLiLibConfigs;
import fi.dy.masa.malilib.MaLiLibReference;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.IConfigResettable;
import fi.dy.masa.malilib.config.gui.ButtonPressDirtyListenerSimple;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.GuiKeybindSettings;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ConfigButtonKeybind;
import fi.dy.masa.malilib.gui.interfaces.IConfigInfoProvider;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.ModInfo;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.gui.screens.Screen;

import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Fast Masa Config 的全屏配置界面。
 * 主体列表自绘，右上角模组切换沿用 MaLiLib 的 WidgetDropDownList 行为。
 */
public final class FastMasaConfigGui extends GuiBase implements IKeybindConfigGui {
    private static final int MARGIN = 12;
    private static final int TAB_Y = 28;
    private static final int SEARCH_Y = 54;
    private static final int LIST_Y = 80;
    private static final int ROW_HEIGHT = 30;
    private static final int ROW_GAP = 3;
    private static final int BUTTON_HEIGHT = 20;
    private static final int COLOR_ROW = FastMasaMenuPalette.SURFACE_TRANSLUCENT;
    private static final int COLOR_ROW_HOVER = FastMasaMenuPalette.ROW_HOVER_TRANSLUCENT;
    private static final int COLOR_BORDER = FastMasaMenuPalette.BORDER;
    private static final int COLOR_ACCENT = FastMasaMenuPalette.ACCENT;
    private static final int COLOR_TEXT = FastMasaMenuPalette.TEXT;
    private static final int COLOR_MUTED = FastMasaMenuPalette.MUTED;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int NUMERIC_VALUE_WIDTH = 50;
    private static final int NUMERIC_SLIDER_WIDTH = 68;

    private static ConfigGuiTab tab = ConfigGuiTab.GENERIC;

    private final HeldKeyInputSuppressor inputSuppressor;
    private final List<Runnable> hotkeyChangeListeners = new ArrayList<>();
    private final ButtonPressDirtyListenerSimple dirtyListener = new ButtonPressDirtyListenerSimple();

    private GuiTextFieldGeneric searchField;
    private GuiTextFieldGeneric groupNameField;
    private int searchFieldWidth;
    private boolean searchFieldFocused;
    private int groupNameFieldWidth;
    private boolean groupNameFieldFocused;
    private ConfigButtonKeybind activeKeybindButton;
    private ConfigButtonKeybind openQuickConfigButton;
    private ButtonGeneric hotkeySettingsButton;
    private IConfigBase activeNumericSliderConfig;

    private List<IConfigBase> filteredGenericConfigs = List.of();
    private List<ConfigIndexEntry> filteredConfigs = List.of();
    private FilterMode filterMode = FilterMode.ALL;
    private String selectedModId = "";
    private String selectedConfigGroupId = "";
    private String selectedGroupId = "";
    private int scrollOffset;

    /**
     * 从 ModMenu 或命令直接打开时使用的构造函数，没有父界面，也不需要吞掉打开热键。
     */
    public FastMasaConfigGui() {
        this(null, Set.of());
    }

    /**
     * 从其它 Screen 打开时保留父界面引用，关闭后可按 MaLiLib 行为返回。
     */
    public FastMasaConfigGui(Screen parent) {
        this(parent, Set.of());
    }

    /**
     * 从快捷面板打开时使用的构造函数。
     * suppressKeys 是进入全屏页那一刻仍被按住的打开热键，用于防止它们进入搜索框。
     */
    public FastMasaConfigGui(Screen parent, Set<Integer> suppressKeys) {
        this(parent, suppressKeys, null);
    }

    /** 打开完整配置页并预选指定的浮动分组。 */
    public FastMasaConfigGui(Screen parent, Set<Integer> suppressKeys, String targetGroupId) {
        super();
        this.setParent(parent);
        this.setTitle(StringUtils.translate("fast-masa-config.gui.title.configs"));
        this.inputSuppressor = new HeldKeyInputSuppressor(suppressKeys);
        if (targetGroupId != null) {
            tab = ConfigGuiTab.ALL_CONFIGS;
            this.selectedGroupId = targetGroupId;
        }
    }

    static String recoveryTargetGroupId() {
        return "default";
    }

    @Override
    public void initGui() {
        super.initGui();
        ConfigGroupStore.ensureDefaultGroup();
        if (tab == ConfigGuiTab.ALL_CONFIGS) {
            this.normalizeSelectedGroup();
        }
        this.clearOptions();
        this.buildConfigSwitcher();
        this.createTabButtons();
        this.createTabInputs();
        this.refreshVisibleRows();
    }

    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor drawContext,
            int mouseX, int mouseY, float partialTicks) {
        GuiContext ctx = GuiContext.fromGuiGraphics(drawContext);
        ctx.nextStratum();

        this.drawScreenBackground(ctx, mouseX, mouseY);
        this.drawTitle(ctx, mouseX, mouseY, partialTicks);
        this.drawContents(ctx, mouseX, mouseY, partialTicks);
        this.drawButtons(ctx, mouseX, mouseY, partialTicks);
        this.drawTextFields(ctx, mouseX, mouseY);
        this.drawWidgets(ctx, mouseX, mouseY);
        this.drawSearchSuggestion(ctx);
        this.drawHoveredWidget(ctx, mouseX, mouseY);
        this.drawButtonHoverTexts(ctx, mouseX, mouseY, partialTicks);
        this.drawGuiMessages(ctx);
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean onMouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubleClick) {
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        this.searchFieldFocused = this.isSearchFieldHit(mouseX, mouseY);
        this.groupNameFieldFocused = this.isGroupNameFieldHit(mouseX, mouseY);

        if (super.onMouseClicked(click, doubleClick)) {
            return true;
        }

        if (this.activeKeybindButton != null) {
            this.setActiveKeybindButton(null);
            return true;
        }

        return switch (tab) {
            case GENERIC -> this.handleGenericClick(mouseX, mouseY);
            case ALL_CONFIGS -> this.handleAllConfigsClick(mouseX, mouseY);
        };
    }

    @Override
    public boolean onMouseDragged(net.minecraft.client.input.MouseButtonEvent click,
            double dragXAmount, double dragYAmount) {
        int mouseX = (int) click.x();

        if (this.activeNumericSliderConfig != null) {
            this.applyNumericSliderValue(this.activeNumericSliderConfig, mouseX);
            return true;
        }

        return super.onMouseDragged(click, dragXAmount, dragYAmount);
    }

    @Override
    public boolean onMouseReleased(net.minecraft.client.input.MouseButtonEvent click) {
        this.activeNumericSliderConfig = null;
        return super.onMouseReleased(click);
    }

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (super.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }

        if (this.isInsideList((int) mouseX, (int) mouseY)) {
            int previous = this.scrollOffset;
            this.scrollOffset = clamp(this.scrollOffset + (verticalAmount < 0 ? 1 : -1), 0,
                    Math.max(0, this.getCurrentRowCount() - this.getVisibleRows()));
            return previous != this.scrollOffset;
        }

        return false;
    }

    @Override
    public boolean onKeyTyped(net.minecraft.client.input.KeyEvent input) {
        int keyCode = input.key();

        if (this.inputSuppressor.shouldSuppressKey(keyCode)) {
            return true;
        }

        if (this.activeKeybindButton != null) {
            this.activeKeybindButton.onKeyPressed(keyCode);
            this.notifyOwnConfigChanged(true);
            return true;
        }

        return super.onKeyTyped(input);
    }

    @Override
    public boolean onCharTyped(net.minecraft.client.input.CharacterEvent input) {
        if (this.inputSuppressor.shouldSuppressChar()) {
            return true;
        }

        return super.onCharTyped(input);
    }

    @Override
    public boolean keyReleased(net.minecraft.client.input.KeyEvent input) {
        int keyCode = input.key();
        this.inputSuppressor.release(keyCode);
        return super.keyReleased(input);
    }

    @Override
    public void removed() {
        if (this.activeKeybindButton != null) {
            this.setActiveKeybindButton(null);
        }

        if (this.dirtyListener.isDirty()) {
            this.notifyOwnConfigChanged(true);
            this.dirtyListener.resetDirty();
        }

        super.removed();
    }

    @Override
    protected void drawScreenBackground(GuiContext ctx, int mouseX, int mouseY) {
        RenderUtils.drawRect(ctx, 0, 0, this.width, this.height, FastMasaMenuPalette.SCREEN_BACKGROUND);
        RenderUtils.drawRect(ctx, 0, 0, this.width, 26, FastMasaMenuPalette.SCREEN_HEADER);
        RenderUtils.drawRect(ctx, 0, 25, this.width, 1, FastMasaMenuPalette.BORDER);
    }

    @Override
    protected void drawTitle(GuiContext ctx, int mouseX, int mouseY, float partialTicks) {
        this.drawString(ctx, StringUtils.translate("fast-masa-config.gui.title.configs"), MARGIN, 9,
                FastMasaMenuPalette.TEXT);
        RenderUtils.drawRect(ctx, MARGIN, 24, 40, 2, FastMasaMenuPalette.ACCENT);
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks) {
        switch (tab) {
            case GENERIC -> this.drawGenericRows(ctx, mouseX, mouseY);
            case ALL_CONFIGS -> this.drawAllConfigRows(ctx, mouseX, mouseY);
        }
    }

    @Override
    public String getModId() {
        return FastMasaConfig.MOD_ID;
    }

    @Override
    public void clearOptions() {
        this.setActiveKeybindButton(null);
        this.hotkeyChangeListeners.clear();
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return List.of();
    }

    @Override
    public ButtonPressDirtyListenerSimple getButtonPressListener() {
        return this.dirtyListener;
    }

    @Override
    public IConfigInfoProvider getHoverInfoProvider() {
        return null;
    }

    @Override
    public void addKeybindChangeListener(Runnable listener) {
        this.hotkeyChangeListeners.add(listener);
    }

    @SuppressWarnings("null")
    @Override
    public void setActiveKeybindButton(@Nullable ConfigButtonKeybind button) {
        if (this.activeKeybindButton != null) {
            this.activeKeybindButton.onClearSelection();
            this.updateKeybindButtons();
        }

        this.activeKeybindButton = button;

        if (this.activeKeybindButton != null) {
            this.activeKeybindButton.onSelected();
        }
    }

    private void buildConfigSwitcher() {
        ModInfo thisMod = Registry.CONFIG_SCREEN.getModInfoFromConfigScreen(this.getClass());

        if (thisMod == null) {
            try {
                MaLiLib.debugLog("FastMasaConfigGui#initGui(): Attempting to register [{}] ...", this.getModId());
                Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                        new ModInfo(this.getModId(), StringUtils.splitCamelCase(this.getModId()), () -> this));
                thisMod = Registry.CONFIG_SCREEN.getModInfoFromConfigScreen(this.getClass());
            } catch (Exception ignored) {
                MaLiLib.LOGGER.warn("FastMasaConfigGui#initGui(): Failed to automatically register [{}]",
                        this.getModId());
                return;
            }
        }

        if (thisMod != null && MaLiLibConfigs.Generic.ENABLE_CONFIG_SWITCHER.getBooleanValue()) {
            ModInfo selectedMod = thisMod;
            WidgetDropDownList<ModInfo> modSwitchWidget = new WidgetDropDownList<>(
                    GuiUtils.getScaledWindowWidth() - 155, 6, 130, 18, 200, 10,
                    Registry.CONFIG_SCREEN.getAllModsWithConfigScreens()) {
                {
                    selectedEntry = selectedMod;
                }

                @Override
                protected void setSelectedEntry(int index) {
                    super.setSelectedEntry(index);

                    ModInfo currentSelection = selectedEntry;
                    if (currentSelection != null) {
                        var screenSupplier = currentSelection.configScreenSupplier();
                        if (screenSupplier != null) {
                            mc.setScreen(screenSupplier.get());
                        }
                    }
                }

                @Override
                protected String getDisplayString(ModInfo entry) {
                    return entry.modName();
                }
            };

            this.addWidget(modSwitchWidget);
        }
    }

    private void createTabButtons() {
        int x = MARGIN;

        for (ConfigGuiTab value : ConfigGuiTab.values()) {
            ButtonGeneric button = new ButtonGeneric(x, TAB_Y, -1, BUTTON_HEIGHT, value.getDisplayName());
            button.setEnabled(tab != value);
            this.addButton(button, (clicked, mouseButton) -> {
                tab = value;
                this.scrollOffset = 0;
                this.initGui();
            });
            x += button.getWidth() + 4;
        }
    }

    private void createTabInputs() {
        this.searchField = null;
        this.groupNameField = null;
        this.searchFieldFocused = false;
        this.groupNameFieldFocused = false;

        boolean compactFilters = this.isCompactFilterLayout();
        boolean wrapFilters = tab != ConfigGuiTab.GENERIC && filterControlsWrap(this.width);
        int filterButtonWidth = tab == ConfigGuiTab.GENERIC ? 0
                : (compactFilters ? 60 : 110);
        int modButtonWidth = tab == ConfigGuiTab.GENERIC ? 0 : (compactFilters ? 60 : 118);
        int groupButtonWidth = tab == ConfigGuiTab.GENERIC ? 0 : (compactFilters ? 60 : 118);
        int searchWidth = wrapFilters ? Math.max(80, this.width - MARGIN * 2) : Math.min(220,
                Math.max(80, this.width - MARGIN * 2 - filterButtonWidth - modButtonWidth - groupButtonWidth - 18));
        this.searchField = new GuiTextFieldGeneric(MARGIN, SEARCH_Y, searchWidth, 18, this.font);
        this.searchFieldWidth = searchWidth;
        this.searchField.setMaxLength(128);
        this.searchField.setSuggestion("");
        this.addTextField(this.searchField, field -> {
            this.scrollOffset = 0;
            this.refreshVisibleRows();
            return true;
        });

        if (tab != ConfigGuiTab.GENERIC) {
            int filterY = wrapFilters ? SEARCH_Y + BUTTON_HEIGHT + 4 : SEARCH_Y - 1;
            int filterX = wrapFilters ? MARGIN : MARGIN + searchWidth + 6;
            this.addButton(new ButtonGeneric(filterX, filterY, filterButtonWidth,
                    BUTTON_HEIGHT, this.getFilterButtonText()), (button, mouseButton) -> {
                        this.filterMode = this.getNextFilterMode();
                        this.scrollOffset = 0;
                        this.initGui();
                    });
            this.addButton(new ButtonGeneric(filterX + filterButtonWidth + 6, filterY,
                    modButtonWidth, BUTTON_HEIGHT, this.getModFilterButtonText()), (button, mouseButton) -> {
                        this.cycleModFilter();
                        this.scrollOffset = 0;
                        this.initGui();
                    });
            this.addButton(
                    new ButtonGeneric(filterX + filterButtonWidth + modButtonWidth + 12, filterY,
                            groupButtonWidth, BUTTON_HEIGHT, this.getGroupFilterButtonText()),
                    (button, mouseButton) -> {
                        this.cycleGroupFilter();
                        this.scrollOffset = 0;
                        this.initGui();
                    });
        } else {
            this.createGenericButtons();
        }

        if (tab == ConfigGuiTab.ALL_CONFIGS) {
            this.createAllConfigsGroupControls();
        }
    }

    private void createGenericButtons() {
        int controlX = this.getControlX();
        int settingsWidth = 20;
        int keybindWidth = Math.max(80, Math.min(150, this.width - controlX - MARGIN - settingsWidth - 4));
        this.openQuickConfigButton = new ConfigButtonKeybind(controlX, -1000, keybindWidth, BUTTON_HEIGHT,
                FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind(), this);
        this.hotkeySettingsButton = new HotkeySettingsButton(controlX + keybindWidth + 4, -1000, settingsWidth,
                BUTTON_HEIGHT, FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind());
        this.hotkeySettingsButton.setHoverStrings("fast-masa-config.gui.full.hotkey_settings.hover");
        this.addKeybindChangeListener(this.openQuickConfigButton::updateDisplayString);
        this.addButton(this.openQuickConfigButton, this.getButtonPressListener());
        this.addButton(this.hotkeySettingsButton, (button, mouseButton) -> {
            if (mouseButton == 1) {
                FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().resetSettingsToDefaults();
                this.notifyOwnConfigChanged(true);
            } else {
                GuiBase.openGui(new GuiKeybindSettings(FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind(),
                        FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getName(), null, GuiUtils.getCurrentScreen()));
            }
        });
    }

    private void refreshVisibleRows() {
        if (tab == ConfigGuiTab.GENERIC) {
            String filter = this.getSearchText();
            this.filteredGenericConfigs = FastMasaConfigs.Generic.OPTIONS.stream()
                    .filter(config -> this.matchesGenericConfig(config, filter))
                    .toList();
        } else if (tab == ConfigGuiTab.ALL_CONFIGS) {
            String filter = this.getSearchText();
            List<ConfigIndexEntry> index = ConfigIndexService.scanSupportedConfigs();
            this.normalizeSelectedFilters(index);
            this.normalizeSelectedGroup();
            ConfigGroup selectedGroup = this.getSelectedGroup();
            List<GroupItem> selectedItems = selectedGroup == null ? List.of() : selectedGroup.items();
            this.filteredConfigs = index.stream()
                    .filter(this::matchesSelectedFilters)
                    .filter(entry -> this.matchesConfig(entry, filter))
                    .filter(this::matchesConfigFilterMode)
                    .sorted(Comparator.comparingInt(entry -> groupItemOrder(selectedItems, entry)))
                    .toList();
        }

        this.scrollOffset = clamp(this.scrollOffset, 0, Math.max(0, this.getCurrentRowCount() - this.getVisibleRows()));
    }

    private void drawGenericRows(GuiContext context, int mouseX, int mouseY) {
        int x = MARGIN;
        int width = this.width - MARGIN * 2;
        int controlX = this.getControlX();
        this.drawListHeader(context, this.filteredGenericConfigs.size(), FastMasaConfigs.Generic.OPTIONS.size());

        if (this.filteredGenericConfigs.isEmpty()) {
            this.drawEmptyText(context, StringUtils.translate("fast-masa-config.gui.full.empty_search"));
            return;
        }

        int visible = this.getVisibleRows();
        int end = Math.min(this.filteredGenericConfigs.size(), this.scrollOffset + visible);
        this.positionOpenQuickConfigButton(-1000);

        for (int i = this.scrollOffset; i < end; i++) {
            IConfigBase config = this.filteredGenericConfigs.get(i);
            int y = LIST_Y + (i - this.scrollOffset) * (ROW_HEIGHT + ROW_GAP);
            boolean hovered = GuiHitTest.isInside(mouseX, mouseY, x, y, width, ROW_HEIGHT);
            this.drawRowBase(context, x, y, width, hovered, false);
            this.drawString(context, fitText(config.getConfigGuiDisplayName(), controlX - x - 24), x + 8, y + 6,
                    COLOR_TEXT);
            this.drawString(context, fitText(config.getComment() == null ? "" : config.getComment(), controlX - x - 24),
                    x + 8, y + 18, COLOR_MUTED);

            if (config.getType() != ConfigType.HOTKEY) {
                this.drawGenericControl(context, config, controlX, y + 5, mouseX, mouseY);
            } else if (config == FastMasaConfigs.Generic.OPEN_QUICK_CONFIG) {
                this.positionOpenQuickConfigButton(y + 5);
            }
        }

        this.drawScrollBar(context, this.filteredGenericConfigs.size());
    }

    private void drawGenericControl(GuiContext context, IConfigBase config, int x, int y, int mouseX, int mouseY) {
        if (config instanceof IConfigBoolean booleanConfig) {
            boolean enabled = booleanConfig.getBooleanValue();
            int bg = enabled ? COLOR_ACCENT : FastMasaMenuPalette.NEUTRAL;
            this.drawSmallButton(context, x, y, 64, StringUtils.translate(enabled
                    ? "fast-masa-config.gui.boolean.on" : "fast-masa-config.gui.boolean.off"), bg,
                    GuiHitTest.isInside(mouseX, mouseY, x, y, 64, BUTTON_HEIGHT));
            this.drawResetButton(context, config, x + 70, y, 54, mouseX, mouseY);
        } else if (config instanceof IConfigInteger integerConfig) {
            this.drawNumericControl(context, config, NumericControlLayout.calculate(this.width), y, mouseX, mouseY,
                    integerConfig.getStringValue(),
                    this.getIntegerRatio(integerConfig));
        } else if (config instanceof IConfigDouble doubleConfig) {
            this.drawNumericControl(context, config, NumericControlLayout.calculate(this.width), y, mouseX, mouseY,
                    formatDouble(doubleConfig.getDoubleValue()),
                    this.getDoubleRatio(doubleConfig));
        }
    }

    private void positionOpenQuickConfigButton(int y) {
        if (this.openQuickConfigButton != null) {
            this.openQuickConfigButton.setX(this.getControlX());
            this.openQuickConfigButton.setY(y);
            this.openQuickConfigButton.setEnabled(y >= 0);
        }

        if (this.hotkeySettingsButton != null && this.openQuickConfigButton != null) {
            this.hotkeySettingsButton
                    .setX(this.openQuickConfigButton.getX() + this.openQuickConfigButton.getWidth() + 4);
            this.hotkeySettingsButton.setY(y);
            this.hotkeySettingsButton.setEnabled(y >= 0);
        }
    }

    private void drawAllConfigRows(GuiContext context, int mouseX, int mouseY) {
        this.drawListHeader(context, this.filteredConfigs.size(), ConfigIndexService.scanSupportedConfigs().size());

        if (this.filteredConfigs.isEmpty()) {
            this.drawEmptyText(context, StringUtils.translate("fast-masa-config.gui.full.empty_search"));
            return;
        }

        int visible = this.getVisibleRows();
        int end = Math.min(this.filteredConfigs.size(), this.scrollOffset + visible);

        for (int i = this.scrollOffset; i < end; i++) {
            this.drawAllConfigRow(context, this.filteredConfigs.get(i), i - this.scrollOffset, mouseX, mouseY);
        }

        this.drawScrollBar(context, this.filteredConfigs.size());
    }

    private void drawAllConfigRow(GuiContext context, ConfigIndexEntry entry, int visibleIndex, int mouseX,
            int mouseY) {
        int x = MARGIN;
        int y = this.getListTop() + visibleIndex * (ROW_HEIGHT + ROW_GAP);
        int width = this.width - MARGIN * 2;
        int buttonX = x + width - 76;
        boolean selected = this.isInSelectedGroup(entry);
        boolean hovered = GuiHitTest.isInside(mouseX, mouseY, x, y, width, ROW_HEIGHT);
        String meta = entry.modName() + " / " + entry.groupName() + " / " + entry.manualId();

        this.drawRowBase(context, x, y, width, hovered, selected);
        this.drawString(context, fitText(entry.displayName(), buttonX - x - 16), x + 8, y + 6, COLOR_TEXT);
        this.drawString(context, fitText(meta, buttonX - x - 16), x + 8, y + 18, COLOR_MUTED);
        if (selected) {
            this.drawSmallButton(context, buttonX - 48, y + 5, 20, "↑", FastMasaMenuPalette.CONTROL_DARK,
                    GuiHitTest.isInside(mouseX, mouseY, buttonX - 48, y + 5, 20, BUTTON_HEIGHT));
            this.drawSmallButton(context, buttonX - 24, y + 5, 20, "↓", FastMasaMenuPalette.CONTROL_DARK,
                    GuiHitTest.isInside(mouseX, mouseY, buttonX - 24, y + 5, 20, BUTTON_HEIGHT));
        }
        this.drawSmallButton(context, buttonX, y + 5, 64, selected ? "-" : "+", selected
                        ? FastMasaMenuPalette.ACTION_REMOVE : FastMasaMenuPalette.ACTION_ADD,
                GuiHitTest.isInside(mouseX, mouseY, buttonX, y + 5, 64, BUTTON_HEIGHT));
    }

    private void drawListHeader(GuiContext context, int visibleCount, int totalCount) {
        String text = visibleCount + " / " + totalCount;
        this.drawString(context, text, this.width - MARGIN - this.getStringWidth(text), SEARCH_Y + 5, COLOR_MUTED);
    }

    private void drawSearchSuggestion(GuiContext context) {
        if (this.searchField != null && !this.searchFieldFocused && this.searchField.getValue().isBlank()) {
            this.drawString(context, StringUtils.translate("fast-masa-config.gui.full.search"), MARGIN + 4,
                    SEARCH_Y + 5, COLOR_MUTED);
        }
        if (this.groupNameField != null && !this.groupNameFieldFocused && this.groupNameField.getValue().isBlank()) {
            int y = this.getGroupControlsY() + BUTTON_HEIGHT + 4;
            this.drawString(context, StringUtils.translate("fast-masa-config.gui.groups.name"), MARGIN + 4,
                    y + 5, COLOR_MUTED);
        }
    }

    private boolean isSearchFieldHit(int mouseX, int mouseY) {
        return this.searchField != null && GuiHitTest.isInside(mouseX, mouseY, MARGIN, SEARCH_Y,
                this.searchFieldWidth, 18);
    }

    private boolean isGroupNameFieldHit(int mouseX, int mouseY) {
        if (this.groupNameField == null || tab != ConfigGuiTab.ALL_CONFIGS) {
            return false;
        }
        int y = this.getGroupControlsY() + BUTTON_HEIGHT + 4;
        return GuiHitTest.isInside(mouseX, mouseY, MARGIN, y, this.groupNameFieldWidth, 18);
    }

    private void drawEmptyText(GuiContext context, String text) {
        this.drawString(context, fitText(text, this.width - MARGIN * 2 - 16), MARGIN + 8, this.getListTop() + 12,
                COLOR_MUTED);
    }

    private void drawRowBase(GuiContext context, int x, int y, int width, boolean hovered, boolean active) {
        int background = active ? FastMasaMenuPalette.MODULE_BACKGROUND : (hovered ? COLOR_ROW_HOVER : COLOR_ROW);
        RenderUtils.drawRect(context, x, y, width, ROW_HEIGHT, background);
        RenderUtils.drawRect(context, x, y, width, 1, hovered ? FastMasaMenuPalette.BORDER_HOVER : COLOR_BORDER);
        RenderUtils.drawRect(context, x, y, 2, ROW_HEIGHT, active || hovered ? COLOR_ACCENT : COLOR_BORDER);
    }

    private void drawScrollBar(GuiContext context, int rowCount) {
        int visibleRows = this.getVisibleRows();

        if (rowCount <= visibleRows) {
            return;
        }

        int top = this.getListTop();
        int bottom = this.height - 18;
        int height = Math.max(1, bottom - top);
        int thumbHeight = Math.max(16, height * visibleRows / rowCount);
        int maxOffset = Math.max(1, rowCount - visibleRows);
        int thumbY = top + (height - thumbHeight) * this.scrollOffset / maxOffset;
        int x = this.width - MARGIN - SCROLLBAR_WIDTH;

        RenderUtils.drawRect(context, x, top, SCROLLBAR_WIDTH, height, FastMasaMenuPalette.SCROLLBAR);
        RenderUtils.drawRect(context, x, thumbY, SCROLLBAR_WIDTH, thumbHeight, COLOR_ACCENT);
    }

    private void drawSmallButton(GuiContext context, int x, int y, int width, String text, int color,
            boolean hovered) {
        RenderUtils.drawRect(context, x, y, width, BUTTON_HEIGHT, hovered ? lighten(color) : color);
        RenderUtils.drawRect(context, x, y, width, 1, COLOR_BORDER);
        int textX = x + (width - this.getStringWidth(text)) / 2;
        this.drawString(context, text, textX, y + 6, COLOR_TEXT);
    }

    private void drawValueBox(GuiContext context, int x, int y, int width, String text) {
        RenderUtils.drawRect(context, x, y, width, BUTTON_HEIGHT, FastMasaMenuPalette.BUTTON);
        this.drawString(context, fitText(text, width - 8), x + 4, y + 6, COLOR_TEXT);
    }

    private void drawNumericControl(GuiContext context, IConfigBase config, NumericControlLayout layout, int y,
            int mouseX, int mouseY,
            String valueText, double ratio) {
        this.drawValueBox(context, layout.valueX(), y, layout.valueWidth(), valueText);
        this.drawNumericSlider(context, layout.sliderX(), y, layout.sliderWidth(), ratio,
                GuiHitTest.isInside(mouseX, mouseY, layout.sliderX(), y, layout.sliderWidth(), BUTTON_HEIGHT));
        this.drawResetButton(context, config, layout.resetX(), y, layout.resetWidth(), mouseX, mouseY);
    }

    private void drawNumericSlider(GuiContext context, int x, int y, int width, double ratio, boolean hovered) {
        int trackY = y + BUTTON_HEIGHT / 2 - 1;
        int fillWidth = (int) Math.round(width * clampRatio(ratio));
        int knobX = x + Math.max(0, fillWidth - 2);

        RenderUtils.drawRect(context, x, trackY, width, 3,
                hovered ? FastMasaMenuPalette.NUMERIC_TRACK_HOVER : FastMasaMenuPalette.NUMERIC_TRACK);
        RenderUtils.drawRect(context, x, trackY, fillWidth, 3, COLOR_ACCENT);
        RenderUtils.drawRect(context, knobX, y + 3, 4, BUTTON_HEIGHT - 6, COLOR_TEXT);
    }

    private void drawResetButton(GuiContext context, IConfigBase config, int x, int y, int width, int mouseX, int mouseY) {
        boolean modified = config instanceof IConfigResettable resettable && resettable.isModified();
        this.drawSmallButton(context, x, y, width, StringUtils.translate("malilib.gui.button.reset.caps"),
                modified ? FastMasaMenuPalette.RESET_MODIFIED : FastMasaMenuPalette.RESET_DEFAULT,
                modified && GuiHitTest.isInside(mouseX, mouseY, x, y, width, BUTTON_HEIGHT));
    }

    private boolean handleGenericClick(int mouseX, int mouseY) {
        int controlX = this.getControlX();

        int index = this.getRowIndexAt(mouseX, mouseY, this.filteredGenericConfigs.size());

        if (index < 0) {
            return false;
        }

        IConfigBase config = this.filteredGenericConfigs.get(index);
        int y = LIST_Y + (index - this.scrollOffset) * (ROW_HEIGHT + ROW_GAP) + 5;

        if (config instanceof IConfigBoolean booleanConfig) {
            if (GuiHitTest.isInside(mouseX, mouseY, controlX, y, 64, BUTTON_HEIGHT)) {
                booleanConfig.setBooleanValue(!booleanConfig.getBooleanValue());
                this.notifyOwnConfigChanged(false);
                return true;
            }

            if (this.handleResetClick(config, mouseX, mouseY, controlX + 70, y, 54)) {
                return true;
            }
        } else if (config instanceof IConfigInteger || config instanceof IConfigDouble) {
            NumericControlLayout layout = NumericControlLayout.calculate(this.width);
            if (this.handleNumericSliderClick(config, mouseX, mouseY, layout, y)) {
                return true;
            }

            if (this.handleResetClick(config, mouseX, mouseY, layout.resetX(), y, layout.resetWidth())) {
                return true;
            }
        }

        return false;
    }

    private boolean handleNumericSliderClick(IConfigBase config, int mouseX, int mouseY, NumericControlLayout layout,
            int y) {
        if (GuiHitTest.isInside(mouseX, mouseY, layout.sliderX(), y, layout.sliderWidth(),
                BUTTON_HEIGHT) == false) {
            return false;
        }

        this.activeNumericSliderConfig = config;
        this.applyNumericSliderValue(config, mouseX, layout);
        return true;
    }

    private boolean handleAllConfigsClick(int mouseX, int mouseY) {
        if (this.handleAllConfigsGroupAction(mouseX, mouseY)) {
            return true;
        }
        int index = this.getRowIndexAt(mouseX, mouseY, this.filteredConfigs.size());

        if (index < 0) {
            return false;
        }

        ConfigIndexEntry entry = this.filteredConfigs.get(index);
        int x = MARGIN;
        int width = this.width - MARGIN * 2;
        int rowY = this.getListTop() + (index - this.scrollOffset) * (ROW_HEIGHT + ROW_GAP) + 5;
        int buttonX = x + width - 76;

        if (this.isInSelectedGroup(entry)) {
            int itemIndex = this.getSelectedGroupItemIndex(entry);
            ConfigGroup selected = this.getSelectedGroup();
            if (selected != null && GuiHitTest.isInside(mouseX, mouseY, buttonX - 48, rowY, 20, BUTTON_HEIGHT)) {
                if (ConfigGroupStore.moveItem(selected.id(), itemIndex, -1)) {
                    this.afterGroupChanged();
                }
                return true;
            }
            if (selected != null && GuiHitTest.isInside(mouseX, mouseY, buttonX - 24, rowY, 20, BUTTON_HEIGHT)) {
                if (ConfigGroupStore.moveItem(selected.id(), itemIndex, 1)) {
                    this.afterGroupChanged();
                }
                return true;
            }
        }

        if (GuiHitTest.isInside(mouseX, mouseY, buttonX, rowY, 64, BUTTON_HEIGHT)) {
            ConfigGroup selected = this.getSelectedGroup();
            if (selected == null) {
                return true;
            }
            if (this.isInSelectedGroup(entry)) {
                int itemIndex = this.getSelectedGroupItemIndex(entry);
                if (itemIndex >= 0) {
                    ConfigGroupStore.removeItem(selected.id(), itemIndex);
                }
            } else {
                ConfigGroupStore.addItem(selected.id(), new GroupItem(entry.modId(), entry.groupId(), entry.configName(), false));
            }
            this.afterGroupChanged();
            return true;
        }

        return false;
    }

    private boolean handleAllConfigsGroupAction(int mouseX, int mouseY) {
        int actionY = this.getGroupControlsY();
        int selectorWidth = this.getGroupSelectorWidth();
        int actionX = MARGIN + selectorWidth + 4;
        if (GuiHitTest.isInside(mouseX, mouseY, MARGIN, actionY, selectorWidth, BUTTON_HEIGHT)) {
            this.selectNextTargetGroup();
            return true;
        }
        if (GuiHitTest.isInside(mouseX, mouseY, actionX, actionY, 30, BUTTON_HEIGHT)) {
            ConfigGroup selected = this.getSelectedGroup();
            if (selected != null && ConfigGroupStore.hide(selected.id(), !selected.hidden())) {
                this.afterGroupChanged();
            }
            return true;
        }
        if (GuiHitTest.isInside(mouseX, mouseY, actionX + 34, actionY, 30, BUTTON_HEIGHT)) {
            ConfigGroup selected = this.getSelectedGroup();
            if (selected != null && ConfigGroupStore.remove(selected.id())) {
                this.selectedGroupId = "";
                this.afterGroupChanged();
            }
            return true;
        }
        if (GuiHitTest.isInside(mouseX, mouseY, actionX + 68, actionY, 24, BUTTON_HEIGHT)) {
            ConfigGroup selected = this.getSelectedGroup();
            if (selected != null && ConfigGroupStore.moveGroup(selected.id(), -1)) {
                this.afterGroupChanged();
                this.initGui();
            }
            return true;
        }
        if (GuiHitTest.isInside(mouseX, mouseY, actionX + 96, actionY, 24, BUTTON_HEIGHT)) {
            ConfigGroup selected = this.getSelectedGroup();
            if (selected != null && ConfigGroupStore.moveGroup(selected.id(), 1)) {
                this.afterGroupChanged();
                this.initGui();
            }
            return true;
        }
        return false;
    }

    private void createGroup() {
        if (this.groupNameField == null || this.groupNameField.getValue().trim().isBlank()) {
            return;
        }
        ConfigGroup group = ConfigGroupStore.create(this.groupNameField.getValue().trim());
        this.selectedGroupId = group.id();
        this.groupNameField.setValue("");
        this.afterGroupChanged();
    }

    private void renameSelectedGroup() {
        if (this.groupNameField == null || this.selectedGroupId.isBlank()
                || this.groupNameField.getValue().trim().isBlank()) {
            return;
        }
        if (ConfigGroupStore.rename(this.selectedGroupId, this.groupNameField.getValue().trim())) {
            this.groupNameField.setValue("");
            this.afterGroupChanged();
        }
    }

    private void createAllConfigsGroupControls() {
        int controlsY = this.getGroupControlsY();
        GroupActionLayout actionLayout = GroupActionLayout.calculate(this.width);
        int selectorWidth = actionLayout.selectorWidth();
        int actionX = actionLayout.actionX();
        ConfigGroup selected = this.getSelectedGroup();
        String targetLabel = StringUtils.translate("fast-masa-config.gui.group.target")
                + (selected == null ? StringUtils.translate("fast-masa-config.gui.group.none") : selected.name());
        this.addButton(new ButtonGeneric(MARGIN, controlsY, selectorWidth, BUTTON_HEIGHT, targetLabel),
                (button, mouseButton) -> this.selectNextTargetGroup());
        this.addButton(new ButtonGeneric(actionX, controlsY, 30, BUTTON_HEIGHT,
                StringUtils.translate(selected != null && selected.hidden() ? "fast-masa-config.gui.group.show"
                        : "fast-masa-config.gui.group.hide")), (button, mouseButton) -> {
                    ConfigGroup current = this.getSelectedGroup();
                    if (current != null && ConfigGroupStore.hide(current.id(), !current.hidden())) {
                        this.afterGroupChanged();
                        this.initGui();
                    }
                });
        ButtonGeneric deleteButton = new ButtonGeneric(actionX + 34, controlsY, 30, BUTTON_HEIGHT, "x");
        deleteButton.setHoverStrings("fast-masa-config.gui.group.delete");
        deleteButton.setEnabled(selected != null && !selected.system());
        this.addButton(deleteButton, (button, mouseButton) -> {
            ConfigGroup current = this.getSelectedGroup();
            if (current != null && ConfigGroupStore.remove(current.id())) {
                this.selectedGroupId = "";
                this.afterGroupChanged();
                this.initGui();
            }
        });
        ButtonGeneric moveGroupUpButton = new ButtonGeneric(actionX + 68, controlsY, 24, BUTTON_HEIGHT, "↑");
        moveGroupUpButton.setHoverStrings("fast-masa-config.gui.group.move_up");
        this.addButton(moveGroupUpButton, (button, mouseButton) -> this.moveSelectedGroup(-1));
        ButtonGeneric moveGroupDownButton = new ButtonGeneric(actionX + 96, controlsY, 24, BUTTON_HEIGHT, "↓");
        moveGroupDownButton.setHoverStrings("fast-masa-config.gui.group.move_down");
        this.addButton(moveGroupDownButton, (button, mouseButton) -> this.moveSelectedGroup(1));

        int nameY = controlsY + BUTTON_HEIGHT + 4;
        int nameWidth = Math.max(60, this.width - MARGIN * 2 - 68);
        this.groupNameField = new GuiTextFieldGeneric(MARGIN, nameY, nameWidth, 18, this.font);
        this.groupNameFieldWidth = nameWidth;
        this.groupNameField.setMaxLength(128);
        this.groupNameField.setSuggestion("");
        this.addTextField(this.groupNameField, field -> true);
        ButtonGeneric createButton = new ButtonGeneric(MARGIN + nameWidth + 4, nameY - 1, 30, BUTTON_HEIGHT, "+");
        createButton.setHoverStrings("fast-masa-config.gui.group.create");
        this.addButton(createButton, (button, mouseButton) -> this.createGroup());
        ButtonGeneric renameButton = new ButtonGeneric(MARGIN + nameWidth + 38, nameY - 1, 30, BUTTON_HEIGHT, "R");
        renameButton.setHoverStrings("fast-masa-config.gui.group.rename");
        this.addButton(renameButton, (button, mouseButton) -> this.renameSelectedGroup());
    }

    private void afterGroupChanged() {
        this.scrollOffset = 0;
        this.refreshVisibleRows();
        this.notifyOwnConfigChanged(false);
    }

    private void moveSelectedGroup(int offset) {
        ConfigGroup selected = this.getSelectedGroup();
        if (selected != null && ConfigGroupStore.moveGroup(selected.id(), offset)) {
            this.afterGroupChanged();
            this.initGui();
        }
    }

    private boolean handleResetClick(IConfigBase config, int mouseX, int mouseY, int x, int y, int width) {
        if (GuiHitTest.isInside(mouseX, mouseY, x, y, width, BUTTON_HEIGHT)
                && config instanceof IConfigResettable resettable && resettable.isModified()) {
            resettable.resetToDefault();
            this.notifyOwnConfigChanged(false);
            return true;
        }

        return false;
    }

    private void notifyOwnConfigChanged(boolean updateHotkeys) {
        ConfigManager.getInstance().onConfigsChanged(FastMasaConfig.MOD_ID);

        if (updateHotkeys) {
            InputEventHandler.getKeybindManager().updateUsedKeys();
            this.updateKeybindButtons();
        }
    }

    private void updateKeybindButtons() {
        for (Runnable listener : this.hotkeyChangeListeners) {
            listener.run();
        }
    }

    private boolean matchesGenericConfig(IConfigBase config, String filter) {
        if (filter.isBlank()) {
            return true;
        }

        String haystack = (config.getName() + " " + config.getConfigGuiDisplayName() + " " + config.getComment())
                .toLowerCase(Locale.ROOT);
        return haystack.contains(filter);
    }

    private boolean matchesConfig(ConfigIndexEntry entry, String filter) {
        if (filter.isBlank()) {
            return true;
        }

        String haystack = (entry.modId() + " " + entry.modName() + " " + entry.groupId() + " " + entry.groupName() + " "
                + entry.configName() + " " + entry.displayName()).toLowerCase(Locale.ROOT);
        return haystack.contains(filter);
    }

    private boolean matchesSelectedFilters(ConfigIndexEntry entry) {
        if (this.selectedModId.isBlank() == false && entry.modId().equals(this.selectedModId) == false) {
            return false;
        }

        return this.selectedConfigGroupId.isBlank() || entry.groupId().equals(this.selectedConfigGroupId);
    }

    private boolean matchesConfigFilterMode(ConfigIndexEntry entry) {
        ConfigGroup selected = this.getSelectedGroup();
        boolean added = selected != null && isTargetInGroup(selected.items(), entry.modId(), entry.groupId(),
                entry.configName());
        return switch (this.filterMode) {
            case ALL -> true;
            case ADDED -> added;
            case MISSING -> !added;
        };
    }

    private boolean isInSelectedGroup(ConfigIndexEntry entry) {
        ConfigGroup selected = this.getSelectedGroup();
        return selected != null && isTargetInGroup(selected.items(), entry.modId(), entry.groupId(), entry.configName());
    }

    private int getSelectedGroupItemIndex(ConfigIndexEntry entry) {
        ConfigGroup selected = this.getSelectedGroup();
        return selected == null ? -1 : groupItemIndex(selected.items(), entry);
    }

    static int groupItemOrder(List<GroupItem> items, ConfigIndexEntry entry) {
        int index = groupItemIndex(items, entry);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private static int groupItemIndex(List<GroupItem> items, ConfigIndexEntry entry) {
        for (int index = 0; index < items.size(); index++) {
            GroupItem item = items.get(index);
            if (item.isSameTarget(entry.modId(), entry.groupId(), entry.configName())) {
                return index;
            }
        }
        return -1;
    }

    static boolean isTargetInGroup(List<GroupItem> items, String modId, String groupId, String configName) {
        return items.stream().anyMatch(item -> item.isSameTarget(modId, groupId, configName));
    }

    private String getSearchText() {
        return this.searchField == null ? "" : this.searchField.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private int getCurrentRowCount() {
        return switch (tab) {
            case GENERIC -> this.filteredGenericConfigs.size();
            case ALL_CONFIGS -> this.filteredConfigs.size();
        };
    }

    private int getVisibleRows() {
        int bottom = this.height - 18;
        int top = this.getListTop();
        return Math.max(1, (bottom - top) / (ROW_HEIGHT + ROW_GAP));
    }

    private boolean isInsideList(int mouseX, int mouseY) {
        int top = this.getListTop();
        int bottom = this.height - 18;
        return GuiHitTest.isInside(mouseX, mouseY, MARGIN, top, this.width - MARGIN * 2, bottom - top);
    }

    private boolean isCompactFilterLayout() {
        return this.width <= 420;
    }

    private int getListTop() {
        if (tab == ConfigGuiTab.ALL_CONFIGS) {
            return this.getGroupControlsY() + BUTTON_HEIGHT * 2 + 8;
        }
        return tab != ConfigGuiTab.GENERIC && filterControlsWrap(this.width) ? SEARCH_Y + BUTTON_HEIGHT * 2 + 8 : LIST_Y;
    }

    private int getGroupControlsY() {
        return SEARCH_Y + BUTTON_HEIGHT + 4 + (filterControlsWrap(this.width) ? BUTTON_HEIGHT + 4 : 0);
    }

    static boolean filterControlsWrap(int screenWidth) {
        return screenWidth < 290;
    }

    private int getGroupSelectorWidth() {
        return GroupActionLayout.calculate(this.width).selectorWidth();
    }

    static record GroupActionLayout(int selectorWidth, int actionX, int rightEdge) {
        static GroupActionLayout calculate(int screenWidth) {
            int actionsWidth = 120;
            int selectorWidth = Math.min(180, Math.max(80, screenWidth - MARGIN * 2 - actionsWidth - 4));
            int actionX = MARGIN + selectorWidth + 4;
            return new GroupActionLayout(selectorWidth, actionX, actionX + actionsWidth);
        }
    }

    private void selectNextTargetGroup() {
        List<ConfigGroup> groups = ConfigGroupStore.getGroups();
        if (groups.isEmpty()) {
            return;
        }
        int current = -1;
        for (int index = 0; index < groups.size(); index++) {
            if (groups.get(index).id().equals(this.selectedGroupId)) {
                current = index;
                break;
            }
        }
        this.selectedGroupId = groups.get((current + 1) % groups.size()).id();
        this.initGui();
    }

    private void normalizeSelectedGroup() {
        this.selectedGroupId = normalizedTargetGroupId(this.selectedGroupId,
                ConfigGroupStore.getGroups().stream().map(ConfigGroup::id).toList());
    }

    static String normalizedTargetGroupId(String selectedGroupId, List<String> groupIds) {
        return groupIds.contains(selectedGroupId) ? selectedGroupId
                : (groupIds.contains("default") ? "default" : "");
    }

    @Nullable
    private ConfigGroup getSelectedGroup() {
        return ConfigGroupStore.get(this.selectedGroupId).orElse(null);
    }

    private int getRowIndexAt(int mouseX, int mouseY, int rowCount) {
        if (this.isInsideList(mouseX, mouseY) == false) {
            return -1;
        }

        int visibleIndex = (mouseY - this.getListTop()) / (ROW_HEIGHT + ROW_GAP);
        int index = this.scrollOffset + visibleIndex;
        int rowY = this.getListTop() + visibleIndex * (ROW_HEIGHT + ROW_GAP);

        if (mouseY >= rowY + ROW_HEIGHT || index < 0 || index >= rowCount) {
            return -1;
        }

        return index;
    }

    private int getControlX() {
        return Math.max(MARGIN + 120, this.width - MARGIN - 184);
    }

    private String getFilterButtonText() {
        if (this.isCompactFilterLayout()) {
            return StringUtils.translate("fast-masa-config.gui.full.filter.compact");
        }
        return StringUtils.translate(this.filterMode.translationKey);
    }

    private FilterMode getNextFilterMode() {
        return this.filterMode.next();
    }

    private String getModFilterButtonText() {
        if (this.isCompactFilterLayout()) {
            return StringUtils.translate("fast-masa-config.gui.full.filter.mod.compact");
        }
        String label = this.selectedModId.isBlank()
                ? StringUtils.translate("fast-masa-config.gui.full.filter.value_all")
                : this.getSelectedModName();
        return StringUtils.translate("fast-masa-config.gui.full.filter.mod", label);
    }

    private String getGroupFilterButtonText() {
        if (this.isCompactFilterLayout()) {
            return StringUtils.translate("fast-masa-config.gui.full.filter.group.compact");
        }
        String label = this.selectedConfigGroupId.isBlank()
                ? StringUtils.translate("fast-masa-config.gui.full.filter.value_all")
                : this.getSelectedGroupName();
        return StringUtils.translate("fast-masa-config.gui.full.filter.group", label);
    }

    private void cycleModFilter() {
        List<String> modIds = ConfigIndexService.scanSupportedConfigs().stream()
                .map(entry -> entry.modId())
                .distinct()
                .toList();
        int index = modIds.indexOf(this.selectedModId);
        this.selectedModId = index < 0 ? (modIds.isEmpty() ? "" : modIds.get(0))
                : (index + 1 >= modIds.size() ? "" : modIds.get(index + 1));
        this.selectedConfigGroupId = "";
    }

    private void cycleGroupFilter() {
        List<String> groupIds = ConfigIndexService.scanSupportedConfigs().stream()
                .filter(entry -> this.selectedModId.isBlank() || entry.modId().equals(this.selectedModId))
                .map(entry -> entry.groupId())
                .filter(groupId -> groupId.isBlank() == false)
                .distinct()
                .toList();
        int index = groupIds.indexOf(this.selectedConfigGroupId);
        this.selectedConfigGroupId = index < 0 ? (groupIds.isEmpty() ? "" : groupIds.get(0))
                : (index + 1 >= groupIds.size() ? "" : groupIds.get(index + 1));
    }

    private void normalizeSelectedFilters(List<ConfigIndexEntry> index) {
        if (this.selectedModId.isBlank() == false
                && index.stream().noneMatch(entry -> entry.modId().equals(this.selectedModId))) {
            this.selectedModId = "";
            this.selectedConfigGroupId = "";
        }

        if (this.selectedConfigGroupId.isBlank() == false && index.stream()
                .filter(entry -> this.selectedModId.isBlank() || entry.modId().equals(this.selectedModId))
                .noneMatch(entry -> entry.groupId().equals(this.selectedConfigGroupId))) {
            this.selectedConfigGroupId = "";
        }
    }

    private String getSelectedModName() {
        return ConfigIndexService.scanSupportedConfigs().stream()
                .filter(entry -> entry.modId().equals(this.selectedModId))
                .map(entry -> entry.modName())
                .findFirst()
                .orElse(this.selectedModId);
    }

    private String getSelectedGroupName() {
        return ConfigIndexService.scanSupportedConfigs().stream()
                .filter(entry -> this.selectedModId.isBlank() || entry.modId().equals(this.selectedModId))
                .filter(entry -> entry.groupId().equals(this.selectedConfigGroupId))
                .map(entry -> entry.groupName())
                .findFirst()
                .orElse(this.selectedConfigGroupId);
    }

    private double getIntegerRatio(IConfigInteger config) {
        int min = config.getMinIntegerValue();
        int max = config.getMaxIntegerValue();
        return max <= min ? 0.0 : (config.getIntegerValue() - min) / (double) (max - min);
    }

    private double getDoubleRatio(IConfigDouble config) {
        double min = config.getMinDoubleValue();
        double max = config.getMaxDoubleValue();
        return max <= min ? 0.0 : (config.getDoubleValue() - min) / (max - min);
    }

    private void applyNumericSliderValue(IConfigBase config, int mouseX) {
        this.applyNumericSliderValue(config, mouseX, NumericControlLayout.calculate(this.width));
    }

    private void applyNumericSliderValue(IConfigBase config, int mouseX, NumericControlLayout layout) {
        if (config instanceof IConfigInteger integerConfig) {
            this.applyNumericSliderValue(integerConfig, mouseX, layout);
        } else if (config instanceof IConfigDouble doubleConfig) {
            this.applyNumericSliderValue(doubleConfig, mouseX, layout);
        }
    }

    private void applyNumericSliderValue(IConfigInteger config, int mouseX, NumericControlLayout layout) {
        int min = config.getMinIntegerValue();
        int max = config.getMaxIntegerValue();
        config.setIntegerValue(min + (int) Math.round(this.getSliderRatioAt(mouseX, layout) * (max - min)));
        this.notifyOwnConfigChanged(false);
    }

    private void applyNumericSliderValue(IConfigDouble config, int mouseX, NumericControlLayout layout) {
        double min = config.getMinDoubleValue();
        double max = config.getMaxDoubleValue();
        config.setDoubleValue(min + this.getSliderRatioAt(mouseX, layout) * (max - min));
        this.notifyOwnConfigChanged(false);
    }

    private double getSliderRatioAt(int mouseX, NumericControlLayout layout) {
        return clampRatio((mouseX - layout.sliderX()) / (double) layout.sliderWidth());
    }

    private String fitText(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }

        if (this.getStringWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        if (this.getStringWidth(ellipsis) > maxWidth) {
            int end = text.length();
            while (end > 0 && this.getStringWidth(text.substring(0, end)) > maxWidth) {
                end--;
            }
            return text.substring(0, end);
        }
        int end = text.length();

        while (end > 0 && this.getStringWidth(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }

        return text.substring(0, Math.max(0, end)) + ellipsis;
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static int lighten(int color) {
        int red = Math.min(255, ((color >> 16) & 0xFF) + 24);
        int green = Math.min(255, ((color >> 8) & 0xFF) + 24);
        int blue = Math.min(255, (color & 0xFF) + 24);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampRatio(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    static record NumericControlLayout(int valueX, int valueWidth, int sliderX, int sliderWidth, int resetX,
            int resetWidth) {
        static NumericControlLayout calculate(int screenWidth) {
            int valueWidth = screenWidth <= 360 ? 36 : NUMERIC_VALUE_WIDTH;
            int sliderWidth = screenWidth <= 360 ? 42 : NUMERIC_SLIDER_WIDTH;
            int resetWidth = 54;
            int totalWidth = valueWidth + 6 + sliderWidth + 6 + resetWidth;
            int rightEdge = screenWidth - MARGIN - SCROLLBAR_WIDTH;
            int valueX = Math.max(MARGIN + 96, rightEdge - totalWidth);
            int sliderX = valueX + valueWidth + 6;
            int resetX = sliderX + sliderWidth + 6;
            return new NumericControlLayout(valueX, valueWidth, sliderX, sliderWidth, resetX, resetWidth);
        }
    }

    private enum ConfigGuiTab {
        GENERIC("fast-masa-config.gui.tab.generic"),
        ALL_CONFIGS("fast-masa-config.gui.tab.all_configs");

        private final String translationKey;

        ConfigGuiTab(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getDisplayName() {
            return StringUtils.translate(this.translationKey);
        }
    }

    private enum FilterMode {
        ALL("fast-masa-config.gui.full.filter.all"),
        ADDED("fast-masa-config.gui.full.filter.added"),
        MISSING("fast-masa-config.gui.full.filter.missing");

        private final String translationKey;

        FilterMode(String translationKey) {
            this.translationKey = translationKey;
        }

        private FilterMode next() {
            FilterMode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    private static final class HotkeySettingsButton extends ButtonGeneric {
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MaLiLibReference.MOD_ID,
                "textures/gui/gui_widgets.png");
        private final IKeybind keybind;

        private HotkeySettingsButton(int x, int y, int width, int height, IKeybind keybind) {
            super(x, y, width, height, "");
            this.keybind = keybind;
            this.setRenderDefaultBackground(false);
        }

        @Override
        public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected) {
            if (this.visible == false) {
                return;
            }

            this.hovered = this.enabled && mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width
                    && mouseY < this.y + this.height;

            KeybindSettings settings = this.keybind.getSettings();
            int iconSize = 18;
            int x = this.x;
            int y = this.y;
            int edgeColor = this.keybind.areSettingsModified() ? FastMasaMenuPalette.KEYBIND_MODIFIED
                    : (this.hovered ? COLOR_TEXT : FastMasaMenuPalette.KEYBIND_DEFAULT);

            RenderUtils.drawRect(ctx, x, y, 20, 20, edgeColor);
            RenderUtils.drawRect(ctx, x + 1, y + 1, 18, 18, FastMasaMenuPalette.BLACK);
            RenderUtils.drawTexturedRect(ctx, TEXTURE, x + 1, y + 1, 0,
                    settings.getActivateOn().ordinal() * iconSize, iconSize, iconSize, 0);
            RenderUtils.drawTexturedRect(ctx, TEXTURE, x + 1, y + 1, 18,
                    settings.getAllowExtraKeys() ? 0 : iconSize, iconSize, iconSize, 0);
            RenderUtils.drawTexturedRect(ctx, TEXTURE, x + 1, y + 1, 36,
                    settings.isOrderSensitive() ? iconSize : 0, iconSize, iconSize, 0);
            RenderUtils.drawTexturedRect(ctx, TEXTURE, x + 1, y + 1, 54, settings.isExclusive() ? iconSize : 0,
                    iconSize, iconSize, 0);
            RenderUtils.drawTexturedRect(ctx, TEXTURE, x + 1, y + 1, 72, settings.shouldCancel() ? iconSize : 0,
                    iconSize, iconSize, 0);
        }
    }
}
