package net.spacerust.client.guns;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.spacerust.client.SpaceRustItems;
import net.spacerust.client.world.RustObjModel;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Renders animated 3D CoilRust weapons, tools, and medical items in first-person
 * driven by RustViewModelAnimator (deploy, ADS, recoil, muzzle flash, reload, melee chop, sprint, medical use).
 */
public final class RustViewModelRenderer {

    private static final Identifier TEX_MUZZLE_FLASH = new Identifier("spacerust", "textures/effects/muzzle_flash.png");
    private static final Identifier TEX_PLAYER_HANDS = new Identifier("spacerust", "textures/viewmodels/player_hands.png");
    private static final Map<String, RustObjModel.MultiMesh> MESH_CACHE = new HashMap<>();
    private static final Map<String, RustObjModel.MultiMesh> HANDS_CACHE = new HashMap<>();

    private RustViewModelRenderer() {
    }

    public static String resolveViewModelKey(ItemStack stack) {
        SpaceRustItems.ItemDef def = SpaceRustItems.getDef(stack);
        return def != null ? def.viewModelKey : null;
    }

    public static boolean supports(ItemStack stack) {
        return resolveViewModelKey(stack) != null;
    }

    public static boolean render(AbstractClientPlayerEntity player, float tickDelta,
                                 MatrixStack matrices, ItemStack stack, int light) {
        String key = resolveViewModelKey(stack);
        if (key == null) return false;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;

        RustObjModel.MultiMesh mesh = MESH_CACHE.computeIfAbsent(key, k ->
                RustObjModel.loadGzMulti(new Identifier("spacerust", "models/viewmodels/" + k + ".obj.gz")));
        if (mesh == null || mesh.parts.isEmpty()) {
            return false;
        }

        RustViewModelAnimator.updateFrame(client);
        boolean isGun = isGunKey(key);

        float ads = isGun ? smoothStep(RustViewModelAnimator.adsProgress) : 0.0f;
        if (("l96".equals(key) || "bolt".equals(key)) && ads >= 0.72f) {
            return true; // Scoped-in 8x optic overlay covers view cleanly
        }
        float deploy = RustViewModelAnimator.deployProgress;
        float deployInv = 1.0f - smoothStep(deploy);
        float sprint = smoothStep(RustViewModelAnimator.sprintProgress) * (1.0f - ads);
        float kick = RustViewModelAnimator.recoilKick;
        float kickYaw = RustViewModelAnimator.recoilYawJitter;
        float rel = RustViewModelAnimator.reloadProgress;
        float swing = RustViewModelAnimator.swingProgress;
        float med = RustViewModelAnimator.medUseProgress;

        // Idle figure-8 sway + walk bob (minimized during ADS for steady iron-sight alignment)
        float bobScale = (1.0f - 0.94f * ads);
        float swayX = (float) Math.sin(RustViewModelAnimator.idleTime * 1.6) * 0.003f * bobScale
                + (float) Math.cos(RustViewModelAnimator.walkPhase) * 0.014f * RustViewModelAnimator.walkAmount * bobScale;
        float swayY = (float) Math.cos(RustViewModelAnimator.idleTime * 3.2) * 0.002f * bobScale
                + (float) Math.abs(Math.sin(RustViewModelAnimator.walkPhase)) * -0.016f * RustViewModelAnimator.walkAmount * bobScale;

        // Base hip-fire placement vs ADS centered placement
        float hipX = getHipX(key);
        float hipY = getHipY(key);
        float hipZ = getHipZ(key);

        float adsX = getAdsX(key);
        float adsY = getAdsY(key);
        float adsZ = getAdsZ(key);

        float tx = lerp(hipX, adsX, ads) + swayX;
        float ty = lerp(hipY, adsY, ads) + swayY;
        float tz = lerp(hipZ, adsZ, ads);

        float rx = lerp(isGun ? 1.5f : 0.0f, getAdsPitch(key), ads);
        float ry = lerp(getHipYaw(key), 0.0f, ads);
        float rz = lerp(getHipRoll(key), 0.0f, ads);

        // 1. Deploy / Pull-out drop + wrist roll
        if (deployInv > 0.001f) {
            ty -= 0.42f * deployInv * deployInv;
            tz += 0.14f * deployInv;
            rx -= 36.0f * deployInv;
            ry += 22.0f * deployInv;
            rz += 28.0f * deployInv;
        }

        // 2. Tactical Sprint diagonal cant inward toward the player's chest/side
        if (sprint > 0.001f) {
            tx -= 0.14f * sprint;
            ty -= 0.09f * sprint;
            tz += 0.05f * sprint;
            rx -= 12.0f * sprint;
            ry += 48.0f * sprint;
            rz += 20.0f * sprint;
        }

        // 3. Shooting Recoil Kickback
        if (kick > 0.001f) {
            float k = Math.min(1.3f, kick);
            tz += (isGun ? 0.075f : 0.02f) * k * (1.0f - 0.45f * ads);
            ty += 0.012f * k * (1.0f - 0.35f * ads);
            rx += (ads > 0.5f ? 3.8f : 10.5f) * k;
            ry += kickYaw * (ads > 0.5f ? 1.2f : 2.5f) * k;
            rz += kickYaw * (ads > 0.5f ? 2.2f : 5.0f) * k;
        }

        // 4. Reload Tilt & Mag Insert Dip
        if (rel > 0.001f) {
            float dip = (float) Math.sin(rel * Math.PI);
            float bump = (rel > 0.42f && rel < 0.72f)
                    ? (float) Math.sin(((rel - 0.42f) / 0.30f) * Math.PI) * 0.055f : 0.0f;
            tx -= 0.05f * dip;
            ty -= (0.14f * dip - bump);
            rx -= 18.0f * dip;
            ry += 24.0f * dip;
            rz -= 36.0f * dip;
        }

        // 5. Tool / Rock / Axe / Pickaxe Swing Arc
        if (swing > 0.001f) {
            // 0..0.25 windup, 0.25..0.60 strike down-left, 0.60..1.0 recover
            if (swing < 0.25f) {
                float p = swing / 0.25f;
                tx += 0.06f * p;
                ty += 0.09f * p;
                rx += 24.0f * p;
                rz -= 18.0f * p;
            } else if (swing < 0.58f) {
                float p = smoothStep((swing - 0.25f) / 0.33f);
                tx += lerp(0.06f, -0.18f, p);
                ty += lerp(0.09f, -0.11f, p);
                tz -= 0.14f * (float) Math.sin(p * Math.PI);
                rx += lerp(24.0f, -54.0f, p);
                ry += lerp(0.0f, 32.0f, p);
                rz += lerp(-18.0f, 44.0f, p);
            } else {
                float p = 1.0f - smoothStep((swing - 0.58f) / 0.42f);
                tx -= 0.18f * p;
                ty -= 0.11f * p;
                rx -= 54.0f * p;
                ry += 32.0f * p;
                rz += 44.0f * p;
            }
        }

        // 6. Medical Syringe / Bandage Self-Use Animation
        if (med > 0.001f) {
            float raise = (float) Math.sin(Math.min(1.0f, med * 1.15f) * Math.PI);
            float plunge = (med > 0.45f && med < 0.78f)
                    ? (float) Math.sin(((med - 0.45f) / 0.33f) * Math.PI) : 0.0f;
            tx -= 0.18f * raise;
            ty -= (0.02f * raise + 0.05f * plunge);
            tz -= (0.04f * raise + 0.05f * plunge);
            rx -= (22.0f * raise + 10.0f * plunge);
            ry += 32.0f * raise;
            rz += 18.0f * raise;
        }

        // 7. Hunting Bow Draw Pullback, Rust Aim Cant & Full-Draw Micro-Tremor
        if ("bow".equals(key)) {
            float draw = smoothStep(RustViewModelAnimator.bowDrawProgress);
            if (draw > 0.001f) {
                tx -= 0.055f * draw;
                ty += 0.025f * draw;
                tz += 0.085f * draw;
                rx -= 5.0f * draw;
                ry += 8.0f * draw;
                rz += 26.0f * draw;
                if (RustViewModelAnimator.bowDrawProgress > 0.90f) {
                    tx += (float) Math.sin(RustViewModelAnimator.idleTime * 28.0) * 0.0018f;
                    ty += (float) Math.cos(RustViewModelAnimator.idleTime * 34.0) * 0.0014f;
                }
            }
        }

        float scale = getScale(key);

        matrices.push();
        matrices.translate(tx, ty, tz);
        matrices.multiply(new Quaternion(0f, ry, 0f, true));
        matrices.multiply(new Quaternion(rx, 0f, 0f, true));
        matrices.multiply(new Quaternion(0f, 0f, rz, true));

        // Re-orient CoilRust model so +Z barrel faces -Z into screen
        matrices.multiply(new Quaternion(0f, 180f, 0f, true));
        if ("torch".equals(key)) {
            matrices.multiply(new Quaternion(-16f, 0f, 0f, true));
            matrices.multiply(new Quaternion(0f, 0f, 76f, true));
        } else if ("rock".equals(key)) {
            matrices.multiply(new Quaternion(-18f, 24f, -14f, true));
        } else if ("hammer".equals(key) || key.contains("axe") || key.contains("pick")) {
            matrices.multiply(new Quaternion(-18f, -12f, 8f, true));
        } else if ("syringe".equals(key)) {
            // Standalone syringe lies along X; point needle diagonally forward-left & upright in right hand
            matrices.multiply(new Quaternion(0f, 115f, 0f, true));
            matrices.multiply(new Quaternion(18f, 0f, -14f, true));
        } else if ("bandage".equals(key)) {
            matrices.multiply(new Quaternion(-22f, 28f, -12f, true));
        } else if ("plan".equals(key)) {
            matrices.multiply(new Quaternion(-28f, -18f, 10f, true));
        } else if ("m39".equals(key)) {
            matrices.multiply(new Quaternion(0f, 90f, 0f, true));
        } else if ("woodenspear".equals(key) || "stonespear".equals(key)) {
            matrices.multiply(new Quaternion(0f, 82f, -16f, true));
        } else if ("salvagedicepick".equals(key)) {
            matrices.multiply(new Quaternion(-16f, 75f, 6f, true));
        } else if ("satchel".equals(key) || "f1grenade".equals(key)) {
            matrices.multiply(new Quaternion(-15f, 15f, -10f, true));
        }

        matrices.scale(scale, scale, scale);
        matrices.translate(-mesh.cx, -mesh.cy, -mesh.cz);

        RenderSystem.pushMatrix();
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableLighting();
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        client.gameRenderer.getLightmapTextureManager().disable();

        Matrix4f mat = matrices.peek().getModel();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        int blockL = (light >> 4) & 0xF;
        int skyL = (light >> 20) & 0xF;
        float env = Math.max(blockL, skyL) / 15.0f;
        float envScale = 0.74f + 0.26f * env;
        if ("torch".equals(key) && RustViewModelAnimator.isTorchLitForRender()) {
            envScale = Math.max(envScale, 0.98f);
        }
        for (RustObjModel.Part part : mesh.parts) {
            if ("ak47".equals(key) && ads >= 0.45f
                    && "m_2a93466b-328b-885e-29e3-cdf224895911".equals(part.material)) {
                // Skip rear shovel stock when shouldered in ADS so only receiver & iron sights fill the sight picture
                continue;
            }
            if ("l96".equals(key) && "m_f9901731-a5d1-3f3a-c00f-ee29fcfa6bce".equals(part.material)) {
                continue;
            }
            drawPart(client, resolveTexture(key, part.material), mat, tess, buf, part.vertices, envScale);
        }

        // Render CoilRust 3D player arms/hands holding the weapon/tool in identical viewmodel origin
        RustObjModel.MultiMesh hands = HANDS_CACHE.computeIfAbsent(key, k ->
                RustObjModel.loadGzMulti(new Identifier("spacerust", "models/viewmodels/" + k + "_hands.obj.gz")));
        if (hands != null && !hands.parts.isEmpty()) {
            for (RustObjModel.Part part : hands.parts) {
                drawPart(client, TEX_PLAYER_HANDS, mat, tess, buf, part.vertices, envScale);
            }
        }

        // Draw subtle flickering flame glow at the top cloth tip of CoilRust torch only when lit (RMB)
        if ("torch".equals(key) && RustViewModelAnimator.isTorchLitForRender()) {
            long tNow = System.currentTimeMillis();
            float flicker = 0.068f + (float) Math.sin(tNow * 0.017) * 0.008f + (float) Math.cos(tNow * 0.029) * 0.005f;
            matrices.push();
            matrices.translate(mesh.cx + mesh.spanX * 0.44f, mesh.cy + 0.012f, mesh.cz);
            matrices.multiply(new Quaternion(0f, 0f, -76f, true));
            matrices.multiply(new Quaternion(0f, 180f, 0f, true));
            Matrix4f tmat = matrices.peek().getModel();
            client.getTextureManager().bindTexture(TEX_MUZZLE_FLASH);
            RenderSystem.enableBlend();
            RenderSystem.disableAlphaTest();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            RenderSystem.depthMask(false);
            buf.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
            buf.vertex(tmat, -flicker, -flicker, 0).color(1f, 0.58f, 0.16f, 0.72f).texture(0f, 1f).next();
            buf.vertex(tmat,  flicker, -flicker, 0).color(1f, 0.58f, 0.16f, 0.72f).texture(1f, 1f).next();
            buf.vertex(tmat,  flicker,  flicker, 0).color(1f, 0.78f, 0.28f, 0.72f).texture(1f, 0f).next();
            buf.vertex(tmat, -flicker,  flicker, 0).color(1f, 0.78f, 0.28f, 0.72f).texture(0f, 0f).next();
            tess.draw();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableAlphaTest();
            RenderSystem.disableBlend();
            matrices.pop();
        }

        // Draw Muzzle Flash at barrel tip for ~50ms after shooting
        if (isGun && System.currentTimeMillis() < RustViewModelAnimator.muzzleFlashUntilMs) {
            matrices.push();
            matrices.translate(mesh.cx, mesh.cy + mesh.spanY * 0.12f, mesh.cz + mesh.spanZ * 0.52f);
            float flashRoll = (System.currentTimeMillis() * 7) % 360;
            matrices.multiply(new Quaternion(0f, 0f, flashRoll, true));
            float fs = "shotgun".equals(key) ? 0.24f : 0.17f;
            Matrix4f fmat = matrices.peek().getModel();
            client.getTextureManager().bindTexture(TEX_MUZZLE_FLASH);
            RenderSystem.enableBlend();
            RenderSystem.disableAlphaTest();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            RenderSystem.depthMask(false);
            buf.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
            buf.vertex(fmat, -fs, -fs, 0).color(1f, 0.82f, 0.42f, 0.94f).texture(0f, 1f).next();
            buf.vertex(fmat,  fs, -fs, 0).color(1f, 0.82f, 0.42f, 0.94f).texture(1f, 1f).next();
            buf.vertex(fmat,  fs,  fs, 0).color(1f, 0.82f, 0.42f, 0.94f).texture(1f, 0f).next();
            buf.vertex(fmat, -fs,  fs, 0).color(1f, 0.82f, 0.42f, 0.94f).texture(0f, 0f).next();
            tess.draw();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableAlphaTest();
            RenderSystem.disableBlend();
            matrices.pop();
        }

        client.gameRenderer.getLightmapTextureManager().enable();
        RenderSystem.enableCull();
        RenderSystem.popMatrix();
        matrices.pop();
        return true;
    }

