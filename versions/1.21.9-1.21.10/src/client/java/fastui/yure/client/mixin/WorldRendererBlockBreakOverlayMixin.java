package fastui.yure.client.mixin;

import fastui.yure.config.FastMasaConfigs;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.9 changed renderBlockDamage to (MatrixStack, Immediate, WorldRenderState) and
 * 1.21.10 rewires the third parameter again, so the callback captures no arguments.
 */
@Mixin(WorldRenderer.class)
abstract class WorldRendererBlockBreakOverlayMixin {
    @Inject(method = "renderBlockDamage", at = @At("HEAD"), cancellable = true)
    private void fastui$replaceVanillaBlockBreakOverlay(CallbackInfo ci) {
        if (FastMasaConfigs.Generic.BLOCK_BREAK_INDICATOR.getBooleanValue()) {
            ci.cancel();
        }
    }
}
