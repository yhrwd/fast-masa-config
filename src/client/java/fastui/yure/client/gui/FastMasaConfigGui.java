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
import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.config.IStringRepresentable;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 1.21.1 的完整配置页，避免依赖 26.1 的 MaLiLib 自绘接口。 */
public final class FastMasaConfigGui extends Screen {
    private static final int MARGIN = 12;
    private static final int TAB_Y = 28;
    private static final int TOOLBAR_Y = 54;
    private static final int LIST_Y = 80;
    private static final int ROW_HEIGHT = 30;
    private static final int ROW_GAP = 3;
    private static final int BUTTON_HEIGHT = 20;
    private static ConfigGuiTab tab = ConfigGuiTab.GENERIC;

    private final Screen parent;
    private final HeldKeyInputSuppressor inputSuppressor;
    private final MasaConfigEditor editor = new MasaConfigEditor();
    private TextFieldWidget searchField;
    private TextFieldWidget valueField;
    private TextFieldWidget manualIdField;
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
        super(Text.translatable("fast-masa-config.gui.title.configs"));
        this.parent = parent;
        this.inputSuppressor = new HeldKeyInputSuppressor(suppressKeys);
    }

    @Override
    protected void init() {
        this.clearChildren();
        this.createTabs();
        this.createToolbar();
        this.refreshRows();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
        this.drawRows(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int rowIndex = this.getRowIndexAt((int) mouseX, (int) mouseY, this.currentRowCount());

        if (rowIndex < 0) {
            return false;
        }

        return switch (tab) {
            case GENERIC -> this.selectGenericRow(rowIndex);
            case SHORTCUTS -> this.handleShortcutClick(rowIndex, (int) mouseX, (int) mouseY);
            case ALL_CONFIGS -> this.handleAllConfigsClick(rowIndex, (int) mouseX, (int) mouseY);
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.isInsideList((int) mouseX, (int) mouseY)) {
            int next = clamp(this.scrollOffset + (verticalAmount < 0 ? 1 : -1), 0,
                    Math.max(0, this.currentRowCount() - this.visibleRows()));
            boolean changed = next != this.scrollOffset;
            this.scrollOffset = next;
            return changed;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.inputSuppressor.shouldSuppressKey(keyCode)) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.inputSuppressor.shouldSuppressChar()) {
            return true;
        }

        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        this.inputSuppressor.release(keyCode);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    private void createTabs() {
        int x = MARGIN;

        for (ConfigGuiTab value : ConfigGuiTab.values()) {
            String label = StringUtils.translate(value.translationKey);
            int buttonWidth = this.textRenderer.getWidth(label) + 18;
            ButtonWidget button = ButtonWidget.builder(Text.literal(label), ignored -> {
                tab = value;
                this.scrollOffset = 0;
                this.selectedGenericConfig = null;
                this.init();
            }).dimensions(x, TAB_Y, buttonWidth, BUTTON_HEIGHT).build();
            button.active = tab != value;
            this.addDrawableChild(button);
            x += buttonWidth + 4;
        }
    }

    private void createToolbar() {
        int filterWidth = tab == ConfigGuiTab.GENERIC ? 0 : 96;
        int selectionWidth = tab == ConfigGuiTab.GENERIC ? 0 : 104;
        int searchWidth = Math.max(80, Math.min(220, this.width - MARGIN * 2 - filterWidth - selectionWidth * 2 - 18));
        this.searchField = new TextFieldWidget(this.textRenderer, MARGIN, TOOLBAR_Y, searchWidth, 18,
                Text.translatable("fast-masa-config.gui.full.search"));
        this.searchField.setMaxLength(128);
        this.searchField.setPlaceholder(Text.translatable("fast-masa-config.gui.full.search"));
        this.searchField.setChangedListener(value -> {
            this.scrollOffset = 0;
            this.refreshRows();
        });
        this.addDrawableChild(this.searchField);

        if (tab == ConfigGuiTab.GENERIC) {
            this.createGenericEditor();
            return;
        }

        int x = MARGIN + searchWidth + 6;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(this.filterLabel()), ignored -> {
            this.filterMode = this.filterMode.next();
            this.scrollOffset = 0;
            this.refreshRows();
            this.init();
        }).dimensions(x, TOOLBAR_Y, filterWidth, BUTTON_HEIGHT).build());
        x += filterWidth + 6;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(this.modFilterLabel()), ignored -> {
            this.cycleModFilter();
            this.scrollOffset = 0;
            this.refreshRows();
            this.init();
        }).dimensions(x, TOOLBAR_Y, selectionWidth, BUTTON_HEIGHT).build());
        x += selectionWidth + 6;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(this.groupFilterLabel()), ignored -> {
            this.cycleGroupFilter();
            this.scrollOffset = 0;
            this.refreshRows();
            this.init();
        }).dimensions(x, TOOLBAR_Y, selectionWidth, BUTTON_HEIGHT).build());

        if (tab == ConfigGuiTab.SHORTCUTS) {
            this.createManualShortcutEditor();
        }
    }

    private void createGenericEditor() {
        if (this.selectedGenericConfig == null) {
            return;
        }

        int valueWidth = Math.max(100, this.width - MARGIN * 2 - 116);
        this.valueField = new TextFieldWidget(this.textRenderer, MARGIN, this.height - 28, valueWidth, 18,
                Text.literal(this.selectedGenericConfig.getName()));
        this.valueField.setText(this.configValue(this.selectedGenericConfig));
        this.valueField.setMaxLength(256);
        this.addDrawableChild(this.valueField);
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("fast-masa-config.gui.full.apply"),
                ignored -> this.applySelectedGeneric()).dimensions(MARGIN + valueWidth + 6, this.height - 28, 52,
                        BUTTON_HEIGHT).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("malilib.gui.button.reset.caps"),
                ignored -> this.resetSelectedGeneric()).dimensions(MARGIN + valueWidth + 64, this.height - 28, 52,
                        BUTTON_HEIGHT).build());
    }

    private void createManualShortcutEditor() {
        int valueWidth = Math.max(100, this.width - MARGIN * 2 - 58);
        this.manualIdField = new TextFieldWidget(this.textRenderer, MARGIN, this.height - 28, valueWidth, 18,
                Text.literal("modId/groupId/configName"));
        this.manualIdField.setMaxLength(256);
        this.manualIdField.setPlaceholder(Text.literal("modId/groupId/configName"));
        this.addDrawableChild(this.manualIdField);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), ignored -> this.addManualShortcut())
                .dimensions(MARGIN + valueWidth + 6, this.height - 28, 52, BUTTON_HEIGHT).build());
    }

    private void drawRows(DrawContext context, int mouseX, int mouseY) {
        int total = this.currentRowCount();
        String count = total + " / " + this.totalRowCount();
        context.drawTextWithShadow(this.textRenderer, count, this.width - MARGIN - this.textRenderer.getWidth(count), TOOLBAR_Y + 5,
                0xAAAAAA);

        if (total == 0) {
            context.drawTextWithShadow(this.textRenderer, Text.translatable(tab == ConfigGuiTab.SHORTCUTS
                    ? "fast-masa-config.gui.full.empty_shortcuts" : "fast-masa-config.gui.full.empty_search"), MARGIN, LIST_Y + 8,
                    0xAAAAAA);
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
                case GENERIC -> this.drawGenericRow(context, this.genericRows.get(index), y);
                case SHORTCUTS -> this.drawShortcutRow(context, this.shortcutRows.get(index), y);
                case ALL_CONFIGS -> this.drawAllConfigRow(context, this.allConfigRows.get(index), y);
            }
        }
    }

    private void drawGenericRow(DrawContext context, IConfigBase config, int y) {
        String name = config.getConfigGuiDisplayName();
        String value = this.configValue(config);
        context.drawTextWithShadow(this.textRenderer, name == null ? config.getName() : name, MARGIN + 8, y + 5, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, this.fit(config.getComment(), this.width - MARGIN * 2 - 16), MARGIN + 8,
                y + 18, 0xCFA4B7);
        context.drawTextWithShadow(this.textRenderer, this.fit(value, 130), this.width - MARGIN - 138, y + 10, 0xFFFFFF);
    }

    private void drawShortcutRow(DrawContext context, ShortcutRow row, int y) {
        String name = row.entry == null ? row.shortcut.manualId() : row.entry.displayName();
        String meta = row.entry == null ? StringUtils.translate("fast-masa-config.gui.full.status.not_found")
                : row.entry.modName() + " / " + row.entry.groupName() + " / " + row.shortcut.manualId();
        int buttonsX = this.width - MARGIN - 102;
        context.drawTextWithShadow(this.textRenderer, this.fit(name, buttonsX - MARGIN - 16), MARGIN + 8, y + 5, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, this.fit(meta, buttonsX - MARGIN - 16), MARGIN + 8, y + 18, 0xCFA4B7);
        this.drawAction(context, buttonsX, y + 5, 24, "^");
        this.drawAction(context, buttonsX + 28, y + 5, 24, "v");
        this.drawAction(context, buttonsX + 56, y + 5, 42, "-");
    }

    private void drawAllConfigRow(DrawContext context, ConfigIndexEntry entry, int y) {
        int buttonX = this.width - MARGIN - 64;
        String meta = entry.modName() + " / " + entry.groupName() + " / " + entry.manualId();
        context.drawTextWithShadow(this.textRenderer, this.fit(entry.displayName(), buttonX - MARGIN - 16), MARGIN + 8, y + 5,
                0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, this.fit(meta, buttonX - MARGIN - 16), MARGIN + 8, y + 18, 0xCFA4B7);
        this.drawAction(context, buttonX, y + 5, 64,
                ShortcutConfigStore.containsTarget(entry.modId(), entry.groupId(), entry.configName()) ? "-" : "+");
    }

    private void drawAction(DrawContext context, int x, int y, int width, String label) {
        context.fill(x, y, x + width, y + BUTTON_HEIGHT, 0xFF303030);
        context.drawCenteredTextWithShadow(this.textRenderer, label, x + width / 2, y + 6, 0xFFFFFF);
    }

    private boolean selectGenericRow(int rowIndex) {
        this.selectedGenericConfig = this.genericRows.get(rowIndex);
        this.init();
        return true;
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
        String manualId = this.manualIdField.getText().trim();
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
        ConfigEditResult result = this.editor.apply(this.selectedGenericConfig, this.valueField.getText());
        this.setStatus(result.message());
        if (result.success()) {
            this.afterGenericConfigChanged();
        }
    }

    private void resetSelectedGeneric() {
        ConfigEditResult result = this.editor.reset(this.selectedGenericConfig);
        this.setStatus(result.message());
        if (result.success()) {
            this.valueField.setText(this.configValue(this.selectedGenericConfig));
            this.afterGenericConfigChanged();
        }
    }

    private void afterGenericConfigChanged() {
        ConfigManager.getInstance().onConfigsChanged(FastMasaConfig.MOD_ID);
        if (this.selectedGenericConfig instanceof IHotkey) {
            InputEventHandler.getKeybindManager().updateUsedKeys();
        }
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
        ConfigManager.getInstance().onConfigsChanged(FastMasaConfig.MOD_ID);
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
        return this.searchField == null ? "" : this.searchField.getText().trim().toLowerCase(Locale.ROOT);
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
        if (text == null) {
            return "";
        }
        if (this.textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        int end = text.length();
        while (end > 0 && this.textRenderer.getWidth(text.substring(0, end) + "...") > maxWidth) {
            end--;
        }
        return text.substring(0, end) + "...";
    }

    private static String nextValue(List<String> values, String current) {
        int index = values.indexOf(current);
        return index < 0 ? (values.isEmpty() ? "" : values.getFirst()) : (index + 1 == values.size() ? "" : values.get(index + 1));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

    private record ShortcutRow(int storeIndex, ShortcutEntry shortcut, ConfigIndexEntry entry) {
    }
}
