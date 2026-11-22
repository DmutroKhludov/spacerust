package net.spacerust.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders smooth anti-aliased TrueType fonts (Roboto Condensed Bold/Regular)
 * baked at 2x oversampling into an OpenGL texture atlas for a native Rust UI look.
 */
public class RustFontRenderer {

    private static final float OVERSAMPLE = 2.0f;
    private static final int ATLAS_SIZE = 1024;
    private static final int PAD = 2;

    private static RustFontRenderer boldLarge;   // 13 GUI-px (headers/titles)
    private static RustFontRenderer boldMedium;  // 10.5 GUI-px (tabs/server names/buttons)
    private static RustFontRenderer boldSmall;   // 8.5 GUI-px (pills/badges)
    private static RustFontRenderer regularMed;  // 9.5 GUI-px (body/filters/settings)
    private static RustFontRenderer regularSmall;// 8.0 GUI-px (subtitles/micro text)

    public static void ensureLoaded() {
        if (boldLarge != null) return;
        boldLarge = new RustFontRenderer("roboto_condensed_bold.ttf", 13.0f, "bold_13");
        boldMedium = new RustFontRenderer("roboto_condensed_bold.ttf", 10.5f, "bold_10_5");
        boldSmall = new RustFontRenderer("roboto_condensed_bold.ttf", 8.5f, "bold_8_5");
        regularMed = new RustFontRenderer("roboto_condensed_regular.ttf", 9.5f, "reg_9_5");
        regularSmall = new RustFontRenderer("roboto_condensed_regular.ttf", 8.0f, "reg_8_0");
    }

    public static RustFontRenderer boldLarge() { ensureLoaded(); return boldLarge; }
    public static RustFontRenderer bold() { ensureLoaded(); return boldMedium; }
    public static RustFontRenderer boldSmall() { ensureLoaded(); return boldSmall; }
    public static RustFontRenderer regular() { ensureLoaded(); return regularMed; }
    public static RustFontRenderer small() { ensureLoaded(); return regularSmall; }

    private static class Glyph {
        float u0, v0, u1, v1;
        float w, h;
        float advance;
    }

    private final Map<Character, Glyph> glyphs = new HashMap<>();
    private final Identifier textureId;
    private final float lineHeight;
    private final float capHeight;
    private final float capTopOffset;
    private final float guiSize;
    private boolean ready = false;

    private RustFontRenderer(String fontFile, float guiSize, String idSuffix) {
        this.guiSize = guiSize;
        this.textureId = new Identifier("spacerust", "font_atlas_" + idSuffix);
        float bakedHeight = guiSize * OVERSAMPLE;
        float computedLineH = guiSize * 1.2f;
        float computedCapH = guiSize * 0.72f;
        float computedCapOffset = guiSize * 0.16f;

        try {
            Font awtFont;
            try (InputStream is = RustFontRenderer.class.getResourceAsStream("/assets/spacerust/font/" + fontFile)) {
                if (is != null) {
                    awtFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.PLAIN, bakedHeight);
                } else {
                    awtFont = new Font("SansSerif", fontFile.contains("bold") ? Font.BOLD : Font.PLAIN, Math.round(bakedHeight));
                }
            }

            BufferedImage bi = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            g.setFont(awtFont);
            g.setColor(Color.WHITE);

            FontMetrics fm = g.getFontMetrics();
            int ascent = fm.getAscent();
            int descent = fm.getDescent();
            int cellH = ascent + descent + PAD * 2;
            computedLineH = (ascent + descent) / OVERSAMPLE;

            java.awt.geom.Rectangle2D capBounds = awtFont.createGlyphVector(g.getFontRenderContext(), "H").getVisualBounds();
            double capVisualH = capBounds.getHeight();
            double capTopFromBaseline = -capBounds.getY();
            if (capVisualH > 1.0) {
                computedCapH = (float) (capVisualH / OVERSAMPLE);
                computedCapOffset = (float) ((ascent - capTopFromBaseline) / OVERSAMPLE);
            }

            StringBuilder chars = new StringBuilder();
            for (char c = 0x0020; c <= 0x007E; c++) chars.append(c);
            for (char c = 0x0400; c <= 0x045F; c++) chars.append(c);
            chars.append("•—–«»№…✕−+●○✓☐☑⚑");

            int penX = PAD;
            int penY = PAD;

            for (int i = 0; i < chars.length(); i++) {
                char ch = chars.charAt(i);
                if (glyphs.containsKey(ch)) continue;

                int adv = Math.max(1, fm.charWidth(ch));
                int drawW = adv + PAD * 2;

                if (penX + drawW + PAD >= ATLAS_SIZE) {
                    penX = PAD;
                    penY += cellH + PAD;
                }
                if (penY + cellH >= ATLAS_SIZE) {
                    break;
                }

                if (ch != ' ') {
                    g.drawString(String.valueOf(ch), penX, penY + ascent);
                }

                Glyph glyph = new Glyph();
                glyph.u0 = (float) (penX - 1) / ATLAS_SIZE;
                glyph.v0 = (float) (penY - 1) / ATLAS_SIZE;
                glyph.u1 = (float) (penX + adv + 1) / ATLAS_SIZE;
                glyph.v1 = (float) (penY + ascent + descent + 1) / ATLAS_SIZE;
                glyph.w = (adv + 2) / OVERSAMPLE;
                glyph.h = (ascent + descent + 2) / OVERSAMPLE;
                glyph.advance = adv / OVERSAMPLE;
                glyphs.put(ch, glyph);

                penX += drawW + PAD;
            }
            g.dispose();

            NativeImage ni = new NativeImage(ATLAS_SIZE, ATLAS_SIZE, false);
            for (int y = 0; y < ATLAS_SIZE; y++) {
                for (int x = 0; x < ATLAS_SIZE; x++) {
                    int argb = bi.getRGB(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    ni.setPixelColor(x, y, a == 0 ? 0 : ((a << 24) | 0x00FFFFFF));
                }
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getTextureManager() != null) {
                client.getTextureManager().registerTexture(this.textureId, new NativeImageBackedTexture(ni));
                this.ready = true;
            } else {
                ni.close();
            }
        } catch (Exception e) {
            System.err.println("[SpaceRust] Failed to bake font " + fontFile + ": " + e.getMessage());
        }

        this.lineHeight = computedLineH;
        this.capHeight = computedCapH;
        this.capTopOffset = computedCapOffset;
    }

