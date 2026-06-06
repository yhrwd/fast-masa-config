package fastui.yure.client;

import net.fabricmc.api.ClientModInitializer;

public class FastMasaConfigClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MasaConfigProbe.register();
	}
}
