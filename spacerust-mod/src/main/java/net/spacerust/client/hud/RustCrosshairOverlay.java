package net.spacerust.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

public class RustCrosshairOverlay implements HudRenderCallback {
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

        // 1. TACTICAL RUST CROSSHAIR
        // Dynamic gap based on player movement
        int gap = 4;
        if (client.player.isSprinting()) {
            gap = 9;
        } else if (client.player.isSneaking()) {
            gap = 2;
        } else if (Math.abs(client.player.getVelocity().x) > 0.05 || Math.abs(client.player.getVelocity().z) > 0.05) {
            gap = 6;
        }

        int lineLen = 4;
        int color = 0xBBFFFFFF; // Translucent tactical white

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
