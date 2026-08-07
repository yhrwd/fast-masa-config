package fastui.yure.client.gui;

import fastui.yure.FastMasaConfig;
import fastui.yure.client.input.BoundKeyReader;
import fastui.yure.client.shortcut.ResolvedShortcut;
import fastui.yure.client.shortcut.ShortcutControl;
import fastui.yure.config.ConfigGroup;
import fastui.yure.config.ConfigGroupStore;
import fastui.yure.config.FastMasaConfigs;
import fastui.yure.config.GroupItem;
import fastui.yure.config.MovementKeyPassthrough;
import fastui.yure.config.ShortcutControlType;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.util.KeyCodes;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** 按住热键时显示的非暂停浮动分组窗口。 */
public final class QuickConfigScreen extends Screen {
    private final QuickConfigPanel panel;
    private final List<KeyMapping> movementKeys;
    private MovementKeyPassthrough movementKeyPassthrough = new MovementKeyPassthrough(Set.of());
    private String activeFloatingGroupId;
    private int floatingDragOffsetX;
    private int floatingDragOffsetY;
    private String activeFloatingSliderGroupId;
    private int activeFloatingSliderIndex = -1;
    private boolean floatingDragDirty;
    private boolean redirectToFullConfig;

    public QuickConfigScreen() {
        super(CommonComponents.EMPTY);
        Minecraft mc = Minecraft.getInstance();
        this.panel = new QuickConfigPanel(mc);
        this.movementKeys = List.of(mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight,
                mc.options.keyJump, mc.options.keyShift, mc.options.keySprint);
    }

    @Override
    public void added() {
        ConfigGroupStore.ensureDefaultGroup();
        if (ConfigGroupStore.getGroups().stream().noneMatch(group -> !group.hidden())) {
            // Screen.added() 仍处于 Fabric 事件初始化阶段，不能在这里直接 setScreen。
            // 延迟到首个 tick()，既保留“全隐藏时打开完整配置”的入口，也不会触发未初始化崩溃。
            this.redirectToFullConfig = true;
            return;
        }
        this.movementKeyPassthrough = createMovementPassthrough(Minecraft.getInstance());
        syncHeldMovementKeys();
    }

    @Override
    public void tick() {
        if (this.redirectToFullConfig) {
            this.redirectToFullConfig = false;
            Minecraft.getInstance().setScreenAndShow(new FastMasaConfigGui(null, getHeldOpenHotkeyCodes()));
            return;
        }
        syncHeldMovementKeys();
        if (FastMasaConfigs.Generic.RELEASE_TO_CLOSE.getBooleanValue() && !isOpenHotkeyPhysicallyHeld()) {
            this.onClose();
        }
    }

