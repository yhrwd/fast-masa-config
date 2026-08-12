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
    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private void fastui$applyEntityRenderFilter(T entity, Frustum frustum, double x, double y, double z,
            CallbackInfoReturnable<Boolean> cir) {
        // 只收紧原版已经判定为可渲染的实体；关闭过滤时完全保留原版结果。
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }

        EntityRenderFilter.State filter = EntityRenderFilter.State.from(
                FastMasaConfigs.Tools.ENTITY_RENDER_FILTER.getBooleanValue(),
                FastMasaConfigs.Tools.ENTITY_RENDER_WHITELIST.getBooleanValue(),
                FastMasaConfigs.Tools.ENTITY_RENDER_ENTITIES.getStrings());
        if (!filter.enabled()) {
            return;
        }

        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (!filter.shouldRender(id)) {
            cir.setReturnValue(false);
        }
    }
}
