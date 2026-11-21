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
        int barHeight = 9;
        int startX = screenWidth - hudWidth - 14;
        int startY = screenHeight - 65;

        // 1. HEALTH BAR (Green, turns Red when critical <= 25)
        int hpColor = health <= 25 ? 0xFFE53935 : 0xFF4CAF50;
        drawRustBar(matrices, textRenderer, startX, startY, hudWidth, barHeight,
                "+  HP", (int) health, maxHealth, hpColor);

        // 2. HUNGER / CALORIES BAR (Orange)
        drawRustBar(matrices, textRenderer, startX, startY + 14, hudWidth, barHeight,
                "ЕДА", calories, 500, 0xFFFF9800);

        // 3. HYDRATION / WATER BAR (Cyan / Blue)
        drawRustBar(matrices, textRenderer, startX, startY + 28, hudWidth, barHeight,
                "ВОДА", hydration, 250, 0xFF00BCD4);

        // 4. STATUS INDICATORS (Comfort / Radiation / Bleeding)
        int statusY = startY + 44;
        if (radiation > 0) {
            String radText = "РАД " + radiation;
            DrawableHelper.fill(matrices, startX, statusY, startX + 50, statusY + 12, 0xCCF44336);
            textRenderer.drawWithShadow(matrices, radText, startX + 4, statusY + 2, 0xFFFFFFFF);
        } else {
            String comfortText = comfort + "% КОМФОРТ";
            textRenderer.drawWithShadow(matrices, comfortText, startX, statusY + 2, 0xAAFFFFFF);
        }

        if (isBleeding) {
            String bleedText = "! КРОВОТЕЧЕНИЕ";
            DrawableHelper.fill(matrices, startX + 60, statusY, startX + 138, statusY + 12, 0xEEE53935);
            textRenderer.drawWithShadow(matrices, bleedText, startX + 64, statusY + 2, 0xFFFFFFFF);
        }
    }

    private void drawRustBar(MatrixStack matrices, TextRenderer textRenderer, int x, int y,
                            int width, int height, String label, int current, int max,
                            int fillColor) {
        // Dark background plate with subtle border
        DrawableHelper.fill(matrices, x - 1, y - 1, x + width + 1, y + height + 1, 0xAA0A0D12);
        DrawableHelper.fill(matrices, x, y, x + width, y + height, 0xCC151922);

        // Fill bar based on percentage
        float ratio = Math.max(0.0f, Math.min(1.0f, (float) current / (float) max));
        int fillWidth = (int) (width * ratio);
        if (fillWidth > 0) {
            DrawableHelper.fill(matrices, x, y, x + fillWidth, y + height, fillColor);
        }

        // Left Label: e.g. "+ HP", "ЕДА", "ВОДА"
        textRenderer.drawWithShadow(matrices, label, x + 3, y + (height - 8) / 2, 0xFFFFFFFF);

        // Right Value: e.g. "100", "500", "250"
        String valStr = String.valueOf(current);
        int valW = textRenderer.getWidth(valStr);
        textRenderer.drawWithShadow(matrices, valStr, x + width - valW - 3, y + (height - 8) / 2, 0xFFFFFFFF);
    }
}
