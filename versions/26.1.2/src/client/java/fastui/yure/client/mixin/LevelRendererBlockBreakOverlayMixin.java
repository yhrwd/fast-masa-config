package fastui.yure.client.mixin;

import fastui.yure.config.FastMasaConfigs;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelRendererBlockBreakOverlayMixin {
    @Inject(method = "extractBlockDestroyAnimation", at = @At("HEAD"), cancellable = true)
    private void fastui$replaceBlockBreakOverlay(Camera camera, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (FastMasaConfigs.Generic.BLOCK_BREAK_INDICATOR.getBooleanValue()) {
            levelRenderState.blockBreakingRenderStates.clear();
            ci.cancel();
        }
    }
}
