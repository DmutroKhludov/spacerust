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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders placed 3D CoilRust player base pieces (Twig -> Wood -> Stone -> Metal -> Armored/HQM)
 * plus the live translucent blue/red Building Plan ghost preview.
 */
public final class RustBuildingRenderer {

    private static final Identifier TEX_TWIG  = new Identifier("spacerust", "textures/building/twig_map.png");
    private static final Identifier TEX_BUILD = new Identifier("spacerust", "textures/building/build_map.png");
    private static final Identifier TEX_HQM   = new Identifier("spacerust", "textures/building/hqm_map.png");
    private static final Map<String, RustObjModel> MODEL_CACHE = new HashMap<>();

    private RustBuildingRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.world == null || client.player == null) return;

            List<RustBuildingSystem.BuildingPiece> pieces = RustBuildingSystem.pieces;
            boolean holdingPlan = RustBuildingSystem.isHoldingPlan(client);
            boolean holdingDeployable = RustBuildingSystem.isHoldingDeployable(client);
            if ((pieces == null || pieces.isEmpty()) && !holdingPlan && !holdingDeployable) return;

            Vec3d cam = ctx.camera().getPos();
            MatrixStack matrices = ctx.matrixStack();

            RenderSystem.pushMatrix();
            RenderSystem.enableTexture();
            RenderSystem.enableDepthTest();
            RenderSystem.disableLighting();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            client.gameRenderer.getLightmapTextureManager().disable();

            // 1. Render all solid placed 3D Rust Building Pieces nearby
            if (pieces != null && !pieces.isEmpty()) {
                for (RustBuildingSystem.BuildingPiece p : pieces) {
                    double dx = p.x - cam.x;
                    double dy = p.y - cam.y;
                    double dz = p.z - cam.z;
                    if (dx * dx + dy * dy + dz * dz > 115.0 * 115.0) continue;

                    RustObjModel model = getModel(p.type, p.tier);
                    if (model == null || model.vertexCount == 0) continue;

                    client.getTextureManager().bindTexture(getTexture(p.tier));
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

                    matrices.push();
                    matrices.translate(dx, dy, dz);
                    matrices.multiply(new Quaternion(0f, p.yaw, 0f, true));
                    drawModel(matrices.peek().getModel(), model, 1.0f, 1.0f, 1.0f, 1.0f);
                    matrices.pop();
                }
            }

            // 2. Render live translucent CoilRust blueprint ghost preview while holding Plan
            if (holdingPlan && client.currentScreen == null) {
                RustBuildingSystem.SnapTarget snap = RustBuildingSystem.computeSnapTarget(client);
                if (snap != null) {
                    RustObjModel model = getModel(snap.type, "twig");
                    if (model != null && model.vertexCount > 0) {
                        double dx = snap.x - cam.x;
                        double dy = snap.y - cam.y;
                        double dz = snap.z - cam.z;

                        client.getTextureManager().bindTexture(TEX_TWIG);
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        RenderSystem.depthMask(false);

                        matrices.push();
                        matrices.translate(dx, dy, dz);
                        matrices.multiply(new Quaternion(0f, snap.yaw, 0f, true));
                        if (snap.valid) {
                            drawModel(matrices.peek().getModel(), model, 0.25f, 0.78f, 1.0f, 0.56f);
                        } else {
                            drawModel(matrices.peek().getModel(), model, 1.0f, 0.28f, 0.24f, 0.50f);
                        }
                        matrices.pop();

                        RenderSystem.depthMask(true);
                        RenderSystem.disableBlend();
                    }
                }
            }

            // 3. Translucent blue/red 3D Deployable Ghost Preview (Video 3: Boxes / Furnace / Campfire / Bed)
            if (holdingDeployable && client.currentScreen == null) {
                Vec3d eye = client.player.getCameraPosVec(1.0f);
                Vec3d dir = client.player.getRotationVec(1.0f).normalize();
                RustBuildingSystem.BuildHit bh = RustBuildingSystem.raycastBuildings(eye, dir, 6.0);
                Vec3d target = null;
                boolean canPlace = false;
                if (bh != null) {
                    target = bh.hitPos;
                    canPlace = true;
                } else if (client.crosshairTarget != null
                        && client.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
                    target = client.crosshairTarget.getPos();
                    canPlace = true;
                }
                if (target != null) {
                    double dx = target.x - cam.x;
                    double dy = target.y - cam.y;
                    double dz = target.z - cam.z;
                    float cr = canPlace ? 0.25f : 1.0f;
                    float cg = canPlace ? 0.78f : 0.28f;
                    float cb = canPlace ? 1.0f : 0.24f;
                    RenderSystem.disableTexture();
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.depthMask(false);
                    matrices.push();
                    matrices.translate(dx, dy, dz);
                    matrices.multiply(new Quaternion(0f, -client.player.yaw, 0f, true));
                    Matrix4f dm = matrices.peek().getModel();
                    Tessellator tess = Tessellator.getInstance();
                    BufferBuilder buf = tess.getBuffer();
                    buf.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
                    float hx = 0.45f, hy = 0.75f, hz = 0.35f;
                    // Top, Sides box ghost
                    buf.vertex(dm, -hx, hy, -hz).color(cr, cg, cb, 0.48f).next();
                    buf.vertex(dm, -hx, hy,  hz).color(cr, cg, cb, 0.48f).next();
                    buf.vertex(dm,  hx, hy,  hz).color(cr, cg, cb, 0.48f).next();
                    buf.vertex(dm,  hx, hy, -hz).color(cr, cg, cb, 0.48f).next();
                    buf.vertex(dm, -hx, 0f,  hz).color(cr, cg, cb, 0.42f).next();
                    buf.vertex(dm,  hx, 0f,  hz).color(cr, cg, cb, 0.42f).next();
                    buf.vertex(dm,  hx, hy,  hz).color(cr, cg, cb, 0.42f).next();
                    buf.vertex(dm, -hx, hy,  hz).color(cr, cg, cb, 0.42f).next();
                    buf.vertex(dm,  hx, 0f, -hz).color(cr, cg, cb, 0.42f).next();
                    buf.vertex(dm, -hx, 0f, -hz).color(cr, cg, cb, 0.42f).next();
                    buf.vertex(dm, -hx, hy, -hz).color(cr, cg, cb, 0.42f).next();
                    buf.vertex(dm,  hx, hy, -hz).color(cr, cg, cb, 0.42f).next();
                    buf.vertex(dm, -hx, 0f, -hz).color(cr, cg, cb, 0.40f).next();
                    buf.vertex(dm, -hx, 0f,  hz).color(cr, cg, cb, 0.40f).next();
                    buf.vertex(dm, -hx, hy,  hz).color(cr, cg, cb, 0.40f).next();
                    buf.vertex(dm, -hx, hy, -hz).color(cr, cg, cb, 0.40f).next();
                    buf.vertex(dm,  hx, 0f,  hz).color(cr, cg, cb, 0.40f).next();
                    buf.vertex(dm,  hx, 0f, -hz).color(cr, cg, cb, 0.40f).next();
                    buf.vertex(dm,  hx, hy, -hz).color(cr, cg, cb, 0.40f).next();
                    buf.vertex(dm,  hx, hy,  hz).color(cr, cg, cb, 0.40f).next();
                    tess.draw();
                    matrices.pop();
                    RenderSystem.depthMask(true);
                    RenderSystem.disableBlend();
                    RenderSystem.enableTexture();
                }
            }

            client.gameRenderer.getLightmapTextureManager().enable();
            RenderSystem.enableCull();
            RenderSystem.popMatrix();
        });
    }

    private static void drawModel(Matrix4f mat, RustObjModel model, float cr, float cg, float cb, float ca) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE);
        float[] v = model.vertices;
        for (int i = 0; i < v.length; i += 8) {
            float nx = v[i + 5], ny = v[i + 6];
            float shade = Math.min(1.0f, 0.78f + 0.22f * Math.max(-0.5f, Math.min(1.0f, 0.75f * ny + 0.25f * nx)));
            buf.vertex(mat, v[i], v[i + 1], v[i + 2])
               .color(cr * shade, cg * shade, cb * shade, ca)
               .texture(v[i + 3], v[i + 4])
               .next();
        }
        tess.draw();
    }

    private static RustObjModel getModel(String type, String tier) {
        String mapped = "ramp".equals(type) ? "stairs" : type;
        String key = mapped + "_" + tier;
        return MODEL_CACHE.computeIfAbsent(key, k ->
                RustObjModel.loadGz(new Identifier("spacerust", "models/building/" + k + ".obj.gz")));
    }

    private static Identifier getTexture(String tier) {
        if ("twig".equals(tier)) return TEX_TWIG;
        if ("armored".equals(tier)) return TEX_HQM;
        return TEX_BUILD;
    }
}
