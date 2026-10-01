package fastui.yure.client.mixin;

import fastui.yure.client.render.BlockBreakIndicator;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires the block-break indicator at the same point meteor client uses for its
 * 3D overlay rendering: right after the world pass finished (the profiler swap
 * to "hand"), where the global model-view stack is clean and overlays add the
 * camera rotation themselves. The callback captures no arguments so it binds
 * across the renderWorld signature changes in 1.21.9/1.21.10.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderWorld", at = @At(value = "INVOKE_STRING",
            target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = {"ldc=hand"}))
    private void fastui$onRenderWorldEnd(CallbackInfo ci) {
        BlockBreakIndicator.render();
    }
}
