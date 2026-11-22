package net.spacerust.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.world.Difficulty;
import net.spacerust.client.SpaceRustClient;
import net.spacerust.client.character.RustCharacterManager;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RustServerBrowserScreen extends Screen {

    // Crisp PNG icons (64x64) + real SpaceRust project logo
    private static final Identifier TEX_LOGO          = new Identifier("spacerust", "textures/gui/logo.png");
    private static final Identifier TEX_HOME          = new Identifier("spacerust", "textures/gui/icon_home.png");
    private static final Identifier TEX_PLAY          = new Identifier("spacerust", "textures/gui/icon_play.png");
    private static final Identifier TEX_PLAY_ARROW    = new Identifier("spacerust", "textures/gui/icon_play_arrow.png");
    private static final Identifier TEX_SETTINGS      = new Identifier("spacerust", "textures/gui/icon_settings.png");
    private static final Identifier TEX_EXIT          = new Identifier("spacerust", "textures/gui/icon_exit.png");
    private static final Identifier TEX_OFFICIAL      = new Identifier("spacerust", "textures/gui/icon_official.png");
    private static final Identifier TEX_MODDED        = new Identifier("spacerust", "textures/gui/icon_modded.png");
    private static final Identifier TEX_HEART         = new Identifier("spacerust", "textures/gui/icon_heart.png");
    private static final Identifier TEX_HISTORY       = new Identifier("spacerust", "textures/gui/icon_history.png");
    private static final Identifier TEX_SEARCH        = new Identifier("spacerust", "textures/gui/icon_search.png");
    private static final Identifier TEX_REFRESH       = new Identifier("spacerust", "textures/gui/icon_refresh.png");
    private static final Identifier TEX_RESET         = new Identifier("spacerust", "textures/gui/icon_reset.png");

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

    public enum SettingsCategory {
        GAME("ИГРА"),
        AUDIO("АУДИО"),
        DISPLAY("ЭКРАН"),
        GRAPHICS("ГРАФИКА"),
        CONTROLS("УПРАВЛЕНИЕ"),
        LANGUAGE("ЯЗЫК");

        public final String title;
        SettingsCategory(String title) { this.title = title; }
    }

    public static class ServerEntry {
        public final String name;
        public final String map;
        public final String region;
        public final String ip;
        public final String modeBadge;
        public final String wipeTag;
        public final String[] tags;
        public final boolean isModded;
        public final ServerInfo serverInfo;
        public boolean isFavorite = true;
        public boolean inHistory = true;

        public boolean isOnline = false;
        public boolean isPinging = true;
        public int currentPlayers = 0;
        public int maxPlayers = 350;
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
            this.serverInfo = new ServerInfo(name, ip, false);
        }
    }

    public static final List<ServerEntry> SERVERS = new ArrayList<>();
    public static final List<ServerEntry> RECENT_SERVERS = new ArrayList<>();

    private static String resolveDefaultServerIp() {
        String defaultIp = "26.226.103.84:25565"; // Radmin VPN host IP
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.runDirectory != null) {
                java.io.File cfg = new java.io.File(mc.runDirectory, "config/spacerust_server_ip.txt");
                if (cfg.isFile()) {
                    String raw = new String(java.nio.file.Files.readAllBytes(cfg.toPath()), StandardCharsets.UTF_8).trim();
                    if (!raw.isEmpty()) {
                        return raw.contains(":") ? raw : (raw + ":25565");
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return defaultIp;
    }

    static {
        ServerEntry mainServer = new ServerEntry(
                "[EU/RU] SpaceRust - UNLIMITED X2",
                "Procedural Map 3800",
                "RU / EU",
                resolveDefaultServerIp(),
                "ВАНИЛЛА",
                "ЕЖЕМЕСЯЧНЫЙ",
                new String[]{"X2 РЕСУРСЫ", "ВАНИЛЛА", "ЕЖЕМЕСЯЧНЫЙ", "MAX 3"},
                false
        );
        SERVERS.add(mainServer);
        RECENT_SERVERS.add(mainServer);
    }

    public static MenuTab currentTab = MenuTab.HOME;
    public static PlayCategory selectedPlayCategory = PlayCategory.OFFICIAL;
    public static SettingsCategory selectedSettingCategory = SettingsCategory.GAME;

    private static final String[] WIPE_OPTIONS = {"ВСЕ", "ЕЖЕМЕСЯЧНЫЙ", "ЕЖЕНЕДЕЛЬНЫЙ"};
    private int wipeIndex = 0;
    private boolean expandFilters = false;
    private boolean expandWipe = false;
    private boolean onlyOnlineFilter = false;

    private String serverSearchQuery = "";
    private boolean isSearchFocused = false;
    private long lastRowClickMs = 0L;
    private int lastClickedRowIdx = -1;

    private String hoveredSettingTitle = "ПОЛЕ ЗРЕНИЯ";
    private String hoveredSettingDesc = "Угол обзора камеры в игре (FOV). Более высокий угол дает широкий обзор окружения.";
    private String activeSliderId = null;

    private static final int TOP_H = 32;
    private static final int BOTTOM_H = 26;

    public RustServerBrowserScreen() {
        super(new LiteralText("SpaceRust Main Menu"));
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
        MenuVideoPlayer.getInstance().stop();
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
                        int pOnline = parseJsonInt(json, "\"online\":", 0);
                        int pMax = parseJsonInt(json, "\"max\":", 350);

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
        try {
            return Integer.parseInt(json.substring(idx + key.length(), end).trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
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

        // 0. Full-screen HD MP4 Video Background (1280x720)
        boolean videoRendered = MenuVideoPlayer.getInstance().render(screenW, screenH);
        if (!videoRendered) {
            renderFallbackBackground(matrices, screenW, screenH);
        }

        // Authentic Rust translucent backdrop & vignette
        if (currentTab == MenuTab.HOME) {
            DrawableHelper.fill(matrices, 0, 0, screenW, screenH, 0x22080A0D);
            this.fillGradient(matrices, 0, 0, screenW, 72, 0x77080A0D, 0x00080A0D);
            this.fillGradient(matrices, 0, screenH - 64, screenW, screenH, 0x00080A0D, 0x88080A0D);
        } else {
            DrawableHelper.fill(matrices, 0, 0, screenW, screenH, 0xCC111316);
        }

        // 1. Top Navigation Header Bar
        renderTopBar(matrices, mouseX, mouseY, screenW);

        // 2. Active Tab View
        if (currentTab == MenuTab.HOME) {
            renderHomeTab(matrices, mouseX, mouseY, screenW, screenH);
        } else if (currentTab == MenuTab.PLAY) {
            renderPlayTab(matrices, mouseX, mouseY, screenW, screenH);
        } else if (currentTab == MenuTab.SETTINGS) {
            renderSettingsTab(matrices, mouseX, mouseY, screenW, screenH);
        }

        // 3. Clean Bottom Bar (ONLY character avatar on bottom right)
        renderBottomBar(matrices, mouseX, mouseY, screenW, screenH);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void renderFallbackBackground(MatrixStack matrices, int screenW, int screenH) {
        this.fillGradient(matrices, 0, 0, screenW, screenH, 0xFF1A1E26, 0xFF0C0E13);
    }

    private void drawIcon(MatrixStack matrices, Identifier tex, int x, int y, int size, int argb) {
        if (this.client == null) return;
        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        float r = ((argb >>> 16) & 0xFF) / 255.0f;
        float g = ((argb >>> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(r, g, b, a);
        this.client.getTextureManager().bindTexture(tex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        DrawableHelper.drawTexture(matrices, x, y, size, size, 0.0f, 0.0f, 64, 64, 64, 64);
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawLogoTexture(MatrixStack matrices, int x, int y, int size) {
        if (this.client == null) return;
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        this.client.getTextureManager().bindTexture(TEX_LOGO);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        DrawableHelper.drawTexture(matrices, x, y, size, size, 0.0f, 0.0f, 512, 512, 512, 512);
    }

    private void renderTopBar(MatrixStack matrices, int mouseX, int mouseY, int screenW) {
        DrawableHelper.fill(matrices, 0, 0, screenW, TOP_H, 0xDF16181C);
        DrawableHelper.fill(matrices, 0, TOP_H, screenW, TOP_H + 1, 0x20FFFFFF);

        // Real SpaceRust project icon on Top-Left
        drawLogoTexture(matrices, 10, 5, 22);

        // Top Center: ONLY ГЛАВНАЯ and ИГРАТЬ
        int tabW = 92;
        int gap = 2;
        int centerX = screenW / 2;
        int homeTabX = centerX - tabW - (gap / 2);
        int playTabX = centerX + (gap / 2);

        drawTopNavTab(matrices, mouseX, mouseY, homeTabX, 0, tabW, TOP_H, TEX_HOME, MenuTab.HOME.title, currentTab == MenuTab.HOME);
        drawTopNavTab(matrices, mouseX, mouseY, playTabX, 0, tabW, TOP_H, TEX_PLAY, MenuTab.PLAY.title, currentTab == MenuTab.PLAY);

        // Top Right: ONLY НАСТРОЙКИ and ВЫЙТИ (No bell icon)
        int setW = 86;
        int exitW = 66;
        int exitX = screenW - exitW - 10;
        int setX = exitX - setW - 5;
        int btnY = 5;
        int btnH = 22;

        boolean setActive = currentTab == MenuTab.SETTINGS;
        boolean setHov = inRect(mouseX, mouseY, setX, btnY, setW, btnH);
        int setBg = setActive ? 0xEE7C3629 : (setHov ? 0x663A3E45 : 0x5522252A);
        DrawableHelper.fill(matrices, setX, btnY, setX + setW, btnY + btnH, setBg);

        RustFontRenderer bf = RustFontRenderer.boldSmall();
        float stw = bf.getWidth(MenuTab.SETTINGS.title);
        int sIcon = 10;
        float sTotal = sIcon + 5 + stw;
        int sStart = Math.round(setX + (setW - sTotal) / 2f);
        int sCol = (setActive || setHov) ? 0xFFFFFFFF : 0xFFD0D4DA;
        drawIcon(matrices, TEX_SETTINGS, sStart, btnY + (btnH - sIcon) / 2, sIcon, sCol);
        bf.drawCenteredY(matrices, MenuTab.SETTINGS.title, sStart + sIcon + 5, btnY, btnH, sCol);

        boolean exitHov = inRect(mouseX, mouseY, exitX, btnY, exitW, btnH);
        int exitBg = exitHov ? 0xDDB72820 : 0x5522252A;
        DrawableHelper.fill(matrices, exitX, btnY, exitX + exitW, btnY + btnH, exitBg);
        String exitTxt = "ВЫЙТИ";
        float etw = bf.getWidth(exitTxt);
        int eIcon = 10;
        float eTotal = eIcon + 5 + etw;
        int eStart = Math.round(exitX + (exitW - eTotal) / 2f);
        int eCol = exitHov ? 0xFFFFFFFF : 0xFFD0D4DA;
        drawIcon(matrices, TEX_EXIT, eStart, btnY + (btnH - eIcon) / 2, eIcon, eCol);
        bf.drawCenteredY(matrices, exitTxt, eStart + eIcon + 5, btnY, btnH, eCol);
    }

    private void drawTopNavTab(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, int h,
                               Identifier icon, String title, boolean active) {
        boolean hov = inRect(mouseX, mouseY, x, y, w, h);
        int bg = active ? 0xEE7C3629 : (hov ? 0x5530343B : 0x331E2024);
        DrawableHelper.fill(matrices, x, y, x + w, y + h, bg);

        RustFontRenderer bf = RustFontRenderer.bold();
        float tw = bf.getWidth(title);
        int iconSize = 11;
        float totalW = iconSize + 6 + tw;
        int startX = Math.round(x + (w - totalW) / 2f);
        int col = active ? 0xFFFFFFFF : (hov ? 0xFFEAECEF : 0xFFB8BDC4);

        drawIcon(matrices, icon, startX, y + (h - iconSize) / 2, iconSize, col);
        bf.drawCenteredY(matrices, title, startX + iconSize + 6, y, h, col);
    }

    private void renderBottomBar(MatrixStack matrices, int mouseX, int mouseY, int screenW, int screenH) {
        int barY = screenH - BOTTOM_H;
        DrawableHelper.fill(matrices, 0, barY, screenW, screenH, 0x99121418);
        DrawableHelper.fill(matrices, 0, barY, screenW, barY + 1, 0x18FFFFFF);

        // Subtle FPS on bottom-left
        String fpsText = MinecraftClient.getInstance().fpsDebugString;
        if (fpsText != null && !fpsText.isEmpty()) {
            int sp = fpsText.indexOf(' ');
            String shortFps = (sp > 0 ? fpsText.substring(0, sp) : fpsText) + " FPS";
            RustFontRenderer.small().drawCenteredY(matrices, shortFps, 10, barY, BOTTOM_H, 0x77AAAAAA);
        }

        // Bottom-Right: Static Character Avatar (no hover reaction, no race/nick tooltip)
        RustCharacterManager.CharacterProfile profile = RustCharacterManager.getLocalProfile();
        int avSize = 20;
        int avX = screenW - avSize - 10;
        int avY = barY + 3;

        DrawableHelper.fill(matrices, avX - 1, avY - 1, avX + avSize + 1, avY + avSize + 1, 0x44FFFFFF);
        DrawableHelper.fill(matrices, avX, avY, avX + avSize, avY + avSize, 0xFF1A1D22);

        if (this.client != null && profile != null && profile.textureId != null) {
            this.client.getTextureManager().bindTexture(profile.textureId);
            RenderSystem.enableTexture();
            RenderSystem.enableBlend();
            RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
            DrawableHelper.drawTexture(matrices, avX, avY, avSize, avSize, 8.0f, 8.0f, 8, 8, 64, 64);
            DrawableHelper.drawTexture(matrices, avX, avY, avSize, avSize, 40.0f, 8.0f, 8, 8, 64, 64);
        }
    }

    // =========================================================================
    // VIEW 1: ГЛАВНАЯ (Rust Home — Video BG + Quick Connect top-right only)
    // =========================================================================
    private void renderHomeTab(MatrixStack matrices, int mouseX, int mouseY, int screenW, int screenH) {
        int qcW = 264;
        int qcX = screenW - qcW - 18;
        int qcY = TOP_H + 18;

        // Crisp dark header strip so "БЫСТРОЕ ПОДКЛЮЧЕНИЕ" is clearly visible over bright skies/snow
        int hdrH = 18;
        DrawableHelper.fill(matrices, qcX, qcY, qcX + qcW, qcY + hdrH, 0xAA13161B);
        drawIcon(matrices, TEX_HISTORY, qcX + 6, qcY + 4, 10, 0xFFE6EAEE);
        RustFontRenderer.boldSmall().drawCenteredShadowY(matrices, "БЫСТРОЕ ПОДКЛЮЧЕНИЕ", qcX + 20, qcY, hdrH, 0xFFE6EAEE);

        int cardY = qcY + hdrH + 3;
        int cardH = 40;
        int cardGap = 4;
        int count = Math.min(3, RECENT_SERVERS.size());

        for (int i = 0; i < count; i++) {
            ServerEntry s = RECENT_SERVERS.get(i);
            int y = cardY + i * (cardH + cardGap);
            boolean cardHov = inRect(mouseX, mouseY, qcX, y, qcW, cardH);

            DrawableHelper.fill(matrices, qcX, y, qcX + qcW, y + cardH, cardHov ? 0xE22A2D34 : 0xCC1C1F24);

            RustFontRenderer bf = RustFontRenderer.bold();
            String title = bf.trim(s.name, qcW - 46);
            bf.drawCenteredShadowY(matrices, title, qcX + 9, y + 4, 14, 0xFFFFFFFF);

            // Sub-pills row
            float px = qcX + 9;
            int py = y + 21;
            px += drawMiniPill(matrices, px, py, s.modeBadge, 0xFF2F333A, 0xFFC8CDD4) + 4;
            px += drawMiniPill(matrices, px, py, s.wipeTag, 0xFF2F333A, 0xFFC8CDD4) + 6;

            String stat = s.isOnline
                    ? ("● " + s.currentPlayers + "/" + s.maxPlayers)
                    : (s.isPinging ? "● ОПРОС..." : "● ОФФЛАЙН");
            int statCol = s.isOnline ? 0xFF8BC34A : 0xFF9EA5AE;
            RustFontRenderer.small().drawCenteredY(matrices, stat, px, py, 13, statCol);

            // Right Play square button
            int playBtnSize = 24;
            int pbX = qcX + qcW - playBtnSize - 8;
            int pbY = y + (cardH - playBtnSize) / 2;
            boolean pbHov = inRect(mouseX, mouseY, pbX, pbY, playBtnSize, playBtnSize);
            DrawableHelper.fill(matrices, pbX, pbY, pbX + playBtnSize, pbY + playBtnSize,
                    pbHov ? 0xFFCD412B : (cardHov ? 0xFF3E434C : 0xFF2B2F36));
            drawIcon(matrices, TEX_PLAY_ARROW, pbX + 7, pbY + 7, 10, 0xFFEAECEF);
        }
    }

    private float drawMiniPill(MatrixStack matrices, float x, int y, String label, int bg, int fg) {
        RustFontRenderer sf = RustFontRenderer.boldSmall();
        float w = sf.getWidth(label) + 10;
        int h = 13;
        int ix = Math.round(x);
        DrawableHelper.fill(matrices, ix, y, ix + Math.round(w), y + h, bg);
        sf.drawCenteredY(matrices, label, x + 5.0f, y, h, fg);
        return w;
    }

    // =========================================================================
    // VIEW 2: ИГРАТЬ (Screenshot 1 Server Browser)
    // =========================================================================
    private void renderPlayTab(MatrixStack matrices, int mouseX, int mouseY, int screenW, int screenH) {
        int subY = TOP_H + 8;
        int subH = 22;
        int sx = 14;

        int officialOnline = 0;
        int moddedOnline = 0;
        int favCount = 0;
        int histCount = 0;
        for (ServerEntry s : SERVERS) {
            if (s.isModded) moddedOnline += s.isOnline ? s.currentPlayers : 0;
            else officialOnline += s.isOnline ? s.currentPlayers : 0;
            if (s.isFavorite) favCount++;
            if (s.inHistory) histCount++;
        }

        sx += drawPlayCategoryPill(matrices, mouseX, mouseY, sx, subY, subH,
                TEX_OFFICIAL, PlayCategory.OFFICIAL, String.valueOf(officialOnline), true) + 5;
        sx += drawPlayCategoryPill(matrices, mouseX, mouseY, sx, subY, subH,
                TEX_MODDED, PlayCategory.MODDED, String.valueOf(moddedOnline), false) + 5;
        sx += drawPlayCategoryPill(matrices, mouseX, mouseY, sx, subY, subH,
                TEX_HEART, PlayCategory.FAVORITES, String.valueOf(favCount), false) + 5;
        drawPlayCategoryPill(matrices, mouseX, mouseY, sx, subY, subH,
                TEX_HISTORY, PlayCategory.HISTORY, String.valueOf(histCount), false);

        // Search box on far right
        int searchW = 156;
        int searchX = screenW - searchW - 14;
        DrawableHelper.fill(matrices, searchX, subY, searchX + searchW, subY + subH, 0xCC1A1D22);
        DrawableHelper.fill(matrices, searchX, subY + subH - 1, searchX + searchW, subY + subH,
                isSearchFocused ? 0xFFCD412B : 0x28FFFFFF);
        RustFontRenderer rf = RustFontRenderer.regular();
        if (serverSearchQuery.isEmpty()) {
            rf.drawCenteredY(matrices, isSearchFocused ? "|" : "ПОИСК...", searchX + 8, subY, subH, 0x66AAAAAA);
        } else {
            String disp = rf.trim(serverSearchQuery, searchW - 26) + (isSearchFocused ? "|" : "");
            rf.drawCenteredY(matrices, disp, searchX + 8, subY, subH, 0xFFFFFFFF);
        }
        drawIcon(matrices, TEX_SEARCH, searchX + searchW - 17, subY + 6, 10, 0x88AAAAAA);

        // Left Filter Sidebar (clean: ФИЛЬТРЫ, ЧАСТОТА ВАЙПА, action buttons)
        int sideX = 14;
        int sideY = subY + subH + 10;
        int sideW = 148;
        int sideBottom = screenH - BOTTOM_H - 8;
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();

        int curY = sideY;
        // 1. ФИЛЬТРЫ
        curY += drawCollapsibleHeader(matrices, mouseX, mouseY, sideX, curY, sideW, "ФИЛЬТРЫ", expandFilters);
        if (expandFilters) {
            boolean onlHov = inRect(mouseX, mouseY, sideX, curY, sideW, 18);
            DrawableHelper.fill(matrices, sideX, curY, sideX + sideW, curY + 18,
                    onlyOnlineFilter ? 0xFF3A4A28 : (onlHov ? 0x33FFFFFF : 0x22181B20));
            // Crisp drawn square checkbox (no missing Unicode glyphs)
            int cbX = sideX + 7;
            int cbY = curY + 5;
            DrawableHelper.fill(matrices, cbX, cbY, cbX + 8, cbY + 8, 0xAAFFFFFF);
            DrawableHelper.fill(matrices, cbX + 1, cbY + 1, cbX + 7, cbY + 7, onlyOnlineFilter ? 0xFF7BC950 : 0xFF1A1D22);
            bfSmall.drawCenteredY(matrices, "ТОЛЬКО ОНЛАЙН", sideX + 19, curY, 18,
                    onlyOnlineFilter ? 0xFFAED581 : 0xFFC2C7CE);
            curY += 20;
        }
        curY += 4;

        // 2. ЧАСТОТА ВАЙПА
        curY += drawCollapsibleHeader(matrices, mouseX, mouseY, sideX, curY, sideW, "ЧАСТОТА ВАЙПА", expandWipe);
        if (expandWipe) {
            boolean wHov = inRect(mouseX, mouseY, sideX, curY, sideW, 18);
            DrawableHelper.fill(matrices, sideX, curY, sideX + sideW, curY + 18, wHov ? 0x33FFFFFF : 0x22181B20);
            bfSmall.drawCenteredY(matrices, WIPE_OPTIONS[wipeIndex], sideX + 7, curY, 18, 0xFFE0E4EA);
        }

        // Bottom of Left Sidebar
        int utilBtnH = 20;
        int utilBtnY = sideBottom - utilBtnH;

        int resetW = 71;
        int refW = sideW - resetW - 4;
        drawSidebarBtn(matrices, mouseX, mouseY, sideX, utilBtnY, resetW, utilBtnH, TEX_RESET, "СБРОС",
                0xAA252930, 0xCC353B45, 0xFFD0D5DC);
        drawSidebarBtn(matrices, mouseX, mouseY, sideX + resetW + 4, utilBtnY, refW, utilBtnH, TEX_REFRESH, "ОБНОВИТЬ",
                0xAA252930, 0xCC353B45, 0xFFD0D5DC);

        // ==========================================
        // SERVER TABLE
        // ==========================================
        int tableX = sideX + sideW + 12;
        int tableW = screenW - tableX - 14;
        int headerY = sideY;
        int headerH = 18;

        DrawableHelper.fill(matrices, tableX, headerY, tableX + tableW, headerY + headerH, 0xEE14171C);
        drawIcon(matrices, TEX_HEART, tableX + 8, headerY + 5, 8, 0x88AAAAAA);
        bfSmall.drawCenteredY(matrices, "НАЗВАНИЕ", tableX + 26, headerY, headerH, 0xFF8F96A0);
        int colModeX = tableX + (int) (tableW * 0.57f);
        int colPlayersX = tableX + (int) (tableW * 0.71f);
        int colPingX = tableX + (int) (tableW * 0.83f);
        bfSmall.drawCenteredY(matrices, "РЕЖИМ", colModeX, headerY, headerH, 0xFF8F96A0);
        bfSmall.drawCenteredY(matrices, "ИГРОКИ", colPlayersX, headerY, headerH, 0xFF8F96A0);
        bfSmall.drawCenteredY(matrices, "ПИНГ", colPingX, headerY, headerH, 0xFF8F96A0);

        List<ServerEntry> filtered = getFilteredServers();
        int rowY = headerY + headerH + 4;
        int rowH = 40;
        int rowGap = 3;

        if (filtered.isEmpty()) {
            DrawableHelper.fill(matrices, tableX, rowY, tableX + tableW, rowY + 34, 0x9916191F);
            String empty = "Нет серверов, подходящих под выбранные фильтры.";
            float ew = rf.getWidth(empty);
            rf.drawCenteredY(matrices, empty, tableX + (tableW - ew) / 2f, rowY, 34, 0x88AAAAAA);
            return;
        }

        RustFontRenderer bf = RustFontRenderer.bold();
        RustFontRenderer sf = RustFontRenderer.small();
        for (int i = 0; i < filtered.size(); i++) {
            ServerEntry s = filtered.get(i);
            int y = rowY + i * (rowH + rowGap);
            if (y + rowH > sideBottom) break;

            boolean isHover = inRect(mouseX, mouseY, tableX, y, tableW, rowH);
            DrawableHelper.fill(matrices, tableX, y, tableX + tableW, y + rowH, isHover ? 0xDD22262E : 0xB8171A1F);
            DrawableHelper.fill(matrices, tableX, y + rowH - 1, tableX + tableW, y + rowH, 0x16FFFFFF);
            DrawableHelper.fill(matrices, tableX, y, tableX + 2, y + rowH, s.isOnline ? 0xFF5EC739 : 0xFFCD412B);

            // Favorite Heart
            drawIcon(matrices, TEX_HEART, tableX + 8, y + (rowH - 9) / 2, 9, s.isFavorite ? 0xFFCD412B : 0x66AAAAAA);

            // Server Name
            bf.drawCenteredShadowY(matrices, s.name, tableX + 26, y + 4, 14, 0xFFFFFFFF);

            // Tag pills
            float tx = tableX + 26;
            int ty = y + 21;
            for (int t = 0; t < s.tags.length; t++) {
                String tag = s.tags[t];
                boolean isGold = (t == 0);
                int tBg = isGold ? 0xFFA97B2A : 0xFF2B2F36;
                int tFg = isGold ? 0xFFFFE082 : 0xFFB0B6BE;
                tx += drawMiniPill(matrices, tx, ty, tag, tBg, tFg) + 4;
            }
            sf.drawCenteredY(matrices, s.map, tx + 3, ty, 13, 0x88AAAAAA);

            // Mode Pill
            drawMiniPill(matrices, colModeX, y + (rowH - 13) / 2, s.modeBadge, 0xFF435E24, 0xFFAED581);

            // Players
            String countStr = s.isOnline ? (s.currentPlayers + " / " + s.maxPlayers) : "ОФФЛАЙН";
            int countCol = s.isOnline ? 0xFFFFFFFF : 0xFFD9655A;
            bfSmall.drawCenteredY(matrices, countStr, colPlayersX, y, rowH, countCol);

            // Ping
            String pingStr = s.isOnline ? (s.pingMs + " ms") : "-- ms";
            int pingCol = s.isOnline ? 0xFF8BC34A : 0x77AAAAAA;
            bfSmall.drawCenteredY(matrices, pingStr, colPingX, y, rowH, pingCol);

            // Join button
            int btnW = 66;
            int btnH = 22;
            int btnX = tableX + tableW - btnW - 8;
            int btnY = y + (rowH - btnH) / 2;
            boolean hovBtn = inRect(mouseX, mouseY, btnX, btnY, btnW, btnH);
            int bBg = hovBtn ? 0xFFE04B32 : (isHover || s.isOnline ? 0xFFCD412B : 0xFF282C34);
            DrawableHelper.fill(matrices, btnX, btnY, btnX + btnW, btnY + btnH, bBg);
            float btw = bfSmall.getWidth("ИГРАТЬ");
            bfSmall.drawCenteredY(matrices, "ИГРАТЬ", btnX + (btnW - btw) / 2f, btnY, btnH, 0xFFFFFFFF);
        }
    }

    private int drawPlayCategoryPill(MatrixStack matrices, int mouseX, int mouseY,
                                     int x, int y, int h, Identifier icon, PlayCategory cat,
                                     String badge, boolean greenTheme) {
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        float titleW = bf.getWidth(cat.title);
        float badgeW = bf.getWidth(badge);
        int iconSize = 9;
        int pad = 8;
        int badgeBoxW = Math.round(badgeW) + 8;
        int w = pad + iconSize + 5 + Math.round(titleW) + 6 + badgeBoxW + 6;

        boolean active = selectedPlayCategory == cat;
        boolean hov = inRect(mouseX, mouseY, x, y, w, h);

        int bg;
        int fg;
        if (active) {
            bg = greenTheme ? 0xFF486327 : 0xFF7C3629;
            fg = greenTheme ? 0xFFAED581 : 0xFFFFFFFF;
        } else {
            bg = hov ? 0x44FFFFFF : 0xAA1D2026;
            fg = hov ? 0xFFFFFFFF : 0xFFB8BDC4;
        }
        DrawableHelper.fill(matrices, x, y, x + w, y + h, bg);
        drawIcon(matrices, icon, x + pad, y + (h - iconSize) / 2, iconSize, fg);
        bf.drawCenteredY(matrices, cat.title, x + pad + iconSize + 5, y, h, fg);

        int bx = x + w - badgeBoxW - 4;
        int by = y + 4;
        int bh = h - 8;
        DrawableHelper.fill(matrices, bx, by, bx + badgeBoxW, by + bh, 0x44000000);
        bf.drawCenteredY(matrices, badge, bx + 4, by, bh, fg);
        return w;
    }

    private int drawCollapsibleHeader(MatrixStack matrices, int mouseX, int mouseY,
                                      int x, int y, int w, String title, boolean expanded) {
        int h = 19;
        boolean hov = inRect(mouseX, mouseY, x, y, w, h);
        DrawableHelper.fill(matrices, x, y, x + w, y + h, hov ? 0x33FFFFFF : 0x881A1D22);
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        bf.drawCenteredY(matrices, title, x + 7, y, h, 0xFFC4C9D0);
        bf.drawCenteredY(matrices, expanded ? "−" : "+", x + w - 12, y, h, 0xFF9EA5AE);
        return h + 2;
    }

    private void drawSidebarBtn(MatrixStack matrices, int mouseX, int mouseY,
                                int x, int y, int w, int h, Identifier icon, String label,
                                int bg, int hovBg, int fg) {
        boolean hov = inRect(mouseX, mouseY, x, y, w, h);
        DrawableHelper.fill(matrices, x, y, x + w, y + h, hov ? hovBg : bg);
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        float tw = bf.getWidth(label);
        int iconS = 9;
        float total = iconS + 5 + tw;
        int sx = Math.round(x + (w - total) / 2f);
        drawIcon(matrices, icon, sx, y + (h - iconS) / 2, iconS, fg);
        bf.drawCenteredY(matrices, label, sx + iconS + 5, y, h, fg);
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

    // =========================================================================
    // VIEW 3: НАСТРОЙКИ (Authentic Rust Settings with Roboto Condensed)
    // =========================================================================
    private void renderSettingsTab(MatrixStack matrices, int mouseX, int mouseY, int screenW, int screenH) {
        int catW = 108;
        int catX = 14;
        int startY = TOP_H + 12;

        int sBoxH = 18;
        DrawableHelper.fill(matrices, catX, startY, catX + catW, startY + sBoxH, 0xAA12151B);
        DrawableHelper.fill(matrices, catX, startY, catX + catW, startY + 1, 0x22FFFFFF);
        RustFontRenderer.regular().drawCenteredY(matrices, "ПОИСК...", catX + 7, startY, sBoxH, 0x55AAAAAA);

        int curCatY = startY + sBoxH + 8;
        int itemH = 19;
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();

        for (SettingsCategory cat : SettingsCategory.values()) {
            boolean active = cat == selectedSettingCategory;
            boolean hov = inRect(mouseX, mouseY, catX, curCatY, catW, itemH);
            int bg = active ? 0xFF5A7232 : (hov ? 0x33FFFFFF : 0x1A14171E);
            DrawableHelper.fill(matrices, catX, curCatY, catX + catW, curCatY + itemH, bg);
            int txtColor = active ? 0xFFFFFFFF : (hov ? 0xFFE0E5EC : 0x88AAAAAA);
            bfSmall.drawCenteredY(matrices, cat.title, catX + 8, curCatY, itemH, txtColor);
            curCatY += itemH + 3;
        }

        int centerColX = catX + catW + 16;
        int descColW = 200;
        int centerColW = screenW - centerColX - descColW - 20;

        RustFontRenderer.boldLarge().draw(matrices, selectedSettingCategory.title, centerColX, startY + 1, 0xFFFFFFFF);

        int optY = startY + 24;
        int rowH = 22;

        if (selectedSettingCategory == SettingsCategory.GAME) {
            drawSliderRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ПОЛЕ ЗРЕНИЯ",
                    String.valueOf((int) this.client.options.fov), (float) (this.client.options.fov - 30.0) / 80.0f,
                    "Настройка угла обзора камеры в игре (FOV). Более высокий угол дает широкий обзор.");
            optY += rowH;

            drawToggleRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ПОКАЧИВАНИЕ КАМЕРЫ",
                    this.client.options.bobView,
                    "Включает или выключает реалистичное покачивание рук и предметов при ходьбе.");
            optY += rowH;

            drawToggleRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "АВТОПРЫЖОК",
                    this.client.options.autoJump,
                    "Автоматический прыжок при подходе к блокам высотой в 1 блок.");
            optY += rowH;

            drawCycleRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "СЛОЖНОСТЬ",
                    this.client.options.difficulty.getName(),
                    "Уровень сложности игрового мира и урон от окружения.");
            optY += rowH;

            drawInfoRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "МАСШТАБ ИНТЕРФЕЙСА",
                    "ФИКСИРОВАН 2 (RUST UI)", "Размер интерфейса зафиксирован для отображения Rust UI.");

        } else if (selectedSettingCategory == SettingsCategory.AUDIO) {
            drawVolumeSlider(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ОБЩАЯ ГРОМКОСТЬ", SoundCategory.MASTER,
                    "Регулировка главной громкости всех звуков в игре.");
            optY += rowH;
            drawVolumeSlider(matrices, mouseX, mouseY, centerColX, optY, centerColW, "МУЗЫКА", SoundCategory.MUSIC,
                    "Громкость фоновой музыки в игре.");
            optY += rowH;
            drawVolumeSlider(matrices, mouseX, mouseY, centerColX, optY, centerColW, "БЛОКИ И ШАГИ", SoundCategory.BLOCKS,
                    "Громкость шагов, ломания и установки строительных блоков.");
            optY += rowH;
            drawVolumeSlider(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ИГРОКИ", SoundCategory.PLAYERS,
                    "Звуки других игроков, ударов и перестрелок.");
            optY += rowH;
            drawVolumeSlider(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ОКРУЖАЮЩАЯ СРЕДА", SoundCategory.AMBIENT,
                    "Звуки ветра, природы и ночной атмосферы Rust.");

        } else if (selectedSettingCategory == SettingsCategory.DISPLAY || selectedSettingCategory == SettingsCategory.GRAPHICS) {
            drawToggleRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ПОЛНОЭКРАННЫЙ РЕЖИМ",
                    this.client.options.fullscreen,
                    "Полноэкранный режим работы дисплея без рамок Windows.");
            optY += rowH;
            drawToggleRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ВЕРТИКАЛЬНАЯ СИНХРОНИЗАЦИЯ (VSync)",
                    this.client.options.enableVsync,
                    "Синхронизация частоты кадров с герцовкой вашего монитора.");
            optY += rowH;

            int fps = this.client.options.maxFps;
            String fpsStr = fps >= 260 ? "МАКСИМУМ" : (fps + " FPS");
            drawSliderRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ЧАСТОТА КАДРОВ (FPS)",
                    fpsStr, (float) (fps - 10) / 250.0f,
                    "Максимальный лимит кадров в секунду (FPS).");
            optY += rowH;

            int dist = this.client.options.viewDistance;
            drawSliderRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ДАЛЬНОСТЬ ПРОРИСОВКИ",
                    dist + " ЧАНКОВ", (float) (dist - 2) / 30.0f,
                    "Дальность видимости мира в чанках.");
            optY += rowH;

            drawCycleRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ГРАФИКА",
                    this.client.options.graphicsMode == GraphicsMode.FAST ? "БЫСТРО" : "ДЕТАЛЬНО",
                    "Качество отрисовки листьев, прозрачности и частиц мира.");

        } else if (selectedSettingCategory == SettingsCategory.CONTROLS) {
            int sens = (int) (this.client.options.mouseSensitivity * 200);
            drawSliderRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ЧУВСТВИТЕЛЬНОСТЬ МЫШИ",
                    sens + "%", (float) this.client.options.mouseSensitivity,
                    "Скорость поворота прицела и камеры при движении мыши.");
            optY += rowH;
            drawToggleRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ИНВЕРСИЯ МЫШИ",
                    this.client.options.invertYMouse,
                    "Инвертирует вертикальную ось движения мыши.");
            optY += rowH;
            drawButtonRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "НАЗНАЧЕНИЕ КЛАВИШ...", "ОТКРЫТЬ КЛАВИШИ",
                    "Настройка кнопок управления персонажем, стрельбы и движения.");

        } else if (selectedSettingCategory == SettingsCategory.LANGUAGE) {
            drawButtonRow(matrices, mouseX, mouseY, centerColX, optY, centerColW, "ЯЗЫК ИНТЕРФЕЙСА...", "ВЫБРАТЬ ЯЗЫК",
                    "Смена языка интерфейса Minecraft и мода.");
        }

        int descX = screenW - descColW - 14;
        int descY = startY + 24;
        DrawableHelper.fill(matrices, descX, descY, descX + 2, descY + 120, 0xFFCD412B);
        RustFontRenderer.bold().draw(matrices, hoveredSettingTitle, descX + 8, descY, 0xFFFFFFFF);

        RustFontRenderer rf = RustFontRenderer.regular();
        List<String> dLines = wrapText(hoveredSettingDesc, descColW - 16, rf);
        int dy = descY + 16;
        for (String line : dLines) {
            rf.draw(matrices, line, descX + 8, dy, 0xFF9EA5AE);
            dy += 12;
        }
    }

    private void drawSliderRow(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, String title, String valStr, float ratio, String desc) {
        if (mouseY >= y && mouseY <= y + 18 && mouseX >= x && mouseX <= x + w) {
            hoveredSettingTitle = title;
            hoveredSettingDesc = desc;
        }
        RustFontRenderer rf = RustFontRenderer.regular();
        rf.drawCenteredY(matrices, title, x, y, 18, 0xFFE0E5EC);

        int trackW = 100;
        int trackH = 4;
        int trackX = x + w - trackW;
        int trackY = y + 7;

        float valW = rf.getWidth(valStr);
        rf.drawCenteredY(matrices, valStr, trackX - valW - 8, y, 18, 0xFFFFFFFF);
        DrawableHelper.fill(matrices, trackX, trackY, trackX + trackW, trackY + trackH, 0x55000000);

        int fill = (int) (trackW * Math.max(0.0f, Math.min(1.0f, ratio)));
        if (fill > 0) {
            DrawableHelper.fill(matrices, trackX, trackY, trackX + fill, trackY + trackH, 0xFFCD412B);
        }
    }

    private void drawVolumeSlider(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, String title, SoundCategory cat, String desc) {
        float vol = this.client.options.getSoundVolume(cat);
        int percent = (int) (vol * 100);
        drawSliderRow(matrices, mouseX, mouseY, x, y, w, title, percent + "%", vol, desc);
    }

    private void drawToggleRow(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, String title, boolean value, String desc) {
        if (mouseY >= y && mouseY <= y + 18 && mouseX >= x && mouseX <= x + w) {
            hoveredSettingTitle = title;
            hoveredSettingDesc = desc;
        }
        RustFontRenderer.regular().drawCenteredY(matrices, title, x, y, 18, 0xFFE0E5EC);

        int btnW = 60;
        int btnH = 16;
        int btnX = x + w - btnW;
        int btnY = y + 1;
        boolean hov = inRect(mouseX, mouseY, btnX, btnY, btnW, btnH);
        int bg = value ? (hov ? 0xFF445729 : 0xFF354823) : (hov ? 0x44FFFFFF : 0x22181C22);
        DrawableHelper.fill(matrices, btnX, btnY, btnX + btnW, btnY + btnH, bg);

        String txt = value ? "ВКЛ" : "ВЫКЛ";
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        int col = value ? 0xFF8BC34A : 0x77AAAAAA;
        bf.drawCenteredY(matrices, txt, btnX + (btnW - bf.getWidth(txt)) / 2f, btnY, btnH, col);
    }

    private void drawCycleRow(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, String title, String valStr, String desc) {
        if (mouseY >= y && mouseY <= y + 18 && mouseX >= x && mouseX <= x + w) {
            hoveredSettingTitle = title;
            hoveredSettingDesc = desc;
        }
        RustFontRenderer.regular().drawCenteredY(matrices, title, x, y, 18, 0xFFE0E5EC);

        int btnW = 80;
        int btnH = 16;
        int btnX = x + w - btnW;
        int btnY = y + 1;
        boolean hov = inRect(mouseX, mouseY, btnX, btnY, btnW, btnH);
        DrawableHelper.fill(matrices, btnX, btnY, btnX + btnW, btnY + btnH, hov ? 0x33FFFFFF : 0x22181C22);

        RustFontRenderer bf = RustFontRenderer.boldSmall();
        bf.drawCenteredY(matrices, valStr, btnX + (btnW - bf.getWidth(valStr)) / 2f, btnY, btnH, 0xFFFFFFFF);
    }

    private void drawButtonRow(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, String title, String btnText, String desc) {
        if (mouseY >= y && mouseY <= y + 18 && mouseX >= x && mouseX <= x + w) {
            hoveredSettingTitle = title;
            hoveredSettingDesc = desc;
        }
        RustFontRenderer.regular().drawCenteredY(matrices, title, x, y, 18, 0xFFE0E5EC);

        int btnW = 90;
        int btnH = 16;
        int btnX = x + w - btnW;
        int btnY = y + 1;
        boolean hov = inRect(mouseX, mouseY, btnX, btnY, btnW, btnH);
        DrawableHelper.fill(matrices, btnX, btnY, btnX + btnW, btnY + btnH, hov ? 0xFF445729 : 0x22181C22);

        RustFontRenderer bf = RustFontRenderer.boldSmall();
        bf.drawCenteredY(matrices, btnText, btnX + (btnW - bf.getWidth(btnText)) / 2f, btnY, btnH, 0xFFFFFFFF);
    }

    private void drawInfoRow(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, String title, String valStr, String desc) {
        if (mouseY >= y && mouseY <= y + 18 && mouseX >= x && mouseX <= x + w) {
            hoveredSettingTitle = title;
            hoveredSettingDesc = desc;
        }
        RustFontRenderer.regular().drawCenteredY(matrices, title, x, y, 18, 0xFFE0E5EC);
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        bf.drawCenteredY(matrices, valStr, x + w - bf.getWidth(valStr), y, 18, 0xFFCD412B);
    }

    private List<String> wrapText(String text, float maxWidth, RustFontRenderer font) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            if (font.getWidth(cur + " " + w) > maxWidth) {
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
    // MOUSE & KEYBOARD INTERACTION
    // ==========================================
    private void playClickSound() {
        if (this.client != null && this.client.getSoundManager() != null) {
            this.client.getSoundManager().play(PositionedSoundInstance.master(SpaceRustClient.UI_CLICK_SOUND, 1.0f));
        }
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        if (currentTab == MenuTab.PLAY && isSearchFocused) {
            if (chr >= 32 && chr != 127 && serverSearchQuery.length() < 32) {
                serverSearchQuery += chr;
                return true;
            }
        }
        return super.charTyped(chr, keyCode);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int getCategoryPillWidth(PlayCategory cat, String badge) {
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        int badgeBoxW = Math.round(bf.getWidth(badge)) + 8;
        return 8 + 9 + 5 + Math.round(bf.getWidth(cat.title)) + 6 + badgeBoxW + 6;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int screenW = this.width;
        int screenH = this.height;

        // Top Bar Tabs
        int tabW = 92;
        int gap = 2;
        int centerX = screenW / 2;
        int homeTabX = centerX - tabW - (gap / 2);
        int playTabX = centerX + (gap / 2);

        if (inRect(mouseX, mouseY, homeTabX, 0, tabW, TOP_H)) {
            currentTab = MenuTab.HOME;
            isSearchFocused = false;
            playClickSound();
            return true;
        }
        if (inRect(mouseX, mouseY, playTabX, 0, tabW, TOP_H)) {
            currentTab = MenuTab.PLAY;
            isSearchFocused = false;
            playClickSound();
            return true;
        }

        // Top-Right: НАСТРОЙКИ & ВЫЙТИ
        int setW = 86;
        int exitW = 66;
        int exitX = screenW - exitW - 10;
        int setX = exitX - setW - 5;

        if (inRect(mouseX, mouseY, setX, 5, setW, 22)) {
            currentTab = MenuTab.SETTINGS;
            isSearchFocused = false;
            playClickSound();
            return true;
        }
        if (inRect(mouseX, mouseY, exitX, 5, exitW, 22)) {
            MenuVideoPlayer.getInstance().stop();
            this.client.scheduleStop();
            return true;
        }

        // HOME: Quick Connect clicks
        if (currentTab == MenuTab.HOME) {
            int qcW = 262;
            int qcX = screenW - qcW - 18;
            int cardY = TOP_H + 22 + 14;
            int cardH = 38;
            int cardGap = 4;
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

        // PLAY
        if (currentTab == MenuTab.PLAY) {
            int subY = TOP_H + 8;
            int subH = 22;
            int sx = 14;

            int officialOnline = 0;
            int moddedOnline = 0;
            int favCount = 0;
            int histCount = 0;
            for (ServerEntry s : SERVERS) {
                if (s.isModded) moddedOnline += s.isOnline ? s.currentPlayers : 0;
                else officialOnline += s.isOnline ? s.currentPlayers : 0;
                if (s.isFavorite) favCount++;
                if (s.inHistory) histCount++;
            }

            int wOff = getCategoryPillWidth(PlayCategory.OFFICIAL, String.valueOf(officialOnline));
            if (inRect(mouseX, mouseY, sx, subY, wOff, subH)) {
                selectedPlayCategory = PlayCategory.OFFICIAL;
                playClickSound();
                return true;
            }
            sx += wOff + 5;

            int wMod = getCategoryPillWidth(PlayCategory.MODDED, String.valueOf(moddedOnline));
            if (inRect(mouseX, mouseY, sx, subY, wMod, subH)) {
                selectedPlayCategory = PlayCategory.MODDED;
                playClickSound();
                return true;
            }
            sx += wMod + 5;

            int wFav = getCategoryPillWidth(PlayCategory.FAVORITES, String.valueOf(favCount));
            if (inRect(mouseX, mouseY, sx, subY, wFav, subH)) {
                selectedPlayCategory = PlayCategory.FAVORITES;
                playClickSound();
                return true;
            }
            sx += wFav + 5;

            int wHist = getCategoryPillWidth(PlayCategory.HISTORY, String.valueOf(histCount));
            if (inRect(mouseX, mouseY, sx, subY, wHist, subH)) {
                selectedPlayCategory = PlayCategory.HISTORY;
                playClickSound();
                return true;
            }

            // Search box
            int searchW = 156;
            int searchX = screenW - searchW - 14;
            isSearchFocused = inRect(mouseX, mouseY, searchX, subY, searchW, subH);
            if (isSearchFocused) {
                playClickSound();
                return true;
            }

            // Left Sidebar (matches renderPlayTab: ФИЛЬТРЫ, ЧАСТОТА ВАЙПА, action buttons)
            int sideX = 14;
            int sideY = subY + subH + 10;
            int sideW = 148;
            int sideBottom = screenH - BOTTOM_H - 8;

            int curY = sideY;
            if (inRect(mouseX, mouseY, sideX, curY, sideW, 19)) {
                expandFilters = !expandFilters;
                playClickSound();
                return true;
            }
            curY += 21;
            if (expandFilters) {
                if (inRect(mouseX, mouseY, sideX, curY, sideW, 18)) {
                    onlyOnlineFilter = !onlyOnlineFilter;
                    playClickSound();
                    return true;
                }
                curY += 20;
            }
            curY += 4;

            if (inRect(mouseX, mouseY, sideX, curY, sideW, 19)) {
                expandWipe = !expandWipe;
                playClickSound();
                return true;
            }
            curY += 21;
            if (expandWipe && inRect(mouseX, mouseY, sideX, curY, sideW, 18)) {
                wipeIndex = (wipeIndex + 1) % WIPE_OPTIONS.length;
                playClickSound();
                return true;
            }

            int utilBtnH = 20;
            int utilBtnY = sideBottom - utilBtnH;
            int resetW = 71;
            int refW = sideW - resetW - 4;

            if (inRect(mouseX, mouseY, sideX, utilBtnY, resetW, utilBtnH)) {
                wipeIndex = 0;
                onlyOnlineFilter = false;
                serverSearchQuery = "";
                playClickSound();
                return true;
            }
            if (inRect(mouseX, mouseY, sideX + resetW + 4, utilBtnY, refW, utilBtnH)) {
                refreshServers();
                playClickSound();
                return true;
            }

            // Server rows
            int tableX = sideX + sideW + 12;
            int tableW = screenW - tableX - 14;
            int rowY = sideY + 18 + 4;
            int rowH = 40;
            int rowGap = 3;
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

                int btnW = 66;
                int btnH = 22;
                int btnX = tableX + tableW - btnW - 8;
                int btnY = y + (rowH - btnH) / 2;
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

        // SETTINGS
        if (currentTab == MenuTab.SETTINGS) {
            int catW = 108;
            int catX = 14;
            int startY = TOP_H + 12;
            int curCatY = startY + 18 + 8;
            int itemH = 19;

            for (SettingsCategory cat : SettingsCategory.values()) {
                if (inRect(mouseX, mouseY, catX, curCatY, catW, itemH)) {
                    selectedSettingCategory = cat;
                    playClickSound();
                    return true;
                }
                curCatY += itemH + 3;
            }

            int centerColX = catX + catW + 16;
            int descColW = 200;
            int centerColW = screenW - centerColX - descColW - 20;
            int optY = startY + 24;
            int rowH = 22;

            if (selectedSettingCategory == SettingsCategory.GAME) {
                if (inRect(mouseX, mouseY, centerColX + centerColW - 100, optY + 2, 100, 14)) {
                    activeSliderId = "fov";
                    updateSliderFromMouse(mouseX, centerColX + centerColW - 100, 100, "fov");
                    return true;
                }
                optY += rowH;
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 60, optY + 1, 60, 16)) {
                    this.client.options.bobView = !this.client.options.bobView;
                    saveOptions();
                    return true;
                }
                optY += rowH;
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 60, optY + 1, 60, 16)) {
                    this.client.options.autoJump = !this.client.options.autoJump;
                    saveOptions();
                    return true;
                }
                optY += rowH;
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 80, optY + 1, 80, 16)) {
                    int nextId = (this.client.options.difficulty.getId() + 1) % 4;
                    this.client.options.difficulty = Difficulty.byOrdinal(nextId);
                    saveOptions();
                    return true;
                }
            } else if (selectedSettingCategory == SettingsCategory.AUDIO) {
                String[] cats = {"vol_MASTER", "vol_MUSIC", "vol_BLOCKS", "vol_PLAYERS", "vol_AMBIENT"};
                for (String cid : cats) {
                    if (inRect(mouseX, mouseY, centerColX + centerColW - 100, optY + 2, 100, 14)) {
                        activeSliderId = cid;
                        updateSliderFromMouse(mouseX, centerColX + centerColW - 100, 100, cid);
                        return true;
                    }
                    optY += rowH;
                }
            } else if (selectedSettingCategory == SettingsCategory.DISPLAY || selectedSettingCategory == SettingsCategory.GRAPHICS) {
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 60, optY + 1, 60, 16)) {
                    this.client.options.fullscreen = !this.client.options.fullscreen;
                    if (this.client.getWindow().isFullscreen() != this.client.options.fullscreen) {
                        this.client.getWindow().toggleFullscreen();
                    }
                    saveOptions();
                    return true;
                }
                optY += rowH;
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 60, optY + 1, 60, 16)) {
                    this.client.options.enableVsync = !this.client.options.enableVsync;
                    this.client.getWindow().setVsync(this.client.options.enableVsync);
                    saveOptions();
                    return true;
                }
                optY += rowH;
                if (inRect(mouseX, mouseY, centerColX + centerColW - 100, optY + 2, 100, 14)) {
                    activeSliderId = "maxFps";
                    updateSliderFromMouse(mouseX, centerColX + centerColW - 100, 100, "maxFps");
                    return true;
                }
                optY += rowH;
                if (inRect(mouseX, mouseY, centerColX + centerColW - 100, optY + 2, 100, 14)) {
                    activeSliderId = "viewDistance";
                    updateSliderFromMouse(mouseX, centerColX + centerColW - 100, 100, "viewDistance");
                    return true;
                }
                optY += rowH;
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 80, optY + 1, 80, 16)) {
                    this.client.options.graphicsMode = this.client.options.graphicsMode == GraphicsMode.FAST ? GraphicsMode.FANCY : GraphicsMode.FAST;
                    saveOptions();
                    return true;
                }
            } else if (selectedSettingCategory == SettingsCategory.CONTROLS) {
                if (inRect(mouseX, mouseY, centerColX + centerColW - 100, optY + 2, 100, 14)) {
                    activeSliderId = "mouseSensitivity";
                    updateSliderFromMouse(mouseX, centerColX + centerColW - 100, 100, "mouseSensitivity");
                    return true;
                }
                optY += rowH;
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 60, optY + 1, 60, 16)) {
                    this.client.options.invertYMouse = !this.client.options.invertYMouse;
                    saveOptions();
                    return true;
                }
                optY += rowH;
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 90, optY + 1, 90, 16)) {
                    this.client.openScreen(new ControlsOptionsScreen(this, this.client.options));
                    return true;
                }
            } else if (selectedSettingCategory == SettingsCategory.LANGUAGE) {
                if (checkToggleClick(mouseX, mouseY, centerColX + centerColW - 90, optY + 1, 90, 16)) {
                    this.client.openScreen(new LanguageOptionsScreen(this, this.client.options, this.client.getLanguageManager()));
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean checkToggleClick(double mouseX, double mouseY, int x, int y, int w, int h) {
        if (inRect(mouseX, mouseY, x, y, w, h)) {
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
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (activeSliderId != null && currentTab == MenuTab.SETTINGS) {
            int catW = 108;
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
            recordRecentServer(entry);
            MenuVideoPlayer.getInstance().stop();
            this.client.openScreen(new ConnectScreen(this, this.client, entry.serverInfo));
        }
    }
}
