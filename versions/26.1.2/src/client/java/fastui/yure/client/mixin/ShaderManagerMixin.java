package fastui.yure.client.mixin;

import fastui.yure.client.render.BlockBreakIndicator;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderManager.class)
abstract class ShaderManagerMixin {
    @Inject(method = "apply", at = @At("TAIL"))
    private void fastui$precompileBreakIndicatorPipelines(ShaderManager.Configs configs, ResourceManager resourceManager,
            ProfilerFiller profiler, CallbackInfo ci) {
        BlockBreakIndicator.precompilePipelines(resourceManager);
    }
}
