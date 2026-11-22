package net.spacerust.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.spacerust.client.SpaceRustClient;
import org.lwjgl.opengl.GL11;

import java.util.Locale;

/**
 * Authentic Rust / CoilRust UI primitives, colors, sprites, and shared widgets.
 * Palette & proportions derived from CoilRust's inventory_source.json,
 * hud_style.json, and prefab_layout.json.
 */
public final class RustUiHelper {

    // Exact extracted Rust/CoilRust sprites
    public static final Identifier TEX_SWITCH_CRAFTING = new Identifier("spacerust", "textures/gui/rust_ui/icon_crafting.png");
    public static final Identifier TEX_STAT_WASH       = new Identifier("spacerust", "textures/gui/rust_ui/stat_wash.png");
    public static final Identifier TEX_SELECTED_DROP   = new Identifier("spacerust", "textures/gui/rust_ui/selected_drop.png");
    public static final Identifier TEX_SELECTED_BG     = new Identifier("spacerust", "textures/gui/rust_ui/selected_bg.png");
    public static final Identifier TEX_STOPWATCH       = new Identifier("spacerust", "textures/gui/rust_ui/stopwatch.png");
    public static final Identifier TEX_FAV_ACTIVE      = new Identifier("spacerust", "textures/gui/rust_ui/fav_active.png");
    public static final Identifier TEX_FAV_INACTIVE    = new Identifier("spacerust", "textures/gui/rust_ui/fav_inactive.png");
    public static final Identifier TEX_BTN_ADD         = new Identifier("spacerust", "textures/gui/rust_ui/btn_add.png");
    public static final Identifier TEX_BTN_SUBTRACT    = new Identifier("spacerust", "textures/gui/rust_ui/btn_subtract.png");
    public static final Identifier TEX_BTN_MAX         = new Identifier("spacerust", "textures/gui/rust_ui/btn_max.png");
    public static final Identifier TEX_BTN_CLOSE       = new Identifier("spacerust", "textures/gui/rust_ui/btn_close.png");
    public static final Identifier TEX_BP_TILE         = new Identifier("spacerust", "textures/gui/rust_ui/bp_tile.png");
    public static final Identifier TEX_BP_LOCK         = new Identifier("spacerust", "textures/gui/rust_ui/bp_lock.png");

    // Categories
    public static final Identifier TEX_CAT_FAV          = new Identifier("spacerust", "textures/gui/rust_ui/cat_fav.png");
    public static final Identifier TEX_CAT_COMMON       = new Identifier("spacerust", "textures/gui/rust_ui/cat_common.png");
    public static final Identifier TEX_CAT_CONSTRUCTION = new Identifier("spacerust", "textures/gui/rust_ui/cat_construction.png");
    public static final Identifier TEX_CAT_ITEMS        = new Identifier("spacerust", "textures/gui/rust_ui/cat_items.png");
    public static final Identifier TEX_CAT_TOOLS        = new Identifier("spacerust", "textures/gui/rust_ui/cat_tools.png");
    public static final Identifier TEX_CAT_MEDICAL      = new Identifier("spacerust", "textures/gui/rust_ui/cat_medical.png");
    public static final Identifier TEX_CAT_WEAPONS      = new Identifier("spacerust", "textures/gui/rust_ui/cat_weapons.png");
    public static final Identifier TEX_CAT_AMMO         = new Identifier("spacerust", "textures/gui/rust_ui/cat_ammo.png");
    public static final Identifier TEX_CAT_ELECTRIC     = new Identifier("spacerust", "textures/gui/rust_ui/cat_electric.png");
    public static final Identifier TEX_CAT_FUN          = new Identifier("spacerust", "textures/gui/rust_ui/cat_fun.png");
    public static final Identifier TEX_CAT_OTHER        = new Identifier("spacerust", "textures/gui/rust_ui/cat_other.png");

