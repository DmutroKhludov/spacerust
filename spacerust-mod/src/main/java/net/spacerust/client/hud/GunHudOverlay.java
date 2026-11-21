package net.spacerust.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.spacerust.client.guns.GunManager;

public class GunHudOverlay implements HudRenderCallback {

    @Override
    public void onHudRender(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) {
            return;
        }

        GunManager.GunInfo gun = GunManager.getHeldGun(client);
        if (gun == null) {
            return; // Not holding a weapon
        }

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        TextRenderer textRenderer = client.textRenderer;

        // Display above metabolism HUD
        int boxW = 140;
        int boxH = 22;
        int x = screenW - boxW - 16;
        int y = screenH - 95;

        // Dark background plate with orange corner accent
        DrawableHelper.fill(matrices, x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0xEE12161f);
        DrawableHelper.fill(matrices, x - 1, y - 1, x + 2, y + boxH + 1, 0xFFe0462c);

        // Weapon Name
        textRenderer.draw(matrices, gun.name, x + 8, y + 4, 0xFFFFFFFF);

        // Ammo Counter (e.g. "30 / 30")
        int ammoColor = GunManager.currentAmmo <= 5 ? 0xFFf44336 : 0xFFffb74d;
        String ammoText = GunManager.currentAmmo + " / " + gun.maxAmmo;
        int ammoW = textRenderer.getWidth(ammoText);
        textRenderer.draw(matrices, ammoText, x + boxW - ammoW - 8, y + 4, ammoColor);

        // Caliber subtitle
        String calText = gun.ammoType;
        textRenderer.draw(matrices, calText, x + 8, y + 14, 0x66FFFFFF);

        if (GunManager.currentAmmo == 0) {
            String reloadAlert = "НЕТ ПАТРОНОВ [R]";
            int alertW = textRenderer.getWidth(reloadAlert);
            textRenderer.draw(matrices, reloadAlert, x + boxW - alertW - 8, y + 14, 0xFFf44336);
        }
    }
}
