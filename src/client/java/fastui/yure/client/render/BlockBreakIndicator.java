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
        double scale = Math.max(0.10, 1.0 - normalized);
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
        if (line != 0) {
            addCornerPoints(box, line, FastMasaConfigs.Generic.BLOCK_BREAK_LINE_WIDTH.getIntegerValue());
            addMeteorTrail(box, line, FastMasaConfigs.Generic.BLOCK_BREAK_LINE_WIDTH.getIntegerValue());
        }
    }

    private static void addCornerPoints(AABB box, int color, int lineWidth) {
        float pointSize = Math.max(2.0F, lineWidth * 1.35F);
        double[] xs = {box.minX, box.maxX};
        double[] ys = {box.minY, box.maxY};
        double[] zs = {box.minZ, box.maxZ};
        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    Gizmos.point(new Vec3(x, y, z), color, pointSize).setAlwaysOnTop();
                }
            }
        }
    }

    private static void addMeteorTrail(AABB box, int color, int lineWidth) {
        Vec3[] corners = corners(box);
        long cycleMillis = 1100L;
        long elapsed = System.currentTimeMillis();
        int pair = (int) ((elapsed / cycleMillis) & 3L);
        float progress = (elapsed % cycleMillis) / (float) cycleMillis;
        Vec3 start = corners[pair];
        Vec3 end = corners[pair ^ 7];
        Vec3 meteor = lerp(start, end, progress);

        int trailSegments = 7;
        for (int i = trailSegments; i >= 1; i--) {
            float from = Math.max(0.0F, progress - i * 0.055F);
            Vec3 tail = lerp(start, end, from);
            int alpha = Math.max(12, 150 - i * 18);
            int trailColor = ARGB.color(alpha, ARGB.red(color), ARGB.green(color), ARGB.blue(color));
            Gizmos.line(tail, meteor, trailColor, Math.max(1.0F, lineWidth * (1.2F - i * 0.08F)))
                    .setAlwaysOnTop();
        }
        int glowColor = ARGB.color(255, ARGB.red(color), ARGB.green(color), ARGB.blue(color));
        Gizmos.point(meteor, glowColor, Math.max(3.0F, lineWidth * 2.2F)).setAlwaysOnTop();
    }

    private static Vec3[] corners(AABB box) {
        return new Vec3[] {
                new Vec3(box.minX, box.minY, box.minZ),
                new Vec3(box.minX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.minZ),
                new Vec3(box.minX, box.maxY, box.maxZ),
                new Vec3(box.maxX, box.minY, box.minZ),
                new Vec3(box.maxX, box.minY, box.maxZ),
                new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.maxZ)
        };
    }

    private static Vec3 lerp(Vec3 start, Vec3 end, float progress) {
        return new Vec3(
                start.x + (end.x - start.x) * progress,
                start.y + (end.y - start.y) * progress,
                start.z + (end.z - start.z) * progress);
    }
}
