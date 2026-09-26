package fastui.yure.client.init;

import fastui.yure.FastMasaConfig;
import fastui.yure.client.gui.FastMasaConfigGui;
import fastui.yure.client.input.FastMasaInputHandler;
import fastui.yure.config.FastMasaConfigHandler;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;

public final class FastMasaClientInitHandler implements IInitializationHandler {
    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(FastMasaConfig.MOD_ID, new FastMasaConfigHandler());
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(new ModInfo(FastMasaConfig.MOD_ID, "Fast Masa Config", FastMasaConfigGui::new));
        InputEventHandler.getKeybindManager().registerKeybindProvider(FastMasaInputHandler.getInstance());
        FastMasaInputHandler.getInstance().initCallbacks();
    }
}
