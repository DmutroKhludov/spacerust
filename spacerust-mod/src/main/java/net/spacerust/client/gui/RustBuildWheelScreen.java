package net.spacerust.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.spacerust.client.world.RustBuildingSystem;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Authentic CoilRust Radial Wheel Screen:
 * - PLAN mode (holding Building Plan): select Foundation, Triangle Foundation, Wall, Doorway,
 *   Window, Floor, Triangle Floor, Halfwall, Stairs, or Roof with CoilRust icons.
 * - HAMMER mode (holding Киянка looking at a building piece): Upgrade (Wood/Stone/Metal/HQM),
 *   Rotate, or Demolish with CoilRust mallet icons.
 */
public class RustBuildWheelScreen extends Screen {

    public enum Mode { PLAN, HAMMER }

    private static class Slice {
        final String actionId;
        final String title;
        final String subtitle;
        final Identifier icon;
        final Identifier iconSelected;
        final boolean enabled;

        Slice(String actionId, String title, String subtitle, Identifier icon, Identifier iconSelected, boolean enabled) {
            this.actionId = actionId;
            this.title = title;
            this.subtitle = subtitle;
            this.icon = icon;
            this.iconSelected = iconSelected != null ? iconSelected : icon;
            this.enabled = enabled;
        }
    }

    private final Mode mode;
    private final RustBuildingSystem.BuildingPiece targetPiece;
    private final List<Slice> slices = new ArrayList<>();
    private int hoveredIndex = -1;

    public RustBuildWheelScreen(Mode mode, RustBuildingSystem.BuildingPiece targetPiece) {
        super(new LiteralText(mode == Mode.PLAN ? "План постройки" : "Киянка"));
        this.mode = mode;
        this.targetPiece = targetPiece;
        buildSlices();
    }

    private static Identifier bTex(String file) {
        return new Identifier("spacerust", "textures/gui/building/" + file);
    }

