package fastui.yure.client.compat;

import fi.dy.masa.malilib.util.input.KeyCodes;
import net.minecraft.client.Minecraft;

/**
 * Isolates MaLiLib API surface that changed between versions so the shared GUI
 * code compiles for every build target. Each target overrides this class when
 * its MaLiLib line differs (see versions/&lt;target&gt;/src).
 */
public final class TargetCompat
{
    private TargetCompat() {}

    public static final int KEY_LEFT_SHIFT = KeyCodes.KEY_LEFT_SHIFT;
    public static final int KEY_RETURN = KeyCodes.KEY_RETURN;
    public static final int KEY_KP_ENTER = KeyCodes.KEY_KP_ENTER;
    public static final int KEY_RETURN2 = KeyCodes.KEY_RETURN2;
    public static final int KEY_ESCAPE = KeyCodes.KEY_ESCAPE;

    public static void startTextInput(Minecraft minecraft, Object owner)
    {
        minecraft.textInputManager().startTextInput(owner);
    }

    public static net.minecraft.client.gui.screens.Screen getCurrentScreen(Minecraft minecraft)
    {
        return minecraft.gui.screen();
    }

    public static void registerBlockBreakIndicatorRenderHook()
    {
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.END_MAIN
                .register(context -> fastui.yure.client.render.BlockBreakIndicator.render(context.levelState()));
    }
}
