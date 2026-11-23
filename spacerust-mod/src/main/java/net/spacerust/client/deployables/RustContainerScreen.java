package net.spacerust.client.deployables;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.spacerust.client.gui.RustFontRenderer;
import net.spacerust.client.gui.RustUiHelper;
import net.spacerust.client.sound.RustSoundManager;
import org.lwjgl.glfw.GLFW;

public class RustContainerScreen extends Screen {

    private final RustDeployableData box;

    public RustContainerScreen(RustDeployableData box) {
        super(new LiteralText(box.type.title));
        this.box = box;
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

        int cols = (box.type == RustDeployableType.SMALL_BOX) ? 4 : 6;
        int rows = (box.type == RustDeployableType.SMALL_BOX) ? 3 : 5;
        int totalSlots = cols * rows;

        int slotSize = 32;
        int gap = 4;
        int gridW = cols * (slotSize + gap) - gap;
        int gridH = rows * (slotSize + gap) - gap;

        int padW = gridW + 36;
        int padH = gridH + 64;
        int px = cx - padW / 2;
        int py = cy - padH / 2;

        // Background
        DrawableHelper.fill(matrices, px, py, px + padW, py + padH, 0xF0181A16);
        RustUiHelper.drawBorder(matrices, px, py, padW, padH, 0x44F7EBE1);

        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();
        RustFontRenderer sf = RustFontRenderer.small();

        // Header
        DrawableHelper.fill(matrices, px, py, px + padW, py + 26, 0x99242820);
        RustUiHelper.drawSprite(matrices, this.client, box.type.icon, px + 8, py + 4, 18, 18, 0xFFFFFFFF);
        bfSmall.draw(matrices, box.type.title.toUpperCase(), px + 32, py + 8, 0xFFF7EBE1);

        // Slots grid
        int startX = px + 18;
        int startY = py + 38;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int sx = startX + c * (slotSize + gap);
                int sy = startY + r * (slotSize + gap);
                int idx = r * cols + c;

                boolean hov = mouseX >= sx && mouseX <= sx + slotSize && mouseY >= sy && mouseY <= sy + slotSize;
                DrawableHelper.fill(matrices, sx, sy, sx + slotSize, sy + slotSize, hov ? 0xCC3E4532 : 0xBB232522);
                RustUiHelper.drawBorder(matrices, sx, sy, slotSize, slotSize, hov ? 0xFFFFFFFF : 0x22F7EBE1);

                if (idx < box.inventory.size()) {
                    ItemStack st = box.inventory.get(idx);
                    if (!st.isEmpty()) {
                        this.itemRenderer.renderInGui(st, sx + 8, sy + 8);
                        this.itemRenderer.renderGuiItemOverlay(this.textRenderer, st, sx + 8, sy + 8);
                    }
                }
            }
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int cols = (box.type == RustDeployableType.SMALL_BOX) ? 4 : 6;
        int rows = (box.type == RustDeployableType.SMALL_BOX) ? 3 : 5;

        int slotSize = 32;
        int gap = 4;
        int gridW = cols * (slotSize + gap) - gap;
        int gridH = rows * (slotSize + gap) - gap;
        int padW = gridW + 36;
        int padH = gridH + 64;
        int px = cx - padW / 2;
        int py = cy - padH / 2;

        int startX = px + 18;
        int startY = py + 38;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int sx = startX + c * (slotSize + gap);
                int sy = startY + r * (slotSize + gap);
                int idx = r * cols + c;

                if (mouseX >= sx && mouseX <= sx + slotSize && mouseY >= sy && mouseY <= sy + slotSize) {
                    if (this.client.player != null) {
                        ItemStack held = this.client.player.getMainHandStack();
                        ItemStack inSlot = (idx < box.inventory.size()) ? box.inventory.get(idx) : ItemStack.EMPTY;

                        // Quick deposit from hand to slot
                        if (!held.isEmpty() && inSlot.isEmpty()) {
                            box.inventory.set(idx, held.copy());
                            held.setCount(0);
                            RustSoundManager.play("inventory_pickup_wood", 1.0f, 1.0f);
                            RustDeployableManager.save();
                            return true;
                        } else if (held.isEmpty() && !inSlot.isEmpty()) {
                            // Quick withdraw to player
                            this.client.player.inventory.offerOrDrop(this.client.world, inSlot.copy());
                            box.inventory.set(idx, ItemStack.EMPTY);
                            RustSoundManager.play("inventory_pickup_wood", 1.0f, 1.2f);
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
