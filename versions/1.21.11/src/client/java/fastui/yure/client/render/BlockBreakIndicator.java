package fastui.yure.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fastui.yure.FastMasaConfig;
import fastui.yure.client.mixin.CameraAccessor;
import fastui.yure.client.mixin.ClientPlayerInteractionManagerAccessor;
import fastui.yure.client.mixin.WorldRendererAccessor;
import fastui.yure.config.FastMasaConfigs;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.player.BlockBreakingInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Block-break indicator for 1.21.9-1.21.10, drawn meteor-style from the
 * GameRenderer.renderWorld hook: the clean camera rotation is pushed onto the
 * global model-view stack, each indicator translates the stack to its
 * camera-relative block center, and raw local-space quads go through malilib's
 * RenderContext with the position-color translucent/no-depth/no-cull pipeline
 * (the pipeline itself carries blend, depth, and cull state on this version).
 * malilib's own world render hooks are not used here: on 1.21.9+ they fire
 * inside the vanilla debug pass whose batches never flush custom layers.
 */
public final class BlockBreakIndicator {
    private static final int[][] EDGE_PAIRS = {
            {0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3},
            {2, 6}, {3, 7}, {4, 5}, {4, 6}, {5, 7}, {6, 7}
    };

    private static long lastDiagnosticLog;

    private BlockBreakIndicator() {
    }

    public static void render() {
        if (!FastMasaConfigs.Generic.BLOCK_BREAK_INDICATOR.getBooleanValue()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.interactionManager == null) {
            return;
        }

        RenderStyle style = RenderStyle.read();
        if (!style.lines && !style.sides) {
            return;
        }

        ClientPlayerInteractionManagerAccessor gameMode =
                (ClientPlayerInteractionManagerAccessor) client.interactionManager;
        BlockPos ownPosition = gameMode.fastui$getCurrentBreakingPos();
        float ownProgress = gameMode.fastui$getCurrentBreakingProgress();
        boolean hasOwnTarget = ownPosition != null && ownProgress > 0.0F;

        List<Indicator> indicators = new ArrayList<>();
        if (hasOwnTarget) {
            indicators.add(new Indicator(ownPosition, ownProgress));
        }
        if (FastMasaConfigs.Generic.BLOCK_BREAK_REMOTE.getBooleanValue()
                && client.worldRenderer instanceof WorldRendererAccessor accessor) {
            for (BlockBreakingInfo info : accessor.fastui$getBlockBreakingInfos().values()) {
                BlockPos position = info.getPos();
                if (hasOwnTarget && position.equals(ownPosition)) {
                    continue;
                }
                // Stage is 0..9; map it onto the same 0..1 progress as vanilla.
                indicators.add(new Indicator(position, (info.getStage() + 1) / 9.0F));
            }
        }
        if (indicators.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastDiagnosticLog > 10_000L) {
            lastDiagnosticLog = now;
            FastMasaConfig.LOGGER.info("[block-break-indicator] drawing {} indicator(s), own pos {}, progress {}",
                    indicators.size(), ownPosition, ownProgress);
        }

        // The clean camera rotation, built exactly like GameRenderer.renderWorld
        // does for the world pass: conjugated camera rotation as a matrix.
        Quaternionf conjugate = client.gameRenderer.getCamera().getRotation().conjugate(new Quaternionf());
        Matrix4f rotation = new Matrix4f().rotation(conjugate);

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(rotation);

        try {
            Vec3d cameraPosition = ((CameraAccessor) client.gameRenderer.getCamera()).fastui$getPos();
            for (Indicator indicator : indicators) {
                addIndicator(client.world, indicator, cameraPosition, style);
            }
        } catch (RuntimeException exception) {
            FastMasaConfig.LOGGER.warn("Failed to render block break indicator", exception);
        } finally {
            modelViewStack.popMatrix();
        }
    }

