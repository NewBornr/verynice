package com.example.myclient.render;

import com.example.myclient.MyClient;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Boxes through walls, using the extraction/drawing split from Fabric's 26.x rendering docs.
 * Modules add boxes to a Collector during extraction; drawing happens after translucent terrain.
 * Outlines are drawn as thin filled boxes so only the filled-box pipeline is needed.
 */
public final class Render3D {
    private Render3D() {}

    public record Shape(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                        int argb, boolean fill) {}

    /** Limit per frame (nearest first) so the staging buffer never overflows. */
    private static final int MAX_SHAPES = 100;

    public static final class Collector {
        private final List<Shape> shapes = new ArrayList<>();
        private final Vec3 origin;

        Collector(Vec3 origin) { this.origin = origin; }

        public void box(AABB b, int argb, boolean fill) {
            shapes.add(new Shape(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, argb, fill));
        }

        List<Shape> finish() {
            shapes.sort(Comparator.comparingDouble(s -> {
                double dx = (s.minX() + s.maxX()) / 2 - origin.x;
                double dy = (s.minY() + s.maxY()) / 2 - origin.y;
                double dz = (s.minZ() + s.maxZ()) / 2 - origin.z;
                return dx * dx + dy * dy + dz * dz;
            }));
            return List.copyOf(shapes.size() > MAX_SHAPES ? shapes.subList(0, MAX_SHAPES) : shapes);
        }
    }

    private static final RenderPipeline THROUGH_WALLS = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("myclient", "pipeline/esp_through_walls"))
                    .withDepthStencilState(Optional.empty())
                    .build());

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer STAGED = new StagedVertexBuffer(() -> "MyClient ESP", RenderType.SMALL_BUFFER_SIZE);

    private static volatile List<Shape> frame = List.of();

    // ------------------------------------------------------------------ extraction phase
    public static void extract(LevelExtractionContext context) {
        var player = Minecraft.getInstance().player;
        if (player == null || MyClient.modules == null) { frame = List.of(); return; }
        Collector collector = new Collector(player.getEyePosition());
        MyClient.modules.collect3D(collector);
        frame = collector.finish();
    }

    // ------------------------------------------------------------------ drawing phase
    public static void draw(LevelRenderContext context) {
        List<Shape> shapes = frame;
        if (shapes.isEmpty()) return;

        VertexFormat formatBinding = THROUGH_WALLS.getVertexFormatBinding(0);
        if (formatBinding == null) return;

        PrimitiveTopology primitive = THROUGH_WALLS.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = STAGED.appendDraw(formatBinding, primitive,
                primitive == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null);

        PoseStack poseStack = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        VertexConsumer builder = STAGED.getVertexBuilder(draw);
        Matrix4fc matrix = poseStack.last().pose();
        for (Shape s : shapes) writeShape(matrix, builder, s);

        poseStack.popPose();

        STAGED.upload();
        StagedVertexBuffer.ExecuteInfo info = STAGED.getExecuteInfo(draw);
        if (info != null) execute(Minecraft.getInstance(), info, THROUGH_WALLS);
        STAGED.endFrame();
    }

    private static void writeShape(Matrix4fc m, VertexConsumer b, Shape s) {
        float r = ((s.argb() >> 16) & 255) / 255f;
        float g = ((s.argb() >> 8) & 255) / 255f;
        float bl = (s.argb() & 255) / 255f;
        float a = ((s.argb() >>> 24) & 255) / 255f;
        float x1 = (float) s.minX(), y1 = (float) s.minY(), z1 = (float) s.minZ();
        float x2 = (float) s.maxX(), y2 = (float) s.maxY(), z2 = (float) s.maxZ();

        if (s.fill()) filledBox(m, b, x1, y1, z1, x2, y2, z2, r, g, bl, 0.18f);

        float t = 0.02f; // outline thickness in blocks
        for (float y : new float[]{y1, y2}) {
            for (float z : new float[]{z1, z2}) filledBox(m, b, x1, y - t, z - t, x2, y + t, z + t, r, g, bl, a); // along X
        }
        for (float x : new float[]{x1, x2}) {
            for (float z : new float[]{z1, z2}) filledBox(m, b, x - t, y1, z - t, x + t, y2, z + t, r, g, bl, a); // along Y
        }
        for (float x : new float[]{x1, x2}) {
            for (float y : new float[]{y1, y2}) filledBox(m, b, x - t, y - t, z1, x + t, y + t, z2, r, g, bl, a); // along Z
        }
    }

    private static void filledBox(Matrix4fc m, VertexConsumer b, float minX, float minY, float minZ,
                                  float maxX, float maxY, float maxZ, float r, float g, float bl, float a) {
        // front
        b.addVertex(m, minX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, maxY, maxZ).setColor(r, g, bl, a);
        // back
        b.addVertex(m, maxX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, maxY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, maxY, minZ).setColor(r, g, bl, a);
        // left
        b.addVertex(m, minX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, maxY, minZ).setColor(r, g, bl, a);
        // right
        b.addVertex(m, maxX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, maxY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, maxY, maxZ).setColor(r, g, bl, a);
        // top
        b.addVertex(m, minX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, maxY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, maxY, minZ).setColor(r, g, bl, a);
        // bottom
        b.addVertex(m, minX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, minY, maxZ).setColor(r, g, bl, a);
    }

    private static void execute(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainTarget.getColorTextureView();
        if (colorTexture == null) return;

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "myclient esp", colorTexture, Optional.empty(), mainTarget.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.setVertexBuffer(0, info.vertexBuffer().slice());
            pass.setIndexBuffer(info.indexBuffer(), info.indexType());
            pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }

    public static void close() { STAGED.close(); }
}
