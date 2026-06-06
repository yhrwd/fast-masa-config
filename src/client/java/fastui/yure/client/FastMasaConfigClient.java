package fastui.yure.client;

import fastui.yure.client.init.FastMasaClientInitHandler;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;

public class FastMasaConfigClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		InitializationHandler.getInstance().registerInitializationHandler(new FastMasaClientInitHandler());
		MasaConfigProbe.register();
	}
}