    private static Identifier resolveTexture(String key, String matName) {
        String texFile = "part0_0.png";
        if ("ak47".equals(key)) {
            if ("m_76370e4f-0214-f397-43c9-808cacaec63c".equals(matName)) texFile = "tex1.png";
            else if ("m_d0658c38-d022-bb28-1c53-8b9c43937af4".equals(matName)) texFile = "tex2.png";
            else if ("m_4d0c92a0-3204-80ce-c88f-df0b7b5f18be".equals(matName)) texFile = "tex3.png";
            else texFile = "tex0.png";
        } else if ("customsmg".equals(key)) {
            if ("m_1280d117-2a1a-e603-6e60-d07762007c43".equals(matName)) texFile = "tex1.png";
            else if ("m_7e1b8d70-eb5c-eedc-b3b2-2d24d83bca94".equals(matName)) texFile = "tex2.png";
            else texFile = "tex0.png";
        } else if ("sar".equals(key)) {
            if ("m_843bf5c9-17b4-2162-80a8-73b8d5d3ff4a".equals(matName)) texFile = "tex1.png";
            else if ("m_cbd75397-7d3e-7e51-50e5-aad3e4e045c9".equals(matName)) texFile = "tex2.png";
            else if ("m_e840f306-e14a-fafa-504e-88f15247203a".equals(matName)) texFile = "tex3.png";
            else texFile = "tex0.png";
        } else if ("lr300".equals(key)) {
            texFile = "m_09b490e5-dedb-029e-d5f0-c465b31c6300".equals(matName) ? "tex1.png" : "tex0.png";
        } else if ("hmlmg".equals(key)) {
            if ("m_46d95dbf-c678-c045-d44d-3de8a0b76ccb".equals(matName)) texFile = "tex1.png";
            else if ("m_f27a829f-6cd1-e01f-1707-dd308496f5f0".equals(matName)) texFile = "skin.png";
            else texFile = "tex0.png";
        } else if ("l96".equals(key)) {
            texFile = "l96_albedo.png";
        } else if ("eoka".equals(key) || "crossbow".equals(key) || "bow".equals(key) || "f1grenade".equals(key)) {
            texFile = "part1_0".equals(matName) ? "part1_0.png" : "part0_0.png";
        } else if ("satchel".equals(key)) {
            texFile = "part0_1".equals(matName) ? "part0_1.png" : "part0_0.png";
        } else if ("jackhammer".equals(key) || "boneknife".equals(key)) {
            texFile = "part0.png";
        } else if ("revolver".equals(key) || "thompson".equals(key) || "bolt".equals(key)
                || "mp5".equals(key) || "p250".equals(key) || "m92".equals(key)
                || "python".equals(key) || "pump".equals(key) || "m39".equals(key)
                || "rock".equals(key) || "salvagedicepick".equals(key) || "salvagedaxe".equals(key)
                || "woodenspear".equals(key) || "stonespear".equals(key) || "machete".equals(key)) {
            texFile = "tex0.png";
        } else if ("shotgun".equals(key)) {
            if ("part1_0".equals(matName)) texFile = "part1_0.png";
            else if ("part2_0".equals(matName)) texFile = "part2_0.png";
            else if ("part3_0".equals(matName)) texFile = "part3_0.png";
            else if ("part3_1".equals(matName)) texFile = "part3_1.png";
            else if ("part3_2".equals(matName)) texFile = "main.png";
            else if ("part3_3".equals(matName)) texFile = "part3_3.png";
            else texFile = "part0_0.png";
        }
        return new Identifier("spacerust", "textures/viewmodels/" + key + "/" + texFile);
    }

