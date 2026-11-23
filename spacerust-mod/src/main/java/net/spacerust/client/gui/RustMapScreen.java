package net.spacerust.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.spacerust.client.world.RustWorldSync;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Interactive Rust-style Island & RT Monument Map (opens with G).
 * Renders Rust grid coordinates (A1..H8), terrain height/biome shading,
 * roads between monuments, RT markers with radiation rings, and player direction.
 */
public class RustMapScreen extends Screen {

    private static final int MAP_HALF_WORLD = 520; // covers [-520..+520] blocks around spawn
    private static final int TEX_SIZE = 260;       // 1 px = 4x4 blocks

    private static NativeImageBackedTexture mapTex;
    private static Identifier mapTexId;

    public static double lastDeathX = 0;
    public static double lastDeathZ = 0;
    public static boolean hasDeathMarker = false;

    private static boolean showDeathPin = true;
    private static boolean showMonumentsPin = true;
    private static boolean showGridPin = true;

    private double centerX = 0;
    private double centerZ = 0;
    private float zoom = 1.0f;

    public RustMapScreen() {
        super(new LiteralText("Rust Map"));
        RustFontRenderer.ensureLoaded();
    }

    @Override
    protected void init() {
        super.init();
        RustFontRenderer.ensureLoaded();
        RustUiHelper.enableWorldBlur(this.client);
        if (this.client != null && this.client.player != null) {
            centerX = this.client.player.getX() * 0.25;
            centerZ = this.client.player.getZ() * 0.25;
        }
        rebuildMapTexture(this.client);
    }

    public static void ensureMapBuilt(net.minecraft.client.MinecraftClient mc) {
        if (mc != null && mapTexId == null) {
            rebuildMapTexture(mc);
        }
    }

    @Override
    public void removed() {
        super.removed();
        RustUiHelper.disableWorldBlur(this.client);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void rebuildMapTexture(net.minecraft.client.MinecraftClient mc) {
        if (mc == null) return;
        if (mapTex == null) {
            mapTex = new NativeImageBackedTexture(TEX_SIZE, TEX_SIZE, false);
            mapTexId = mc.getTextureManager().registerDynamicTexture("spacerust_map", mapTex);
        }
        NativeImage img = mapTex.getImage();
        if (img == null) return;

        List<RustWorldSync.Monument> mons = RustWorldSync.monuments;

        for (int py = 0; py < TEX_SIZE; py++) {
            int wz = -MAP_HALF_WORLD + py * (MAP_HALF_WORLD * 2 / TEX_SIZE);
            for (int px = 0; px < TEX_SIZE; px++) {
                int wx = -MAP_HALF_WORLD + px * (MAP_HALF_WORLD * 2 / TEX_SIZE);

                int abgr = sampleTerrainAbgr(mc, wx, wz);

                // Subtle road paths from (0,0) to each monument
                if (isNearRoad(wx, wz, mons)) {
                    abgr = 0xFF3E3A34; // ABGR -> #343A3E dark gravel road
                }

                // Monument base pad highlight
                for (RustWorldSync.Monument m : mons) {
                    int dx = wx - m.x;
                    int dz = wz - m.z;
                    if (dx * dx + dz * dz <= m.radius * m.radius) {
                        abgr = (m.radLevel > 0) ? 0xFF444854 : 0xFF52565B;
                        break;
                    }
                }

                img.setPixelColor(px, py, abgr);
            }
        }
        mapTex.upload();
    }

    private static int sampleTerrainAbgr(net.minecraft.client.MinecraftClient mc, int wx, int wz) {
        if (mc != null && mc.world != null && mc.world.isChunkLoaded(wx >> 4, wz >> 4)) {
            int topY = mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz);
            Biome biome = mc.world.getBiome(new net.minecraft.util.math.BlockPos(wx, topY, wz));
            int sea = mc.world.getSeaLevel();
            if (topY <= sea) {
                return 0xFF584421; // deep coastal water (#214458 in ABGR)
            }
            Biome.Category cat = biome.getCategory();
            float shade = Math.max(0.75f, Math.min(1.2f, 0.85f + (topY - sea) * 0.006f));
            int r = 92, g = 114, b = 68;
            if (cat == Biome.Category.DESERT || cat == Biome.Category.MESA || cat == Biome.Category.BEACH) {
                r = 166; g = 152; b = 112;
            } else if (cat == Biome.Category.ICY || cat == Biome.Category.TAIGA && topY > 86) {
                r = 188; g = 196; b = 200;
            } else if (cat == Biome.Category.SWAMP) {
                r = 74; g = 96; b = 66;
            } else if (cat == Biome.Category.EXTREME_HILLS) {
                r = 110; g = 114; b = 104;
            }
            r = Math.min(255, Math.round(r * shade));
            g = Math.min(255, Math.round(g * shade));
            b = Math.min(255, Math.round(b * shade));
            return (0xFF << 24) | (b << 16) | (g << 8) | r;
        }

        // Unloaded chunks: pleasant Rust island topographic shading around spawn
        double dist = Math.hypot(wx, wz);
        if (dist > 475) {
            return 0xFF4C391B; // deep Rust ocean
        } else if (dist > 445) {
            return 0xFF624F2A; // shallow coast
        } else if (dist > 425) {
            return 0xFF8CADA6; // sandy shoreline
        }
        int hills = (int) (Math.sin(wx * 0.015) * 10 + Math.cos(wz * 0.015) * 10);
        int r = 82 + hills, g = 104 + hills, b = 64 + hills / 2;
        return (0xFF << 24) | (b << 16) | (g << 8) | r;
    }

