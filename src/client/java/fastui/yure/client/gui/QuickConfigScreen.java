package fastui.yure.minecraft.gui;

import fastui.yure.minecraft.input.BoundKeyReader;
import fastui.yure.minecraft.index.ConfigIndexService;
import fastui.yure.minecraft.shortcut.ResolvedShortcut;
import fastui.yure.minecraft.shortcut.ShortcutControl;
import fastui.yure.minecraft.shortcut.ShortcutResolver;
import fastui.yure.config.FastMasaConfigs;
import fastui.yure.config.MovementKeyPassthrough;
import fastui.yure.config.ShortcutConfigStore;
import fastui.yure.config.ShortcutControlType;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.util.KeyCodes;
import net.minecraft.minecraft.Minecraft;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.minecraft.gui.screens.Screen;
import net.minecraft.minecraft.KeyMapping;
import net.minecraft.network.chat.CommonComponents;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 快捷配置面板 Screen。
 * 这个 Screen 不暂停游戏、不绘制背景，主要负责把键鼠事件转发给 QuickConfigPanel 和快捷项控制逻辑。
 */
public final class QuickConfigScreen extends Screen {
    private final QuickConfigPanel panel;
    private final List<KeyMapping> movementKeys;
    private List<QuickPanelItem> items = List.of();
    private MovementKeyPassthrough movementKeyPassthrough = new MovementKeyPassthrough(java.util.Set.of());
    private int activeSliderIndex = -1;
    private int scrollOffset;
    private QuickConfigPanel.PanelMode panelMode = QuickConfigPanel.PanelMode.SHORTCUTS;

    /**
     * 创建快捷面板并记录需要透传的移动键。
     * 打开面板后玩家仍可能按着移动键，所以这些 KeyMapping 需要在 Screen 里继续同步状态。
     */
    public QuickConfigScreen() {
        super(CommonComponents.EMPTY);
        this.panel = new QuickConfigPanel(Minecraft.getInstance());
        this.movementKeys = List.of(
                Minecraft.getInstance().options.forwardKey,
                Minecraft.getInstance().options.backKey,
                Minecraft.getInstance().options.leftKey,
                Minecraft.getInstance().options.rightKey,
                Minecraft.getInstance().options.jumpKey,
                Minecraft.getInstance().options.sneakKey,
                Minecraft.getInstance().options.sprintKey);
    }

    @Override
    /**
     * 初始化时解析当前快捷方式并同步移动键状态。
     * 快捷方式 Store 可能在全屏 UI 中被修改，因此每次打开都重新 resolve。
     */
    protected void init() {
        this.refreshShortcuts();
        this.movementKeyPassthrough = createMovementPassthrough(this.client);
        this.syncHeldMovementKeys();
    }

    @Override
    /**
     * 每 tick 检查“松开关闭”逻辑。
     * MaLiLib 的 keybind 状态在 Screen 打开后不可靠，所以这里读物理按键状态。
     */
    public void tick() {
        if (FastMasaConfigs.Generic.RELEASE_TO_CLOSE.getBooleanValue() &&
                isOpenHotkeyPhysicallyHeld() == false) {
            this.onClose();
        }
    }

    @Override
    /**
     * 渲染快捷面板本体。
     * 所有布局计算在 QuickConfigPanel 内完成，Screen 只传入当前窗口尺寸和鼠标状态。
     */
    public void extractRenderState(GuiContext context, int mouseX, int mouseY, float delta) {
        this.panel.render(context, this.width, this.height, mouseX, mouseY, this.items, this.scrollOffset,
                this.panelMode);
    }

    @Override
    /**
     * 不绘制默认背景。
     * 快捷面板是游戏内叠层，保留世界画面能减少“打开菜单”的割裂感。
     */
    public void extractBackground(GuiContext context, int mouseX, int mouseY, float delta) {
        // 快捷弹层不绘制背景，不触发 vanilla 菜单背景/模糊效果。
    }

