package fastui.yure.client.mixin;

import fastui.yure.config.EntityRenderFilter;
import fastui.yure.config.FastMasaConfigs;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin<T extends Entity> {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void fastui$applyEntityRenderFilter(T entity, Frustum frustum, double x, double y, double z,
            CallbackInfoReturnable<Boolean> cir) {
        if (!FastMasaConfigs.Tools.ENTITY_RENDER_FILTER.getBooleanValue()) {
            return;
        }
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (!EntityRenderFilter.shouldRender(true,
                FastMasaConfigs.Tools.ENTITY_RENDER_WHITELIST.getBooleanValue(),
                FastMasaConfigs.Tools.ENTITY_RENDER_ENTITIES.getStrings(), id)) {
            cir.setReturnValue(false);
        }
    }
}
