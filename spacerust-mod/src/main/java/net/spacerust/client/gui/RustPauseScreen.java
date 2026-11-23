package net.spacerust.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.SaveLevelScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.Locale;

/**
 * SpaceRust Escape Pause Screen when pressing ESC in-game.
 */
public class RustPauseScreen extends Screen {

    private static final Identifier TEX_LOGO        = new Identifier("spacerust", "textures/gui/logo.png");
    private static final Identifier TEX_SERVER_CARD = new Identifier("spacerust", "textures/gui/rust_ui/server_card.png");

    private final int navBarH = 32;
    private final int subBarH = 26;

    public RustPauseScreen() {
        super(new LiteralText("SpaceRust Menu"));
        RustFontRenderer.ensureLoaded();
    }

    @Override
    protected void init() {
        super.init();
        RustFontRenderer.ensureLoaded();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int w = this.width;
        int h = this.height;
        DrawableHelper.fill(matrices, 0, 0, w, h, 0xE60B0C0B);

        RustFontRenderer bf = RustFontRenderer.boldSmall();
        RustFontRenderer sf = RustFontRenderer.small();
        RustFontRenderer hdf = RustFontRenderer.bold();

        // 1. Top Dark Nav Bar (ТЕКУЩИЙ СЕРВЕР | ИГРАТЬ | НАСТРОЙКИ | ВЫЙТИ)
        DrawableHelper.fill(matrices, 0, 0, w, navBarH, 0xFF141514);
        if (this.client != null) {
            RustUiHelper.drawSprite(matrices, this.client, TEX_LOGO, 14, 6, 20, 20, 0xFFFFFFFF);
        }

        int navX = Math.max(68, w / 2 - 120);
        String[] tabs = { "ТЕКУЩИЙ СЕРВЕР", "ИГРАТЬ" };
        for (int i = 0; i < tabs.length; i++) {
            int tw = Math.round(bf.getWidth(tabs[i])) + 28;
            boolean active = (i == 0);
            boolean hov = RustUiHelper.inRect(mouseX, mouseY, navX, 0, tw, navBarH);
            if (active) {
                DrawableHelper.fill(matrices, navX, 0, navX + tw, navBarH, 0x664D1E17);
                DrawableHelper.fill(matrices, navX, navBarH - 2, navX + tw, navBarH, 0xFFCD412B);
            } else if (hov) {
                DrawableHelper.fill(matrices, navX, 0, navX + tw, navBarH, 0x22FFFFFF);
            }
            bf.drawCenteredY(matrices, tabs[i], navX + 14, 0, navBarH, active ? 0xFFF7EBE1 : 0xBBF7EBE1);
            navX += tw + 4;
        }

        // Right nav buttons: НАСТРОЙКИ, ВЫЙТИ
        int exitW = 66;
        int setW = 86;
        int exitX = w - exitW - 12;
        int setX = exitX - setW - 6;
        drawNavBtn(matrices, setX, 5, setW, 22, "НАСТРОЙКИ", mouseX, mouseY, false);
        drawNavBtn(matrices, exitX, 5, exitW, 22, "ВЫЙТИ", mouseX, mouseY, false);

        // 2. Server Sub-header + Suicide ("ПЕРЕРОДИТЬСЯ") button
        int subY = navBarH;
        DrawableHelper.fill(matrices, 0, subY, w, subY + subBarH, 0xFF1D1E1D);
        DrawableHelper.fill(matrices, 0, subY + subBarH, w, subY + subBarH + 1, 0x22FFFFFF);

        ServerInfo si = this.client != null ? this.client.getCurrentServerEntry() : null;
        RustServerBrowserScreen.ServerEntry activeSrv = RustServerBrowserScreen.activeServer;
        String srvName = (activeSrv != null) ? activeSrv.name
                : (si != null ? si.name : "SpaceRust | Локальный сервер");
        String playersTxt = (activeSrv != null) ? (activeSrv.currentPlayers + "/" + activeSrv.maxPlayers) : "1/350";
        String mapTxt = (activeSrv != null) ? activeSrv.map.toUpperCase(Locale.ROOT) : "PROCEDURAL MAP";
        String wipeTxt = (activeSrv != null) ? activeSrv.wipeTag.toUpperCase(Locale.ROOT) : "СЕГОДНЯ";
        String pingTxt = (activeSrv != null && activeSrv.pingMs >= 0) ? (activeSrv.pingMs + " MS") : "0 MS";

        if (this.client != null) {
            RustUiHelper.drawSprite(matrices, this.client, TEX_LOGO, 16, subY + 4, 18, 18, 0xFFFFFFFF);
        }
        bf.drawCenteredY(matrices, srvName, 42, subY, subBarH, 0xFFF7EBE1);

        int respW = 104;
        int respX = w - respW - 14;
        drawActionBtn(matrices, respX, subY + 4, respW, 18, "ПЕРЕРОДИТЬСЯ", mouseX, mouseY, true);

        // 3. Center SpaceRust Server Detail Card
        int cardW = Math.min(400, w - 40);
        int cardH = Math.min(286, Math.max(210, h - (subY + subBarH) - 42));
        int cardX = (w - cardW) / 2;
        int cardY = subY + subBarH + Math.max(24, (h - (subY + subBarH) - cardH) / 2);

        hdf.drawShadow(matrices, srvName, cardX, cardY - 16, 0xFFF7EBE1);
        DrawableHelper.fill(matrices, cardX, cardY, cardX + cardW, cardY + cardH, 0xFF191A19);
        RustUiHelper.drawBorder(matrices, cardX, cardY, cardW, cardH, 0x28FFFFFF);

        int bannerH = Math.max(96, Math.round(cardH * 0.46f));
        if (this.client != null) {
            RenderSystem.enableTexture();
            RenderSystem.color4f(1f, 1f, 1f, 1f);
            this.client.getTextureManager().bindTexture(TEX_SERVER_CARD);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            DrawableHelper.drawTexture(matrices, cardX + 1, cardY + 1, cardW - 2, bannerH, 0f, 0f, 1, 1, 1, 1);
            // Subtle SpaceRust emblem + label overlay on bottom-left of banner
            DrawableHelper.fill(matrices, cardX + 1, cardY + bannerH - 22, cardX + cardW - 1, cardY + bannerH + 1, 0x88101010);
            RustUiHelper.drawSprite(matrices, this.client, TEX_LOGO, cardX + 8, cardY + bannerH - 19, 16, 16, 0xFFFFFFFF);
            bf.drawCenteredY(matrices, "SPACERUST SURVIVAL", cardX + 30, cardY + bannerH - 20, 18, 0xFFF7EBE1);
        }

        // Stat pills row under banner
        int statY = cardY + bannerH + 10;
        String[] sLbl = { "ИГРОКИ", "КАРТА", "ВАЙП БЫЛ", "ПИНГ" };
        String[] sVal = { playersTxt, mapTxt, wipeTxt, pingTxt };
        int sx = cardX + 12;
        for (int i = 0; i < sLbl.length; i++) {
            int bw = Math.round(bf.getWidth(sVal[i])) + 12;
            sf.draw(matrices, sLbl[i], sx, statY, 0x88F7EBE1);
            DrawableHelper.fill(matrices, sx, statY + 10, sx + bw, statY + 24, 0xFF252625);
            bf.drawCenteredY(matrices, sVal[i], sx + 6, statY + 10, 14, 0xFFF7EBE1);
            sx += Math.max(bw, Math.round(sf.getWidth(sLbl[i]))) + 12;
        }

        // Description text
        int descY = statY + 34;
        sf.draw(matrices, "Добро пожаловать в SpaceRust — выживание на острове SpaceRust.", cardX + 12, descY, 0xBBF7EBE1);
        sf.draw(matrices, "Добывайте ресурсы по меткам, стройте базы, крафтите [Q] и открывайте карту [G].", cardX + 12, descY + 12, 0x99F7EBE1);

        // Bottom Buttons: ПРОДОЛЖИТЬ & ОТКЛЮЧИТЬСЯ
        int btnY = cardY + cardH - 30;
        drawActionBtn(matrices, cardX + 12, btnY, 118, 22, "ПРОДОЛЖИТЬ", mouseX, mouseY, false);
        drawActionBtn(matrices, cardX + cardW - 128, btnY, 116, 22, "ОТКЛЮЧИТЬСЯ", mouseX, mouseY, true);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawNavBtn(MatrixStack matrices, int x, int y, int w, int h, String label, int mx, int my, boolean active) {
        boolean hov = RustUiHelper.inRect(mx, my, x, y, w, h);
        DrawableHelper.fill(matrices, x, y, x + w, y + h, active ? 0xFF333533 : (hov ? 0xFF2D2E2D : 0xFF222322));
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        bf.drawCenteredY(matrices, label, x + (w - bf.getWidth(label)) / 2f, y, h, 0xFFF7EBE1);
    }

    private void drawActionBtn(MatrixStack matrices, int x, int y, int w, int h, String label, int mx, int my, boolean danger) {
        boolean hov = RustUiHelper.inRect(mx, my, x, y, w, h);
        int bg = danger ? (hov ? 0xFFE34D34 : 0xFFCD412B) : (hov ? 0xFF3D403D : 0xFF2B2D2B);
        DrawableHelper.fill(matrices, x, y, x + w, y + h, bg);
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        bf.drawCenteredY(matrices, label, x + (w - bf.getWidth(label)) / 2f, y, h, 0xFFF7EBE1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || this.client == null) return super.mouseClicked(mouseX, mouseY, button);
        int w = this.width;
        int h = this.height;
        RustFontRenderer bf = RustFontRenderer.boldSmall();

        // Nav bar clicks (ТЕКУЩИЙ СЕРВЕР, ИГРАТЬ)
        int navX = Math.max(68, w / 2 - 120);
        String[] tabs = { "ТЕКУЩИЙ СЕРВЕР", "ИГРАТЬ" };
        for (int i = 0; i < tabs.length; i++) {
            int tw = Math.round(bf.getWidth(tabs[i])) + 28;
            if (RustUiHelper.inRect(mouseX, mouseY, navX, 0, tw, navBarH)) {
                RustUiHelper.playMenuClick(this.client);
                if (i == 0) {
                    this.client.openScreen(null);
                } else {
                    RustServerBrowserScreen.currentTab = RustServerBrowserScreen.MenuTab.PLAY;
                    this.client.openScreen(new RustServerBrowserScreen());
                }
                return true;
            }
            navX += tw + 4;
        }

        int exitX = w - 66 - 12;
        int setX = exitX - 86 - 6;
        if (RustUiHelper.inRect(mouseX, mouseY, setX, 5, 86, 22)) {
            RustUiHelper.playMenuClick(this.client);
            this.client.openScreen(new RustSettingsScreen(this));
            return true;
        }
        if (RustUiHelper.inRect(mouseX, mouseY, exitX, 5, 66, 22)) {
            RustUiHelper.playMenuClick(this.client);
            disconnectToBrowser();
            return true;
        }

        // Suicide / Respawn ("ПЕРЕРОДИТЬСЯ")
        int respX = w - 104 - 14;
        if (RustUiHelper.inRect(mouseX, mouseY, respX, navBarH + 4, 104, 18)) {
            RustUiHelper.playMenuClick(this.client);
            if (this.client.player != null) {
                this.client.player.sendChatMessage("/kill");
            }
            this.onClose();
            return true;
        }

        // Center card bottom buttons
        int cardW = Math.min(400, w - 40);
        int cardH = Math.min(286, Math.max(210, h - (navBarH + subBarH) - 42));
        int cardX = (w - cardW) / 2;
        int cardY = navBarH + subBarH + Math.max(24, (h - (navBarH + subBarH) - cardH) / 2);
        int btnY = cardY + cardH - 30;

        if (RustUiHelper.inRect(mouseX, mouseY, cardX + 12, btnY, 118, 22)) {
            RustUiHelper.playMenuClick(this.client);
            this.client.openScreen(null);
            return true;
        }
        if (RustUiHelper.inRect(mouseX, mouseY, cardX + cardW - 128, btnY, 116, 22)) {
            RustUiHelper.playMenuClick(this.client);
            disconnectToBrowser();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void disconnectToBrowser() {
        if (this.client == null) return;
        boolean single = this.client.isInSingleplayer();
        if (this.client.world != null) {
            this.client.world.disconnect();
        }
        if (single) {
            this.client.disconnect(new SaveLevelScreen(new TranslatableText("menu.savingLevel")));
        } else {
            this.client.disconnect();
        }
        RustServerBrowserScreen.currentTab = RustServerBrowserScreen.MenuTab.HOME;
        this.client.openScreen(new RustServerBrowserScreen());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.client.openScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
