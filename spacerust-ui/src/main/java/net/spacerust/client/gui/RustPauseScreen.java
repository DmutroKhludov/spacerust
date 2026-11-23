package net.spacerust.client.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.spacerust.client.SpaceRustClient;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * SpaceRust Escape Pause Screen when pressing ESC in-game (1.21.11).
 */
public class RustPauseScreen extends Screen {

    private static final Identifier TEX_LOGO        = Identifier.of("spacerust", "textures/gui/logo.png");
    private static final Identifier TEX_SERVER_CARD = Identifier.of("spacerust", "textures/gui/rust_ui/server_card.png");

    private final int navBarH = 32;
    private final int subBarH = 26;

    public RustPauseScreen() {
        super(Text.literal("SpaceRust Menu"));
        RustFontRenderer.ensureLoaded();
    }

    @Override
    protected void init() {
        super.init();
        RustFontRenderer.ensureLoaded();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int w = this.width;
        int h = this.height;
        ctx.applyBlur();
        ctx.fill(0, 0, w, h, 0xD80B0C0B);

        RustFontRenderer bf = RustFontRenderer.boldSmall();
        RustFontRenderer sf = RustFontRenderer.small();
        RustFontRenderer hdf = RustFontRenderer.bold();

        // 1. Top Dark Nav Bar (ТЕКУЩИЙ СЕРВЕР | ИГРАТЬ | НАСТРОЙКИ | ВЫЙТИ)
        ctx.fill(0, 0, w, navBarH, 0xFF141514);
        if (this.client != null) {
            RustUiHelper.drawSprite(ctx, this.client, TEX_LOGO, 14, 6, 20, 20, 0xFFFFFFFF);
        }

        int navX = Math.max(68, w / 2 - 120);
        String[] tabs = { "ТЕКУЩИЙ СЕРВЕР", "ИГРАТЬ" };
        for (int i = 0; i < tabs.length; i++) {
            int tw = Math.round(bf.getWidth(tabs[i])) + 28;
            boolean active = (i == 0);
            boolean hov = RustUiHelper.inRect(mouseX, mouseY, navX, 0, tw, navBarH);
            if (active) {
                ctx.fill(navX, 0, navX + tw, navBarH, 0x664D1E17);
                ctx.fill(navX, navBarH - 2, navX + tw, navBarH, 0xFFCD412B);
            } else if (hov) {
                ctx.fill(navX, 0, navX + tw, navBarH, 0x22FFFFFF);
            }
            bf.drawCenteredY(ctx, tabs[i], navX + 14, 0, navBarH, active ? 0xFFF7EBE1 : 0xBBF7EBE1);
            navX += tw + 4;
        }

        // Right nav buttons: МЕНЮ MC, НАСТРОЙКИ, ВЫЙТИ
        int exitW = 66;
        int setW = 86;
        int mcW = 76;
        int exitX = w - exitW - 12;
        int setX = exitX - setW - 6;
        int mcX = setX - mcW - 6;
        drawNavBtn(ctx, mcX, 5, mcW, 22, "МОДЫ / MC", mouseX, mouseY, false);
        drawNavBtn(ctx, setX, 5, setW, 22, "НАСТРОЙКИ", mouseX, mouseY, false);
        drawNavBtn(ctx, exitX, 5, exitW, 22, "ВЫЙТИ", mouseX, mouseY, false);

        // 2. Server Sub-header + Respawn button
        int subY = navBarH;
        ctx.fill(0, subY, w, subY + subBarH, 0xFF1D1E1D);
        ctx.fill(0, subY + subBarH, w, subY + subBarH + 1, 0x22FFFFFF);

        ServerInfo si = this.client != null ? this.client.getCurrentServerEntry() : null;
        RustServerBrowserScreen.ServerEntry activeSrv = RustServerBrowserScreen.activeServer;
        String srvName = (activeSrv != null) ? activeSrv.name
                : (si != null ? si.name : "Наша | SpaceRust Server");
        String playersTxt = (activeSrv != null) ? (activeSrv.currentPlayers + "/" + activeSrv.maxPlayers) : "1/20";
        String mapTxt = (activeSrv != null) ? activeSrv.map.toUpperCase(Locale.ROOT) : "PROCEDURAL MAP";
        String wipeTxt = (activeSrv != null) ? activeSrv.wipeTag.toUpperCase(Locale.ROOT) : "ЕЖЕМЕСЯЧНЫЙ";
        String pingTxt = (activeSrv != null && activeSrv.pingMs >= 0) ? (activeSrv.pingMs + " MS") : "1 MS";

        if (this.client != null) {
            RustUiHelper.drawSprite(ctx, this.client, TEX_LOGO, 16, subY + 4, 18, 18, 0xFFFFFFFF);
        }
        bf.drawCenteredY(ctx, srvName, 42, subY, subBarH, 0xFFF7EBE1);

        int respW = 104;
        int respX = w - respW - 14;
        drawActionBtn(ctx, respX, subY + 4, respW, 18, "ПЕРЕРОДИТЬСЯ", mouseX, mouseY, true);

        // 3. Center SpaceRust Server Detail Card
        int cardW = Math.min(400, w - 40);
        int cardH = Math.min(286, Math.max(210, h - (subY + subBarH) - 42));
        int cardX = (w - cardW) / 2;
        int cardY = subY + subBarH + Math.max(24, (h - (subY + subBarH) - cardH) / 2);

        hdf.drawShadow(ctx, srvName, cardX, cardY - 16, 0xFFF7EBE1);
        ctx.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xFF191A19);
        RustUiHelper.drawBorder(ctx, cardX, cardY, cardW, cardH, 0x28FFFFFF);

