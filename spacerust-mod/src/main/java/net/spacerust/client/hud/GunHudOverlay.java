package net.spacerust.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.spacerust.client.gui.RustFontRenderer;
import net.spacerust.client.gui.RustUiHelper;
import net.spacerust.client.guns.RustShootingManager;
import net.spacerust.client.guns.RustWeapon;

public class GunHudOverlay implements HudRenderCallback {

    @Override
    public void onHudRender(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) {
            return;
        }

        RustWeapon weapon = RustShootingManager.getHeldWeapon(client);
        if (weapon == null) {
            return; // Not holding a weapon
        }

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        RustFontRenderer bf = RustFontRenderer.bold();
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();
        RustFontRenderer sf = RustFontRenderer.small();

        int ammo = RustShootingManager.getAmmo(weapon);
        int maxAmmo = weapon.maxAmmo;

        // Position bottom-right, just above Rust metabolism bars
        int cardW = 155;
        int cardH = 44;
        int cardX = screenW - cardW - 12;
        int cardY = screenH - 105;

        // Background plate (Rust signature slate-olive)
        DrawableHelper.fill(matrices, cardX, cardY, cardX + cardW, cardY + cardH, 0xD81B1C1A);
        RustUiHelper.drawBorder(matrices, cardX, cardY, cardW, cardH, 0x44F7EBE1);

        // Weapon Icon
        int iconS = 28;
        RustUiHelper.drawSprite(matrices, client, weapon.icon, cardX + 8, cardY + (cardH - iconS) / 2, iconS, iconS, 0xFFFFFFFF);

        // Weapon Title
        bfSmall.draw(matrices, weapon.title.toUpperCase(), cardX + 42, cardY + 8, 0xFFF7EBE1);

        // Caliber
        sf.draw(matrices, weapon.ammoName, cardX + 42, cardY + 24, 0x88F7EBE1);

        // Ammo Counter (e.g. "30 / 30")
        String ammoText = ammo + " / " + maxAmmo;
        int ammoColor = (ammo == 0) ? 0xFFE0462C : ((ammo <= 5) ? 0xFFE5A823 : 0xFF8DC63F);
        float tw = bf.getWidth(ammoText);
        bf.draw(matrices, ammoText, cardX + cardW - tw - 10, cardY + 12, ammoColor);

        // Reload prompt if empty
        if (ammo == 0) {
            String rPrompt = "НАЖМИТЕ [R]";
            float rw = sf.getWidth(rPrompt);
            sf.draw(matrices, rPrompt, cardX + cardW - rw - 10, cardY + 28, 0xFFE0462C);
        }
    }
}
