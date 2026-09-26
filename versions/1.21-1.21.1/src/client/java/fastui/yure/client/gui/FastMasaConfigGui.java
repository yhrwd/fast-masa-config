package fastui.yure.client.gui;

import fastui.yure.FastMasaConfig;
import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.client.index.ConfigIndexService;
import fastui.yure.client.input.HeldKeyInputSuppressor;
import fastui.yure.client.shortcut.ShortcutResolver;
import fastui.yure.config.ConfigEditResult;
import fastui.yure.config.FastMasaConfigs;
import fastui.yure.config.MasaConfigEditor;
import fastui.yure.config.ShortcutConfigStore;
import fastui.yure.config.ShortcutControlType;
import fastui.yure.config.ShortcutEntry;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.IConfigResettable;
import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.config.IStringRepresentable;
import fi.dy.masa.malilib.config.gui.ButtonPressDirtyListenerSimple;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.MaLiLibConfigs;
import fi.dy.masa.malilib.MaLiLibReference;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.GuiKeybindSettings;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ConfigButtonKeybind;
import fi.dy.masa.malilib.gui.interfaces.IConfigInfoProvider;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.data.ModInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntFunction;

/** 1.21.1 的完整配置页，使用 MaLiLib 0.21.10 的 GuiBase 控件生命周期。 */
public final class FastMasaConfigGui extends GuiBase implements IKeybindConfigGui {
    private static final int MARGIN = 12;
    private static final int TAB_Y = 28;
    private static final int TOOLBAR_Y = 54;
    private static final int LIST_Y = 80;
    private static final int ROW_HEIGHT = 30;
    private static final int ROW_GAP = 3;
    private static final int BUTTON_HEIGHT = 20;
    private static final int NUMERIC_VALUE_WIDTH = 50;
    private static final int NUMERIC_SLIDER_WIDTH = 68;
    private static final int NUMERIC_RESET_WIDTH = 54;
    private static final int NUMERIC_CONTROL_WIDTH = NUMERIC_VALUE_WIDTH + 6 + NUMERIC_SLIDER_WIDTH + 6 + NUMERIC_RESET_WIDTH;
    private static ConfigGuiTab tab = ConfigGuiTab.GENERIC;

    private final HeldKeyInputSuppressor inputSuppressor;
    private final MasaConfigEditor editor = new MasaConfigEditor();
    private final List<Runnable> keybindChangeListeners = new ArrayList<>();
    private final ButtonPressDirtyListenerSimple dirtyListener = new ButtonPressDirtyListenerSimple();
    private GuiTextFieldGeneric searchField;
    private GuiTextFieldGeneric valueField;
    private GuiTextFieldGeneric manualIdField;
    private ConfigButtonKeybind openQuickConfigButton;
    private ConfigButtonKeybind activeKeybindButton;
    private ButtonGeneric hotkeySettingsButton;
    private KeybindSettings lastObservedOpenQuickConfigSettings;
    private String activeKeybindValueBeforeCapture;
    private IConfigBase activeNumericSliderConfig;
    private IConfigBase selectedGenericConfig;
    private FilterMode filterMode = FilterMode.ALL;
    private String selectedModId = "";
    private String selectedGroupId = "";
    private int scrollOffset;
    private List<IConfigBase> genericRows = List.of();
    private List<ConfigIndexEntry> allConfigRows = List.of();
    private List<ShortcutRow> shortcutRows = List.of();
    private String statusMessage = "";

    public FastMasaConfigGui() {
        this(null, Set.of());
    }

    public FastMasaConfigGui(Screen parent) {
        this(parent, Set.of());
    }

    public FastMasaConfigGui(Screen parent, Set<Integer> suppressKeys) {
        this.setParent(parent);
        this.setTitle(StringUtils.translate("fast-masa-config.gui.title.configs"));
        this.inputSuppressor = new HeldKeyInputSuppressor(suppressKeys);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.clearOptions();
        this.buildConfigSwitcher();
        this.createTabs();
        this.createToolbar();
        this.refreshRows();
        this.observeOpenQuickConfigSettings();
    }

