package net.spacerust.client.gui;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.spacerust.client.hud.RustHudOverlay;

/**
 * Authentic Rust Death & Respawn Map Screen ("МЁРТВ" + "ВОЗРОДИТЬСЯ >>").
 */
public class RustDeathScreen extends Screen {

    private final String reason;

    public RustDeathScreen(Text vanillaMessage) {
        super(new LiteralText("Dead"));
        this.reason = vanillaMessage != null ? vanillaMessage.getString() : "Вы погибли";
        RustFontRenderer.ensureLoaded();
    }

    @Override
    protected void init() {
        super.init();
        RustFontRenderer.ensureLoaded();
        if (this.client != null) {
            if (this.client.player != null) {
                RustMapScreen.lastDeathX = this.client.player.getX();
                RustMapScreen.lastDeathZ = this.client.player.getZ();
                RustMapScreen.hasDeathMarker = true;
            }
            RustMapScreen.ensureMapBuilt(this.client);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int w = this.width;
        int h = this.height;
        DrawableHelper.fill(matrices, 0, 0, w, h, 0xFF122833);

        int mapBoxSize = Math.min(w - 24, h - 84);
        RustMapScreen.renderMapViewport(matrices, this.client, w, 34, mapBoxSize,
                RustMapScreen.lastDeathX * 0.25, RustMapScreen.lastDeathZ * 0.25, 1.0f, false, true);

        // Top Dark Bar ("МЁРТВ")
        DrawableHelper.fill(matrices, 0, 0, w, 30, 0xEE141514);
        RustFontRenderer hdf = RustFontRenderer.boldLarge();
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        RustFontRenderer sf = RustFontRenderer.small();
        hdf.drawCenteredY(matrices, "МЁРТВ", 14, 0, 30, 0xFFE5DED6);

        int badgeW = Math.round(bf.getWidth(this.reason)) + 24;
        DrawableHelper.fill(matrices, w / 2 - badgeW / 2, 5, w / 2 + badgeW / 2, 25, 0xFF232423);
        DrawableHelper.fill(matrices, w / 2 - badgeW / 2, 5, w / 2 - badgeW / 2 + 24, 25, 0xFF8C2D22);
        sf.drawCenteredY(matrices, "!", w / 2 - badgeW / 2 + 10, 5, 20, 0xFFFFFFFF);
        bf.drawCenteredY(matrices, this.reason, w / 2 - badgeW / 2 + 30, 5, 20, 0xFFF7EBE1);

        // Bottom Bar (Left Beach/Sleeping Bag Card + Right Green "ВОЗРОДИТЬСЯ >>")
        DrawableHelper.fill(matrices, 0, h - 46, w, h, 0xEE141514);

        int bagX = 14;
        int bagY = h - 38;
        int bagW = 132;
        int bagH = 30;
        boolean bagHov = RustUiHelper.inRect(mouseX, mouseY, bagX, bagY, bagW, bagH);
        DrawableHelper.fill(matrices, bagX, bagY, bagX + bagW, bagY + bagH, bagHov ? 0xFFD95A2B : 0xFFCD412B);
        bf.drawCenteredY(matrices, "СЛУЧАЙНЫЙ ПЛЯЖ", bagX + 10, bagY, bagH, 0xFFFFFFFF);

        int respW = 134;
        int respH = 30;
        int respX = w - respW - 14;
        int respY = h - 38;
        boolean respHov = RustUiHelper.inRect(mouseX, mouseY, respX, respY, respW, respH);
        DrawableHelper.fill(matrices, respX, respY, respX + respW, respY + respH, respHov ? 0xFF74A037 : 0xFF5E802E);
        String respTxt = "ВОЗРОДИТЬСЯ  >>";
        bf.drawCenteredY(matrices, respTxt, respX + (respW - bf.getWidth(respTxt)) / 2f, respY, respH, 0xFFF7EBE1);

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.client != null && this.client.player != null) {
            int w = this.width;
            int h = this.height;
            boolean clickBag = RustUiHelper.inRect(mouseX, mouseY, 14, h - 38, 132, 30);
            boolean clickResp = RustUiHelper.inRect(mouseX, mouseY, w - 134 - 14, h - 38, 134, 30);
            if (clickBag || clickResp) {
                RustUiHelper.playMenuClick(this.client);
                RustHudOverlay.triggerWakeupFade();
                this.client.player.requestRespawn();
                this.client.openScreen(null);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
