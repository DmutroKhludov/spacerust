package net.spacerust.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

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

        // Sync health with player health if alive
        health = client.player.getHealth() * 5.0f; // Scale 20 vanilla hearts -> 100 Rust HP
        int hunger = client.player.getHungerManager().getFoodLevel() * 25; // Scale 20 -> 500 Rust calories
        calories = hunger;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        TextRenderer textRenderer = client.textRenderer;

        // Position: Bottom Right corner (Rust standard)
        int hudWidth = 140;
        int barHeight = 8;
        int startX = screenWidth - hudWidth - 16;
        int startY = screenHeight - 65;

        // 1. HEALTH BAR (Green / Red)
        drawRustBar(matrices, textRenderer, startX, startY, hudWidth, barHeight,
                "HP", (int) health, maxHealth, 0xFF4caf50, 0xFF1b5e20);

        // 2. HUNGER / CALORIES BAR (Orange)
        drawRustBar(matrices, textRenderer, startX, startY + 14, hudWidth, barHeight,
                "FOOD", calories, 500, 0xFFff9800, 0xFFe65100);

        // 3. HYDRATION / WATER BAR (Cyan / Blue)
        drawRustBar(matrices, textRenderer, startX, startY + 28, hudWidth, barHeight,
                "WATER", hydration, 250, 0xFF00bcd4, 0xFF006064);

        // 4. STATUS INDICATORS (Comfort / Radiation / Bleeding)
        int statusY = startY + 44;
        if (radiation > 0) {
            String radText = "RAD " + radiation;
            DrawableHelper.fill(matrices, startX, statusY, startX + 45, statusY + 12, 0xCCf44336);
            textRenderer.draw(matrices, radText, startX + 4, statusY + 2, 0xFFFFFFFF);
        } else {
            String comfortText = comfort + "% COMFORT";
            textRenderer.draw(matrices, comfortText, startX, statusY + 2, 0xAAFFFFFF);
        }

        if (isBleeding) {
            String bleedText = "! BLEEDING";
            DrawableHelper.fill(matrices, startX + 55, statusY, startX + 130, statusY + 12, 0xEEe53935);
            textRenderer.draw(matrices, bleedText, startX + 60, statusY + 2, 0xFFFFFFFF);
        }
    }

    private void drawRustBar(MatrixStack matrices, TextRenderer textRenderer, int x, int y,
                            int width, int height, String label, int current, int max,
                            int fillColor, int bgGlow) {
        // Dark background plate
        DrawableHelper.fill(matrices, x - 1, y - 1, x + width + 1, y + height + 1, 0xDD12161f);

        // Fill bar based on percentage
        float ratio = Math.max(0.0f, Math.min(1.0f, (float) current / (float) max));
        int fillWidth = (int) (width * ratio);
        if (fillWidth > 0) {
            DrawableHelper.fill(matrices, x, y, x + fillWidth, y + height, fillColor);
        }

        // Text display: e.g. "HP 95"
        String text = label + "  " + current;
        textRenderer.draw(matrices, text, x + 3, y - 1, 0xFFFFFFFF);
    }
}
