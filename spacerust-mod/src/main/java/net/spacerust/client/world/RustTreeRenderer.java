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
 * Renders 3D Rust Trees (Pine, Birch, Oak) using CoilRust bark/foliage textures,
 * painted red X chop minigame markers (cross.png), hit shake, and timber fall animations.
 */
public final class RustTreeRenderer {

    private static final Identifier TEX_LOG_PINE  = new Identifier("spacerust", "textures/trees/log_spruce.png");
    private static final Identifier TEX_LOG_BIRCH = new Identifier("spacerust", "textures/trees/log_birch.png");
    private static final Identifier TEX_LOG_OAK   = new Identifier("spacerust", "textures/trees/log_oak.png");

    private static final Identifier TEX_LEAF_PINE  = new Identifier("spacerust", "textures/trees/leaves_spruce.png");
    private static final Identifier TEX_LEAF_BIRCH = new Identifier("spacerust", "textures/trees/leaves_birch.png");
    private static final Identifier TEX_LEAF_OAK   = new Identifier("spacerust", "textures/trees/leaves_oak.png");

    private static final Identifier TEX_CROSS = new Identifier("spacerust", "textures/farm/cross.png");

    private static class TreeMesh {
        final float[] barkVerts; // stride 8: x, y, z, u, v, nx, ny, nz
        final float[] leafVerts;

        TreeMesh(float[] barkVerts, float[] leafVerts) {
            this.barkVerts = barkVerts;
            this.leafVerts = leafVerts;
        }
    }

    private static class FallingTree {
        final RustWorldSync.RustTree tree;
        final float fallYawDeg;
        final long startMs;

        FallingTree(RustWorldSync.RustTree tree, float fallYawDeg, long startMs) {
            this.tree = tree;
            this.fallYawDeg = fallYawDeg;
            this.startMs = startMs;
        }
    }

    private static final Map<String, TreeMesh> MESH_CACHE = new HashMap<>();
    private static final Map<Long, Long> SHAKE_UNTIL = new HashMap<>();
    private static final List<FallingTree> FALLING = new ArrayList<>();

    private RustTreeRenderer() {}

    public static void triggerTreeShake(double worldX, double worldZ) {
        long k = (((long) Math.round(worldX)) << 32) ^ (Math.round(worldZ) & 0xFFFFFFFFL);
        SHAKE_UNTIL.put(k, System.currentTimeMillis() + 200L);
    }

    public static void triggerFall(int treeId, float fallYawDeg) {
        List<RustWorldSync.RustTree> list = RustWorldSync.trees;
        if (list == null) return;
        for (RustWorldSync.RustTree tr : list) {
            if (tr.id == treeId) {
                FALLING.add(new FallingTree(tr, fallYawDeg, System.currentTimeMillis()));
                break;
            }
        }
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.world == null || client.player == null) return;

            List<RustWorldSync.RustTree> trees = RustWorldSync.trees;
            if ((trees == null || trees.isEmpty()) && FALLING.isEmpty()) return;

            Vec3d cam = ctx.camera().getPos();
            MatrixStack matrices = ctx.matrixStack();
            long now = System.currentTimeMillis();

            RenderSystem.pushMatrix();
            RenderSystem.enableTexture();
            RenderSystem.enableDepthTest();
            RenderSystem.disableLighting();
            client.gameRenderer.getLightmapTextureManager().disable();

