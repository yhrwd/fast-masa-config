package fastui.yure.client.input;

import fastui.yure.FastMasaConfig;
import fastui.yure.client.gui.QuickConfigScreen;
import fastui.yure.config.FastMasaConfigs;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IMouseInputHandler;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.Set;

public final class FastMasaInputHandler implements IKeybindProvider, IMouseInputHandler, IKeyboardInputHandler {
    private static final FastMasaInputHandler INSTANCE = new FastMasaInputHandler();
    private final HotkeyReleaseGate quickConfigReleaseGate = new HotkeyReleaseGate();
    private final HeldInputState heldInput = new HeldInputState();

    private FastMasaInputHandler() {
    }

    public static FastMasaInputHandler getInstance() {
        return INSTANCE;
    }

    public void initCallbacks() {
        FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().setCallback(new QuickConfigCallback());
        InputEventHandler.getInputManager().registerMouseInputHandler(this);
        InputEventHandler.getInputManager().registerKeyboardInputHandler(this);
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (IHotkey hotkey : FastMasaConfigs.Generic.HOTKEY_LIST) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(FastMasaConfig.MOD_NAME, "fast-masa-config.hotkeys.category.quick",
                FastMasaConfigs.Generic.HOTKEY_LIST);
    }

    public List<IHotkey> getHotkeys() {
        return FastMasaConfigs.Generic.HOTKEY_LIST;
    }

    public void tick() {
        updateFocus();
        this.quickConfigReleaseGate.refresh(getHeldOpenHotkeyCodes());
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY, int button, boolean pressed) {
        updateFocus();
        this.heldInput.onMouseButton(button, pressed);
        // Releases are observed even when the hotkey callback is configured for PRESS only.
        this.quickConfigReleaseGate.refresh(getHeldOpenHotkeyCodes());
        return false;
    }

    @Override
    public boolean onKeyInput(int keyCode, int scanCode, int modifiers, boolean pressed) {
        tick();
        return false;
    }

    private void updateFocus() {
        this.heldInput.setFocused(MinecraftClient.getInstance().isWindowFocused());
    }

    public boolean isOpenHotkeyPhysicallyHeld() {
        updateFocus();
        return this.heldInput.isChordHeld(FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getKeys(),
                KeybindMulti::isKeyDown);
    }

    public boolean isOpenHotkeyPressedAgain(int pressedKeyCode) {
        updateFocus();
        return this.heldInput.isChordPressedAgain(FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getKeys(),
                pressedKeyCode, KeybindMulti::isKeyDown);
    }

    public Set<Integer> getHeldOpenHotkeyCodes() {
        updateFocus();
        return this.heldInput.heldKeys(FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getKeys(),
                KeybindMulti::isKeyDown);
    }

    private static final class QuickConfigCallback implements IHotkeyCallback {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key) {
            MinecraftClient client = MinecraftClient.getInstance();
            INSTANCE.updateFocus();
            if (action != KeyAction.PRESS || !client.isWindowFocused()) {
                return false;
            }

            if (client.currentScreen instanceof QuickConfigScreen screen) {
                if (FastMasaConfigs.Generic.RELEASE_TO_CLOSE.getBooleanValue() == false) {
                    screen.close();
                    return true;
                }
                return true;
            }

            if (client.currentScreen instanceof QuickConfigScreen == false) {
                // MaLiLib calls hotkey callbacks before raw listeners update the activating mouse press.
                if (INSTANCE.quickConfigReleaseGate.isBlocked(Set.copyOf(key.getKeys()))) {
                    return true;
                }
                INSTANCE.quickConfigReleaseGate.arm(Set.copyOf(key.getKeys()));
                client.setScreen(new QuickConfigScreen());
                return true;
            }

            return false;
        }
    }
}
