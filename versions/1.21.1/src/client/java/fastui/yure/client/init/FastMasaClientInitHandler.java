package fastui.yure.client.init;

import fastui.yure.FastMasaConfig;
import fastui.yure.client.input.FastMasaInputHandler;
import fastui.yure.config.FastMasaConfigHandler;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;

public final class FastMasaClientInitHandler implements IInitializationHandler {
    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(FastMasaConfig.MOD_ID, new FastMasaConfigHandler());
        InputEventHandler.getKeybindManager().registerKeybindProvider(FastMasaInputHandler.getInstance());
        FastMasaInputHandler.getInstance().initCallbacks();
    }
}
