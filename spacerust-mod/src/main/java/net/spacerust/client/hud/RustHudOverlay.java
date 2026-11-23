package net.spacerust.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.spacerust.client.gui.RustCraftingQueue;
import net.spacerust.client.gui.RustFontRenderer;
import net.spacerust.client.gui.RustUiHelper;
import net.spacerust.client.voice.RustVoiceEngine;
import net.spacerust.client.world.RustBuildingSystem;

import java.util.List;

public class RustHudOverlay implements HudRenderCallback {
    private static final Identifier TEX_HUD_CRAFT = new Identifier("spacerust", "textures/gui/rust_ui/hud_icon_craft.png");
    // Rust metabolism stats (can be updated from packets or player status)
    public static float health = 100f;
    public static int maxHealth = 100;
    public static int calories = 480;
    public static int hydration = 230;
    public static int radiation = 0;
    public static int comfort = 50;
    public static boolean isBleeding = false;
    private static long wakeupBannerUntilMs = 0L;
    private static int lastSeenWorldHash = 0;

    public static void triggerWakeupFade() {
        wakeupBannerUntilMs = System.currentTimeMillis() + 3600L;
    }

    @Override
    public void onHudRender(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) {
            return;
        }
        // Avoid drawing HUD bars twice when Rust inventory/crafting screen already renders them
        if (client.currentScreen != null) {
            return;
        }

        RustFontRenderer.ensureLoaded();
        health = client.player.getHealth() * 5.0f;
        calories = client.player.getHungerManager().getFoodLevel() * 25;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Trigger Rust "WELCOME TO SPACERUST" banner when joining/changing world
        if (client.world != null) {
            int wh = System.identityHashCode(client.world);
            if (wh != lastSeenWorldHash) {
                lastSeenWorldHash = wh;
                triggerWakeupFade();
            }
        }

        // Top-Center Rust Compass Strip (matches Gameplay Videos 1-3)
        drawTopCompass(matrices, client, screenWidth);

        // Draw Rust 6-slot Belt/Hotbar & bottom-right metabolism bars
        RustUiHelper.drawHotbar(matrices, client, -1, -1, screenWidth, screenHeight, client.getItemRenderer(), false);
        RustUiHelper.drawMetabolismBars(matrices, client, screenWidth, screenHeight);

        // Optional status chips above bars
        int statusX = screenWidth - 114;
        int statusY = screenHeight - 68;
        RustFontRenderer sf = RustFontRenderer.boldSmall();

        boolean cold = false;
        if (client.world != null && client.player != null) {
            net.minecraft.world.biome.Biome.Category cat = client.world.getBiome(client.player.getBlockPos()).getCategory();
            cold = (cat == net.minecraft.world.biome.Biome.Category.ICY || cat == net.minecraft.world.biome.Biome.Category.TAIGA);
        }

        if (radiation > 0) {
            DrawableHelper.fill(matrices, statusX, statusY, statusX + 52, statusY + 11, 0xE6C73E2B);
            sf.drawCenteredY(matrices, "РАД " + radiation, statusX + 4, statusY, 11, 0xFFFFFFFF);
        } else if (cold) {
            DrawableHelper.fill(matrices, statusX, statusY, statusX + 52, statusY + 11, 0xE6306B9E);
            sf.drawCenteredY(matrices, "ХОЛОД", statusX + 6, statusY, 11, 0xFFFFFFFF);
        }
        if (isBleeding) {
            DrawableHelper.fill(matrices, statusX + 54, statusY, statusX + 104, statusY + 11, 0xE6C73E2B);
            sf.drawCenteredY(matrices, "КРОВОТОК", statusX + 57, statusY, 11, 0xFFFFFFFF);
        } else if (comfort > 0 && radiation == 0 && !cold) {
            DrawableHelper.fill(matrices, statusX + 54, statusY, statusX + 104, statusY + 11, 0xCC3E822A);
            sf.drawCenteredY(matrices, "УЮТ " + comfort + "%", statusX + 57, statusY, 11, 0xFFFFFFFF);
        }

