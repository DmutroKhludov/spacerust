package net.spacerust.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;
import net.spacerust.client.SpaceRustClient;
import net.spacerust.client.config.RustSettings;

/**
 * Shared SpaceRust UI primitives, sprites, sound triggers, and 1.21.11 blur helper.
 */
public final class RustUiHelper {

    public static final Identifier TEX_BTN_CLOSE = Identifier.of("spacerust", "textures/gui/rust_ui/btn_close.png");
    public static final Identifier TEX_SEARCH    = Identifier.of("spacerust", "textures/gui/icon_search.png");

    private RustUiHelper() {}

    public static void playMenuClick(MinecraftClient client) {
        float uiVol = RustSettings.get().uiVolume;
        if (uiVol > 0.005f && client != null && client.getSoundManager() != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(SpaceRustClient.UI_CLICK_SOUND, 1.0f, 0.55f * uiVol));
        }
    }

    public static void playMenuHover(MinecraftClient client) {
        float uiVol = RustSettings.get().uiVolume;
        if (uiVol > 0.005f && client != null && client.getSoundManager() != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(SpaceRustClient.UI_HOVER_SOUND, 1.0f, 0.28f * uiVol));
        }
    }

    public static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void drawSprite(DrawContext ctx, MinecraftClient client, Identifier tex,
                                  int x, int y, int w, int h, int argb) {
        if (client == null || tex == null || w <= 0 || h <= 0) return;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x, y, 0.0f, 0.0f, w, h, w, h, argb);
    }
}
