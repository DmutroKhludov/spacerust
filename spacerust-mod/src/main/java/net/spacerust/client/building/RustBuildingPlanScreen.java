package net.spacerust.client.building;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.spacerust.client.gui.RustFontRenderer;
import net.spacerust.client.gui.RustUiHelper;
import org.lwjgl.glfw.GLFW;

public class RustBuildingPlanScreen extends Screen {

    public static final RustBuildingPiece[] WHEEL_PIECES = {
            RustBuildingPiece.FOUNDATION,
            RustBuildingPiece.WALL,
            RustBuildingPiece.DOORWAY,
            RustBuildingPiece.WINDOW,
            RustBuildingPiece.FRAME,
            RustBuildingPiece.FLOOR,
            RustBuildingPiece.FLOOR_TRIANGLE,
            RustBuildingPiece.ROOF,
            RustBuildingPiece.HALF_WALL,
            RustBuildingPiece.LOW_WALL,
            RustBuildingPiece.STAIRS_L,
            RustBuildingPiece.FOUNDATION_STEPS,
            RustBuildingPiece.RAMP
    };

    public static final Identifier TEX_RUST_CENTER = new Identifier("spacerust", "textures/gui/radial/rust_center.png");
    public static final Identifier TEX_RUST_RING   = new Identifier("spacerust", "textures/gui/radial/rust_ring.png");

    private int hoveredIndex = -1;
    private int lastHovered = -1;

    public RustBuildingPlanScreen() {
        super(new LiteralText("Building Plan"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // Dark vignette overlay
        DrawableHelper.fill(matrices, 0, 0, this.width, this.height, 0x99121110);

        int cx = this.width / 2;
        int cy = this.height / 2;
        float radius = Math.min(140f, Math.min(this.width, this.height) * 0.38f);

        // Calculate mouse angle
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.hypot(dx, dy);

        int count = WHEEL_PIECES.length;
        double sliceAngle = 360.0 / count;

        if (dist > 35) {
            double angle = Math.toDegrees(Math.atan2(dy, dx));
            if (angle < 0) angle += 360.0;
            // Align top (angle 270) to slice 0
            angle = (angle + 90.0 + sliceAngle / 2.0) % 360.0;
            hoveredIndex = (int) (angle / sliceAngle);
            if (hoveredIndex < 0 || hoveredIndex >= count) hoveredIndex = 0;
            if (hoveredIndex != lastHovered) {
                lastHovered = hoveredIndex;
                RustUiHelper.playClick(this.client);
            }
        } else {
            hoveredIndex = -1;
        }

        // 1. Draw Ring Backdrop
        int ringSize = (int) (radius * 2.2f);
        RustUiHelper.drawSprite(matrices, this.client, TEX_RUST_RING, cx - ringSize / 2, cy - ringSize / 2, ringSize, ringSize, 0x44FFFFFF);

        // 2. Draw Wheel Slices
        RustFontRenderer sf = RustFontRenderer.small();
        RustFontRenderer bf = RustFontRenderer.bold();
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();

        for (int i = 0; i < count; i++) {
            RustBuildingPiece piece = WHEEL_PIECES[i];
            double midA = Math.toRadians((i * sliceAngle) - 90.0);
            float px = cx + (float) (Math.cos(midA) * radius);
            float py = cy + (float) (Math.sin(midA) * radius);

            boolean isHov = (i == hoveredIndex);
            boolean isCur = (piece == RustBuildingManager.selectedPiece);

            int slotSize = isHov ? 40 : 34;
            int bg = isHov ? 0xE84A5B2C : (isCur ? 0xD0383C32 : 0xD0232522);
            DrawableHelper.fill(matrices, (int) (px - slotSize / 2), (int) (py - slotSize / 2),
                    (int) (px + slotSize / 2), (int) (py + slotSize / 2), bg);

            RustUiHelper.drawBorder(matrices, (int) (px - slotSize / 2), (int) (py - slotSize / 2),
                    slotSize, slotSize, isHov ? 0xFFF7EBE1 : (isCur ? 0x88F7EBE1 : 0x33F7EBE1));

            // Icon
            int iconS = isHov ? 24 : 20;
            RustUiHelper.drawSprite(matrices, this.client, piece.icon,
                    (int) (px - iconS / 2), (int) (py - iconS / 2), iconS, iconS, isHov ? 0xFFFFFFFF : 0xCCF7EBE1);
        }

        // 3. Center Info Circle
        int centerS = 96;
        RustUiHelper.drawSprite(matrices, this.client, TEX_RUST_CENTER, cx - centerS / 2, cy - centerS / 2, centerS, centerS, 0xF51A1815);
        RustUiHelper.drawBorder(matrices, cx - centerS / 2, cy - centerS / 2, centerS, centerS, 0x55F7EBE1);

        RustBuildingPiece activePiece = (hoveredIndex >= 0) ? WHEEL_PIECES[hoveredIndex] : RustBuildingManager.selectedPiece;
        if (activePiece != null) {
            int cIconS = 32;
            RustUiHelper.drawSprite(matrices, this.client, activePiece.icon, cx - cIconS / 2, cy - 36, cIconS, cIconS, 0xFFFFFFFF);

            String title = activePiece.title.toUpperCase();
            float tw = bfSmall.getWidth(title);
            bfSmall.draw(matrices, title, cx - tw / 2f, cy + 2, 0xFFF7EBE1);

            String cost = "20 ДЕРЕВА";
            float cw = sf.getWidth(cost);
            sf.draw(matrices, cost, cx - cw / 2f, cy + 18, 0xFF8DC63F);
        }

        // Instructions banner
        String tip = "ЛКМ / ПКМ: Выбрать постройку | R: Повернуть при размещении";
        float tipW = sf.getWidth(tip);
        sf.draw(matrices, tip, cx - tipW / 2f, this.height - 24, 0x88F7EBE1);

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredIndex >= 0 && hoveredIndex < WHEEL_PIECES.length) {
            RustBuildingManager.selectedPiece = WHEEL_PIECES[hoveredIndex];
            RustUiHelper.playClick(this.client);
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1 && hoveredIndex >= 0 && hoveredIndex < WHEEL_PIECES.length) {
            RustBuildingManager.selectedPiece = WHEEL_PIECES[hoveredIndex];
            RustUiHelper.playClick(this.client);
            this.onClose();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
