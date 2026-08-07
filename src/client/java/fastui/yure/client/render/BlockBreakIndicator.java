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
import net.minecraft.world.phys.Vec3;
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
        // Keep a small stable core near completion so the cuboid does not
        // collapse into a degenerate, flickering line on the final frames.
        double scale = Math.max(0.08, 1.0 - normalized * 0.92);
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

        if (fill != 0) {
            Gizmos.cuboid(box, GizmoStyle.fill(fill), false).setAlwaysOnTop();
        }
        if (line != 0) {
            addBoxEdges(box, line, FastMasaConfigs.Generic.BLOCK_BREAK_LINE_WIDTH.getIntegerValue());
        }
    }

    private static void addBoxEdges(AABB box, int color, int lineWidth) {
        Vec3[] c = corners(box);
        int[][] edges = {
                {0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3},
                {2, 6}, {3, 7}, {4, 5}, {4, 6}, {5, 7}, {6, 7}
        };
        for (int[] edge : edges) {
            Vec3 start = c[edge[0]];
            Vec3 end = c[edge[1]];
            Gizmos.line(start, end, color, lineWidth).setAlwaysOnTop();
        }
    }

    private static Vec3[] corners(AABB box) {
        return new Vec3[] {
                new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.minX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.minZ), new Vec3(box.minX, box.maxY, box.maxZ),
                new Vec3(box.maxX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.maxZ),
                new Vec3(box.maxX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.maxZ)
        };
    }
}