    private static void addIndicator(World world, Indicator indicator, Vec3d cameraPosition, RenderStyle style) {
        BlockPos position = indicator.position();
        BlockState state = world.getBlockState(position);
        VoxelShape shape = state.getOutlineShape(world, position);
        if (shape.isEmpty()) {
            return;
        }

        float normalized = MathHelper.clamp(indicator.progress(), 0.0F, 1.0F);
        Box bounds = shape.getBoundingBox();
        // Keep a small stable core near completion so the cuboid does not
        // collapse into a degenerate, flickering line on the final frames.
        double scale = Math.max(0.08, 1.0 - normalized * 0.92);
        double hx = (bounds.maxX - bounds.minX) * scale / 2.0;
        double hy = (bounds.maxY - bounds.minY) * scale / 2.0;
        double hz = (bounds.maxZ - bounds.minZ) * scale / 2.0;
        // Block center in camera-relative space; the model-view stack already
        // carries the camera rotation, so this translation positions the box.
        double cx = position.getX() + (bounds.minX + bounds.maxX) / 2.0 - cameraPosition.x;
        double cy = position.getY() + (bounds.minY + bounds.maxY) / 2.0 - cameraPosition.y;
        double cz = position.getZ() + (bounds.minZ + bounds.maxZ) / 2.0 - cameraPosition.z;
        double centerDistance = Math.sqrt(cx * cx + cy * cy + cz * cz);
        double widthScale = centerDistance * projectionScale() / 2.0;
        // Local camera position relative to the block center, for edge glow that
        // faces the viewer.
        double camLocalX = -cx;
        double camLocalY = -cy;
        double camLocalZ = -cz;

        int line = style.lines ? lerpColor(normalized, style.startLine, style.endLine) : 0;
        int fill = style.sides ? lerpColor(normalized, style.startSide, style.endSide) : 0;

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.translate((float) cx, (float) cy, (float) cz);
        try {
            RenderContext context = new RenderContext(() -> "fast-masa-config:block_break_indicator",
                    MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL);
            try {
                BufferBuilder buffer = context.getBuilder();
                if (fill != 0) {
                    addBoxSides(buffer, hx, hy, hz, fill);
                }
                if (line != 0) {
                    int lineWidth = style.lineWidth;
                    int glowAlpha = (line >>> 24) / 4;
                    if (glowAlpha > 0) {
                        addBoxEdges(buffer, hx, hy, hz, camLocalX, camLocalY, camLocalZ,
                                withAlpha(line, glowAlpha), Math.max(lineWidth + 1, lineWidth * 3) * widthScale);
                    }
                    addBoxEdges(buffer, hx, hy, hz, camLocalX, camLocalY, camLocalZ, line,
                            lineWidth * widthScale);
                }
                BuiltBuffer meshData = buffer.endNullable();
                if (meshData != null) {
                    context.draw(meshData, false);
                    meshData.close();
                }
            } finally {
                context.close();
            }
        } catch (Exception exception) {
            FastMasaConfig.LOGGER.warn("Failed to render block break indicator", exception);
        } finally {
            modelViewStack.popMatrix();
        }
    }

    private static void addBoxSides(BufferBuilder buffer, double hx, double hy, double hz, int color) {
        quad(buffer, -hx, -hy, -hz, -hx, -hy, hz, -hx, hy, hz, -hx, hy, -hz, color);
        quad(buffer, hx, -hy, -hz, hx, hy, -hz, hx, hy, hz, hx, -hy, hz, color);
        quad(buffer, -hx, -hy, -hz, -hx, hy, -hz, hx, hy, -hz, hx, -hy, -hz, color);
        quad(buffer, -hx, -hy, hz, hx, -hy, hz, hx, hy, hz, -hx, hy, hz, color);
        quad(buffer, -hx, -hy, -hz, hx, -hy, -hz, hx, -hy, hz, -hx, -hy, hz, color);
        quad(buffer, -hx, hy, -hz, -hx, hy, hz, hx, hy, hz, hx, hy, -hz, color);
    }