    private static boolean isGunKey(String key) {
        return "ak47".equals(key) || "hmlmg".equals(key) || "thompson".equals(key) || "revolver".equals(key)
                || "shotgun".equals(key) || "bolt".equals(key) || "lr300".equals(key)
                || "mp5".equals(key) || "sar".equals(key) || "customsmg".equals(key)
                || "p250".equals(key) || "m92".equals(key) || "python".equals(key)
                || "pump".equals(key) || "eoka".equals(key) || "l96".equals(key)
                || "m39".equals(key) || "crossbow".equals(key) || "bow".equals(key);
    }

    private static float getScale(String key) {
        if ("syringe".equals(key)) return 1.25f;
        if ("bandage".equals(key)) return 1.15f;
        if ("rock".equals(key)) return 1.08f;
        if ("shotgun".equals(key) || "pump".equals(key)) return 0.85f;
        if ("bolt".equals(key) || "l96".equals(key) || "m39".equals(key)) return 0.78f;
        if ("ak47".equals(key) || "hmlmg".equals(key) || "thompson".equals(key) || "lr300".equals(key)
                || "mp5".equals(key) || "sar".equals(key) || "customsmg".equals(key)) return 0.88f;
        if ("revolver".equals(key) || "python".equals(key) || "p250".equals(key) || "m92".equals(key)) return 1.02f;
        if ("eoka".equals(key)) return 0.65f;
        if ("crossbow".equals(key) || "bow".equals(key)) return 0.68f;
        if ("woodenspear".equals(key) || "stonespear".equals(key)) return 0.55f;
        if ("jackhammer".equals(key)) return 0.85f;
        if ("torch".equals(key)) return 1.25f;
        if ("plan".equals(key)) return 0.72f;
        return 1.05f;
    }

