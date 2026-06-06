package fastui.yure.client.gui;

import fastui.yure.FastMasaConfig;
import fastui.yure.client.input.HeldKeyInputSuppressor;
import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.client.index.ConfigIndexService;
import fastui.yure.config.FastMasaConfigs;
import fastui.yure.config.ShortcutConfigStore;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Fast Masa Config 的全屏配置界面。
 * 基于 MaLiLib 的 GuiConfigsBase，保留原配置列表、搜索、滚动和右上角 mod 切换能力。
 */
public final class FastMasaConfigGui extends GuiConfigsBase {
    private static ConfigGuiTab tab = ConfigGuiTab.GENERIC;
    private static final ShortcutStringListConfig SHORTCUTS = new ShortcutStringListConfig();
    private List<ConfigOptionWrapper> allConfigOptions = List.of();
    private final HeldKeyInputSuppressor inputSuppressor;

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
        super(10, 50, FastMasaConfig.MOD_ID, parent, "fast-masa-config.gui.title.configs");
        this.inputSuppressor = new HeldKeyInputSuppressor(suppressKeys);
    }

    @Override
    /**
     * 初始化 MaLiLib 基础界面，并在顶部补充本 mod 的三个分页按钮。
     */
    public void initGui() {
        super.initGui();
        this.clearOptions();

        int x = 10;
        int y = 26;

        for (ConfigGuiTab tab : ConfigGuiTab.values()) {
            x += this.createButton(x, y, -1, tab) + 2;
        }
    }

    @Override
    /**
     * 控制配置项左侧名称列宽。
     * All Configs 页只显示“是否加入快捷面板”的开关，左侧列可以更窄，让右侧说明有更多空间。
     */
    protected int getConfigWidth() {
        return tab == ConfigGuiTab.ALL_CONFIGS ? 42 : 220;
    }

    @Override
    /**
     * 根据当前分页返回 MaLiLib 需要渲染的配置项列表。
     * Generic 返回本 mod 设置，Shortcuts 返回手工快捷方式列表，All Configs 返回扫描到的所有可加入项。
     */
    public List<ConfigOptionWrapper> getConfigs() {
        if (tab == ConfigGuiTab.GENERIC) {
            return ConfigOptionWrapper.createFor(FastMasaConfigs.Generic.OPTIONS);
        }

        if (tab == ConfigGuiTab.SHORTCUTS) {
            SHORTCUTS.syncFromStore();
            return ConfigOptionWrapper.createFor(List.of(SHORTCUTS));
        }

        if (this.allConfigOptions.isEmpty()) {
            this.allConfigOptions = buildAllConfigOptions();
        }

        return this.allConfigOptions;
    }

    @Override
    /**
     * MaLiLib 在关闭或应用配置变更时调用。
     * 这里负责保存本 mod 配置、刷新索引缓存，并避免 All Configs 页把 Shortcuts 文本框的旧值写回 Store。
     */
    protected void onSettingsChanged() {
        // All Configs 页的开关会直接写 Store；只有 Shortcuts 页文本编辑器需要反向同步。
        syncShortcutEditorToStore(tab == ConfigGuiTab.SHORTCUTS);
        super.onSettingsChanged();
        ConfigIndexService.invalidate();
    }

    /**
     * 将 Shortcuts 文本编辑器同步回快捷方式 Store。
     * 只有用户真的在 Shortcuts 页编辑文本列表时才执行，避免空编辑器覆盖 All Configs 页刚勾选的快捷项。
     */
    static void syncShortcutEditorToStore(boolean shortcutsTabActive) {
        if (shortcutsTabActive) {
            SHORTCUTS.syncToStore();
        }
    }

    @Override
    /**
     * 处理全屏 UI 的按键事件。
     * 进入界面时仍按住的打开热键会被吞掉，松开后再交给 MaLiLib 搜索框和快捷键组件处理。
     */
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers) {
        if (this.inputSuppressor.shouldSuppressKey(keyCode)) {
            return true;
        }

        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    @Override
    /**
     * 处理全屏 UI 的字符输入事件。
     * GLFW 会在 keyPressed 后派发 charTyped，因此这里也要配合 HeldKeyInputSuppressor 做二次拦截。
     */
    public boolean onCharTyped(char charIn, int modifiers) {
        // 从快捷面板跳入全屏 UI 时，仍按住的打开热键会继续产生 char 事件，先吞掉避免污染搜索框。
        if (this.inputSuppressor.shouldSuppressChar()) {
            return true;
        }

        return super.onCharTyped(charIn, modifiers);
    }

    @Override
    /**
     * 释放按键后取消对应输入抑制。
     * 当所有打开热键都释放后，搜索框输入恢复正常。
     */
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        this.inputSuppressor.release(keyCode);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    /**
     * 创建顶部分页按钮。
     * 当前激活分页的按钮会禁用，避免重复点击导致列表无意义重建。
     */
    private int createButton(int x, int y, int width, ConfigGuiTab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName());
        button.setEnabled(FastMasaConfigGui.tab != tab);
        this.addButton(button, new ButtonListener(tab, this));
        return button.getWidth();
    }

    /**
     * 构建 All Configs 页的配置包装列表。
     * 扫描结果按 mod/group 插入标题行，每个可支持配置项包装成 ShortcutToggleConfig。
     */
    private static List<ConfigOptionWrapper> buildAllConfigOptions() {
        List<ConfigOptionWrapper> wrappers = new ArrayList<>();
        String lastGroup = "";

        for (ConfigIndexEntry entry : ConfigIndexService.scanSupportedConfigs()) {
            String group = entry.modName() + " / " + entry.groupName();

            if (group.equals(lastGroup) == false) {
                wrappers.add(new ConfigOptionWrapper(group));
                lastGroup = group;
            }

            wrappers.add(new ConfigOptionWrapper(new ShortcutToggleConfig(entry, ConfigIndexService::invalidate)));
        }

        return wrappers;
    }

    private static final class ButtonListener implements IButtonActionListener {
        private final ConfigGuiTab tab;
        private final FastMasaConfigGui parent;

        /**
         * 保存按钮对应的目标分页和所属界面。
         * MaLiLib 的按钮监听器是独立对象，所以需要显式持有 parent 来重建列表。
         */
        private ButtonListener(ConfigGuiTab tab, FastMasaConfigGui parent) {
            this.tab = tab;
            this.parent = parent;
        }

        /**
         * 切换分页并重建配置列表。
         * 切页时会清空 All Configs 缓存、重置滚动条，避免上一页的搜索/滚动位置影响新页。
         */
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            FastMasaConfigGui.tab = this.tab;
            this.parent.allConfigOptions = List.of();
            this.parent.reCreateListWidget();
            WidgetListConfigOptions listWidget = this.parent.getListWidget();

            if (listWidget != null) {
                listWidget.resetScrollbarPosition();
            }

            this.parent.initGui();
        }
    }

    private enum ConfigGuiTab {
        GENERIC("fast-masa-config.gui.tab.generic"),
        SHORTCUTS("fast-masa-config.gui.tab.shortcuts"),
        ALL_CONFIGS("fast-masa-config.gui.tab.all_configs");

        private final String translationKey;

        /**
         * 保存分页标题翻译键。
         * 实际显示文本在渲染前通过 MaLiLib StringUtils 翻译，跟随语言文件变化。
         */
        ConfigGuiTab(String translationKey) {
            this.translationKey = translationKey;
        }

        /**
         * 获取当前语言环境下的分页显示名。
         */
        public String getDisplayName() {
            return StringUtils.translate(this.translationKey);
        }
    }

    private static final class ShortcutStringListConfig extends ConfigStringList {
        /**
         * 创建暴露给 MaLiLib 的快捷方式文本列表配置。
         * 用户可以手工输入 modId/groupId/configName，也可以通过 All Configs 页开关自动维护。
         */
        private ShortcutStringListConfig() {
            super("shortcuts", com.google.common.collect.ImmutableList.of(),
                    "每行一个快捷方式 ID，格式为 modId/groupId/configName 或 modId:configName。",
                    "Shortcuts", "shortcuts");
            this.apply(FastMasaConfig.MOD_ID + ".config.shortcuts");
        }

        /**
         * 从内存 Store 刷新文本列表。
         * 打开 Shortcuts 页前调用，保证列表显示的是最新快捷方式集合。
         */
        private void syncFromStore() {
            super.setStrings(ShortcutConfigStore.toManualIds());
        }

        /**
         * 将文本列表写回内存 Store。
         * 这里会走 ShortcutConfigStore 的解析和去重逻辑，过滤空行并保持目标唯一。
         */
        private void syncToStore() {
            ShortcutConfigStore.replaceWithManualIds(this.getStrings());
        }

        /**
         * MaLiLib 字符串列表编辑器设置新值时调用。
         * 覆盖后立即同步 Store，让快捷面板无需等到关闭配置界面也能读到变化。
         */
        @Override
        public void setStrings(List<String> strings) {
            super.setStrings(strings);
            this.syncToStore();
        }

        /**
         * MaLiLib 列表编辑器在增删行、编辑行内容后会标记 modified。
         * 这里同步 Store，保证所有编辑入口都能更新快捷方式数据。
         */
        @Override
        public void setModified() {
            super.setModified();
            this.syncToStore();
        }
    }

}
