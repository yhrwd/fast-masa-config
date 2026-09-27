package fastui.yure.client.mixin;

import fastui.yure.config.EntityRenderFilter;
import fastui.yure.config.FastMasaConfigs;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies the configured entity filter after vanilla visibility checks. */
@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin<T extends Entity> {
    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private void fastui$applyEntityRenderFilter(T entity, Frustum frustum, double x, double y, double z,
            CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        EntityRenderFilter.State filter = EntityRenderFilter.State.from(
                FastMasaConfigs.Tools.ENTITY_RENDER_FILTER.getBooleanValue(),
                FastMasaConfigs.Tools.ENTITY_RENDER_WHITELIST.getBooleanValue(),
                FastMasaConfigs.Tools.ENTITY_RENDER_ENTITIES.getStrings());
        if (filter.enabled() && !filter.shouldRender(Registries.ENTITY_TYPE.getId(entity.getType()).toString())) {
            cir.setReturnValue(false);
        }
    }
}
