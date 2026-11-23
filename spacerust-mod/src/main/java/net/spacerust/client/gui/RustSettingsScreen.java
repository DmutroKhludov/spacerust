package net.spacerust.client.gui;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

/**
 * Fullscreen/In-game Rust Settings wrapper opened when pressing Esc -> Options...
 */
public class RustSettingsScreen extends Screen {

    private final Screen parent;
    private final RustSettingsPanel panel = new RustSettingsPanel();

    public RustSettingsScreen(Screen parent) {
        super(new LiteralText("НАСТРОЙКИ SPACERUST"));
        this.parent = parent;
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
        panel.onClosed();
        RustUiHelper.disableWorldBlur(this.client);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        DrawableHelper.fill(matrices, 0, 0, this.width, this.height, 0xCC111316);

        // Top header bar
        int topH = 30;
        DrawableHelper.fill(matrices, 0, 0, this.width, topH, 0xDF16181C);
        DrawableHelper.fill(matrices, 0, topH, this.width, topH + 1, 0x22FFFFFF);
        RustFontRenderer.bold().drawCenteredY(matrices, "НАСТРОЙКИ", 16, 0, topH, 0xFFFFFFFF);

        // Close / Done button on top-right
        int doneW = 90;
        int doneH = 20;
        int doneX = this.width - doneW - 14;
        int doneY = 5;
        boolean hov = RustUiHelper.inRect(mouseX, mouseY, doneX, doneY, doneW, doneH);
        DrawableHelper.fill(matrices, doneX, doneY, doneX + doneW, doneY + doneH, hov ? 0xFFE04E36 : 0xFFCD412B);
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        String doneTxt = "ГОТОВО";
        bf.drawCenteredY(matrices, doneTxt, doneX + (doneW - bf.getWidth(doneTxt)) / 2f, doneY, doneH, 0xFFFFFFFF);

        panel.render(matrices, this, this.client, mouseX, mouseY, 14, topH + 12, this.width - 28, this.height - topH - 22);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int doneW = 90;
        int doneX = this.width - doneW - 14;
        if (button == 0 && RustUiHelper.inRect(mouseX, mouseY, doneX, 5, doneW, 20)) {
            RustUiHelper.playMenuClick(this.client);
            closeToParent();
            return true;
        }
        if (panel.mouseClicked(this, this.client, mouseX, mouseY, button, 14, 42, this.width - 28, this.height - 52)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (panel.mouseDragged(mouseX, 14, this.width - 28)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (panel.mouseReleased(this.client)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (panel.mouseScrolled(amount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (panel.charTyped(chr)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (panel.keyPressed(this.client, keyCode, scanCode)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeToParent();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void closeToParent() {
        if (this.client != null) {
            this.client.openScreen(parent);
        }
    }
}