    private static float getHipX(String key) {
        if ("syringe".equals(key) || "bandage".equals(key)) return 0.24f;
        if ("rock".equals(key) || "torch".equals(key)) return 0.25f;
        if ("plan".equals(key)) return 0.26f;
        if (isGunKey(key) && !"bow".equals(key)) return 0.17f;
        if ("bow".equals(key)) return 0.24f;
        return 0.31f;
    }

    private static float getHipY(String key) {
        if ("syringe".equals(key) || "bandage".equals(key)) return -0.15f;
        if ("rock".equals(key)) return -0.18f;
        if ("plan".equals(key)) return -0.18f;
        if ("bow".equals(key)) return -0.12f;
        if ("bolt".equals(key)) return -0.180f;
        if ("revolver".equals(key)) return -0.165f;
        if ("thompson".equals(key)) return -0.165f;
        if (isGunKey(key)) return -0.175f;
        if ("torch".equals(key)) return -0.20f;
        return -0.22f;
    }

    private static float getHipZ(String key) {
        if ("syringe".equals(key) || "bandage".equals(key)) return -0.34f;
        if ("rock".equals(key)) return -0.35f;
        if ("torch".equals(key)) return -0.36f;
        if ("plan".equals(key)) return -0.38f;
        if ("bow".equals(key)) return -0.44f;
        if ("bolt".equals(key)) return -0.38f;
        if ("ak47".equals(key)) return -0.35f;
        if ("thompson".equals(key)) return -0.33f;
        if ("shotgun".equals(key) || "revolver".equals(key)) return -0.36f;
        return -0.52f;
    }

