package fastui.yure.client.mixin;

import fastui.yure.config.FastMasaConfigs;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
abstract class WorldRendererBlockBreakOverlayMixin {
    @Inject(method = "renderBlockDamage", at = @At("HEAD"), cancellable = true)
    private void fastui$replaceVanillaBlockBreakOverlay(MatrixStack matrices, Camera camera,
            VertexConsumerProvider.Immediate vertexConsumers, CallbackInfo ci) {
        if (FastMasaConfigs.Generic.BLOCK_BREAK_INDICATOR.getBooleanValue()) {
            ci.cancel();
        }
    }
}
