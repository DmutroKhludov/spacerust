package net.spacerust.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.spacerust.client.guns.GunManager;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Radial Ammo Type Wheel opened by holding R (mirrors Rust ammo switcher).
 */
public class RustAmmoWheelScreen extends Screen {

    private static class AmmoSlice {
        final String modeId;
        final String title;
        final String subtitle;
        final Identifier icon;

        AmmoSlice(String modeId, String title, String subtitle, String iconFile) {
            this.modeId = modeId;
            this.title = title;
            this.subtitle = subtitle;
            this.icon = new Identifier("spacerust", "textures/gui/rust_items/" + iconFile);
        }
    }

    private final GunManager.GunInfo gun;
    private final List<AmmoSlice> slices = new ArrayList<>();
    private int hoveredIndex = -1;

    public RustAmmoWheelScreen(GunManager.GunInfo gun) {
        super(new LiteralText("Выбор боеприпаса"));
        this.gun = gun;
        String baseIcon = "rocket_launcher".equals(gun.key) ? "ammo.rocket.basic.png"
                : ("shotgun".equals(gun.key) || "pump".equals(gun.key) || "eoka".equals(gun.key) ? "ammo.shotgun.png"
                : ("bow".equals(gun.key) || "crossbow".equals(gun.key) ? "arrow.wooden.png"
                : ("mp5".equals(gun.key) || "thompson".equals(gun.key) || "customsmg".equals(gun.key)
                || "revolver".equals(gun.key) || "python".equals(gun.key) || "p250".equals(gun.key) || "m92".equals(gun.key)
                ? "ammo.pistol.png" : "ammo.rifle.png")));
        slices.add(new AmmoSlice("standard", "СТАНДАРТНЫЙ", "Стандартный боеприпас высокой точности", baseIcon));
        slices.add(new AmmoSlice("hv", "СКОРОСТНОЙ (HV)", "Повышенная скорость и пробитие на дистанции", baseIcon));
        slices.add(new AmmoSlice("incendiary", "ЗАЖИГАТЕЛЬНЫЙ", "Поджигает цель и создаёт огонь при попадании", baseIcon));
        slices.add(new AmmoSlice("explosive", "РАЗРЫВНОЙ", "Взрывается при попадании, нанося урон по площади", baseIcon));
    }

    @Override
    protected void init() {
        super.init();
        RustFontRenderer.ensureLoaded();
        RustUiHelper.enableWorldBlur(this.client);
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

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        float innerR = 54.0f;
        float outerR = 136.0f;

        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        hoveredIndex = -1;
        if (dist >= innerR * 0.45 && dist <= outerR * 1.45 && !slices.isEmpty()) {
            double deg = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
            if (deg < 0) deg += 360.0;
            hoveredIndex = (int) Math.floor(deg / (360.0 / slices.size())) % slices.size();
        }

        int n = slices.size();
        float sweep = 360.0f / n;
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        Matrix4f mat = matrices.peek().getModel();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        for (int i = 0; i < n; i++) {
            AmmoSlice s = slices.get(i);
            boolean active = s.modeId.equals(GunManager.selectedAmmoMode);
            boolean hov = (i == hoveredIndex);
            float a0 = i * sweep + 2.0f;
            float a1 = (i + 1) * sweep - 2.0f;
            float r = (hov || active) ? 0.80f : 0.94f;
            float g = (hov || active) ? 0.27f : 0.92f;
            float b = (hov || active) ? 0.18f : 0.88f;
            float alpha = (hov || active) ? 0.92f : 0.84f;

            buf.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
            int steps = 18;
            for (int k = 0; k < steps; k++) {
                double r0 = Math.toRadians(a0 + (a1 - a0) * (k / (double) steps) - 90.0);
                double r1 = Math.toRadians(a0 + (a1 - a0) * ((k + 1) / (double) steps) - 90.0);
                float x0i = cx + (float) Math.cos(r0) * innerR;
                float y0i = cy + (float) Math.sin(r0) * innerR;
                float x0o = cx + (float) Math.cos(r0) * outerR;
                float y0o = cy + (float) Math.sin(r0) * outerR;
                float x1i = cx + (float) Math.cos(r1) * innerR;
                float y1i = cy + (float) Math.sin(r1) * innerR;
                float x1o = cx + (float) Math.cos(r1) * outerR;
                float y1o = cy + (float) Math.sin(r1) * outerR;

                buf.vertex(mat, x0i, y0i, 0).color(r, g, b, alpha).next();
                buf.vertex(mat, x0o, y0o, 0).color(r, g, b, alpha).next();
                buf.vertex(mat, x1o, y1o, 0).color(r, g, b, alpha).next();

                buf.vertex(mat, x0i, y0i, 0).color(r, g, b, alpha).next();
                buf.vertex(mat, x1o, y1o, 0).color(r, g, b, alpha).next();
                buf.vertex(mat, x1i, y1i, 0).color(r, g, b, alpha).next();
            }
            tess.draw();
        }
        RenderSystem.enableCull();
        RenderSystem.enableTexture();

        float iconR = (innerR + outerR) * 0.5f;
        for (int i = 0; i < n; i++) {
            AmmoSlice s = slices.get(i);
            double midRad = Math.toRadians((i + 0.5) * sweep - 90.0);
            int ix = Math.round(cx + (float) Math.cos(midRad) * iconR) - 16;
            int iy = Math.round(cy + (float) Math.sin(midRad) * iconR) - 16;
            RustItemIcons.drawItemPng(matrices, this.client, s.icon, ix, iy, 32);
        }

        AmmoSlice shown = hoveredIndex >= 0 ? slices.get(hoveredIndex)
                : slices.stream().filter(sl -> sl.modeId.equals(GunManager.selectedAmmoMode)).findFirst().orElse(slices.get(0));
        RustFontRenderer bf = RustFontRenderer.bold();
        RustFontRenderer sf = RustFontRenderer.small();
        bf.drawShadow(matrices, shown.title, cx - bf.getWidth(shown.title) / 2f, cy - 12, 0xFFF7EBE1);
        sf.drawShadow(matrices, shown.subtitle, cx - sf.getWidth(shown.subtitle) / 2f, cy + 4, 0xCCF7EBE1);

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredIndex >= 0 && hoveredIndex < slices.size()) {
            GunManager.selectedAmmoMode = slices.get(hoveredIndex).modeId;
            RustUiHelper.playMenuClick(this.client);
            this.onClose();
            return true;
        }
        if (button == 1) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_R) {
            if (hoveredIndex >= 0 && hoveredIndex < slices.size()) {
                GunManager.selectedAmmoMode = slices.get(hoveredIndex).modeId;
            }
            this.onClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }
}
