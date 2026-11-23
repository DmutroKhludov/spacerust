package net.spacerust.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.spacerust.client.guns.GunManager;
import net.spacerust.client.guns.RustViewModelAnimator;

public class RustCrosshairOverlay implements HudRenderCallback {
    private static final Identifier TEX_SCOPE_8X = new Identifier("spacerust", "textures/gui/rust_ui/scope_8x.png");
    public static long hitmarkerTime = 0;
    public static boolean hitmarkerHeadshot = false;

    public static void triggerHitmarker(boolean isHeadshot) {
        hitmarkerTime = System.currentTimeMillis();
        hitmarkerHeadshot = isHeadshot;
    }

    @Override
    public void onHudRender(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) {
            return;
        }

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int centerX = screenW / 2;
        int centerY = screenH / 2;

        // 0. 8x Sniper Scope Overlay for L96 / Bolt when scoped in
        GunManager.GunInfo heldGun = GunManager.getHeldGun(client);
        boolean scoped8x = heldGun != null && ("l96".equals(heldGun.key) || "bolt".equals(heldGun.key))
                && RustViewModelAnimator.adsProgress >= 0.72f;
        if (scoped8x) {
            int size = Math.min(screenW, screenH);
            int left = (screenW - size) / 2;
            int top = (screenH - size) / 2;
            if (left > 0) {
                DrawableHelper.fill(matrices, 0, 0, left, screenH, 0xFF000000);
                DrawableHelper.fill(matrices, left + size, 0, screenW, screenH, 0xFF000000);
            }
            if (top > 0) {
                DrawableHelper.fill(matrices, left, 0, left + size, top, 0xFF000000);
                DrawableHelper.fill(matrices, left, top + size, left + size, screenH, 0xFF000000);
            }
            RenderSystem.enableTexture();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(1f, 1f, 1f, 1f);
            client.getTextureManager().bindTexture(TEX_SCOPE_8X);
            DrawableHelper.drawTexture(matrices, left, top, size, size, 0f, 0f, 1, 1, 1, 1);
            return;
        }

        // 1. TACTICAL RUST CROSSHAIR (hidden during ADS iron-sight aiming)
        float bowDraw = RustViewModelAnimator.bowDrawProgress;
        if (RustViewModelAnimator.adsProgress < 0.35f || bowDraw > 0.02f) {
            int gap = 4;
            if (bowDraw > 0.02f) {
                gap = Math.max(1, Math.round(4.0f * (1.0f - bowDraw)));
            } else if (client.player.isSprinting()) {
                gap = 9;
            } else if (client.player.isSneaking()) {
                gap = 2;
            } else if (Math.abs(client.player.getVelocity().x) > 0.05 || Math.abs(client.player.getVelocity().z) > 0.05) {
                gap = 6;
            }

            int lineLen = bowDraw > 0.02f ? 3 : 4;
            int color = bowDraw >= 0.42f ? 0xDD9AE65A : 0xBBFFFFFF;

            // Top tick
            DrawableHelper.fill(matrices, centerX, centerY - gap - lineLen, centerX + 1, centerY - gap, color);
            // Bottom tick
            DrawableHelper.fill(matrices, centerX, centerY + gap, centerX + 1, centerY + gap + lineLen, color);
            // Left tick
            DrawableHelper.fill(matrices, centerX - gap - lineLen, centerY, centerX - gap, centerY + 1, color);
            // Right tick
            DrawableHelper.fill(matrices, centerX + gap, centerY, centerX + gap + lineLen, centerY + 1, color);

            // Center dot
            DrawableHelper.fill(matrices, centerX, centerY, centerX + 1, centerY + 1, 0x88FFFFFF);
        }

        // Hunting Bow draw tension gauge under crosshair
        if (bowDraw > 0.02f) {
            int bw = 28;
            int bx = centerX - bw / 2;
            int by = centerY + 12;
            int fill = Math.max(1, Math.round(bw * Math.min(1.0f, bowDraw)));
            int barCol = bowDraw >= 0.42f ? 0xFF7AD645 : 0xFFE5A024;
            DrawableHelper.fill(matrices, bx - 1, by - 1, bx + bw + 1, by + 3, 0x99101010);
            DrawableHelper.fill(matrices, bx, by, bx + fill, by + 2, barCol);
        }

        // 2. RUST HITMARKER RENDERER (Duration ~280ms)
        long elapsed = System.currentTimeMillis() - hitmarkerTime;
        if (elapsed < 280) {
            int hitColor = hitmarkerHeadshot ? 0xFFe53935 : 0xFFFFFFFF; // Red for headshot, white for body
            int hGap = 3;
            int hLen = 4;

            // Diagonal slashes forming 'X'
            // Top-left
            DrawableHelper.fill(matrices, centerX - hGap - hLen, centerY - hGap - hLen, centerX - hGap, centerY - hGap, hitColor);
            // Top-right
            DrawableHelper.fill(matrices, centerX + hGap, centerY - hGap - hLen, centerX + hGap + hLen, centerY - hGap, hitColor);
            // Bottom-left
            DrawableHelper.fill(matrices, centerX - hGap - hLen, centerY + hGap, centerX - hGap, centerY + hGap + hLen, hitColor);
            // Bottom-right
            DrawableHelper.fill(matrices, centerX + hGap, centerY + hGap, centerX + hGap + hLen, centerY + hGap + hLen, hitColor);
        }
    }
}
