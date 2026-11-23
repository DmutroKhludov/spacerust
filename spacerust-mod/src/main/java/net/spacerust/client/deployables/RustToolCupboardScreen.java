package net.spacerust.client.deployables;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.spacerust.client.building.RustBuildingManager;
import net.spacerust.client.building.RustBuildingPiece;
import net.spacerust.client.building.RustBuildingTier;
import net.spacerust.client.gui.RustFontRenderer;
import net.spacerust.client.gui.RustUiHelper;
import net.spacerust.client.sound.RustSoundManager;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public class RustToolCupboardScreen extends Screen {

    public static final Identifier TEX_TC = new Identifier("spacerust", "textures/gui/rust_icons/tool_cupboard.png");
    public static final Identifier ICON_WOOD = new Identifier("spacerust", "textures/gui/rust_icons/wood.png");
    public static final Identifier ICON_STONE = new Identifier("spacerust", "textures/gui/rust_icons/stones.png");
    public static final Identifier ICON_METAL = new Identifier("spacerust", "textures/gui/rust_icons/metal_fragments.png");
    public static final Identifier ICON_HQM = new Identifier("spacerust", "textures/gui/rust_icons/high_quality_metal.png");

    private final RustDeployableData tc;

    public RustToolCupboardScreen(RustDeployableData tc) {
        super(new LiteralText("Tool Cupboard"));
        this.tc = tc;
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

        int panelW = 380;
        int panelH = 260;
        int px = cx - panelW / 2;
        int py = cy - panelH / 2;

        // Background
        DrawableHelper.fill(matrices, px, py, px + panelW, py + panelH, 0xF0181A16);
        RustUiHelper.drawBorder(matrices, px, py, panelW, panelH, 0x44F7EBE1);

        RustFontRenderer bf = RustFontRenderer.bold();
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();
        RustFontRenderer sf = RustFontRenderer.small();

        // Header
        DrawableHelper.fill(matrices, px, py, px + panelW, py + 26, 0x99242820);
        RustUiHelper.drawSprite(matrices, this.client, TEX_TC, px + 8, py + 4, 18, 18, 0xFFFFFFFF);
        bfSmall.draw(matrices, "ШКАФ С ИНСТРУМЕНТАМИ", px + 32, py + 8, 0xFFF7EBE1);

        // 1. LEFT COLUMN: Privilege & Upkeep Stats
        int leftW = 160;
        int leftX = px + 12;
        int leftY = py + 36;

        // Privilege Status Box
        boolean isAuth = (this.client.player != null) && tc.authorizedPlayers.contains(this.client.player.getUuidAsString());
        int privBg = isAuth ? 0xD02D4E1E : 0xD06E221E;
        DrawableHelper.fill(matrices, leftX, leftY, leftX + leftW, leftY + 32, privBg);
        RustUiHelper.drawBorder(matrices, leftX, leftY, leftW, 32, isAuth ? 0xFF8DC63F : 0xFFE0462C);

        String privTitle = isAuth ? "ВЫ АВТОРИЗОВАНЫ" : "НЕТ АВТОРИЗАЦИИ";
        bfSmall.draw(matrices, privTitle, leftX + 8, leftY + 6, 0xFFF7EBE1);
        sf.draw(matrices, isAuth ? "Строительство разрешено" : "Нажмите для авторизации", leftX + 8, leftY + 18, 0xCCF7EBE1);

        // Upkeep calculation
        int costWood = 0;
        int costStone = 0;
        int costMetal = 0;
        int costHqm = 0;

        for (RustBuildingManager.BuildingPieceData b : RustBuildingManager.pieces) {
            double distSq = (b.x - tc.x) * (b.x - tc.x) + (b.z - tc.z) * (b.z - tc.z);
            if (distSq < 30.0 * 30.0) { // 30 block radius
                if (b.tier == RustBuildingTier.TWIG) costWood += 5;
                else if (b.tier == RustBuildingTier.WOOD) costWood += 25;
                else if (b.tier == RustBuildingTier.STONE) costStone += 40;
                else if (b.tier == RustBuildingTier.METAL) costMetal += 30;
                else if (b.tier == RustBuildingTier.ARMORED) costHqm += 4;
            }
        }

        // Upkeep Header
        int uY = leftY + 42;
        bfSmall.draw(matrices, "СОДЕРЖАНИЕ БАЗЫ (24 Ч):", leftX, uY, 0xFFE5A823);

        // Upkeep items
        drawUpkeepItem(matrices, ICON_WOOD, "Дерево:", costWood + " шт.", leftX, uY + 16);
        drawUpkeepItem(matrices, ICON_STONE, "Камень:", costStone + " шт.", leftX, uY + 36);
        drawUpkeepItem(matrices, ICON_METAL, "Фрагменты:", costMetal + " шт.", leftX, uY + 56);
        drawUpkeepItem(matrices, ICON_HQM, "МВК:", costHqm + " шт.", leftX, uY + 76);

        // Protection timer
        int tY = uY + 104;
        boolean hasUpkeep = (costWood == 0 && costStone == 0 && costMetal == 0 && costHqm == 0) || (tc.protectedUntil > System.currentTimeMillis());
        DrawableHelper.fill(matrices, leftX, tY, leftX + leftW, tY + 28, hasUpkeep ? 0xD024321E : 0xD0501814);
        RustUiHelper.drawBorder(matrices, leftX, tY, leftW, 28, hasUpkeep ? 0xFF8DC63F : 0xFFE0462C);

        String protText = hasUpkeep ? "ЗАЩИЩЕНО НА: 24 Ч" : "ГНИЕНИЕ ЧЕРЕЗ: 0 МИН";
        bfSmall.draw(matrices, protText, leftX + 8, tY + 6, hasUpkeep ? 0xFF8DC63F : 0xFFE0462C);
        sf.draw(matrices, hasUpkeep ? "База не гниет" : "Положите ресурсы в шкаф!", leftX + 8, tY + 16, 0x88F7EBE1);

        // Authorize Button
        int btnW = 160;
        int btnH = 22;
        int btnX = leftX;
        int btnY = py + panelH - 32;

        boolean hovAuth = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        DrawableHelper.fill(matrices, btnX, btnY, btnX + btnW, btnY + btnH, hovAuth ? 0xFF4A5B2C : 0xFF2A2E26);
        RustUiHelper.drawBorder(matrices, btnX, btnY, btnW, btnH, hovAuth ? 0xFFFFFFFF : 0x33F7EBE1);
        String btnText = isAuth ? "ОЧИСТИТЬ АВТОРИЗАЦИЮ" : "АВТОРИЗОВАТЬСЯ";
        float bw = bfSmall.getWidth(btnText);
        bfSmall.draw(matrices, btnText, btnX + (btnW - bw) / 2f, btnY + 6, 0xFFF7EBE1);

        // 2. RIGHT COLUMN: 24 Upkeep & Storage Slots (6x4 grid)
        int gridX = px + 188;
        int gridY = py + 36;
        int slotSize = 28;
        int gap = 4;

        bfSmall.draw(matrices, "СЛОТЫ ДЛЯ РЕСУРСОВ СОДЕРЖАНИЯ:", gridX, py + 24, 0x88F7EBE1);

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 6; c++) {
                int sx = gridX + c * (slotSize + gap);
                int sy = gridY + r * (slotSize + gap);
                int idx = r * 6 + c;

                boolean hovSlot = mouseX >= sx && mouseX <= sx + slotSize && mouseY >= sy && mouseY <= sy + slotSize;
                DrawableHelper.fill(matrices, sx, sy, sx + slotSize, sy + slotSize, hovSlot ? 0xCC3E4532 : 0xBB232522);
                RustUiHelper.drawBorder(matrices, sx, sy, slotSize, slotSize, hovSlot ? 0xFFFFFFFF : 0x22F7EBE1);

                if (idx < tc.inventory.size()) {
                    ItemStack st = tc.inventory.get(idx);
                    if (!st.isEmpty()) {
                        this.itemRenderer.renderInGui(st, sx + 6, sy + 6);
                        this.itemRenderer.renderGuiItemOverlay(this.textRenderer, st, sx + 6, sy + 6);
                    }
                }
            }
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawUpkeepItem(MatrixStack matrices, Identifier icon, String label, String amount, int x, int y) {
        RustFontRenderer sf = RustFontRenderer.small();
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();
        RustUiHelper.drawSprite(matrices, this.client, icon, x, y, 16, 16, 0xFFFFFFFF);
        sf.draw(matrices, label, x + 20, y + 4, 0xCCF7EBE1);
        float aw = bfSmall.getWidth(amount);
        bfSmall.draw(matrices, amount, x + 160 - aw, y + 4, 0xFFF7EBE1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int panelW = 380;
        int panelH = 260;
        int px = cx - panelW / 2;
        int py = cy - panelH / 2;

        int leftX = px + 12;
        int btnW = 160;
        int btnH = 22;
        int btnX = leftX;
        int btnY = py + panelH - 32;

        if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            if (this.client.player != null) {
                String uuid = this.client.player.getUuidAsString();
                if (tc.authorizedPlayers.contains(uuid)) {
                    tc.authorizedPlayers.clear();
                    RustSoundManager.play("keylock_lock", 1.0f, 1.0f);
                } else {
                    tc.authorizedPlayers.add(uuid);
                    RustSoundManager.play("keylock_unlock", 1.0f, 1.0f);
                }
                RustDeployableManager.save();
            }
            return true;
        }

        // Quick deposit from player inventory into TC on click
        int gridX = px + 188;
        int gridY = py + 36;
        int slotSize = 28;
        int gap = 4;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 6; c++) {
                int sx = gridX + c * (slotSize + gap);
                int sy = gridY + r * (slotSize + gap);
                int idx = r * 6 + c;
                if (mouseX >= sx && mouseX <= sx + slotSize && mouseY >= sy && mouseY <= sy + slotSize) {
                    if (this.client.player != null) {
                        // Move wood / stone from main hand into TC
                        ItemStack held = this.client.player.getMainHandStack();
                        if (!held.isEmpty()) {
                            tc.inventory.set(idx, held.copy());
                            held.setCount(0);
                            tc.protectedUntil = System.currentTimeMillis() + 86400000L; // 24 hours
                            RustSoundManager.play("inventory_pickup_wood", 1.0f, 1.0f);
                            RustDeployableManager.save();
                            return true;
                        }
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
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
