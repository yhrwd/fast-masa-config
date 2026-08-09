package fastui.yure.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fastui.yure.FastMasaConfig;
import fastui.yure.client.mixin.ClientPlayerInteractionManagerAccessor;
import fastui.yure.config.FastMasaConfigs;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Renders the block-break indicator through Iris-compatible MaLiLib pipelines. */
public final class BlockBreakIndicator {
    private static final RenderPipeline PIPELINE = MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL;
    private static final String RENDERER_NAME = "fast-masa-config:block_break_indicator";
    private static RenderContext renderContext;

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

        Vec3 cameraPosition = client.gameRenderer.mainCamera().position();
        RenderContext context = getRenderContext();
        BufferBuilder buffer = context.start(() -> RENDERER_NAME, PIPELINE, 0);

        ClientPlayerInteractionManagerAccessor gameMode = (ClientPlayerInteractionManagerAccessor) client.gameMode;
        BlockPos ownPosition = gameMode.fastui$getDestroyBlockPos();
        float ownProgress = gameMode.fastui$getDestroyProgress();
        if (ownPosition != null && ownProgress > 0.0F) {
            addIndicator(client.level, ownPosition, ownProgress, cameraPosition, buffer);
        }

        if (FastMasaConfigs.Generic.BLOCK_BREAK_REMOTE.getBooleanValue()) {
            for (BlockBreakingRenderState progress : renderState.blockBreakingRenderStates) {
                if (!progress.blockPos().equals(ownPosition)) {
                    addIndicator(client.level, progress.blockPos(), (progress.progress() + 1) / 9.0F, cameraPosition,
                            buffer);
                }
            }
        }

        try (MeshData meshData = buffer.build()) {
            if (meshData != null) {
                context.draw(meshData);
            }
        } catch (RuntimeException exception) {
            FastMasaConfig.LOGGER.warn("Failed to render block break indicator", exception);
        }
    }

    private static RenderContext getRenderContext() {
        if (renderContext == null) {
            renderContext = new RenderContext(() -> RENDERER_NAME, PIPELINE, 0);
        }
        return renderContext;
    }

    private static void addIndicator(ClientLevel level, BlockPos position, float progress, Vec3 cameraPosition,
            VertexConsumer buffer) {
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
        AABB box = new AABB(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz)
                .move(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        int line = ARGB.srgbLerp(normalized,
                FastMasaConfigs.Generic.BLOCK_BREAK_START_LINE.getColor().toVanillaArgb(),
                FastMasaConfigs.Generic.BLOCK_BREAK_END_LINE.getColor().toVanillaArgb());
        int fill = ARGB.srgbLerp(normalized,
                FastMasaConfigs.Generic.BLOCK_BREAK_START_SIDE.getColor().toVanillaArgb(),
                FastMasaConfigs.Generic.BLOCK_BREAK_END_SIDE.getColor().toVanillaArgb());
        if (FastMasaConfigs.Generic.BLOCK_BREAK_SIDES.getBooleanValue()) {
            // The configured color already carries its intended alpha.
        } else {
            fill = 0;
        }
        if (!FastMasaConfigs.Generic.BLOCK_BREAK_LINES.getBooleanValue()) {
            line = 0;
        }

        if (fill != 0) {
            addBoxSides(buffer, box, fill);
        }
        if (line != 0) {
            int lineWidth = FastMasaConfigs.Generic.BLOCK_BREAK_LINE_WIDTH.getIntegerValue();
            int glowAlpha = ARGB.alpha(line) / 4;
            if (glowAlpha > 0) {
                addBoxEdges(buffer, box, withAlpha(line, glowAlpha), Math.max(lineWidth + 1, lineWidth * 3));
            }
            addBoxEdges(buffer, box, line, lineWidth);
        }
    }

    private static void addBoxSides(VertexConsumer buffer, AABB box, int color) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        quad(buffer, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, color);
        quad(buffer, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, color);
        quad(buffer, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color);
        quad(buffer, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, color);
        quad(buffer, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, color);
        quad(buffer, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, color);
    }

    private static void addBoxEdges(VertexConsumer buffer, AABB box, int color, int lineWidth) {
        Vec3[] corners = corners(box);
        int[][] edges = {
                {0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3},
                {2, 6}, {3, 7}, {4, 5}, {4, 6}, {5, 7}, {6, 7}
        };
        for (int[] edge : edges) {
            addLineQuad(buffer, corners[edge[0]], corners[edge[1]], color, lineWidth);
        }
    }

    private static void addLineQuad(VertexConsumer buffer, Vec3 start, Vec3 end, int color, int lineWidth) {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 midpoint = start.add(end).scale(0.5);
        Vec3 towardCamera = midpoint.normalize().reverse();
        Vec3 offset = direction.cross(towardCamera);
        if (offset.lengthSqr() < 1.0E-8) {
            offset = direction.cross(new Vec3(0.0, 1.0, 0.0));
            if (offset.lengthSqr() < 1.0E-8) {
                offset = direction.cross(new Vec3(1.0, 0.0, 0.0));
            }
        }

        offset = offset.normalize().scale(lineHalfWidth(midpoint, lineWidth));
        Vec3 startLeft = start.add(offset);
        Vec3 startRight = start.subtract(offset);
        Vec3 endRight = end.subtract(offset);
        Vec3 endLeft = end.add(offset);
        quad(buffer, startLeft.x, startLeft.y, startLeft.z, startRight.x, startRight.y, startRight.z,
                endRight.x, endRight.y, endRight.z, endLeft.x, endLeft.y, endLeft.z, color);
    }

    private static double lineHalfWidth(Vec3 midpoint, int width) {
        Minecraft client = Minecraft.getInstance();
        int framebufferHeight = Math.max(1, client.getWindow().getHeight());
        double distance = Math.max(0.1, midpoint.length());
        double fovRadians = Math.toRadians(client.options.fov().get());
        double unitsPerPixel = 2.0 * distance * Math.tan(fovRadians / 2.0) / framebufferHeight;
        return Math.max(0.0005, unitsPerPixel * Math.max(1, width) / 2.0);
    }

    private static void quad(VertexConsumer buffer, double x1, double y1, double z1, double x2, double y2,
            double z2, double x3, double y3, double z3, double x4, double y4, double z4, int color) {
        vertex(buffer, x1, y1, z1, color);
        vertex(buffer, x2, y2, z2, color);
        vertex(buffer, x3, y3, z3, color);
        vertex(buffer, x4, y4, z4, color);
    }

    private static void vertex(VertexConsumer buffer, double x, double y, double z, int color) {
        buffer.addVertex((float) x, (float) y, (float) z)
                .setColor(ARGB.red(color), ARGB.green(color), ARGB.blue(color), ARGB.alpha(color));
    }

    private static int withAlpha(int color, int alpha) {
        return ARGB.color(Math.clamp(alpha, 0, 255), ARGB.red(color), ARGB.green(color), ARGB.blue(color));
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
