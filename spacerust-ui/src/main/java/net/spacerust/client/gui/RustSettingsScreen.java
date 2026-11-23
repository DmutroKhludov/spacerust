package net.spacerust.client.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Fullscreen/In-game SpaceRust Settings wrapper opened from Main Menu or Escape pause menu.
 */
public class RustSettingsScreen extends Screen {

    private final Screen parent;
    private final RustSettingsPanel panel = new RustSettingsPanel();

    public RustSettingsScreen(Screen parent) {
        super(Text.literal("НАСТРОЙКИ SPACERUST"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        RustFontRenderer.ensureLoaded();
    }

    @Override
    public void removed() {
        super.removed();
        panel.onClosed();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Custom backdrop + blur handled in render()
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (this.client != null && this.client.world != null) {
            ctx.applyBlur();
        }
        ctx.fill(0, 0, this.width, this.height, 0xCC111316);

        int topH = 30;
        ctx.fill(0, 0, this.width, topH, 0xDF16181C);
        ctx.fill(0, topH, this.width, topH + 1, 0x22FFFFFF);
        RustFontRenderer.bold().drawCenteredY(ctx, "НАСТРОЙКИ", 16, 0, topH, 0xFFFFFFFF);

        int doneW = 90;
        int doneH = 20;
        int doneX = this.width - doneW - 14;
        int doneY = 5;
        boolean hov = RustUiHelper.inRect(mouseX, mouseY, doneX, doneY, doneW, doneH);
        ctx.fill(doneX, doneY, doneX + doneW, doneY + doneH, hov ? 0xFFE04E36 : 0xFFCD412B);
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        String doneTxt = "ГОТОВО";
        bf.drawCenteredY(ctx, doneTxt, doneX + (doneW - bf.getWidth(doneTxt)) / 2f, doneY, doneH, 0xFFFFFFFF);

        panel.render(ctx, this, this.client, mouseX, mouseY, 14, topH + 12, this.width - 28, this.height - topH - 22);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int doneW = 90;
        int doneX = this.width - doneW - 14;
        if (click.button() == 0 && RustUiHelper.inRect(click.x(), click.y(), doneX, 5, doneW, 20)) {
            RustUiHelper.playMenuClick(this.client);
            closeToParent();
            return true;
        }
        if (panel.mouseClicked(this, this.client, click.x(), click.y(), click.button(), 14, 42, this.width - 28, this.height - 52)) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (panel.mouseDragged(click.x(), 14, this.width - 28)) {
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (panel.mouseReleased(this.client)) {
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (panel.mouseScrolled(verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(CharInput input) {
        String s = input.asString();
        if (s != null && !s.isEmpty() && panel.charTyped(s.charAt(0))) {
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (panel.keyPressed(this.client, input)) {
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            closeToParent();
            return true;
        }
        return super.keyPressed(input);
    }

    private void closeToParent() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