        // Crafting queue progress bar right above bottom-right metabolism bars (like Rust)
        boolean statusActive = radiation > 0 || isBleeding || cold;
        boolean craftActive = !RustCraftingQueue.queue.isEmpty();
        if (craftActive) {
            RustCraftingQueue.CraftTask active = RustCraftingQueue.queue.get(0);
            int totalQty = RustCraftingQueue.getTotalRemainingCount();
            int totalSec = Math.max(1, (int) Math.ceil(RustCraftingQueue.getTotalRemainingSeconds()));
            String timeStr = (totalSec >= 60) ? (totalSec / 60 + "м " + (totalSec % 60) + "с") : (totalSec + "с");
            String qtyStr = "x" + totalQty;

            int cw = 104;
            int ch = 15;
            int cx = screenWidth - cw - 10;
            int cy = statusActive ? (screenHeight - 85) : (screenHeight - 70);

            DrawableHelper.fill(matrices, cx, cy, cx + cw, cy + ch, 0xDD273324);
            int progW = Math.round((cw - 15) * RustCraftingQueue.getActiveUnitProgress());
            if (progW > 0) {
                DrawableHelper.fill(matrices, cx + 14, cy + 1, cx + 14 + progW, cy + ch - 1, 0xB8608E34);
            }
            DrawableHelper.fill(matrices, cx, cy, cx + 14, cy + ch, 0xEE1E271C);
            RustUiHelper.drawSprite(matrices, client, TEX_HUD_CRAFT, cx + 3, cy + 3, 9, 9, 0xFF9DF042);

            float timeW = sf.getWidth(timeStr);
            String label = sf.trim(qtyStr + " " + active.name, cw - 20 - Math.round(timeW));
            sf.drawCenteredY(matrices, label, cx + 16, cy, ch, 0xFFF7EBE1);
            sf.drawCenteredY(matrices, timeStr, cx + cw - timeW - 4, cy, ch, 0xFFBCE88B);
        }

        // Stacked Rust resource-gain notifications above crafting/metabolism bars
        RustResourceFeed.render(matrices, screenWidth, screenHeight, statusActive, craftActive);

        // Building Plan / Hammer info prompt
        if (RustBuildingSystem.isHoldingPlan(client)) {
            String pieceName = RustBuildingSystem.displayTypeName(RustBuildingSystem.selectedType);
            int wCost = RustBuildingSystem.woodCostFor(RustBuildingSystem.selectedType);
            String tip = pieceName + " (" + wCost + " Дерево)  |  ЛКМ Строить • ПКМ Меню • R Поворот";
            int tw = Math.round(sf.getWidth(tip)) + 14;
            int tx = (screenWidth - tw) / 2;
            int ty = screenHeight - 66;
            DrawableHelper.fill(matrices, tx, ty, tx + tw, ty + 13, 0xCC1E262E);
            sf.drawCenteredY(matrices, tip, tx + 7, ty, 13, 0xFFD9EFFC);
        } else if (RustBuildingSystem.isHoldingHammer(client)) {
            RustBuildingSystem.BuildHit bh = RustBuildingSystem.raycastBuildings(
                    client.player.getCameraPosVec(1.0f), client.player.getRotationVec(1.0f).normalize(), 6.5);
            if (bh != null) {
                RustBuildingSystem.BuildingPiece bp = bh.piece;
                String tip = RustBuildingSystem.displayTierName(bp.tier) + " • "
                        + RustBuildingSystem.displayTypeName(bp.type)
                        + "  [" + bp.hp + "/" + bp.maxHp + "]  •  ПКМ Улучшить";
                int tw = Math.round(sf.getWidth(tip)) + 14;
                int tx = (screenWidth - tw) / 2;
                int ty = screenHeight - 66;
                DrawableHelper.fill(matrices, tx, ty, tx + tw, ty + 13, 0xCC24201C);
                sf.drawCenteredY(matrices, tip, tx + 7, ty, 13, 0xFFF7EBE1);
            }
        }

        // Rust Proximity Voice Chat HUD indicator (top-center green badge + nearby speakers)
        RustVoiceEngine voice = RustVoiceEngine.get();
        boolean talking = voice.isSelfTalking();
        List<String> speakers = voice.getActiveSpeakerNames();
        if (talking) {
            String tag = "ГОЛОС ВКЛ";
            int bw = Math.round(sf.getWidth(tag)) + 18;
            int bx = (screenWidth - bw) / 2;
            int by = 26;
            DrawableHelper.fill(matrices, bx, by, bx + bw, by + 16, 0xDD4C6E29);
            DrawableHelper.fill(matrices, bx + 5, by + 5, bx + 10, by + 11, 0xFF9DF042);
            sf.drawCenteredY(matrices, tag, bx + 13, by, 16, 0xFFFFFFFF);
        }
        if (!speakers.isEmpty()) {
            int sx = 10;
            int sy = 10;
            for (String sp : speakers) {
                String txt = "♪ " + sp;
                int sw = Math.round(sf.getWidth(txt)) + 12;
                DrawableHelper.fill(matrices, sx, sy, sx + sw, sy + 14, 0xCC1E5E8A);
                sf.drawCenteredY(matrices, txt, sx + 6, sy, 14, 0xFFFFFFFF);
                sy += 16;
            }
        }