    private void buildSlices() {
        slices.clear();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mode == Mode.PLAN) {
            slices.add(new Slice("foundation", "ФУНДАМЕНТ", "50 ДЕРЕВО", bTex("foundation.png"), bTex("foundation_selected.png"), true));
            slices.add(new Slice("foundation_tri", "ТРЕУГ. ФУНД.", "25 ДЕРЕВО", bTex("foundationtriangle.png"), bTex("foundationtriangle_selected.png"), true));
            slices.add(new Slice("wall", "СТЕНА", "50 ДЕРЕВО", bTex("wall.png"), bTex("wall_selected.png"), true));
            slices.add(new Slice("doorway", "ДВЕРНОЙ ПРОЕМ", "50 ДЕРЕВО", bTex("doorway.png"), bTex("doorway_selected.png"), true));
            slices.add(new Slice("window", "ОКНО", "50 ДЕРЕВО", bTex("window.png"), bTex("window_selected.png"), true));
            slices.add(new Slice("floor", "ПОЛ / ПОТОЛОК", "50 ДЕРЕВО", bTex("floor.png"), bTex("floor.png"), true));
            slices.add(new Slice("floor_tri", "ТРЕУГ. ПОЛ", "25 ДЕРЕВО", bTex("floor.triangle.png"), bTex("floor.triangle_selected.png"), true));
            slices.add(new Slice("halfwall", "ПОЛУСТЕНА", "25 ДЕРЕВО", bTex("halfwall.png"), bTex("halfwall_selected.png"), true));
            slices.add(new Slice("lowwall", "НИЗКАЯ СТЕНА", "25 ДЕРЕВО", bTex("lowwall.png"), bTex("lowwall_selected.png"), true));
            slices.add(new Slice("stairs", "ЛЕСТНИЦА", "50 ДЕРЕВО", bTex("ushapedstairs.png"), bTex("ushapedstairs_selected.png"), true));
            slices.add(new Slice("ramp", "РАМПА", "50 ДЕРЕВО", bTex("ramp.png"), bTex("ramp.png"), true));
            slices.add(new Slice("roof", "КРЫША", "50 ДЕРЕВО", bTex("roof.png"), bTex("roof.png"), true));
        } else if (targetPiece != null) {
            int rank = tierRank(targetPiece.tier);
            addUpgradeSlice(mc, "up:wood", "ДЕРЕВО", "wood", rank < 1, Items.OAK_PLANKS, "mallet_wood.png");
            addUpgradeSlice(mc, "up:stone", "КАМЕНЬ", "stone", rank < 2, Items.COBBLESTONE, "mallet_stone.png");
            addUpgradeSlice(mc, "up:metal", "МЕТАЛЛ", "metal", rank < 3, Items.IRON_INGOT, "mallet_metal.png");
            addUpgradeSlice(mc, "up:armored", "МВК (БРОНЯ)", "armored", rank < 4, Items.NETHERITE_INGOT, "mallet_hqm.png");
            slices.add(new Slice("rotate", "ПОВЕРНУТЬ", "ПО ЧАСОВОЙ", bTex("mallet_rotate.png"), bTex("mallet_rotate.png"), true));
            slices.add(new Slice("demolish", "СНЕСТИ", "УДАЛИТЬ БЛОК", bTex("mallet_demolish.png"), bTex("mallet_demolish.png"), true));
        }
    }

    private void addUpgradeSlice(MinecraftClient mc, String id, String title, String tier,
                                 boolean higherTier, Item mat, String iconName) {
        int cost = RustBuildingSystem.upgradeCost(targetPiece.type, tier);
        int have = (mc != null && mc.player != null) ? RustCraftingScreen.countPlayerResource(mc.player, mat) : 0;
        boolean ok = higherTier && ((mc != null && mc.player != null && mc.player.isCreative()) || have >= cost);
        String resLabel = "wood".equals(tier) ? "ДЕРЕВО" : ("stone".equals(tier) ? "КАМЕНЬ" : ("metal".equals(tier) ? "МЕТАЛЛ" : "МВК"));
        String sub = higherTier ? (cost + " " + resLabel) : "УЖЕ УЛУЧШЕНО";
        slices.add(new Slice(id, title, sub, bTex(iconName), bTex(iconName), ok));
    }

    private static int tierRank(String t) {
        if ("wood".equals(t)) return 1;
        if ("stone".equals(t)) return 2;
        if ("metal".equals(t)) return 3;
        if ("armored".equals(t)) return 4;
        return 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (slices.isEmpty()) {
            this.onClose();
            return;
        }
        RustFontRenderer.ensureLoaded();
        int cx = this.width / 2;
        int cy = this.height / 2;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.hypot(dx, dy);

        hoveredIndex = -1;
        int count = slices.size();
        if (dist >= 22.0 && count > 0) {
            double ang = Math.atan2(dy, dx) + Math.PI / 2.0; // 0 at top (12 o'clock)
            if (ang < 0) ang += Math.PI * 2.0;
            double step = (Math.PI * 2.0) / count;
            hoveredIndex = ((int) Math.floor((ang + step * 0.5) / step)) % count;
        }

        // Draw segmented radial ring using triangles (authentic Rust / CoilRust wheel)
        float outerR = 136f;
        float innerR = 52f;
        double step = (Math.PI * 2.0) / count;
        double gapRad = 0.032;

        RenderSystem.pushMatrix();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        for (int i = 0; i < count; i++) {
            Slice s = slices.get(i);
            boolean sel = (i == hoveredIndex) || (mode == Mode.PLAN && s.actionId.equals(RustBuildingSystem.selectedType));
            float r, g, b, a;
            if (sel && s.enabled) {
                r = 0.79f; g = 0.27f; b = 0.17f; a = 0.88f; // Rust orange-red highlight
            } else if (i == hoveredIndex) {
                r = 0.45f; g = 0.20f; b = 0.18f; a = 0.72f;
            } else {
                r = 0.12f; g = 0.12f; b = 0.11f; a = 0.78f;
            }

            double a0 = -Math.PI / 2.0 + (i - 0.5) * step + gapRad * 0.5;
            double a1 = -Math.PI / 2.0 + (i + 0.5) * step - gapRad * 0.5;
            int segs = 10;
            buf.begin(GL11.GL_TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int k = 0; k <= segs; k++) {
                double t = a0 + (a1 - a0) * (k / (double) segs);
                float cs = (float) Math.cos(t), sn = (float) Math.sin(t);
                buf.vertex(matrices.peek().getModel(), cx + cs * innerR, cy + sn * innerR, 0).color(r, g, b, a).next();
                buf.vertex(matrices.peek().getModel(), cx + cs * outerR, cy + sn * outerR, 0).color(r, g, b, a).next();
            }
            tess.draw();
        }
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
        RenderSystem.popMatrix();

        // Draw slice icons around the ring
        float midR = (innerR + outerR) * 0.5f;
        for (int i = 0; i < count; i++) {
            Slice s = slices.get(i);
            double a = -Math.PI / 2.0 + i * step;
            int ix = Math.round((float) (cx + Math.cos(a) * midR)) - 14;
            int iy = Math.round((float) (cy + Math.sin(a) * midR)) - 14;
            boolean sel = (i == hoveredIndex) || (mode == Mode.PLAN && s.actionId.equals(RustBuildingSystem.selectedType));
            int tint = s.enabled ? (sel ? 0xFFFFFFFF : 0xFFE6DDD2) : 0x66AAAAAA;
            RustUiHelper.drawSprite(matrices, this.client, sel ? s.iconSelected : s.icon, ix, iy, 28, 28, tint);
        }

        // Center info readout
        RustFontRenderer hf = RustFontRenderer.bold();
        RustFontRenderer sf = RustFontRenderer.small();
        Slice focus = (hoveredIndex >= 0 && hoveredIndex < count) ? slices.get(hoveredIndex) : null;
        String centerTitle = (focus != null) ? focus.title : (mode == Mode.PLAN ? RustBuildingSystem.displayTypeName(RustBuildingSystem.selectedType) : "КИЯНКА");
        String centerSub = (focus != null) ? focus.subtitle : (targetPiece != null ? RustBuildingSystem.displayTierName(targetPiece.tier) + " " + targetPiece.hp + "/" + targetPiece.maxHp : "ВЫБЕРИТЕ ДЕТАЛЬ");

        DrawableHelper.fill(matrices, cx - 44, cy - 16, cx + 44, cy + 16, 0xBB191A18);
        hf.drawCenteredY(matrices, hf.trim(centerTitle, 84), cx - hf.getWidth(hf.trim(centerTitle, 84)) / 2f, cy - 14, 15, 0xFFF7EBE1);
        int subCol = (focus != null && !focus.enabled) ? 0xFFE06050 : 0xFFAEE268;
        sf.drawCenteredY(matrices, sf.trim(centerSub, 84), cx - sf.getWidth(sf.trim(centerSub, 84)) / 2f, cy + 2, 13, subCol);

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            if (hoveredIndex >= 0 && hoveredIndex < slices.size()) {
                Slice s = slices.get(hoveredIndex);
                if (s.enabled) {
                    applySlice(s);
                }
            }
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void applySlice(Slice s) {
        if (this.client == null) return;
        RustUiHelper.playClick(this.client);
        if (mode == Mode.PLAN) {
            RustBuildingSystem.selectedType = s.actionId;
            return;
        }
        if (targetPiece == null) return;
        if (s.actionId.startsWith("up:")) {
            RustBuildingSystem.upgradePiece(this.client, targetPiece.id, s.actionId.substring(3));
        } else if ("rotate".equals(s.actionId)) {
            RustBuildingSystem.rotatePiece(this.client, targetPiece.id);
        } else if ("demolish".equals(s.actionId)) {
            RustBuildingSystem.demolishPiece(this.client, targetPiece.id);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_TAB || keyCode == GLFW.GLFW_KEY_E) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
