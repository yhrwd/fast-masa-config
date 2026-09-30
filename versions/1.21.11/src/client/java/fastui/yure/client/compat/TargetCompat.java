package fastui.yure.client.compat;

import fi.dy.masa.malilib.util.KeyCodes;
import net.minecraft.client.MinecraftClient;

/**
 * Yarn / MaLiLib 0.25.x compatibility for the 1.21.6~1.21.8 targets: GLFW key
 * code names, no explicit text-input sessions, and the current screen is a field.
 */
public final class TargetCompat
{
    private TargetCompat() {}

    public static final int KEY_LEFT_SHIFT = KeyCodes.KEY_LEFT_SHIFT;
    // GLFW naming: the main enter key is KEY_ENTER.
    public static final int KEY_RETURN = KeyCodes.KEY_ENTER;
    public static final int KEY_KP_ENTER = KeyCodes.KEY_KP_ENTER;
    // GLFW has no separate Return2 key; a never-matching value is equivalent.
    public static final int KEY_RETURN2 = -1;
    public static final int KEY_ESCAPE = KeyCodes.KEY_ESCAPE;

    public static void startTextInput(MinecraftClient client, Object owner)
    {
        // GLFW-based targets deliver char events without an explicit session.
    }

    public static net.minecraft.client.gui.screen.Screen getCurrentScreen(MinecraftClient client)
    {
        return client.currentScreen;
    }

    public static void registerBlockBreakIndicatorRenderHook()
    {
        fastui.yure.client.render.BlockBreakIndicator.register();
    }
}
