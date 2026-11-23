package net.spacerust.client.deployables;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.spacerust.client.gui.RustFontRenderer;
import net.spacerust.client.gui.RustUiHelper;
import net.spacerust.client.sound.RustSoundManager;
import org.lwjgl.glfw.GLFW;

public class RustCodeLockScreen extends Screen {

    public static final Identifier TEX_KEYPAD = new Identifier("spacerust", "textures/gui/rust_icons/code_lock.png");
    private final RustDeployableData target;
    private String enteredCode = "";
    private String statusMsg = "ВВЕДИТЕ 4-ЗНАЧНЫЙ КОД";
    private int statusColor = 0xFFCCCCCC;
    private long statusTimer = 0;

    public RustCodeLockScreen(RustDeployableData target) {
        super(new LiteralText("Code Lock"));
        this.target = target;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        DrawableHelper.fill(matrices, 0, 0, this.width, this.height, 0xAA121110);

        int cx = this.width / 2;
        int cy = this.height / 2;

        int padW = 200;
        int padH = 260;
        int padX = cx - padW / 2;
        int padY = cy - padH / 2;

        // Background
        DrawableHelper.fill(matrices, padX, padY, padX + padW, padY + padH, 0xF0181A16);
        RustUiHelper.drawBorder(matrices, padX, padY, padW, padH, 0x44F7EBE1);

        RustFontRenderer bf = RustFontRenderer.bold();
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();
        RustFontRenderer sf = RustFontRenderer.small();

        // Header
        DrawableHelper.fill(matrices, padX, padY, padX + padW, padY + 24, 0x99242820);
        bfSmall.draw(matrices, "КОДОВЫЙ ЗАМОК", padX + 12, padY + 7, 0xFFF7EBE1);

        // Display Screen
        int dispX = padX + 16;
        int dispY = padY + 34;
        int dispW = padW - 32;
        int dispH = 36;
        DrawableHelper.fill(matrices, dispX, dispY, dispX + dispW, dispY + dispH, 0xFF10120E);
        RustUiHelper.drawBorder(matrices, dispX, dispY, dispW, dispH, 0x33F7EBE1);

        // Bullets / Digits
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i < enteredCode.length()) {
                sb.append("• ");
            } else {
                sb.append("- ");
            }
        }
        String dots = sb.toString().trim();
        float dw = bf.getWidth(dots);
        bf.draw(matrices, dots, dispX + (dispW - dw) / 2f, dispY + 10, 0xFF8DC63F);

        // Status text
        float sw = sf.getWidth(statusMsg);
        sf.draw(matrices, statusMsg, cx - sw / 2f, dispY + dispH + 6, statusColor);

        // Keypad Grid (3x4)
        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"C", "0", "OK"}
        };

        int btnW = 48;
        int btnH = 30;
        int startX = padX + 22;
        int startY = padY + 92;

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                int bx = startX + col * (btnW + 6);
                int by = startY + row * (btnH + 6);

                boolean hov = mouseX >= bx && mouseX <= bx + btnW && mouseY >= by && mouseY <= by + btnH;
                String k = keys[row][col];
                int bg = hov ? 0xFF4A5B2C : 0xFF2A2E26;
                if (k.equals("C") && hov) bg = 0xFF8B2A22;
                if (k.equals("OK") && hov) bg = 0xFF2E6B22;

                DrawableHelper.fill(matrices, bx, by, bx + btnW, by + btnH, bg);
                RustUiHelper.drawBorder(matrices, bx, by, btnW, btnH, hov ? 0xFFFFFFFF : 0x33F7EBE1);

                float kw = bfSmall.getWidth(k);
                bfSmall.draw(matrices, k, bx + (btnW - kw) / 2f, by + 9, 0xFFF7EBE1);
            }
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void pressKey(String k) {
        if (k.equals("C")) {
            enteredCode = "";
            RustSoundManager.play("ui_click", 0.8f, 1.0f);
        } else if (k.equals("OK")) {
            submitCode();
        } else {
            if (enteredCode.length() < 4) {
                enteredCode += k;
                RustSoundManager.play("ui_click", 0.9f, 1.2f + enteredCode.length() * 0.1f);
                if (enteredCode.length() == 4) {
                    submitCode();
                }
            }
        }
    }

    private void submitCode() {
        if (enteredCode.length() < 4) {
            statusMsg = "КОД ДОЛЖЕН БЫТЬ ИЗ 4 ЦИФР";
            statusColor = 0xFFE0462C;
            return;
        }

        // If lock has no code set yet, set this as the master code
        if (target.lockCode == null || target.lockCode.isEmpty()) {
            target.lockCode = enteredCode;
            target.isLocked = true;
            if (this.client.player != null) {
                target.authorizedPlayers.add(this.client.player.getUuidAsString());
            }
            RustDeployableManager.save();
            RustSoundManager.play("door_lock_install", 1.0f, 1.0f);
            this.onClose();
            return;
        }

        // Verify code
        if (target.lockCode.equals(enteredCode)) {
            statusMsg = "ДОСТУП РАЗРЕШЕН!";
            statusColor = 0xFF8DC63F;
            RustSoundManager.play("keylock_unlock", 1.0f, 1.2f);
            if (this.client.player != null) {
                target.authorizedPlayers.add(this.client.player.getUuidAsString());
            }
            RustDeployableManager.save();
            this.onClose();
        } else {
            statusMsg = "НЕВЕРНЫЙ КОД!";
            statusColor = 0xFFE0462C;
            enteredCode = "";
            RustSoundManager.play("door_denied", 1.0f, 1.0f);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int padW = 200;
        int padH = 260;
        int padX = cx - padW / 2;
        int padY = cy - padH / 2;

        int btnW = 48;
        int btnH = 30;
        int startX = padX + 22;
        int startY = padY + 92;

        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"C", "0", "OK"}
        };

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                int bx = startX + col * (btnW + 6);
                int by = startY + row * (btnH + 6);
                if (mouseX >= bx && mouseX <= bx + btnW && mouseY >= by && mouseY <= by + btnH) {
                    pressKey(keys[row][col]);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            pressKey(String.valueOf(keyCode - GLFW.GLFW_KEY_0));
            return true;
        } else if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            pressKey(String.valueOf(keyCode - GLFW.GLFW_KEY_KP_0));
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            submitCode();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_C) {
            pressKey("C");
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
