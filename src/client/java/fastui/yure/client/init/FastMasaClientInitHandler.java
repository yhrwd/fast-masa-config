package fastui.yure.client.init;

import fastui.yure.FastMasaConfig;
import fastui.yure.client.gui.FastMasaConfigGui;
import fastui.yure.client.input.FastMasaInputHandler;
import fastui.yure.client.render.BlockBreakIndicator;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import fastui.yure.config.FastMasaConfigHandler;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;

public final class FastMasaClientInitHandler implements IInitializationHandler {
    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(FastMasaConfig.MOD_ID, new FastMasaConfigHandler());
        FastMasaConfigGui.registerConfigScreen();
        InputEventHandler.getKeybindManager().registerKeybindProvider(FastMasaInputHandler.getInstance());
        FastMasaInputHandler.getInstance().initCallbacks();
        ClientTickEvents.END_CLIENT_TICK.register(client -> FastMasaInputHandler.getInstance().tick());
        LevelRenderEvents.END_MAIN.register(context -> BlockBreakIndicator.render(context.levelState()));
    }
}