    private static boolean isNearRoad(int wx, int wz, List<RustWorldSync.Monument> mons) {
        for (RustWorldSync.Monument m : mons) {
            if (distToSegmentSq(wx, wz, 0, 0, m.x, m.z) <= 20.0) {
                return true;
            }
        }
        return false;
    }

    private static double distToSegmentSq(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        double len2 = dx * dx + dz * dz;
        if (len2 == 0) return (px - ax) * (px - ax) + (pz - az) * (pz - az);
        double t = Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / len2));
        double nx = ax + t * dx, nz = az + t * dz;
        return (px - nx) * (px - nx) + (pz - nz) * (pz - nz);
    }

    public static void renderMapViewport(MatrixStack matrices, net.minecraft.client.MinecraftClient mc,
                                         int screenW, int mapTop, int mapBoxSize,
                                         double cxWorld, double czWorld, float zoomVal,
                                         boolean showPlayer, boolean showDeath) {
        if (mc == null || mapTexId == null) return;
        float drawSize = mapBoxSize * zoomVal;
        float drawLeft = screenW / 2f - drawSize / 2f - (float) (cxWorld * (drawSize / (MAP_HALF_WORLD * 2f)));
        float drawTop = (mapTop + mapBoxSize / 2f) - drawSize / 2f - (float) (czWorld * (drawSize / (MAP_HALF_WORLD * 2f)));

        RenderSystem.enableTexture();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(mapTexId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        int sx = Math.round(drawLeft);
        int sy = Math.round(drawTop);
        int sw = Math.round(drawSize);
        DrawableHelper.drawTexture(matrices, sx, sy, sw, sw, 0f, 0f, 1, 1, 1, 1);

        if (showGridPin) {
            RustFontRenderer sf = RustFontRenderer.small();
            int cells = 8;
            float cellPx = drawSize / cells;
            for (int i = 0; i <= cells; i++) {
                int gx = Math.round(drawLeft + i * cellPx);
                int gy = Math.round(drawTop + i * cellPx);
                DrawableHelper.fill(matrices, gx, sy, gx + 1, sy + sw, 0x26000000);
                DrawableHelper.fill(matrices, sx, gy, sx + sw, gy + 1, 0x26000000);
                if (i < cells) {
                    String colLbl = String.valueOf((char) ('A' + i));
                    for (int row = 0; row < cells; row++) {
                        sf.draw(matrices, colLbl + row, Math.round(drawLeft + i * cellPx) + 3,
                                Math.round(drawTop + row * cellPx) + 2, 0x44FFFFFF);
                    }
                }
            }
        }

        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();
        if (showMonumentsPin) {
            for (RustWorldSync.Monument m : RustWorldSync.monuments) {
                int mx = worldToScreenX(m.x, drawLeft, drawSize);
                int mz = worldToScreenZ(m.z, drawTop, drawSize);
                int mr = Math.max(6, Math.round(m.radius * (drawSize / (MAP_HALF_WORLD * 2f))));
                if (m.radLevel > 0) {
                    DrawableHelper.fill(matrices, mx - mr, mz - mr, mx + mr, mz + mr, 0x33D95328);
                    RustUiHelper.drawBorder(matrices, mx - mr, mz - mr, mr * 2, mr * 2, 0x88E5652E);
                }
                float tw = bfSmall.getWidth(m.name);
                DrawableHelper.fill(matrices, Math.round(mx - tw / 2f - 4), mz - 6,
                        Math.round(mx + tw / 2f + 4), mz + 6, 0xBB161816);
                bfSmall.drawShadow(matrices, m.name, mx - tw / 2f, mz - 4,
                        m.radLevel > 0 ? 0xFFFFB74D : 0xFFF7EBE1);
            }
        }

        if (showDeath && hasDeathMarker) {
            int dx = worldToScreenX(lastDeathX, drawLeft, drawSize);
            int dz = worldToScreenZ(lastDeathZ, drawTop, drawSize);
            DrawableHelper.fill(matrices, dx - 6, dz - 6, dx + 6, dz + 6, 0xEEB82920);
            RustUiHelper.drawBorder(matrices, dx - 6, dz - 6, 12, 12, 0xFFFFFFFF);
            bfSmall.drawShadow(matrices, "X", dx - bfSmall.getWidth("X") / 2f, dz - 4, 0xFFFFFFFF);
        }

        if (showPlayer && mc.player != null) {
            int px = worldToScreenX(mc.player.getX(), drawLeft, drawSize);
            int pz = worldToScreenZ(mc.player.getZ(), drawTop, drawSize);
            drawPlayerArrow(matrices, px, pz, mc.player.yaw);
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        DrawableHelper.fill(matrices, 0, 0, this.width, this.height, 0xCC122833);

        int mapBoxSize = Math.min(this.width - 30, this.height - 46);
        int mapTop = 24;
        renderMapViewport(matrices, this.client, this.width, mapTop, mapBoxSize,
                centerX, centerZ, zoom, true, showDeathPin);

        // Right-edge Rust Map quick toggle pills (like Rust UI)
        String[] toggles = { "СМЕРТЬ", "РТ", "СЕТКА", "ЦЕНТР" };
        boolean[] active = { showDeathPin, showMonumentsPin, showGridPin, false };
        int tx = this.width - 68;
        int ty = 34;
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        for (int i = 0; i < toggles.length; i++) {
            int by = ty + i * 22;
            boolean hov = RustUiHelper.inRect(mouseX, mouseY, tx, by, 58, 18);
            int bg = active[i] ? 0xEE5E802E : (hov ? 0xDD2E312E : 0xCC1C1E1C);
            DrawableHelper.fill(matrices, tx, by, tx + 58, by + 18, bg);
            RustUiHelper.drawBorder(matrices, tx, by, 58, 18, active[i] ? 0xFF9DF042 : 0x55FFFFFF);
            bf.drawShadow(matrices, toggles[i], tx + (58 - bf.getWidth(toggles[i])) / 2f, by + 5, 0xFFF7EBE1);
        }

        // Top Bar Header
        DrawableHelper.fill(matrices, 0, 0, this.width, 20, 0xDD141614);
        RustFontRenderer.bold().drawCenteredY(matrices, "КАРТА ОСТРОВА SPACERUST", 12, 0, 20, RustUiHelper.COLOR_TEXT_CREAM);

        String coords = "";
        if (this.client != null && this.client.player != null) {
            int wx = (int) Math.round(this.client.player.getX());
            int wz = (int) Math.round(this.client.player.getZ());
            int col = Math.max(0, Math.min(7, (wx + MAP_HALF_WORLD) * 8 / (MAP_HALF_WORLD * 2)));
            int row = Math.max(0, Math.min(7, (wz + MAP_HALF_WORLD) * 8 / (MAP_HALF_WORLD * 2)));
            coords = "Сектор " + ((char) ('A' + col)) + row + "  |  X: " + wx + "  Z: " + wz
                    + "  |  РТ: " + RustWorldSync.monuments.size() + "  |  Руды рядом: " + RustWorldSync.oreNodes.size();
        }
        RustFontRenderer.small().drawCenteredY(matrices, coords,
                this.width - RustFontRenderer.small().getWidth(coords) - 12, 0, 20, 0xBBF7EBE1);

        // Bottom Hint Bar
        DrawableHelper.fill(matrices, 0, this.height - 16, this.width, this.height, 0xDD141614);
        String hint = RustWorldSync.serverSynced
                ? "Колёсико мыши: масштаб | ЛКМ: двигать карту | G / ESC: закрыть"
                : "Подключитесь к серверу SpaceRust для загрузки всех РТ и меток камней | G / ESC: закрыть";
        RustFontRenderer.small().drawCenteredY(matrices, hint,
                (this.width - RustFontRenderer.small().getWidth(hint)) / 2f, this.height - 16, 16, 0x99F7EBE1);

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int tx = this.width - 68;
            int ty = 34;
            for (int i = 0; i < 4; i++) {
                int by = ty + i * 22;
                if (RustUiHelper.inRect(mouseX, mouseY, tx, by, 58, 18)) {
                    RustUiHelper.playMenuClick(this.client);
                    if (i == 0) showDeathPin = !showDeathPin;
                    else if (i == 1) showMonumentsPin = !showMonumentsPin;
                    else if (i == 2) showGridPin = !showGridPin;
                    else if (i == 3 && this.client != null && this.client.player != null) {
                        centerX = this.client.player.getX() * 0.25;
                        centerZ = this.client.player.getZ() * 0.25;
                        zoom = 1.0f;
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public static int worldToScreenX(double wx, float drawLeft, float drawSize) {
        return Math.round(drawLeft + (float) ((wx + MAP_HALF_WORLD) / (MAP_HALF_WORLD * 2.0)) * drawSize);
    }

    public static int worldToScreenZ(double wz, float drawTop, float drawSize) {
        return Math.round(drawTop + (float) ((wz + MAP_HALF_WORLD) / (MAP_HALF_WORLD * 2.0)) * drawSize);
    }

    private static void drawPlayerArrow(MatrixStack matrices, int cx, int cy, float yawDeg) {
        matrices.push();
        matrices.translate(cx, cy, 0);
        // Minecraft yaw: 0 = +Z (south/down on map), 90 = -X (west/left), 180 = -Z (north/up)
        matrices.multiply(new net.minecraft.util.math.Quaternion(0f, 0f, yawDeg + 180f, true));
        Matrix4f mat = matrices.peek().getModel();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
        // Outer dark border triangle
        buf.vertex(mat, 0, -7, 0).color(0.08f, 0.1f, 0.08f, 1f).next();
        buf.vertex(mat, -5, 5, 0).color(0.08f, 0.1f, 0.08f, 1f).next();
        buf.vertex(mat, 5, 5, 0).color(0.08f, 0.1f, 0.08f, 1f).next();
        // Inner bright green/lime Rust player arrow
        buf.vertex(mat, 0, -5, 0).color(0.45f, 0.92f, 0.28f, 1f).next();
        buf.vertex(mat, -3.5f, 4, 0).color(0.45f, 0.92f, 0.28f, 1f).next();
        buf.vertex(mat, 3.5f, 4, 0).color(0.45f, 0.92f, 0.28f, 1f).next();
        tess.draw();
        RenderSystem.enableTexture();
        matrices.pop();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        float prev = zoom;
        zoom = Math.max(0.75f, Math.min(3.5f, zoom + (amount > 0 ? 0.15f : -0.15f)));
        return prev != zoom || super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            centerX = Math.max(-MAP_HALF_WORLD, Math.min(MAP_HALF_WORLD, centerX - deltaX / zoom * 2.0));
            centerZ = Math.max(-MAP_HALF_WORLD, Math.min(MAP_HALF_WORLD, centerZ - deltaY / zoom * 2.0));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_G || keyCode == GLFW.GLFW_KEY_M || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
