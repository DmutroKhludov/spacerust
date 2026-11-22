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

        // 1. Draw Rust 6-slot In-Game Hotbar at bottom center (1:1 with screenshot)
        RustUiHelper.drawHotbar(matrices, client, -1, -1, screenWidth, screenHeight, client.getItemRenderer(), false);

        // 2. Draw enlarged Rust bottom-right metabolism bars (HP, Water, Food sprites)
        RustUiHelper.drawMetabolismBars(matrices, client, screenWidth, screenHeight);

        // 3. Draw status chips above metabolism bars
        RustUiHelper.drawStatusChips(matrices, screenWidth, screenHeight, radiation, isBleeding, comfort);
    }
}