    // HUD rust_kit sprites
    public static final Identifier TEX_HUD_BAR_BG      = new Identifier("spacerust", "textures/gui/rust_ui/hud_bar_bg.png");
    public static final Identifier TEX_HUD_HEALTH_FILL = new Identifier("spacerust", "textures/gui/rust_ui/hud_health_fill.png");
    public static final Identifier TEX_HUD_WATER_FILL  = new Identifier("spacerust", "textures/gui/rust_ui/hud_water_fill.png");
    public static final Identifier TEX_HUD_ICON_HEALTH = new Identifier("spacerust", "textures/gui/rust_ui/hud_icon_health.png");
    public static final Identifier TEX_HUD_ICON_WATER  = new Identifier("spacerust", "textures/gui/rust_ui/hud_icon_water.png");
    public static final Identifier TEX_HUD_ICON_FOOD   = new Identifier("spacerust", "textures/gui/rust_ui/hud_icon_food.png");
    public static final Identifier TEX_HUD_ICON_CRAFT  = new Identifier("spacerust", "textures/gui/rust_ui/hud_icon_craft.png");
    public static final Identifier TEX_SHIELD          = new Identifier("spacerust", "textures/gui/icon_shield.png");
    public static final Identifier TEX_SEARCH          = new Identifier("spacerust", "textures/gui/icon_search.png");
    public static final Identifier TEX_ARMOR           = new Identifier("spacerust", "textures/gui/icon_armor.png");

    // Exact Rust Color Palette (247, 235, 225 warm cream + 43, 41, 36 dark olive charcoal)
    public static final int COLOR_BACKDROP       = 0x990D0E0D;
    public static final int COLOR_SURFACE        = 0x10F7EBE1; // rgba(247,235,225,16)
    public static final int COLOR_HEADER         = 0x16F7EBE1; // rgba(247,235,225,22)
    public static final int COLOR_SLOT_EMPTY     = 0x12F7EBE1; // rgba(247,235,225,18)
    public static final int COLOR_SLOT_HOVER     = 0x26F7EBE1; // rgba(247,235,225,38)
    public static final int COLOR_SLOT_OCCUPIED  = 0xE82B2924; // rgba(43,41,36,232)
    public static final int COLOR_SELECTED_BLUE  = 0xC81F6BA0; // rgba(31,107,160,200)
    public static final int COLOR_CAT_ACTIVE     = 0xA0359FE9; // rgba(53,159,233,0.628)
    public static final int COLOR_TEXT_CREAM     = 0xDCF7EBE1; // rgba(247,235,225,220)
    public static final int COLOR_TEXT_DIM       = 0x80F7EBE1; // rgba(247,235,225,128)
    public static final int COLOR_ROW_BG         = 0x8C1D201F; // rgba(29,32,31,0.549)
    public static final int COLOR_GOLD_TEXT      = 0xFFEEB436; // rgba(238,180,54,1)
    public static final int COLOR_SEL_PANEL_BG   = 0xE63A3D35; // #3a3d35
    public static final int COLOR_SEL_DROP_BG    = 0xC29A9788; // rgba(154,151,136,0.76)
    public static final int COLOR_SEL_HEADER_BG  = 0x8F888474; // rgba(136,132,116,0.56)
    public static final int COLOR_QUEUE_TIME     = 0xF0AAEE31; // rgba(170,238,49,0.941)
    public static final int COLOR_QUEUE_CANCEL   = 0xFFE26C16; // rgba(226,108,22,1)
    public static final int COLOR_CRAFT_BTN      = 0xEB6B8B35; // Rust olive-green craft button
    public static final int COLOR_CRAFT_BTN_HOV  = 0xFF7CA13F;

    private RustUiHelper() {}

