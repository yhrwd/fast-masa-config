package fastui.yure.client.compat;

import fi.dy.masa.malilib.util.KeyCodes;
import net.minecraft.client.Minecraft;

/**
 * MaLiLib 0.29.x compatibility: KeyCodes still lives in fi.dy.masa.malilib.util
 * and TextInputManager.startTextInput() takes no owner argument.
 */
public final class TargetCompat
{
    private TargetCompat() {}

    public static final int KEY_LEFT_SHIFT = KeyCodes.KEY_LEFT_SHIFT;
    // 0.29.x still uses GLFW key codes and names the main enter key KEY_ENTER.
    public static final int KEY_RETURN = KeyCodes.KEY_ENTER;
    public static final int KEY_KP_ENTER = KeyCodes.KEY_KP_ENTER;
    // GLFW has no separate Return2 key; the shared GUI only uses it as an
    // extra equality check, so a never-matching value is equivalent.
    public static final int KEY_RETURN2 = -1;
    public static final int KEY_ESCAPE = KeyCodes.KEY_ESCAPE;

    public static void startTextInput(Minecraft minecraft, Object owner)
    {
        minecraft.textInputManager().startTextInput();
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
