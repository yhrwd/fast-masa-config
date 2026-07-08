package fastui.yure.client.gui;

import fastui.yure.FastMasaConfig;
import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.client.index.ConfigIndexService;
import fastui.yure.client.input.HeldKeyInputSuppressor;
import fastui.yure.client.shortcut.ShortcutResolver;
import fastui.yure.config.FastMasaConfigs;
import fastui.yure.config.ShortcutConfigStore;
import fastui.yure.config.ShortcutControlType;
import fastui.yure.config.ShortcutEntry;
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
import java.util.List;
import java.util.Locale;
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
    private static final int COLOR_ROW = 0xA0201820;
    private static final int COLOR_ROW_HOVER = 0xC02A1D25;
    private static final int COLOR_BORDER = 0xFF6A344B;
    private static final int COLOR_ACCENT = 0xFFE6397C;
    private static final int COLOR_TEXT = 0xFFFFEAF2;
    private static final int COLOR_MUTED = 0xFFCFA4B7;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int NUMERIC_VALUE_WIDTH = 50;
    private static final int NUMERIC_SLIDER_X_OFFSET = 56;
    private static final int NUMERIC_SLIDER_WIDTH = 68;
    private static final int NUMERIC_RESET_X_OFFSET = 130;
    private static final int STATUS_MIN_X = MARGIN + 132;
    private static final int STATUS_RIGHT_RESERVED = 258;
    private static final int STATUS_Y = 10;

    private static ConfigGuiTab tab = ConfigGuiTab.GENERIC;

    private final HeldKeyInputSuppressor inputSuppressor;
    private final List<Runnable> hotkeyChangeListeners = new ArrayList<>();
    private final ButtonPressDirtyListenerSimple dirtyListener = new ButtonPressDirtyListenerSimple();

    private GuiTextFieldGeneric searchField;
    private GuiTextFieldGeneric manualIdField;
    private ConfigButtonKeybind activeKeybindButton;
    private ConfigButtonKeybind openQuickConfigButton;
    private ButtonGeneric hotkeySettingsButton;
    private IConfigBase activeNumericSliderConfig;

    private List<IConfigBase> filteredGenericConfigs = List.of();
    private List<ConfigIndexEntry> filteredConfigs = List.of();
    private List<ShortcutView> shortcutViews = List.of();
    private FilterMode filterMode = FilterMode.ALL;
    private String selectedModId = "";
    private String selectedGroupId = "";
    private int scrollOffset;
    private String statusText = "";
    private int statusTicks;

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
        super();
        this.setParent(parent);
        this.setTitle(StringUtils.translate("fast-masa-config.gui.title.configs"));
        this.inputSuppressor = new HeldKeyInputSuppressor(suppressKeys);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.clearOptions();
        this.buildConfigSwitcher();
        this.createTabButtons();
        this.createTabInputs();
        this.refreshVisibleRows();
    }

    @Override
    public void render(GuiContext GuiContext, int mouseX, int mouseY, float partialTicks) {
        if (this.guiContext == null || this.guiContext.equals(GuiContext) == false) {
            this.guiContext = guiContext;
        }

        GuiContext.createNewRootLayer();
        this.drawScreenBackground(GuiContext, mouseX, mouseY);
        this.drawTitle(GuiContext, mouseX, mouseY, partialTicks);
        this.drawContents(GuiContext, mouseX, mouseY, partialTicks);
        this.drawButtons(GuiContext, mouseX, mouseY, partialTicks);
        this.drawTextFields(GuiContext, mouseX, mouseY);
        this.drawSearchPlaceholder(GuiContext);
        this.drawWidgets(GuiContext, mouseX, mouseY);
        this.drawHoveredWidget(GuiContext, mouseX, mouseY);
        this.drawButtonHoverTexts(GuiContext, mouseX, mouseY, partialTicks);
        this.drawGuiMessages(GuiContext);
    }

    @Override
    public void tick() {
        if (this.statusTicks > 0) {
            this.statusTicks--;
        }
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (super.onMouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }

        if (this.activeKeybindButton != null) {
            this.setActiveKeybindButton(null);
            return true;
        }

        return switch (tab) {
            case GENERIC -> this.handleGenericClick(mouseX, mouseY);
            case SHORTCUTS -> this.handleShortcutClick(mouseX, mouseY);
            case ALL_CONFIGS -> this.handleAllConfigsClick(mouseX, mouseY);
        };
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.activeNumericSliderConfig != null) {
            this.applyNumericSliderValue(this.activeNumericSliderConfig, (int) mouseX);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean onMouseReleased(int mouseX, int mouseY, int mouseButton) {
        this.activeNumericSliderConfig = null;
        return super.onMouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onMouseScrolled(int mouseX, int mouseY, double horizontalAmount, double verticalAmount) {
        if (super.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }

        if (this.isInsideList(mouseX, mouseY)) {
            int previous = this.scrollOffset;
            this.scrollOffset = clamp(this.scrollOffset + (verticalAmount < 0 ? 1 : -1), 0,
                    Math.max(0, this.getCurrentRowCount() - this.getVisibleRows()));
            return previous != this.scrollOffset;
        }

        return false;
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers) {
        if (this.inputSuppressor.shouldSuppressKey(keyCode)) {
            return true;
        }

        if (this.activeKeybindButton != null) {
            this.activeKeybindButton.onKeyPressed(keyCode);
            this.notifyOwnConfigChanged(true);
            return true;
        }

        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean onCharTyped(char charIn, int modifiers) {
        if (this.inputSuppressor.shouldSuppressChar()) {
            return true;
        }

        return super.onCharTyped(charIn, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        this.inputSuppressor.release(keyCode);
        return super.keyReleased(keyCode, scanCode, modifiers);
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
    protected void drawScreenBackground(GuiContext GuiContext, int mouseX, int mouseY) {
        super.drawScreenBackground(GuiContext, mouseX, mouseY);
    }

    @Override
    protected void drawTitle(GuiContext GuiContext, int mouseX, int mouseY, float partialTicks) {
        this.text(GuiContext, this.getTitleString(), MARGIN, 10, COLOR_TEXT);
        this.text(GuiContext, StringUtils.translate("fast-masa-config.gui.full.switch_mod"), this.width - 246,
                10, COLOR_MUTED);
    }

    @Override
    protected void drawContents(GuiContext GuiContext, int mouseX, int mouseY, float partialTicks) {
        this.drawStatus(GuiContext);

        switch (tab) {
            case GENERIC -> this.drawGenericRows(GuiContext, mouseX, mouseY);
            case SHORTCUTS -> this.drawShortcutRows(GuiContext, mouseX, mouseY);
            case ALL_CONFIGS -> this.drawAllConfigRows(GuiContext, mouseX, mouseY);
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
                        var screenSupplier = currentSelection.getConfigScreenSupplier();
                        if (screenSupplier != null) {
                            client.setScreen(screenSupplier.get());
                        }
                    }
                }

                @Override
                protected String getDisplayString(ModInfo entry) {
                    return entry.getModName();
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
            x += button.width() + 4;
        }
    }

    private void createTabInputs() {
        this.searchField = null;
        this.manualIdField = null;

        int filterButtonWidth = tab == ConfigGuiTab.GENERIC ? 0 : 110;
        int modButtonWidth = tab == ConfigGuiTab.GENERIC ? 0 : 118;
        int groupButtonWidth = tab == ConfigGuiTab.GENERIC ? 0 : 118;
        int searchWidth = Math.min(220,
                Math.max(80, this.width - MARGIN * 2 - filterButtonWidth - modButtonWidth - groupButtonWidth - 18));
        this.searchField = new GuiTextFieldGeneric(MARGIN, SEARCH_Y, searchWidth, 18, this.textRenderer);
        this.searchField.setMaxLength(128);
        this.addTextField(this.searchField, field -> {
            this.scrollOffset = 0;
            this.refreshVisibleRows();
            return true;
        });

        if (tab != ConfigGuiTab.GENERIC) {
            this.addButton(new ButtonGeneric(MARGIN + searchWidth + 6, SEARCH_Y - 1, filterButtonWidth, BUTTON_HEIGHT,
                    this.getFilterButtonText()), (button, mouseButton) -> {
                        this.filterMode = this.getNextFilterMode();
                        this.scrollOffset = 0;
                        this.initGui();
                    });
            this.addButton(new ButtonGeneric(MARGIN + searchWidth + filterButtonWidth + 12, SEARCH_Y - 1,
                    modButtonWidth, BUTTON_HEIGHT, this.getModFilterButtonText()), (button, mouseButton) -> {
                        this.cycleModFilter();
                        this.scrollOffset = 0;
                        this.initGui();
                    });
            this.addButton(
                    new ButtonGeneric(MARGIN + searchWidth + filterButtonWidth + modButtonWidth + 18, SEARCH_Y - 1,
                            groupButtonWidth, BUTTON_HEIGHT, this.getGroupFilterButtonText()),
                    (button, mouseButton) -> {
                        this.cycleGroupFilter();
                        this.scrollOffset = 0;
                        this.initGui();
                    });
        } else {
            this.createGenericButtons();
        }

        if (tab == ConfigGuiTab.SHORTCUTS) {
            int inputY = this.height - 30;
            int inputWidth = Math.max(80, this.width - MARGIN * 2 - 72);
            this.manualIdField = new GuiTextFieldGeneric(MARGIN, inputY, inputWidth, 18, this.textRenderer);
            this.manualIdField.setMaxLength(256);
            this.manualIdField.setSuggestion("modId/groupId/configName");
            this.addTextField(this.manualIdField, field -> true);
            this.addButton(new ButtonGeneric(MARGIN + inputWidth + 6, inputY - 1, 58, BUTTON_HEIGHT, "+"),
                    (button, mouseButton) -> this.addManualShortcut());
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
            this.filteredConfigs = index.stream()
                    .filter(this::matchesSelectedFilters)
                    .filter(entry -> this.matchesConfig(entry, filter))
                    .filter(this::matchesConfigFilterMode)
                    .toList();
        } else if (tab == ConfigGuiTab.SHORTCUTS) {
            String filter = this.getSearchText();
            List<ConfigIndexEntry> index = ConfigIndexService.scanSupportedConfigs();
            this.normalizeSelectedFilters(index);
            List<ShortcutView> views = new ArrayList<>();
            List<ShortcutEntry> shortcuts = ShortcutConfigStore.getEntries();

            for (int i = 0; i < shortcuts.size(); i++) {
                ShortcutEntry shortcut = shortcuts.get(i);
                ConfigIndexEntry config = ShortcutResolver.find(index, shortcut).orElse(null);
                ShortcutView view = new ShortcutView(i, shortcut, config);

                if (this.matchesSelectedFilters(view) && this.matchesShortcut(view, filter)
                        && this.matchesShortcutFilterMode(view)) {
                    views.add(view);
                }
            }

            this.shortcutViews = List.copyOf(views);
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
            this.drawRowBase(context, x, y, width, hovered);
            this.text(context, fitText(config.getConfigGuiDisplayName(), controlX - x - 24), x + 8, y + 6,
                    COLOR_TEXT);
            this.text(context, fitText(config.getComment() == null ? "" : config.getComment(), controlX - x - 24),
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
            int bg = enabled ? 0xFF256D45 : 0xFF4A2A2A;
            this.drawSmallButton(context, x, y, 64, enabled ? "ON" : "OFF", bg,
                    GuiHitTest.isInside(mouseX, mouseY, x, y, 64, BUTTON_HEIGHT));
            this.drawResetButton(context, config, x + 70, y, mouseX, mouseY);
        } else if (config instanceof IConfigInteger integerConfig) {
            this.drawNumericControl(context, config, x, y, mouseX, mouseY, integerConfig.getStringValue(),
                    this.getIntegerRatio(integerConfig));
        } else if (config instanceof IConfigDouble doubleConfig) {
            this.drawNumericControl(context, config, x, y, mouseX, mouseY, formatDouble(doubleConfig.getDoubleValue()),
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
                    .setX(this.openQuickConfigButton.getX() + this.openQuickConfigButton.width() + 4);
            this.hotkeySettingsButton.setY(y);
            this.hotkeySettingsButton.setEnabled(y >= 0);
        }
    }

    private void drawShortcutRows(GuiContext context, int mouseX, int mouseY) {
        this.drawListHeader(context, this.shortcutViews.size(), ShortcutConfigStore.getEntries().size());

        if (this.shortcutViews.isEmpty()) {
            this.drawEmptyText(context,
                    StringUtils.translate(
                            ShortcutConfigStore.getEntries().isEmpty() ? "fast-masa-config.gui.full.empty_shortcuts"
                                    : "fast-masa-config.gui.full.empty_search"));
            return;
        }

        int visible = this.getVisibleRows();
        int end = Math.min(this.shortcutViews.size(), this.scrollOffset + visible);

        for (int i = this.scrollOffset; i < end; i++) {
            this.drawShortcutRow(context, this.shortcutViews.get(i), i - this.scrollOffset, mouseX, mouseY);
        }

        this.drawScrollBar(context, this.shortcutViews.size());
    }

    private void drawShortcutRow(GuiContext context, ShortcutView view, int visibleIndex, int mouseX, int mouseY) {
        int x = MARGIN;
        int y = LIST_Y + visibleIndex * (ROW_HEIGHT + ROW_GAP);
        int width = this.width - MARGIN * 2;
        boolean hovered = GuiHitTest.isInside(mouseX, mouseY, x, y, width, ROW_HEIGHT);
        String label = view.config == null ? view.shortcut.manualId() : view.config.displayName();
        String meta = view.config == null ? StringUtils.translate("fast-masa-config.gui.full.status.not_found")
                : view.config.modName() + " / " + view.config.groupName() + " / " + view.shortcut.manualId();
        int buttonsX = x + width - 102;

        this.drawRowBase(context, x, y, width, hovered);
        this.text(context, fitText(label, buttonsX - x - 16), x + 8, y + 6, COLOR_TEXT);
        this.text(context, fitText(meta, buttonsX - x - 16), x + 8, y + 18, COLOR_MUTED);
        this.drawSmallButton(context, buttonsX, y + 5, 24, "↑", 0xFF303030,
                GuiHitTest.isInside(mouseX, mouseY, buttonsX, y + 5, 24, BUTTON_HEIGHT));
        this.drawSmallButton(context, buttonsX + 28, y + 5, 24, "↓", 0xFF303030,
                GuiHitTest.isInside(mouseX, mouseY, buttonsX + 28, y + 5, 24, BUTTON_HEIGHT));
        this.drawSmallButton(context, buttonsX + 56, y + 5, 42, "-", 0xFF5A2525,
                GuiHitTest.isInside(mouseX, mouseY, buttonsX + 56, y + 5, 42, BUTTON_HEIGHT));
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
        int y = LIST_Y + visibleIndex * (ROW_HEIGHT + ROW_GAP);
        int width = this.width - MARGIN * 2;
        int buttonX = x + width - 68;
        boolean selected = ShortcutConfigStore.containsTarget(entry.modId(), entry.groupId(), entry.configName());
        boolean hovered = GuiHitTest.isInside(mouseX, mouseY, x, y, width, ROW_HEIGHT);
        String meta = entry.modName() + " / " + entry.groupName() + " / " + entry.manualId();

        this.drawRowBase(context, x, y, width, hovered);
        this.text(context, fitText(entry.displayName(), buttonX - x - 16), x + 8, y + 6, COLOR_TEXT);
        this.text(context, fitText(meta, buttonX - x - 16), x + 8, y + 18, COLOR_MUTED);
        this.drawSmallButton(context, buttonX, y + 5, 64, selected ? "-" : "+", selected ? 0xFF5A2525 : 0xFF303030,
                GuiHitTest.isInside(mouseX, mouseY, buttonX, y + 5, 64, BUTTON_HEIGHT));
    }

    private void drawListHeader(GuiContext context, int visibleCount, int totalCount) {
        String text = visibleCount + " / " + totalCount;
        this.text(context, text, this.width - MARGIN - this.getStringWidth(text), SEARCH_Y + 5, COLOR_MUTED);
    }

    private void drawStatus(GuiContext context) {
        if (this.statusTicks > 0 && this.statusText.isBlank() == false) {
            StatusToastPlacement placement = getStatusToastPlacement(this.width, this.getStringWidth(this.statusText));
            String text = fitText(this.statusText, placement.textWidth());
            int boxX = placement.x() - 6;
            int boxWidth = this.getStringWidth(text) + 12;

            RenderUtils.drawRect(context, boxX, placement.y() - 3, boxWidth, 15, 0xD0181118);
            RenderUtils.drawRect(context, boxX, placement.y() - 3, 2, 15, COLOR_ACCENT);
            this.text(context, text, placement.x(), placement.y(), COLOR_TEXT);
        }
    }

    private void drawSearchPlaceholder(GuiContext context) {
        if (this.searchField != null && this.searchField.getValue().isBlank() && this.searchField.isFocused() == false) {
            this.text(context, StringUtils.translate("fast-masa-config.gui.full.search"),
                    this.searchField.getX() + 4, this.searchField.getY() + 5, 0xFF777777);
        }
    }

    private void drawEmptyText(GuiContext context, String text) {
        this.text(context, fitText(text, this.width - MARGIN * 2 - 16), MARGIN + 8, LIST_Y + 12, COLOR_MUTED);
    }

    private void drawRowBase(GuiContext context, int x, int y, int width, boolean hovered) {
        RenderUtils.drawRect(context, x, y, width, ROW_HEIGHT, hovered ? COLOR_ROW_HOVER : COLOR_ROW);
        RenderUtils.drawRect(context, x, y, 2, ROW_HEIGHT, hovered ? COLOR_ACCENT : COLOR_BORDER);
    }

    private void drawScrollBar(GuiContext context, int rowCount) {
        int visibleRows = this.getVisibleRows();

        if (rowCount <= visibleRows) {
            return;
        }

        int top = LIST_Y;
        int bottom = tab == ConfigGuiTab.SHORTCUTS ? this.height - 40 : this.height - 18;
        int height = Math.max(1, bottom - top);
        int thumbHeight = Math.max(16, height * visibleRows / rowCount);
        int maxOffset = Math.max(1, rowCount - visibleRows);
        int thumbY = top + (height - thumbHeight) * this.scrollOffset / maxOffset;
        int x = this.width - MARGIN - SCROLLBAR_WIDTH;

        RenderUtils.drawRect(context, x, top, SCROLLBAR_WIDTH, height, 0x552C2C2C);
        RenderUtils.drawRect(context, x, thumbY, SCROLLBAR_WIDTH, thumbHeight, COLOR_ACCENT);
    }

    private void drawSmallButton(GuiContext context, int x, int y, int width, String text, int color,
            boolean hovered) {
        RenderUtils.drawRect(context, x, y, width, BUTTON_HEIGHT, hovered ? lighten(color) : color);
        RenderUtils.drawRect(context, x, y, width, 1, COLOR_BORDER);
        int textX = x + (width - this.getStringWidth(text)) / 2;
        this.text(context, text, textX, y + 6, COLOR_TEXT);
    }

    private void drawValueBox(GuiContext context, int x, int y, int width, String text) {
        RenderUtils.drawRect(context, x, y, width, BUTTON_HEIGHT, 0xFF161616);
        this.text(context, fitText(text, width - 8), x + 4, y + 6, COLOR_TEXT);
    }

    private void drawNumericControl(GuiContext context, IConfigBase config, int x, int y, int mouseX, int mouseY,
            String valueText, double ratio) {
        int sliderX = x + NUMERIC_SLIDER_X_OFFSET;
        this.drawValueBox(context, x, y, NUMERIC_VALUE_WIDTH, valueText);
        this.drawNumericSlider(context, sliderX, y, NUMERIC_SLIDER_WIDTH, ratio,
                GuiHitTest.isInside(mouseX, mouseY, sliderX, y, NUMERIC_SLIDER_WIDTH, BUTTON_HEIGHT));
        this.drawResetButton(context, config, x + NUMERIC_RESET_X_OFFSET, y, mouseX, mouseY);
    }

    private void drawNumericSlider(GuiContext context, int x, int y, int width, double ratio, boolean hovered) {
        int trackY = y + BUTTON_HEIGHT / 2 - 1;
        int fillWidth = (int) Math.round(width * clampRatio(ratio));
        int knobX = x + Math.max(0, fillWidth - 2);

        RenderUtils.drawRect(context, x, trackY, width, 3, hovered ? 0xFF404040 : 0xFF2A2A2A);
        RenderUtils.drawRect(context, x, trackY, fillWidth, 3, COLOR_ACCENT);
        RenderUtils.drawRect(context, knobX, y + 3, 4, BUTTON_HEIGHT - 6, COLOR_TEXT);
    }

    private void drawResetButton(GuiContext context, IConfigBase config, int x, int y, int mouseX, int mouseY) {
        boolean modified = config instanceof IConfigResettable resettable && resettable.isModified();
        this.drawSmallButton(context, x, y, 54, StringUtils.translate("malilib.gui.button.reset.caps"),
                modified ? 0xFF303030 : 0xFF202020,
                modified && GuiHitTest.isInside(mouseX, mouseY, x, y, 54, BUTTON_HEIGHT));
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

            if (this.handleResetClick(config, mouseX, mouseY, controlX + 70, y)) {
                return true;
            }
        } else if (config instanceof IConfigInteger || config instanceof IConfigDouble) {
            if (this.handleNumericSliderClick(config, mouseX, mouseY, controlX, y)) {
                return true;
            }

            if (this.handleResetClick(config, mouseX, mouseY, controlX + NUMERIC_RESET_X_OFFSET, y)) {
                return true;
            }
        }

        return false;
    }

    private boolean handleNumericSliderClick(IConfigBase config, int mouseX, int mouseY, int controlX, int y) {
        if (GuiHitTest.isInside(mouseX, mouseY, controlX + NUMERIC_SLIDER_X_OFFSET, y, NUMERIC_SLIDER_WIDTH,
                BUTTON_HEIGHT) == false) {
            return false;
        }

        this.activeNumericSliderConfig = config;
        this.applyNumericSliderValue(config, mouseX);
        return true;
    }

    private boolean handleShortcutClick(int mouseX, int mouseY) {
        int index = this.getRowIndexAt(mouseX, mouseY, this.shortcutViews.size());

        if (index < 0) {
            return false;
        }

        ShortcutView view = this.shortcutViews.get(index);
        int x = MARGIN;
        int width = this.width - MARGIN * 2;
        int rowY = LIST_Y + (index - this.scrollOffset) * (ROW_HEIGHT + ROW_GAP) + 5;
        int buttonsX = x + width - 102;

        if (GuiHitTest.isInside(mouseX, mouseY, buttonsX, rowY, 24, BUTTON_HEIGHT)) {
            this.afterMoveShortcut(ShortcutConfigStore.move(view.storeIndex, -1));
            return true;
        }

        if (GuiHitTest.isInside(mouseX, mouseY, buttonsX + 28, rowY, 24, BUTTON_HEIGHT)) {
            this.afterMoveShortcut(ShortcutConfigStore.move(view.storeIndex, 1));
            return true;
        }

        if (GuiHitTest.isInside(mouseX, mouseY, buttonsX + 56, rowY, 42, BUTTON_HEIGHT)) {
            ShortcutConfigStore.removeTarget(view.shortcut.modId(), view.shortcut.groupId(),
                    view.shortcut.configName());
            this.afterShortcutChanged("fast-masa-config.gui.full.status.removed");
            return true;
        }

        return false;
    }

    private boolean handleAllConfigsClick(int mouseX, int mouseY) {
        int index = this.getRowIndexAt(mouseX, mouseY, this.filteredConfigs.size());

        if (index < 0) {
            return false;
        }

        ConfigIndexEntry entry = this.filteredConfigs.get(index);
        int x = MARGIN;
        int width = this.width - MARGIN * 2;
        int rowY = LIST_Y + (index - this.scrollOffset) * (ROW_HEIGHT + ROW_GAP) + 5;
        int buttonX = x + width - 68;

        if (GuiHitTest.isInside(mouseX, mouseY, buttonX, rowY, 64, BUTTON_HEIGHT)) {
            if (ShortcutConfigStore.containsTarget(entry.modId(), entry.groupId(), entry.configName())) {
                ShortcutConfigStore.removeTarget(entry.modId(), entry.groupId(), entry.configName());
                this.afterShortcutChanged("fast-masa-config.gui.full.status.removed");
            } else {
                if (this.addShortcut(entry)) {
                    this.afterShortcutChanged("fast-masa-config.gui.full.status.added");
                } else {
                    this.setStatus("fast-masa-config.gui.full.status.duplicate");
                }
            }

            return true;
        }

        return false;
    }

    private void addManualShortcut() {
        if (this.manualIdField == null) {
            return;
        }

        String manualId = this.manualIdField.getValue().trim();

        if (manualId.isBlank()) {
            this.setStatus("fast-masa-config.gui.full.status.empty_input");
            return;
        }

        try {
            ShortcutEntry candidate = ShortcutEntry.fromManualId(manualId);
            ConfigIndexEntry entry = ShortcutResolver.find(ConfigIndexService.scanSupportedConfigs(), candidate)
                    .orElse(null);

            if (entry == null) {
                this.setStatus("fast-masa-config.gui.full.status.not_found");
                return;
            }

            if (this.addShortcut(entry)) {
                this.manualIdField.setValue("");
                this.afterShortcutChanged("fast-masa-config.gui.full.status.added");
            } else {
                this.setStatus("fast-masa-config.gui.full.status.duplicate");
            }
        } catch (IllegalArgumentException ignored) {
            this.setStatus("fast-masa-config.gui.full.status.not_found");
        }
    }

    private boolean addShortcut(ConfigIndexEntry entry) {
        return ShortcutConfigStore.add(new ShortcutEntry(
                entry.modId(),
                entry.groupId(),
                entry.configName(),
                "",
                entry.config().getType() == ConfigType.BOOLEAN ? ShortcutControlType.TOGGLE
                        : ShortcutControlType.SLIDER,
                entry.config().getType() == ConfigType.INTEGER ? 1.0 : 0.05,
                null,
                null));
    }

    private void afterMoveShortcut(boolean moved) {
        if (moved) {
            this.afterShortcutChanged("fast-masa-config.gui.full.status.moved");
        } else {
            this.setStatus("fast-masa-config.gui.full.status.move_blocked");
        }
    }

    private void afterShortcutChanged(String statusKey) {
        this.notifyOwnConfigChanged(false);
        this.refreshVisibleRows();
        this.setStatus(statusKey);
    }

    private boolean handleResetClick(IConfigBase config, int mouseX, int mouseY, int x, int y) {
        if (GuiHitTest.isInside(mouseX, mouseY, x, y, 54, BUTTON_HEIGHT)
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

        return this.selectedGroupId.isBlank() || entry.groupId().equals(this.selectedGroupId);
    }

    private boolean matchesConfigFilterMode(ConfigIndexEntry entry) {
        return switch (this.filterMode) {
            case ALL -> true;
            case ADDED -> ShortcutConfigStore.containsTarget(entry.modId(), entry.groupId(), entry.configName());
            case MISSING ->
                ShortcutConfigStore.containsTarget(entry.modId(), entry.groupId(), entry.configName()) == false;
        };
    }

    private boolean matchesShortcut(ShortcutView view, String filter) {
        if (filter.isBlank()) {
            return true;
        }

        String haystack = (view.shortcut.manualId() + " "
                + (view.config == null ? ""
                        : view.config.modName() + " " + view.config.groupName() + " " + view.config.displayName()))
                .toLowerCase(Locale.ROOT);
        return haystack.contains(filter);
    }

    private boolean matchesSelectedFilters(ShortcutView view) {
        String modId = view.config == null ? view.shortcut.modId() : view.config.modId();
        String groupId = view.config == null ? view.shortcut.groupId() : view.config.groupId();

        if (this.selectedModId.isBlank() == false && modId.equals(this.selectedModId) == false) {
            return false;
        }

        return this.selectedGroupId.isBlank() || groupId.equals(this.selectedGroupId);
    }

    private boolean matchesShortcutFilterMode(ShortcutView view) {
        return switch (this.filterMode) {
            case ALL -> true;
            case ADDED -> view.config != null;
            case MISSING -> view.config == null;
        };
    }

    private String getSearchText() {
        return this.searchField == null ? "" : this.searchField.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private int getCurrentRowCount() {
        return switch (tab) {
            case GENERIC -> this.filteredGenericConfigs.size();
            case SHORTCUTS -> this.shortcutViews.size();
            case ALL_CONFIGS -> this.filteredConfigs.size();
        };
    }

    private int getVisibleRows() {
        int bottom = tab == ConfigGuiTab.SHORTCUTS ? this.height - 40 : this.height - 18;
        int top = LIST_Y;
        return Math.max(1, (bottom - top) / (ROW_HEIGHT + ROW_GAP));
    }

    private boolean isInsideList(int mouseX, int mouseY) {
        int top = LIST_Y;
        int bottom = tab == ConfigGuiTab.SHORTCUTS ? this.height - 40 : this.height - 18;
        return GuiHitTest.isInside(mouseX, mouseY, MARGIN, top, this.width - MARGIN * 2, bottom - top);
    }

    private int getRowIndexAt(int mouseX, int mouseY, int rowCount) {
        if (this.isInsideList(mouseX, mouseY) == false) {
            return -1;
        }

        int visibleIndex = (mouseY - LIST_Y) / (ROW_HEIGHT + ROW_GAP);
        int index = this.scrollOffset + visibleIndex;
        int rowY = LIST_Y + visibleIndex * (ROW_HEIGHT + ROW_GAP);

        if (mouseY >= rowY + ROW_HEIGHT || index < 0 || index >= rowCount) {
            return -1;
        }

        return index;
    }

    private int getControlX() {
        return Math.max(MARGIN + 120, this.width - MARGIN - 184);
    }

    private String getFilterButtonText() {
        if (tab == ConfigGuiTab.SHORTCUTS && this.filterMode == FilterMode.MISSING) {
            return StringUtils.translate("fast-masa-config.gui.full.filter.invalid");
        }

        return StringUtils.translate(this.filterMode.translationKey);
    }

    private FilterMode getNextFilterMode() {
        if (tab == ConfigGuiTab.SHORTCUTS) {
            return this.filterMode == FilterMode.MISSING ? FilterMode.ALL : FilterMode.MISSING;
        }

        return this.filterMode.next();
    }

    private String getModFilterButtonText() {
        String label = this.selectedModId.isBlank()
                ? StringUtils.translate("fast-masa-config.gui.full.filter.value_all")
                : this.getSelectedModName();
        return StringUtils.translate("fast-masa-config.gui.full.filter.mod", label);
    }

    private String getGroupFilterButtonText() {
        String label = this.selectedGroupId.isBlank()
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
        this.selectedGroupId = "";
    }

    private void cycleGroupFilter() {
        List<String> groupIds = ConfigIndexService.scanSupportedConfigs().stream()
                .filter(entry -> this.selectedModId.isBlank() || entry.modId().equals(this.selectedModId))
                .map(entry -> entry.groupId())
                .filter(groupId -> groupId.isBlank() == false)
                .distinct()
                .toList();
        int index = groupIds.indexOf(this.selectedGroupId);
        this.selectedGroupId = index < 0 ? (groupIds.isEmpty() ? "" : groupIds.get(0))
                : (index + 1 >= groupIds.size() ? "" : groupIds.get(index + 1));
    }

    private void normalizeSelectedFilters(List<ConfigIndexEntry> index) {
        if (this.selectedModId.isBlank() == false
                && index.stream().noneMatch(entry -> entry.modId().equals(this.selectedModId))) {
            this.selectedModId = "";
            this.selectedGroupId = "";
        }

        if (this.selectedGroupId.isBlank() == false && index.stream()
                .filter(entry -> this.selectedModId.isBlank() || entry.modId().equals(this.selectedModId))
                .noneMatch(entry -> entry.groupId().equals(this.selectedGroupId))) {
            this.selectedGroupId = "";
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
                .filter(entry -> entry.groupId().equals(this.selectedGroupId))
                .map(entry -> entry.groupName())
                .findFirst()
                .orElse(this.selectedGroupId);
    }

    private void setStatus(String translationKey) {
        this.statusText = StringUtils.translate(translationKey);
        this.statusTicks = 80;
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
        if (config instanceof IConfigInteger integerConfig) {
            this.applyNumericSliderValue(integerConfig, mouseX);
        } else if (config instanceof IConfigDouble doubleConfig) {
            this.applyNumericSliderValue(doubleConfig, mouseX);
        }
    }

    private void applyNumericSliderValue(IConfigInteger config, int mouseX) {
        int min = config.getMinIntegerValue();
        int max = config.getMaxIntegerValue();
        config.setIntegerValue(min + (int) Math.round(this.getSliderRatioAt(mouseX) * (max - min)));
        this.notifyOwnConfigChanged(false);
    }

    private void applyNumericSliderValue(IConfigDouble config, int mouseX) {
        double min = config.getMinDoubleValue();
        double max = config.getMaxDoubleValue();
        config.setDoubleValue(min + this.getSliderRatioAt(mouseX) * (max - min));
        this.notifyOwnConfigChanged(false);
    }

    private double getSliderRatioAt(int mouseX) {
        int sliderX = this.getControlX() + NUMERIC_SLIDER_X_OFFSET;
        return clampRatio((mouseX - sliderX) / (double) NUMERIC_SLIDER_WIDTH);
    }

    private String fitText(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }

        if (this.textRenderer.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int end = text.length();

        while (end > 0 && this.textRenderer.width(text.substring(0, end) + ellipsis) > maxWidth) {
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

    static StatusToastPlacement getStatusToastPlacement(int screenWidth, int textWidth) {
        int maxTextWidth = Math.max(0, screenWidth - STATUS_MIN_X - STATUS_RIGHT_RESERVED);
        int width = Math.min(textWidth, maxTextWidth);
        int x = STATUS_MIN_X + Math.max(0, (maxTextWidth - width) / 2);
        return new StatusToastPlacement(x, STATUS_Y, width);
    }

    record StatusToastPlacement(int x, int y, int textWidth) {
    }

    private enum ConfigGuiTab {
        GENERIC("fast-masa-config.gui.tab.generic"),
        SHORTCUTS("fast-masa-config.gui.tab.shortcuts"),
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

    private record ShortcutView(int storeIndex, ShortcutEntry shortcut, ConfigIndexEntry config) {
    }

    private static final class HotkeySettingsButton extends ButtonGeneric {
        private static final Identifier TEXTURE = Identifier.of(MaLiLibReference.MOD_ID,
                "textures/gui/gui_widgets.png");
        private final IKeybind keybind;

        private HotkeySettingsButton(int x, int y, int width, int height, IKeybind keybind) {
            super(x, y, width, height, "");
            this.keybind = keybind;
            this.setRenderDefaultBackground(false);
        }

        @Override
        public void render(GuiContext GuiContext, int mouseX, int mouseY, boolean selected) {
            if (this.guiContext == null || this.guiContext.equals(GuiContext) == false) {
                this.guiContext = guiContext;
            }

            if (this.visible == false) {
                return;
            }

            this.hovered = this.enabled && mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width
                    && mouseY < this.y + this.height;

            KeybindSettings settings = this.keybind.getSettings();
            int iconSize = 18;
            int x = this.x;
            int y = this.y;
            int edgeColor = this.keybind.areSettingsModified() ? 0xFFFFBB33 : (this.hovered ? COLOR_TEXT : 0xFFFFFFFF);

            RenderUtils.drawRect(GuiContext, x, y, 20, 20, edgeColor);
            RenderUtils.drawRect(GuiContext, x + 1, y + 1, 18, 18, 0xFF000000);
            RenderUtils.drawTexturedRect(GuiContext, TEXTURE, x + 1, y + 1, 0,
                    settings.getActivateOn().ordinal() * iconSize, iconSize, iconSize, 0);
            RenderUtils.drawTexturedRect(GuiContext, TEXTURE, x + 1, y + 1, 18,
                    settings.getAllowExtraKeys() ? 0 : iconSize, iconSize, iconSize, 0);
            RenderUtils.drawTexturedRect(GuiContext, TEXTURE, x + 1, y + 1, 36,
                    settings.isOrderSensitive() ? iconSize : 0, iconSize, iconSize, 0);
            RenderUtils.drawTexturedRect(GuiContext, TEXTURE, x + 1, y + 1, 54, settings.isExclusive() ? iconSize : 0,
                    iconSize, iconSize, 0);
            RenderUtils.drawTexturedRect(GuiContext, TEXTURE, x + 1, y + 1, 72, settings.shouldCancel() ? iconSize : 0,
                    iconSize, iconSize, 0);
        }
    }
}
