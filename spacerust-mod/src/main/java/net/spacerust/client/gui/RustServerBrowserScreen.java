package net.spacerust.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DirectConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.world.Difficulty;
import org.lwjgl.glfw.GLFW;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RustServerBrowserScreen extends Screen {

    // Top Navigation Tabs matching Rust UI
    public enum MenuTab {
        HOME("⌂  ГЛАВНАЯ"),
        PLAY("▶  ИГРАТЬ"),
        SETTINGS("⚙  НАСТРОЙКИ");

        public final String title;
        MenuTab(String title) { this.title = title; }
    }

    // Settings Categories matching Screenshot 3
    public enum SettingsCategory {
        GAME("❖  ИГРА"),
        AUDIO("🔊  АУДИО"),
        DISPLAY("🖥  ЭКРАН"),
        GRAPHICS("⚡  ГРАФИКА"),
        CONTROLS("🎮  УПРАВЛЕНИЕ"),
        LANGUAGE("🌐  ЯЗЫК");

        public final String title;
        SettingsCategory(String title) { this.title = title; }
    }

    public static class ServerEntry {
        public final String name;
        public final String map;
        public final String region;
        public final String ip;
        public final String[] tags;
        public final ServerInfo serverInfo;
        public boolean isFavorite = true;

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
    }

    public static final List<ServerEntry> SERVERS = new ArrayList<>();
    static {
        SERVERS.add(new ServerEntry(
                "[EU/RU] SpaceRust - UNLIMITED X2",
                "Procedural Map 3800",
                "RU / EU",
                "127.0.0.1:25565",
                new String[]{"ВАНИЛЛА", "МЕСЯЧНЫЙ ВАЙП", "MAX 3", "x2 RES", "FAST SMELT"}
        ));
    }

    public static MenuTab currentTab = MenuTab.HOME;
    public static SettingsCategory selectedSettingCategory = SettingsCategory.GAME;

    // Search query in Play tab and Settings tab
    private String serverSearchQuery = "";
    private String settingsSearchQuery = "";
    private boolean isSearchFocused = false;

    // Active hovered setting for description pane (Screenshot 3)
    private String hoveredSettingTitle = "ПОЛЕ ЗРЕНИЯ";
    private String hoveredSettingDesc = "Угол обзора камеры в игре (FOV). Более высокий угол дает широкий обзор окружения.";

    // Active slider being dragged
    private String activeSliderId = null;

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
            s.statusText = "Опрос...";
            pingServerAsync(s);
        }
    }

    private static void pingServerAsync(ServerEntry s) {
        new Thread(() -> {
            String host = "127.0.0.1";
            int port = 25565;
            try {
                if (s.ip.contains(":")) {
                    String[] parts = s.ip.split(":");
                    host = parts[0];
                    port = Integer.parseInt(parts[1]);
                } else {
                    host = s.ip;
                }

                long start = System.currentTimeMillis();
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, port), 2000);
                    socket.setSoTimeout(2000);
                    long ping = System.currentTimeMillis() - start;

                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    DataInputStream in = new DataInputStream(socket.getInputStream());

                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    DataOutputStream handshake = new DataOutputStream(b);
                    handshake.writeByte(0x00);
                    writeVarInt(handshake, 754);
                    writeString(handshake, host);
                    handshake.writeShort(port);
                    writeVarInt(handshake, 1);

                    byte[] bytes = b.toByteArray();
                    writeVarInt(out, bytes.length);
                    out.write(bytes);

                    out.writeByte(0x01);
                    out.writeByte(0x00);

                    readVarInt(in);
                    int id = readVarInt(in);
                    if (id == 0x00) {
                        String json = readString(in);
                        int pOnline = 0;
                        int pMax = 350;

                        int idxOnline = json.indexOf("\"online\":");
                        if (idxOnline != -1) {
                            int end = json.indexOf(",", idxOnline);
                            if (end == -1) end = json.indexOf("}", idxOnline);
                            if (end != -1) {
                                pOnline = Integer.parseInt(json.substring(idxOnline + 9, end).trim());
                            }
                        }

                        int idxMax = json.indexOf("\"max\":");
                        if (idxMax != -1) {
                            int end = json.indexOf(",", idxMax);
                            if (end == -1) end = json.indexOf("}", idxMax);
                            if (end != -1) {
                                pMax = Integer.parseInt(json.substring(idxMax + 6, end).trim());
                            }
                        }

                        s.isOnline = true;
                        s.isPinging = false;
                        s.currentPlayers = pOnline;
                        s.maxPlayers = pMax;
                        s.pingMs = Math.max(1, ping);
                        s.statusText = pOnline + " / " + pMax;
                    }
                }
            } catch (Exception e) {
                s.isOnline = false;
                s.isPinging = false;
                s.statusText = "ОФФЛАЙН";
                s.pingMs = -1;
            }
        }, "SpaceRust-Ping").start();
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) throw new RuntimeException("VarInt is too big");
        } while ((read & 0b10000000) != 0);
        return result;
    }

    private static void writeString(DataOutputStream out, String string) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int screenW = this.width;
        int screenH = this.height;

        // Base Dark Slate Background
        DrawableHelper.fill(matrices, 0, 0, screenW, screenH, 0xFF0c0e12);

        // ==========================================
        // 1. TOP HEADER BAR (Present on all screens)
        // ==========================================
        int topH = 34;
        DrawableHelper.fill(matrices, 0, 0, screenW, topH, 0xEE12151B);
        DrawableHelper.fill(matrices, 0, topH, screenW, topH + 1, 0x22FFFFFF);

        // Logo on Top-Left: Rust Orange Badge + "SPACERUST"
        int logoSize = 18;
        int logoX = 14;
        int logoY = 8;
        DrawableHelper.fill(matrices, logoX, logoY, logoX + logoSize, logoY + logoSize, 0xFFE0462C);
        DrawableHelper.fill(matrices, logoX + 5, logoY + 5, logoX + logoSize - 5, logoY + logoSize - 5, 0xFF12151B);

        this.textRenderer.drawWithShadow(matrices, "SPACE", logoX + logoSize + 8, 13, 0xFFFFFFFF);
        this.textRenderer.drawWithShadow(matrices, "RUST", logoX + logoSize + 46, 13, 0xFFE0462C);

        // Tabs in Top-Center: ГЛАВНАЯ and ИГРАТЬ
        int tabW = 86;
        int tabH = 22;
        int tabY = 6;
        int centerX = screenW / 2;

        int homeTabX = centerX - tabW - 4;
        int playTabX = centerX + 4;

        // "ГЛАВНАЯ" tab
        drawTopTab(matrices, mouseX, mouseY, homeTabX, tabY, tabW, tabH, "⌂  ГЛАВНАЯ", currentTab == MenuTab.HOME);

        // "ИГРАТЬ" tab
        drawTopTab(matrices, mouseX, mouseY, playTabX, tabY, tabW, tabH, "▶  ИГРАТЬ", currentTab == MenuTab.PLAY);

        // Top-Right: "НАСТРОЙКИ" and "ВЫЙТИ" (No avatar as requested!)
        int setW = 95;
        int exitW = 75;
        int setX = screenW - setW - exitW - 20;
        int exitX = screenW - exitW - 14;

        drawTopTab(matrices, mouseX, mouseY, setX, tabY, setW, tabH, "⚙  НАСТРОЙКИ", currentTab == MenuTab.SETTINGS);

        // Exit Button (turns soft red on hover)
        boolean exitHov = mouseX >= exitX && mouseX <= exitX + exitW && mouseY >= tabY && mouseY <= tabY + tabH;
        int exitBg = exitHov ? 0xCCB71C1C : 0x44202530;
        DrawableHelper.fill(matrices, exitX, tabY, exitX + exitW, tabY + tabH, exitBg);
        int exitTxtW = this.textRenderer.getWidth("➔  ВЫЙТИ");
        this.textRenderer.drawWithShadow(matrices, "➔  ВЫЙТИ", exitX + (exitW - exitTxtW) / 2, tabY + 7, exitHov ? 0xFFFFFFFF : 0xCCFFFFFF);

        // ==========================================
        // 2. ACTIVE VIEW ROUTING
        // ==========================================
        if (currentTab == MenuTab.HOME) {
            renderHomeTab(matrices, mouseX, mouseY, screenW, screenH, topH);
        } else if (currentTab == MenuTab.PLAY) {
            renderPlayTab(matrices, mouseX, mouseY, screenW, screenH, topH);
        } else if (currentTab == MenuTab.SETTINGS) {
            renderSettingsTab(matrices, mouseX, mouseY, screenW, screenH, topH);
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawTopTab(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, int h, String title, boolean active) {
        boolean hov = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int bg = active ? 0xFF962D1C : (hov ? 0x33FFFFFF : 0x22181C22);
        DrawableHelper.fill(matrices, x, y, x + w, y + h, bg);
        if (active) {
            DrawableHelper.fill(matrices, x, y + h - 2, x + w, y + h, 0xFFFF5733);
        }
        int tw = this.textRenderer.getWidth(title);
        int textColor = active ? 0xFFFFFFFF : (hov ? 0xFFE0E5EC : 0x88AAAAAA);
        this.textRenderer.drawWithShadow(matrices, title, x + (w - tw) / 2, y + 7, textColor);
    }

    // =========================================================================
    // VIEW 1: ГЛАВНАЯ (Screenshot 2 - Clean Cinematic Rust Home)
    // =========================================================================
    private void renderHomeTab(MatrixStack matrices, int mouseX, int mouseY, int screenW, int screenH, int topH) {
        // Atmospheric Twilight / Sunset Gradient Background
        DrawableHelper.fill(matrices, 0, topH, screenW, screenH, 0xFF0E1217);

        // Sun Radial Glow in upper left
        int sunX = (int) (screenW * 0.22f);
        int sunY = (int) (screenH * 0.38f);
        DrawableHelper.fill(matrices, sunX - 55, sunY - 55, sunX + 55, sunY + 55, 0x1AFFE0B2);
        DrawableHelper.fill(matrices, sunX - 40, sunY - 40, sunX + 40, sunY + 40, 0x33FFCC80);
        DrawableHelper.fill(matrices, sunX - 25, sunY - 25, sunX + 25, sunY + 25, 0x66FFA726);

        // Distant Industrial Silhouette (Oil Rig / Radar tower)
        int rigX = (int) (screenW * 0.72f);
        int rigY = (int) (screenH * 0.45f);
        DrawableHelper.fill(matrices, rigX, rigY, rigX + 80, rigY + 50, 0x55111419);
        DrawableHelper.fill(matrices, rigX + 15, rigY - 25, rigX + 65, rigY, 0x66111419);
        DrawableHelper.fill(matrices, rigX + 35, rigY - 50, rigX + 45, rigY - 25, 0x66111419);

        // Hero Card on Left / Center
        int heroX = 40;
        int heroY = (int) (screenH * 0.42f);

        this.textRenderer.drawWithShadow(matrices, "SPACERUST SURVIVAL ENGINE", heroX, heroY, 0xFFE0462C);
        this.textRenderer.drawWithShadow(matrices, "КЛИЕНТ ГОТОВ К ВЫЖИВАНИЮ", heroX, heroY + 16, 0xFFFFFFFF);

        String desc = "Официальный сервер SpaceRust запущен на процедурной карте 3800. Исследуйте мир и стройте базы.";
        this.textRenderer.drawWithShadow(matrices, desc, heroX, heroY + 34, 0x88FFFFFF);

        // Server status indicator
        ServerEntry s = SERVERS.get(0);
        int dotColor = s.isOnline ? 0xFF5EC739 : 0xFFE53935;
        String statText = s.isOnline ? ("● СЕРВЕР ОНЛАЙН: " + s.currentPlayers + " / " + s.maxPlayers + " ИГРОКОВ") : "● СЕРВЕР ВЫКЛЮЧЕН (ОФФЛАЙН)";
        this.textRenderer.drawWithShadow(matrices, statText, heroX, heroY + 52, dotColor);

        // Action Buttons: [ ▶ БЫСТРАЯ ИГРА ] and [ ☰ СПИСОК СЕРВЕРОВ ]
        int btnW = 160;
        int btnH = 32;
        int btnY = heroY + 74;

        // Big Primary "НАЧАТЬ ИГРАТЬ" Button
        boolean hovPlay = mouseX >= heroX && mouseX <= heroX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        int playBg = hovPlay ? 0xFFFF5733 : 0xFFE0462C;
        DrawableHelper.fill(matrices, heroX, btnY, heroX + btnW, btnY + btnH, playBg);
        int ptw = this.textRenderer.getWidth("▶  НАЧАТЬ ИГРАТЬ");
        this.textRenderer.drawWithShadow(matrices, "▶  НАЧАТЬ ИГРАТЬ", heroX + (btnW - ptw) / 2, btnY + 12, 0xFFFFFFFF);

        // Secondary "СПИСОК СЕРВЕРОВ" Button
        int listBtnX = heroX + btnW + 12;
        boolean hovList = mouseX >= listBtnX && mouseX <= listBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        DrawableHelper.fill(matrices, listBtnX, btnY, listBtnX + btnW, btnY + btnH, hovList ? 0x44FFFFFF : 0x22181C24);
        DrawableHelper.fill(matrices, listBtnX, btnY, listBtnX + btnW, btnY + 1, 0x33FFFFFF);
        DrawableHelper.fill(matrices, listBtnX, btnY + btnH - 1, listBtnX + btnW, btnY + btnH, 0x33FFFFFF);
        DrawableHelper.fill(matrices, listBtnX, btnY, listBtnX + 1, btnY + btnH, 0x33FFFFFF);
        DrawableHelper.fill(matrices, listBtnX + btnW - 1, btnY, listBtnX + btnW, btnY + btnH, 0x33FFFFFF);
        int ltw = this.textRenderer.getWidth("☰  СПИСОК СЕРВЕРОВ");
        this.textRenderer.drawWithShadow(matrices, "☰  СПИСОК СЕРВЕРОВ", listBtnX + (btnW - ltw) / 2, btnY + 12, 0xFFFFFFFF);
    }

    // =========================================================================
    // VIEW 2: ИГРАТЬ (Screenshot 1 - Server Browser, clean full width table)
    // =========================================================================
    private void renderPlayTab(MatrixStack matrices, int mouseX, int mouseY, int screenW, int screenH, int topH) {
        // Sub-bar with Server Filters (ОФИЦИАЛЬНЫЕ, МОДИФИЦИРОВАННЫЕ, ИЗБРАННОЕ, ИСТОРИЯ) + Search
        int filterBarY = topH + 8;
        int filterH = 20;

        int fx = 18;
        // ОФИЦИАЛЬНЫЕ (Active green badge with live online)
        int onlineCount = SERVERS.get(0).isOnline ? SERVERS.get(0).currentPlayers : 0;
        String offStr = "★  ОФИЦИАЛЬНЫЕ  " + onlineCount;
        int offW = this.textRenderer.getWidth(offStr) + 14;
        DrawableHelper.fill(matrices, fx, filterBarY, fx + offW, filterBarY + filterH, 0xFF354823);
        this.textRenderer.drawWithShadow(matrices, offStr, fx + 7, filterBarY + 6, 0xFF8BC34A);
        fx += offW + 8;

        // МОДИФИЦИРОВАННЫЕ
        drawFilterPill(matrices, mouseX, mouseY, fx, filterBarY, "МОДИФИЦИРОВАННЫЕ");
        fx += this.textRenderer.getWidth("МОДИФИЦИРОВАННЫЕ") + 22;

        // ИЗБРАННОЕ
        drawFilterPill(matrices, mouseX, mouseY, fx, filterBarY, "♥  ИЗБРАННОЕ");
        fx += this.textRenderer.getWidth("♥  ИЗБРАННОЕ") + 22;

        // ИСТОРИЯ
        drawFilterPill(matrices, mouseX, mouseY, fx, filterBarY, "⏱  ИСТОРИЯ");

        // Search Bar on far right
        int searchW = 140;
        int searchX = screenW - searchW - 18;
        DrawableHelper.fill(matrices, searchX, filterBarY, searchX + searchW, filterBarY + filterH, 0xAA12151B);
        DrawableHelper.fill(matrices, searchX, filterBarY, searchX + searchW, filterBarY + 1, isSearchFocused ? 0xFFE0462C : 0x22FFFFFF);
        if (serverSearchQuery.isEmpty()) {
            this.textRenderer.drawWithShadow(matrices, "ПОИСК...", searchX + 8, filterBarY + 6, 0x55AAAAAA);
        } else {
            this.textRenderer.drawWithShadow(matrices, serverSearchQuery, searchX + 8, filterBarY + 6, 0xFFFFFFFF);
        }

        // ==========================================
        // SERVER TABLE (Full width, no left sidebar)
        // ==========================================
        int tableX = 18;
        int tableW = screenW - 36;
        int headerY = filterBarY + filterH + 10;
        int headerH = 16;

        // Header bar
        DrawableHelper.fill(matrices, tableX, headerY, tableX + tableW, headerY + headerH, 0xEE101318);
        this.textRenderer.drawWithShadow(matrices, "♥", tableX + 8, headerY + 4, 0x88AAAAAA);
        this.textRenderer.drawWithShadow(matrices, "НАЗВАНИЕ СЕРВЕРА", tableX + 32, headerY + 4, 0x88AAAAAA);
        this.textRenderer.drawWithShadow(matrices, "РЕЖИМ", tableX + (int)(tableW * 0.62f), headerY + 4, 0x88AAAAAA);
        this.textRenderer.drawWithShadow(matrices, "ИГРОКИ", tableX + (int)(tableW * 0.77f), headerY + 4, 0x88AAAAAA);
        this.textRenderer.drawWithShadow(matrices, "ПИНГ", tableX + (int)(tableW * 0.88f), headerY + 4, 0x88AAAAAA);

        // Server Row (Unlimited X2)
        ServerEntry s = SERVERS.get(0);
        int rowY = headerY + headerH + 6;
        int rowH = 46;

        boolean isHover = mouseX >= tableX && mouseX <= tableX + tableW && mouseY >= rowY && mouseY <= rowY + rowH;
        DrawableHelper.fill(matrices, tableX, rowY, tableX + tableW, rowY + rowH, isHover ? 0xDD1B202A : 0xAA13171F);

        // Left accent border: green if online
        DrawableHelper.fill(matrices, tableX, rowY, tableX + 3, rowY + rowH, s.isOnline ? 0xFF5EC739 : 0xFFE0462C);

        // Favorite Heart icon
        String heart = s.isFavorite ? "♥" : "♡";
        int heartColor = s.isFavorite ? 0xFFE0462C : 0x77AAAAAA;
        this.textRenderer.drawWithShadow(matrices, heart, tableX + 8, rowY + 12, heartColor);

        // Server Name & Subtitle
        this.textRenderer.drawWithShadow(matrices, s.name, tableX + 32, rowY + 8, 0xFFFFFFFF);
        String sub = s.map + " • " + s.region + " • " + s.ip;
        this.textRenderer.drawWithShadow(matrices, sub, tableX + 32, rowY + 22, 0x77AAAAAA);

        // Tags
        int tagX = tableX + 32;
        int tagY = rowY + 34;
        for (String tag : s.tags) {
            this.textRenderer.drawWithShadow(matrices, "[" + tag + "]", tagX, tagY, 0xFFE0462C);
            tagX += this.textRenderer.getWidth("[" + tag + "]") + 5;
        }

        // Mode Pill (Exact match to screenshot 1: ВАНИЛЛА in green pill)
        int modeX = tableX + (int)(tableW * 0.62f);
        int modeW = 54;
        int modeH = 14;
        DrawableHelper.fill(matrices, modeX, rowY + 12, modeX + modeW, rowY + 12 + modeH, 0xFF354823);
        this.textRenderer.drawWithShadow(matrices, "ВАНИЛЛА", modeX + 6, rowY + 15, 0xFF8BC34A);

        // Players Count & Progress bar
        int playX = tableX + (int)(tableW * 0.77f);
        String countStr = s.isOnline ? (s.currentPlayers + " / " + s.maxPlayers) : "ОФФЛАЙН";
        int countColor = s.isOnline ? 0xFFFFFFFF : 0xFFE53935;
        this.textRenderer.drawWithShadow(matrices, countStr, playX, rowY + 10, countColor);

        // Mini player bar
        int pBarW = 60;
        int pBarH = 3;
        DrawableHelper.fill(matrices, playX, rowY + 24, playX + pBarW, rowY + 24 + pBarH, 0x44000000);
        if (s.isOnline && s.maxPlayers > 0) {
            int fill = (int) (pBarW * Math.min(1.0f, (float) s.currentPlayers / (float) s.maxPlayers));
            DrawableHelper.fill(matrices, playX, rowY + 24, playX + fill, rowY + 24 + pBarH, 0xFF5EC739);
        }

        // Ping Text
        int pingX = tableX + (int)(tableW * 0.88f);
        String pingStr = s.isOnline ? (s.pingMs + " ms") : "-- ms";
        int pCol = s.isOnline ? 0xFF5EC739 : 0x77AAAAAA;
        this.textRenderer.drawWithShadow(matrices, pingStr, pingX, rowY + 14, pCol);

        // Join Button on far right
        int btnW = 80;
        int btnH = 26;
        int btnX = tableX + tableW - btnW - 10;
        int btnY = rowY + 10;
        boolean hovBtn = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        int bBg = hovBtn ? 0xFFFF5733 : (s.isOnline ? 0xFFE0462C : 0xFF252B36);
        DrawableHelper.fill(matrices, btnX, btnY, btnX + btnW, btnY + btnH, bBg);
        int btw = this.textRenderer.getWidth("ИГРАТЬ");
        this.textRenderer.drawWithShadow(matrices, "ИГРАТЬ", btnX + (btnW - btw) / 2, btnY + 9, 0xFFFFFFFF);

        // Bottom Actions: Direct Connect & Refresh
        int bY = screenH - 26;
        drawBottomActionBtn(matrices, mouseX, mouseY, 18, bY, 160, 18, "⊕  ПОДКЛЮЧЕНИЕ ПО IP");
        drawBottomActionBtn(matrices, mouseX, mouseY, 186, bY, 140, 18, "⟳  ОБНОВИТЬ СПИСОК");
    }

    private void drawFilterPill(MatrixStack matrices, int mouseX, int mouseY, int x, int y, String title) {
        int w = this.textRenderer.getWidth(title) + 14;
        boolean hov = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 20;
        DrawableHelper.fill(matrices, x, y, x + w, y + 20, hov ? 0x33FFFFFF : 0x1AFFFFFF);
        this.textRenderer.drawWithShadow(matrices, title, x + 7, y + 6, hov ? 0xFFFFFFFF : 0x88AAAAAA);
    }

    private void drawBottomActionBtn(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, int h, String title) {
        boolean hov = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        DrawableHelper.fill(matrices, x, y, x + w, y + h, hov ? 0x44FFFFFF : 0x22181C22);
        DrawableHelper.fill(matrices, x, y, x + w, y + 1, 0x33FFFFFF);
        DrawableHelper.fill(matrices, x, y + h - 1, x + w, y + h, 0x33FFFFFF);
        int tw = this.textRenderer.getWidth(title);
        this.textRenderer.drawWithShadow(matrices, title, x + (w - tw) / 2, y + 5, hov ? 0xFFFFFFFF : 0x88AAAAAA);
    }

    // =========================================================================
    // VIEW 3: НАСТРОЙКИ (Screenshot 3 - Authentic Rust Settings)
    // =========================================================================
    private void renderSettingsTab(MatrixStack matrices, int mouseX, int mouseY, int screenW, int screenH, int topH) {
        // Left Column: Categories list (Screenshot 3)
        int catW = 100;
        int catX = 14;
        int startY = topH + 12;

        // Search Bar in Settings
        int sBoxH = 18;
        DrawableHelper.fill(matrices, catX, startY, catX + catW, startY + sBoxH, 0xAA12151B);
        DrawableHelper.fill(matrices, catX, startY, catX + catW, startY + 1, 0x22FFFFFF);
        this.textRenderer.drawWithShadow(matrices, "ПОИСК...", catX + 6, startY + 5, 0x55AAAAAA);

        int curCatY = startY + sBoxH + 8;
        int itemH = 18;

        for (SettingsCategory cat : SettingsCategory.values()) {
            boolean active = cat == selectedSettingCategory;
            boolean hov = mouseX >= catX && mouseX <= catX + catW && mouseY >= curCatY && mouseY <= curCatY + itemH;

            // RUST OLIVE GREEN ACTIVE BAR (#596c34 / 0xFF5A7232 from screenshot 3)
            int bg = active ? 0xFF5A7232 : (hov ? 0x33FFFFFF : 0x1A14171E);
            DrawableHelper.fill(matrices, catX, curCatY, catX + catW, curCatY + itemH, bg);
            int txtColor = active ? 0xFFFFFFFF : (hov ? 0xFFE0E5EC : 0x88AAAAAA);
            this.textRenderer.drawWithShadow(matrices, cat.title, catX + 8, curCatY + 5, txtColor);

            curCatY += itemH + 3;
        }

        // Center Column: Options Rows
        int centerColX = catX + catW + 16;
        int descColW = 200;
        int centerColW = screenW - centerColX - descColW - 20;

        // Title of selected category: e.g. "❖  ИГРА"
        this.textRenderer.drawWithShadow(matrices, selectedSettingCategory.title, centerColX, startY + 2, 0xFFFFFFFF);

        int optY = startY + 24;
        int rowH = 22;

        if (selectedSettingCategory == SettingsCategory.GAME) {
            // 1. FOV (Поле зрения)
            drawSliderRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ПОЛЕ ЗРЕНИЯ",
                    String.valueOf((int) this.client.options.fov), (float) (this.client.options.fov - 30.0) / 80.0f, "fov",
                    "Настройка угла обзора камеры в игре (FOV). Более высокий угол дает широкий обзор.");
            optY += rowH;

            // 2. View Bobbing (Покачивание камеры)
            drawToggleRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ПОКАЧИВАНИЕ КАМЕРЫ",
                    this.client.options.bobView, "bobView",
                    "Включает или выключает реалистичное покачивание рук и предметов при ходьбе.");
            optY += rowH;

            // 3. Auto-jump (Автопрыжок)
            drawToggleRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "АВТОПРЫЖОК",
                    this.client.options.autoJump, "autoJump",
                    "Автоматический прыжок при подходе к блокам высотой в 1 блок.");
            optY += rowH;

            // 4. Difficulty (Сложность)
            drawCycleRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "СЛОЖНОСТЬ",
                    this.client.options.difficulty.getName(), "difficulty",
                    "Уровень сложности игрового мира и урон от окружения.");
            optY += rowH;

            // 5. GUI Scale (Fixed!)
            drawInfoRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "МАСШТАБ ИНТЕРФЕЙСА",
                    "ФИКСИРОВАН 2 (RUST UI)", "Настройка заблокирована. Размер интерфейса зафиксирован для идеального отображения Rust UI.");

        } else if (selectedSettingCategory == SettingsCategory.AUDIO) {
            // Master Volume
            drawVolumeSlider(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ОБЩАЯ ГРОМКОСТЬ", SoundCategory.MASTER,
                    "Регулировка главной громкости всех звуков в игре.");
            optY += rowH;

            // Music
            drawVolumeSlider(matrices, mouseX, mouseY, centerColX, optY, centerColW, "МУЗЫКА", SoundCategory.MUSIC,
                    "Громкость фоновой музыки в игре.");
            optY += rowH;

            // Blocks
            drawVolumeSlider(matrices, mouseX, mouseY, centerColX, optY, centerColW, "БЛОКИ И ШАГИ", SoundCategory.BLOCKS,
                    "Громкость шагов, ломания и установки строительных блоков.");
            optY += rowH;

            // Players
            drawVolumeSlider(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ИГРОКИ", SoundCategory.PLAYERS,
                    "Звуки других игроков, ударов и перестрелок.");
            optY += rowH;

            // Ambient
            drawVolumeSlider(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ОКРУЖАЮЩАЯ СРЕДА", SoundCategory.AMBIENT,
                    "Звуки ветра, природы и ночной атмосферы Rust.");

        } else if (selectedSettingCategory == SettingsCategory.DISPLAY || selectedSettingCategory == SettingsCategory.GRAPHICS) {
            // Fullscreen
            drawToggleRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ПОЛНОЭКРАННЫЙ РЕЖИМ",
                    this.client.options.fullscreen, "fullscreen",
                    "Полноэкранный режим работы дисплея без рамок Windows.");
            optY += rowH;

            // VSync
            drawToggleRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ВЕРТИКАЛЬНАЯ СИНХРОНИЗАЦИЯ (VSync)",
                    this.client.options.enableVsync, "vsync",
                    "Синхронизация частоты кадров с герцовкой вашего монитора.");
            optY += rowH;

            // Max FPS
            int fps = this.client.options.maxFps;
            String fpsStr = fps >= 260 ? "МАКСИМУМ (БЕЗ ОГРАНИЧЕНИЙ)" : (fps + " FPS");
            drawSliderRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ЧАСТОТА КАДРОВ (FPS)",
                    fpsStr, (float) (fps - 10) / 250.0f, "maxFps",
                    "Максимальный лимит кадров в секунду (FPS) для экономии видеокарты.");
            optY += rowH;

            // Render Distance
            int dist = this.client.options.viewDistance;
            drawSliderRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ДАЛЬНОСТЬ ПРОРИСОВКИ",
                    dist + " ЧАНКОВ", (float) (dist - 2) / 30.0f, "viewDistance",
                    "Дальность видимости мира в чанках. Чем выше, тем дальше видны постройки и враги.");
            optY += rowH;

            // Graphics mode
            drawCycleRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ГРАФИКА",
                    this.client.options.graphicsMode == GraphicsMode.FAST ? "БЫСТРО" : "ДЕТАЛЬНО", "graphicsMode",
                    "Качество отрисовки листьев, прозрачности и частиц мира.");

        } else if (selectedSettingCategory == SettingsCategory.CONTROLS) {
            // Mouse Sensitivity
            int sens = (int) (this.client.options.mouseSensitivity * 200);
            drawSliderRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ЧУВСТВИТЕЛЬНОСТЬ МЫШИ",
                    sens + "%", (float) this.client.options.mouseSensitivity, "mouseSensitivity",
                    "Скорость поворота прицела и камеры при движении мыши.");
            optY += rowH;

            // Invert Mouse
            drawToggleRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ИНВЕРСИЯ МЫШИ",
                    this.client.options.invertYMouse, "invertYMouse",
                    "Инвертирует вертикальную ось движения мыши.");
            optY += rowH;

            // Keybindings button
            drawButtonRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "НАЗНАЧЕНИЕ КЛАВИШ...", "ОТКРЫТЬ КЛАВИШИ", "openControls",
                    "Настройка кнопок управления персонажем, стрельбы и движения.");

        } else if (selectedSettingCategory == SettingsCategory.LANGUAGE) {
            drawButtonRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ЯЗЫК ИНТЕРФЕЙСА...", "ВЫБРАТЬ ЯЗЫК", "openLanguage",
                    "Смена языка интерфейса Minecraft и мода.");
        }

        // ==========================================
        // Right Column: Setting Description Panel (Screenshot 3)
        // ==========================================
        int descX = screenW - descColW - 14;
        int descY = startY + 24;

        // Orange accent bar on left edge of description
        DrawableHelper.fill(matrices, descX, descY, descX + 2, descY + 120, 0xFFE0462C);

        this.textRenderer.drawWithShadow(matrices, hoveredSettingTitle, descX + 8, descY, 0xFFFFFFFF);

        List<String> dLines = wrapText(hoveredSettingDesc, descColW - 16);
        int dy = descY + 16;
        for (String line : dLines) {
            this.textRenderer.drawWithShadow(matrices, line, descX + 8, dy, 0x88AAAAAA);
            dy += 12;
        }
    }

    // Helper: Draw Slider Row in Rust style (Value on left, Track on right)
    private void drawSliderRow(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, String title, String valStr, float ratio, String id, String desc) {
        boolean rowHov = mouseY >= y && mouseY <= y + 18 && mouseX >= x && mouseX <= x + w;
        if (rowHov) {
            hoveredSettingTitle = title;
            hoveredSettingDesc = desc;
        }

        this.textRenderer.drawWithShadow(matrices, title, x, y + 4, 0xFFE0E5EC);

        int trackW = 100;
        int trackH = 4;
        int trackX = x + w - trackW;
        int trackY = y + 7;

        // Value text placed to the left of the slider track (Screenshot 3)
        int valW = this.textRenderer.getWidth(valStr);
        this.textRenderer.drawWithShadow(matrices, valStr, trackX - valW - 8, y + 4, 0xFFFFFFFF);

        // Track background
        DrawableHelper.fill(matrices, trackX, trackY, trackX + trackW, trackY + trackH, 0x55000000);

        // Filled active portion in Rust Orange
        int fill = (int) (trackW * Math.max(0.0f, Math.min(1.0f, ratio)));
        if (fill > 0) {
            DrawableHelper.fill(matrices, trackX, trackY, trackX + fill, trackY + trackH, 0xFFE0462C);
        }
    }

    private void drawVolumeSlider(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, String title, SoundCategory cat, String desc) {
        float vol = this.client.options.getSoundVolume(cat);
        int percent = (int) (vol * 100);
        drawSliderRow(matrices, mouseX, mouseY, x, y, w, title, percent + "%", vol, "vol_" + cat.name(), desc);
    }

    // Helper: Draw Toggle Row in Rust style (Rectangular ВКЛ / ВЫКЛ button)
    private void drawToggleRow(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, String title, boolean value, String id, String desc) {
        boolean rowHov = mouseY >= y && mouseY <= y + 18 && mouseX >= x && mouseX <= x + w;
        if (rowHov) {
            hoveredSettingTitle = title;
            hoveredSettingDesc = desc;
        }

        this.textRenderer.drawWithShadow(matrices, title, x, y + 4, 0xFFE0E5EC);

        int btnW = 60;
        int btnH = 16;
        int btnX = x + w - btnW;
        int btnY = y + 1;

        boolean hov = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        int bg = value ? (hov ? 0xFF445729 : 0xFF354823) : (hov ? 0x44FFFFFF : 0x22181C22);
        DrawableHelper.fill(matrices, btnX, btnY, btnX + btnW, btnY + btnH, bg);

        String txt = value ? "ВКЛ" : "ВЫКЛ";
        int tw = this.textRenderer.getWidth(txt);
        int col = value ? 0xFF8BC34A : 0x77AAAAAA;
        this.textRenderer.drawWithShadow(matrices, txt, btnX + (btnW - tw) / 2, btnY + 4, col);
    }

    // Helper: Draw Cycle Row (Option with choices)
    private void drawCycleRow(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, String title, String valStr, String id, String desc) {
        boolean rowHov = mouseY >= y && mouseY <= y + 18 && mouseX >= x && mouseX <= x + w;
        if (rowHov) {
            hoveredSettingTitle = title;
            hoveredSettingDesc = desc;
        }

        this.textRenderer.drawWithShadow(matrices, title, x, y + 4, 0xFFE0E5EC);

        int btnW = 80;
        int btnH = 16;
        int btnX = x + w - btnW;
        int btnY = y + 1;

        boolean hov = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        DrawableHelper.fill(matrices, btnX, btnY, btnX + btnW, btnY + btnH, hov ? 0x33FFFFFF : 0x22181C22);

        int tw = this.textRenderer.getWidth(valStr);
        this.textRenderer.drawWithShadow(matrices, valStr, btnX + (btnW - tw) / 2, btnY + 4, 0xFFFFFFFF);
    }

    // Helper: Draw Open Subscreen Button
    private void drawButtonRow(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, String title, String btnText, String id, String desc) {
        boolean rowHov = mouseY >= y && mouseY <= y + 18 && mouseX >= x && mouseX <= x + w;
        if (rowHov) {
            hoveredSettingTitle = title;
            hoveredSettingDesc = desc;
        }

        this.textRenderer.drawWithShadow(matrices, title, x, y + 4, 0xFFE0E5EC);

        int btnW = 90;
        int btnH = 16;
        int btnX = x + w - btnW;
        int btnY = y + 1;

        boolean hov = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        DrawableHelper.fill(matrices, btnX, btnY, btnX + btnW, btnY + btnH, hov ? 0xFF445729 : 0x22181C22);

        int tw = this.textRenderer.getWidth(btnText);
        this.textRenderer.drawWithShadow(matrices, btnText, btnX + (btnW - tw) / 2, btnY + 4, 0xFFFFFFFF);
    }

    private void drawInfoRow(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, String title, String valStr, String desc) {
        boolean rowHov = mouseY >= y && mouseY <= y + 18 && mouseX >= x && mouseX <= x + w;
        if (rowHov) {
            hoveredSettingTitle = title;
            hoveredSettingDesc = desc;
        }
        this.textRenderer.drawWithShadow(matrices, title, x, y + 4, 0xFFE0E5EC);
        int tw = this.textRenderer.getWidth(valStr);
        this.textRenderer.drawWithShadow(matrices, valStr, x + w - tw, y + 4, 0xFFE0462C);
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();

        for (String w : words) {
            if (this.textRenderer.getWidth(cur + " " + w) > maxWidth) {
                lines.add(cur.toString());
                cur = new StringBuilder(w);
            } else {
                if (cur.length() > 0) cur.append(" ");
                cur.append(w);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    // ==========================================
    // MOUSE & KEY INTERACTION
    // ==========================================
    private void playClickSound() {
        if (this.client != null && this.client.getSoundManager() != null) {
            this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int screenW = this.width;
        int screenH = this.height;

        int topH = 34;
        int tabW = 86;
        int tabH = 22;
        int tabY = 6;
        int centerX = screenW / 2;

        int homeTabX = centerX - tabW - 4;
        int playTabX = centerX + 4;

        // Click "ГЛАВНАЯ"
        if (mouseX >= homeTabX && mouseX <= homeTabX + tabW && mouseY >= tabY && mouseY <= tabY + tabH) {
            currentTab = MenuTab.HOME;
            playClickSound();
            return true;
        }

        // Click "ИГРАТЬ"
        if (mouseX >= playTabX && mouseX <= playTabX + tabW && mouseY >= tabY && mouseY <= tabY + tabH) {
            currentTab = MenuTab.PLAY;
            playClickSound();
            return true;
        }

        // Click "НАСТРОЙКИ"
        int setW = 95;
        int exitW = 75;
        int setX = screenW - setW - exitW - 20;
        int exitX = screenW - exitW - 14;

        if (mouseX >= setX && mouseX <= setX + setW && mouseY >= tabY && mouseY <= tabY + tabH) {
            currentTab = MenuTab.SETTINGS;
            playClickSound();
            return true;
        }

        // Click "ВЫЙТИ"
        if (mouseX >= exitX && mouseX <= exitX + exitW && mouseY >= tabY && mouseY <= tabY + tabH) {
            this.client.scheduleStop();
            return true;
        }

        // Interactions in HOME
        if (currentTab == MenuTab.HOME) {
            int heroX = 40;
            int heroY = (int) (screenH * 0.42f);
            int btnW = 160;
            int btnH = 32;
            int btnY = heroY + 74;

            // Click "НАЧАТЬ ИГРАТЬ"
            if (mouseX >= heroX && mouseX <= heroX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                connectToServer(SERVERS.get(0));
                return true;
            }

            // Click "СПИСОК СЕРВЕРОВ"
            int listBtnX = heroX + btnW + 12;
            if (mouseX >= listBtnX && mouseX <= listBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                currentTab = MenuTab.PLAY;
                playClickSound();
                return true;
            }
        }

        // Interactions in PLAY
        if (currentTab == MenuTab.PLAY) {
            int tableX = 18;
            int tableW = screenW - 36;
            int headerY = topH + 8 + 20 + 10;
            int rowY = headerY + 16 + 6;
            int rowH = 46;

            // Click Heart
            if (mouseX >= tableX + 8 && mouseX <= tableX + 24 && mouseY >= rowY + 8 && mouseY <= rowY + 30) {
                SERVERS.get(0).isFavorite = !SERVERS.get(0).isFavorite;
                playClickSound();
                return true;
            }

            // Click "ИГРАТЬ" connect
            int btnW = 80;
            int btnH = 26;
            int btnX = tableX + tableW - btnW - 10;
            int btnY = rowY + 10;
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                connectToServer(SERVERS.get(0));
                return true;
            }

            // Bottom Buttons: Direct Connect & Refresh
            int bY = screenH - 26;
            // Direct Connect
            if (mouseX >= 18 && mouseX <= 178 && mouseY >= bY && mouseY <= bY + 18) {
                this.client.openScreen(new DirectConnectScreen(this, (confirmed) -> {
                    if (confirmed) {
                        this.client.openScreen(this);
                    } else {
                        this.client.openScreen(this);
                    }
                }, new ServerInfo("Direct Connect", "127.0.0.1:25565", false)));
                return true;
            }

            // Refresh
            if (mouseX >= 186 && mouseX <= 326 && mouseY >= bY && mouseY <= bY + 18) {
                refreshServers();
                playClickSound();
                return true;
            }
        }

        // Interactions in SETTINGS
        if (currentTab == MenuTab.SETTINGS) {
            int catW = 100;
            int catX = 14;
            int startY = topH + 12;
            int curCatY = startY + 18 + 8;
            int itemH = 18;

            // Categories Click
            for (SettingsCategory cat : SettingsCategory.values()) {
                if (mouseX >= catX && mouseX <= catX + catW && mouseY >= curCatY && mouseY <= curCatY + itemH) {
                    selectedSettingCategory = cat;
                    playClickSound();
                    return true;
                }
                curCatY += itemH + 3;
            }

            // Check Controls and Sliders
            int centerColX = catX + catW + 16;
            int descColW = 200;
            int centerColW = screenW - centerColX - descColW - 20;
            int optY = startY + 24;
            int rowH = 22;

            if (selectedSettingCategory == SettingsCategory.GAME) {
                // FOV slider track click
                if (checkSliderClick(mouseX, mouseY, centerColX + centerColW - 100, optY + 2, 100, 14)) {
                    activeSliderId = "fov";
                    updateSliderFromMouse(mouseX, centerColX + centerColW - 100, 100, "fov");
                    return true;
                }
                optY += rowH;

                // View Bobbing Toggle
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 60, optY + 1, 60, 16)) {
                    this.client.options.bobView = !this.client.options.bobView;
                    saveOptions();
                    return true;
                }
                optY += rowH;

                // Auto Jump Toggle
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 60, optY + 1, 60, 16)) {
                    this.client.options.autoJump = !this.client.options.autoJump;
                    saveOptions();
                    return true;
                }
                optY += rowH;

                // Difficulty Cycle
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 80, optY + 1, 80, 16)) {
                    int nextId = (this.client.options.difficulty.getId() + 1) % 4;
                    this.client.options.difficulty = Difficulty.byOrdinal(nextId);
                    saveOptions();
                    return true;
                }

            } else if (selectedSettingCategory == SettingsCategory.AUDIO) {
                // Master Volume
                if (checkSliderClick(mouseX, mouseY, centerColX + centerColW - 100, optY + 2, 100, 14)) {
                    activeSliderId = "vol_MASTER";
                    updateSliderFromMouse(mouseX, centerColX + centerColW - 100, 100, "vol_MASTER");
                    return true;
                }
                optY += rowH;

                // Music
                if (checkSliderClick(mouseX, mouseY, centerColX + centerColW - 100, optY + 2, 100, 14)) {
                    activeSliderId = "vol_MUSIC";
                    updateSliderFromMouse(mouseX, centerColX + centerColW - 100, 100, "vol_MUSIC");
                    return true;
                }
                optY += rowH;

                // Blocks
                if (checkSliderClick(mouseX, mouseY, centerColX + centerColW - 100, optY + 2, 100, 14)) {
                    activeSliderId = "vol_BLOCKS";
                    updateSliderFromMouse(mouseX, centerColX + centerColW - 100, 100, "vol_BLOCKS");
                    return true;
                }
                optY += rowH;

                // Players
                if (checkSliderClick(mouseX, mouseY, centerColX + centerColW - 100, optY + 2, 100, 14)) {
                    activeSliderId = "vol_PLAYERS";
                    updateSliderFromMouse(mouseX, centerColX + centerColW - 100, 100, "vol_PLAYERS");
                    return true;
                }

            } else if (selectedSettingCategory == SettingsCategory.DISPLAY || selectedSettingCategory == SettingsCategory.GRAPHICS) {
                // Fullscreen toggle
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 60, optY + 1, 60, 16)) {
                    this.client.options.fullscreen = !this.client.options.fullscreen;
                    if (this.client.getWindow().isFullscreen() != this.client.options.fullscreen) {
                        this.client.getWindow().toggleFullscreen();
                    }
                    saveOptions();
                    return true;
                }
                optY += rowH;

                // VSync toggle
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 60, optY + 1, 60, 16)) {
                    this.client.options.enableVsync = !this.client.options.enableVsync;
                    this.client.getWindow().setVsync(this.client.options.enableVsync);
                    saveOptions();
                    return true;
                }
                optY += rowH;

                // Max FPS slider
                if (checkSliderClick(mouseX, mouseY, centerColX + centerColW - 100, optY + 2, 100, 14)) {
                    activeSliderId = "maxFps";
                    updateSliderFromMouse(mouseX, centerColX + centerColW - 100, 100, "maxFps");
                    return true;
                }
                optY += rowH;

                // Render Distance slider
                if (checkSliderClick(mouseX, mouseY, centerColX + centerColW - 100, optY + 2, 100, 14)) {
                    activeSliderId = "viewDistance";
                    updateSliderFromMouse(mouseX, centerColX + centerColW - 100, 100, "viewDistance");
                    return true;
                }
                optY += rowH;

                // Graphics mode cycle
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 80, optY + 1, 80, 16)) {
                    this.client.options.graphicsMode = this.client.options.graphicsMode == GraphicsMode.FAST ? GraphicsMode.FANCY : GraphicsMode.FAST;
                    saveOptions();
                    return true;
                }

            } else if (selectedSettingCategory == SettingsCategory.CONTROLS) {
                // Mouse Sensitivity slider
                if (checkSliderClick(mouseX, mouseY, centerColX + centerColW - 100, optY + 2, 100, 14)) {
                    activeSliderId = "mouseSensitivity";
                    updateSliderFromMouse(mouseX, centerColX + centerColW - 100, 100, "mouseSensitivity");
                    return true;
                }
                optY += rowH;

                // Invert Mouse toggle
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 60, optY + 1, 60, 16)) {
                    this.client.options.invertYMouse = !this.client.options.invertYMouse;
                    saveOptions();
                    return true;
                }
                optY += rowH;

                // Open Controls Screen
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 90, optY + 1, 90, 16)) {
                    this.client.openScreen(new ControlsOptionsScreen(this, this.client.options));
                    return true;
                }

            } else if (selectedSettingCategory == SettingsCategory.LANGUAGE) {
                // Open Language Screen
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 90, optY + 1, 90, 16)) {
                    this.client.openScreen(new LanguageOptionsScreen(this, this.client.options, this.client.getLanguageManager()));
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean checkSliderClick(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private boolean checkToggleClick(double mouseX, double mouseY, int x, int y, int w, int h) {
        if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
            playClickSound();
            return true;
        }
        return false;
    }

    private void updateSliderFromMouse(double mouseX, int trackX, int trackW, String id) {
        float ratio = (float) (mouseX - trackX) / (float) trackW;
        ratio = Math.max(0.0f, Math.min(1.0f, ratio));

        if ("fov".equals(id)) {
            this.client.options.fov = 30.0 + (ratio * 80.0);
        } else if ("maxFps".equals(id)) {
            int fps = (int) (10 + ratio * 250);
            this.client.options.maxFps = fps >= 255 ? 260 : fps;
        } else if ("viewDistance".equals(id)) {
            this.client.options.viewDistance = (int) (2 + ratio * 30);
        } else if ("mouseSensitivity".equals(id)) {
            this.client.options.mouseSensitivity = ratio;
        } else if (id.startsWith("vol_")) {
            String catName = id.substring(4);
            try {
                SoundCategory cat = SoundCategory.valueOf(catName);
                this.client.options.setSoundVolume(cat, ratio);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (activeSliderId != null && currentTab == MenuTab.SETTINGS) {
            int catW = 100;
            int catX = 14;
            int descColW = 200;
            int centerColX = catX + catW + 16;
            int centerColW = this.width - centerColX - descColW - 20;
            int trackX = centerColX + centerColW - 100;

            updateSliderFromMouse(mouseX, trackX, 100, activeSliderId);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (activeSliderId != null) {
            activeSliderId = null;
            saveOptions();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void saveOptions() {
        if (this.client != null && this.client.options != null) {
            this.client.options.write();
        }
    }

    private void connectToServer(ServerEntry entry) {
        if (this.client != null) {
            this.client.openScreen(new ConnectScreen(this, this.client, entry.serverInfo));
        }
    }
}
