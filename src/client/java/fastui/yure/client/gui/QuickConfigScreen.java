package fastui.yure.client.gui;

import fastui.yure.client.input.BoundKeyReader;
import fastui.yure.client.index.ConfigIndexService;
import fastui.yure.client.shortcut.ResolvedShortcut;
import fastui.yure.client.shortcut.ShortcutControl;
import fastui.yure.client.shortcut.ShortcutResolver;
import fastui.yure.config.FastMasaConfigs;
import fastui.yure.config.MovementKeyPassthrough;
import fastui.yure.config.ShortcutConfigStore;
import fastui.yure.config.ShortcutControlType;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.util.KeyCodes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.CommonComponents;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 快捷配置面板 Screen (26.x Mojang mappings).
 * 不暂停游戏、不绘制背景。
 * 26.x Screen API 移除了 mouseClicked/mouseDragged 等覆写点，
 * 鼠标交互通过 handleMouseXxx() 供外部输入系统手动调用。
 */
public final class QuickConfigScreen extends Screen {
    private final QuickConfigPanel panel;
    private final List<KeyMapping> movementKeys;
    private List<QuickPanelItem> items = List.of();
    private MovementKeyPassthrough movementKeyPassthrough = new MovementKeyPassthrough(Set.of());
    private int activeSliderIndex = -1;
    private int scrollOffset;
    private QuickConfigPanel.PanelMode panelMode = QuickConfigPanel.PanelMode.SHORTCUTS;

    public QuickConfigScreen() {
        super(CommonComponents.EMPTY);
        Minecraft mc = Minecraft.getInstance();
        this.panel = new QuickConfigPanel(mc);
        this.movementKeys = List.of(
                mc.options.keyUp, mc.options.keyDown,
                mc.options.keyLeft, mc.options.keyRight,
                mc.options.keyJump, mc.options.keyShift, mc.options.keySprint);
    }

    @Override
    public void added() {
        refreshShortcuts();
        this.movementKeyPassthrough = createMovementPassthrough(Minecraft.getInstance());
        syncHeldMovementKeys();
    }

