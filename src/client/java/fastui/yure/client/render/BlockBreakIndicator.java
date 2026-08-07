package fastui.yure.client.render;

import fastui.yure.config.FastMasaConfigs;
import fastui.yure.client.mixin.ClientPlayerInteractionManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Minecraft 26.2 implementation backed by the vanilla Gizmo collector. */
public final class BlockBreakIndicator {
    private BlockBreakIndicator() {
    }

    public static void render(LevelRenderState renderState) {
        if (!FastMasaConfigs.Generic.BLOCK_BREAK_INDICATOR.getBooleanValue()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.gameMode == null) {
            return;
        }

        ClientPlayerInteractionManagerAccessor gameMode = (ClientPlayerInteractionManagerAccessor) client.gameMode;
        BlockPos ownPosition = gameMode.fastui$getDestroyBlockPos();
        float ownProgress = gameMode.fastui$getDestroyProgress();
        if (ownPosition != null && ownProgress > 0.0F) {
            addIndicator(client.level, ownPosition, ownProgress);
        }

        if (FastMasaConfigs.Generic.BLOCK_BREAK_REMOTE.getBooleanValue()) {
            for (BlockBreakingRenderState progress : renderState.blockBreakingRenderStates) {
                if (!progress.blockPos().equals(ownPosition)) {
                    addIndicator(client.level, progress.blockPos(), (progress.progress() + 1) / 9.0F);
                }
            }
        }
    }

    private static void addIndicator(ClientLevel level, BlockPos position, float progress) {
        BlockState state = level.getBlockState(position);
        VoxelShape shape = state.getShape(level, position);
        if (shape.isEmpty()) {
            return;
        }

        float normalized = Math.clamp(progress, 0.0F, 1.0F);
        AABB bounds = shape.bounds();
        double scale = 1.0 - normalized;
        double cx = position.getX() + (bounds.minX + bounds.maxX) / 2.0;
        double cy = position.getY() + (bounds.minY + bounds.maxY) / 2.0;
        double cz = position.getZ() + (bounds.minZ + bounds.maxZ) / 2.0;
        double hx = bounds.getXsize() * scale / 2.0;
        double hy = bounds.getYsize() * scale / 2.0;
        double hz = bounds.getZsize() * scale / 2.0;
        AABB box = new AABB(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz);

        int line = ARGB.srgbLerp(normalized,
                FastMasaConfigs.Generic.BLOCK_BREAK_START_LINE.getColor().toVanillaArgb(),
                FastMasaConfigs.Generic.BLOCK_BREAK_END_LINE.getColor().toVanillaArgb());
        int fill = ARGB.srgbLerp(normalized,
                FastMasaConfigs.Generic.BLOCK_BREAK_START_SIDE.getColor().toVanillaArgb(),
                FastMasaConfigs.Generic.BLOCK_BREAK_END_SIDE.getColor().toVanillaArgb());
        if (FastMasaConfigs.Generic.BLOCK_BREAK_SIDES.getBooleanValue()) {
            fill = ARGB.color(ARGB.alpha(fill) / 2, ARGB.red(fill), ARGB.green(fill), ARGB.blue(fill));
        } else {
            fill = 0;
        }
        if (!FastMasaConfigs.Generic.BLOCK_BREAK_LINES.getBooleanValue()) {
            line = 0;
        }
        if (line == 0 && fill == 0) {
            return;
        }

        GizmoStyle style = GizmoStyle.strokeAndFill(line,
                FastMasaConfigs.Generic.BLOCK_BREAK_LINE_WIDTH.getIntegerValue(), fill);
        Gizmos.cuboid(box, style, false).setAlwaysOnTop();
    }
}
