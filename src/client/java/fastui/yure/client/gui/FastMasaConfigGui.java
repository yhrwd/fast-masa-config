package fastui.yure.client.gui;

import fastui.yure.FastMasaConfig;
import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.client.index.ConfigIndexService;
import fastui.yure.client.input.HeldKeyInputSuppressor;
import fastui.yure.config.FastMasaConfigs;
import fastui.yure.config.ConfigGroup;
import fastui.yure.config.ConfigGroupStore;
import fastui.yure.config.GroupItem;
import fastui.yure.config.QuickMessage;
import fastui.yure.config.QuickMessageGroup;
import fastui.yure.config.QuickMessageStore;
import fi.dy.masa.malilib.MaLiLib;
import fi.dy.masa.malilib.MaLiLibConfigs;
import fi.dy.masa.malilib.MaLiLibReference;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigColor;
import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.IConfigResettable;
import fi.dy.masa.malilib.config.gui.ButtonPressDirtyListenerSimple;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiColorEditorHSV;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.GuiKeybindSettings;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ConfigButtonKeybind;
import fi.dy.masa.malilib.gui.interfaces.IConfigInfoProvider;
import fi.dy.masa.malilib.gui.interfaces.IDialogHandler;
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
import net.minecraft.client.Minecraft;
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
    private static final int MARGIN = FullConfigListLayout.MARGIN;
    private static final int ROW_HEIGHT = FullConfigListLayout.ROW_HEIGHT;
    private static final int ROW_GAP = FullConfigListLayout.ROW_GAP;
    private static final int BUTTON_HEIGHT = FullConfigPageLayout.BUTTON_HEIGHT;
    private static final int COLOR_ROW = FullConfigPalette.SURFACE_TRANSLUCENT;
    private static final int COLOR_ROW_HOVER = FullConfigPalette.ROW_HOVER_TRANSLUCENT;
    private static final int COLOR_BORDER = FullConfigPalette.BORDER;
    private static final int COLOR_ACCENT = FullConfigPalette.ACCENT;
    private static final int COLOR_TEXT = FullConfigPalette.TEXT;
    private static final int COLOR_MUTED = FullConfigPalette.MUTED;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int NUMERIC_VALUE_WIDTH = 50;
    private static final int NUMERIC_SLIDER_WIDTH = 68;

    /** 页面状态属于当前 Screen，不能在多个 GUI 实例之间共享。 */
    private ConfigGuiTab tab = ConfigGuiTab.GENERIC;

    private final HeldKeyInputSuppressor inputSuppressor;
    private final List<Runnable> hotkeyChangeListeners = new ArrayList<>();
    private final ButtonPressDirtyListenerSimple dirtyListener = new ButtonPressDirtyListenerSimple();

    private GuiTextFieldGeneric searchField;
    private GuiTextFieldGeneric groupNameField;
    private GuiTextFieldGeneric quickMessageLabelField;
    private GuiTextFieldGeneric quickMessageContentField;
    private int searchFieldWidth;
    private boolean searchFieldFocused;
    private int groupNameFieldWidth;
    private boolean groupNameFieldFocused;
    private int quickMessageLabelFieldWidth;
    private int quickMessageContentFieldWidth;
    private boolean quickMessageLabelFieldFocused;
    private boolean quickMessageContentFieldFocused;
    private ConfigButtonKeybind activeKeybindButton;
    private ConfigButtonKeybind openQuickConfigButton;
    private ButtonGeneric hotkeySettingsButton;
    private IConfigBase activeNumericSliderConfig;

    private List<IConfigBase> filteredGenericConfigs = List.of();
    private List<ConfigIndexEntry> configIndex = List.of();
    private List<ConfigIndexEntry> filteredConfigs = List.of();
    private Map<ConfigIndexService.Target, Integer> selectedGroupItemOrder = Map.of();
    private FilterMode filterMode = FilterMode.ALL;
    private String selectedModId = "";
    private String selectedConfigGroupId = "";
    private String selectedGroupId = "";
    private String selectedQuickMessageGroupId = "";
    private String editingQuickMessageId = "";
    private int scrollOffset;
    private List<QuickMessage> filteredQuickMessages = List.of();

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

    /** 在 MaLiLib 注册本模组配置页，初始化入口和切换器共用这条路径。 */
    public static void registerConfigScreen() {
        if (Registry.CONFIG_SCREEN.getModInfoFromConfigScreen(FastMasaConfigGui.class) != null) {
            return;
        }
        try {
            Registry.CONFIG_SCREEN.registerConfigScreenFactory(new ModInfo(FastMasaConfig.MOD_ID,
                    FastMasaConfig.MOD_NAME, FastMasaConfigGui::new));
        } catch (Exception | LinkageError exception) {
            MaLiLib.LOGGER.warn("FastMasaConfigGui: Failed to register [{}]", FastMasaConfig.MOD_ID, exception);
        }
    }

    static String recoveryTargetGroupId() {
        return "default";
    }

    @Override
    public void initGui() {
        super.initGui();
        this.ensureTextInputEnabled();
        ConfigGroupStore.ensureDefaultGroup();
        if (tab == ConfigGuiTab.ALL_CONFIGS) {
            this.configIndex = ConfigIndexService.scanSupportedConfigs();
            this.normalizeSelectedGroup();
        }
        if (tab == ConfigGuiTab.QUICK_MESSAGES) {
            this.normalizeSelectedQuickMessageGroup();
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
        // 全屏配置页允许用户在失焦状态下切换输入法；Minecraft 默认会在没有焦点文本框时关闭 IME。
        this.ensureTextInputEnabled();
    }

    @Override
    public boolean onMouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubleClick) {
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        this.searchFieldFocused = this.isSearchFieldHit(mouseX, mouseY);
        this.groupNameFieldFocused = this.isGroupNameFieldHit(mouseX, mouseY);
        this.quickMessageLabelFieldFocused = this.isQuickMessageLabelFieldHit(mouseX, mouseY);
        this.quickMessageContentFieldFocused = this.isQuickMessageContentFieldHit(mouseX, mouseY);

        if (super.onMouseClicked(click, doubleClick)) {
            this.ensureTextInputEnabled();
            return true;
        }

        if (this.activeKeybindButton != null) {
            this.setActiveKeybindButton(null);
            this.ensureTextInputEnabled();
            return true;
        }

        boolean handled = switch (tab) {
            case GENERIC -> this.handleGenericClick(mouseX, mouseY);
            case ALL_CONFIGS -> this.handleAllConfigsClick(mouseX, mouseY);
            case QUICK_MESSAGES -> this.handleQuickMessagesClick(mouseX, mouseY);
            case TOOLS -> this.handleGenericClick(mouseX, mouseY);
        };
        this.ensureTextInputEnabled();
        return handled;
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

        // MaLiLib 的 GuiBase 会在文本框聚焦时消费按键。左 Shift 是常用的中英文切换键，
        // 非热键录制状态下放行，但仍保留 GuiBase.keyPressed() 的输入计数和事件链。
        if (keyCode == fi.dy.masa.malilib.util.KeyCodes.KEY_LEFT_SHIFT && this.activeKeybindButton == null) {
            this.ensureTextInputEnabled();
            return false;
        }

        if (this.activeKeybindButton != null) {
            this.activeKeybindButton.onKeyPressed(keyCode);
            this.notifyOwnConfigChanged(true);
            this.ensureTextInputEnabled();
            return true;
        }

        if (keyCode == 257 && this.tab == ConfigGuiTab.QUICK_MESSAGES
                && this.quickMessageContentField != null && this.quickMessageContentField.isFocused()) {
            this.saveQuickMessage();
            this.ensureTextInputEnabled();
            return true;
        }

        boolean handled = super.onKeyTyped(input);
        this.ensureTextInputEnabled();
        return handled;
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
        Minecraft.getInstance().textInputManager().stopTextInput();
        if (this.activeKeybindButton != null) {
            this.setActiveKeybindButton(null);
        }

        if (this.dirtyListener.isDirty()) {
            this.notifyOwnConfigChanged(true);
            this.dirtyListener.resetDirty();
        }

        super.removed();
    }

    private void ensureTextInputEnabled() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() == this) {
            minecraft.textInputManager().startTextInput();
        }
    }

    @Override
    protected void drawScreenBackground(GuiContext ctx, int mouseX, int mouseY) {
        super.drawScreenBackground(ctx, mouseX, mouseY);
        RenderUtils.drawRect(ctx, 0, 0, this.width, this.height, FullConfigPalette.SCREEN_BACKGROUND);
        RenderUtils.drawRect(ctx, 0, 0, this.width, 26, FullConfigPalette.SCREEN_HEADER);
        RenderUtils.drawRect(ctx, 0, 25, this.width, 1, FullConfigPalette.BORDER);
    }

    @Override
    protected void drawTitle(GuiContext ctx, int mouseX, int mouseY, float partialTicks) {
        this.drawString(ctx, StringUtils.translate("fast-masa-config.gui.title.configs"), MARGIN, 9,
                FullConfigPalette.TEXT);
        RenderUtils.drawRect(ctx, MARGIN, 24, 40, 2, FullConfigPalette.ACCENT);
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks) {
        switch (tab) {
            case GENERIC -> this.drawGenericRows(ctx, mouseX, mouseY);
            case ALL_CONFIGS -> this.drawAllConfigRows(ctx, mouseX, mouseY);
            case QUICK_MESSAGES -> this.drawQuickMessageRows(ctx, mouseX, mouseY);
            case TOOLS -> this.drawGenericRows(ctx, mouseX, mouseY);
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
            registerConfigScreen();
            thisMod = Registry.CONFIG_SCREEN.getModInfoFromConfigScreen(this.getClass());
            if (thisMod == null) {
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
                            mc.setScreenAndShow(screenSupplier.get());
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
        FullConfigPageLayout.TabStrip layout = this.getTabStrip();
        ConfigGuiTab[] tabs = ConfigGuiTab.values();
        for (int index = 0; index < tabs.length; index++) {
            ConfigGuiTab value = tabs[index];
            ButtonGeneric button = new ButtonGeneric(layout.xFor(index), layout.yFor(index), layout.buttonWidth(index),
                    BUTTON_HEIGHT, value.getDisplayName(layout.usesCompactLabels()));
            button.setEnabled(tab != value);
            this.addButton(button, (clicked, mouseButton) -> {
                tab = value;
                this.scrollOffset = 0;
                this.initGui();
            });
        }
    }

    private void createTabInputs() {
        this.searchField = null;
        this.groupNameField = null;
        this.quickMessageLabelField = null;
        this.quickMessageContentField = null;
        this.searchFieldFocused = false;
        this.groupNameFieldFocused = false;
        this.quickMessageLabelFieldFocused = false;
        this.quickMessageContentFieldFocused = false;

        boolean compactFilters = this.isCompactFilterLayout();
        boolean supportsConfigFilters = tab == ConfigGuiTab.ALL_CONFIGS;
        boolean wrapFilters = supportsConfigFilters && filterControlsWrap(this.width);
        int filterButtonWidth = !supportsConfigFilters ? 0
                : (compactFilters ? 60 : 110);
        int modButtonWidth = !supportsConfigFilters ? 0 : (compactFilters ? 60 : 118);
        int groupButtonWidth = !supportsConfigFilters ? 0 : (compactFilters ? 60 : 118);
        int searchY = this.getSearchY();
        int searchWidth = wrapFilters ? Math.max(80, this.width - MARGIN * 2) : Math.min(220,
                Math.max(80, this.width - MARGIN * 2 - filterButtonWidth - modButtonWidth - groupButtonWidth - 18));
        this.searchField = new GuiTextFieldGeneric(MARGIN, searchY, searchWidth, 18, this.font);
        this.searchFieldWidth = searchWidth;
        this.searchField.setMaxLength(128);
        this.searchField.setSuggestion("");
        this.addTextField(this.searchField, field -> {
            this.scrollOffset = 0;
            this.refreshVisibleRows();
            return true;
        });

        if (tab == ConfigGuiTab.ALL_CONFIGS) {
            int filterY = wrapFilters ? searchY + BUTTON_HEIGHT + 4 : searchY - 1;
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
        } else if (tab == ConfigGuiTab.GENERIC) {
            this.createGenericButtons();
        }

        if (tab == ConfigGuiTab.ALL_CONFIGS) {
            this.createAllConfigsGroupControls();
        } else if (tab == ConfigGuiTab.QUICK_MESSAGES) {
            this.createQuickMessageControls();
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
            this.filteredGenericConfigs = GenericConfigPage.filter(FastMasaConfigs.Generic.OPTIONS, filter);
        } else if (tab == ConfigGuiTab.TOOLS) {
            String filter = this.getSearchText();
            this.filteredGenericConfigs = GenericConfigPage.filter(FastMasaConfigs.Tools.OPTIONS, filter);
        } else if (tab == ConfigGuiTab.ALL_CONFIGS) {
            String filter = this.getSearchText();
            this.normalizeSelectedFilters(this.configIndex);
            this.normalizeSelectedGroup();
            ConfigGroup selectedGroup = this.getSelectedGroup();
            List<GroupItem> selectedItems = selectedGroup == null ? List.of() : selectedGroup.items();
            this.selectedGroupItemOrder = AllConfigsPage.buildGroupItemOrder(selectedItems);
            this.filteredConfigs = this.configIndex.stream()
                    .filter(this::matchesSelectedFilters)
                    .filter(entry -> AllConfigsPage.matches(entry, filter))
                    .filter(this::matchesConfigFilterMode)
                    .sorted(Comparator.comparingInt(this::getSelectedGroupItemOrder))
                    .toList();
        } else if (tab == ConfigGuiTab.QUICK_MESSAGES) {
            QuickMessageGroup selected = this.getSelectedQuickMessageGroup();
            String filter = this.getSearchText();
            this.filteredQuickMessages = QuickMessagesPage.filter(selected, filter);
        }

        this.scrollOffset = clamp(this.scrollOffset, 0, Math.max(0, this.getCurrentRowCount() - this.getVisibleRows()));
    }

    private void drawGenericRows(GuiContext context, int mouseX, int mouseY) {
        int x = MARGIN;
        int width = this.width - MARGIN * 2;
        int controlX = this.getControlX();
        int total = tab == ConfigGuiTab.TOOLS ? FastMasaConfigs.Tools.OPTIONS.size()
                : FastMasaConfigs.Generic.OPTIONS.size();
        this.drawListHeader(context, this.filteredGenericConfigs.size(), total);

        if (this.filteredGenericConfigs.isEmpty()) {
            this.drawEmptyText(context, StringUtils.translate("fast-masa-config.gui.full.empty_search"));
            return;
        }

        int visible = this.getVisibleRows();
        int end = Math.min(this.filteredGenericConfigs.size(), this.scrollOffset + visible);
        this.positionOpenQuickConfigButton(-1000);

        for (int i = this.scrollOffset; i < end; i++) {
            IConfigBase config = this.filteredGenericConfigs.get(i);
            int y = this.getListTop() + (i - this.scrollOffset) * (ROW_HEIGHT + ROW_GAP);
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
            int bg = enabled ? COLOR_ACCENT : FullConfigPalette.NEUTRAL;
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
        } else if (config instanceof IConfigColor colorConfig) {
            int swatch = colorConfig.getColor().toVanillaArgb();
            boolean hovered = GuiHitTest.isInside(mouseX, mouseY, x, y, 64, BUTTON_HEIGHT);
            RenderUtils.drawRect(context, x, y, 64, BUTTON_HEIGHT, hovered ? lighten(swatch) : swatch);
            RenderUtils.drawRect(context, x, y, 64, 1, COLOR_BORDER);
            RenderUtils.drawRect(context, x, y + BUTTON_HEIGHT - 1, 64, 1, COLOR_BORDER);
            RenderUtils.drawRect(context, x, y, 1, BUTTON_HEIGHT, COLOR_BORDER);
            RenderUtils.drawRect(context, x + 63, y, 1, BUTTON_HEIGHT, COLOR_BORDER);
            this.drawString(context, colorConfig.getColor().toHexString(), x + 4, y + 6, COLOR_TEXT);
            this.drawResetButton(context, config, x + 70, y, 54, mouseX, mouseY);
        } else if (config == FastMasaConfigs.Tools.ENTITY_RENDER_ENTITIES) {
            String count = StringUtils.translate("fast-masa-config.gui.tools.entities.selected",
                    Integer.toString(FastMasaConfigs.Tools.ENTITY_RENDER_ENTITIES.getStrings().size()));
            this.drawSmallButton(context, x, y, 124, count, FullConfigPalette.CONTROL_DARK,
                    GuiHitTest.isInside(mouseX, mouseY, x, y, 124, BUTTON_HEIGHT));
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
        this.drawListHeader(context, this.filteredConfigs.size(), this.configIndex.size());

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

    private void drawQuickMessageRows(GuiContext context, int mouseX, int mouseY) {
        QuickMessageGroup selected = this.getSelectedQuickMessageGroup();
        int total = selected == null ? 0 : selected.messages().size();
        this.drawListHeader(context, this.filteredQuickMessages.size(), total);
        if (selected == null) {
            this.drawEmptyText(context, StringUtils.translate("fast-masa-config.gui.quick_messages.no_group"));
            return;
        }
        if (this.filteredQuickMessages.isEmpty()) {
            this.drawEmptyText(context, StringUtils.translate("fast-masa-config.gui.quick_messages.empty"));
            return;
        }
        int visible = this.getVisibleRows();
        int end = Math.min(this.filteredQuickMessages.size(), this.scrollOffset + visible);
        for (int index = this.scrollOffset; index < end; index++) {
            QuickMessage message = this.filteredQuickMessages.get(index);
            int x = MARGIN;
            int y = this.getListTop() + (index - this.scrollOffset) * (ROW_HEIGHT + ROW_GAP);
            int width = this.width - MARGIN * 2;
            int buttonX = x + width - 76;
            boolean selectedMessage = message.id().equals(this.editingQuickMessageId);
            boolean hovered = GuiHitTest.isInside(mouseX, mouseY, x, y, width, ROW_HEIGHT);
            this.drawRowBase(context, x, y, width, hovered, selectedMessage);
            this.drawString(context, fitText(message.displayName(), buttonX - x - 16), x + 8, y + 6, COLOR_TEXT);
            String meta = (message.isCommand() ? "[CMD] " : "") + message.content();
            this.drawString(context, fitText(meta, buttonX - x - 16), x + 8, y + 18, COLOR_MUTED);
            this.drawSmallButton(context, buttonX - 48, y + 5, 20, "↑", FullConfigPalette.CONTROL_DARK,
                    GuiHitTest.isInside(mouseX, mouseY, buttonX - 48, y + 5, 20, BUTTON_HEIGHT));
            this.drawSmallButton(context, buttonX - 24, y + 5, 20, "↓", FullConfigPalette.CONTROL_DARK,
                    GuiHitTest.isInside(mouseX, mouseY, buttonX - 24, y + 5, 20, BUTTON_HEIGHT));
            this.drawSmallButton(context, buttonX, y + 5, 64, "x", FullConfigPalette.ACTION_REMOVE,
                    GuiHitTest.isInside(mouseX, mouseY, buttonX, y + 5, 64, BUTTON_HEIGHT));
        }
        this.drawScrollBar(context, this.filteredQuickMessages.size());
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
            this.drawSmallButton(context, buttonX - 48, y + 5, 20, "↑", FullConfigPalette.CONTROL_DARK,
                    GuiHitTest.isInside(mouseX, mouseY, buttonX - 48, y + 5, 20, BUTTON_HEIGHT));
            this.drawSmallButton(context, buttonX - 24, y + 5, 20, "↓", FullConfigPalette.CONTROL_DARK,
                    GuiHitTest.isInside(mouseX, mouseY, buttonX - 24, y + 5, 20, BUTTON_HEIGHT));
        }
        this.drawSmallButton(context, buttonX, y + 5, 64, selected ? "-" : "+", selected
                        ? FullConfigPalette.ACTION_REMOVE : FullConfigPalette.ACTION_ADD,
                GuiHitTest.isInside(mouseX, mouseY, buttonX, y + 5, 64, BUTTON_HEIGHT));
    }

    private void drawListHeader(GuiContext context, int visibleCount, int totalCount) {
        String text = visibleCount + " / " + totalCount;
        this.drawString(context, text, this.width - MARGIN - this.getStringWidth(text), this.getSearchY() + 5,
                COLOR_MUTED);
    }

    private void drawSearchSuggestion(GuiContext context) {
        if (this.searchField != null && !this.searchFieldFocused && this.searchField.getValue().isBlank()) {
            this.drawString(context, StringUtils.translate("fast-masa-config.gui.full.search"), MARGIN + 4,
                    this.getSearchY() + 5, COLOR_MUTED);
        }
        if (this.groupNameField != null && !this.groupNameFieldFocused && this.groupNameField.getValue().isBlank()) {
            int y = this.getGroupNameFieldY();
            this.drawString(context, StringUtils.translate("fast-masa-config.gui.groups.name"), MARGIN + 4,
                    y + 5, COLOR_MUTED);
        }
        if (this.quickMessageLabelField != null && !this.quickMessageLabelFieldFocused
                && this.quickMessageLabelField.getValue().isBlank()) {
            this.drawString(context, StringUtils.translate("fast-masa-config.gui.quick_messages.label"), MARGIN + 4,
                    this.getQuickMessageEditorY() + 5, COLOR_MUTED);
        }
        if (this.quickMessageContentField != null && !this.quickMessageContentFieldFocused
                && this.quickMessageContentField.getValue().isBlank()) {
            this.drawString(context, StringUtils.translate("fast-masa-config.gui.quick_messages.content"), MARGIN + 4,
                    this.getQuickMessageEditorY() + BUTTON_HEIGHT + 4 + 5, COLOR_MUTED);
        }
        if (this.tab == ConfigGuiTab.QUICK_MESSAGES) {
            String variables = StringUtils.translate("fast-masa-config.gui.quick_messages.variables");
            int y = this.getQuickMessageVariablesY();
            for (String line : variables.split("\\n", -1)) {
                this.drawString(context, fitText(line, this.width - MARGIN * 2), MARGIN, y, COLOR_MUTED);
                y += this.font.lineHeight + 2;
            }
        }
    }

    private boolean isSearchFieldHit(int mouseX, int mouseY) {
        return this.searchField != null && GuiHitTest.isInside(mouseX, mouseY, MARGIN, this.getSearchY(),
                this.searchFieldWidth, 18);
    }

    private boolean isGroupNameFieldHit(int mouseX, int mouseY) {
        if (this.groupNameField == null || (tab != ConfigGuiTab.ALL_CONFIGS && tab != ConfigGuiTab.QUICK_MESSAGES)) {
            return false;
        }
        int y = this.getGroupNameFieldY();
        return GuiHitTest.isInside(mouseX, mouseY, MARGIN, y, this.groupNameFieldWidth, 18);
    }

    private boolean isQuickMessageLabelFieldHit(int mouseX, int mouseY) {
        return this.quickMessageLabelField != null && GuiHitTest.isInside(mouseX, mouseY, MARGIN,
                this.getQuickMessageEditorY(), this.quickMessageLabelFieldWidth, 18);
    }

    private boolean isQuickMessageContentFieldHit(int mouseX, int mouseY) {
        return this.quickMessageContentField != null && GuiHitTest.isInside(mouseX, mouseY, MARGIN,
                this.getQuickMessageEditorY() + BUTTON_HEIGHT + 4, this.quickMessageContentFieldWidth, 18);
    }

    private void drawEmptyText(GuiContext context, String text) {
        this.drawString(context, fitText(text, this.width - MARGIN * 2 - 16), MARGIN + 8, this.getListTop() + 12,
                COLOR_MUTED);
    }

    private void drawRowBase(GuiContext context, int x, int y, int width, boolean hovered, boolean active) {
        int background = active ? FullConfigPalette.MODULE_BACKGROUND : (hovered ? COLOR_ROW_HOVER : COLOR_ROW);
        RenderUtils.drawRect(context, x, y, width, ROW_HEIGHT, background);
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

        RenderUtils.drawRect(context, x, top, SCROLLBAR_WIDTH, height, FullConfigPalette.SCROLLBAR);
        RenderUtils.drawRect(context, x, thumbY, SCROLLBAR_WIDTH, thumbHeight, COLOR_ACCENT);
    }

    private void drawSmallButton(GuiContext context, int x, int y, int width, String text, int color,
            boolean hovered) {
        RenderUtils.drawRect(context, x, y, width, BUTTON_HEIGHT, hovered ? lighten(color) : color);
        int border = hovered ? FullConfigPalette.BORDER_HOVER : COLOR_BORDER;
        RenderUtils.drawRect(context, x, y, width, 1, border);
        RenderUtils.drawRect(context, x, y + BUTTON_HEIGHT - 1, width, 1, border);
        RenderUtils.drawRect(context, x, y, 1, BUTTON_HEIGHT, border);
        RenderUtils.drawRect(context, x + width - 1, y, 1, BUTTON_HEIGHT, border);
        int textX = x + (width - this.getStringWidth(text)) / 2;
        int textColor = color == COLOR_ACCENT || color == FullConfigPalette.ACTION_ADD
                || color == FullConfigPalette.ACTION_REMOVE ? 0xFFFFFFFF : COLOR_TEXT;
        this.drawString(context, text, textX, y + 6, textColor);
    }

    private void drawValueBox(GuiContext context, int x, int y, int width, String text) {
        RenderUtils.drawRect(context, x, y, width, BUTTON_HEIGHT, FullConfigPalette.BUTTON);
        RenderUtils.drawRect(context, x, y, width, 1, COLOR_BORDER);
        RenderUtils.drawRect(context, x, y + BUTTON_HEIGHT - 1, width, 1, COLOR_BORDER);
        RenderUtils.drawRect(context, x, y, 1, BUTTON_HEIGHT, COLOR_BORDER);
        RenderUtils.drawRect(context, x + width - 1, y, 1, BUTTON_HEIGHT, COLOR_BORDER);
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
                hovered ? FullConfigPalette.NUMERIC_TRACK_HOVER : FullConfigPalette.NUMERIC_TRACK);
        RenderUtils.drawRect(context, x, trackY, fillWidth, 3, COLOR_ACCENT);
        RenderUtils.drawRect(context, knobX, y + 3, 4, BUTTON_HEIGHT - 6, COLOR_TEXT);
    }

    private void drawResetButton(GuiContext context, IConfigBase config, int x, int y, int width, int mouseX, int mouseY) {
        boolean modified = config instanceof IConfigResettable resettable && resettable.isModified();
        this.drawSmallButton(context, x, y, width, StringUtils.translate("malilib.gui.button.reset.caps"),
                modified ? FullConfigPalette.RESET_MODIFIED : FullConfigPalette.RESET_DEFAULT,
                modified && GuiHitTest.isInside(mouseX, mouseY, x, y, width, BUTTON_HEIGHT));
    }

    private boolean handleGenericClick(int mouseX, int mouseY) {
        int controlX = this.getControlX();

        int index = this.getRowIndexAt(mouseX, mouseY, this.filteredGenericConfigs.size());

        if (index < 0) {
            return false;
        }

        IConfigBase config = this.filteredGenericConfigs.get(index);
        int y = this.getListTop() + (index - this.scrollOffset) * (ROW_HEIGHT + ROW_GAP) + 5;

        if (config instanceof IConfigBoolean booleanConfig) {
            if (GuiHitTest.isInside(mouseX, mouseY, controlX, y, 64, BUTTON_HEIGHT)) {
                booleanConfig.setBooleanValue(!booleanConfig.getBooleanValue());
                this.notifyOwnConfigChanged(false);
                if (config == FastMasaConfigs.Tools.ENTITY_RENDER_FILTER
                        || config == FastMasaConfigs.Tools.ENTITY_RENDER_WHITELIST) {
                    FastMasaConfig.LOGGER.info("实体渲染过滤配置已更新：enabled={}, whitelist={}, entities={}",
                            FastMasaConfigs.Tools.ENTITY_RENDER_FILTER.getBooleanValue(),
                            FastMasaConfigs.Tools.ENTITY_RENDER_WHITELIST.getBooleanValue(),
                            FastMasaConfigs.Tools.ENTITY_RENDER_ENTITIES.getStrings().size());
                }
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
        } else if (config instanceof IConfigColor colorConfig) {
            if (GuiHitTest.isInside(mouseX, mouseY, controlX, y, 64, BUTTON_HEIGHT)) {
                this.openColorEditor(colorConfig);
                return true;
            }
            if (this.handleResetClick(config, mouseX, mouseY, controlX + 70, y, 54)) {
                return true;
            }
        } else if (config == FastMasaConfigs.Tools.ENTITY_RENDER_ENTITIES
                && GuiHitTest.isInside(mouseX, mouseY, controlX, y, 124, BUTTON_HEIGHT)) {
            GuiBase.openGui(new EntityRenderSelectionScreen(this, FastMasaConfigs.Tools.ENTITY_RENDER_ENTITIES,
                    () -> this.notifyOwnConfigChanged(false)));
            return true;
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

    private boolean handleQuickMessagesClick(int mouseX, int mouseY) {
        if (this.handleQuickMessageGroupAction(mouseX, mouseY)) {
            return true;
        }
        int index = this.getRowIndexAt(mouseX, mouseY, this.filteredQuickMessages.size());
        if (index < 0) {
            return false;
        }
        QuickMessageGroup group = this.getSelectedQuickMessageGroup();
        QuickMessage message = this.filteredQuickMessages.get(index);
        int x = MARGIN;
        int width = this.width - MARGIN * 2;
        int rowY = this.getListTop() + (index - this.scrollOffset) * (ROW_HEIGHT + ROW_GAP) + 5;
        int buttonX = x + width - 76;
        if (GuiHitTest.isInside(mouseX, mouseY, buttonX - 48, rowY, 20, BUTTON_HEIGHT)) {
            int itemIndex = group == null ? -1 : group.messages().indexOf(message);
            if (itemIndex >= 0 && QuickMessageStore.moveMessage(group.id(), itemIndex, -1)) {
                this.afterQuickMessageChanged();
            }
            return true;
        }
        if (GuiHitTest.isInside(mouseX, mouseY, buttonX - 24, rowY, 20, BUTTON_HEIGHT)) {
            int itemIndex = group == null ? -1 : group.messages().indexOf(message);
            if (itemIndex >= 0 && QuickMessageStore.moveMessage(group.id(), itemIndex, 1)) {
                this.afterQuickMessageChanged();
            }
            return true;
        }
        if (GuiHitTest.isInside(mouseX, mouseY, buttonX, rowY, 64, BUTTON_HEIGHT)) {
            int itemIndex = group == null ? -1 : group.messages().indexOf(message);
            if (itemIndex >= 0 && QuickMessageStore.removeMessage(group.id(), itemIndex)) {
                this.clearQuickMessageEditor();
                this.afterQuickMessageChanged();
            }
            return true;
        }
        this.editingQuickMessageId = message.id();
        if (this.quickMessageLabelField != null) {
            this.quickMessageLabelField.setValue(message.label());
        }
        if (this.quickMessageContentField != null) {
            this.quickMessageContentField.setValue(message.content());
        }
        return true;
    }

    private boolean handleQuickMessageGroupAction(int mouseX, int mouseY) {
        int actionY = this.getQuickMessageControlsY();
        int selectorWidth = this.getGroupSelectorWidth();
        int actionX = MARGIN + selectorWidth + 4;
        if (GuiHitTest.isInside(mouseX, mouseY, MARGIN, actionY, selectorWidth, BUTTON_HEIGHT)) {
            this.selectNextQuickMessageGroup();
            return true;
        }
        if (GuiHitTest.isInside(mouseX, mouseY, actionX, actionY, 30, BUTTON_HEIGHT)) {
            QuickMessageGroup selected = this.getSelectedQuickMessageGroup();
            if (selected != null && QuickMessageStore.hideGroup(selected.id(), !selected.hidden())) {
                this.afterQuickMessageChanged();
                this.initGui();
            }
            return true;
        }
        if (GuiHitTest.isInside(mouseX, mouseY, actionX + 34, actionY, 30, BUTTON_HEIGHT)) {
            if (QuickMessageStore.removeGroup(this.selectedQuickMessageGroupId)) {
                this.selectedQuickMessageGroupId = "";
                this.clearQuickMessageEditor();
                this.afterQuickMessageChanged();
                this.initGui();
            }
            return true;
        }
        if (GuiHitTest.isInside(mouseX, mouseY, actionX + 68, actionY, 24, BUTTON_HEIGHT)) {
            this.moveSelectedQuickMessageGroup(-1);
            return true;
        }
        if (GuiHitTest.isInside(mouseX, mouseY, actionX + 96, actionY, 24, BUTTON_HEIGHT)) {
            this.moveSelectedQuickMessageGroup(1);
            return true;
        }
        return false;
    }

    private void openColorEditor(IConfigColor config) {
        IDialogHandler dialogHandler = new IDialogHandler() {
            @Override
            public void openDialog(fi.dy.masa.malilib.gui.GuiBase dialog) {
                GuiBase.openGui(dialog);
            }

            @Override
            public void closeDialog() {
                GuiBase.openGui(FastMasaConfigGui.this);
            }
        };
        GuiBase.openGui(new GuiColorEditorHSV(config, dialogHandler, this));
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

    private void createQuickMessageControls() {
        int controlsY = this.getQuickMessageControlsY();
        GroupActionLayout actionLayout = GroupActionLayout.calculate(this.width);
        int selectorWidth = actionLayout.selectorWidth();
        int actionX = actionLayout.actionX();
        QuickMessageGroup selected = this.getSelectedQuickMessageGroup();
        String targetLabel = StringUtils.translate("fast-masa-config.gui.quick_messages.group")
                + (selected == null ? StringUtils.translate("fast-masa-config.gui.group.none") : selected.name());
        this.addButton(new ButtonGeneric(MARGIN, controlsY, selectorWidth, BUTTON_HEIGHT, targetLabel),
                (button, mouseButton) -> this.selectNextQuickMessageGroup());
        this.addButton(new ButtonGeneric(actionX, controlsY, 30, BUTTON_HEIGHT,
                StringUtils.translate(selected != null && selected.hidden() ? "fast-masa-config.gui.group.show"
                        : "fast-masa-config.gui.group.hide")), (button, mouseButton) -> {
                    QuickMessageGroup current = this.getSelectedQuickMessageGroup();
                    if (current != null && QuickMessageStore.hideGroup(current.id(), !current.hidden())) {
                        this.afterQuickMessageChanged();
                        this.initGui();
                    }
                });
        ButtonGeneric deleteButton = new ButtonGeneric(actionX + 34, controlsY, 30, BUTTON_HEIGHT, "x");
        deleteButton.setEnabled(selected != null);
        this.addButton(deleteButton, (button, mouseButton) -> {
            QuickMessageGroup current = this.getSelectedQuickMessageGroup();
            if (current != null && QuickMessageStore.removeGroup(current.id())) {
                this.selectedQuickMessageGroupId = "";
                this.editingQuickMessageId = "";
                this.afterQuickMessageChanged();
                this.initGui();
            }
        });
        ButtonGeneric moveUp = new ButtonGeneric(actionX + 68, controlsY, 24, BUTTON_HEIGHT, "↑");
        ButtonGeneric moveDown = new ButtonGeneric(actionX + 96, controlsY, 24, BUTTON_HEIGHT, "↓");
        this.addButton(moveUp, (button, mouseButton) -> this.moveSelectedQuickMessageGroup(-1));
        this.addButton(moveDown, (button, mouseButton) -> this.moveSelectedQuickMessageGroup(1));

        int nameY = controlsY + BUTTON_HEIGHT + 4;
        int nameWidth = Math.max(60, this.width - MARGIN * 2 - 68);
        this.groupNameField = new GuiTextFieldGeneric(MARGIN, nameY, nameWidth, 18, this.font);
        this.groupNameFieldWidth = nameWidth;
        this.groupNameField.setMaxLength(128);
        this.addTextField(this.groupNameField, field -> true);
        ButtonGeneric createButton = new ButtonGeneric(MARGIN + nameWidth + 4, nameY - 1, 30, BUTTON_HEIGHT, "+");
        createButton.setHoverStrings("fast-masa-config.gui.group.create");
        this.addButton(createButton, (button, mouseButton) -> this.createQuickMessageGroup());
        ButtonGeneric renameButton = new ButtonGeneric(MARGIN + nameWidth + 38, nameY - 1, 30, BUTTON_HEIGHT, "R");
        renameButton.setHoverStrings("fast-masa-config.gui.group.rename");
        this.addButton(renameButton, (button, mouseButton) -> this.renameSelectedQuickMessageGroup());

        int editorY = this.getQuickMessageEditorY();
        // 消息内容通常比配置项名称长，编辑器始终使用整行宽度，操作按钮放在下一行。
        int editorWidth = Math.max(80, this.width - MARGIN * 2);
        this.quickMessageLabelField = new GuiTextFieldGeneric(MARGIN, editorY, editorWidth, 18, this.font);
        this.quickMessageLabelFieldWidth = editorWidth;
        this.quickMessageLabelField.setMaxLength(Integer.MAX_VALUE);
        this.addTextField(this.quickMessageLabelField, field -> true);
        this.quickMessageContentField = new GuiTextFieldGeneric(MARGIN, editorY + BUTTON_HEIGHT + 4, editorWidth, 18,
                this.font);
        this.quickMessageContentFieldWidth = editorWidth;
        this.quickMessageContentField.setMaxLength(Integer.MAX_VALUE);
        this.addTextField(this.quickMessageContentField, field -> true);
        int editorActionX = MARGIN;
        int editorActionY = this.getQuickMessageActionY();
        ButtonGeneric saveButton = new ButtonGeneric(editorActionX, editorActionY, 64, BUTTON_HEIGHT,
                StringUtils.translate("fast-masa-config.gui.quick_messages.save"));
        this.addButton(saveButton, (button, mouseButton) -> this.saveQuickMessage());
        ButtonGeneric clearButton = new ButtonGeneric(editorActionX + 68, editorActionY, 64,
                BUTTON_HEIGHT, StringUtils.translate("fast-masa-config.gui.quick_messages.clear"));
        this.addButton(clearButton, (button, mouseButton) -> this.clearQuickMessageEditor());
    }

    private void createQuickMessageGroup() {
        if (this.groupNameField == null || this.groupNameField.getValue().trim().isBlank()) {
            return;
        }
        QuickMessageGroup group = QuickMessageStore.createGroup(this.groupNameField.getValue().trim());
        this.selectedQuickMessageGroupId = group.id();
        this.groupNameField.setValue("");
        this.afterQuickMessageChanged();
        this.initGui();
    }

    private void renameSelectedQuickMessageGroup() {
        if (this.groupNameField == null || this.selectedQuickMessageGroupId.isBlank()
                || this.groupNameField.getValue().trim().isBlank()) {
            return;
        }
        if (QuickMessageStore.renameGroup(this.selectedQuickMessageGroupId, this.groupNameField.getValue().trim())) {
            this.groupNameField.setValue("");
            this.afterQuickMessageChanged();
            this.initGui();
        }
    }

    private void saveQuickMessage() {
        QuickMessageGroup group = this.getSelectedQuickMessageGroup();
        if (group == null || this.quickMessageContentField == null) {
            return;
        }
        String label = this.quickMessageLabelField == null ? "" : this.quickMessageLabelField.getValue();
        String content = this.quickMessageContentField.getValue();
        boolean saved = this.editingQuickMessageId.isBlank()
                ? QuickMessageStore.addMessage(group.id(), label, content) != null
                : QuickMessageStore.updateMessage(group.id(), this.editingQuickMessageId, label, content);
        if (saved) {
            this.clearQuickMessageEditor();
            this.afterQuickMessageChanged();
            this.initGui();
        }
    }

    private void clearQuickMessageEditor() {
        this.editingQuickMessageId = "";
        if (this.quickMessageLabelField != null) {
            this.quickMessageLabelField.setValue("");
        }
        if (this.quickMessageContentField != null) {
            this.quickMessageContentField.setValue("");
        }
    }

    private void afterQuickMessageChanged() {
        this.scrollOffset = 0;
        this.refreshVisibleRows();
        this.notifyOwnConfigChanged(false);
    }

    private void moveSelectedQuickMessageGroup(int offset) {
        if (QuickMessageStore.moveGroup(this.selectedQuickMessageGroupId, offset)) {
            this.afterQuickMessageChanged();
            this.initGui();
        }
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

    private boolean matchesSelectedFilters(ConfigIndexEntry entry) {
        if (this.selectedModId.isBlank() == false && entry.modId().equals(this.selectedModId) == false) {
            return false;
        }

        return this.selectedConfigGroupId.isBlank() || entry.groupId().equals(this.selectedConfigGroupId);
    }

    private boolean matchesConfigFilterMode(ConfigIndexEntry entry) {
        boolean added = this.selectedGroupItemOrder.containsKey(targetOf(entry));
        return switch (this.filterMode) {
            case ALL -> true;
            case ADDED -> added;
            case MISSING -> !added;
        };
    }

    private boolean isInSelectedGroup(ConfigIndexEntry entry) {
        return this.selectedGroupItemOrder.containsKey(targetOf(entry));
    }

    private int getSelectedGroupItemIndex(ConfigIndexEntry entry) {
        return this.selectedGroupItemOrder.getOrDefault(targetOf(entry), -1);
    }

    private int getSelectedGroupItemOrder(ConfigIndexEntry entry) {
        return this.selectedGroupItemOrder.getOrDefault(targetOf(entry), Integer.MAX_VALUE);
    }

    static Map<ConfigIndexService.Target, Integer> buildGroupItemOrder(List<GroupItem> items) {
        return AllConfigsPage.buildGroupItemOrder(items);
    }

    private static ConfigIndexService.Target targetOf(ConfigIndexEntry entry) {
        return AllConfigsPage.targetOf(entry);
    }

    private String getSearchText() {
        return this.searchField == null ? "" : this.searchField.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private int getCurrentRowCount() {
        return switch (tab) {
            case GENERIC, TOOLS -> this.filteredGenericConfigs.size();
            case ALL_CONFIGS -> this.filteredConfigs.size();
            case QUICK_MESSAGES -> this.filteredQuickMessages.size();
        };
    }

    private int getVisibleRows() {
        int bottom = this.height - 18;
        int top = this.getListTop();
        return FullConfigListLayout.visibleRows(this.height, top);
    }

    private boolean isInsideList(int mouseX, int mouseY) {
        return FullConfigListLayout.containsListPoint(mouseX, mouseY, this.width, this.height, this.getListTop());
    }

    private boolean isCompactFilterLayout() {
        return this.width <= 420;
    }

    private int getListTop() {
        if (tab == ConfigGuiTab.ALL_CONFIGS) {
            return this.getGroupControlsY() + BUTTON_HEIGHT * 2 + 8;
        }
        if (tab == ConfigGuiTab.QUICK_MESSAGES) {
            return this.getQuickMessageListTop();
        }
        return tab != ConfigGuiTab.GENERIC && filterControlsWrap(this.width)
                ? this.getSearchY() + BUTTON_HEIGHT * 2 + 8 : this.getSearchY() + BUTTON_HEIGHT + 6;
    }

    private int getGroupControlsY() {
        return this.getSearchY() + BUTTON_HEIGHT + 4 + (filterControlsWrap(this.width) ? BUTTON_HEIGHT + 4 : 0);
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
            int availableWidth = Math.max(0, screenWidth - MARGIN * 2);
            int selectorWidth = Math.min(180, Math.max(0, availableWidth - actionsWidth - 4));
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

    private void selectNextQuickMessageGroup() {
        List<QuickMessageGroup> groups = QuickMessageStore.getGroups();
        if (groups.isEmpty()) {
            return;
        }
        int current = -1;
        for (int index = 0; index < groups.size(); index++) {
            if (groups.get(index).id().equals(this.selectedQuickMessageGroupId)) {
                current = index;
                break;
            }
        }
        this.selectedQuickMessageGroupId = groups.get((current + 1) % groups.size()).id();
        this.editingQuickMessageId = "";
        this.initGui();
    }

    private void normalizeSelectedGroup() {
        this.selectedGroupId = normalizedTargetGroupId(this.selectedGroupId,
                ConfigGroupStore.getGroups().stream().map(ConfigGroup::id).toList());
    }

    private void normalizeSelectedQuickMessageGroup() {
        List<String> groupIds = QuickMessageStore.getGroups().stream().map(QuickMessageGroup::id).toList();
        this.selectedQuickMessageGroupId = groupIds.contains(this.selectedQuickMessageGroupId)
                ? this.selectedQuickMessageGroupId : (groupIds.isEmpty() ? "" : groupIds.getFirst());
    }

    static String normalizedTargetGroupId(String selectedGroupId, List<String> groupIds) {
        return groupIds.contains(selectedGroupId) ? selectedGroupId
                : (groupIds.contains("default") ? "default" : "");
    }

    @Nullable
    private ConfigGroup getSelectedGroup() {
        return ConfigGroupStore.get(this.selectedGroupId).orElse(null);
    }

    @Nullable
    private QuickMessageGroup getSelectedQuickMessageGroup() {
        return QuickMessageStore.get(this.selectedQuickMessageGroupId).orElse(null);
    }

    private int getQuickMessageControlsY() {
        return this.getSearchY() + BUTTON_HEIGHT + 4;
    }

    private int getGroupNameFieldY() {
        return (tab == ConfigGuiTab.QUICK_MESSAGES ? this.getQuickMessageControlsY() : this.getGroupControlsY())
                + BUTTON_HEIGHT + 4;
    }

    private int getQuickMessageEditorY() {
        return this.getQuickMessageControlsY() + BUTTON_HEIGHT * 2 + 8;
    }

    private int getQuickMessageListTop() {
        return this.getQuickMessageActionY() + BUTTON_HEIGHT + 10;
    }

    private int getQuickMessageVariablesY() {
        return this.getQuickMessageEditorY() + BUTTON_HEIGHT * 2 + 10;
    }

    private int getQuickMessageActionY() {
        int lineCount = StringUtils.translate("fast-masa-config.gui.quick_messages.variables").split("\\n", -1).length;
        return this.getQuickMessageVariablesY() + lineCount * (this.font.lineHeight + 2) + 14;
    }

    private int getRowIndexAt(int mouseX, int mouseY, int rowCount) {
        return FullConfigListLayout.rowIndexAt(mouseX, mouseY, this.width, this.height, this.getListTop(),
                this.scrollOffset, rowCount);
    }

    private int getControlX() {
        return Math.max(MARGIN + 120, this.width - MARGIN - 184);
    }

    private FullConfigPageLayout.TabStrip getTabStrip() {
        ConfigGuiTab[] tabs = ConfigGuiTab.values();
        int availableWidth = this.width - MARGIN * 2;
        int[] fullWidths = this.getTabWidths(tabs, false);
        boolean compact = this.totalTabWidth(fullWidths) > availableWidth;
        return FullConfigPageLayout.calculateTabStrip(this.width, this.getTabWidths(tabs, compact), compact);
    }

    private int[] getTabWidths(ConfigGuiTab[] tabs, boolean compact) {
        int[] widths = new int[tabs.length];
        for (int index = 0; index < tabs.length; index++) {
            widths[index] = Math.max(48, this.getStringWidth(tabs[index].getDisplayName(compact)) + 18);
        }
        return widths;
    }

    private int totalTabWidth(int[] widths) {
        int total = Math.max(0, (widths.length - 1) * FullConfigPageLayout.GAP);
        for (int width : widths) {
            total += width;
        }
        return total;
    }

    private int getSearchY() {
        return this.getTabStrip().contentTop();
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
        List<String> modIds = this.configIndex.stream()
                .map(entry -> entry.modId())
                .distinct()
                .toList();
        int index = modIds.indexOf(this.selectedModId);
        this.selectedModId = index < 0 ? (modIds.isEmpty() ? "" : modIds.get(0))
                : (index + 1 >= modIds.size() ? "" : modIds.get(index + 1));
        this.selectedConfigGroupId = "";
    }

    private void cycleGroupFilter() {
        List<String> groupIds = this.configIndex.stream()
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
        return this.configIndex.stream()
                .filter(entry -> entry.modId().equals(this.selectedModId))
                .map(entry -> entry.modName())
                .findFirst()
                .orElse(this.selectedModId);
    }

    private String getSelectedGroupName() {
        return this.configIndex.stream()
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
        return FloatingGroupPanel.fitText(text, maxWidth, this::getStringWidth);
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
        ALL_CONFIGS("fast-masa-config.gui.tab.all_configs"),
        QUICK_MESSAGES("fast-masa-config.gui.tab.quick_messages"),
        TOOLS("fast-masa-config.gui.tab.tools");

        private final String translationKey;

        ConfigGuiTab(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getDisplayName(boolean compact) {
            return StringUtils.translate(compact ? this.translationKey + ".compact" : this.translationKey);
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
            int edgeColor = this.keybind.areSettingsModified() ? FullConfigPalette.KEYBIND_MODIFIED
                    : (this.hovered ? COLOR_TEXT : FullConfigPalette.KEYBIND_DEFAULT);

            RenderUtils.drawRect(ctx, x, y, 20, 20, edgeColor);
            RenderUtils.drawRect(ctx, x + 1, y + 1, 18, 18, FullConfigPalette.BLACK);
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
