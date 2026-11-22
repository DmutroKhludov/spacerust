package net.spacerust.client.building;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.spacerust.client.gui.RustFontRenderer;
import net.spacerust.client.gui.RustUiHelper;

public class RustBuildingHudOverlay implements HudRenderCallback {

    @Override
    public void onHudRender(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.currentScreen != null) return;

        RustBuildingManager.BuildingPieceData hovered = RustBuildingManager.hoveredBuilding;
        if (hovered == null) return;

        if (!RustBuildingManager.isHoldingHammer(client) && !RustBuildingManager.isHoldingBuildingPlan(client)) return;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();
        RustFontRenderer sf = RustFontRenderer.small();

        int cardW = 164;
        int cardH = 42;
        int cardX = (screenW - cardW) / 2;
        int cardY = screenH / 2 + 35; // Just below crosshair

        // Card background (Rust olive dark)
        DrawableHelper.fill(matrices, cardX, cardY, cardX + cardW, cardY + cardH, 0xD0232522);
        RustUiHelper.drawBorder(matrices, cardX, cardY, cardW, cardH, 0x44F7EBE1);

        // Header
        DrawableHelper.fill(matrices, cardX, cardY, cardX + cardW, cardY + 14, 0x99181614);
        String title = hovered.tier.title.toUpperCase() + " " + hovered.piece.title.toUpperCase();
        bfSmall.drawCenteredY(matrices, title, cardX + 6, cardY, 14, 0xFFF7EBE1);

        // Health bar
        float ratio = Math.max(0f, Math.min(1f, hovered.health / hovered.maxHealth));
        int barX = cardX + 6;
        int barY = cardY + 18;
        int barW = cardW - 12;
        int barH = 8;
        DrawableHelper.fill(matrices, barX, barY, barX + barW, barY + barH, 0x99000000);

        int fillW = (int) (barW * ratio);
        int barColor = (ratio > 0.5f) ? 0xFF8DC63F : ((ratio > 0.2f) ? 0xFFE5A823 : 0xFFD3362D);
        if (fillW > 0) {
            DrawableHelper.fill(matrices, barX, barY, barX + fillW, barY + barH, barColor);
        }

        // HP numbers and stability
        String hpText = (int) hovered.health + " / " + (int) hovered.maxHealth + " HP";
        sf.draw(matrices, hpText, cardX + 6, cardY + 28, 0xFFCCCCCC);

        String stabText = "СТАБИЛЬНОСТЬ: 100%";
        float stabW = sf.getWidth(stabText);
        sf.draw(matrices, stabText, cardX + cardW - stabW - 6, cardY + 28, 0xFF8DC63F);
    }
}
