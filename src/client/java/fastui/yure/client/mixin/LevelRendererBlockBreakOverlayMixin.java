package fastui.yure.client.mixin;

import fastui.yure.config.FastMasaConfigs;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelRendererBlockBreakOverlayMixin {
    @Inject(method = "submitBlockDestroyAnimation", at = @At("HEAD"), cancellable = true)
    private void fastui$disableVanillaBlockBreakOverlay(PoseStack poseStack, SubmitNodeCollector collector,
            LevelRenderState renderState, CallbackInfo ci) {
        if (FastMasaConfigs.Generic.BLOCK_BREAK_INDICATOR.getBooleanValue()) {
            ci.cancel();
        }
    }
}