    public float getLineHeight() {
        return lineHeight;
    }

    public float getCenteredY(float boxY, float boxH) {
        return boxY + (boxH - capHeight) * 0.5f - capTopOffset;
    }

    public void drawCenteredY(MatrixStack matrices, String text, float x, float boxY, float boxH, int argb) {
        draw(matrices, text, x, getCenteredY(boxY, boxH), argb);
    }

    public void drawCenteredShadowY(MatrixStack matrices, String text, float x, float boxY, float boxH, int argb) {
        drawShadow(matrices, text, x, getCenteredY(boxY, boxH), argb);
    }

    public float getWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        if (!ready) {
            MinecraftClient c = MinecraftClient.getInstance();
            return c != null && c.textRenderer != null ? c.textRenderer.getWidth(text) : text.length() * (guiSize * 0.55f);
        }
        float w = 0;
        Glyph space = glyphs.get(' ');
        float fallbackAdv = space != null ? space.advance : guiSize * 0.4f;
        for (int i = 0; i < text.length(); i++) {
            Glyph g = glyphs.get(text.charAt(i));
            w += (g != null) ? g.advance : fallbackAdv;
        }
        return w;
    }

    public String trim(String text, float maxWidth) {
        if (text == null || text.isEmpty()) return "";
        if (getWidth(text) <= maxWidth) return text;
        String ell = "...";
        float ellW = getWidth(ell);
        StringBuilder sb = new StringBuilder();
        float w = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            Glyph g = glyphs.get(ch);
            float adv = g != null ? g.advance : (guiSize * 0.45f);
            if (w + adv + ellW > maxWidth) break;
            sb.append(ch);
            w += adv;
        }
        return sb + ell;
    }

    public void draw(MatrixStack matrices, String text, float x, float y, int argb) {
        if (text == null || text.isEmpty()) return;
        if (!ready) {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c != null && c.textRenderer != null) {
                c.textRenderer.draw(matrices, text, x, y, argb);
            }
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        if (a == 0) return;

        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        client.getTextureManager().bindTexture(this.textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        Matrix4f mat = matrices.peek().getModel();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE);

        float curX = x;
        Glyph space = glyphs.get(' ');
        float fallbackAdv = space != null ? space.advance : guiSize * 0.4f;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            Glyph gl = glyphs.get(ch);
            if (gl == null) {
                curX += fallbackAdv;
                continue;
            }
            if (ch != ' ') {
                float gx0 = curX - 0.5f;
                float gy0 = y - 0.5f;
                float gx1 = gx0 + gl.w;
                float gy1 = gy0 + gl.h;
                buf.vertex(mat, gx0, gy1, 0.0F).color(r, g, b, a).texture(gl.u0, gl.v1).next();
                buf.vertex(mat, gx1, gy1, 0.0F).color(r, g, b, a).texture(gl.u1, gl.v1).next();
                buf.vertex(mat, gx1, gy0, 0.0F).color(r, g, b, a).texture(gl.u1, gl.v0).next();
                buf.vertex(mat, gx0, gy0, 0.0F).color(r, g, b, a).texture(gl.u0, gl.v0).next();
            }
            curX += gl.advance;
        }
        tess.draw();
    }

    public void drawShadow(MatrixStack matrices, String text, float x, float y, int argb) {
        int a = (argb >>> 24) & 0xFF;
        int shadowAlpha = Math.max(0, Math.min(255, (int) (a * 0.45f)));
        if (shadowAlpha > 0) {
            draw(matrices, text, x + 0.6f, y + 0.6f, (shadowAlpha << 24));
        }
        draw(matrices, text, x, y, argb);
    }
}
