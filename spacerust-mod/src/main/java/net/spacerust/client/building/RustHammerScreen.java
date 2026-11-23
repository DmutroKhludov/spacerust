package net.spacerust.client.building;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.spacerust.client.gui.RustFontRenderer;
import net.spacerust.client.gui.RustUiHelper;
import org.lwjgl.glfw.GLFW;

public class RustHammerScreen extends Screen {

    public enum HammerAction {
        UPGRADE_WOOD("ДЕРЕВО", "100 Дерева", RustBuildingTier.WOOD, new Identifier("spacerust", "textures/gui/radial/wood_light.png")),
        UPGRADE_STONE("КАМЕНЬ", "150 Камня", RustBuildingTier.STONE, new Identifier("spacerust", "textures/gui/radial/stone_light.png")),
        UPGRADE_METAL("МЕТАЛЛ", "100 Металла", RustBuildingTier.METAL, new Identifier("spacerust", "textures/gui/radial/metal_light.png")),
        UPGRADE_ARMORED("МВК / БРОНЯ", "25 МВК", RustBuildingTier.ARMORED, new Identifier("spacerust", "textures/gui/radial/armored_light.png")),
        ROTATE("ПОВЕРНУТЬ", "Разворот на 180°", null, new Identifier("spacerust", "textures/gui/rust_icons/rotate.png")),
        REPAIR("ПОЧИНИТЬ", "Восстановить HP", null, new Identifier("spacerust", "textures/gui/radial/repair_light.png")),
        DEMOLISH("СНЕСТИ", "Удалить постройку", null, new Identifier("spacerust", "textures/gui/rust_icons/demolish.png"));

        public final String title;
        public final String costDesc;
        public final RustBuildingTier targetTier;
        public final Identifier icon;

        HammerAction(String title, String costDesc, RustBuildingTier targetTier, Identifier icon) {
            this.title = title;
            this.costDesc = costDesc;
            this.targetTier = targetTier;
            this.icon = icon;
        }
    }

    public static final Identifier TEX_RUST_CENTER = new Identifier("spacerust", "textures/gui/radial/rust_center.png");
    public static final Identifier TEX_RUST_RING   = new Identifier("spacerust", "textures/gui/radial/rust_ring.png");

    private final RustBuildingManager.BuildingPieceData targetPiece;
    private int hoveredIndex = -1;
    private int lastHovered = -1;

    public RustHammerScreen(RustBuildingManager.BuildingPieceData targetPiece) {
        super(new LiteralText("Hammer Menu"));
        this.targetPiece = targetPiece;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        DrawableHelper.fill(matrices, 0, 0, this.width, this.height, 0x99121110);

        int cx = this.width / 2;
        int cy = this.height / 2;
        float radius = Math.min(130f, Math.min(this.width, this.height) * 0.36f);

        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.hypot(dx, dy);

        HammerAction[] actions = HammerAction.values();
        int count = actions.length;
        double sliceAngle = 360.0 / count;

        if (dist > 35) {
            double angle = Math.toDegrees(Math.atan2(dy, dx));
            if (angle < 0) angle += 360.0;
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
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();

        for (int i = 0; i < count; i++) {
            HammerAction act = actions[i];
            double midA = Math.toRadians((i * sliceAngle) - 90.0);
            float px = cx + (float) (Math.cos(midA) * radius);
            float py = cy + (float) (Math.sin(midA) * radius);

            boolean isHov = (i == hoveredIndex);
            int slotSize = isHov ? 42 : 36;
            int bg = isHov ? 0xE84A5B2C : 0xD0232522;
            if (act == HammerAction.DEMOLISH && isHov) bg = 0xE88B2A22; // Red highlight for demolish

            DrawableHelper.fill(matrices, (int) (px - slotSize / 2), (int) (py - slotSize / 2),
                    (int) (px + slotSize / 2), (int) (py + slotSize / 2), bg);

            RustUiHelper.drawBorder(matrices, (int) (px - slotSize / 2), (int) (py - slotSize / 2),
                    slotSize, slotSize, isHov ? 0xFFF7EBE1 : 0x33F7EBE1);

            int iconS = isHov ? 24 : 20;
            RustUiHelper.drawSprite(matrices, this.client, act.icon,
                    (int) (px - iconS / 2), (int) (py - iconS / 2), iconS, iconS, isHov ? 0xFFFFFFFF : 0xCCF7EBE1);
        }

        // 3. Center Info Circle
        int centerS = 100;
        RustUiHelper.drawSprite(matrices, this.client, TEX_RUST_CENTER, cx - centerS / 2, cy - centerS / 2, centerS, centerS, 0xF51A1815);
        RustUiHelper.drawBorder(matrices, cx - centerS / 2, cy - centerS / 2, centerS, centerS, 0x55F7EBE1);

        if (hoveredIndex >= 0 && hoveredIndex < actions.length) {
            HammerAction act = actions[hoveredIndex];
            int cIconS = 30;
            RustUiHelper.drawSprite(matrices, this.client, act.icon, cx - cIconS / 2, cy - 36, cIconS, cIconS, 0xFFFFFFFF);

            String title = act.title;
            float tw = bfSmall.getWidth(title);
            bfSmall.draw(matrices, title, cx - tw / 2f, cy + 2, 0xFFF7EBE1);

            String cost = act.costDesc;
            float cw = sf.getWidth(cost);
            int color = (act == HammerAction.DEMOLISH) ? 0xFFD3362D : 0xFF8DC63F;
            sf.draw(matrices, cost, cx - cw / 2f, cy + 18, color);
        } else if (targetPiece != null) {
            String title = targetPiece.piece.title.toUpperCase();
            float tw = bfSmall.getWidth(title);
            bfSmall.draw(matrices, title, cx - tw / 2f, cy - 18, 0xFFF7EBE1);

            String tier = targetPiece.tier.title + " (" + (int) targetPiece.health + "/" + (int) targetPiece.maxHealth + " HP)";
            float trW = sf.getWidth(tier);
            sf.draw(matrices, tier, cx - trW / 2f, cy + 2, 0xFFE5A823);

            String prompt = "ВЫБЕРИТЕ ДЕЙСТВИЕ";
            float pw = sf.getWidth(prompt);
            sf.draw(matrices, prompt, cx - pw / 2f, cy + 18, 0x88F7EBE1);
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void applyAction(int index) {
        if (targetPiece == null || index < 0 || index >= HammerAction.values().length) return;
        HammerAction act = HammerAction.values()[index];

        switch (act) {
            case UPGRADE_WOOD:
            case UPGRADE_STONE:
            case UPGRADE_METAL:
            case UPGRADE_ARMORED:
                RustBuildingManager.tryUpgradePiece(this.client, targetPiece, act.targetTier);
                break;
            case ROTATE:
                RustBuildingManager.rotatePiece(targetPiece);
                break;
            case REPAIR:
                RustBuildingManager.repairPiece(this.client, targetPiece);
                break;
            case DEMOLISH:
                RustBuildingManager.demolishPiece(this.client, targetPiece);
                break;
        }
        RustUiHelper.playClick(this.client);
        this.onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredIndex >= 0) {
            applyAction(hoveredIndex);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1 && hoveredIndex >= 0) {
            applyAction(hoveredIndex);
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
