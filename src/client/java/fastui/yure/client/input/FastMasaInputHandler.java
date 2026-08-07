package fastui.yure.client.input;

import fastui.yure.client.gui.QuickConfigScreen;
import fastui.yure.config.FastMasaConfigs;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class FastMasaInputHandler implements IKeybindProvider {
    private static final FastMasaInputHandler INSTANCE = new FastMasaInputHandler();
    private final HotkeyReleaseGate quickConfigReleaseGate = new HotkeyReleaseGate();

    private FastMasaInputHandler() {
    }

    public static FastMasaInputHandler getInstance() {
        return INSTANCE;
    }

    public void initCallbacks() {
        FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().setCallback(new QuickConfigCallback());
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (IHotkey hotkey : FastMasaConfigs.Generic.HOTKEY_LIST) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory("Fast Masa Config", "fast-masa-config.hotkeys.category.quick",
                FastMasaConfigs.Generic.HOTKEY_LIST);
    }

    public List<IHotkey> getHotkeys() {
        return FastMasaConfigs.Generic.HOTKEY_LIST;
    }

    public void tick() {
        this.quickConfigReleaseGate.refresh(QuickConfigCallback.getHeldOpenHotkeyCodes());
    }

    private static final class QuickConfigCallback implements IHotkeyCallback {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key) {
            Minecraft client = Minecraft.getInstance();
            Set<Integer> heldKeys = getHeldOpenHotkeyCodes();

            Object currentScreen = getCurrentScreen(client);
            if (action == KeyAction.PRESS && currentScreen instanceof QuickConfigScreen screen) {
                if (FastMasaConfigs.Generic.RELEASE_TO_CLOSE.getBooleanValue() == false) {
                    screen.onClose();
                    return true;
                }
                return true;
            }

            if (action == KeyAction.PRESS && currentScreen instanceof QuickConfigScreen == false) {
                if (INSTANCE.quickConfigReleaseGate.isBlocked(heldKeys)) {
                    return true;
                }
                INSTANCE.quickConfigReleaseGate.arm(Set.copyOf(
                        FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getKeys()));
                client.setScreenAndShow(new QuickConfigScreen());
                return true;
            }

            return false;
        }

        private static Object getCurrentScreen(Minecraft client) {
            try {
                return Minecraft.class.getMethod("screen").invoke(client);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        private static Set<Integer> getHeldOpenHotkeyCodes() {
            return FastMasaConfigs.Generic.OPEN_QUICK_CONFIG.getKeybind().getKeys().stream()
                    .filter(fi.dy.masa.malilib.hotkeys.KeybindMulti::isKeyDown)
                    .collect(Collectors.toSet());
        }
    }
}
