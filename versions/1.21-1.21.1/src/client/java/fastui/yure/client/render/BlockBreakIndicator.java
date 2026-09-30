package fastui.yure.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fastui.yure.FastMasaConfig;
import fastui.yure.client.mixin.ClientPlayerInteractionManagerAccessor;
import fastui.yure.client.mixin.WorldRendererBlockBreakingAccessor;
import fastui.yure.config.FastMasaConfigs;
import fi.dy.masa.malilib.config.options.ConfigColor;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.player.BlockBreakingInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * Yarn 1.21.1 port of the block-break indicator. There is no LevelRenderState
 * here: own progress comes from the interaction manager and remote progress
 * from the world renderer's breaking map, and the quads are drawn directly on
 * the render thread with the position-color shader.
 */
public final class BlockBreakIndicator {
    private static final int[][] EDGE_PAIRS = {
            {0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3},
            {2, 6}, {3, 7}, {4, 5}, {4, 6}, {5, 7}, {6, 7}
    };

    private BlockBreakIndicator() {
    }

    public static void render(WorldRenderContext context) {
        if (!FastMasaConfigs.Generic.BLOCK_BREAK_INDICATOR.getBooleanValue()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.interactionManager == null || context.matrixStack() == null) {
            return;
        }

        RenderStyle style = RenderStyle.read();
        if (!style.lines && !style.sides) {
            return;
        }

        ClientPlayerInteractionManagerAccessor gameMode =
                (ClientPlayerInteractionManagerAccessor) client.interactionManager;
        BlockPos ownPosition = gameMode.fastui$getDestroyBlockPos();
        float ownProgress = gameMode.fastui$getDestroyProgress();
        boolean hasOwnTarget = ownPosition != null && ownProgress > 0.0F;

        List<Indicator> indicators = new ArrayList<>();
        if (hasOwnTarget) {
            indicators.add(new Indicator(ownPosition, ownProgress));
        }
        if (FastMasaConfigs.Generic.BLOCK_BREAK_REMOTE.getBooleanValue()
                && client.worldRenderer instanceof WorldRendererBlockBreakingAccessor accessor) {
            for (Long2ObjectMap.Entry<java.util.SortedSet<BlockBreakingInfo>> entry
                    : accessor.fastui$getBlockBreakingProgressions().long2ObjectEntrySet()) {
                SortedSet<BlockBreakingInfo> infos = entry.getValue();
                if (infos == null || infos.isEmpty()) {
                    continue;
                }
                BlockPos position = infos.last().getPos();
                if (hasOwnTarget && position.equals(ownPosition)) {
                    continue;
                }
                // Stage is 0..9; map it onto the same 0..1 progress as vanilla.
                indicators.add(new Indicator(position, (infos.last().getStage() + 1) / 9.0F));
            }
        }
        if (indicators.isEmpty()) {
            return;
        }

        Vec3d cameraPosition = context.camera().getPos();
        Matrix4f positionMatrix = context.matrixStack().peek().getPositionMatrix();
        double projectionScale = projectionScale(client);

        RenderLayer layer = RenderLayer.getDebugQuads();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer;
        try {
            buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (Indicator indicator : indicators) {
                addIndicator(client.world, indicator.position(), indicator.progress(), cameraPosition,
                        projectionScale, style, positionMatrix, buffer);
            }
        } catch (RuntimeException exception) {
            FastMasaConfig.LOGGER.warn("Failed to render block break indicator", exception);
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } catch (RuntimeException exception) {
            FastMasaConfig.LOGGER.warn("Failed to render block break indicator", exception);
        } finally {
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private static void addIndicator(World world, BlockPos position, float progress, Vec3d cameraPosition,
            double projectionScale, RenderStyle style, Matrix4f positionMatrix, BufferBuilder buffer) {
        BlockState state = world.getBlockState(position);
        VoxelShape shape = state.getOutlineShape(world, position);
        if (shape.isEmpty()) {
            return;
        }

        float normalized = MathHelper.clamp(progress, 0.0F, 1.0F);
        Box bounds = shape.getBoundingBox();
        // Keep a small stable core near completion so the cuboid does not
        // collapse into a degenerate, flickering line on the final frames.
        double scale = Math.max(0.08, 1.0 - normalized * 0.92);
        double cx = position.getX() + (bounds.minX + bounds.maxX) / 2.0;
        double cy = position.getY() + (bounds.minY + bounds.maxY) / 2.0;
        double cz = position.getZ() + (bounds.minZ + bounds.maxZ) / 2.0;
        double hx = (bounds.maxX - bounds.minX) * scale / 2.0;
        double hy = (bounds.maxY - bounds.minY) * scale / 2.0;
        double hz = (bounds.maxZ - bounds.minZ) * scale / 2.0;
        Box box = new Box(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz)
                .offset(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        int line = style.lines ? lerpColor(normalized, style.startLine, style.endLine) : 0;
        int fill = style.sides ? lerpColor(normalized, style.startSide, style.endSide) : 0;

        if (fill != 0) {
            addBoxSides(buffer, positionMatrix, box, fill);
        }
        if (line != 0) {
            int lineWidth = style.lineWidth;
            int glowAlpha = (line >>> 24) / 4;
            if (glowAlpha > 0) {
                addBoxEdges(buffer, positionMatrix, box, withAlpha(line, glowAlpha),
                        Math.max(lineWidth + 1, lineWidth * 3), projectionScale);
            }
            addBoxEdges(buffer, positionMatrix, box, line, lineWidth, projectionScale);
        }
    }

    private static void addBoxSides(BufferBuilder buffer, Matrix4f matrix, Box box, int color) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        quad(buffer, matrix, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, color);
        quad(buffer, matrix, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, color);
        quad(buffer, matrix, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color);
        quad(buffer, matrix, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, color);
        quad(buffer, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, color);
        quad(buffer, matrix, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, color);
    }

    private static void addBoxEdges(BufferBuilder buffer, Matrix4f matrix, Box box, int color, int lineWidth,
            double projectionScale) {
        for (int[] edge : EDGE_PAIRS) {
            addLineQuad(buffer, matrix,
                    cornerX(box, edge[0]), cornerY(box, edge[0]), cornerZ(box, edge[0]),
                    cornerX(box, edge[1]), cornerY(box, edge[1]), cornerZ(box, edge[1]),
                    color, lineWidth, projectionScale);
        }
    }

    private static void addLineQuad(BufferBuilder buffer, Matrix4f matrix, double startX, double startY,
            double startZ, double endX, double endY, double endZ, int color, int lineWidth,
            double projectionScale) {
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
        quad(buffer, matrix,
                startX + offsetX, startY + offsetY, startZ + offsetZ,
                startX - offsetX, startY - offsetY, startZ - offsetZ,
                endX - offsetX, endY - offsetY, endZ - offsetZ,
                endX + offsetX, endY + offsetY, endZ + offsetZ, color);
    }

    private static double projectionScale(MinecraftClient client) {
        int framebufferHeight = Math.max(1, client.getWindow().getHeight());
        double fovRadians = Math.toRadians(client.options.getFov().getValue());
        return 2.0 * Math.tan(fovRadians / 2.0) / framebufferHeight;
    }

    private record RenderStyle(boolean lines, boolean sides, int startLine, int endLine, int startSide, int endSide,
            int lineWidth) {
        private static RenderStyle read() {
            return new RenderStyle(
                    FastMasaConfigs.Generic.BLOCK_BREAK_LINES.getBooleanValue(),
                    FastMasaConfigs.Generic.BLOCK_BREAK_SIDES.getBooleanValue(),
                    colorInt(FastMasaConfigs.Generic.BLOCK_BREAK_START_LINE),
                    colorInt(FastMasaConfigs.Generic.BLOCK_BREAK_END_LINE),
                    colorInt(FastMasaConfigs.Generic.BLOCK_BREAK_START_SIDE),
                    colorInt(FastMasaConfigs.Generic.BLOCK_BREAK_END_SIDE),
                    FastMasaConfigs.Generic.BLOCK_BREAK_LINE_WIDTH.getIntegerValue());
        }
    }

    private static int colorInt(ConfigColor config) {
        return config.getColor().intValue;
    }

    private static int lerpColor(float t, int start, int end) {
        int alpha = lerpChannel(t, (start >>> 24) & 0xFF, (end >>> 24) & 0xFF);
        int red = lerpChannel(t, (start >>> 16) & 0xFF, (end >>> 16) & 0xFF);
        int green = lerpChannel(t, (start >>> 8) & 0xFF, (end >>> 8) & 0xFF);
        int blue = lerpChannel(t, start & 0xFF, end & 0xFF);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int lerpChannel(float t, int start, int end) {
        return Math.round(start + (end - start) * t);
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix, double x1, double y1, double z1, double x2,
            double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, int color) {
        vertex(buffer, matrix, x1, y1, z1, color);
        vertex(buffer, matrix, x2, y2, z2, color);
        vertex(buffer, matrix, x3, y3, z3, color);
        vertex(buffer, matrix, x4, y4, z4, color);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, double x, double y, double z, int color) {
        buffer.vertex(matrix, (float) x, (float) y, (float) z)
                .color((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, (color >>> 24) & 0xFF);
    }

    private static int withAlpha(int color, int alpha) {
        int clamped = MathHelper.clamp(alpha, 0, 255);
        return (clamped << 24) | (color & 0x00FFFFFF);
    }

    private static double cornerX(Box box, int index) {
        return (index & 4) == 0 ? box.minX : box.maxX;
    }

    private static double cornerY(Box box, int index) {
        return (index & 2) == 0 ? box.minY : box.maxY;
    }

    private static double cornerZ(Box box, int index) {
        return (index & 1) == 0 ? box.minZ : box.maxZ;
    }

    private record Indicator(BlockPos position, float progress) {
    }
}
