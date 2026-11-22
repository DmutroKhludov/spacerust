package net.spacerust.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.spacerust.client.gui.RustFontRenderer;
import net.spacerust.client.gui.RustUiHelper;

public class RustHudOverlay implements HudRenderCallback {
    // Rust metabolism stats (can be updated from packets or player status)
    public static float health = 100f;
    public static int maxHealth = 100;
    public static int calories = 480;
    public static int hydration = 230;
    public static int radiation = 0;
    public static int comfort = 50;
    public static boolean isBleeding = false;

    @Override
    public void onHudRender(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) {
            return;
        }
        // Avoid drawing HUD bars twice when Rust inventory/crafting screen already renders them
        if (client.currentScreen != null) {
            return;
        }

        RustFontRenderer.ensureLoaded();
        health = client.player.getHealth() * 5.0f;
        calories = client.player.getHungerManager().getFoodLevel() * 25;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Draw CoilRust / Rust bottom-right metabolism bars (HP, Water, Food sprites)
        RustUiHelper.drawMetabolismBars(matrices, client, screenWidth, screenHeight);

        // Optional status chips above bars
        int statusX = screenWidth - 114;
        int statusY = screenHeight - 68;
        RustFontRenderer sf = RustFontRenderer.boldSmall();

        if (radiation > 0) {
            DrawableHelper.fill(matrices, statusX, statusY, statusX + 52, statusY + 11, 0xE6C73E2B);
            sf.drawCenteredY(matrices, "РАД " + radiation, statusX + 4, statusY, 11, 0xFFFFFFFF);
        }
        if (isBleeding) {
            DrawableHelper.fill(matrices, statusX + 54, statusY, statusX + 104, statusY + 11, 0xE6C73E2B);
            sf.drawCenteredY(matrices, "КРОВОТОК", statusX + 57, statusY, 11, 0xFFFFFFFF);
        }
    }
}
