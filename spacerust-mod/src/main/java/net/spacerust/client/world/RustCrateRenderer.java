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
 * Renders 3D Rust Loot Crates of different rarities (Common, Tools, Food, Military, Elite)
 * using authentic CoilRust .obj.gz models and textures.
 */
public final class RustCrateRenderer {

    private static final Identifier TEX_NORMAL   = new Identifier("spacerust", "textures/crates/crate_normal.png");
    private static final Identifier TEX_TOOLS    = new Identifier("spacerust", "textures/crates/crate_tools.png");
    private static final Identifier TEX_FOOD     = new Identifier("spacerust", "textures/crates/crate_food.png");
    private static final Identifier TEX_MILITARY = new Identifier("spacerust", "textures/crates/crate_military.png");

    private static final Map<String, RustObjModel> MODEL_CACHE = new HashMap<>();

    private RustCrateRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            List<RustWorldSync.RustCrate> crates = RustWorldSync.crates;
            if (crates == null || crates.isEmpty()) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.world == null || client.player == null) return;

            Vec3d cam = ctx.camera().getPos();
            MatrixStack matrices = ctx.matrixStack();

            RenderSystem.pushMatrix();
            RenderSystem.enableTexture();
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();

            for (RustWorldSync.RustCrate c : crates) {
                double dx = (c.x + 0.5) - cam.x;
                double dy = c.y - cam.y;
                double dz = (c.z + 0.5) - cam.z;
                if (dx * dx + dy * dy + dz * dz > 96.0 * 96.0) continue;

                RustObjModel model = getModel(c.tier);
                if (model == null || model.vertexCount == 0) continue;

                client.getTextureManager().bindTexture(getTexture(c.tier));
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

                matrices.push();
                matrices.translate(dx, dy, dz);
                float yaw = Math.abs((c.x * 31 + c.z * 17) % 4) * 90.0f;
                matrices.multiply(new Quaternion(0f, yaw, 0f, true));
                float scale = getScale(c.tier);
                matrices.scale(scale, scale, scale);

                boolean isElite = "elite".equals(c.tier);
                float rTint = isElite ? 0.85f : 1.0f;
                float gTint = isElite ? 0.96f : 1.0f;
                float bTint = isElite ? 1.0f : 1.0f;

                Matrix4f mat = matrices.peek().getModel();
                Tessellator tess = Tessellator.getInstance();
                BufferBuilder buf = tess.getBuffer();
                buf.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
                float[] v = model.vertices;
                for (int i = 0; i < v.length; i += 8) {
                    float nx = v[i + 5], ny = v[i + 6], nz = v[i + 7];
                    float sun = Math.max(0.48f, Math.min(1.0f, 0.65f + 0.30f * ny + 0.10f * nx));
                    buf.vertex(mat, v[i], v[i + 1], v[i + 2])
                       .color(sun * rTint, sun * gTint, sun * bTint, 1.0f)
                       .texture(v[i + 3], v[i + 4])
                       .light(0x00F000F0)
                       .normal(nx, ny, nz)
                       .next();
                }
                tess.draw();
                matrices.pop();
            }

            RenderSystem.popMatrix();
        });
    }

    private static RustObjModel getModel(String tier) {
        String key;
        if ("tools".equals(tier)) key = "crate_tools";
        else if ("food".equals(tier)) key = "crate_food";
        else if ("military".equals(tier) || "elite".equals(tier)) key = "crate_military";
        else key = "crate_normal";
        return MODEL_CACHE.computeIfAbsent(key, k ->
                RustObjModel.loadGz(new Identifier("spacerust", "models/crates/" + key + ".obj.gz")));
    }

    private static Identifier getTexture(String tier) {
        if ("tools".equals(tier)) return TEX_TOOLS;
        if ("food".equals(tier)) return TEX_FOOD;
        if ("military".equals(tier) || "elite".equals(tier)) return TEX_MILITARY;
        return TEX_NORMAL;
    }

    private static float getScale(String tier) {
        if ("tools".equals(tier)) return 1.04f;
        if ("elite".equals(tier)) return 0.96f;
        if ("military".equals(tier)) return 0.90f;
        return 0.85f;
    }
}
