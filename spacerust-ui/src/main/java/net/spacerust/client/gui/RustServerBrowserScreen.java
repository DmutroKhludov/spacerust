package net.spacerust.client.gui;

import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.spacerust.client.config.RustSettings;
import org.lwjgl.glfw.GLFW;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SpaceRust Main Menu & Server Browser for 1.21.11.
 */
public class RustServerBrowserScreen extends Screen {

    private static final Identifier TEX_LOGO        = Identifier.of("spacerust", "textures/gui/logo.png");
    private static final Identifier TEX_HOME        = Identifier.of("spacerust", "textures/gui/icon_home.png");
    private static final Identifier TEX_PLAY        = Identifier.of("spacerust", "textures/gui/icon_play.png");
    private static final Identifier TEX_PLAY_ARROW  = Identifier.of("spacerust", "textures/gui/icon_play_arrow.png");
    private static final Identifier TEX_SETTINGS    = Identifier.of("spacerust", "textures/gui/icon_settings.png");
    private static final Identifier TEX_EXIT        = Identifier.of("spacerust", "textures/gui/icon_exit.png");
    private static final Identifier TEX_OFFICIAL    = Identifier.of("spacerust", "textures/gui/icon_official.png");
    private static final Identifier TEX_MODDED      = Identifier.of("spacerust", "textures/gui/icon_modded.png");
    private static final Identifier TEX_HEART       = Identifier.of("spacerust", "textures/gui/icon_heart.png");
    private static final Identifier TEX_HISTORY     = Identifier.of("spacerust", "textures/gui/icon_history.png");
    private static final Identifier TEX_SEARCH      = Identifier.of("spacerust", "textures/gui/icon_search.png");
    private static final Identifier TEX_REFRESH     = Identifier.of("spacerust", "textures/gui/icon_refresh.png");
    private static final Identifier TEX_RESET       = Identifier.of("spacerust", "textures/gui/icon_reset.png");
    private static final Identifier TEX_BG_FALLBACK = Identifier.of("spacerust", "textures/gui/bg_main.png");

    public static ServerEntry activeServer = null;

    public enum MenuTab {
        HOME("ГЛАВНАЯ"),
        PLAY("ИГРАТЬ"),
        SETTINGS("НАСТРОЙКИ");

        public final String title;
        MenuTab(String title) { this.title = title; }
    }

    public enum PlayCategory {
        OFFICIAL("ОФИЦИАЛЬНЫЕ"),
        MODDED("МОДИФИЦИРОВАННЫЕ"),
        FAVORITES("ИЗБРАННОЕ"),
        HISTORY("ИСТОРИЯ");

        public final String title;
        PlayCategory(String title) { this.title = title; }
    }

    public static class ServerEntry {
        public final String name;
        public final String map;
        public final String region;
        public String ip;
        public final String modeBadge;
        public final String wipeTag;
        public final String[] tags;
        public final boolean isModded;
        public ServerInfo serverInfo;
        public boolean isFavorite = true;
        public boolean inHistory = true;

        public boolean isOnline = false;
        public boolean isPinging = true;
        public int currentPlayers = 0;
        public int maxPlayers = 20;
        public long pingMs = -1;
        public String statusText = "Опрос...";

        public ServerEntry(String name, String map, String region, String ip,
                           String modeBadge, String wipeTag, String[] tags, boolean isModded) {
            this.name = name;
            this.map = map;
            this.region = region;
            this.ip = ip;
            this.modeBadge = modeBadge;
            this.wipeTag = wipeTag;
            this.tags = tags;
            this.isModded = isModded;
            this.serverInfo = new ServerInfo(name, ip, ServerInfo.ServerType.OTHER);
        }
    }

    public static final List<ServerEntry> SERVERS = new ArrayList<>();
    public static final List<ServerEntry> RECENT_SERVERS = new ArrayList<>();

