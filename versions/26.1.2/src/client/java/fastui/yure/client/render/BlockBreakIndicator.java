package fastui.yure.client.render;

import fastui.yure.FastMasaConfig;
import fastui.yure.client.mixin.ClientLevelRendererAccessor;
import fastui.yure.client.mixin.ClientPlayerInteractionManagerAccessor;
import fastui.yure.config.FastMasaConfigs;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Client-only replacement for the vanilla break-crack overlay. Progress is read
 * exclusively from Minecraft's local and server-synchronised render state.
 */
public final class BlockBreakIndicator {
    private static final RenderPipeline LINES_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(FastMasaConfig.MOD_ID, "break_indicator_lines"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
            .withVertexShader("core/rendertype_lines")
            .withFragmentShader("core/rendertype_lines")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .build();
    private static final RenderPipeline FILLED_BOX_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(FastMasaConfig.MOD_ID, "break_indicator_filled_box"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .build();
    private static final RenderType LINES = RenderType.create("fastui_break_indicator_lines", RenderSetup.builder(
            LINES_PIPELINE)
            .createRenderSetup());
    private static final RenderType FILLED_BOX = RenderType.create("fastui_break_indicator_filled_box", RenderSetup.builder(
            FILLED_BOX_PIPELINE)
            .sortOnUpload()
            .createRenderSetup());

    private BlockBreakIndicator() {
    }

    public static void precompilePipelines(ResourceManager resourceManager) {
        GpuDevice device = RenderSystem.getDevice();
        for (RenderPipeline pipeline : new RenderPipeline[] {LINES_PIPELINE, FILLED_BOX_PIPELINE}) {
            device.precompilePipeline(pipeline, (identifier, _) -> {
                try (var stream = resourceManager.getResource(identifier).orElseThrow().open()) {
                    return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException exception) {
                    throw new UncheckedIOException("Failed to load break indicator shader " + identifier, exception);
                }
            });
        }
    }

    public static void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Vec3 cameraPosition) {
        Minecraft client = Minecraft.getInstance();
        if (isEnabled() == false || client.level == null || client.gameMode == null) {
            return;
        }

        ClientLevel level = client.level;
        ClientPlayerInteractionManagerAccessor gameMode = (ClientPlayerInteractionManagerAccessor) client.gameMode;
        BlockPos ownPosition = gameMode.fastui$getDestroyBlockPos();
        float ownProgress = gameMode.fastui$getDestroyProgress();

        if (ownProgress > 0.0F && ownPosition != null) {
            addIndicator(level, ownPosition, ownProgress, bufferSource, poseStack, cameraPosition);
        }

        if (FastMasaConfigs.Generic.BLOCK_BREAK_REMOTE.getBooleanValue()) {
            ClientLevelRendererAccessor levelRenderer = (ClientLevelRendererAccessor) client.levelRenderer;
            for (BlockDestructionProgress progress : levelRenderer.fastui$getDestroyingBlocks().values()) {
                BlockPos position = progress.getPos();
                if (position.equals(ownPosition)) {
                    continue;
                }
                addIndicator(level, position, (progress.getProgress() + 1) / 9.0F, bufferSource, poseStack,
                        cameraPosition);
            }
        }

        bufferSource.endBatch(LINES);
        bufferSource.endBatch(FILLED_BOX);
    }

    private static boolean isEnabled() {
        return FastMasaConfigs.Generic.BLOCK_BREAK_INDICATOR.getBooleanValue();
    }

    private static void addIndicator(ClientLevel level, BlockPos position, float progress,
            MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Vec3 cameraPosition) {
        float normalizedProgress = Math.clamp(progress, 0.0F, 1.0F);
        BlockState state = level.getBlockState(position);
        VoxelShape shape = state.getShape(level, position);
        if (shape.isEmpty()) {
            return;
        }

        AABB shapeBounds = shape.bounds();
        double scale = 1.0 - normalizedProgress;
        double halfWidth = shapeBounds.getXsize() * scale / 2.0;
        double halfHeight = shapeBounds.getYsize() * scale / 2.0;
        double halfDepth = shapeBounds.getZsize() * scale / 2.0;
        double centerX = position.getX() + (shapeBounds.minX + shapeBounds.maxX) / 2.0;
        double centerY = position.getY() + (shapeBounds.minY + shapeBounds.maxY) / 2.0;
        double centerZ = position.getZ() + (shapeBounds.minZ + shapeBounds.maxZ) / 2.0;
        AABB box = new AABB(centerX - halfWidth, centerY - halfHeight, centerZ - halfDepth,
                centerX + halfWidth, centerY + halfHeight, centerZ + halfDepth);

        int strokeStart = FastMasaConfigs.Generic.BLOCK_BREAK_START_LINE.getColor().toVanillaArgb();
        int strokeEnd = FastMasaConfigs.Generic.BLOCK_BREAK_END_LINE.getColor().toVanillaArgb();
        int fillStart = halfAlpha(FastMasaConfigs.Generic.BLOCK_BREAK_START_SIDE.getColor().toVanillaArgb());
        int fillEnd = halfAlpha(FastMasaConfigs.Generic.BLOCK_BREAK_END_SIDE.getColor().toVanillaArgb());
        int stroke = FastMasaConfigs.Generic.BLOCK_BREAK_LINES.getBooleanValue()
                ? ARGB.srgbLerp(normalizedProgress, strokeStart, strokeEnd) : 0;
        int fill = FastMasaConfigs.Generic.BLOCK_BREAK_SIDES.getBooleanValue()
                ? ARGB.srgbLerp(normalizedProgress, fillStart, fillEnd) : 0;
        if (stroke == 0 && fill == 0) {
            return;
        }
        if (stroke != 0) {
            ShapeRenderer.renderShape(poseStack, bufferSource.getBuffer(LINES), Shapes.create(box),
                    -cameraPosition.x, -cameraPosition.y, -cameraPosition.z, stroke,
                    FastMasaConfigs.Generic.BLOCK_BREAK_LINE_WIDTH.getIntegerValue());
        }
        if (fill != 0) {
            renderFilledBox(poseStack, bufferSource.getBuffer(FILLED_BOX), box, cameraPosition, fill);
        }
    }

    private static int halfAlpha(int color) {
        return ARGB.color(ARGB.alpha(color) / 2, ARGB.red(color), ARGB.green(color), ARGB.blue(color));
    }

    private static void renderFilledBox(PoseStack poseStack, VertexConsumer buffer, AABB box, Vec3 cameraPosition,
            int color) {
        double x0 = box.minX - cameraPosition.x;
        double y0 = box.minY - cameraPosition.y;
        double z0 = box.minZ - cameraPosition.z;
        double x1 = box.maxX - cameraPosition.x;
        double y1 = box.maxY - cameraPosition.y;
        double z1 = box.maxZ - cameraPosition.z;

        addQuad(poseStack, buffer, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, color);
        addQuad(poseStack, buffer, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, color);
        addQuad(poseStack, buffer, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, color);
        addQuad(poseStack, buffer, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, color);
        addQuad(poseStack, buffer, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, color);
        addQuad(poseStack, buffer, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, color);
    }

    private static void addQuad(PoseStack poseStack, VertexConsumer buffer, double ax, double ay, double az,
            double bx, double by, double bz, double cx, double cy, double cz, double dx, double dy, double dz,
            int color) {
        buffer.addVertex(poseStack.last(), (float) ax, (float) ay, (float) az).setColor(color);
        buffer.addVertex(poseStack.last(), (float) bx, (float) by, (float) bz).setColor(color);
        buffer.addVertex(poseStack.last(), (float) cx, (float) cy, (float) cz).setColor(color);
        buffer.addVertex(poseStack.last(), (float) dx, (float) dy, (float) dz).setColor(color);
    }

}
