package fastui.yure.client.input;

import com.google.common.collect.ImmutableList;
import fastui.yure.client.gui.QuickConfigScreen;
import fastui.yure.config.FastMasaConfigs;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.minecraft.client.MinecraftClient;

import java.util.List;

public final class FastMasaInputHandler implements IKeybindProvider {
    private static final FastMasaInputHandler INSTANCE = new FastMasaInputHandler();

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
        manager.addHotkeysForCategory("Fast Masa Config", "fast-masa-config.hotkeys.category.quick", ImmutableList.copyOf(FastMasaConfigs.Generic.HOTKEY_LIST));
    }

    public List<IHotkey> getHotkeys() {
        return FastMasaConfigs.Generic.HOTKEY_LIST;
    }

    private static final class QuickConfigCallback implements IHotkeyCallback {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key) {
            MinecraftClient client = MinecraftClient.getInstance();

            if (action == KeyAction.PRESS && client.currentScreen instanceof QuickConfigScreen screen) {
                if (FastMasaConfigs.Generic.RELEASE_TO_CLOSE.getBooleanValue() == false) {
                    screen.close();
                    return true;
                }
            }

            if (action == KeyAction.PRESS && client.currentScreen instanceof QuickConfigScreen == false) {
                client.setScreen(new QuickConfigScreen());
                return true;
            }

            return false;
        }
    }
}