    private static void addBoxEdges(BufferBuilder buffer, double hx, double hy, double hz, double camX,
            double camY, double camZ, int color, double halfWidth) {
        for (int[] edge : EDGE_PAIRS) {
            addLineQuad(buffer,
                    cornerX(hx, edge[0]), cornerY(hy, edge[0]), cornerZ(hz, edge[0]),
                    cornerX(hx, edge[1]), cornerY(hy, edge[1]), cornerZ(hz, edge[1]),
                    camX, camY, camZ, color, halfWidth);
        }
    }

    private static void addLineQuad(BufferBuilder buffer, double startX, double startY, double startZ,
            double endX, double endY, double endZ, double camX, double camY, double camZ, int color,
            double halfWidth) {
        double directionX = endX - startX;
        double directionY = endY - startY;
        double directionZ = endZ - startZ;
        double directionLength = Math.sqrt(directionX * directionX + directionY * directionY
                + directionZ * directionZ);
        if (directionLength < 1.0E-8 || halfWidth < 1.0E-8) {
            return;
        }
        directionX /= directionLength;
        directionY /= directionLength;
        directionZ /= directionLength;

        double midpointX = (startX + endX) * 0.5;
        double midpointY = (startY + endY) * 0.5;
        double midpointZ = (startZ + endZ) * 0.5;
        // Face the camera: the camera is at (camX, camY, camZ) in this local
        // space, so widen the edge quad along the perpendicular towards it.
        double towardCameraX = camX - midpointX;
        double towardCameraY = camY - midpointY;
        double towardCameraZ = camZ - midpointZ;
        double towardCameraLength = Math.sqrt(towardCameraX * towardCameraX + towardCameraY * towardCameraY
                + towardCameraZ * towardCameraZ);
        if (towardCameraLength < 1.0E-8) {
            towardCameraX = -midpointX;
            towardCameraY = -midpointY;
            towardCameraZ = -midpointZ;
            towardCameraLength = Math.sqrt(towardCameraX * towardCameraX + towardCameraY * towardCameraY
                    + towardCameraZ * towardCameraZ);
            if (towardCameraLength < 1.0E-8) {
                return;
            }
        }
        towardCameraX /= towardCameraLength;
        towardCameraY /= towardCameraLength;
        towardCameraZ /= towardCameraLength;

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
                return;
            }
        }

        offsetX = offsetX / offsetLength * halfWidth;
        offsetY = offsetY / offsetLength * halfWidth;
        offsetZ = offsetZ / offsetLength * halfWidth;
        quad(buffer,
                startX + offsetX, startY + offsetY, startZ + offsetZ,
                startX - offsetX, startY - offsetY, startZ - offsetZ,
                endX - offsetX, endY - offsetY, endZ - offsetZ,
                endX + offsetX, endY + offsetY, endZ + offsetZ, color);
    }

    private static double projectionScale() {
        MinecraftClient client = MinecraftClient.getInstance();
        int framebufferHeight = Math.max(1, client.getWindow().getFramebufferHeight());
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
        return config.getColor().toVanillaArgb();
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

    private static void quad(BufferBuilder buffer, double x1, double y1, double z1, double x2, double y2,
            double z2, double x3, double y3, double z3, double x4, double y4, double z4, int color) {
        vertex(buffer, x1, y1, z1, color);
        vertex(buffer, x2, y2, z2, color);
        vertex(buffer, x3, y3, z3, color);
        vertex(buffer, x4, y4, z4, color);
    }

    private static void vertex(BufferBuilder buffer, double x, double y, double z, int color) {
        buffer.vertex((float) x, (float) y, (float) z)
                .color((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, (color >>> 24) & 0xFF);
    }

    private static int withAlpha(int color, int alpha) {
        int clamped = MathHelper.clamp(alpha, 0, 255);
        return (clamped << 24) | (color & 0x00FFFFFF);
    }

    private static double cornerX(double halfExtent, int index) {
        return (index & 4) == 0 ? -halfExtent : halfExtent;
    }

    private static double cornerY(double halfExtent, int index) {
        return (index & 2) == 0 ? -halfExtent : halfExtent;
    }

    private static double cornerZ(double halfExtent, int index) {
        return (index & 1) == 0 ? -halfExtent : halfExtent;
    }

    private record Indicator(BlockPos position, float progress) {
    }
}
