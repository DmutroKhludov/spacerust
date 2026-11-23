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
import net.spacerust.client.guns.GunManager;
import net.spacerust.client.mixin.GameRendererAccessor;
import org.lwjgl.opengl.GL11;

import java.util.Locale;

/**
 * Shared Rust / CoilRust UI primitives, world blur helper, sprites, and widgets.
 */
public final class RustUiHelper {

    public static final Identifier TEX_SWITCH_CRAFTING = new Identifier("spacerust", "textures/gui/rust_ui/icon_crafting.png");
    public static final Identifier TEX_STAT_WASH       = new Identifier("spacerust", "textures/gui/rust_ui/stat_wash.png");
    public static final Identifier TEX_SELECTED_DROP   = new Identifier("spacerust", "textures/gui/rust_ui/selected_drop.png");
    public static final Identifier TEX_STOPWATCH       = new Identifier("spacerust", "textures/gui/rust_ui/stopwatch.png");
    public static final Identifier TEX_FAV_ACTIVE      = new Identifier("spacerust", "textures/gui/rust_ui/fav_active.png");
    public static final Identifier TEX_FAV_INACTIVE    = new Identifier("spacerust", "textures/gui/rust_ui/fav_inactive.png");
    public static final Identifier TEX_BTN_ADD         = new Identifier("spacerust", "textures/gui/rust_ui/btn_add.png");
    public static final Identifier TEX_BTN_SUBTRACT    = new Identifier("spacerust", "textures/gui/rust_ui/btn_subtract.png");
    public static final Identifier TEX_BTN_CLOSE       = new Identifier("spacerust", "textures/gui/rust_ui/btn_close.png");

    // 4 Protection icons (Screenshot #2)
    public static final Identifier TEX_PROT_EXPLOSION  = new Identifier("spacerust", "textures/gui/rust_ui/prot_explosion.png");
    public static final Identifier TEX_PROT_COLD       = new Identifier("spacerust", "textures/gui/rust_ui/prot_cold.png");
    public static final Identifier TEX_PROT_RAD        = new Identifier("spacerust", "textures/gui/rust_ui/prot_rad.png");
    public static final Identifier TEX_PROT_BITE       = new Identifier("spacerust", "textures/gui/rust_ui/prot_bite.png");

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
    public static final Identifier TEX_CAT_OTHER        = new Identifier("spacerust", "textures/gui/rust_ui/cat_other.png");

    // HUD rust_kit sprites
    public static final Identifier TEX_HUD_BAR_BG      = new Identifier("spacerust", "textures/gui/rust_ui/hud_bar_bg.png");
    public static final Identifier TEX_HUD_HEALTH_FILL = new Identifier("spacerust", "textures/gui/rust_ui/hud_health_fill.png");
    public static final Identifier TEX_HUD_WATER_FILL  = new Identifier("spacerust", "textures/gui/rust_ui/hud_water_fill.png");
    public static final Identifier TEX_HUD_ICON_HEALTH = new Identifier("spacerust", "textures/gui/rust_ui/hud_icon_health.png");
    public static final Identifier TEX_HUD_ICON_WATER  = new Identifier("spacerust", "textures/gui/rust_ui/hud_icon_water.png");
    public static final Identifier TEX_HUD_ICON_FOOD   = new Identifier("spacerust", "textures/gui/rust_ui/hud_icon_food.png");
    public static final Identifier TEX_SHIELD          = new Identifier("spacerust", "textures/gui/icon_shield.png");
    public static final Identifier TEX_SEARCH          = new Identifier("spacerust", "textures/gui/icon_search.png");
    public static final Identifier TEX_ARMOR           = new Identifier("spacerust", "textures/gui/icon_armor.png");

    private static final Identifier BLUR_SHADER = new Identifier("shaders/post/blur.json");

    // Exact Rust Translucent Warm Palette (Screenshot #1 & #2)
    public static final int COLOR_BACKDROP       = 0x62121512; // Soft olive-grey tint over blurred world
    public static final int COLOR_SURFACE        = 0x16F7EBE1;
    public static final int COLOR_HEADER         = 0x20F7EBE1;
    public static final int COLOR_SLOT_EMPTY     = 0x1AF7EBE1; // Translucent warm glass square
    public static final int COLOR_SLOT_HOVER     = 0x32F7EBE1;
    public static final int COLOR_SLOT_OCCUPIED  = 0x581E1E1A; // Translucent dark charcoal when occupied
    public static final int COLOR_SELECTED_BLUE  = 0xCC1E6BA0;
    public static final int COLOR_CAT_ACTIVE     = 0xCC2C8FD4;
    public static final int COLOR_TEXT_CREAM     = 0xECF7EBE1;
    public static final int COLOR_TEXT_DIM       = 0x7AF7EBE1;
    public static final int COLOR_ROW_BG         = 0x721C1E1B;
    public static final int COLOR_GOLD_TEXT      = 0xFFEEB436;
    public static final int COLOR_SEL_PANEL_BG   = 0xE63A3D35;
    public static final int COLOR_SEL_DROP_BG    = 0xC29A9788;
    public static final int COLOR_SEL_HEADER_BG  = 0x8F888474;
    public static final int COLOR_QUEUE_TIME     = 0xF0AAEE31;
    public static final int COLOR_QUEUE_CANCEL   = 0xFFE26C16;
    public static final int COLOR_CRAFT_BTN      = 0xEB6B8B35;
    public static final int COLOR_CRAFT_BTN_HOV  = 0xFF7CA13F;