            if (trees != null) {
                for (RustWorldSync.RustTree tr : trees) {
                    if ("vanilla".equals(tr.kind)) continue; // Vanilla blocks render natively with CoilRust textures
                    double dx = tr.x - cam.x;
                    double dy = tr.y - cam.y;
                    double dz = tr.z - cam.z;
                    if (dx * dx + dy * dy + dz * dz > 96.0 * 96.0) continue;

                    float shakeDeg = 0f;
                    long sk = (((long) Math.round(tr.x)) << 32) ^ (Math.round(tr.z) & 0xFFFFFFFFL);
                    Long su = SHAKE_UNTIL.get(sk);
                    if (su != null && now < su) {
                        float rem = (su - now) / 200.0f;
                        shakeDeg = (float) Math.sin(rem * Math.PI * 3.0) * 2.4f * rem;
                    }

                    matrices.push();
                    matrices.translate(dx, dy - 0.08, dz);
                    if (shakeDeg != 0f) {
                        matrices.multiply(new Quaternion(shakeDeg, 0f, shakeDeg * 0.6f, true));
                    }
                    float yaw = (tr.id * 97) % 360;
                    matrices.multiply(new Quaternion(0f, yaw, 0f, true));
                    drawTreeMesh(client, tr.kind, matrices);
                    matrices.pop();
                }

                // Render painted Red X markers ("крестики") crisp on tree trunks
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableAlphaTest();
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.08f);
                RenderSystem.disableCull();
                client.getTextureManager().bindTexture(TEX_CROSS);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