        int bannerH = Math.max(96, Math.round(cardH * 0.46f));
        if (this.client != null) {
            RustUiHelper.drawSprite(ctx, this.client, TEX_SERVER_CARD, cardX + 1, cardY + 1, cardW - 2, bannerH, 0xFFFFFFFF);
            ctx.fill(cardX + 1, cardY + bannerH - 22, cardX + cardW - 1, cardY + bannerH + 1, 0x88101010);
            RustUiHelper.drawSprite(ctx, this.client, TEX_LOGO, cardX + 8, cardY + bannerH - 19, 16, 16, 0xFFFFFFFF);
            bf.drawCenteredY(ctx, "НАША  •  SPACERUST", cardX + 30, cardY + bannerH - 20, 18, 0xFFF7EBE1);
        }

        // Stat pills row under banner
        int statY = cardY + bannerH + 10;
        String[] sLbl = { "ИГРОКИ", "КАРТА", "ВАЙП БЫЛ", "ПИНГ" };
        String[] sVal = { playersTxt, mapTxt, wipeTxt, pingTxt };
        int sx = cardX + 12;
        for (int i = 0; i < sLbl.length; i++) {
            int bw = Math.round(bf.getWidth(sVal[i])) + 12;
            sf.draw(ctx, sLbl[i], sx, statY, 0x88F7EBE1);
            ctx.fill(sx, statY + 10, sx + bw, statY + 24, 0xFF252625);
            bf.drawCenteredY(ctx, sVal[i], sx + 6, statY + 10, 14, 0xFFF7EBE1);
            sx += Math.max(bw, Math.round(sf.getWidth(sLbl[i]))) + 12;
        }

        // Description text
        int descY = statY + 34;
        sf.draw(ctx, "Добро пожаловать на сервер Наша (SpaceRust 1.21.11) — выживание в стиле Rust.", cardX + 12, descY, 0xBBF7EBE1);
        sf.draw(ctx, "Стройте базы, добывайте ресурсы и используйте встроенное меню настроек.", cardX + 12, descY + 12, 0x99F7EBE1);

