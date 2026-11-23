package net.spacerust.client.mixin;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.spacerust.client.gui.RustFontRenderer;
import net.spacerust.client.gui.RustServerBrowserScreen;
import net.spacerust.client.gui.RustUiHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/**
 * Replaces vanilla server-connect screen with SpaceRust Loading Screen (1.21.11).
 */
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen {

    private static final Identifier TEX_LOADING_BG = Identifier.of("spacerust", "textures/gui/rust_ui/loading_bg.png");
    private static final Identifier TEX_SERVER_AVATAR = Identifier.of("spacerust", "textures/gui/logo.png");

    @Shadow
    private Text status;

    protected ConnectScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void repositionCancelButton(CallbackInfo ci) {
        for (Element el : this.children()) {
            if (el instanceof ClickableWidget cancel) {
                cancel.setX(this.width - 118);
                cancel.setY(this.height - 46);
                cancel.setWidth(96);
                break;
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderRustLoadingScreen(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        RustFontRenderer.ensureLoaded();
        int w = this.width;
        int h = this.height;

        ctx.fill(0, 0, w, h, 0xFF0E0E0E);
        if (this.client != null) {
            int bh = Math.max(100, h - 92);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, TEX_LOADING_BG, 0, 32, 0f, 0f, w, bh, w, bh, 0xFFD9D9D9);
        }

        ctx.fill(0, 0, w, 34, 0xF5111111);
        ctx.fill(0, h - 62, w, h, 0xF0101010);

        RustServerBrowserScreen.ServerEntry srv = RustServerBrowserScreen.activeServer;
        String serverName = srv != null ? srv.name : "Наша | SpaceRust Server";
        String playersStr = srv != null ? (srv.currentPlayers + "/" + srv.maxPlayers) : "1/20";
        String mapStr = srv != null ? srv.map.toUpperCase(Locale.ROOT) : "PROCEDURAL MAP";
        String modeStr = srv != null ? srv.modeBadge.toUpperCase(Locale.ROOT) : "SPACERUST";
        String wipeStr = srv != null ? srv.wipeTag.toUpperCase(Locale.ROOT) : "ЕЖЕМЕСЯЧНЫЙ";

        if (this.client != null) {
            RustUiHelper.drawSprite(ctx, this.client, TEX_SERVER_AVATAR, 14, 7, 20, 20, 0xFFFFFFFF);
        } else {
            ctx.fill(14, 7, 34, 27, 0xFFCD412B);
        }
        RustFontRenderer.bold().drawCenteredY(ctx, serverName, 42, 0, 34, 0xFFF7EBE1);

        RustFontRenderer sf = RustFontRenderer.small();
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        String[] labels = { "ИГРОКИ", "КАРТА", "РЕЖИМ", "ВАЙП БЫЛ" };
        String[] vals = { playersStr, mapStr, modeStr, wipeStr };
        int rx = w - 14;
        for (int i = labels.length - 1; i >= 0; i--) {
            int valW = Math.round(bf.getWidth(vals[i])) + 12;
            int lblW = Math.round(sf.getWidth(labels[i]));
            int boxX = rx - valW;
            ctx.fill(boxX, 8, rx, 26, 0xFF232323);
            bf.drawCenteredY(ctx, vals[i], boxX + 6, 8, 18, 0xFFF7EBE1);
            sf.drawCenteredY(ctx, labels[i], boxX - lblW - 5, 8, 18, 0x99F7EBE1);
            rx = boxX - lblW - 14;
        }

        String tip = "SPACERUST 1.21.11  •  ПОДКЛЮЧЕНИЕ К СЕРВЕРУ НАША";
        ctx.fill(16, h - 38, 30, h - 24, 0xFF444444);
        sf.drawCenteredY(ctx, tip, 38, h - 42, 22, 0xCCF7EBE1);

        int cancelX = w - 118, cancelY = h - 46, cancelW = 96, cancelH = 26;
        boolean cancelHover = RustUiHelper.inRect(mouseX, mouseY, cancelX, cancelY, cancelW, cancelH);
        ctx.fill(cancelX, cancelY, cancelX + cancelW, cancelY + cancelH, cancelHover ? 0xFFE34D34 : 0xFFCD412B);
        String cancelLbl = "×  ОТМЕНА";
        bf.drawCenteredY(ctx, cancelLbl, cancelX + (cancelW - bf.getWidth(cancelLbl)) / 2f, cancelY, cancelH, 0xFFFFFFFF);

        String stage = localizeStatus(this.status);
        String title = "ЗАГРУЗКА";
        RustFontRenderer titleFont = RustFontRenderer.boldLarge();
        titleFont.drawShadow(ctx, title, cancelX - titleFont.getWidth(title) - 16, h - 50, 0xFFF7EBE1);
        sf.drawShadow(ctx, stage, cancelX - sf.getWidth(stage) - 16, h - 28, 0xBBF7EBE1);

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