                Tessellator tess = Tessellator.getInstance();
                BufferBuilder buf = tess.getBuffer();
                for (RustWorldSync.RustTree tr : trees) {
                    double dx = tr.x - cam.x;
                    double dz = tr.z - cam.z;
                    if (dx * dx + dz * dz > 52.0 * 52.0) continue;

                    double nx = Math.cos(tr.xAngle);
                    double nz = Math.sin(tr.xAngle);
                    // Taper radius slightly at height xY + float slightly outside bark to avoid z-fighting
                    float hRatio = Math.max(0f, Math.min(1f, tr.xY / tr.height));
                    float surfR = "vanilla".equals(tr.kind) ? 0.54f : (tr.radius * (1.0f - hRatio * 0.55f) + 0.045f);

                    double mx = dx + nx * surfR;
                    double my = (tr.y + tr.xY) - cam.y;
                    double mz = dz + nz * surfR;

                    matrices.push();
                    matrices.translate(mx, my, mz);
                    // Face quad outward radially along (nx, nz)
                    float faceYaw = (float) Math.toDegrees(Math.atan2(nx, nz));
                    matrices.multiply(new Quaternion(0f, faceYaw, 0f, true));

                    float sz = 0.24f;
                    Matrix4f mat = matrices.peek().getModel();
                    buf.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
                    buf.vertex(mat, -sz, -sz, 0).color(1.0f, 0.25f, 0.22f, 0.96f).texture(0f, 1f).next();
                    buf.vertex(mat,  sz, -sz, 0).color(1.0f, 0.25f, 0.22f, 0.96f).texture(1f, 1f).next();
                    buf.vertex(mat,  sz,  sz, 0).color(1.0f, 0.25f, 0.22f, 0.96f).texture(1f, 0f).next();
                    buf.vertex(mat, -sz,  sz, 0).color(1.0f, 0.25f, 0.22f, 0.96f).texture(0f, 0f).next();
                    tess.draw();
                    matrices.pop();
                }
            }

            // Render falling felled trees animation (1.35s)
            Iterator<FallingTree> fi = FALLING.iterator();
            while (fi.hasNext()) {
                FallingTree ft = fi.next();
                if ("vanilla".equals(ft.tree.kind)) {
                    fi.remove();
                    continue;
                }
                float p = (now - ft.startMs) / 1350.0f;
                if (p >= 1.0f) {
                    fi.remove();
                    continue;
                }
                float tilt = p * p * 86.0f;
                matrices.push();
                matrices.translate(ft.tree.x - cam.x, ft.tree.y - cam.y - 0.08, ft.tree.z - cam.z);
                matrices.multiply(new Quaternion(0f, -ft.fallYawDeg, 0f, true));
                matrices.multiply(new Quaternion(0f, 0f, -tilt, true));
                drawTreeMesh(client, ft.tree.kind, matrices);
                matrices.pop();
            }

            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            client.gameRenderer.getLightmapTextureManager().enable();
            RenderSystem.popMatrix();
        });
    }

    private static void drawTreeMesh(MinecraftClient client, String kind, MatrixStack matrices) {
        TreeMesh mesh = MESH_CACHE.computeIfAbsent(kind, RustTreeRenderer::buildTreeMesh);
        Matrix4f mat = matrices.peek().getModel();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        // 1. Trunk & limbs
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        client.getTextureManager().bindTexture(getLogTex(kind));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        buf.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE);
        float[] bv = mesh.barkVerts;
        for (int i = 0; i < bv.length; i += 8) {
            float shade = Math.max(0.68f, Math.min(1.0f, 0.80f + 0.18f * bv[i + 6] + 0.08f * bv[i + 5]));
            buf.vertex(mat, bv[i], bv[i + 1], bv[i + 2])
               .color(shade, shade, shade, 1.0f)
               .texture(bv[i + 3], bv[i + 4])
               .next();
        }
        tess.draw();

        // 2. Foliage canopy (cutout leaves)
        RenderSystem.disableCull();
        RenderSystem.enableAlphaTest();
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.30f);
        client.getTextureManager().bindTexture(getLeafTex(kind));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        float leafR = "birch".equals(kind) ? 0.68f : ("pine".equals(kind) ? 0.45f : 0.54f);
        float leafG = "birch".equals(kind) ? 0.86f : ("pine".equals(kind) ? 0.68f : 0.78f);
        float leafB = "birch".equals(kind) ? 0.48f : ("pine".equals(kind) ? 0.42f : 0.40f);

        buf.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE);
        float[] lv = mesh.leafVerts;
        for (int i = 0; i < lv.length; i += 8) {
            float shade = Math.max(0.64f, Math.min(1.0f, 0.82f + 0.18f * lv[i + 6]));
            buf.vertex(mat, lv[i], lv[i + 1], lv[i + 2])
               .color(leafR * shade, leafG * shade, leafB * shade, 1.0f)
               .texture(lv[i + 3], lv[i + 4])
               .next();
        }
        tess.draw();
    }

    private static Identifier getLogTex(String kind) {
        if ("birch".equals(kind)) return TEX_LOG_BIRCH;
        if ("oak".equals(kind)) return TEX_LOG_OAK;
        return TEX_LOG_PINE;
    }

    private static Identifier getLeafTex(String kind) {
        if ("birch".equals(kind)) return TEX_LEAF_BIRCH;
        if ("oak".equals(kind)) return TEX_LEAF_OAK;
        return TEX_LEAF_PINE;
    }

    private static TreeMesh buildTreeMesh(String kind) {
        List<Float> bark = new ArrayList<>();
        List<Float> leaves = new ArrayList<>();

        if ("pine".equals(kind)) {
            // Tall tapered Rust conifer trunk + 5 tiered wide needle skirts
            addTaperedCylinder(bark, 0, 0, 0, 0, 7.2f, 0, 0.50f, 0.12f, 10, 4.0f);
            addTaperedCylinder(bark, 0, 0, 0, 0, 0.55f, 0, 0.64f, 0.48f, 10, 0.8f); // flared root base
            float[][] tiers = {
                    {2.3f, 3.8f, 2.55f},
                    {3.4f, 4.9f, 2.20f},
                    {4.5f, 6.0f, 1.75f},
                    {5.6f, 7.0f, 1.25f},
                    {6.5f, 7.8f, 0.70f}
            };
            for (float[] t : tiers) {
                addConeCanopy(leaves, 0, t[0], 0, t[1] - t[0], t[2], 10);
            }
        } else if ("birch".equals(kind)) {
            // Slender pale birch trunk + upper forks + layered foliage crowns
            addTaperedCylinder(bark, 0, 0, 0, 0.12f, 6.1f, -0.06f, 0.43f, 0.14f, 10, 3.8f);
            addTaperedCylinder(bark, 0, 0, 0, 0, 0.45f, 0, 0.56f, 0.42f, 10, 0.6f);
            addTaperedCylinder(bark, 0.05f, 3.7f, 0, 1.15f, 5.2f, 0.55f, 0.16f, 0.06f, 6, 1.5f);
            addTaperedCylinder(bark, 0.06f, 4.0f, -0.02f, -1.05f, 5.5f, -0.45f, 0.15f, 0.06f, 6, 1.5f);
            addCanopyCluster(leaves, 0.12f, 6.0f, -0.06f, 1.85f, 1.45f);
            addCanopyCluster(leaves, 1.15f, 5.15f, 0.55f, 1.45f, 1.15f);
            addCanopyCluster(leaves, -1.05f, 5.45f, -0.45f, 1.45f, 1.15f);
            addCanopyCluster(leaves, -0.25f, 5.05f, 1.05f, 1.35f, 1.05f);
        } else {
            // Broad gnarled oak trunk + 4 heavy limbs + lush full crown
            addTaperedCylinder(bark, 0, 0, 0, 0, 5.2f, 0, 0.56f, 0.24f, 10, 3.2f);
            addTaperedCylinder(bark, 0, 0, 0, 0, 0.55f, 0, 0.72f, 0.54f, 10, 0.7f);
            addTaperedCylinder(bark, 0, 3.2f, 0, 1.45f, 4.9f, 0.65f, 0.22f, 0.09f, 6, 1.6f);
            addTaperedCylinder(bark, 0, 3.4f, 0, -1.35f, 5.0f, 0.75f, 0.20f, 0.09f, 6, 1.6f);
            addTaperedCylinder(bark, 0, 3.5f, 0, 0.25f, 5.1f, -1.40f, 0.20f, 0.09f, 6, 1.6f);
            addCanopyCluster(leaves, 0.0f, 5.6f, 0.0f, 2.35f, 1.65f);
            addCanopyCluster(leaves, 1.45f, 4.9f, 0.65f, 1.70f, 1.30f);
            addCanopyCluster(leaves, -1.35f, 5.0f, 0.75f, 1.65f, 1.25f);
            addCanopyCluster(leaves, 0.25f, 5.1f, -1.40f, 1.65f, 1.25f);
        }

        return new TreeMesh(toArray(bark), toArray(leaves));
    }

    private static void addTaperedCylinder(List<Float> out,
                                           float x0, float y0, float z0,
                                           float x1, float y1, float z1,
                                           float r0, float r1, int sides, float vTile) {
        for (int i = 0; i < sides; i++) {
            double a0 = (2.0 * Math.PI * i) / sides;
            double a1 = (2.0 * Math.PI * (i + 1)) / sides;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float u0 = (float) i / sides;
            float u1 = (float) (i + 1) / sides;

            float ax = x0 + c0 * r0, ay = y0, az = z0 + s0 * r0;
            float bx = x0 + c1 * r0, by = y0, bz = z0 + s1 * r0;
            float cx = x1 + c1 * r1, cy = y1, cz = z1 + s1 * r1;
            float dx = x1 + c0 * r1, dy = y1, dz = z1 + s0 * r1;

            addTri(out, ax, ay, az, u0, vTile, c0, 0.1f, s0,
                        dx, dy, dz, u0, 0f,    c0, 0.1f, s0,
                        cx, cy, cz, u1, 0f,    c1, 0.1f, s1);
            addTri(out, ax, ay, az, u0, vTile, c0, 0.1f, s0,
                        cx, cy, cz, u1, 0f,    c1, 0.1f, s1,
                        bx, by, bz, u1, vTile, c1, 0.1f, s1);
        }
    }

    private static void addConeCanopy(List<Float> out, float cx, float baseY, float cz,
                                      float height, float radius, int sides) {
        float topY = baseY + height;
        for (int i = 0; i < sides; i++) {
            double a0 = (2.0 * Math.PI * i) / sides;
            double a1 = (2.0 * Math.PI * (i + 1)) / sides;
            float x0 = cx + (float) Math.cos(a0) * radius;
            float z0 = cz + (float) Math.sin(a0) * radius;
            float x1 = cx + (float) Math.cos(a1) * radius;
            float z1 = cz + (float) Math.sin(a1) * radius;
            float nx = (float) Math.cos((a0 + a1) * 0.5);
            float nz = (float) Math.sin((a0 + a1) * 0.5);

            addTri(out, x0, baseY, z0, 0f, 1f, nx, 0.45f, nz,
                        cx, topY,  cz, 0.5f, 0f, 0f, 1.0f, 0f,
                        x1, baseY, z1, 1f, 1f, nx, 0.45f, nz);
            // Underside dark skirt closing cone
            addTri(out, cx, baseY + 0.18f, cz, 0.5f, 0.5f, 0f, -1f, 0f,
                        x1, baseY, z1, 1f, 1f, 0f, -0.8f, 0f,
                        x0, baseY, z0, 0f, 1f, 0f, -0.8f, 0f);
        }
    }

    private static void addCanopyCluster(List<Float> out, float cx, float cy, float cz, float rx, float ry) {
        // Double-crossed volumetric octa-dome layers for dense lush Rust-style canopies
        int sides = 8;
        float[] ringY = {-ry * 0.55f, 0f, ry * 0.65f};
        float[] ringR = {rx * 0.72f, rx, rx * 0.56f};
        for (int s = 0; s < sides; s++) {
            double a0 = (2.0 * Math.PI * s) / sides;
            double a1 = (2.0 * Math.PI * (s + 1)) / sides;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            for (int l = 0; l < 2; l++) {
                float yA = cy + ringY[l], yB = cy + ringY[l + 1];
                float rA = ringR[l], rB = ringR[l + 1];
                addTri(out, cx + c0 * rA, yA, cz + s0 * rA, 0f, 1f, c0, 0.2f, s0,
                            cx + c0 * rB, yB, cz + s0 * rB, 0f, 0f, c0, 0.4f, s0,
                            cx + c1 * rB, yB, cz + s1 * rB, 1f, 0f, c1, 0.4f, s1);
                addTri(out, cx + c0 * rA, yA, cz + s0 * rA, 0f, 1f, c0, 0.2f, s0,
                            cx + c1 * rB, yB, cz + s1 * rB, 1f, 0f, c1, 0.4f, s1,
                            cx + c1 * rA, yA, cz + s1 * rA, 1f, 1f, c1, 0.2f, s1);
            }
            // Top cap
            addTri(out, cx + c0 * ringR[2], cy + ringY[2], cz + s0 * ringR[2], 0f, 1f, c0, 0.7f, s0,
                        cx, cy + ry, cz, 0.5f, 0f, 0f, 1f, 0f,
                        cx + c1 * ringR[2], cy + ringY[2], cz + s1 * ringR[2], 1f, 1f, c1, 0.7f, s1);
            // Bottom cap
            addTri(out, cx, cy - ry * 0.75f, cz, 0.5f, 0.5f, 0f, -1f, 0f,
                        cx + c1 * ringR[0], cy + ringY[0], cz + s1 * ringR[0], 1f, 1f, c1, -0.7f, s1,
                        cx + c0 * ringR[0], cy + ringY[0], cz + s0 * ringR[0], 0f, 1f, c0, -0.7f, s0);
        }
    }

    private static void addTri(List<Float> out,
                               float x0, float y0, float z0, float u0, float v0, float nx0, float ny0, float nz0,
                               float x1, float y1, float z1, float u1, float v1, float nx1, float ny1, float nz1,
                               float x2, float y2, float z2, float u2, float v2, float nx2, float ny2, float nz2) {
        out.add(x0); out.add(y0); out.add(z0); out.add(u0); out.add(v0); out.add(nx0); out.add(ny0); out.add(nz0);
        out.add(x1); out.add(y1); out.add(z1); out.add(u1); out.add(v1); out.add(nx1); out.add(ny1); out.add(nz1);
        out.add(x2); out.add(y2); out.add(z2); out.add(u2); out.add(v2); out.add(nx2); out.add(ny2); out.add(nz2);
    }

    private static float[] toArray(List<Float> list) {
        float[] a = new float[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        return a;
    }
}