        // Bottom Buttons: ПРОДОЛЖИТЬ & ОТКЛЮЧИТЬСЯ
        int btnY = cardY + cardH - 30;
        drawActionBtn(ctx, cardX + 12, btnY, 118, 22, "ПРОДОЛЖИТЬ", mouseX, mouseY, false);
        drawActionBtn(ctx, cardX + cardW - 128, btnY, 116, 22, "ОТКЛЮЧИТЬСЯ", mouseX, mouseY, true);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawNavBtn(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my, boolean active) {
        boolean hov = RustUiHelper.inRect(mx, my, x, y, w, h);
        ctx.fill(x, y, x + w, y + h, active ? 0xFF333533 : (hov ? 0xFF2D2E2D : 0xFF222322));
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        bf.drawCenteredY(ctx, label, x + (w - bf.getWidth(label)) / 2f, y, h, 0xFFF7EBE1);
    }

    private void drawActionBtn(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my, boolean danger) {
        boolean hov = RustUiHelper.inRect(mx, my, x, y, w, h);
        int bg = danger ? (hov ? 0xFFE34D34 : 0xFFCD412B) : (hov ? 0xFF3D403D : 0xFF2B2D2B);
        ctx.fill(x, y, x + w, y + h, bg);
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        bf.drawCenteredY(ctx, label, x + (w - bf.getWidth(label)) / 2f, y, h, 0xFFF7EBE1);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0 || this.client == null) return super.mouseClicked(click, doubled);
        double mouseX = click.x();
        double mouseY = click.y();
        int w = this.width;
        int h = this.height;
        RustFontRenderer bf = RustFontRenderer.boldSmall();

        // Nav bar clicks (ТЕКУЩИЙ СЕРВЕР -> вернуться в мир, ИГРАТЬ -> браузер серверов)
        int navX = Math.max(68, w / 2 - 120);
        String[] tabs = { "ТЕКУЩИЙ СЕРВЕР", "ИГРАТЬ" };
        for (int i = 0; i < tabs.length; i++) {
            int tw = Math.round(bf.getWidth(tabs[i])) + 28;
            if (RustUiHelper.inRect(mouseX, mouseY, navX, 0, tw, navBarH)) {
                RustUiHelper.playMenuClick(this.client);
                if (i == 0) {
                    this.client.setScreen(null);
                } else {
                    RustServerBrowserScreen.currentTab = RustServerBrowserScreen.MenuTab.PLAY;
                    this.client.setScreen(new RustServerBrowserScreen());
                }
                return true;
            }
            navX += tw + 4;
        }

        int exitX = w - 66 - 12;
        int setX = exitX - 86 - 6;
        int mcX = setX - 76 - 6;
        if (RustUiHelper.inRect(mouseX, mouseY, mcX, 5, 76, 22)) {
            RustUiHelper.playMenuClick(this.client);
            SpaceRustClient.allowVanillaPauseOnce = true;
            this.client.setScreen(new GameMenuScreen(true));
            return true;
        }
        if (RustUiHelper.inRect(mouseX, mouseY, setX, 5, 86, 22)) {
            RustUiHelper.playMenuClick(this.client);
            this.client.setScreen(new RustSettingsScreen(this));
            return true;
        }
        if (RustUiHelper.inRect(mouseX, mouseY, exitX, 5, 66, 22)) {
            RustUiHelper.playMenuClick(this.client);
            disconnectToBrowser();
            return true;
        }

        // Suicide / Respawn
        int respX = w - 104 - 14;
        if (RustUiHelper.inRect(mouseX, mouseY, respX, navBarH + 4, 104, 18)) {
            RustUiHelper.playMenuClick(this.client);
            if (this.client.getNetworkHandler() != null) {
                this.client.getNetworkHandler().sendChatCommand("kill");
            }
            this.close();
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
            this.client.setScreen(null);
            return true;
        }
        if (RustUiHelper.inRect(mouseX, mouseY, cardX + cardW - 128, btnY, 116, 22)) {
            RustUiHelper.playMenuClick(this.client);
            disconnectToBrowser();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private void disconnectToBrowser() {
        if (this.client == null) return;
        RustServerBrowserScreen.currentTab = RustServerBrowserScreen.MenuTab.HOME;
        if (this.client.isInSingleplayer()) {
            this.client.disconnectWithSavingScreen();
        } else {
            this.client.disconnectWithProgressScreen();
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (this.client != null) {
                this.client.setScreen(null);
            }
            return true;
        }
        return super.keyPressed(input);
    }
}
