package net.spacerust.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.spacerust.client.gui.RustFontRenderer;
import net.spacerust.client.gui.RustServerBrowserScreen;
import net.spacerust.client.gui.RustUiHelper;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/**
 * Replaces vanilla dirt server-connect screen with authentic Rust Server Loading Screen.
 */
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen {

    private static final Identifier TEX_LOADING_BG = new Identifier("spacerust", "textures/gui/rust_ui/loading_bg.png");
    private static final Identifier TEX_SERVER_AVATAR = new Identifier("spacerust", "textures/gui/logo.png");

    @Shadow
    private Text status;

    protected ConnectScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void repositionCancelButton(CallbackInfo ci) {
        if (!this.buttons.isEmpty()) {
            ClickableWidget cancel = this.buttons.get(0);
            cancel.x = this.width - 118;
            cancel.y = this.height - 46;
            cancel.setWidth(96);
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderRustLoadingScreen(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        RustFontRenderer.ensureLoaded();
        int w = this.width;
        int h = this.height;

        // 1. Fullscreen dark Hazmat Soldier background
        DrawableHelper.fill(matrices, 0, 0, w, h, 0xFF0E0E0E);
        if (this.client != null) {
            RenderSystem.enableTexture();
            RenderSystem.color4f(0.85f, 0.85f, 0.85f, 1.0f);
            this.client.getTextureManager().bindTexture(TEX_LOADING_BG);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            DrawableHelper.drawTexture(matrices, 0, 32, w, Math.max(100, h - 92), 0f, 0f, 1, 1, 1, 1);
            RenderSystem.color4f(1f, 1f, 1f, 1f);
        }

        // Subtle atmospheric bottom/top vignette
        DrawableHelper.fill(matrices, 0, 0, w, 34, 0xF5111111);
        DrawableHelper.fill(matrices, 0, h - 62, w, h, 0xF0101010);

        // 2. Top Server Info Header
        RustServerBrowserScreen.ServerEntry srv = RustServerBrowserScreen.activeServer;
        String serverName = srv != null ? srv.name : "SpaceRust Server";
        String playersStr = srv != null ? (srv.currentPlayers + "/" + srv.maxPlayers) : "1/350";
        String mapStr = srv != null ? srv.map.toUpperCase(Locale.ROOT) : "PROCEDURAL MAP";
        String modeStr = srv != null ? srv.modeBadge.toUpperCase(Locale.ROOT) : "SURVIVAL";
        String wipeStr = srv != null ? srv.wipeTag.toUpperCase(Locale.ROOT) : "СЕГОДНЯ";

        if (this.client != null) {
            RustUiHelper.drawSprite(matrices, this.client, TEX_SERVER_AVATAR, 14, 7, 20, 20, 0xFFFFFFFF);
        } else {
            DrawableHelper.fill(matrices, 14, 7, 34, 27, 0xFFCD412B);
        }
        RustFontRenderer.bold().drawCenteredY(matrices, serverName, 42, 0, 34, 0xFFF7EBE1);

        // Right-aligned info pills
        RustFontRenderer sf = RustFontRenderer.small();
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        String[] labels = { "ИГРОКИ", "КАРТА", "РЕЖИМ", "ВАЙП БЫЛ" };
        String[] vals = { playersStr, mapStr, modeStr, wipeStr };
        int rx = w - 14;
        for (int i = labels.length - 1; i >= 0; i--) {
            int valW = Math.round(bf.getWidth(vals[i])) + 12;
            int lblW = Math.round(sf.getWidth(labels[i]));
            int boxX = rx - valW;
            DrawableHelper.fill(matrices, boxX, 8, rx, 26, 0xFF232323);
            bf.drawCenteredY(matrices, vals[i], boxX + 6, 8, 18, 0xFFF7EBE1);
            sf.drawCenteredY(matrices, labels[i], boxX - lblW - 5, 8, 18, 0x99F7EBE1);
            rx = boxX - lblW - 14;
        }

        // 3. Bottom-Left Hint Strip
        String tip = "НАЖМИТЕ G ДЛЯ КАРТЫ ОСТРОВА • TAB — ИНВЕНТАРЬ И КРАФТ • V — ГОЛОСОВОЙ ЧАТ";
        DrawableHelper.fill(matrices, 16, h - 38, 30, h - 24, 0xFF444444);
        sf.drawCenteredY(matrices, tip, 38, h - 42, 22, 0xCCF7EBE1);

        // 4. Bottom-Right Loading Status & Cancel Button
        int cancelX = w - 118;
        int cancelY = h - 46;
        int cancelW = 96;
        int cancelH = 26;
        boolean cancelHover = RustUiHelper.inRect(mouseX, mouseY, cancelX, cancelY, cancelW, cancelH);

        DrawableHelper.fill(matrices, cancelX, cancelY, cancelX + cancelW, cancelY + cancelH,
                cancelHover ? 0xFFE34D34 : 0xFFCD412B);
        String cancelLbl = "×  ОТМЕНА";
        bf.drawCenteredY(matrices, cancelLbl, cancelX + (cancelW - bf.getWidth(cancelLbl)) / 2f, cancelY, cancelH, 0xFFFFFFFF);

        String stage = localizeStatus(this.status);
        String title = "ЗАГРУЗКА";
        RustFontRenderer titleFont = RustFontRenderer.boldLarge();
        float tw = titleFont.getWidth(title);
        float sw = sf.getWidth(stage);
        titleFont.drawShadow(matrices, title, cancelX - tw - 16, h - 50, 0xFFF7EBE1);
        sf.drawShadow(matrices, stage, cancelX - sw - 16, h - 28, 0xBBF7EBE1);

        ci.cancel();
    }

    private static String localizeStatus(Text raw) {
        if (raw == null) return "ОТКРЫТИЕ СЦЕНЫ";
        String s = raw.getString().toLowerCase(Locale.ROOT);
        if (s.contains("encrypt") || s.contains("author") || s.contains("logging")) return "АВТОРИЗАЦИЯ И ВХОД";
        if (s.contains("terrain") || s.contains("join")) return "ОТКРЫТИЕ СЦЕНЫ";
        if (s.contains("connect")) return "ПОДКЛЮЧЕНИЕ К СЕРВЕРУ";
        return raw.getString().toUpperCase(Locale.ROOT);
    }
}
