package fastui.yure.client;

import fastui.yure.client.init.FastMasaClientInitHandler;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class FastMasaConfigClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		InitializationHandler.getInstance().registerInitializationHandler(new FastMasaClientInitHandler());
		// /fastmasaconfig scan|csv 是排障诊断命令，只在开发环境注册，不随发行版暴露。
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			MasaConfigProbe.register();
		}
	}
}
