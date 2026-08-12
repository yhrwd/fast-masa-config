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
    private static final int[][] EDGE_PAIRS = {
            {0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3},
            {2, 6}, {3, 7}, {4, 5}, {4, 6}, {5, 7}, {6, 7}
    };
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

        ClientPlayerInteractionManagerAccessor gameMode = (ClientPlayerInteractionManagerAccessor) client.gameMode;
        BlockPos ownPosition = gameMode.fastui$getDestroyBlockPos();
        float ownProgress = gameMode.fastui$getDestroyProgress();
        boolean hasOwnTarget = ownPosition != null && ownProgress > 0.0F;
        boolean hasRemoteTarget = hasRemoteTarget(renderState, ownPosition);
        if (!hasOwnTarget && !hasRemoteTarget) {
            return;
        }

        Vec3 cameraPosition = client.gameRenderer.mainCamera().position();
        double projectionScale = projectionScale(client);
        RenderStyle style = RenderStyle.read();
        if (!style.lines && !style.sides) {
            return;
        }
        RenderContext context = getRenderContext();
        BufferBuilder buffer = context.start(() -> RENDERER_NAME, PIPELINE, 0);
        if (hasOwnTarget) {
            addIndicator(client.level, ownPosition, ownProgress, cameraPosition, projectionScale, style, buffer);
        }

        if (hasRemoteTarget) {
            for (BlockBreakingRenderState progress : renderState.blockBreakingRenderStates) {
                if (!progress.blockPos().equals(ownPosition)) {
                    addIndicator(client.level, progress.blockPos(), (progress.progress() + 1) / 9.0F, cameraPosition,
                            projectionScale, style, buffer);
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

    private static boolean hasRemoteTarget(LevelRenderState renderState, BlockPos ownPosition) {
        if (!FastMasaConfigs.Generic.BLOCK_BREAK_REMOTE.getBooleanValue()) {
            return false;
        }
        for (BlockBreakingRenderState progress : renderState.blockBreakingRenderStates) {
            if (!progress.blockPos().equals(ownPosition)) {
                return true;
            }
        }
        return false;
    }

    private static RenderContext getRenderContext() {
        if (renderContext == null) {
            renderContext = new RenderContext(() -> RENDERER_NAME, PIPELINE, 0);
        }
        return renderContext;
    }

    private static void addIndicator(ClientLevel level, BlockPos position, float progress, Vec3 cameraPosition,
            double projectionScale, RenderStyle style, VertexConsumer buffer) {
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

        int line = style.lines ? ARGB.srgbLerp(normalized, style.startLine, style.endLine) : 0;
        int fill = style.sides ? ARGB.srgbLerp(normalized, style.startSide, style.endSide) : 0;

        if (fill != 0) {
            addBoxSides(buffer, box, fill);
        }
        if (line != 0) {
            int lineWidth = style.lineWidth;
            int glowAlpha = ARGB.alpha(line) / 4;
            if (glowAlpha > 0) {
                addBoxEdges(buffer, box, withAlpha(line, glowAlpha), Math.max(lineWidth + 1, lineWidth * 3),
                        projectionScale);
            }
            addBoxEdges(buffer, box, line, lineWidth, projectionScale);
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

    private static void addBoxEdges(VertexConsumer buffer, AABB box, int color, int lineWidth,
            double projectionScale) {
        for (int[] edge : EDGE_PAIRS) {
            addLineQuad(buffer, cornerX(box, edge[0]), cornerY(box, edge[0]), cornerZ(box, edge[0]),
                    cornerX(box, edge[1]), cornerY(box, edge[1]), cornerZ(box, edge[1]), color, lineWidth,
                    projectionScale);
        }
    }

    private static void addLineQuad(VertexConsumer buffer, double startX, double startY, double startZ, double endX,
            double endY, double endZ, int color, int lineWidth, double projectionScale) {
        double directionX = endX - startX;
        double directionY = endY - startY;
        double directionZ = endZ - startZ;
        double directionLength = Math.sqrt(directionX * directionX + directionY * directionY
                + directionZ * directionZ);
        if (directionLength < 1.0E-8) {
            return;
        }
        directionX /= directionLength;
        directionY /= directionLength;
        directionZ /= directionLength;

        double midpointX = (startX + endX) * 0.5;
        double midpointY = (startY + endY) * 0.5;
        double midpointZ = (startZ + endZ) * 0.5;
        double midpointLength = Math.max(0.1,
                Math.sqrt(midpointX * midpointX + midpointY * midpointY + midpointZ * midpointZ));
        double towardCameraX = -midpointX / midpointLength;
        double towardCameraY = -midpointY / midpointLength;
        double towardCameraZ = -midpointZ / midpointLength;

        double offsetX = directionY * towardCameraZ - directionZ * towardCameraY;
        double offsetY = directionZ * towardCameraX - directionX * towardCameraZ;
        double offsetZ = directionX * towardCameraY - directionY * towardCameraX;
        double offsetLength = Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
        if (offsetLength < 1.0E-8) {
            offsetX = directionY;
            offsetY = -directionX;
            offsetZ = 0.0;
            offsetLength = Math.sqrt(offsetX * offsetX + offsetY * offsetY);
            if (offsetLength < 1.0E-8) {
                offsetX = 0.0;
                offsetY = directionZ;
                offsetZ = -directionY;
                offsetLength = Math.sqrt(offsetY * offsetY + offsetZ * offsetZ);
            }
        }

        double halfWidth = Math.max(0.0005,
                midpointLength * projectionScale * Math.max(1, lineWidth) / 2.0);
        offsetX = offsetX / offsetLength * halfWidth;
        offsetY = offsetY / offsetLength * halfWidth;
        offsetZ = offsetZ / offsetLength * halfWidth;
        quad(buffer, startX + offsetX, startY + offsetY, startZ + offsetZ,
                startX - offsetX, startY - offsetY, startZ - offsetZ,
                endX - offsetX, endY - offsetY, endZ - offsetZ,
                endX + offsetX, endY + offsetY, endZ + offsetZ, color);
    }

    private static double projectionScale(Minecraft client) {
        int framebufferHeight = Math.max(1, client.getWindow().getHeight());
        double fovRadians = Math.toRadians(client.options.fov().get());
        return 2.0 * Math.tan(fovRadians / 2.0) / framebufferHeight;
    }

    private record RenderStyle(boolean lines, boolean sides, int startLine, int endLine, int startSide, int endSide,
            int lineWidth) {
        private static RenderStyle read() {
            return new RenderStyle(
                    FastMasaConfigs.Generic.BLOCK_BREAK_LINES.getBooleanValue(),
                    FastMasaConfigs.Generic.BLOCK_BREAK_SIDES.getBooleanValue(),
                    FastMasaConfigs.Generic.BLOCK_BREAK_START_LINE.getColor().toVanillaArgb(),
                    FastMasaConfigs.Generic.BLOCK_BREAK_END_LINE.getColor().toVanillaArgb(),
                    FastMasaConfigs.Generic.BLOCK_BREAK_START_SIDE.getColor().toVanillaArgb(),
                    FastMasaConfigs.Generic.BLOCK_BREAK_END_SIDE.getColor().toVanillaArgb(),
                    FastMasaConfigs.Generic.BLOCK_BREAK_LINE_WIDTH.getIntegerValue());
        }
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

    private static double cornerX(AABB box, int index) {
        return (index & 4) == 0 ? box.minX : box.maxX;
    }

    private static double cornerY(AABB box, int index) {
        return (index & 2) == 0 ? box.minY : box.maxY;
    }

    private static double cornerZ(AABB box, int index) {
        return (index & 1) == 0 ? box.minZ : box.maxZ;
    }
}
