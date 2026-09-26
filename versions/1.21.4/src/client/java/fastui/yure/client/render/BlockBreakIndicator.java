package fastui.yure.client.render;

import fastui.yure.client.mixin.ClientPlayerInteractionManagerAccessor;
import fastui.yure.client.mixin.WorldRendererAccessor;
import fastui.yure.config.FastMasaConfigs;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.BlockBreakingInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Renders the 1.21.8 equivalent of Meteor's expanding block-break indicator. */
public final class BlockBreakIndicator {
    private static final RenderLayer SIDE_LAYER = RenderLayer.of("fastui_block_break_indicator_sides", 1536, false,
            true, MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL,
            RenderLayer.MultiPhaseParameters.builder().build(false));

    private BlockBreakIndicator() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (context.matrixStack() != null && context.consumers() != null) {
                render(context.matrixStack(), context.consumers(), context.camera());
            }
        });
    }

    private static void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!FastMasaConfigs.Generic.BLOCK_BREAK_INDICATOR.getBooleanValue()
                || client.world == null || client.interactionManager == null) {
            return;
        }

        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        ClientPlayerInteractionManagerAccessor accessor = (ClientPlayerInteractionManagerAccessor) interactionManager;
        BlockPos ownPosition = accessor.fastui$getCurrentBreakingPos();
        float ownProgress = accessor.fastui$getCurrentBreakingProgress();
        if (ownPosition != null && ownProgress > 0.0F) {
            addIndicator(client.world, ownPosition, ownProgress, matrices, consumers, camera.getPos());
        }

        if (FastMasaConfigs.Generic.BLOCK_BREAK_REMOTE.getBooleanValue()) {
            WorldRendererAccessor worldRenderer = (WorldRendererAccessor) client.worldRenderer;
            for (BlockBreakingInfo progress : worldRenderer.fastui$getBlockBreakingInfos().values()) {
                BlockPos position = progress.getPos();
                if (!position.equals(ownPosition)) {
                    addIndicator(client.world, position, (progress.getStage() + 1) / 9.0F, matrices, consumers,
                            camera.getPos());
                }
            }
        }
    }

    private static void addIndicator(World world, BlockPos position, float progress, MatrixStack matrices,
            VertexConsumerProvider consumers, Vec3d cameraPosition) {
        float normalizedProgress = clamp(progress, 0.0F, 1.0F);
        var shape = world.getBlockState(position).getOutlineShape(world, position);
        if (shape.isEmpty()) {
            return;
        }

        Box bounds = shape.getBoundingBox();
        // Match the referenced Meteor animation: the box grows out from the block center.
        double halfWidth = (bounds.maxX - bounds.minX) * normalizedProgress / 2.0;
        double halfHeight = (bounds.maxY - bounds.minY) * normalizedProgress / 2.0;
        double halfDepth = (bounds.maxZ - bounds.minZ) * normalizedProgress / 2.0;
        double centerX = position.getX() + (bounds.minX + bounds.maxX) / 2.0;
        double centerY = position.getY() + (bounds.minY + bounds.maxY) / 2.0;
        double centerZ = position.getZ() + (bounds.minZ + bounds.maxZ) / 2.0;
        Box box = new Box(centerX - halfWidth, centerY - halfHeight,
                centerZ - halfDepth, centerX + halfWidth, centerY + halfHeight, centerZ + halfDepth);

        int lineColor = FastMasaConfigs.Generic.BLOCK_BREAK_LINES.getBooleanValue()
                ? lerpArgb(normalizedProgress, color(FastMasaConfigs.Generic.BLOCK_BREAK_START_LINE),
                        color(FastMasaConfigs.Generic.BLOCK_BREAK_END_LINE))
                : 0;
        int sideColor = FastMasaConfigs.Generic.BLOCK_BREAK_SIDES.getBooleanValue()
                ? halfAlpha(lerpArgb(normalizedProgress, color(FastMasaConfigs.Generic.BLOCK_BREAK_START_SIDE),
                        color(FastMasaConfigs.Generic.BLOCK_BREAK_END_SIDE)))
                : 0;

        Box cameraRelativeBox = box.offset(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        VertexConsumer indicator = consumers.getBuffer(SIDE_LAYER);
        if (sideColor != 0) {
            drawBoxSides(matrices, indicator, cameraRelativeBox, sideColor);
        }
        if (lineColor != 0) {
            drawBoxLines(matrices, indicator, cameraRelativeBox, lineColor,
                    FastMasaConfigs.Generic.BLOCK_BREAK_LINE_WIDTH.getIntegerValue());
        }
    }

    // Render line width as camera-facing quads. Core OpenGL renderers commonly clamp native lines to one pixel.
    private static void drawBoxLines(MatrixStack matrices, VertexConsumer vertices, Box box, int color, int width) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        line(matrices, vertices, x1, y1, z1, x1, y2, z1, color, width);
        line(matrices, vertices, x1, y1, z2, x1, y2, z2, color, width);
        line(matrices, vertices, x2, y1, z1, x2, y2, z1, color, width);
        line(matrices, vertices, x2, y1, z2, x2, y2, z2, color, width);
        line(matrices, vertices, x1, y1, z1, x1, y1, z2, color, width);
        line(matrices, vertices, x2, y1, z1, x2, y1, z2, color, width);
        line(matrices, vertices, x1, y1, z1, x2, y1, z1, color, width);
        line(matrices, vertices, x1, y1, z2, x2, y1, z2, color, width);
        line(matrices, vertices, x1, y2, z1, x1, y2, z2, color, width);
        line(matrices, vertices, x2, y2, z1, x2, y2, z2, color, width);
        line(matrices, vertices, x1, y2, z1, x2, y2, z1, color, width);
        line(matrices, vertices, x1, y2, z2, x2, y2, z2, color, width);
    }

    private static void drawBoxSides(MatrixStack matrices, VertexConsumer vertices, Box box, int color) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        quad(matrices, vertices, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, color);
        quad(matrices, vertices, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, color);
        quad(matrices, vertices, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color);
        quad(matrices, vertices, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, color);
        quad(matrices, vertices, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, color);
        quad(matrices, vertices, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, color);
    }

    private static void line(MatrixStack matrices, VertexConsumer vertices, double x1, double y1, double z1,
            double x2, double y2, double z2, int color, int width) {
        Vec3d start = new Vec3d(x1, y1, z1);
        Vec3d end = new Vec3d(x2, y2, z2);
        Vec3d direction = end.subtract(start).normalize();
        Vec3d midpoint = start.add(end).multiply(0.5);
        Vec3d towardCamera = midpoint.negate().normalize();
        Vec3d offset = direction.crossProduct(towardCamera);
        if (offset.lengthSquared() < 1.0E-8) {
            offset = direction.crossProduct(new Vec3d(0.0, 1.0, 0.0));
            if (offset.lengthSquared() < 1.0E-8) {
                offset = direction.crossProduct(new Vec3d(1.0, 0.0, 0.0));
            }
        }

        offset = offset.normalize().multiply(lineHalfWidth(midpoint, width));
        Vec3d startLeft = start.add(offset);
        Vec3d startRight = start.subtract(offset);
        Vec3d endRight = end.subtract(offset);
        Vec3d endLeft = end.add(offset);
        quad(matrices, vertices, startLeft.x, startLeft.y, startLeft.z, startRight.x, startRight.y, startRight.z,
                endRight.x, endRight.y, endRight.z, endLeft.x, endLeft.y, endLeft.z, color);
    }

    private static double lineHalfWidth(Vec3d midpoint, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        int framebufferHeight = Math.max(1, client.getWindow().getFramebufferHeight());
        double distance = Math.max(0.1, midpoint.length());
        double fovRadians = Math.toRadians(client.options.getFov().getValue());
        double unitsPerPixel = 2.0 * distance * Math.tan(fovRadians / 2.0) / framebufferHeight;
        return Math.max(0.0005, unitsPerPixel * Math.max(1, width) / 2.0);
    }

    private static void quad(MatrixStack matrices, VertexConsumer vertices, double x1, double y1, double z1,
            double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4,
            int color) {
        vertex(matrices, vertices, x1, y1, z1, color);
        vertex(matrices, vertices, x2, y2, z2, color);
        vertex(matrices, vertices, x3, y3, z3, color);
        vertex(matrices, vertices, x4, y4, z4, color);
    }

    private static void vertex(MatrixStack matrices, VertexConsumer vertices, double x, double y, double z, int color) {
        vertices.vertex(matrices.peek(), (float) x, (float) y, (float) z)
                .color(red(color), green(color), blue(color), alpha(color));
    }

    private static int color(fi.dy.masa.malilib.config.options.ConfigColor config) {
        return config.getColor().toVanillaArgb();
    }

    private static int lerpArgb(float delta, int start, int end) {
        return argb(lerp(delta, alpha(start), alpha(end)), lerp(delta, red(start), red(end)),
                lerp(delta, green(start), green(end)), lerp(delta, blue(start), blue(end)));
    }

    private static int halfAlpha(int color) {
        return argb(alpha(color) / 2, red(color), green(color), blue(color));
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int alpha(int color) { return color >>> 24 & 0xFF; }
    private static int red(int color) { return color >>> 16 & 0xFF; }
    private static int green(int color) { return color >>> 8 & 0xFF; }
    private static int blue(int color) { return color & 0xFF; }
    private static int lerp(float delta, int start, int end) { return Math.round(start + (end - start) * delta); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
}