    private RustUiHelper() {}

    public static void enableWorldBlur(MinecraftClient client) {
        if (client != null && client.world != null && client.gameRenderer != null) {
            try {
                ((GameRendererAccessor) client.gameRenderer).spaceRustLoadShader(BLUR_SHADER);
            } catch (Throwable ignored) {}
        }
    }

    public static void disableWorldBlur(MinecraftClient client) {
        if (client != null && client.gameRenderer != null && client.gameRenderer.getShader() != null) {
            try {
                client.gameRenderer.disableShader();
            } catch (Throwable ignored) {}
        }
    }

    public static void playClick(MinecraftClient client) {
        // Silent in-game inventory clicks per user request
    }

    public static void playMenuClick(MinecraftClient client) {
        float uiVol = net.spacerust.client.config.RustSettings.get().uiVolume;
        if (uiVol > 0.005f && client != null && client.getSoundManager() != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SpaceRustClient.UI_CLICK_SOUND, 1.0f, 0.55f * uiVol));
        }
    }

    public static void playMenuHover(MinecraftClient client) {
        float uiVol = net.spacerust.client.config.RustSettings.get().uiVolume;
        if (uiVol > 0.005f && client != null && client.getSoundManager() != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SpaceRustClient.UI_HOVER_SOUND, 1.0f, 0.28f * uiVol));
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
        if (client == null || tex == null) return;
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
     * Exact top-center Rust Switch Button:
     * Inventory shows "КРАФТ ->" with right exit icon;
     * Crafting shows "<- ИНВЕНТАРЬ" with left exit icon (Screenshot #1 & #2).
     */
    public static void drawTopSwitchButton(MatrixStack matrices, MinecraftClient client,
                                           int mouseX, int mouseY, int screenW, boolean inCrafting) {
        int btnW = 122;
        int btnH = 22;
        int x = (screenW - btnW) / 2;
        int y = 2;
        boolean hov = inRect(mouseX, mouseY, x, y, btnW, btnH);

        DrawableHelper.fill(matrices, x, y, x + btnW, y + btnH, hov ? 0x38F7EBE1 : 0x22F7EBE1);

        String label = inCrafting ? "ИНВЕНТАРЬ" : "КРАФТ";
        RustFontRenderer bf = RustFontRenderer.bold();
        float tw = bf.getWidth(label);
        int iconS = 11;
        int fg = hov ? 0xFFF7EBE1 : 0xCCF7EBE1;

        if (inCrafting) {
            int sx = Math.round(x + (btnW - (iconS + 7 + tw)) / 2f);
            drawSprite(matrices, client, TEX_SWITCH_CRAFTING, sx, y + (btnH - iconS) / 2, iconS, iconS, fg);
            bf.drawCenteredY(matrices, label, sx + iconS + 7, y, btnH, fg);
        } else {
            int sx = Math.round(x + (btnW - (tw + 7 + iconS)) / 2f);
            bf.drawCenteredY(matrices, label, sx, y, btnH, fg);
            drawSprite(matrices, client, TEX_SWITCH_CRAFTING, Math.round(sx + tw + 7), y + (btnH - iconS) / 2, iconS, iconS, fg);
        }
    }

    public static boolean hitTestTopSwitchButton(double mouseX, double mouseY, int screenW) {
        int btnW = 122;
        int btnH = 22;
        int x = (screenW - btnW) / 2;
        int y = 2;
        return inRect(mouseX, mouseY, x, y, btnW, btnH);
    }

    /**
     * Rust Crafting Queue: matches Screenshot #1 large semi-transparent "ОЧЕРЕДЬ КРАФТА" text
     * plus live queue tiles when tasks are running.
     */
    public static void drawCraftingQueue(MatrixStack matrices, MinecraftClient client,
                                         int mouseX, int mouseY, int startX, int bottomY,
                                         ItemRenderer itemRenderer) {
        int qSize = 30;
        int qBoxY = bottomY - qSize;

        if (RustCraftingQueue.queue.isEmpty()) {
            RustFontRenderer.boldLarge().drawCenteredY(matrices, "ОЧЕРЕДЬ КРАФТА", startX + 2, qBoxY + 4, 22, 0x36F7EBE1);
            return;
        }

        RustFontRenderer.boldSmall().draw(matrices, "ОЧЕРЕДЬ КРАФТА", startX, qBoxY - 11, 0x66F7EBE1);
        int qx = startX;
        RustFontRenderer sf = RustFontRenderer.small();

        for (int i = 0; i < RustCraftingQueue.queue.size() && i < 6; i++) {
            RustCraftingQueue.CraftTask task = RustCraftingQueue.queue.get(i);
            boolean hov = inRect(mouseX, mouseY, qx, qBoxY, qSize, qSize);

            DrawableHelper.fill(matrices, qx, qBoxY, qx + qSize, qBoxY + qSize, COLOR_SLOT_OCCUPIED);
            RustItemIcons.drawRecipeIcon(matrices, client, itemRenderer, task.recipeId, task.resultItem,
                    qx + 4, qBoxY + 4, qSize - 8);

            if (i == 0) {
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
        int qSize = 30;
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
    public static final int HOTBAR_GAP = 3;

    public static int getHotbarSlotSize(int screenH) {
        return Math.max(28, Math.min(38, (int) (screenH * 0.088f)));
    }

    public static int getHotbarStartX(int screenW, int screenH) {
        int slotSize = getHotbarSlotSize(screenH);
        int totalW = HOTBAR_SLOTS * slotSize + (HOTBAR_SLOTS - 1) * HOTBAR_GAP;
        return (screenW - totalW) / 2;
    }

    public static int drawHotbar(MatrixStack matrices, MinecraftClient client,
                                 int mouseX, int mouseY, int screenW, int screenH,
                                 ItemRenderer itemRenderer, boolean interactive) {
        return drawHotbarAt(matrices, client, mouseX, mouseY,
                getHotbarStartX(screenW, screenH), screenH, getHotbarSlotSize(screenH), HOTBAR_GAP,
                itemRenderer, interactive);
    }

    public static int drawHotbarAt(MatrixStack matrices, MinecraftClient client,
                                   int mouseX, int mouseY, int startX, int screenH,
                                   int slotSize, int gap, ItemRenderer itemRenderer, boolean interactive) {
        if (client == null || client.player == null) return -1;
        PlayerInventory inv = client.player.inventory;
        int y = screenH - slotSize - 8;
        int hoveredHandlerSlot = -1;

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int sx = startX + i * (slotSize + gap);
            boolean isSelected = (inv.selectedSlot == i);
            boolean isHov = interactive && inRect(mouseX, mouseY, sx, y, slotSize, slotSize);
            if (isHov) hoveredHandlerSlot = 36 + i;

            ItemStack stack = inv.getStack(i);
            int bg = isSelected ? COLOR_SELECTED_BLUE
                    : (isHov ? COLOR_SLOT_HOVER
                    : (stack.isEmpty() ? COLOR_SLOT_EMPTY : COLOR_SLOT_OCCUPIED));
            DrawableHelper.fill(matrices, sx, y, sx + slotSize, y + slotSize, bg);

            if (!stack.isEmpty()) {
                RustItemIcons.drawSlotItem(matrices, client, itemRenderer, stack, sx, y, slotSize);
                // Show remaining magazine ammo ONLY on active belt weapon slots
                GunManager.GunInfo gun = GunManager.getGunForStack(stack);
                if (gun != null) {
                    int ammo = isSelected ? Math.min(gun.maxAmmo, Math.max(0, GunManager.currentAmmo)) : gun.maxAmmo;
                    String aStr = String.valueOf(ammo);
                    RustFontRenderer sf = slotSize >= 32 ? RustFontRenderer.boldSmall() : RustFontRenderer.small();
                    float aw = sf.getWidth(aStr);
                    sf.drawShadow(matrices, aStr, sx + slotSize - aw - 3, y + slotSize - 10, 0xFFF7EBE1);
                }
            }
        }
        return hoveredHandlerSlot;
    }

    /**
     * CoilRust / Rust bottom-right metabolism bars matching Screenshot #1 & #2.
     */
    public static void drawMetabolismBars(MatrixStack matrices, MinecraftClient client, int screenW, int screenH) {
        if (client == null || client.player == null) return;
        int hp = Math.round(client.player.getHealth() * 5.0f);
        int food = client.player.getHungerManager().getFoodLevel() * 25;
        int water = 230;

        int barW = 104;
        int barH = 14;
        int gap = 2;
        int x = screenW - barW - 10;
        int startY = screenH - (barH * 3 + gap * 2) - 8;

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
        drawSprite(matrices, client, TEX_HUD_BAR_BG, x, y, w, h, 0xF0FFFFFF);

        float ratio = Math.max(0.0f, Math.min(1.0f, value / (float) max));
        int fillAreaX = x + 15;
        int fillAreaY = y + 2;
        int fillMaxW = w - 18;
        int fillH = h - 4;
        int fw = Math.round(fillMaxW * ratio);
        if (fw > 0) {
            if (fillTex != null) {
                drawSprite(matrices, client, fillTex, fillAreaX, fillAreaY, fw, fillH, 0xFFFFFFFF);
            } else {
                DrawableHelper.fill(matrices, fillAreaX, fillAreaY, fillAreaX + fw, fillAreaY + fillH, fallbackFill);
            }
        }

        drawSprite(matrices, client, icon, x + 3, y + 3, 8, 8, 0xCCE8E2D8);
        RustFontRenderer.boldSmall().drawCenteredY(matrices, String.valueOf(value), x + 19, y, h, 0xFFF7EBE1);
    }
}
