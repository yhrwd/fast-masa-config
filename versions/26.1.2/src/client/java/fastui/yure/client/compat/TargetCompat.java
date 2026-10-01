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

    // Vanilla 26.1 has no Minecraft#gui accessor; the current screen is a field,
    // and Screen still lives in net.minecraft.client.gui.screens.
    public static net.minecraft.client.gui.screens.Screen getCurrentScreen(Minecraft minecraft)
    {
        return minecraft.screen;
    }

    // 26.1's level render event context exposes the buffer source and pose stack
    // separately, and BlockBreakIndicator still renders from those.
    public static void registerBlockBreakIndicatorRenderHook()
    {
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.END_MAIN
                .register(context -> fastui.yure.client.render.BlockBreakIndicator.render(
                        context.bufferSource(), context.poseStack(), context.levelState().cameraRenderState.pos));
    }

    /** 当前语言代码；26.x 使用 Mojang 映射的 LanguageManager.getSelected()。 */
    public static String currentLanguageCode()
    {
        return Minecraft.getInstance().getLanguageManager().getSelected();
    }
}
