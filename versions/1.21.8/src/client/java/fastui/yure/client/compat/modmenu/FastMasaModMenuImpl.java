package fastui.yure.client.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import fastui.yure.client.gui.FastMasaConfigGui;

public final class FastMasaModMenuImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new FastMasaConfigGui().setParent(parent);
    }
}
