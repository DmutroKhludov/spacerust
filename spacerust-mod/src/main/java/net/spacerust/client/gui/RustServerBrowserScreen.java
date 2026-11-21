package net.spacerust.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DirectConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class RustServerBrowserScreen extends Screen {

    public static class ServerEntry {
        public final String name;
        public final String map;
        public final String region;
        public final String ip;
        public final String[] tags;
        public final ServerInfo serverInfo;

        // Dynamic Real Server Status (Updated via real ping)
        public boolean isOnline = false;
        public boolean isPinging = true;
        public int currentPlayers = 0;
        public int maxPlayers = 0;
        public long pingMs = -1;
        public String statusText = "Опрос...";

        public ServerEntry(String name, String map, String region, String ip, String[] tags) {
            this.name = name;
            this.map = map;
            this.region = region;
            this.ip = ip;
            this.tags = tags;
            this.serverInfo = new ServerInfo(name, ip, false);
        }

        public void updateStatsFromInfo() {
            this.isPinging = false;
            if (this.serverInfo.ping >= 0) {
                this.isOnline = true;
                this.pingMs = this.serverInfo.ping;
                if (this.serverInfo.playerCountLabel != null) {
                    String raw = this.serverInfo.playerCountLabel.getString();
                    if (raw.contains("/")) {
                        String[] parts = raw.split("/");
                        try {
                            this.currentPlayers = Integer.parseInt(parts[0].trim());
                            this.maxPlayers = Integer.parseInt(parts[1].trim());
                            this.statusText = this.currentPlayers + " / " + this.maxPlayers;
                        } catch (Exception ignored) {
                            this.statusText = raw;
                        }
                    } else {
                        this.statusText = raw;
                    }
                } else {
                    this.statusText = "0 / 20";
                }
            } else {
                this.isOnline = false;
                this.pingMs = -1;
                this.currentPlayers = 0;
                this.maxPlayers = 0;
                this.statusText = "ОФФЛАЙН";
            }
        }
    }

    // Only 1 server as requested: UNLIMITED X2
    public static final List<ServerEntry> SERVERS = new ArrayList<>();
    static {
        SERVERS.add(new ServerEntry(
                "UNLIMITED X2",
                "Procedural Map 3800",
                "LOCAL",
                "127.0.0.1:25565",
                new String[]{"x2 RES", "UNLIMITED", "MAX 3", "EVENTS", "FAST SMELT"}
        ));
    }

    private final MultiplayerServerListPinger pinger = new MultiplayerServerListPinger();

    public RustServerBrowserScreen() {
        super(new LiteralText("SpaceRust Main Menu"));
    }

    @Override
    protected void init() {
        super.init();
        refreshServers();
    }

    public void refreshServers() {
        for (ServerEntry s : SERVERS) {
            s.isPinging = true;
            s.isOnline = false;
            s.statusText = "Опрос...";
            try {
                this.pinger.add(s.serverInfo, s::updateStatsFromInfo);
            } catch (UnknownHostException ignored) {
                s.isPinging = false;
                s.isOnline = false;
                s.statusText = "ОФФЛАЙН";
                s.pingMs = -1;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        try {
            this.pinger.tick();
        } catch (Exception ignored) {}
    }

    @Override
    public void removed() {
        super.removed();
        this.pinger.cancel();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int screenW = this.width;
        int screenH = this.height;

        // 1. BACKGROUND: Deep gunmetal dark slate
        DrawableHelper.fill(matrices, 0, 0, screenW, screenH, 0xFF0c0e12);

        // Header and Footer bars
        DrawableHelper.fill(matrices, 0, 0, screenW, 36, 0xEE12161f);
        DrawableHelper.fill(matrices, 0, screenH - 38, screenW, screenH, 0xEE12161f);

        // Top line
        DrawableHelper.fill(matrices, 0, 36, screenW, 37, 0x44e0462c);
        // Bottom line
        DrawableHelper.fill(matrices, 0, screenH - 38, screenW, screenH - 37, 0x44e0462c);

        // 2. HEADER
        this.textRenderer.draw(matrices, "SPACE", 18, 14, 0xFFFFFFFF);
        this.textRenderer.draw(matrices, "RUST", 54, 14, 0xFFe0462c);
        this.textRenderer.draw(matrices, "• RUST SURVIVAL ENGINE", 92, 14, 0x66FFFFFF);

        // REAL Global Online (Only 1 server: shows real online)
        ServerEntry server = SERVERS.get(0);
        int realOnline = server.isOnline ? server.currentPlayers : 0;
        int statusDotColor = server.isOnline ? 0xFF5ec739 : 0xFFe53935;
        String onlineText = server.isOnline ? (realOnline + " ОНЛАЙН") : "СЕРВЕР ВЫКЛЮЧЕН";
        int onlineW = this.textRenderer.getWidth(onlineText);
        int dotX = screenW - onlineW - 32;

        DrawableHelper.fill(matrices, dotX, 16, dotX + 5, 21, statusDotColor);
        this.textRenderer.draw(matrices, onlineText, dotX + 10, 14, statusDotColor);

        // 3. TAB HEADER
        int tabX = 18;
        int tabY = 48;
        String tabTitle = "СПИСОК СЕРВЕРОВ (1)";
        int tabW = this.textRenderer.getWidth(tabTitle) + 14;
        DrawableHelper.fill(matrices, tabX, tabY, tabX + tabW, tabY + 16, 0xFFe0462c);
        this.textRenderer.draw(matrices, tabTitle, tabX + 7, tabY + 4, 0xFFFFFFFF);

        // 4. SERVER CARD
        int cardY = 72;
        int cardH = 54;
        int cardW = screenW - 36;
        int cardX = 18;

        boolean isHovered = mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH;

        // Card background
        int cardBg = isHovered ? 0xDD1c2330 : 0xAA141922;
        DrawableHelper.fill(matrices, cardX, cardY, cardX + cardW, cardY + cardH, cardBg);

        // Left indicator: Green if online, Red if offline
        int leftBorderColor = server.isOnline ? 0xFF5ec739 : 0xFFe0462c;
        DrawableHelper.fill(matrices, cardX, cardY, cardX + 3, cardY + cardH, leftBorderColor);

        // Server title
        this.textRenderer.draw(matrices, server.name, cardX + 12, cardY + 8, 0xFFFFFFFF);

        // Subtitle: Map & IP
        String sub = server.map + " • " + server.region + " • " + server.ip;
        this.textRenderer.draw(matrices, sub, cardX + 12, cardY + 23, 0x77FFFFFF);

        // Tags
        int tagX = cardX + 12;
        for (String tag : server.tags) {
            this.textRenderer.draw(matrices, "[" + tag + "]", tagX, cardY + 37, 0xFFe0462c);
            tagX += this.textRenderer.getWidth("[" + tag + "]") + 6;
        }

        // Status Bar & Ping
        int barW = 120;
        int barH = 5;
        int barX = cardX + cardW - 260;
        int barY = cardY + 24;

        DrawableHelper.fill(matrices, barX, barY, barX + barW, barY + barH, 0x55000000);
        if (server.isOnline && server.maxPlayers > 0) {
            float ratio = (float) server.currentPlayers / (float) server.maxPlayers;
            int fillW = (int) (barW * Math.min(1.0f, ratio));
            if (fillW > 0) {
                DrawableHelper.fill(matrices, barX, barY, barX + fillW, barY + barH, 0xFF5ec739);
            }
        }

        // Real status text (e.g. "0 / 20" or "ОФФЛАЙН")
        int statusTextColor = server.isOnline ? 0xFFFFFFFF : 0xFFe53935;
        this.textRenderer.draw(matrices, server.statusText, barX, barY - 11, statusTextColor);

        // Real ping text
        String pingStr = server.isOnline ? (server.pingMs + " ms") : "-- ms";
        int pingColor = server.isOnline ? 0xFF5ec739 : 0xFF888888;
        int pingStrW = this.textRenderer.getWidth(pingStr);
        this.textRenderer.draw(matrices, pingStr, barX + barW - pingStrW, barY - 11, pingColor);

        // Connect button
        int btnW = 110;
        int btnH = 30;
        int btnX = cardX + cardW - btnW - 12;
        int btnY = cardY + 12;

        boolean btnHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        int btnBg = btnHover ? 0xFFff5733 : (server.isOnline ? 0xFFe0462c : 0xFF2a3242);
        DrawableHelper.fill(matrices, btnX, btnY, btnX + btnW, btnY + btnH, btnBg);

        String btnText = "ИГРАТЬ";
        int tw = this.textRenderer.getWidth(btnText);
        this.textRenderer.draw(matrices, btnText, btnX + (btnW - tw) / 2, btnY + 11, 0xFFFFFFFF);

        // 5. BOTTOM NAVIGATION BAR (No singleplayer button, nicely spaced without overlaps)
        int botY = screenH - 28;
        int bWidth = 130;
        int bHeight = 20;
        int gap = 12;
        int totalW = (bWidth * 4) + (gap * 3);
        int startX = Math.max(18, (screenW - totalW) / 2);

        // 1. НАСТРОЙКИ
        drawBottomBtn(matrices, mouseX, mouseY, startX, botY, bWidth, bHeight, "НАСТРОЙКИ");
        // 2. ПОДКЛЮЧЕНИЕ ПО IP
        drawBottomBtn(matrices, mouseX, mouseY, startX + (bWidth + gap), botY, bWidth, bHeight, "ПОДКЛЮЧЕНИЕ ПО IP");
        // 3. ОБНОВИТЬ СЕРВЕР
        drawBottomBtn(matrices, mouseX, mouseY, startX + (bWidth + gap) * 2, botY, bWidth, bHeight, "ОБНОВИТЬ СЕРВЕР");
        // 4. ВЫХОД ИЗ ИГРЫ
        drawBottomBtn(matrices, mouseX, mouseY, startX + (bWidth + gap) * 3, botY, bWidth, bHeight, "ВЫХОД ИЗ ИГРЫ");

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawBottomBtn(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, int h, String text) {
        boolean hov = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        DrawableHelper.fill(matrices, x, y, x + w, y + h, hov ? 0x66FFFFFF : 0x221a202c);
        // Border
        DrawableHelper.fill(matrices, x, y, x + w, y + 1, hov ? 0xFFe0462c : 0x33FFFFFF);
        DrawableHelper.fill(matrices, x, y + h - 1, x + w, y + h, hov ? 0xFFe0462c : 0x33FFFFFF);
        DrawableHelper.fill(matrices, x, y, x + 1, y + h, hov ? 0xFFe0462c : 0x33FFFFFF);
        DrawableHelper.fill(matrices, x + w - 1, y, x + w, y + h, hov ? 0xFFe0462c : 0x33FFFFFF);

        int tw = this.textRenderer.getWidth(text);
        this.textRenderer.draw(matrices, text, x + (w - tw) / 2, y + 6, hov ? 0xFFFFFFFF : 0xAAFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int screenW = this.width;
        int screenH = this.height;

        // Server Connect Click
        int cardY = 72;
        int cardH = 54;
        int cardW = screenW - 36;
        int cardX = 18;

        ServerEntry server = SERVERS.get(0);
        int btnW = 110;
        int btnH = 30;
        int btnX = cardX + cardW - btnW - 12;
        int btnY = cardY + 12;

        if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            connectToServer(server.name, server.ip);
            return true;
        }

        // Bottom Button clicks
        int botY = screenH - 28;
        int bWidth = 130;
        int gap = 12;
        int totalW = (bWidth * 4) + (gap * 3);
        int startX = Math.max(18, (screenW - totalW) / 2);

        // 1. Settings
        if (mouseX >= startX && mouseX <= startX + bWidth && mouseY >= botY && mouseY <= botY + 20) {
            if (this.client != null) {
                this.client.openScreen(new OptionsScreen(this, this.client.options));
            }
            return true;
        }

        // 2. Direct Connect
        int directX = startX + (bWidth + gap);
        if (mouseX >= directX && mouseX <= directX + bWidth && mouseY >= botY && mouseY <= botY + 20) {
            if (this.client != null) {
                this.client.openScreen(new DirectConnectScreen(this, (confirmed) -> {
                    if (confirmed) {
                        // Connection already handled by DirectConnectScreen
                    } else {
                        this.client.openScreen(this);
                    }
                }, new ServerInfo("Direct Connect", "127.0.0.1", false)));
            }
            return true;
        }

        // 3. Refresh Server
        int refreshX = startX + (bWidth + gap) * 2;
        if (mouseX >= refreshX && mouseX <= refreshX + bWidth && mouseY >= botY && mouseY <= botY + 20) {
            refreshServers();
            return true;
        }

        // 4. Quit Game
        int quitX = startX + (bWidth + gap) * 3;
        if (mouseX >= quitX && mouseX <= quitX + bWidth && mouseY >= botY && mouseY <= botY + 20) {
            if (this.client != null) {
                this.client.scheduleStop();
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void connectToServer(String name, String ip) {
        if (this.client == null) return;
        System.out.println("[SpaceRust] Connecting to server: " + name + " (" + ip + ")");
        ServerInfo info = new ServerInfo(name, ip, false);
        this.client.openScreen(new ConnectScreen(this, this.client, info));
    }
}