    @Override
    public void tick() {
        if (FastMasaConfigs.Generic.RELEASE_TO_CLOSE.getBooleanValue()
                && !isOpenHotkeyPhysicallyHeld()) {
            this.onClose();
        }
    }

    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor gfx, int mouseX, int mouseY,
            float delta) {
        this.panel.render(fi.dy.masa.malilib.render.GuiContext.fromGuiGraphics(gfx), this.width, this.height, mouseX,
                mouseY,
                this.items, this.scrollOffset, this.panelMode);
    }

    @Override
    public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor gfx, int mouseX, int mouseY,
            float delta) {
    }

    // --- 26.x 鼠标事件覆写 ---

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubleClick) {
        return this.handleMouseClicked(click.x(), click.y(), click.input());
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent click, double dragXAmount, double dragYAmount) {
        return this.handleMouseDragged(click.x(), click.y(), click.input(), dragXAmount, dragYAmount);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent click) {
        return this.handleMouseReleased(click.x(), click.y(), click.input());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return this.handleMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX, y = (int) mouseY;
        if (this.panel.isSettingsButtonHovered(x, y)) {
            Minecraft.getInstance().setScreen(new FastMasaConfigGui(null, getHeldOpenHotkeyCodes()));
            return true;
        }
        QuickConfigPanel.PanelMode mode = this.panel.getModeAt(x, y);
        if (mode != null) {
            this.panelMode = mode;
            this.scrollOffset = 0;
            refreshShortcuts();
            return true;
        }
        int index = this.panel.getShortcutIndexAt(x, y, this.items.size());
        if (index >= 0) {
            QuickPanelItem item = this.items.get(index);
            ResolvedShortcut sc = new ResolvedShortcut(item.shortcut(), item.configEntry());
            if (ShortcutControl.getControlType(sc.configEntry().config()) == ShortcutControlType.TOGGLE) {
                ShortcutControl.toggle(sc);
                if (this.panelMode == QuickConfigPanel.PanelMode.ENABLED_BOOLEANS)
                    refreshShortcuts();
            } else {
                this.activeSliderIndex = index;
                ShortcutControl.setSliderValue(sc, this.panel.getSliderRatioAt(x, index));
            }
            return true;
        }
        return false;
    }

    public boolean handleMouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (this.activeSliderIndex >= 0 && this.activeSliderIndex < this.items.size()) {
            QuickPanelItem item = this.items.get(this.activeSliderIndex);
            ShortcutControl.setSliderValue(new ResolvedShortcut(item.shortcut(), item.configEntry()),
                    this.panel.getSliderRatioAt((int) mouseX, this.activeSliderIndex));
            return true;
        }
        return false;
    }

    public boolean handleMouseScrolled(double mouseX, double mouseY, double h, double v) {
        int next = this.panel.scroll(this.scrollOffset, v);
        if (next != this.scrollOffset) {
            this.scrollOffset = next;
            return true;
        }
        return false;
    }

    public boolean handleMouseReleased(double mouseX, double mouseY, int button) {
        this.activeSliderIndex = -1;
        return false;
    }

    // --- 键盘事件 (26.x: 仅 keyPressed 可覆写) ---

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        Minecraft mc = Minecraft.getInstance();

        if (!FastMasaConfigs.Generic.RELEASE_TO_CLOSE.getBooleanValue()
                && isOpenHotkeyPressedAgain(keyCode)) {
            this.onClose();
            return true;
        }
        if (this.movementKeyPassthrough.shouldPassThrough(keyCode)) {
            setMovementKeyPressed(keyCode, scanCode, true);
            return false;
        }
        if (FastMasaConfigs.Generic.CLOSE_ON_INVENTORY_KEY.getBooleanValue()
                && mc.options.keyInventory.matches(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, 0))) {
            this.onClose();
            return true;
        }
        if (keyCode == KeyCodes.KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void afterKeyboardAction() {
        for (KeyMapping mk : this.movementKeys) {
            int code = BoundKeyReader.getBoundKeyCode(mk);
            if (this.movementKeyPassthrough.shouldPassThrough(code)) {
                mk.setDown(KeybindMulti.isKeyDown(code));
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    // --- 内部 ---

    private void refreshShortcuts() {
        if (this.panelMode == QuickConfigPanel.PanelMode.ENABLED_BOOLEANS) {
            this.items = ConfigIndexService.scanSupportedConfigs().stream()
                    .filter(e -> e.config().getType() == ConfigType.BOOLEAN)
                    .filter(e -> e.config() instanceof IConfigBoolean b && b.getBooleanValue())
                    .map(QuickPanelItem::fromEnabledConfig).toList();
        } else {
            this.items = ShortcutResolver.resolve(ShortcutConfigStore.getEntries()).stream()
                    .map(QuickPanelItem::fromShortcut).toList();
        }
        this.scrollOffset = Math.min(this.scrollOffset, Math.max(0, this.items.size() - 1));
    }

    private static MovementKeyPassthrough createMovementPassthrough(Minecraft mc) {
        if (mc == null)
            return new MovementKeyPassthrough(Set.of());
        return new MovementKeyPassthrough(Set.of(
                BoundKeyReader.getBoundKeyCode(mc.options.keyUp),
                BoundKeyReader.getBoundKeyCode(mc.options.keyDown),
                BoundKeyReader.getBoundKeyCode(mc.options.keyLeft),
                BoundKeyReader.getBoundKeyCode(mc.options.keyRight),
                BoundKeyReader.getBoundKeyCode(mc.options.keyJump),
                BoundKeyReader.getBoundKeyCode(mc.options.keyShift),
                BoundKeyReader.getBoundKeyCode(mc.options.keySprint)));
    }

    private void syncHeldMovementKeys() {
        for (KeyMapping mk : this.movementKeys) {
            mk.setDown(KeybindMulti.isKeyDown(BoundKeyReader.getBoundKeyCode(mk)));
        }
    }

    private boolean isOpenHotkeyPhysicallyHeld() {
        for (int kc : FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getKeys()) {
            if (!KeybindMulti.isKeyDown(kc))
                return false;
        }
        return true;
    }

    private boolean isOpenHotkeyPressedAgain(int pressedKeyCode) {
        List<Integer> keys = FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getKeys();
        if (!keys.contains(pressedKeyCode))
            return false;
        for (int kc : keys) {
            if (kc != pressedKeyCode && !KeybindMulti.isKeyDown(kc))
                return false;
        }
        return true;
    }

    private static Set<Integer> getHeldOpenHotkeyCodes() {
        return FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getKeys().stream()
                .filter(code -> KeybindMulti.isKeyDown(code)).collect(Collectors.toSet());
    }

    private void setMovementKeyPressed(int keyCode, int scanCode, boolean pressed) {
        for (KeyMapping mk : this.movementKeys) {
            if (mk.matches(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, 0)))
                mk.setDown(pressed);
        }
    }
}