    @Override
    protected void drawContents(DrawContext context, int mouseX, int mouseY, float delta) {
        this.drawRows(context, mouseX, mouseY);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int button) {
        boolean handled = super.onMouseClicked(mouseX, mouseY, button);

        if (this.activeKeybindButton != null && this.activeKeybindButton.isMouseOver(mouseX, mouseY) == false) {
            this.finishActiveKeybindCapture();
        }

        if (handled) {
            return true;
        }

        if (this.activeKeybindButton != null) {
            this.finishActiveKeybindCapture();
            return true;
        }

        int rowIndex = this.getRowIndexAt((int) mouseX, (int) mouseY, this.currentRowCount());

        if (rowIndex < 0) {
            return false;
        }

        return switch (tab) {
            case GENERIC -> this.handleGenericClick(rowIndex, (int) mouseX, (int) mouseY);
            case SHORTCUTS -> this.handleShortcutClick(rowIndex, (int) mouseX, (int) mouseY);
            case ALL_CONFIGS -> this.handleAllConfigsClick(rowIndex, (int) mouseX, (int) mouseY);
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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.activeNumericSliderConfig = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseScrolled(int mouseX, int mouseY, double horizontalAmount, double verticalAmount) {
        if (super.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }

        if (this.isInsideList((int) mouseX, (int) mouseY)) {
            int next = clamp(this.scrollOffset + (verticalAmount < 0 ? 1 : -1), 0,
                    Math.max(0, this.currentRowCount() - this.visibleRows()));
            boolean changed = next != this.scrollOffset;
            this.scrollOffset = next;
            this.updateOpenQuickConfigButtonPosition();
            return changed;
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
            return true;
        }

        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean onCharTyped(char chr, int modifiers) {
        if (this.inputSuppressor.shouldSuppressChar()) {
            return true;
        }

        return super.onCharTyped(chr, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        this.inputSuppressor.release(keyCode);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        this.mc.setScreen(this.getParent());
    }

    @Override
    public void removed() {
        this.finishActiveKeybindCapture();

        super.removed();
    }

    private void createTabs() {
        int x = MARGIN;

        for (ConfigGuiTab value : ConfigGuiTab.values()) {
            String label = StringUtils.translate(value.translationKey);
            ButtonGeneric button = new ButtonGeneric(x, TAB_Y, -1, BUTTON_HEIGHT, label);
            button.setEnabled(tab != value);
            this.addButton(button, (ignored, mouseButton) -> {
                tab = value;
                this.scrollOffset = 0;
                this.selectedGenericConfig = null;
                this.filterMode = normalizeFilterMode(tab == ConfigGuiTab.SHORTCUTS, this.filterMode);
                this.initGui();
            });
            x += button.getWidth() + 4;
        }
    }

    private void createToolbar() {
        int filterWidth = tab == ConfigGuiTab.GENERIC ? 0 : 96;
        int selectionWidth = tab == ConfigGuiTab.GENERIC ? 0 : 104;
        int searchWidth = Math.max(80, Math.min(220, this.width - MARGIN * 2 - filterWidth - selectionWidth * 2 - 18));
        this.searchField = new GuiTextFieldGeneric(MARGIN, TOOLBAR_Y, searchWidth, 18, this.textRenderer);
        this.searchField.setMaxLengthWrapper(128);
        this.addTextField(this.searchField, field -> {
            this.scrollOffset = 0;
            this.refreshRows();
            return true;
        });

        if (tab == ConfigGuiTab.GENERIC) {
            this.createOpenQuickConfigButtons();
            this.createGenericEditor();
            return;
        }

        int x = MARGIN + searchWidth + 6;
        this.addButton(new ButtonGeneric(x, TOOLBAR_Y, filterWidth, BUTTON_HEIGHT, this.filterLabel()), (ignored, mouseButton) -> {
            this.filterMode = nextFilterMode(tab == ConfigGuiTab.SHORTCUTS, this.filterMode);
            this.scrollOffset = 0;
            this.refreshRows();
            this.initGui();
        });
        x += filterWidth + 6;
        this.addButton(new ButtonGeneric(x, TOOLBAR_Y, selectionWidth, BUTTON_HEIGHT, this.modFilterLabel()), (ignored, mouseButton) -> {
            this.cycleModFilter();
            this.scrollOffset = 0;
            this.refreshRows();
            this.initGui();
        });
        x += selectionWidth + 6;
        this.addButton(new ButtonGeneric(x, TOOLBAR_Y, selectionWidth, BUTTON_HEIGHT, this.groupFilterLabel()), (ignored, mouseButton) -> {
            this.cycleGroupFilter();
            this.scrollOffset = 0;
            this.refreshRows();
            this.initGui();
        });

        if (tab == ConfigGuiTab.SHORTCUTS) {
            this.createManualShortcutEditor();
        }
    }

    private void createGenericEditor() {
        if (this.selectedGenericConfig == null) {
            return;
        }

        int valueWidth = Math.max(100, this.width - MARGIN * 2 - 116);
        this.valueField = new GuiTextFieldGeneric(MARGIN, this.height - 28, valueWidth, 18, this.textRenderer);
        this.valueField.setTextWrapper(this.configValue(this.selectedGenericConfig));
        this.valueField.setMaxLengthWrapper(256);
        this.addTextField(this.valueField, field -> true);
        this.addButton(new ButtonGeneric(MARGIN + valueWidth + 6, this.height - 28, 52, BUTTON_HEIGHT,
                StringUtils.translate("fast-masa-config.gui.full.apply")), (ignored, mouseButton) -> this.applySelectedGeneric());
        this.addButton(new ButtonGeneric(MARGIN + valueWidth + 64, this.height - 28, 52, BUTTON_HEIGHT,
                StringUtils.translate("malilib.gui.button.reset.caps")), (ignored, mouseButton) -> this.resetSelectedGeneric());
    }

    private void createManualShortcutEditor() {
        int addWidth = 52;
        int gap = 8;
        int rightPadding = MARGIN + 4;
        int valueWidth = Math.max(40, this.width - MARGIN - rightPadding - addWidth - gap);
        this.manualIdField = new GuiTextFieldGeneric(MARGIN, this.height - 28, valueWidth, 18, this.textRenderer);
        this.manualIdField.setMaxLengthWrapper(256);
        this.addTextField(this.manualIdField, field -> true);
        this.addButton(new ButtonGeneric(MARGIN + valueWidth + gap, this.height - 28, addWidth, BUTTON_HEIGHT, "+"),
                (ignored, mouseButton) -> this.addManualShortcut());
    }

    private void createOpenQuickConfigButtons() {
        int controlX = this.getGenericControlX();
        int keybindWidth = Math.max(80, Math.min(150, this.width - controlX - MARGIN - 24));
        this.openQuickConfigButton = new ConfigButtonKeybind(controlX, -1000, keybindWidth, BUTTON_HEIGHT,
                FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind(), this);
        this.hotkeySettingsButton = new HotkeySettingsButton(controlX + keybindWidth + 4, -1000, 20, BUTTON_HEIGHT,
                FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind());
        this.hotkeySettingsButton.setHoverStrings("fast-masa-config.gui.full.hotkey_settings.hover");
        this.addKeybindChangeListener(this.openQuickConfigButton::updateDisplayString);
        this.addButton(this.openQuickConfigButton, this.dirtyListener);
        this.addButton(this.hotkeySettingsButton, (ignored, mouseButton) -> {
            if (mouseButton == 1) {
                FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().resetSettingsToDefaults();
                this.notifyOwnConfigChanged(true);
                this.lastObservedOpenQuickConfigSettings = FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getSettings();
            } else {
                GuiBase.openGui(new GuiKeybindSettings(FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind(),
                        FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getName(), null, this));
            }
        });
        this.updateOpenQuickConfigButtonPosition();
    }

    private void updateOpenQuickConfigButtonPosition() {
        if (this.openQuickConfigButton == null || this.hotkeySettingsButton == null) {
            return;
        }

        int index = this.genericRows.indexOf(FastMasaConfigs.Generic.OPEN_QUICK_CONFIG);
        int visibleIndex = index - this.scrollOffset;
        int y = visibleIndex >= 0 && visibleIndex < this.visibleRows()
                ? LIST_Y + visibleIndex * (ROW_HEIGHT + ROW_GAP) + 5
                : -1000;
        int x = this.getGenericControlX();
        this.openQuickConfigButton.setX(x);
        this.openQuickConfigButton.setY(y);
        this.hotkeySettingsButton.setX(x + this.openQuickConfigButton.getWidth() + 4);
        this.hotkeySettingsButton.setY(y);
    }

    private void buildConfigSwitcher() {
        ModInfo currentMod = Registry.CONFIG_SCREEN.getModInfoFromConfigScreen(this.getClass());

        if (currentMod == null) {
            try {
                Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                        new ModInfo(FastMasaConfig.MOD_ID, FastMasaConfig.MOD_ID, () -> this));
                currentMod = Registry.CONFIG_SCREEN.getModInfoFromConfigScreen(this.getClass());
            } catch (RuntimeException e) {
                FastMasaConfig.LOGGER.warn("Failed to register Fast Masa Config in the MaLiLib config screen registry", e);
                return;
            }
        }

        if (currentMod == null || MaLiLibConfigs.Generic.ENABLE_CONFIG_SWITCHER.getBooleanValue() == false) {
            return;
        }

        ModInfo selectedMod = currentMod;
        this.addWidget(new WidgetDropDownList<ModInfo>(this.width - 155, 6, 130, 18, 200, 10,
                Registry.CONFIG_SCREEN.getAllModsWithConfigScreens()) {
            {
                this.selectedEntry = selectedMod;
            }

            @Override
            protected void setSelectedEntry(int index) {
                super.setSelectedEntry(index);

                ModInfo selection = this.selectedEntry;
                if (selection != null && selection.getConfigScreenSupplier() != null) {
                    FastMasaConfigGui.this.mc.setScreen(selection.getConfigScreenSupplier().get());
                }
            }

            @Override
            protected String getDisplayString(ModInfo entry) {
                return entry.getModName();
            }
        });
    }

    @Override
    public String getModId() {
        return FastMasaConfig.MOD_ID;
    }

    @Override
    public void clearOptions() {
        this.finishActiveKeybindCapture();
        this.keybindChangeListeners.clear();
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
        this.keybindChangeListeners.add(listener);
    }

    @Override
    public void setActiveKeybindButton(ConfigButtonKeybind button) {
        if (this.activeKeybindButton != null) {
            this.activeKeybindButton.onClearSelection();
        }

        ConfigButtonKeybind previousButton = this.activeKeybindButton;
        this.activeKeybindButton = button;

        if (this.activeKeybindButton != null) {
            this.activeKeybindValueBeforeCapture = FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getStringValue();
            this.activeKeybindButton.onSelected();
        } else if (previousButton != null) {
            this.flushPendingKeybindChange();
        }
    }

    private void finishActiveKeybindCapture() {
        if (this.activeKeybindButton != null) {
            this.setActiveKeybindButton(null);
        }
    }

    private void flushPendingKeybindChange() {
        String currentValue = FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getStringValue();
        boolean changed = shouldCommitKeybindCapture(this.activeKeybindValueBeforeCapture, currentValue, false, true);
        this.activeKeybindValueBeforeCapture = null;

        if (changed) {
            this.notifyOwnConfigChanged(true);
        }

        this.dirtyListener.resetDirty();
    }

    static boolean shouldCommitKeybindCapture(String initialValue, String currentValue, boolean dirty, boolean captureEnded) {
        return captureEnded && Objects.equals(initialValue, currentValue) == false;
    }

    private void notifyOwnConfigChanged(boolean updateHotkeys) {
        ConfigManager.getInstance().onConfigsChanged(FastMasaConfig.MOD_ID);

        if (updateHotkeys) {
            InputEventHandler.getKeybindManager().updateUsedKeys();
            this.updateKeybindButtons();
        }
    }

    private void observeOpenQuickConfigSettings() {
        KeybindSettings currentSettings = FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getSettings();
        if (hasOpenQuickConfigSettingsChanged(this.lastObservedOpenQuickConfigSettings, currentSettings)) {
            this.notifyOwnConfigChanged(true);
        }
        this.lastObservedOpenQuickConfigSettings = currentSettings;
    }

    static boolean hasOpenQuickConfigSettingsChanged(KeybindSettings previousSettings, KeybindSettings currentSettings) {
        return previousSettings != null && (previousSettings.getContext() != currentSettings.getContext()
                || previousSettings.getActivateOn() != currentSettings.getActivateOn()
                || previousSettings.getAllowEmpty() != currentSettings.getAllowEmpty()
                || previousSettings.getAllowExtraKeys() != currentSettings.getAllowExtraKeys()
                || previousSettings.isOrderSensitive() != currentSettings.isOrderSensitive()
                || previousSettings.isExclusive() != currentSettings.isExclusive()
                || previousSettings.shouldCancel() != currentSettings.shouldCancel());
    }

    private void updateKeybindButtons() {
        for (Runnable listener : this.keybindChangeListeners) {
            listener.run();
        }
    }

    private void drawRows(DrawContext context, int mouseX, int mouseY) {
        int total = this.currentRowCount();
        String count = total + " / " + this.totalRowCount();
        context.drawTextWithShadow(this.textRenderer, count, this.width - MARGIN - this.textRenderer.getWidth(count), TOOLBAR_Y + 5,
                0xAAAAAA);

        if (total == 0) {
            context.drawTextWithShadow(this.textRenderer, StringUtils.translate(tab == ConfigGuiTab.SHORTCUTS
                    ? "fast-masa-config.gui.full.empty_shortcuts" : "fast-masa-config.gui.full.empty_search"), MARGIN,
                    LIST_Y + 8, 0xAAAAAA);
            return;
        }

        if (this.statusMessage.isBlank() == false) {
            context.drawTextWithShadow(this.textRenderer, this.fit(this.statusMessage, this.width - MARGIN * 2), MARGIN,
                    this.height - 46, 0xFFDD88);
        }

        int end = Math.min(total, this.scrollOffset + this.visibleRows());
        for (int index = this.scrollOffset; index < end; index++) {
            int y = LIST_Y + (index - this.scrollOffset) * (ROW_HEIGHT + ROW_GAP);
            boolean hovered = GuiHitTest.contains(MARGIN, y, this.width - MARGIN * 2, ROW_HEIGHT, mouseX, mouseY);
            context.fill(MARGIN, y, this.width - MARGIN, y + ROW_HEIGHT, hovered ? 0xCC2A1D25 : 0xA0201820);
            context.fill(MARGIN, y, MARGIN + 2, y + ROW_HEIGHT, hovered ? 0xFFE6397C : 0xFF6A344B);

            switch (tab) {
                case GENERIC -> this.drawGenericRow(context, this.genericRows.get(index), y, mouseX, mouseY);
                case SHORTCUTS -> this.drawShortcutRow(context, this.shortcutRows.get(index), y, mouseX, mouseY);
                case ALL_CONFIGS -> this.drawAllConfigRow(context, this.allConfigRows.get(index), y, mouseX, mouseY);
            }
        }
    }

    private void drawGenericRow(DrawContext context, IConfigBase config, int y, int mouseX, int mouseY) {
        String name = config.getConfigGuiDisplayName();
        String value = this.configValue(config);
        int textWidth = genericControlKind(config) == GenericControlKind.NONE && config != FastMasaConfigs.Generic.OPEN_QUICK_CONFIG
                ? this.width - MARGIN * 2 - 16
                : this.getGenericControlX() - MARGIN - 16;
        context.drawTextWithShadow(this.textRenderer, this.fit(name == null ? config.getName() : name, textWidth), MARGIN + 8, y + 5,
                0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, this.fit(config.getComment(), textWidth), MARGIN + 8, y + 18, 0xCFA4B7);
        if (config == FastMasaConfigs.Generic.OPEN_QUICK_CONFIG) {
            this.updateOpenQuickConfigButtonPosition();
        } else if (genericControlKind(config) != GenericControlKind.NONE) {
            this.drawGenericControl(context, config, this.getGenericControlX(), y + 5, mouseX, mouseY);
        } else {
            context.drawTextWithShadow(this.textRenderer, this.fit(value, 130), this.width - MARGIN - 138, y + 10,
                    0xFFFFFF);
        }
    }

    private void drawGenericControl(DrawContext context, IConfigBase config, int x, int y, int mouseX, int mouseY) {
        switch (genericControlKind(config)) {
            case BOOLEAN -> {
                BooleanControlLayout layout = booleanControlLayout(this.width);
                boolean enabled = ((IConfigBoolean) config).getBooleanValue();
                this.drawAction(context, layout.controlX(), y, layout.toggleWidth(), enabled ? "ON" : "OFF", mouseX, mouseY);
                this.drawAction(context, layout.resetX(), y, layout.resetWidth(),
                        StringUtils.translate("malilib.gui.button.reset.caps"), mouseX, mouseY);
            }
            case INTEGER -> this.drawNumericControl(context, config, x, y,
                    Integer.toString(((IConfigInteger) config).getIntegerValue()), this.getIntegerRatio((IConfigInteger) config),
                    mouseX, mouseY);
            case DOUBLE -> this.drawNumericControl(context, config, x, y,
                    String.format(Locale.ROOT, "%.2f", ((IConfigDouble) config).getDoubleValue()), this.getDoubleRatio((IConfigDouble) config),
                    mouseX, mouseY);
            case NONE -> {
            }
        }
    }

    private void drawNumericControl(DrawContext context, IConfigBase config, int x, int y, String value, double ratio,
            int mouseX, int mouseY) {
        NumericControlLayout layout = numericControlLayout(this.width);
        context.fill(x, y, x + layout.valueWidth(), y + BUTTON_HEIGHT, 0xFF161616);
        context.drawTextWithShadow(this.textRenderer, this.fit(value, layout.valueWidth() - 8), x + 4, y + 6, 0xFFFFFF);
        int fillWidth = (int) Math.round(layout.sliderWidth() * clampRatio(ratio));
        context.fill(layout.sliderX(), y + BUTTON_HEIGHT / 2 - 1, layout.sliderX() + layout.sliderWidth(), y + BUTTON_HEIGHT / 2 + 2,
                GuiHitTest.contains(layout.sliderX(), y, layout.sliderWidth(), BUTTON_HEIGHT, mouseX, mouseY) ? 0xFF404040 : 0xFF2A2A2A);
        context.fill(layout.sliderX(), y + BUTTON_HEIGHT / 2 - 1, layout.sliderX() + fillWidth, y + BUTTON_HEIGHT / 2 + 2, 0xFFE6397C);
        int knobX = layout.sliderX() + Math.max(0, fillWidth - 2);
        int knobWidth = Math.min(4, Math.max(0, layout.right() - knobX));
        context.fill(knobX, y + 3, knobX + knobWidth, y + BUTTON_HEIGHT - 3, 0xFFFFFFFF);
        this.drawAction(context, layout.resetX(), y, layout.resetWidth(),
                StringUtils.translate("malilib.gui.button.reset.caps"), mouseX, mouseY);
    }

    private void drawShortcutRow(DrawContext context, ShortcutRow row, int y, int mouseX, int mouseY) {
        String name = row.entry == null ? row.shortcut.manualId() : row.entry.displayName();
        String meta = row.entry == null ? StringUtils.translate("fast-masa-config.gui.full.status.not_found")
                : row.entry.modName() + " / " + row.entry.groupName() + " / " + row.shortcut.manualId();
        int buttonsX = this.width - MARGIN - 102;
        context.drawTextWithShadow(this.textRenderer, this.fit(name, buttonsX - MARGIN - 16), MARGIN + 8, y + 5, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, this.fit(meta, buttonsX - MARGIN - 16), MARGIN + 8, y + 18, 0xCFA4B7);
        this.drawAction(context, buttonsX, y + 5, 24, "^", mouseX, mouseY);
        this.drawAction(context, buttonsX + 28, y + 5, 24, "v", mouseX, mouseY);
        this.drawAction(context, buttonsX + 56, y + 5, 42, "-", mouseX, mouseY);
    }

    private void drawAllConfigRow(DrawContext context, ConfigIndexEntry entry, int y, int mouseX, int mouseY) {
        int buttonX = this.width - MARGIN - 64;
        String meta = entry.modName() + " / " + entry.groupName() + " / " + entry.manualId();
        context.drawTextWithShadow(this.textRenderer, this.fit(entry.displayName(), buttonX - MARGIN - 16), MARGIN + 8, y + 5,
                0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, this.fit(meta, buttonX - MARGIN - 16), MARGIN + 8, y + 18, 0xCFA4B7);
        this.drawAction(context, buttonX, y + 5, 64,
                ShortcutConfigStore.containsTarget(entry.modId(), entry.groupId(), entry.configName()) ? "-" : "+",
                mouseX, mouseY);
    }

    private void drawAction(DrawContext context, int x, int y, int width, String label, int mouseX, int mouseY) {
        int background = label.equals("-") ? 0xFF5A2525 : 0xFF303030;
        boolean hovered = GuiHitTest.contains(x, y, width, BUTTON_HEIGHT, mouseX, mouseY);
        context.fill(x, y, x + width, y + BUTTON_HEIGHT, hovered ? lighten(background) : background);
        context.drawCenteredTextWithShadow(this.textRenderer, this.fit(label, width - 6), x + width / 2, y + 6, 0xFFFFFF);
    }

    private boolean handleGenericClick(int rowIndex, int mouseX, int mouseY) {
        IConfigBase config = this.genericRows.get(rowIndex);
        if (config instanceof IHotkey) {
            return false;
        }

        int y = LIST_Y + (rowIndex - this.scrollOffset) * (ROW_HEIGHT + ROW_GAP) + 5;
        int controlX = this.getGenericControlX();
        if (config instanceof IConfigBoolean booleanConfig) {
            BooleanControlLayout layout = booleanControlLayout(this.width);
            if (GuiHitTest.contains(layout.controlX(), y, layout.toggleWidth(), BUTTON_HEIGHT, mouseX, mouseY)) {
                booleanConfig.setBooleanValue(booleanConfig.getBooleanValue() == false);
                this.afterInlineGenericConfigChanged();
                return true;
            }
            if (this.resetGenericConfig(config, layout.resetX(), y, mouseX, mouseY, layout.resetWidth())) {
                return true;
            }
        } else if (config instanceof IConfigInteger || config instanceof IConfigDouble) {
            NumericControlLayout layout = numericControlLayout(this.width);
            if (GuiHitTest.contains(layout.sliderX(), y, layout.sliderWidth(), BUTTON_HEIGHT, mouseX, mouseY)) {
                this.activeNumericSliderConfig = config;
                this.applyNumericSliderValue(config, mouseX);
                return true;
            }
            if (this.resetGenericConfig(config, layout.resetX(), y, mouseX, mouseY, layout.resetWidth())) {
                return true;
            }
        }

        this.selectedGenericConfig = config;
        this.initGui();
        return true;
    }

    private boolean resetGenericConfig(IConfigBase config, int x, int y, int mouseX, int mouseY) {
        return this.resetGenericConfig(config, x, y, mouseX, mouseY, 54);
    }

    private boolean resetGenericConfig(IConfigBase config, int x, int y, int mouseX, int mouseY, int width) {
        if (GuiHitTest.contains(x, y, width, BUTTON_HEIGHT, mouseX, mouseY)
                && config instanceof IConfigResettable resettable && resettable.isModified()) {
            resettable.resetToDefault();
            this.afterInlineGenericConfigChanged();
            return true;
        }
        return false;
    }

    private void applyNumericSliderValue(IConfigBase config, int mouseX) {
        NumericControlLayout layout = numericControlLayout(this.width);
        double ratio = layout.sliderWidth() == 0 ? 0.0 : clampRatio((mouseX - layout.sliderX()) / (double) layout.sliderWidth());
        if (config instanceof IConfigInteger integerConfig) {
            int min = integerConfig.getMinIntegerValue();
            integerConfig.setIntegerValue(min + (int) Math.round(ratio * (integerConfig.getMaxIntegerValue() - min)));
        } else if (config instanceof IConfigDouble doubleConfig) {
            double min = doubleConfig.getMinDoubleValue();
            doubleConfig.setDoubleValue(min + ratio * (doubleConfig.getMaxDoubleValue() - min));
        }
        this.afterInlineGenericConfigChanged();
    }

    private void afterInlineGenericConfigChanged() {
        this.notifyOwnConfigChanged(false);
        if (this.selectedGenericConfig != null && this.valueField != null) {
            this.valueField.setTextWrapper(this.configValue(this.selectedGenericConfig));
        }
        this.refreshRows();
    }

    private boolean handleShortcutClick(int rowIndex, int mouseX, int mouseY) {
        int y = LIST_Y + (rowIndex - this.scrollOffset) * (ROW_HEIGHT + ROW_GAP) + 5;
        int x = this.width - MARGIN - 102;
        ShortcutRow row = this.shortcutRows.get(rowIndex);
        if (GuiHitTest.contains(x, y, 24, BUTTON_HEIGHT, mouseX, mouseY)) {
            return this.moveShortcut(row.storeIndex, -1);
        }
        if (GuiHitTest.contains(x + 28, y, 24, BUTTON_HEIGHT, mouseX, mouseY)) {
            return this.moveShortcut(row.storeIndex, 1);
        }
        if (GuiHitTest.contains(x + 56, y, 42, BUTTON_HEIGHT, mouseX, mouseY)) {
            ShortcutConfigStore.remove(row.storeIndex);
            this.afterShortcutChanged();
            return true;
        }
        return false;
    }

    private boolean handleAllConfigsClick(int rowIndex, int mouseX, int mouseY) {
        int y = LIST_Y + (rowIndex - this.scrollOffset) * (ROW_HEIGHT + ROW_GAP) + 5;
        int x = this.width - MARGIN - 64;
        if (GuiHitTest.contains(x, y, 64, BUTTON_HEIGHT, mouseX, mouseY) == false) {
            return false;
        }
        ConfigIndexEntry entry = this.allConfigRows.get(rowIndex);
        if (ShortcutConfigStore.containsTarget(entry.modId(), entry.groupId(), entry.configName())) {
            ShortcutConfigStore.removeTarget(entry.modId(), entry.groupId(), entry.configName());
        } else {
            this.addShortcut(entry);
        }
        this.afterShortcutChanged();
        return true;
    }

    private boolean moveShortcut(int index, int offset) {
        if (ShortcutConfigStore.move(index, offset)) {
            this.afterShortcutChanged();
            return true;
        }
        return false;
    }

    private void addManualShortcut() {
        String manualId = this.manualIdField.getTextWrapper().trim();
        if (manualId.isBlank()) {
            return;
        }
        try {
            ShortcutEntry shortcut = ShortcutEntry.fromManualId(manualId);
            ConfigIndexEntry entry = ShortcutResolver.find(ConfigIndexService.scanSupportedConfigs(), shortcut).orElse(null);
            if (entry != null && this.addShortcut(entry)) {
                this.manualIdField.setText("");
                this.afterShortcutChanged();
            }
        } catch (IllegalArgumentException ignored) {
            this.setStatus(StringUtils.translate("fast-masa-config.gui.full.status.not_found"));
        }
    }

    private boolean addShortcut(ConfigIndexEntry entry) {
        if (ShortcutConfigStore.containsTarget(entry.modId(), entry.groupId(), entry.configName())) {
            return false;
        }
        ShortcutConfigStore.add(new ShortcutEntry(entry.modId(), entry.groupId(), entry.configName(), "",
                entry.config().getType() == ConfigType.BOOLEAN ? ShortcutControlType.TOGGLE : ShortcutControlType.SLIDER,
                entry.config().getType() == ConfigType.INTEGER ? 1.0 : 0.05, null, null));
        return true;
    }

    private void applySelectedGeneric() {
        ConfigEditResult result = this.editor.apply(this.selectedGenericConfig, this.valueField.getTextWrapper());
        this.setStatus(result.message());
        if (result.success()) {
            this.afterGenericConfigChanged();
        }
    }

    private void resetSelectedGeneric() {
        ConfigEditResult result = this.editor.reset(this.selectedGenericConfig);
        this.setStatus(result.message());
        if (result.success()) {
            this.valueField.setTextWrapper(this.configValue(this.selectedGenericConfig));
            this.afterGenericConfigChanged();
        }
    }

    private void afterGenericConfigChanged() {
        this.notifyOwnConfigChanged(this.selectedGenericConfig instanceof IHotkey);
        ConfigIndexService.invalidate();
        this.refreshRows();
    }

    private String configValue(IConfigBase config) {
        if (config instanceof IStringRepresentable representable) {
            return representable.getStringValue();
        }
        if (config instanceof IHotkey hotkey) {
            return hotkey.getKeybind().getStringValue();
        }
        if (config instanceof IConfigStringList stringList) {
            return String.join(", ", stringList.getStrings());
        }
        return "";
    }

    private void setStatus(String message) {
        this.statusMessage = message == null ? "" : message;
    }

    static void syncShortcutEditorToStore(boolean shortcutsTabActive) {
        // 旧的 MaLiLib 文本列表编辑器已替换为页面内手工 ID 输入；保留该入口不覆盖快捷项。
    }

    private void afterShortcutChanged() {
        this.notifyOwnConfigChanged(false);
        ConfigIndexService.invalidate();
        this.refreshRows();
    }

    private void refreshRows() {
        String search = this.searchText();
        List<ConfigIndexEntry> index = ConfigIndexService.scanSupportedConfigs();
        this.normalizeFilters(index);
        this.genericRows = FastMasaConfigs.Generic.OPTIONS.stream().filter(config -> matchesGeneric(config, search)).toList();
        this.allConfigRows = index.stream().filter(this::matchesFilters).filter(entry -> matchesEntry(entry, search))
                .filter(this::matchesAllMode).toList();
        List<ShortcutRow> shortcuts = new ArrayList<>();
        List<ShortcutEntry> entries = ShortcutConfigStore.getEntries();
        for (int indexInStore = 0; indexInStore < entries.size(); indexInStore++) {
            ShortcutEntry shortcut = entries.get(indexInStore);
            ConfigIndexEntry entry = ShortcutResolver.find(index, shortcut).orElse(null);
            ShortcutRow row = new ShortcutRow(indexInStore, shortcut, entry);
            if (this.matchesFilters(row) && this.matchesShortcut(row, search) && this.matchesShortcutMode(row)) {
                shortcuts.add(row);
            }
        }
        this.shortcutRows = List.copyOf(shortcuts);
        this.scrollOffset = clamp(this.scrollOffset, 0, Math.max(0, this.currentRowCount() - this.visibleRows()));
        this.updateOpenQuickConfigButtonPosition();
    }

    private boolean matchesGeneric(IConfigBase config, String search) {
        return search.isBlank() || (config.getName() + " " + config.getConfigGuiDisplayName() + " " + config.getComment())
                .toLowerCase(Locale.ROOT).contains(search);
    }

    private boolean matchesEntry(ConfigIndexEntry entry, String search) {
        return search.isBlank() || (entry.modId() + " " + entry.modName() + " " + entry.groupId() + " "
                + entry.groupName() + " " + entry.configName() + " " + entry.displayName()).toLowerCase(Locale.ROOT)
                .contains(search);
    }

    private boolean matchesFilters(ConfigIndexEntry entry) {
        return (this.selectedModId.isBlank() || this.selectedModId.equals(entry.modId()))
                && (this.selectedGroupId.isBlank() || this.selectedGroupId.equals(entry.groupId()));
    }

    private boolean matchesFilters(ShortcutRow row) {
        String modId = row.entry == null ? row.shortcut.modId() : row.entry.modId();
        String groupId = row.entry == null ? row.shortcut.groupId() : row.entry.groupId();
        return (this.selectedModId.isBlank() || this.selectedModId.equals(modId))
                && (this.selectedGroupId.isBlank() || this.selectedGroupId.equals(groupId));
    }

    private boolean matchesAllMode(ConfigIndexEntry entry) {
        boolean added = ShortcutConfigStore.containsTarget(entry.modId(), entry.groupId(), entry.configName());
        return this.filterMode == FilterMode.ALL || (this.filterMode == FilterMode.ADDED) == added;
    }

    private boolean matchesShortcut(ShortcutRow row, String search) {
        return search.isBlank() || (row.shortcut.manualId() + " " + (row.entry == null ? "" : row.entry.modName() + " "
                + row.entry.groupName() + " " + row.entry.displayName())).toLowerCase(Locale.ROOT).contains(search);
    }

    private boolean matchesShortcutMode(ShortcutRow row) {
        boolean resolved = row.entry != null;
        return this.filterMode == FilterMode.ALL || (this.filterMode == FilterMode.ADDED) == resolved;
    }

    private void cycleModFilter() {
        List<String> mods = ConfigIndexService.scanSupportedConfigs().stream().map(ConfigIndexEntry::modId).distinct().toList();
        this.selectedModId = nextValue(mods, this.selectedModId);
        this.selectedGroupId = "";
    }

    private void cycleGroupFilter() {
        List<String> groups = ConfigIndexService.scanSupportedConfigs().stream()
                .filter(entry -> this.selectedModId.isBlank() || this.selectedModId.equals(entry.modId()))
                .map(ConfigIndexEntry::groupId).filter(group -> group.isBlank() == false).distinct().toList();
        this.selectedGroupId = nextValue(groups, this.selectedGroupId);
    }

    private void normalizeFilters(List<ConfigIndexEntry> index) {
        if (index.stream().noneMatch(entry -> entry.modId().equals(this.selectedModId))) {
            this.selectedModId = "";
            this.selectedGroupId = "";
        }
        if (this.selectedGroupId.isBlank() == false && index.stream()
                .noneMatch(entry -> (this.selectedModId.isBlank() || this.selectedModId.equals(entry.modId()))
                        && this.selectedGroupId.equals(entry.groupId()))) {
            this.selectedGroupId = "";
        }
    }

    private String filterLabel() {
        return StringUtils.translate(this.filterMode.translationKey);
    }

    private String modFilterLabel() {
        return StringUtils.translate("fast-masa-config.gui.full.filter.mod",
                this.selectedModId.isBlank() ? StringUtils.translate("fast-masa-config.gui.full.filter.value_all") : this.selectedModId);
    }

    private String groupFilterLabel() {
        return StringUtils.translate("fast-masa-config.gui.full.filter.group",
                this.selectedGroupId.isBlank() ? StringUtils.translate("fast-masa-config.gui.full.filter.value_all") : this.selectedGroupId);
    }

    private String searchText() {
        return this.searchField == null ? "" : this.searchField.getTextWrapper().trim().toLowerCase(Locale.ROOT);
    }

    private int currentRowCount() {
        return switch (tab) {
            case GENERIC -> this.genericRows.size();
            case SHORTCUTS -> this.shortcutRows.size();
            case ALL_CONFIGS -> this.allConfigRows.size();
        };
    }

    private int totalRowCount() {
        return switch (tab) {
            case GENERIC -> FastMasaConfigs.Generic.OPTIONS.size();
            case SHORTCUTS -> ShortcutConfigStore.getEntries().size();
            case ALL_CONFIGS -> ConfigIndexService.scanSupportedConfigs().size();
        };
    }

    private int visibleRows() {
        int bottom = tab == ConfigGuiTab.SHORTCUTS || this.selectedGenericConfig != null ? this.height - 36 : this.height - 12;
        return Math.max(1, (bottom - LIST_Y) / (ROW_HEIGHT + ROW_GAP));
    }

    private int getGenericControlX() {
        return numericControlLayout(this.width).controlX();
    }

    static NumericControlLayout numericControlLayout(int width) {
        // 窄窗口压缩数值框、滑条和重置按钮，但保持常规宽度下的原始尺寸和顺序。
        int right = Math.max(MARGIN, width - MARGIN);
        int controlX = Math.max(MARGIN + 80, right - NUMERIC_CONTROL_WIDTH);
        if (controlX > right - 30) {
            controlX = Math.max(MARGIN, right - 30);
        }
        int available = Math.max(0, right - controlX);
        int gap = Math.min(6, available / 20);
        int remaining = Math.max(0, available - gap * 2);
        int valueWidth = Math.min(NUMERIC_VALUE_WIDTH, remaining / 3);
        int resetWidth = Math.min(NUMERIC_RESET_WIDTH, remaining / 3);
        int sliderWidth = Math.max(0, remaining - valueWidth - resetWidth);
        return new NumericControlLayout(controlX, valueWidth, gap, sliderWidth, resetWidth);
    }

    static BooleanControlLayout booleanControlLayout(int width) {
        NumericControlLayout numeric = numericControlLayout(width);
        int available = Math.max(0, numeric.right() - numeric.controlX());
        int gap = Math.min(numeric.gap(), available / 20);
        int remaining = Math.max(0, available - gap);
        int preferredTotal = 64 + 54;
        int toggleWidth = Math.min(64, remaining * 64 / preferredTotal);
        int resetWidth = Math.min(54, remaining - toggleWidth);
        return new BooleanControlLayout(numeric.controlX(), toggleWidth, gap, resetWidth);
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

    private boolean isInsideList(int mouseX, int mouseY) {
        int bottom = tab == ConfigGuiTab.SHORTCUTS || this.selectedGenericConfig != null ? this.height - 36 : this.height - 12;
        return GuiHitTest.contains(MARGIN, LIST_Y, this.width - MARGIN * 2, bottom - LIST_Y, mouseX, mouseY);
    }

    private int getRowIndexAt(int mouseX, int mouseY, int rowCount) {
        if (this.isInsideList(mouseX, mouseY) == false) {
            return -1;
        }
        int visibleIndex = (mouseY - LIST_Y) / (ROW_HEIGHT + ROW_GAP);
        int rowY = LIST_Y + visibleIndex * (ROW_HEIGHT + ROW_GAP);
        int index = this.scrollOffset + visibleIndex;
        return mouseY < rowY + ROW_HEIGHT && index < rowCount ? index : -1;
    }

    private String fit(String text, int maxWidth) {
        return fitText(text, maxWidth, this::getStringWidth);
    }

    static String fitText(String text, int maxWidth, ToIntFunction<String> width) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        if (width.applyAsInt(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int end = text.length();
        if (width.applyAsInt(ellipsis) > maxWidth) {
            while (end > 0 && width.applyAsInt(text.substring(0, end)) > maxWidth) {
                end--;
            }
            return text.substring(0, end);
        }
        while (end > 0 && width.applyAsInt(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    private static String nextValue(List<String> values, String current) {
        int index = values.indexOf(current);
        return index < 0 ? (values.isEmpty() ? "" : values.getFirst()) : (index + 1 == values.size() ? "" : values.get(index + 1));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampRatio(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    static GenericControlKind genericControlKind(IConfigBase config) {
        if (config instanceof IConfigBoolean) {
            return GenericControlKind.BOOLEAN;
        }
        if (config instanceof IConfigInteger) {
            return GenericControlKind.INTEGER;
        }
        if (config instanceof IConfigDouble) {
            return GenericControlKind.DOUBLE;
        }
        return GenericControlKind.NONE;
    }

    static FilterMode nextFilterMode(boolean shortcutsTabActive, FilterMode currentMode) {
        return shortcutsTabActive ? (currentMode == FilterMode.MISSING ? FilterMode.ALL : FilterMode.MISSING) : currentMode.next();
    }

    static FilterMode normalizeFilterMode(boolean shortcutsTabActive, FilterMode currentMode) {
        return shortcutsTabActive && currentMode == FilterMode.ADDED ? FilterMode.ALL : currentMode;
    }

    private static int lighten(int color) {
        int red = Math.min(255, ((color >> 16) & 0xFF) + 24);
        int green = Math.min(255, ((color >> 8) & 0xFF) + 24);
        int blue = Math.min(255, (color & 0xFF) + 24);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private enum ConfigGuiTab {
        GENERIC("fast-masa-config.gui.tab.generic"),
        SHORTCUTS("fast-masa-config.gui.tab.shortcuts"),
        ALL_CONFIGS("fast-masa-config.gui.tab.all_configs");

        private final String translationKey;

        ConfigGuiTab(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    enum FilterMode {
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

    enum GenericControlKind {
        BOOLEAN,
        INTEGER,
        DOUBLE,
        NONE
    }

    record NumericControlLayout(int controlX, int valueWidth, int gap, int sliderWidth, int resetWidth) {
        int sliderX() {
            return this.controlX + this.valueWidth + this.gap;
        }

        int resetX() {
            return this.sliderX() + this.sliderWidth + this.gap;
        }

        int right() {
            return this.resetX() + this.resetWidth;
        }

        int resetOffset() {
            return this.resetX() - this.controlX;
        }
    }

    record BooleanControlLayout(int controlX, int toggleWidth, int gap, int resetWidth) {
        int resetX() {
            return this.controlX + this.toggleWidth + this.gap;
        }

        int right() {
            return this.resetX() + this.resetWidth;
        }
    }

    private record ShortcutRow(int storeIndex, ShortcutEntry shortcut, ConfigIndexEntry entry) {
    }

    /** 复用 MaLiLib 的热键设置图标，并让 GuiBase 能正确读取 hover 状态。 */
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
        public void render(int mouseX, int mouseY, boolean selected, DrawContext context) {
            if (this.visible == false) {
                return;
            }

            this.hovered = this.enabled && GuiHitTest.contains(this.x, this.y, this.width, this.height, mouseX, mouseY);
            KeybindSettings settings = this.keybind.getSettings();
            int iconSize = 18;
            int edgeColor = this.keybind.areSettingsModified() ? 0xFFFFBB33
                    : (this.hovered ? 0xFFFFA0A0 : 0xFFFFFFFF);

            context.fill(this.x, this.y, this.x + 20, this.y + 20, edgeColor);
            context.fill(this.x + 1, this.y + 1, this.x + 19, this.y + 19, 0xFF000000);
            RenderUtils.bindTexture(TEXTURE);
            RenderUtils.drawTexturedRect(this.x + 1, this.y + 1, 0,
                    settings.getActivateOn().ordinal() * iconSize, iconSize, iconSize);
            RenderUtils.drawTexturedRect(this.x + 1, this.y + 1, 18,
                    settings.getAllowExtraKeys() ? 0 : iconSize, iconSize, iconSize);
            RenderUtils.drawTexturedRect(this.x + 1, this.y + 1, 36,
                    settings.isOrderSensitive() ? iconSize : 0, iconSize, iconSize);
            RenderUtils.drawTexturedRect(this.x + 1, this.y + 1, 54,
                    settings.isExclusive() ? iconSize : 0, iconSize, iconSize);
            RenderUtils.drawTexturedRect(this.x + 1, this.y + 1, 72,
                    settings.shouldCancel() ? iconSize : 0, iconSize, iconSize);
        }
    }
}