    @Override
    public void removed() {
        syncHeldMovementKeys();
        flushPendingDragPersistence();
        super.removed();
    }

    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor gfx, int mouseX, int mouseY,
            float delta) {
        this.panel.render(fi.dy.masa.malilib.render.GuiContext.fromGuiGraphics(gfx), this.width, this.height, mouseX,
                mouseY);
    }

    @Override
    public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor gfx, int mouseX, int mouseY,
            float delta) {
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubleClick) {
        return this.handleMouseClicked(click.x(), click.y(), click.input());
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent click, double dragXAmount,
            double dragYAmount) {
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
        int x = (int) mouseX;
        int y = (int) mouseY;
        for (FloatingGroupPanel floating : this.panel.floatingPanels().reversed()) {
            GroupWindowHitTest.Result hit = floating.hitTest(x, y);
            if (hit.target() == GroupWindowHitTest.Target.NONE) {
                continue;
            }
            this.panel.raiseFloatingGroup(floating.groupId());
            if (hit.target() == GroupWindowHitTest.Target.HEADER) {
                if (floating.isCollapseHit(x, y)) {
                    floating.toggleCollapsed();
                    persistRuntimeGroupState();
                } else {
                    this.activeFloatingGroupId = floating.groupId();
                    this.floatingDragOffsetX = x - floating.x();
                    this.floatingDragOffsetY = y - floating.y();
                }
                return true;
            }

            if (shouldOpenSystemConfigRow(hit.target(), floating.isSystemConfigRow(hit.itemIndex()))) {
                Minecraft.getInstance().setScreenAndShow(new FastMasaConfigGui(null, getHeldOpenHotkeyCodes(), floating.groupId()));
                return true;
            }
            ResolvedShortcut shortcut = floating.shortcutAt(hit.itemIndex());
            if (shortcut == null) {
                return true;
            }
            if (hit.target() == GroupWindowHitTest.Target.ROW
                    && ShortcutControl.getControlType(shortcut.configEntry().config()) == ShortcutControlType.TOGGLE) {
                ShortcutControl.toggle(shortcut);
                return true;
            }
            if (hit.target() == GroupWindowHitTest.Target.ROW || hit.target() == GroupWindowHitTest.Target.EXPAND) {
                toggleExpanded(floating.groupId(), floating.groupItemIndexAt(hit.itemIndex()));
                return true;
            }
            if (hit.target() == GroupWindowHitTest.Target.SLIDER) {
                this.activeFloatingSliderGroupId = floating.groupId();
                this.activeFloatingSliderIndex = hit.itemIndex();
                ShortcutControl.setSliderValue(shortcut, floating.sliderRatioAt(hit.itemIndex(), x));
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean handleMouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (this.activeFloatingGroupId != null) {
            this.floatingDragDirty |= this.panel.moveFloatingGroup(this.activeFloatingGroupId,
                    (int) mouseX - this.floatingDragOffsetX, (int) mouseY - this.floatingDragOffsetY,
                    this.width, this.height);
            return true;
        }
        if (this.activeFloatingSliderGroupId != null) {
            for (FloatingGroupPanel floating : this.panel.floatingPanels()) {
                if (floating.groupId().equals(this.activeFloatingSliderGroupId)) {
                    ResolvedShortcut shortcut = floating.shortcutAt(this.activeFloatingSliderIndex);
                    if (shortcut != null) {
                        ShortcutControl.setSliderValue(shortcut,
                                floating.sliderRatioAt(this.activeFloatingSliderIndex, (int) mouseX));
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean handleMouseScrolled(double mouseX, double mouseY, double h, double v) {
        for (FloatingGroupPanel floating : this.panel.floatingPanels().reversed()) {
            GroupWindowHitTest.Result hit = floating.hitTest((int) mouseX, (int) mouseY);
            if (hit.target() != GroupWindowHitTest.Target.NONE && hit.target() != GroupWindowHitTest.Target.HEADER) {
                floating.scroll(v);
                return true;
            }
        }
        return false;
    }

    public boolean handleMouseReleased(double mouseX, double mouseY, int button) {
        this.activeFloatingGroupId = null;
        this.activeFloatingSliderGroupId = null;
        this.activeFloatingSliderIndex = -1;
        flushPendingDragPersistence();
        return false;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        Minecraft mc = Minecraft.getInstance();
        if (!FastMasaConfigs.Generic.RELEASE_TO_CLOSE.getBooleanValue() && isOpenHotkeyPressedAgain(keyCode)) {
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
        syncHeldMovementKeys();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private void toggleExpanded(String groupId, int itemIndex) {
        ConfigGroupStore.get(groupId).ifPresent(group -> {
            if (isValidToggleItemIndex(itemIndex, group.items().size())) {
                GroupItem item = group.items().get(itemIndex);
                if (ConfigGroupStore.setItemExpanded(group.id(), itemIndex, !item.expanded())) {
                    persistRuntimeGroupState();
                }
            }
        });
    }

    private static void persistRuntimeGroupState() {
        ConfigManager.getInstance().onConfigsChanged(FastMasaConfig.MOD_ID);
    }

    private void flushPendingDragPersistence() {
        if (shouldFlushPendingDrag(this.floatingDragDirty)) {
            persistRuntimeGroupState();
            this.floatingDragDirty = false;
        }
    }

    static boolean shouldFlushPendingDrag(boolean positionChanged) {
        return positionChanged;
    }

    static boolean shouldOpenSystemConfigRow(GroupWindowHitTest.Target target, boolean systemConfigRow) {
        return target == GroupWindowHitTest.Target.ROW && systemConfigRow;
    }

    static boolean isValidToggleItemIndex(int itemIndex, int itemCount) {
        return itemIndex >= 0 && itemIndex < itemCount;
    }

    private static MovementKeyPassthrough createMovementPassthrough(Minecraft mc) {
        return new MovementKeyPassthrough(normalizeMovementKeyCodes(List.of(BoundKeyReader.getBoundKeyCode(mc.options.keyUp),
                BoundKeyReader.getBoundKeyCode(mc.options.keyDown), BoundKeyReader.getBoundKeyCode(mc.options.keyLeft),
                BoundKeyReader.getBoundKeyCode(mc.options.keyRight), BoundKeyReader.getBoundKeyCode(mc.options.keyJump),
                BoundKeyReader.getBoundKeyCode(mc.options.keyShift), BoundKeyReader.getBoundKeyCode(mc.options.keySprint))));
    }

    static Set<Integer> normalizeMovementKeyCodes(List<Integer> keyCodes) {
        Set<Integer> normalized = new LinkedHashSet<>();
        for (Integer keyCode : keyCodes) {
            if (keyCode != null) {
                normalized.add(keyCode);
            }
        }
        return Set.copyOf(normalized);
    }

    private void syncHeldMovementKeys() {
        for (KeyMapping movementKey : this.movementKeys) {
            movementKey.setDown(KeybindMulti.isKeyDown(BoundKeyReader.getBoundKeyCode(movementKey)));
        }
    }

    private boolean isOpenHotkeyPhysicallyHeld() {
        for (int keyCode : FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getKeys()) {
            if (!KeybindMulti.isKeyDown(keyCode)) {
                return false;
            }
        }
        return true;
    }

    private boolean isOpenHotkeyPressedAgain(int pressedKeyCode) {
        List<Integer> keys = FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getKeys();
        if (!keys.contains(pressedKeyCode)) {
            return false;
        }
        for (int keyCode : keys) {
            if (keyCode != pressedKeyCode && !KeybindMulti.isKeyDown(keyCode)) {
                return false;
            }
        }
        return true;
    }

    private static Set<Integer> getHeldOpenHotkeyCodes() {
        return FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getKeys().stream()
                .filter(KeybindMulti::isKeyDown).collect(Collectors.toSet());
    }

    private void setMovementKeyPressed(int keyCode, int scanCode, boolean pressed) {
        for (KeyMapping movementKey : this.movementKeys) {
            if (movementKey.matches(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, 0))) {
                movementKey.setDown(pressed);
            }
        }
    }
}
