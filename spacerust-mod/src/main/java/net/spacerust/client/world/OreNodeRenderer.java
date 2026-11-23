package net.spacerust.client.world;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Renders 3D Rust Ore Nodes (Stone, Metal, Sulfur stages 1..4) in the world
 * using authentic CoilRust .obj.gz models, albedo textures, and the sparkling hotspot star.
 */
public final class OreNodeRenderer {

    private static final Identifier TEX_STONE  = new Identifier("spacerust", "textures/ore/stone_albedo.png");
    private static final Identifier TEX_METAL  = new Identifier("spacerust", "textures/ore/metal_albedo.png");
    private static final Identifier TEX_SULFUR = new Identifier("spacerust", "textures/ore/sulfur_albedo.png");
    private static final Identifier TEX_FLARE  = new Identifier("spacerust", "textures/ore/flare_ore.png");

    private static class Spark {
        final double x, y, z;
        final long startMs;

        Spark(double x, double y, double z, long startMs) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.startMs = startMs;
        }
    }

    private static final Map<String, RustObjModel> MODEL_CACHE = new HashMap<>();
    private static final List<Spark> SPARKS = new ArrayList<>();

    private OreNodeRenderer() {}

    public static void triggerHotspotSpark(double x, double y, double z) {
        SPARKS.add(new Spark(x, y, z, System.currentTimeMillis()));
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            List<RustWorldSync.OreNode> nodes = RustWorldSync.oreNodes;
            if ((nodes == null || nodes.isEmpty()) && SPARKS.isEmpty()) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.world == null || client.player == null) return;

            Vec3d cam = ctx.camera().getPos();
            MatrixStack matrices = ctx.matrixStack();

            RenderSystem.pushMatrix();
            RenderSystem.enableTexture();
            RenderSystem.enableDepthTest();
            RenderSystem.disableLighting();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            client.gameRenderer.getLightmapTextureManager().disable();

            if (nodes != null) {
                for (RustWorldSync.OreNode node : nodes) {
                    double dx = node.x - cam.x;
                    double dy = node.y - cam.y;
                    double dz = node.z - cam.z;
                    if (dx * dx + dy * dy + dz * dz > 96.0 * 96.0) continue;

                    RustObjModel model = getModel(node.kind, node.stage);
                    if (model == null || model.vertexCount == 0) continue;

                    client.getTextureManager().bindTexture(getTexture(node.kind));
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

                    matrices.push();
                    matrices.translate(dx, dy - 0.15, dz);
                    float yaw = (node.id * 73) % 360;
                    matrices.multiply(new Quaternion(0f, yaw, 0f, true));
                    float scale = 0.62f;
                    matrices.scale(scale, scale, scale);

                    Matrix4f mat = matrices.peek().getModel();
                    Tessellator tess = Tessellator.getInstance();
                    BufferBuilder buf = tess.getBuffer();
                    buf.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE);
                    float[] v = model.vertices;
                    for (int i = 0; i < v.length; i += 8) {
                        float nx = v[i + 5], ny = v[i + 6];
                        float sun = Math.max(0.68f, Math.min(1.0f, 0.80f + 0.18f * ny + 0.08f * nx));
                        buf.vertex(mat, v[i], v[i + 1], v[i + 2])
                           .color(sun, sun, sun, 1.0f)
                           .texture(v[i + 3], v[i + 4])
                           .next();
                    }
                    tess.draw();
                    matrices.pop();
                }

                // Render glowing sparkling weak-spot star billboards (flare_ore.png)
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                RenderSystem.disableAlphaTest();
                RenderSystem.disableCull();
                RenderSystem.depthMask(false);
                client.getTextureManager().bindTexture(TEX_FLARE);
                Quaternion camRot = ctx.camera().getRotation();
                long now = System.currentTimeMillis();
                float pulse = 0.34f + 0.07f * (float) Math.sin(now * 0.011);
                float spinA = (now * 0.08f) % 360f;
                float spinB = -(now * 0.05f) % 360f;

                for (RustWorldSync.OreNode node : nodes) {
                    double fx = node.x + node.hx - cam.x;
                    double fy = node.y + node.hy - cam.y;
                    double fz = node.z + node.hz - cam.z;
                    if (fx * fx + fy * fy + fz * fz > 52.0 * 52.0) continue;

                    matrices.push();
                    matrices.translate(fx, fy, fz);
                    matrices.multiply(camRot);

                    // Layer 1: bright golden main star
                    matrices.push();
                    matrices.multiply(new Quaternion(0f, 0f, spinA, true));
                    Matrix4f mat = matrices.peek().getModel();
                    Tessellator tess = Tessellator.getInstance();
                    BufferBuilder buf = tess.getBuffer();
                    buf.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
                    buf.vertex(mat, -pulse, -pulse, 0).color(1f, 0.94f, 0.55f, 1.0f).texture(0f, 1f).next();
                    buf.vertex(mat,  pulse, -pulse, 0).color(1f, 0.94f, 0.55f, 1.0f).texture(1f, 1f).next();
                    buf.vertex(mat,  pulse,  pulse, 0).color(1f, 0.94f, 0.55f, 1.0f).texture(1f, 0f).next();
                    buf.vertex(mat, -pulse,  pulse, 0).color(1f, 0.94f, 0.55f, 1.0f).texture(0f, 0f).next();
                    tess.draw();
                    matrices.pop();

                    // Layer 2: secondary shimmering diagonal glint
                    float p2 = pulse * 0.72f;
                    matrices.push();
                    matrices.multiply(new Quaternion(0f, 0f, spinB + 45f, true));
                    Matrix4f mat2 = matrices.peek().getModel();
                    buf.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
                    buf.vertex(mat2, -p2, -p2, 0).color(1f, 1f, 0.85f, 0.90f).texture(0f, 1f).next();
                    buf.vertex(mat2,  p2, -p2, 0).color(1f, 1f, 0.85f, 0.90f).texture(1f, 1f).next();
                    buf.vertex(mat2,  p2,  p2, 0).color(1f, 1f, 0.85f, 0.90f).texture(1f, 0f).next();
                    buf.vertex(mat2, -p2,  p2, 0).color(1f, 1f, 0.85f, 0.90f).texture(0f, 0f).next();
                    tess.draw();
                    matrices.pop();

                    matrices.pop();
                }

                // Render brief star-burst flash on hotspot hit
                Iterator<Spark> si = SPARKS.iterator();
                while (si.hasNext()) {
                    Spark sp = si.next();
                    float age = (now - sp.startMs) / 340.0f;
                    if (age >= 1.0f) {
                        si.remove();
                        continue;
                    }
                    float sr = 0.30f + 0.45f * age;
                    float alpha = 1.0f - age;
                    matrices.push();
                    matrices.translate(sp.x - cam.x, sp.y - cam.y, sp.z - cam.z);
                    matrices.multiply(camRot);
                    matrices.multiply(new Quaternion(0f, 0f, age * 140f, true));
                    Matrix4f smat = matrices.peek().getModel();
                    Tessellator tess = Tessellator.getInstance();
                    BufferBuilder buf = tess.getBuffer();
                    buf.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
                    buf.vertex(smat, -sr, -sr, 0).color(1f, 0.96f, 0.72f, alpha).texture(0f, 1f).next();
                    buf.vertex(smat,  sr, -sr, 0).color(1f, 0.96f, 0.72f, alpha).texture(1f, 1f).next();
                    buf.vertex(smat,  sr,  sr, 0).color(1f, 0.96f, 0.72f, alpha).texture(1f, 0f).next();
                    buf.vertex(smat, -sr,  sr, 0).color(1f, 0.96f, 0.72f, alpha).texture(0f, 0f).next();
                    tess.draw();
                    matrices.pop();
                }

                RenderSystem.depthMask(true);
                RenderSystem.enableAlphaTest();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableBlend();
            }

            client.gameRenderer.getLightmapTextureManager().enable();
            RenderSystem.enableCull();
            RenderSystem.popMatrix();
        });
    }

    private static RustObjModel getModel(String kind, int stage) {
        String normKind = ("metal".equals(kind) || "sulfur".equals(kind)) ? kind : "stone";
        int s = Math.max(1, Math.min(4, stage));
        String key = normKind + "_" + s;
        return MODEL_CACHE.computeIfAbsent(key, k ->
                RustObjModel.loadGz(new Identifier("spacerust", "models/ore/" + normKind + "/stage_" + s + ".obj.gz")));
    }

    private static Identifier getTexture(String kind) {
        if ("metal".equals(kind)) return TEX_METAL;
        if ("sulfur".equals(kind)) return TEX_SULFUR;
        return TEX_STONE;
    }
}