    public static void playClick(MinecraftClient client) {
        if (client != null && client.getSoundManager() != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SpaceRustClient.UI_CLICK_SOUND, 1.0f));
        }
    }

    public static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static void drawBorder(MatrixStack matrices, int x, int y, int w, int h, int color) {
        DrawableHelper.fill(matrices, x, y, x + w, y + 1, color);
        DrawableHelper.fill(matrices, x, y + h - 1, x + w, y + h, color);
        DrawableHelper.fill(matrices, x, y, x + 1, y + h, color);
        DrawableHelper.fill(matrices, x + w - 1, y, x + w, y + h, color);
    }

    public static void drawSprite(MatrixStack matrices, MinecraftClient client, Identifier tex,
                                  int x, int y, int w, int h, int argb) {
        if (client == null) return;
        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        float r = ((argb >>> 16) & 0xFF) / 255.0f;
        float g = ((argb >>> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(r, g, b, a);
        client.getTextureManager().bindTexture(tex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        DrawableHelper.drawTexture(matrices, x, y, w, h, 0.0f, 0.0f, 1, 1, 1, 1);
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Rust top-center Switch Button (GameUI.Navigation / Switch Button).
     * Shows "КРАФТ >" inside Inventory, "< ИНВЕНТАРЬ" inside Crafting.
     */
    public static void drawTopSwitchButton(MatrixStack matrices, MinecraftClient client,
                                           int mouseX, int mouseY, int screenW, boolean inCrafting) {
        int btnW = 128;
        int btnH = 22;
        int x = (screenW - btnW) / 2;
        int y = 4;
        boolean hov = inRect(mouseX, mouseY, x, y, btnW, btnH);

        int bg = hov ? 0x2AF7EBE1 : 0x16F7EBE1;
        DrawableHelper.fill(matrices, x, y, x + btnW, y + btnH, bg);

        String label = inCrafting ? "ИНВЕНТАРЬ" : "КРАФТ";
        RustFontRenderer bf = RustFontRenderer.bold();
        float tw = bf.getWidth(label);
        int iconS = 12;
        float total = iconS + 6 + tw;
        int sx = Math.round(x + (btnW - total) / 2f);
        int fg = hov ? 0xFFF7EBE1 : 0xB8F7EBE1;

        drawSprite(matrices, client, TEX_SWITCH_CRAFTING, sx, y + (btnH - iconS) / 2, iconS, iconS, fg);
        bf.drawCenteredY(matrices, label, sx + iconS + 6, y, btnH, fg);
    }

    public static boolean hitTestTopSwitchButton(double mouseX, double mouseY, int screenW) {
        int btnW = 128;
        int btnH = 22;
        int x = (screenW - btnW) / 2;
        int y = 4;
        return inRect(mouseX, mouseY, x, y, btnW, btnH);
    }

    /**
     * Authentic Rust Crafting Queue (CraftingQueue.Icon prefab: green time chip top, cream xN bottom-right).
     */
    public static void drawCraftingQueue(MatrixStack matrices, MinecraftClient client,
                                         int mouseX, int mouseY, int startX, int bottomY,
                                         ItemRenderer itemRenderer) {
        int qSize = 32;
        int qBoxY = bottomY - qSize;
        RustFontRenderer.boldSmall().draw(matrices, "ОЧЕРЕДЬ КРАФТА", startX, qBoxY - 12, COLOR_TEXT_DIM);

        if (RustCraftingQueue.queue.isEmpty()) {
            DrawableHelper.fill(matrices, startX, qBoxY, startX + qSize, qBoxY + qSize, 0x0BF7EBE1);
            return;
        }

        int qx = startX;
        RustFontRenderer sf = RustFontRenderer.small();

        for (int i = 0; i < RustCraftingQueue.queue.size() && i < 6; i++) {
            RustCraftingQueue.CraftTask task = RustCraftingQueue.queue.get(i);
            boolean hov = inRect(mouseX, mouseY, qx, qBoxY, qSize, qSize);

            DrawableHelper.fill(matrices, qx, qBoxY, qx + qSize, qBoxY + qSize, COLOR_SLOT_OCCUPIED);
            itemRenderer.renderInGuiWithOverrides(task.resultItem, qx + 8, qBoxY + 8);

            if (i == 0) {
                // Green time pill at top center (rgba(170,238,49,0.941))
                String timeStr = String.format(Locale.US, "%.0f", Math.ceil(task.remainingSeconds));
                int pillW = Math.max(18, Math.round(sf.getWidth(timeStr)) + 12);
                int pillX = qx + (qSize - pillW) / 2;
                int pillY = qBoxY + 1;
                DrawableHelper.fill(matrices, pillX, pillY, pillX + pillW, pillY + 9, COLOR_QUEUE_TIME);
                drawSprite(matrices, client, TEX_STOPWATCH, pillX + 2, pillY + 1, 7, 7, 0xF32B2924);
                sf.draw(matrices, timeStr, pillX + 10, pillY + 1, 0xF32B2924);
            }

            if (task.amount > 1) {
                String amt = "x" + task.amount;
                int aw = Math.round(sf.getWidth(amt)) + 4;
                DrawableHelper.fill(matrices, qx + qSize - aw, qBoxY + qSize - 9, qx + qSize, qBoxY + qSize, 0xFFF7EBE1);
                sf.draw(matrices, amt, qx + qSize - aw + 2, qBoxY + qSize - 8, 0xF32B2924);
            }

            if (hov) {
                DrawableHelper.fill(matrices, qx, qBoxY, qx + qSize, qBoxY + qSize, 0x881A1815);
                drawSprite(matrices, client, TEX_BTN_CLOSE, qx + 7, qBoxY + 7, qSize - 14, qSize - 14, COLOR_QUEUE_CANCEL);
            }
            qx += qSize + 3;
        }
    }

    public static int hitTestCraftingQueue(double mouseX, double mouseY, int startX, int bottomY) {
        int qSize = 32;
        int qBoxY = bottomY - qSize;
        int qx = startX;
        for (int i = 0; i < RustCraftingQueue.queue.size() && i < 6; i++) {
            if (inRect(mouseX, mouseY, qx, qBoxY, qSize, qSize)) {
                return i;
            }
            qx += qSize + 3;
        }
        return -1;
    }

    public static final int HOTBAR_SLOTS = 6;
    public static final int HOTBAR_SLOT_SIZE = 38;
    public static final int HOTBAR_GAP = 3;

    public static int getHotbarStartX(int screenW) {
        int totalW = HOTBAR_SLOTS * HOTBAR_SLOT_SIZE + (HOTBAR_SLOTS - 1) * HOTBAR_GAP;
        return (screenW - totalW) / 2;
    }

    /**
     * Map a 6x5 grid (30 slots) to PlayerScreenHandler slots:
     * Indices 0..26 map to slots 9..35 (main inventory)
     * Indices 27..29 map to slots 42..44 (hotbar slots 6..8)
     */
    public static int getStorageHandlerSlot(int row, int col) {
        int idx = row * 6 + col;
        if (idx < 27) {
            return 9 + idx;
        } else {
            return 42 + (idx - 27);
        }
    }

    /**
     * Rust bottom-center Belt / Hotbar (interactive inside menus or rendered in-game).
     * Returns hovered PlayerScreenHandler slot (36..41) or -1.
     */
    public static int drawHotbar(MatrixStack matrices, MinecraftClient client,
                                 int mouseX, int mouseY, int screenW, int screenH,
                                 ItemRenderer itemRenderer, boolean interactive) {
        if (client == null || client.player == null) return -1;
        PlayerInventory inv = client.player.inventory;
        if (inv.selectedSlot >= HOTBAR_SLOTS) {
            inv.selectedSlot = HOTBAR_SLOTS - 1;
        }

        int slotSize = HOTBAR_SLOT_SIZE;
        int gap = HOTBAR_GAP;
        int startX = getHotbarStartX(screenW);
        int y = screenH - slotSize - 8;
        int hoveredHandlerSlot = -1;
        RustFontRenderer sf = RustFontRenderer.small();
        RustFontRenderer bsf = RustFontRenderer.boldSmall();

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int sx = startX + i * (slotSize + gap);
            boolean isSelected = (inv.selectedSlot == i);
            boolean isHov = interactive && inRect(mouseX, mouseY, sx, y, slotSize, slotSize);
            if (isHov) hoveredHandlerSlot = 36 + i;

            ItemStack stack = inv.getStack(i);

            // Dark translucent charcoal/olive background
            int bg = isSelected ? 0xDD2D332B : (isHov ? 0xDD3B3E35 : 0xD0232522);
            DrawableHelper.fill(matrices, sx, y, sx + slotSize, y + slotSize, bg);

            // Clean border
            if (isSelected) {
                // Active slot: bright cream/white border + green top accent
                drawBorder(matrices, sx, y, slotSize, slotSize, 0xFFF7EBE1);
                DrawableHelper.fill(matrices, sx, y, sx + slotSize, y + 2, 0xFF8DC63F);
            } else {
                drawBorder(matrices, sx, y, slotSize, slotSize, isHov ? 0x55F7EBE1 : 0x22F7EBE1);
            }

            // Slot number 1..6 (subtle in corner)
            sf.draw(matrices, String.valueOf(i + 1), sx + 6, y + 2.5f, isSelected ? 0xFFF7EBE1 : 0x55F7EBE1);

            if (!stack.isEmpty()) {
                // Item icon: nicely scaled & centered in slot
                int itemOff = (slotSize - 20) / 2;
                matrices.push();
                matrices.translate(sx + itemOff, y + itemOff, 0);
                matrices.scale(1.25f, 1.25f, 1.0f);
                itemRenderer.renderInGuiWithOverrides(stack, 0, 0);
                matrices.pop();

                // Vertical durability bar on the LEFT edge of slot (1:1 with Rust)
                if (stack.isDamageable()) {
                    int maxDmg = stack.getMaxDamage();
                    int dmg = stack.getDamage();
                    float ratio = Math.max(0.0f, Math.min(1.0f, (maxDmg - dmg) / (float) maxDmg));

                    int barX = sx + 1;
                    int barY = y + 1;
                    int barW = 3;
                    int barH = slotSize - 2;

                    // Dark track behind durability
                    DrawableHelper.fill(matrices, barX, barY, barX + barW, barY + barH, 0x99181614);

                    int filledH = Math.round(barH * ratio);
                    if (filledH > 0) {
                        int durColor = (ratio > 0.5f) ? 0xFF8DC63F : ((ratio > 0.2f) ? 0xFFE5A823 : 0xFFD3362D);
                        DrawableHelper.fill(matrices, barX, barY + (barH - filledH), barX + barW, barY + barH, durColor);
                    }
                }

                // Item count: formatted as xN (or N) in bottom-right corner in bold cream font
                if (stack.getCount() > 1) {
                    String countStr = "x" + stack.getCount();
                    float tw = bsf.getWidth(countStr);
                    bsf.draw(matrices, countStr, sx + slotSize - tw - 3, y + slotSize - 11, 0xFFF7EBE1);
                }
            }
        }
        return hoveredHandlerSlot;
    }

    /**
     * CoilRust / Rust enlarged bottom-right metabolism bars using real rust_kit PNG sprites.
     */
    public static void drawMetabolismBars(MatrixStack matrices, MinecraftClient client, int screenW, int screenH) {
        if (client == null || client.player == null) return;
        int hp = Math.round(client.player.getHealth() * 5.0f);
        int food = client.player.getHungerManager().getFoodLevel() * 25;
        int water = 230;

        int barW = 148;
        int barH = 20;
        int gap = 4;
        int x = screenW - barW - 12;
        int startY = screenH - (barH * 3 + gap * 2) - 10;

        drawMetabolismRow(matrices, client, x, startY, barW, barH,
                TEX_HUD_ICON_HEALTH, TEX_HUD_HEALTH_FILL, 0xFF7CB846, hp, 100);
        drawMetabolismRow(matrices, client, x, startY + barH + gap, barW, barH,
                TEX_HUD_ICON_WATER, TEX_HUD_WATER_FILL, 0xFF3FA0D8, water, 250);
        drawMetabolismRow(matrices, client, x, startY + (barH + gap) * 2, barW, barH,
                TEX_HUD_ICON_FOOD, null, 0xFFD97828, food, 500);
    }

    private static void drawMetabolismRow(MatrixStack matrices, MinecraftClient client, int x, int y,
                                          int w, int h, Identifier icon, Identifier fillTex, int fallbackFill,
                                          int value, int max) {
        // Real CoilRust hud bar background sprite
        drawSprite(matrices, client, TEX_HUD_BAR_BG, x, y, w, h, 0xF5FFFFFF);

        float ratio = Math.max(0.0f, Math.min(1.0f, value / (float) max));
        int fillAreaX = x + 20;
        int fillAreaY = y + 2;
        int fillMaxW = w - 24;
        int fillH = h - 4;
        int fw = Math.round(fillMaxW * ratio);
        if (fw > 0) {
            if (fillTex != null) {
                drawSprite(matrices, client, fillTex, fillAreaX, fillAreaY, fw, fillH, 0xFFFFFFFF);
            } else {
                DrawableHelper.fill(matrices, fillAreaX, fillAreaY, fillAreaX + fw, fillAreaY + fillH, fallbackFill);
            }
        }

        drawSprite(matrices, client, icon, x + 4, y + (h - 12) / 2, 12, 12, 0xCCE8E2D8);
        String displayVal = (icon == TEX_HUD_ICON_HEALTH && value >= 0) ? ("+ " + value) : String.valueOf(value);
        RustFontRenderer.bold().drawCenteredY(matrices, displayVal, x + 24, y, h, 0xFFF7EBE1);
    }

    public static void drawStatusChips(MatrixStack matrices, int screenW, int screenH,
                                       int radiation, boolean isBleeding, int comfort) {
        int barW = 148;
        int barH = 20;
        int gap = 4;
        int startY = screenH - (barH * 3 + gap * 2) - 10;
        int statusY = startY - 22;
        int statusX = screenW - barW - 12;
        RustFontRenderer sf = RustFontRenderer.boldSmall();

        int chipW = 68;
        int chipH = 16;
        int cx = statusX;

        if (radiation > 0) {
            DrawableHelper.fill(matrices, cx, statusY, cx + chipW, statusY + chipH, 0xE6C73E2B);
            drawBorder(matrices, cx, statusY, chipW, chipH, 0x44FFFFFF);
            sf.drawCenteredY(matrices, "РАД " + radiation, cx + 5, statusY, chipH, 0xFFFFFFFF);
            cx += chipW + 4;
        }
        if (isBleeding) {
            DrawableHelper.fill(matrices, cx, statusY, cx + chipW, statusY + chipH, 0xE6C73E2B);
            drawBorder(matrices, cx, statusY, chipW, chipH, 0x44FFFFFF);
            sf.drawCenteredY(matrices, "КРОВОТОК", cx + 5, statusY, chipH, 0xFFFFFFFF);
            cx += chipW + 4;
        }
        if (comfort > 0 && radiation == 0 && !isBleeding) {
            DrawableHelper.fill(matrices, cx, statusY, cx + chipW, statusY + chipH, 0xCC3E822A);
            drawBorder(matrices, cx, statusY, chipW, chipH, 0x44FFFFFF);
            sf.drawCenteredY(matrices, "УЮТ " + comfort + "%", cx + 5, statusY, chipH, 0xFFFFFFFF);
        }
    }
}
