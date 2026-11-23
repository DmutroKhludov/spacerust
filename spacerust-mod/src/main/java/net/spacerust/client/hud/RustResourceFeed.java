package net.spacerust.client.hud;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.spacerust.client.gui.RustFontRenderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Renders the iconic Rust stacked green resource-gain feed
 * right above the bottom-right metabolism bars (e.g. "+30 КАМЕНЬ", "+75 ЖЕЛЕЗНАЯ РУДА").
 */
public final class RustResourceFeed {

    private static final long LIFETIME_MS = 3800L;
    private static final long FADE_MS = 700L;
    private static final int MAX_ROWS = 10;

    private static class Toast {
        final String label;
        final int amount;
        final long createdMs;

        Toast(String label, int amount, long createdMs) {
            this.label = label;
            this.amount = amount;
            this.createdMs = createdMs;
        }
    }

    private static final List<Toast> ENTRIES = new ArrayList<>();

    private RustResourceFeed() {}

    public static void add(String label, int amount) {
        if (label == null || label.isEmpty() || amount <= 0) return;
        synchronized (ENTRIES) {
            ENTRIES.add(0, new Toast(label.toUpperCase(Locale.ROOT), amount, System.currentTimeMillis()));
            while (ENTRIES.size() > MAX_ROWS) {
                ENTRIES.remove(ENTRIES.size() - 1);
            }
        }
    }

    public static void clear() {
        synchronized (ENTRIES) {
            ENTRIES.clear();
        }
    }

    public static void render(MatrixStack matrices, int screenW, int screenH,
                              boolean statusRowActive, boolean craftRowActive) {
        long now = System.currentTimeMillis();
        List<Toast> snapshot;
        synchronized (ENTRIES) {
            Iterator<Toast> it = ENTRIES.iterator();
            while (it.hasNext()) {
                if (now - it.next().createdMs >= LIFETIME_MS) {
                    it.remove();
                }
            }
            if (ENTRIES.isEmpty()) return;
            snapshot = new ArrayList<>(ENTRIES);
        }

        RustFontRenderer sf = RustFontRenderer.boldSmall();
        int rowW = 114;
        int rowH = 13;
        int gap = 1;
        int x = screenW - rowW - 10;
        int bottomY = statusRowActive ? (screenH - 72) : (screenH - 57);
        if (craftRowActive) {
            bottomY -= 17;
        }

        for (int i = 0; i < snapshot.size(); i++) {
            Toast t = snapshot.get(i);
            long age = now - t.createdMs;
            float alpha = 1.0f;
            if (age > LIFETIME_MS - FADE_MS) {
                alpha = Math.max(0.05f, (LIFETIME_MS - age) / (float) FADE_MS);
            }

            int y = bottomY - (i + 1) * rowH - i * gap;
            if (y < 20) break;

            int bgA = Math.round(alpha * 208) << 24;
            int sideA = Math.round(alpha * 230) << 24;
            int txtA = Math.round(alpha * 255) << 24;
            int iconA = Math.round(alpha * 255) << 24;

            // Olive-green Rust toast body + darker left badge strip
            DrawableHelper.fill(matrices, x, y, x + rowW, y + rowH, bgA | 0x4E6E2B);
            DrawableHelper.fill(matrices, x, y, x + 13, y + rowH, sideA | 0x3A541F);

            // Crisp miniature bright-green pickup arrow inside left square
            int ix = x + 6;
            int iy = y + 3;
            int iconCol = iconA | 0x9DE84A;
            DrawableHelper.fill(matrices, ix, iy, ix + 1, iy + 5, iconCol);
            DrawableHelper.fill(matrices, ix - 2, iy + 3, ix + 3, iy + 4, iconCol);
            DrawableHelper.fill(matrices, ix - 1, iy + 4, ix + 2, iy + 5, iconCol);
            DrawableHelper.fill(matrices, ix, iy + 5, ix + 1, iy + 6, iconCol);

            String plusStr = "+" + t.amount;
            int plusW = Math.round(sf.getWidth(plusStr));
            String trimmedLabel = sf.trim(t.label, rowW - 22 - plusW);

            sf.drawCenteredY(matrices, trimmedLabel, x + 16, y, rowH, txtA | 0xF1F6EB);
            sf.drawCenteredY(matrices, plusStr, x + rowW - plusW - 4, y, rowH, txtA | 0xFFF2C8);
        }
    }
}