    @Override
    /**
     * 处理面板点击。
     * 设置按钮会进入全屏 UI；快捷项点击会根据类型切换布尔值或开始拖动滑条。
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX;
        int y = (int) mouseY;

        if (this.panel.isSettingsButtonHovered(x, y)) {
            // 人手松开热键有延迟，进入全屏 UI 时先记录仍按住的打开键，交给全屏页吞掉首轮输入。
            Minecraft.getInstance().setScreen(new FastMasaConfigGui(null, getHeldOpenHotkeyCodes()));
            return true;
        }

        QuickConfigPanel.PanelMode mode = this.panel.getModeAt(x, y);

        if (mode != null) {
            this.panelMode = mode;
            this.scrollOffset = 0;
            this.refreshShortcuts();
            return true;
        }

        int index = this.panel.getShortcutIndexAt(x, y, this.items.size());

        if (index >= 0) {
            QuickPanelItem item = this.items.get(index);
            ResolvedShortcut shortcut = new ResolvedShortcut(item.shortcut(), item.configEntry());

            if (ShortcutControl.getControlType(shortcut.configEntry().config()) == ShortcutControlType.TOGGLE) {
                ShortcutControl.toggle(shortcut);

                if (this.panelMode == QuickConfigPanel.PanelMode.ENABLED_BOOLEANS) {
                    this.refreshShortcuts();
                }
            } else {
                this.activeSliderIndex = index;
                ShortcutControl.setSliderValue(shortcut, this.panel.getSliderRatioAt(x, index));
            }

            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    /**
     * 拖动当前激活的滑条。
     * activeSliderIndex 在鼠标按下数值快捷项时设置，释放鼠标后清空。
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.activeSliderIndex >= 0 && this.activeSliderIndex < this.items.size()) {
            QuickPanelItem item = this.items.get(this.activeSliderIndex);
            ShortcutControl.setSliderValue(new ResolvedShortcut(item.shortcut(), item.configEntry()),
                    this.panel.getSliderRatioAt((int) mouseX, this.activeSliderIndex));
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    /**
     * 处理快捷面板滚轮。
     * 只有滚动偏移实际变化时才消费事件，否则交回父类处理。
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int nextOffset = this.panel.scroll(this.scrollOffset, verticalAmount);

        if (nextOffset != this.scrollOffset) {
            this.scrollOffset = nextOffset;
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    /**
     * 鼠标释放时结束滑条拖动。
     * 不区分按钮类型，避免异常情况下滑条一直保持激活。
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.activeSliderIndex = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    /**
     * 处理键盘按下事件。
     * 移动键透传给游戏；背包键和 ESC 用于关闭面板，其它按键维持原 Screen 行为。
     */
    public boolean keyPressed(com.mojang.blaze3d.platform.InputConstants.Key key, int scanCode, int modifiers) {
        if (FastMasaConfigs.Generic.RELEASE_TO_CLOSE.getBooleanValue() == false
                && this.isOpenHotkeyPressedAgain(keyCode)) {
            this.onClose();
            return true;
        }

        if (this.movementKeyPassthrough.shouldPassThrough(keyCode)) {
            this.setMovementKeyPressed(keyCode, scanCode, true);
            return false;
        }

        if (FastMasaConfigs.Generic.CLOSE_ON_INVENTORY_KEY.getBooleanValue()
                && Minecraft.getInstance().options.inventoryKey.matchesKey(keyCode, scanCode)) {
            this.onClose();
            return true;
        }

        if (keyCode == KeyCodes.KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    /**
     * 处理键盘释放事件。
     * 移动键释放也要透传到 KeyMapping，否则关闭面板后可能出现卡键。
     */
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (this.movementKeyPassthrough.shouldPassThrough(keyCode)) {
            this.setMovementKeyPressed(keyCode, scanCode, false);
            return false;
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    /**
     * 快捷叠层不暂停游戏。
     * 这样玩家可以按住面板快速切配置，同时游戏逻辑继续运行。
     */
    public boolean shouldPause() {
        return false;
    }

    @Override
    /**
     * 保留 ESC 关闭能力。
     * 具体关闭逻辑也在 keyPressed 中处理，用于兼容 MaLiLib 的 KeyCodes 判断。
     */
    public boolean shouldCloseOnEsc() {
        return true;
    }

    /**
     * 从持久化 Store 重新解析可用快捷项。
     * 解析失败的条目会在 ShortcutResolver 中被过滤，因此面板只显示当前能操作的配置。
     */
    private void refreshShortcuts() {
        if (this.panelMode == QuickConfigPanel.PanelMode.ENABLED_BOOLEANS) {
            this.items = ConfigIndexService.scanSupportedConfigs().stream()
                    .filter(entry -> entry.config().getType() == ConfigType.BOOLEAN)
                    .filter(entry -> entry.config() instanceof IConfigBoolean booleanConfig
                            && booleanConfig.getBooleanValue())
                    .map(QuickPanelItem::fromEnabledConfig)
                    .toList();
        } else {
            this.items = ShortcutResolver.resolve(ShortcutConfigStore.getEntries()).stream()
                    .map(QuickPanelItem::fromShortcut)
                    .toList();
        }

        this.scrollOffset = Math.min(this.scrollOffset, Math.max(0, this.items.size() - 1));
    }

    /**
     * 构造移动键透传白名单。
     * client 为空时返回空白名单，便于测试或异常初始化路径安全退出。
     */
    private static MovementKeyPassthrough createMovementPassthrough(Minecraft client) {
        if (client == null) {
            return new MovementKeyPassthrough(java.util.Set.of());
        }

        return new MovementKeyPassthrough(java.util.Set.of(
                BoundKeyReader.getBoundKeyCode(client.options.forwardKey),
                BoundKeyReader.getBoundKeyCode(client.options.backKey),
                BoundKeyReader.getBoundKeyCode(client.options.leftKey),
                BoundKeyReader.getBoundKeyCode(client.options.rightKey),
                BoundKeyReader.getBoundKeyCode(client.options.jumpKey),
                BoundKeyReader.getBoundKeyCode(client.options.sneakKey),
                BoundKeyReader.getBoundKeyCode(client.options.sprintKey)));
    }

    /**
     * 打开 Screen 后同步已经按住的移动键。
     * 没有这一步时，玩家按住前进键打开面板会突然停下。
     */
    private void syncHeldMovementKeys() {
        for (KeyMapping keyBinding : this.movementKeys) {
            keyBinding.setPressed(KeybindMulti.isKeyDown(BoundKeyReader.getBoundKeyCode(keyBinding)));
        }
    }

    /**
     * 检查打开快捷面板的组合键是否仍被物理按住。
     * 这里不能依赖 MaLiLib 的 isKeybindHeld，因为 Screen 打开后它的上下文状态会变化。
     */
    private boolean isOpenHotkeyPhysicallyHeld() {
        for (int keyCode : FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getKeys()) {
            if (KeybindMulti.isKeyDown(keyCode) == false) {
                return false;
            }
        }

        return true;
    }

    private boolean isOpenHotkeyPressedAgain(int pressedKeyCode) {
        List<Integer> keyCodes = FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getKeys();

        if (keyCodes.contains(pressedKeyCode) == false) {
            return false;
        }

        for (int keyCode : keyCodes) {
            if (keyCode != pressedKeyCode && KeybindMulti.isKeyDown(keyCode) == false) {
                return false;
            }
        }

        return true;
    }

    /**
     * 收集进入全屏 UI 时仍按住的打开热键。
     * 全屏 UI 会临时吞掉这些按键对应的字符事件，防止搜索框被自动输入。
     */
    private static Set<Integer> getHeldOpenHotkeyCodes() {
        return FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getKeys().stream()
                .filter(keyCode -> KeybindMulti.isKeyDown(keyCode))
                .collect(Collectors.toSet());
    }

    /**
     * 将键盘事件同步到 Minecraft 原版移动 KeyMapping。
     * 使用 matchesKey 同时匹配 keyCode 和 scanCode，兼容用户改键后的绑定。
     */
    private void setMovementKeyPressed(int keyCode, int scanCode, boolean pressed) {
        for (KeyMapping movementKey : this.movementKeys) {
            if (movementKey.matchesKey(keyCode, scanCode)) {
                movementKey.setPressed(pressed);
            }
        }
    }
}