    private static float getHipYaw(String key) {
        if ("revolver".equals(key)) return 6.5f;
        if (isGunKey(key)) return 7.5f;
        return 12.0f;
    }

    private static float getHipRoll(String key) {
        if ("revolver".equals(key)) return -3.0f;
        if (isGunKey(key)) return -3.5f;
        return 0.0f;
    }

    private static float getAdsX(String key) {
        if ("ak47".equals(key)) return -0.0004f;
        if ("thompson".equals(key)) return 0.0091f;
        if ("shotgun".equals(key)) return -0.0291f;
        if ("bolt".equals(key)) return 0.0038f;
        if ("bow".equals(key)) return 0.14f;
        return 0.0f; // revolver
    }

    private static float getAdsY(String key) {
        if ("ak47".equals(key)) return -0.1025f;
        if ("thompson".equals(key)) return -0.0725f;
        if ("bolt".equals(key)) return -0.1220f;
        if ("shotgun".equals(key)) return -0.0885f;
        if ("bow".equals(key)) return -0.08f;
        return -0.0890f; // revolver
    }

    private static float getAdsZ(String key) {
        if ("ak47".equals(key) || "thompson".equals(key)) return -0.32f;
        if ("revolver".equals(key)) return -0.38f;
        if ("shotgun".equals(key)) return -0.42f;
        if ("bolt".equals(key)) return -0.44f;
        if ("bow".equals(key)) return -0.42f;
        return -0.46f;
    }

    private static float getAdsPitch(String key) {
        return 0.0f;
    }

    private static void drawPart(MinecraftClient client, Identifier tex, Matrix4f mat,
                                 Tessellator tess, BufferBuilder buf, float[] v, float envScale) {
        client.getTextureManager().bindTexture(tex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        buf.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE);
        for (int i = 0; i < v.length; i += 8) {
            float nx = v[i + 5], ny = v[i + 6];
            float dir = 0.88f + 0.12f * Math.max(-0.4f, Math.min(1.0f, 0.72f * ny + 0.28f * nx));
            float shade = Math.min(1.0f, envScale * dir);
            buf.vertex(mat, v[i], v[i + 1], v[i + 2])
               .color(shade, shade, shade, 1.0f)
               .texture(v[i + 3], v[i + 4])
               .next();
        }
        tess.draw();
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float smoothStep(float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        return t * t * (3.0f - 2.0f * t);
    }
}