    private static String resolveDefaultServerIp() {
        String defaultIp = "26.226.103.84:25565";
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.runDirectory != null) {
                File cfg = new File(mc.runDirectory, "config/spacerust_server_ip.txt");
                if (cfg.isFile()) {
                    String raw = Files.readString(cfg.toPath(), StandardCharsets.UTF_8).trim();
                    if (!raw.isEmpty()) {
                        return raw.contains(":") ? raw : (raw + ":25565");
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try (Socket test = new Socket()) {
            test.connect(new InetSocketAddress("127.0.0.1", 25565), 150);
            return "127.0.0.1:25565";
        } catch (Exception ignored) {
        }
        return defaultIp;
    }

    static {
        ServerEntry mainServer = new ServerEntry(
                "[EU/RU] Наша — SpaceRust 1.21.11 X2",
                "Procedural Map 3800",
                "RU / EU",
                resolveDefaultServerIp(),
                "SPACERUST",
                "ЕЖЕМЕСЯЧНЫЙ",
                new String[]{"X2 РЕСУРСЫ", "1.21.11", "ЕЖЕМЕСЯЧНЫЙ", "MAX 3"},
                false
        );
        SERVERS.add(mainServer);
        RECENT_SERVERS.add(mainServer);
    }

    public static MenuTab currentTab = MenuTab.HOME;
    public static PlayCategory selectedPlayCategory = PlayCategory.OFFICIAL;

    private static final String[] WIPE_OPTIONS = {"ВСЕ", "ЕЖЕМЕСЯЧНЫЙ", "ЕЖЕНЕДЕЛЬНЫЙ"};
    private int wipeIndex = 0;
    private boolean expandFilters = false;
    private boolean expandWipe = false;
    private boolean onlyOnlineFilter = false;

    private String serverSearchQuery = "";
    private boolean isSearchFocused = false;
    private long lastRowClickMs = 0L;
    private int lastClickedRowIdx = -1;

    private final RustSettingsPanel settingsPanel = new RustSettingsPanel();
    private String lastMenuHoverKey = "";

    private static final int TOP_H = 32;
    private static final int BOTTOM_H = 26;

    public RustServerBrowserScreen() {
        super(Text.literal("SpaceRust Main Menu"));
    }

    @Override
    protected void init() {
        super.init();
        RustFontRenderer.ensureLoaded();
        MenuVideoPlayer.getInstance().ensureStarted();
        refreshServers();
    }

    @Override
    public void removed() {
        super.removed();
        settingsPanel.onClosed();
        MenuVideoPlayer.getInstance().stop();
    }

    public void refreshServers() {
        String curIp = resolveDefaultServerIp();
        for (ServerEntry s : SERVERS) {
            s.ip = curIp;
            s.serverInfo = new ServerInfo(s.name, curIp, ServerInfo.ServerType.OTHER);
            s.isPinging = true;
            s.statusText = "Опрос...";
            pingServerAsync(s);
        }
        for (ServerEntry s : RECENT_SERVERS) {
            s.ip = curIp;
            s.serverInfo = new ServerInfo(s.name, curIp, ServerInfo.ServerType.OTHER);
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
                    writeVarInt(handshake, SharedConstants.getProtocolVersion());
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
                        int pOnline = parseJsonInt(json, "\"online\":", 0);
                        int pMax = parseJsonInt(json, "\"max\":", 20);

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

    private static int parseJsonInt(String json, String key, int fallback) {
        int idx = json.indexOf(key);
        if (idx == -1) return fallback;
        int end = json.indexOf(",", idx);
        if (end == -1) end = json.indexOf("}", idx);
        if (end == -1) return fallback;
        try { return Integer.parseInt(json.substring(idx + key.length(), end).trim()); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int numRead = 0, result = 0;
        byte read;
        do {
            read = in.readByte();
            result |= ((read & 0b01111111) << (7 * numRead));
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
        return this.client != null && this.client.world != null;
    }

    @Override
    public void close() {
        if (this.client != null && this.client.world != null) {
            settingsPanel.onClosed();
            this.client.setScreen(new RustPauseScreen());
            return;
        }
        super.close();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int screenW = this.width;
        int screenH = this.height;

        boolean videoRendered = MenuVideoPlayer.getInstance().render(ctx, screenW, screenH);
        if (!videoRendered) {
            renderFallbackBackground(ctx, screenW, screenH);
        }

        if (currentTab == MenuTab.HOME) {
            ctx.fill(0, 0, screenW, screenH, 0x22080A0D);
            ctx.fillGradient(0, 0, screenW, 72, 0x77080A0D, 0x00080A0D);
            ctx.fillGradient(0, screenH - 64, screenW, screenH, 0x00080A0D, 0x88080A0D);
        } else {
            ctx.fill(0, 0, screenW, screenH, 0xCC111316);
        }

        renderTopBar(ctx, mouseX, mouseY, screenW);

        if (currentTab == MenuTab.HOME) {
            renderHomeTab(ctx, mouseX, mouseY, screenW, screenH);
        } else if (currentTab == MenuTab.PLAY) {
            renderPlayTab(ctx, mouseX, mouseY, screenW, screenH);
        } else if (currentTab == MenuTab.SETTINGS) {
            renderSettingsTab(ctx, mouseX, mouseY, screenW, screenH);
        }

        renderBottomBar(ctx, screenW, screenH);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderFallbackBackground(DrawContext ctx, int screenW, int screenH) {
        if (this.client != null) {
            int imgW = 1376;
            int imgH = 768;
            float scale = Math.max((float) screenW / imgW, (float) screenH / imgH);
            int drawW = Math.round(imgW * scale);
            int drawH = Math.round(imgH * scale);
            int x0 = Math.round((screenW - drawW) * 0.5f);
            int y0 = Math.round((screenH - drawH) * 0.5f);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, TEX_BG_FALLBACK, x0, y0, 0f, 0f, drawW, drawH, drawW, drawH);
        } else {
            ctx.fillGradient(0, 0, screenW, screenH, 0xFF1A1E26, 0xFF0C0E13);
        }
    }

    private void drawIcon(DrawContext ctx, Identifier tex, int x, int y, int size, int argb) {
        RustUiHelper.drawSprite(ctx, this.client, tex, x, y, size, size, argb);
    }

    private void renderTopBar(DrawContext ctx, int mouseX, int mouseY, int screenW) {
        ctx.fill(0, 0, screenW, TOP_H, 0xDF16181C);
        ctx.fill(0, TOP_H, screenW, TOP_H + 1, 0x20FFFFFF);

        // Top-Left Brand: Наша
        RustUiHelper.drawSprite(ctx, this.client, TEX_LOGO, 10, 5, 22, 22, 0xFFFFFFFF);
        if (this.client == null || this.client.world == null) {
            RustFontRenderer.bold().drawCenteredY(ctx, "НАША", 38, 0, TOP_H, 0xFFF7EBE1);
        } else {
            int backW = 114;
            int backX = 40;
            boolean bHov = inRect(mouseX, mouseY, backX, 5, backW, 22);
            ctx.fill(backX, 5, backX + backW, 27, bHov ? 0xEECD412B : 0xCC9A2E1E);
            RustFontRenderer bbf = RustFontRenderer.boldSmall();
            String bTxt = "← ВЕРНУТЬСЯ В ИГРУ";
            bbf.drawCenteredY(ctx, bTxt, backX + (backW - bbf.getWidth(bTxt)) / 2f, 5, 22, 0xFFFFFFFF);
        }

        int tabW = 92;
        int gap = 2;
        int centerX = screenW / 2;
        int homeTabX = centerX - tabW - (gap / 2);
        int playTabX = centerX + (gap / 2);

        drawTopNavTab(ctx, mouseX, mouseY, homeTabX, 0, tabW, TOP_H, TEX_HOME, MenuTab.HOME.title, currentTab == MenuTab.HOME);
        drawTopNavTab(ctx, mouseX, mouseY, playTabX, 0, tabW, TOP_H, TEX_PLAY, MenuTab.PLAY.title, currentTab == MenuTab.PLAY);

        // Top Right: ОДИНОЧНАЯ + НАСТРОЙКИ + ВЫЙТИ
        int setW = 86;
        int exitW = 66;
        int singleW = 84;
        int exitX = screenW - exitW - 10;
        int setX = exitX - setW - 5;
        int singleX = setX - singleW - 5;
        int btnY = 5;
        int btnH = 22;

        RustFontRenderer bf = RustFontRenderer.boldSmall();

        boolean singleHov = inRect(mouseX, mouseY, singleX, btnY, singleW, btnH);
        if (singleHov) playMenuHover("top:single");
        ctx.fill(singleX, btnY, singleX + singleW, btnY + btnH, singleHov ? 0x66452622 : 0x5522252A);
        String singleTxt = "ОДИНОЧНАЯ";
        bf.drawCenteredY(ctx, singleTxt, singleX + (singleW - bf.getWidth(singleTxt)) / 2f, btnY, btnH, singleHov ? 0xFFFFFFFF : 0xFFD0D4DA);

        boolean setActive = currentTab == MenuTab.SETTINGS;
        boolean setHov = inRect(mouseX, mouseY, setX, btnY, setW, btnH);
        if (setHov) playMenuHover("top:settings");
        ctx.fill(setX, btnY, setX + setW, btnY + btnH, setActive ? 0xEECD412B : (setHov ? 0x66452622 : 0x5522252A));
        float stw = bf.getWidth(MenuTab.SETTINGS.title);
        int sIcon = 10;
        int sStart = Math.round(setX + (setW - (sIcon + 5 + stw)) / 2f);
        int sCol = (setActive || setHov) ? 0xFFFFFFFF : 0xFFD0D4DA;
        drawIcon(ctx, TEX_SETTINGS, sStart, btnY + (btnH - sIcon) / 2, sIcon, sCol);
        bf.drawCenteredY(ctx, MenuTab.SETTINGS.title, sStart + sIcon + 5, btnY, btnH, sCol);

        boolean exitHov = inRect(mouseX, mouseY, exitX, btnY, exitW, btnH);
        if (exitHov) playMenuHover("top:exit");
        ctx.fill(exitX, btnY, exitX + exitW, btnY + btnH, exitHov ? 0xDDB72820 : 0x5522252A);
        String exitTxt = "ВЫЙТИ";
        float etw = bf.getWidth(exitTxt);
        int eIcon = 10;
        int eStart = Math.round(exitX + (exitW - (eIcon + 5 + etw)) / 2f);
        int eCol = exitHov ? 0xFFFFFFFF : 0xFFD0D4DA;
        drawIcon(ctx, TEX_EXIT, eStart, btnY + (btnH - eIcon) / 2, eIcon, eCol);
        bf.drawCenteredY(ctx, exitTxt, eStart + eIcon + 5, btnY, btnH, eCol);
    }

    private void drawTopNavTab(DrawContext ctx, int mouseX, int mouseY, int x, int y, int w, int h,
                               Identifier icon, String title, boolean active) {
        boolean hov = inRect(mouseX, mouseY, x, y, w, h);
        if (hov) playMenuHover("top:" + title);
        int bg = active ? 0xEECD412B : (hov ? 0x55422521 : 0x331E2024);
        ctx.fill(x, y, x + w, y + h, bg);

        RustFontRenderer bf = RustFontRenderer.bold();
        float tw = bf.getWidth(title);
        int iconSize = 11;
        int startX = Math.round(x + (w - (iconSize + 6 + tw)) / 2f);
        int col = active ? 0xFFFFFFFF : (hov ? 0xFFEAECEF : 0xFFB8BDC4);

        drawIcon(ctx, icon, startX, y + (h - iconSize) / 2, iconSize, col);
        bf.drawCenteredY(ctx, title, startX + iconSize + 6, y, h, col);
    }

    private void renderBottomBar(DrawContext ctx, int screenW, int screenH) {
        int barY = screenH - BOTTOM_H;
        ctx.fill(0, barY, screenW, screenH, 0x99121418);
        ctx.fill(0, barY, screenW, barY + 1, 0x18FFFFFF);

        if (RustSettings.get().showFps && this.client != null) {
            String shortFps = this.client.getCurrentFps() + " FPS";
            RustFontRenderer.small().drawCenteredY(ctx, shortFps, 10, barY, BOTTOM_H, 0x77AAAAAA);
        }

        if (this.client != null) {
            String pName = this.client.getSession().getUsername();
            RustFontRenderer bf = RustFontRenderer.boldSmall();
            float nw = bf.getWidth(pName);
            bf.drawCenteredY(ctx, pName, screenW - nw - 34, barY, BOTTOM_H, 0xBBF7EBE1);
            RustUiHelper.drawSprite(ctx, this.client, TEX_LOGO, screenW - 28, barY + 4, 18, 18, 0xFFFFFFFF);
        }
    }

    private void renderHomeTab(DrawContext ctx, int mouseX, int mouseY, int screenW, int screenH) {
        int qcW = 264;
        int qcX = screenW - qcW - 18;
        int qcY = TOP_H + 18;

        int hdrH = 18;
        ctx.fill(qcX, qcY, qcX + qcW, qcY + hdrH, 0xAA13161B);
        drawIcon(ctx, TEX_HISTORY, qcX + 6, qcY + 4, 10, 0xFFE6EAEE);
        RustFontRenderer.boldSmall().drawCenteredShadowY(ctx, "БЫСТРОЕ ПОДКЛЮЧЕНИЕ", qcX + 20, qcY, hdrH, 0xFFE6EAEE);

        int cardY = qcY + hdrH + 3;
        int cardH = 40;
        int cardGap = 4;
        int count = Math.min(3, RECENT_SERVERS.size());

        for (int i = 0; i < count; i++) {
            ServerEntry s = RECENT_SERVERS.get(i);
            int y = cardY + i * (cardH + cardGap);
            boolean cardHov = inRect(mouseX, mouseY, qcX, y, qcW, cardH);

            ctx.fill(qcX, y, qcX + qcW, y + cardH, cardHov ? 0xE22A2D34 : 0xCC1C1F24);

            RustFontRenderer bf = RustFontRenderer.bold();
            String title = bf.trim(s.name, qcW - 46);
            bf.drawCenteredShadowY(ctx, title, qcX + 9, y + 4, 14, 0xFFFFFFFF);

            float px = qcX + 9;
            int py = y + 21;
            px += drawMiniPill(ctx, px, py, s.modeBadge, 0xFF2F333A, 0xFFC8CDD4) + 4;
            px += drawMiniPill(ctx, px, py, s.wipeTag, 0xFF2F333A, 0xFFC8CDD4) + 6;

            String stat = s.isOnline
                    ? ("● " + s.currentPlayers + "/" + s.maxPlayers)
                    : (s.isPinging ? "● ОПРОС..." : "● ОФФЛАЙН");
            int statCol = s.isOnline ? 0xFF8BC34A : 0xFF9EA5AE;
            RustFontRenderer.small().drawCenteredY(ctx, stat, px, py, 13, statCol);

            int playBtnSize = 24;
            int pbX = qcX + qcW - playBtnSize - 8;
            int pbY = y + (cardH - playBtnSize) / 2;
            boolean pbHov = inRect(mouseX, mouseY, pbX, pbY, playBtnSize, playBtnSize);
            ctx.fill(pbX, pbY, pbX + playBtnSize, pbY + playBtnSize,
                    pbHov ? 0xFFCD412B : (cardHov ? 0xFF3E434C : 0xFF2B2F36));
            drawIcon(ctx, TEX_PLAY_ARROW, pbX + 7, pbY + 7, 10, 0xFFEAECEF);
        }
    }

    private float drawMiniPill(DrawContext ctx, float x, int y, String label, int bg, int fg) {
        RustFontRenderer sf = RustFontRenderer.boldSmall();
        float w = sf.getWidth(label) + 10;
        int h = 13;
        int ix = Math.round(x);
        ctx.fill(ix, y, ix + Math.round(w), y + h, bg);
        sf.drawCenteredY(ctx, label, x + 5.0f, y, h, fg);
        return w;
    }

    private void renderPlayTab(DrawContext ctx, int mouseX, int mouseY, int screenW, int screenH) {
        int subY = TOP_H + 8;
        int subH = 22;
        int sx = 14;

        int officialOnline = 0, moddedOnline = 0, favCount = 0, histCount = 0;
        for (ServerEntry s : SERVERS) {
            if (s.isModded) moddedOnline += s.isOnline ? s.currentPlayers : 0;
            else officialOnline += s.isOnline ? s.currentPlayers : 0;
            if (s.isFavorite) favCount++;
            if (s.inHistory) histCount++;
        }

        sx += drawPlayCategoryPill(ctx, mouseX, mouseY, sx, subY, subH, TEX_OFFICIAL, PlayCategory.OFFICIAL, String.valueOf(officialOnline), true) + 5;
        sx += drawPlayCategoryPill(ctx, mouseX, mouseY, sx, subY, subH, TEX_MODDED, PlayCategory.MODDED, String.valueOf(moddedOnline), false) + 5;
        sx += drawPlayCategoryPill(ctx, mouseX, mouseY, sx, subY, subH, TEX_HEART, PlayCategory.FAVORITES, String.valueOf(favCount), false) + 5;
        drawPlayCategoryPill(ctx, mouseX, mouseY, sx, subY, subH, TEX_HISTORY, PlayCategory.HISTORY, String.valueOf(histCount), false);

        int searchW = 156;
        int searchX = screenW - searchW - 14;
        ctx.fill(searchX, subY, searchX + searchW, subY + subH, 0xCC1A1D22);
        ctx.fill(searchX, subY + subH - 1, searchX + searchW, subY + subH, isSearchFocused ? 0xFFCD412B : 0x28FFFFFF);
        RustFontRenderer rf = RustFontRenderer.regular();
        if (serverSearchQuery.isEmpty()) {
            rf.drawCenteredY(ctx, isSearchFocused ? "|" : "ПОИСК...", searchX + 8, subY, subH, 0x66AAAAAA);
        } else {
            String disp = rf.trim(serverSearchQuery, searchW - 26) + (isSearchFocused ? "|" : "");
            rf.drawCenteredY(ctx, disp, searchX + 8, subY, subH, 0xFFFFFFFF);
        }
        drawIcon(ctx, TEX_SEARCH, searchX + searchW - 17, subY + 6, 10, 0x88AAAAAA);

        int sideX = 14;
        int sideY = subY + subH + 10;
        int sideW = 148;
        int sideBottom = screenH - BOTTOM_H - 8;
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();

        int curY = sideY;
        curY += drawCollapsibleHeader(ctx, mouseX, mouseY, sideX, curY, sideW, "ФИЛЬТРЫ", expandFilters);
        if (expandFilters) {
            boolean onlHov = inRect(mouseX, mouseY, sideX, curY, sideW, 18);
            ctx.fill(sideX, curY, sideX + sideW, curY + 18,
                    onlyOnlineFilter ? 0xFF6B2820 : (onlHov ? 0x33FFFFFF : 0x22181B20));
            int cbX = sideX + 7;
            int cbY = curY + 5;
            ctx.fill(cbX, cbY, cbX + 8, cbY + 8, 0xAAFFFFFF);
            ctx.fill(cbX + 1, cbY + 1, cbX + 7, cbY + 7, onlyOnlineFilter ? 0xFFCD412B : 0xFF1A1D22);
            bfSmall.drawCenteredY(ctx, "ТОЛЬКО ОНЛАЙН", sideX + 19, curY, 18, onlyOnlineFilter ? 0xFFFFFFFF : 0xFFC2C7CE);
            curY += 20;
        }
        curY += 4;

        curY += drawCollapsibleHeader(ctx, mouseX, mouseY, sideX, curY, sideW, "ЧАСТОТА ВАЙПА", expandWipe);
        if (expandWipe) {
            boolean wHov = inRect(mouseX, mouseY, sideX, curY, sideW, 18);
            ctx.fill(sideX, curY, sideX + sideW, curY + 18, wHov ? 0x33FFFFFF : 0x22181B20);
            bfSmall.drawCenteredY(ctx, WIPE_OPTIONS[wipeIndex], sideX + 7, curY, 18, 0xFFE0E4EA);
        }

        int utilBtnH = 20;
        int utilBtnY = sideBottom - utilBtnH;
        int resetW = 71;
        int refW = sideW - resetW - 4;
        drawSidebarBtn(ctx, mouseX, mouseY, sideX, utilBtnY, resetW, utilBtnH, TEX_RESET, "СБРОС", 0xAA252930, 0xCC353B45, 0xFFD0D5DC);
        drawSidebarBtn(ctx, mouseX, mouseY, sideX + resetW + 4, utilBtnY, refW, utilBtnH, TEX_REFRESH, "ОБНОВИТЬ", 0xAA252930, 0xCC353B45, 0xFFD0D5DC);

        int tableX = sideX + sideW + 12;
        int tableW = screenW - tableX - 14;
        int headerY = sideY;
        int headerH = 18;

        ctx.fill(tableX, headerY, tableX + tableW, headerY + headerH, 0xEE14171C);
        drawIcon(ctx, TEX_HEART, tableX + 8, headerY + 5, 8, 0x88AAAAAA);
        bfSmall.drawCenteredY(ctx, "НАЗВАНИЕ", tableX + 26, headerY, headerH, 0xFF8F96A0);
        int colModeX = tableX + (int) (tableW * 0.57f);
        int colPlayersX = tableX + (int) (tableW * 0.71f);
        int colPingX = tableX + (int) (tableW * 0.83f);
        bfSmall.drawCenteredY(ctx, "РЕЖИМ", colModeX, headerY, headerH, 0xFF8F96A0);
        bfSmall.drawCenteredY(ctx, "ИГРОКИ", colPlayersX, headerY, headerH, 0xFF8F96A0);
        bfSmall.drawCenteredY(ctx, "ПИНГ", colPingX, headerY, headerH, 0xFF8F96A0);

        List<ServerEntry> filtered = getFilteredServers();
        int rowY = headerY + headerH + 4;
        int rowH = 40;
        int rowGap = 3;

        if (filtered.isEmpty()) {
            ctx.fill(tableX, rowY, tableX + tableW, rowY + 34, 0x9916191F);
            String empty = "Нет серверов, подходящих под выбранные фильтры.";
            rf.drawCenteredY(ctx, empty, tableX + (tableW - rf.getWidth(empty)) / 2f, rowY, 34, 0x88AAAAAA);
            return;
        }

        RustFontRenderer bf = RustFontRenderer.bold();
        RustFontRenderer sf = RustFontRenderer.small();
        for (int i = 0; i < filtered.size(); i++) {
            ServerEntry s = filtered.get(i);
            int y = rowY + i * (rowH + rowGap);
            if (y + rowH > sideBottom) break;

            boolean isHover = inRect(mouseX, mouseY, tableX, y, tableW, rowH);
            ctx.fill(tableX, y, tableX + tableW, y + rowH, isHover ? 0xDD22262E : 0xB8171A1F);
            ctx.fill(tableX, y + rowH - 1, tableX + tableW, y + rowH, 0x16FFFFFF);
            ctx.fill(tableX, y, tableX + 2, y + rowH, s.isOnline ? 0xFF5EC739 : 0xFFCD412B);

            drawIcon(ctx, TEX_HEART, tableX + 8, y + (rowH - 9) / 2, 9, s.isFavorite ? 0xFFCD412B : 0x66AAAAAA);
            bf.drawCenteredShadowY(ctx, s.name, tableX + 26, y + 4, 14, 0xFFFFFFFF);

            float tx = tableX + 26;
            int ty = y + 21;
            for (int t = 0; t < s.tags.length; t++) {
                String tag = s.tags[t];
                boolean isGold = (t == 0);
                tx += drawMiniPill(ctx, tx, ty, tag, isGold ? 0xFFA97B2A : 0xFF2B2F36, isGold ? 0xFFFFE082 : 0xFFB0B6BE) + 4;
            }
            sf.drawCenteredY(ctx, s.map, tx + 3, ty, 13, 0x88AAAAAA);

            drawMiniPill(ctx, colModeX, y + (rowH - 13) / 2, s.modeBadge, 0xFF6B2820, 0xFFFFD8D1);

            String countStr = s.isOnline ? (s.currentPlayers + " / " + s.maxPlayers) : "ОФФЛАЙН";
            bfSmall.drawCenteredY(ctx, countStr, colPlayersX, y, rowH, s.isOnline ? 0xFFFFFFFF : 0xFFD9655A);

            String pingStr = s.isOnline ? (s.pingMs + " ms") : "-- ms";
            bfSmall.drawCenteredY(ctx, pingStr, colPingX, y, rowH, s.isOnline ? 0xFF8BC34A : 0x77AAAAAA);

            int btnW = 66, btnH = 22;
            int btnX = tableX + tableW - btnW - 8;
            int btnY = y + (rowH - btnH) / 2;
            boolean hovBtn = inRect(mouseX, mouseY, btnX, btnY, btnW, btnH);
            ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, hovBtn ? 0xFFE04B32 : (isHover || s.isOnline ? 0xFFCD412B : 0xFF282C34));
            bfSmall.drawCenteredY(ctx, "ИГРАТЬ", btnX + (btnW - bfSmall.getWidth("ИГРАТЬ")) / 2f, btnY, btnH, 0xFFFFFFFF);
        }
    }

    private int drawPlayCategoryPill(DrawContext ctx, int mouseX, int mouseY,
                                     int x, int y, int h, Identifier icon, PlayCategory cat,
                                     String badge, boolean greenTheme) {
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        int badgeBoxW = Math.round(bf.getWidth(badge)) + 8;
        int w = 8 + 9 + 5 + Math.round(bf.getWidth(cat.title)) + 6 + badgeBoxW + 6;
        boolean active = selectedPlayCategory == cat;
        boolean hov = inRect(mouseX, mouseY, x, y, w, h);

        int bg = active ? (greenTheme ? 0xFFCD412B : 0xFF9E3122) : (hov ? 0x44FFFFFF : 0xAA1D2026);
        int fg = active || hov ? 0xFFFFFFFF : 0xFFB8BDC4;
        ctx.fill(x, y, x + w, y + h, bg);
        drawIcon(ctx, icon, x + 8, y + (h - 9) / 2, 9, fg);
        bf.drawCenteredY(ctx, cat.title, x + 22, y, h, fg);

        int bx = x + w - badgeBoxW - 4, by = y + 4, bh = h - 8;
        ctx.fill(bx, by, bx + badgeBoxW, by + bh, 0x44000000);
        bf.drawCenteredY(ctx, badge, bx + 4, by, bh, fg);
        return w;
    }

    private int drawCollapsibleHeader(DrawContext ctx, int mouseX, int mouseY,
                                      int x, int y, int w, String title, boolean expanded) {
        int h = 19;
        boolean hov = inRect(mouseX, mouseY, x, y, w, h);
        ctx.fill(x, y, x + w, y + h, hov ? 0x33FFFFFF : 0x881A1D22);
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        bf.drawCenteredY(ctx, title, x + 7, y, h, 0xFFC4C9D0);
        bf.drawCenteredY(ctx, expanded ? "−" : "+", x + w - 12, y, h, 0xFF9EA5AE);
        return h + 2;
    }

    private void drawSidebarBtn(DrawContext ctx, int mouseX, int mouseY,
                                int x, int y, int w, int h, Identifier icon, String label,
                                int bg, int hovBg, int fg) {
        boolean hov = inRect(mouseX, mouseY, x, y, w, h);
        ctx.fill(x, y, x + w, y + h, hov ? hovBg : bg);
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        int sx = Math.round(x + (w - (9 + 5 + bf.getWidth(label))) / 2f);
        drawIcon(ctx, icon, sx, y + (h - 9) / 2, 9, fg);
        bf.drawCenteredY(ctx, label, sx + 14, y, h, fg);
    }

    private List<ServerEntry> getFilteredServers() {
        List<ServerEntry> out = new ArrayList<>();
        String q = serverSearchQuery == null ? "" : serverSearchQuery.trim().toLowerCase(Locale.ROOT);
        String wipeFilter = WIPE_OPTIONS[wipeIndex];

        for (ServerEntry s : SERVERS) {
            if (selectedPlayCategory == PlayCategory.OFFICIAL && s.isModded) continue;
            if (selectedPlayCategory == PlayCategory.MODDED && !s.isModded) continue;
            if (selectedPlayCategory == PlayCategory.FAVORITES && !s.isFavorite) continue;
            if (selectedPlayCategory == PlayCategory.HISTORY && !s.inHistory) continue;
            if (onlyOnlineFilter && !s.isOnline) continue;
            if (!"ВСЕ".equals(wipeFilter) && !wipeFilter.equalsIgnoreCase(s.wipeTag)) continue;
            if (!q.isEmpty()) {
                boolean match = s.name.toLowerCase(Locale.ROOT).contains(q)
                        || s.ip.toLowerCase(Locale.ROOT).contains(q)
                        || s.map.toLowerCase(Locale.ROOT).contains(q);
                if (!match) continue;
            }
            out.add(s);
        }
        return out;
    }

    private void renderSettingsTab(DrawContext ctx, int mouseX, int mouseY, int screenW, int screenH) {
        int startY = TOP_H + 12;
        int panelH = Math.max(120, screenH - startY - BOTTOM_H - 10);
        settingsPanel.render(ctx, this, this.client, mouseX, mouseY, 14, startY, screenW - 28, panelH);
    }

    private void playClickSound() { RustUiHelper.playMenuClick(this.client); }

    private void playMenuHover(String key) {
        if (key == null || key.equals(lastMenuHoverKey)) return;
        lastMenuHoverKey = key;
        RustUiHelper.playMenuHover(this.client);
    }

    @Override
    public boolean charTyped(CharInput input) {
        String s = input.asString();
        if (s != null && !s.isEmpty()) {
            char chr = s.charAt(0);
            if (currentTab == MenuTab.SETTINGS && settingsPanel.charTyped(chr)) return true;
            if (currentTab == MenuTab.PLAY && isSearchFocused && chr >= 32 && chr != 127 && serverSearchQuery.length() < 32) {
                serverSearchQuery += chr;
                return true;
            }
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (currentTab == MenuTab.SETTINGS && settingsPanel.keyPressed(this.client, input)) return true;
        int keyCode = input.key();
        if (currentTab == MenuTab.PLAY && isSearchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!serverSearchQuery.isEmpty()) {
                    serverSearchQuery = serverSearchQuery.substring(0, serverSearchQuery.length() - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                isSearchFocused = false;
                return true;
            }
        }
        return super.keyPressed(input);
    }

    private int getCategoryPillWidth(PlayCategory cat, String badge) {
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        int badgeBoxW = Math.round(bf.getWidth(badge)) + 8;
        return 8 + 9 + 5 + Math.round(bf.getWidth(cat.title)) + 6 + badgeBoxW + 6;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        double mouseX = click.x();
        double mouseY = click.y();
        int screenW = this.width;
        int screenH = this.height;

        if (this.client != null && this.client.world != null && inRect(mouseX, mouseY, 40, 5, 114, 22)) {
            if (currentTab == MenuTab.SETTINGS) settingsPanel.onClosed();
            playClickSound();
            this.client.setScreen(null);
            return true;
        }

        int tabW = 92, gap = 2, centerX = screenW / 2;
        int homeTabX = centerX - tabW - (gap / 2);
        int playTabX = centerX + (gap / 2);

        if (inRect(mouseX, mouseY, homeTabX, 0, tabW, TOP_H)) {
            if (currentTab == MenuTab.SETTINGS) settingsPanel.onClosed();
            currentTab = MenuTab.HOME;
            isSearchFocused = false;
            playClickSound();
            return true;
        }
        if (inRect(mouseX, mouseY, playTabX, 0, tabW, TOP_H)) {
            if (currentTab == MenuTab.SETTINGS) settingsPanel.onClosed();
            currentTab = MenuTab.PLAY;
            isSearchFocused = false;
            playClickSound();
            return true;
        }

        int setW = 86, exitW = 66, singleW = 84;
        int exitX = screenW - exitW - 10;
        int setX = exitX - setW - 5;
        int singleX = setX - singleW - 5;

        if (this.client != null && inRect(mouseX, mouseY, singleX, 5, singleW, 22)) {
            if (currentTab == MenuTab.SETTINGS) settingsPanel.onClosed();
            playClickSound();
            MenuVideoPlayer.getInstance().stop();
            this.client.setScreen(new SelectWorldScreen(this));
            return true;
        }
        if (inRect(mouseX, mouseY, setX, 5, setW, 22)) {
            currentTab = MenuTab.SETTINGS;
            isSearchFocused = false;
            playClickSound();
            return true;
        }
        if (this.client != null && inRect(mouseX, mouseY, exitX, 5, exitW, 22)) {
            playClickSound();
            settingsPanel.onClosed();
            MenuVideoPlayer.getInstance().stop();
            this.client.scheduleStop();
            return true;
        }

        if (currentTab == MenuTab.HOME) {
            int qcW = 264, qcX = screenW - qcW - 18, cardY = TOP_H + 18 + 21, cardH = 40, cardGap = 4;
            int count = Math.min(3, RECENT_SERVERS.size());
            for (int i = 0; i < count; i++) {
                int y = cardY + i * (cardH + cardGap);
                if (inRect(mouseX, mouseY, qcX, y, qcW, cardH)) {
                    playClickSound();
                    connectToServer(RECENT_SERVERS.get(i));
                    return true;
                }
            }
        }

        if (currentTab == MenuTab.PLAY) {
            int subY = TOP_H + 8, subH = 22, sx = 14;
            int officialOnline = 0, moddedOnline = 0, favCount = 0, histCount = 0;
            for (ServerEntry s : SERVERS) {
                if (s.isModded) moddedOnline += s.isOnline ? s.currentPlayers : 0;
                else officialOnline += s.isOnline ? s.currentPlayers : 0;
                if (s.isFavorite) favCount++;
                if (s.inHistory) histCount++;
            }

            int wOff = getCategoryPillWidth(PlayCategory.OFFICIAL, String.valueOf(officialOnline));
            if (inRect(mouseX, mouseY, sx, subY, wOff, subH)) { selectedPlayCategory = PlayCategory.OFFICIAL; playClickSound(); return true; }
            sx += wOff + 5;
            int wMod = getCategoryPillWidth(PlayCategory.MODDED, String.valueOf(moddedOnline));
            if (inRect(mouseX, mouseY, sx, subY, wMod, subH)) { selectedPlayCategory = PlayCategory.MODDED; playClickSound(); return true; }
            sx += wMod + 5;
            int wFav = getCategoryPillWidth(PlayCategory.FAVORITES, String.valueOf(favCount));
            if (inRect(mouseX, mouseY, sx, subY, wFav, subH)) { selectedPlayCategory = PlayCategory.FAVORITES; playClickSound(); return true; }
            sx += wFav + 5;
            int wHist = getCategoryPillWidth(PlayCategory.HISTORY, String.valueOf(histCount));
            if (inRect(mouseX, mouseY, sx, subY, wHist, subH)) { selectedPlayCategory = PlayCategory.HISTORY; playClickSound(); return true; }

            int searchW = 156, searchX = screenW - searchW - 14;
            isSearchFocused = inRect(mouseX, mouseY, searchX, subY, searchW, subH);
            if (isSearchFocused) { playClickSound(); return true; }

            int sideX = 14, sideY = subY + subH + 10, sideW = 148, sideBottom = screenH - BOTTOM_H - 8;
            int curY = sideY;
            if (inRect(mouseX, mouseY, sideX, curY, sideW, 19)) { expandFilters = !expandFilters; playClickSound(); return true; }
            curY += 21;
            if (expandFilters) {
                if (inRect(mouseX, mouseY, sideX, curY, sideW, 18)) { onlyOnlineFilter = !onlyOnlineFilter; playClickSound(); return true; }
                curY += 20;
            }
            curY += 4;
            if (inRect(mouseX, mouseY, sideX, curY, sideW, 19)) { expandWipe = !expandWipe; playClickSound(); return true; }
            curY += 21;
            if (expandWipe && inRect(mouseX, mouseY, sideX, curY, sideW, 18)) {
                wipeIndex = (wipeIndex + 1) % WIPE_OPTIONS.length;
                playClickSound();
                return true;
            }

            int utilBtnH = 20, utilBtnY = sideBottom - utilBtnH, resetW = 71, refW = sideW - resetW - 4;
            if (inRect(mouseX, mouseY, sideX, utilBtnY, resetW, utilBtnH)) {
                wipeIndex = 0; onlyOnlineFilter = false; serverSearchQuery = ""; playClickSound(); return true;
            }
            if (inRect(mouseX, mouseY, sideX + resetW + 4, utilBtnY, refW, utilBtnH)) {
                refreshServers(); playClickSound(); return true;
            }

            int tableX = sideX + sideW + 12, tableW = screenW - tableX - 14;
            int rowY = sideY + 18 + 4, rowH = 40, rowGap = 3;
            List<ServerEntry> filtered = getFilteredServers();
            for (int i = 0; i < filtered.size(); i++) {
                ServerEntry s = filtered.get(i);
                int y = rowY + i * (rowH + rowGap);
                if (y + rowH > sideBottom) break;

                if (inRect(mouseX, mouseY, tableX + 4, y + 8, 18, 22)) {
                    s.isFavorite = !s.isFavorite;
                    playClickSound();
                    return true;
                }
                int btnW = 66, btnH = 22, btnX = tableX + tableW - btnW - 8, btnY = y + (rowH - btnH) / 2;
                if (inRect(mouseX, mouseY, btnX, btnY, btnW, btnH)) {
                    playClickSound();
                    connectToServer(s);
                    return true;
                }
                if (inRect(mouseX, mouseY, tableX, y, tableW, rowH)) {
                    long now = System.currentTimeMillis();
                    if (lastClickedRowIdx == i && (now - lastRowClickMs) < 400L) {
                        playClickSound();
                        connectToServer(s);
                        return true;
                    }
                    lastClickedRowIdx = i;
                    lastRowClickMs = now;
                    return true;
                }
            }
        }

        if (currentTab == MenuTab.SETTINGS) {
            int startY = TOP_H + 12;
            int panelH = Math.max(120, screenH - startY - BOTTOM_H - 10);
            if (settingsPanel.mouseClicked(this, this.client, mouseX, mouseY, click.button(), 14, startY, screenW - 28, panelH)) {
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (currentTab == MenuTab.SETTINGS && settingsPanel.mouseDragged(click.x(), 14, this.width - 28)) return true;
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (currentTab == MenuTab.SETTINGS && settingsPanel.mouseReleased(this.client)) return true;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentTab == MenuTab.SETTINGS && settingsPanel.mouseScrolled(verticalAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void recordRecentServer(ServerEntry entry) {
        entry.inHistory = true;
        RECENT_SERVERS.remove(entry);
        RECENT_SERVERS.add(0, entry);
        while (RECENT_SERVERS.size() > 3) {
            RECENT_SERVERS.remove(RECENT_SERVERS.size() - 1);
        }
    }

    private void connectToServer(ServerEntry entry) {
        if (this.client != null && entry != null) {
            activeServer = entry;
            recordRecentServer(entry);
            MenuVideoPlayer.getInstance().stop();
            ConnectScreen.connect(this, this.client, ServerAddress.parse(entry.ip), entry.serverInfo, false, null);
        }
    }
}
