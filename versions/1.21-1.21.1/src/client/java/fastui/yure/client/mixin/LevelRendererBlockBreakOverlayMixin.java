package fastui.yure.client.mixin;

import fastui.yure.config.FastMasaConfigs;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.player.BlockBreakingInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.SortedSet;

/**
 * 1.21~1.21.1 has no LevelRenderState; the vanilla crack overlay loop is inlined
 * inside {@link WorldRenderer#render}. Redirecting the progressions map lookup
 * hides the vanilla cracks while our own indicator draws instead.
 */
@Mixin(WorldRenderer.class)
abstract class LevelRendererBlockBreakOverlayMixin {
    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;long2ObjectEntrySet()Lit/unimi/dsi/fastutil/objects/ObjectSet;",
                    remap = false))
    private ObjectSet<Long2ObjectMap.Entry<SortedSet<BlockBreakingInfo>>> fastui$disableVanillaBlockBreakOverlay(
            Long2ObjectMap<SortedSet<BlockBreakingInfo>> progressions) {
        if (FastMasaConfigs.Generic.BLOCK_BREAK_INDICATOR.getBooleanValue()) {
            return Long2ObjectMaps.EMPTY_MAP.long2ObjectEntrySet();
        }
        return progressions.long2ObjectEntrySet();
    }
}