        // Wake-up / Spawn welcome banner ("WELCOME TO SPACERUST") matching Video 2
        long rem = wakeupBannerUntilMs - System.currentTimeMillis();
        if (rem > 0L) {
            float t = rem / 3600.0f; // 1 -> 0
            if (rem > 2600L) {
                float dark = Math.min(0.85f, (rem - 2600L) / 1000.0f);
                int da = Math.round(dark * 255) << 24;
                DrawableHelper.fill(matrices, 0, 0, screenWidth, screenHeight, da);
            }
            float alpha = Math.min(1.0f, t * 2.4f);
            int col = (Math.round(alpha * 255) << 24) | 0x00F7EBE1;
            int subCol = (Math.round(alpha * 210) << 24) | 0x00CD412B;
            RustFontRenderer hdf = RustFontRenderer.boldLarge();
            RustFontRenderer bf = RustFontRenderer.bold();
            String w1 = "WELCOME TO";
            String w2 = "SPACERUST";
            hdf.drawShadow(matrices, w1, (screenWidth - hdf.getWidth(w1)) / 2f - 28, screenHeight * 0.28f, col);
            hdf.drawShadow(matrices, w2, (screenWidth - hdf.getWidth(w2)) / 2f + 28, screenHeight * 0.28f + 16, subCol);
            if (rem > 2000L) {
                String awake = "ВЫ ПРОСНУЛИСЬ НА ОСТРОВЕ";
                bf.drawShadow(matrices, awake, (screenWidth - bf.getWidth(awake)) / 2f, screenHeight * 0.28f + 38, col);
            }
        }
    }

    private static void drawTopCompass(MatrixStack matrices, MinecraftClient client, int screenW) {
        if (client.player == null) return;
        float heading = (client.player.yaw + 180.0f) % 360.0f;
        if (heading < 0) heading += 360.0f;

        int barW = Math.min(280, screenW - 80);
        int cx = screenW / 2;
        int left = cx - barW / 2;
        int right = cx + barW / 2;
        float pxPerDeg = 1.9f;

        // Subtle center down-triangle indicator at top
        DrawableHelper.fill(matrices, cx - 3, 2, cx + 4, 4, 0xDDEE8833);
        DrawableHelper.fill(matrices, cx - 1, 4, cx + 2, 6, 0xDDEE8833);

        RustFontRenderer bf = RustFontRenderer.boldSmall();
        RustFontRenderer sf = RustFontRenderer.small();
        for (int deg = 0; deg < 360; deg += 15) {
            float diff = deg - heading;
            while (diff > 180.0f) diff -= 360.0f;
            while (diff < -180.0f) diff += 360.0f;
            float px = cx + diff * pxPerDeg;
            if (px < left || px > right) continue;

            float edgeDist = Math.min(px - left, right - px);
            float fade = Math.max(0.0f, Math.min(1.0f, edgeDist / 28.0f));
            int alpha = Math.round(fade * 235);
            if (alpha < 18) continue;

            int ix = Math.round(px);
            String label = compassLabel(deg);
            boolean cardinal = label.length() <= 2 && !Character.isDigit(label.charAt(0));
            int tickH = cardinal ? 5 : 3;
            int color = (alpha << 24) | 0x00F5F2EB;
            DrawableHelper.fill(matrices, ix, 7, ix + 1, 7 + tickH, color);

            RustFontRenderer f = cardinal ? bf : sf;
            f.drawShadow(matrices, label, px - f.getWidth(label) / 2f, 13, color);
        }
    }

    private static String compassLabel(int deg) {
        switch (deg) {
            case 0: return "N";
            case 45: return "NE";
            case 90: return "E";
            case 135: return "SE";
            case 180: return "S";
            case 225: return "SW";
            case 270: return "W";
            case 315: return "NW";
            default: return String.valueOf(deg);
        }
    }
}
