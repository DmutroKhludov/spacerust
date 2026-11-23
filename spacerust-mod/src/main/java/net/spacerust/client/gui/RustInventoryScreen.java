package net.spacerust.client.gui;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.spacerust.client.world.RustWorldSync;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 1:1 Rust / CoilRust Inventory Screen:
 * - World blur backdrop
 * - Large 3D player + aligned 4 stat_wash protection rows + 5 wear slots
 * - Big 7x4 (4 rows tall, wide) Inventory grid covering all main slots (9..35 + 41) with 1000-stack support
 * - 5-slot Rust Belt / Hotbar (slots 36..40) at bottom center
 * - Right Quick Craft grid + CoilRust metabolism HUD bars.
 */
public class RustInventoryScreen extends Screen {

    // Armor layer: Helmet (5), Chest (6), Legs (7), Boots (8), Hands/Shield (45)
    public static final int[] ARMOR_SLOTS = {5, 6, 7, 8, 45};
    public static final String[] ARMOR_LABELS = {"ШЛЕМ", "ГРУДЬ", "НОГИ", "ОБУВЬ", "РУКИ"};

    // Clothing layer: Head (33), Torso (34), Pants (35), Gloves (42), Boots/Feet (43)
    public static final int[] CLOTHING_SLOTS = {33, 34, 35, 42, 43};
    public static final String[] CLOTHING_LABELS = {"ГОЛОВА", "ТОРС", "ШТАНЫ", "ПЕРЧАТКИ", "ОБУВЬ"};

    private static final int INV_COLS = 6;
    private static final int INV_ROWS = 4; // Exact 24 slots (6x4) excluding 6 active hotbar slots and equipment layers

    private int selectedHandlerSlot = -1;
    private int hoveredHandlerSlot = -1;
    private String hoveredEquipTip = null;

    public RustInventoryScreen() {
        super(new LiteralText("Rust Inventory"));
        RustFontRenderer.ensureLoaded();
    }

    @Override
    protected void init() {
        super.init();
        RustFontRenderer.ensureLoaded();
        RustUiHelper.enableWorldBlur(this.client);
    }

    @Override
    public void removed() {
        super.removed();
        RustUiHelper.disableWorldBlur(this.client);
        if (this.client != null && this.client.player != null && this.client.interactionManager != null) {
            ItemStack cursor = this.client.player.inventory.getCursorStack();
            if (!cursor.isEmpty()) {
                PlayerScreenHandler handler = this.client.player.playerScreenHandler;
                for (int s = 9; s <= 32; s++) {
                    if (handler.getSlot(s).getStack().isEmpty()) {
                        this.client.interactionManager.clickSlot(handler.syncId, s, 0, SlotActionType.PICKUP, this.client.player);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int gridIndexToHandlerSlot(int idx) {
        // 0..23 -> handler 9..32 (exact 24 inventory slots, excluding 5 active slots)
        return 9 + idx;
    }

    private List<RustCraftingScreen.Recipe> getQuickCraftRecipes() {
        List<RustCraftingScreen.Recipe> favs = new ArrayList<>();
        List<RustCraftingScreen.Recipe> regular = new ArrayList<>();
        for (RustCraftingScreen.Recipe r : RustCraftingScreen.ALL_RECIPES) {
            if (RustCraftingScreen.canCraftRecipe(this.client != null ? this.client.player : null, r, 1)) {
                if (r.isFavorite) {
                    favs.add(r);
                } else {
                    regular.add(r);
                }
            }
        }
        List<RustCraftingScreen.Recipe> out = new ArrayList<>(favs);
        out.addAll(regular);
        return out;
    }

    private static class Layout {
        int containerX, containerW;
        int col1X, col1W;
        int slotSize, slotGap, gridW, gridH, gridStartX, gridStartY;
        int invSectionTopY, invBottomY;
        int qcX, qcW, qcSlot, qcGap, qcCols, qcGridW, qcStartX, qcStartY, qcHeaderY, qcBottomY;
        int eqSlotSize, eqGap, eqStartX, eqY;
        int entityScale, modelCenterX, previewBottomY;
        int selX, selY, selW, selH;
        int dropX, dropY, dropW, dropH;
        int splitX, splitY, splitW, splitH;
        int closeSelX, closeSelY, closeSelSize;
    }

    private Layout computeLayout(int screenW, int screenH) {
        Layout L = new Layout();
        // Match 6x4 Inventory width, slotSize & X center pixel-for-pixel with the 6-slot active belt below!
        L.slotGap = RustUiHelper.HOTBAR_GAP;
        L.slotSize = RustUiHelper.getHotbarSlotSize(screenH);
        int hotbarTopY = screenH - L.slotSize - 8;

        L.gridW = INV_COLS * L.slotSize + (INV_COLS - 1) * L.slotGap;
        L.gridH = INV_ROWS * L.slotSize + (INV_ROWS - 1) * L.slotGap;
        L.gridStartX = (screenW - L.gridW) / 2;

        L.containerW = Math.min(screenW - 16, Math.max(580, (int) (screenW * 0.90f)));
        L.containerX = (screenW - L.containerW) / 2;

        L.col1W = Math.max(150, Math.min(220, L.gridStartX - 20));
        L.col1X = Math.max(8, L.gridStartX - L.col1W - 14);

        L.invBottomY = hotbarTopY - 10;
        L.gridStartY = L.invBottomY - L.gridH;
        L.invSectionTopY = L.gridStartY - 15;

        // Dual 5-slot Armor & Clothing rows underneath 3D character feet
        L.eqGap = 2;
        L.eqSlotSize = Math.max(20, Math.min(29, (L.col1W - 14) / 5));
        int eqTotalW = 5 * L.eqSlotSize + 4 * L.eqGap;
        L.eqStartX = L.col1X + (L.col1W - eqTotalW) / 2;
        L.eqY = L.invBottomY - L.eqSlotSize;

        // Large 3D Character centered cleanly above both equipment rows
        int armorHeaderTop = L.eqY - 13 - L.eqSlotSize - 4 - 13;
        L.previewBottomY = armorHeaderTop - 8;
        int previewAvailH = Math.max(50, L.previewBottomY - 40);
        L.entityScale = Math.max(36, Math.min(72, (int) (previewAvailH * 0.48f)));
        L.modelCenterX = L.col1X + (int) (L.col1W * 0.34f);

        // Selected Item window docked cleanly directly above ИНВЕНТАРЬ header
        L.selW = L.gridW;
        L.selX = L.gridStartX;
        L.selH = Math.max(66, Math.min(92, L.invSectionTopY - 30));
        L.selY = L.invSectionTopY - L.selH - 6;
        L.closeSelSize = 13;
        L.closeSelX = L.selX + L.selW - L.closeSelSize - 2;
        L.closeSelY = L.selY + 1;

        L.dropW = 74;
        L.dropH = 18;
        L.dropX = L.selX + L.selW - L.dropW - 6;
        L.dropY = L.selY + L.selH - L.dropH - 5;

        L.splitW = 76;
        L.splitH = 18;
        L.splitX = L.dropX - L.splitW - 5;
        L.splitY = L.dropY;

        // Right Column: Rust Quick Craft (6x3 transparent icon grid above metabolism HUD)
        L.qcX = L.gridStartX + L.gridW + 14;
        L.qcW = Math.max(120, Math.min(220, screenW - L.qcX - 10));
        L.qcGap = 4;
        L.qcCols = Math.max(4, Math.min(6, L.qcW / 26));
        L.qcSlot = Math.max(22, Math.min(30, (L.qcW - (L.qcCols - 1) * L.qcGap) / L.qcCols));
        L.qcGridW = L.qcCols * L.qcSlot + (L.qcCols - 1) * L.qcGap;
        L.qcStartX = L.qcX;
        L.qcBottomY = screenH - 58;
        int qcRowsVisible = 3;
        int qcGridH = qcRowsVisible * L.qcSlot + (qcRowsVisible - 1) * L.qcGap;
        L.qcStartY = L.qcBottomY - qcGridH;
        L.qcHeaderY = L.qcStartY - 14;

        return L;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        DrawableHelper.fill(matrices, 0, 0, this.width, this.height, RustUiHelper.COLOR_BACKDROP);
        if (this.client == null || this.client.player == null) {
            super.render(matrices, mouseX, mouseY, delta);
            return;
        }

        int screenW = this.width;
        int screenH = this.height;
        PlayerScreenHandler handler = this.client.player.playerScreenHandler;
        hoveredHandlerSlot = -1;
        Layout L = computeLayout(screenW, screenH);

        RustFontRenderer bf = RustFontRenderer.bold();
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();
        RustFontRenderer sf = RustFontRenderer.small();

        // Top Switch Button: [ КРАФТ -> ]
        RustUiHelper.drawTopSwitchButton(matrices, this.client, mouseX, mouseY, screenW, false);

        // ======================================================
        // 1. LEFT COLUMN: LARGE 3D MODEL + ALIGNED 4 STAT ROWS + DUAL ARMOR & CLOTHING LAYERS
        // ======================================================
        InventoryScreen.drawEntity(
                L.modelCenterX,
                L.previewBottomY,
                L.entityScale,
                (float) (L.modelCenterX - mouseX),
                (float) (L.previewBottomY - L.entityScale * 1.55f - mouseY),
                this.client.player
        );

        // Anchor Nickname + 4 protection stat_wash rows right beside character chest
        int statRight = L.col1X + L.col1W - 2;
        int statW = Math.min(98, (int) (L.col1W * 0.54f));
        int statX = statRight - statW;
        int modelHeadY = Math.max(32, L.previewBottomY - (int) (L.entityScale * 1.85f));

        String playerName = this.client.player.getName().getString();
        String dispName = bf.trim(playerName, statW + 14);
        bf.draw(matrices, dispName, statRight - bf.getWidth(dispName), modelHeadY, RustUiHelper.COLOR_TEXT_CREAM);

        int extraWearCount = 0;
        for (int s : CLOTHING_SLOTS) {
            if (!handler.getSlot(s).getStack().isEmpty()) extraWearCount++;
        }
        int armorPts = this.client.player.getArmor() + extraWearCount * 3;
        int expProt  = Math.min(60, armorPts * 2);
        int coldProt = Math.min(60, armorPts + 4 + extraWearCount * 2);
        int radProt  = Math.min(60, armorPts * 2 + extraWearCount * 3);
        int biteProt = Math.min(70, armorPts * 3);

        int rowY = modelHeadY + 18;
        drawProtRow(matrices, statX, rowY,      statW, 13, expProt  + " %", RustUiHelper.TEX_PROT_EXPLOSION, expProt  > 0);
        drawProtRow(matrices, statX, rowY + 15, statW, 13, coldProt + " %", RustUiHelper.TEX_PROT_COLD,      coldProt > 0);
        drawProtRow(matrices, statX, rowY + 30, statW, 13, radProt  + " %", RustUiHelper.TEX_PROT_RAD,       radProt  > 0);
        drawProtRow(matrices, statX, rowY + 45, statW, 13, biteProt + " %", RustUiHelper.TEX_PROT_BITE,      biteProt > 0);

        int clothingSlotY = L.eqY;
        int clothingHeaderY = clothingSlotY - 12;
        int armorSlotY = clothingHeaderY - L.eqSlotSize - 4;
        int armorHeaderY = armorSlotY - 12;
        int totalEqW = 5 * L.eqSlotSize + 4 * L.eqGap;
        int eqStartX = L.eqStartX;
        hoveredEquipTip = null;

        // 1. БРОНЯ (5 slots)
        DrawableHelper.fill(matrices, eqStartX, armorHeaderY, eqStartX + totalEqW, armorHeaderY + 11, RustUiHelper.COLOR_HEADER);
        sf.drawCenteredY(matrices, "БРОНЯ", eqStartX + 4, armorHeaderY, 11, RustUiHelper.COLOR_TEXT_CREAM);
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            int slotId = ARMOR_SLOTS[i];
            int sx = eqStartX + i * (L.eqSlotSize + L.eqGap);
            boolean hov = RustUiHelper.inRect(mouseX, mouseY, sx, armorSlotY, L.eqSlotSize, L.eqSlotSize);
            if (hov) {
                hoveredHandlerSlot = slotId;
                hoveredEquipTip = "Броня: " + ARMOR_LABELS[i];
            }
            ItemStack stack = handler.getSlot(slotId).getStack();
            boolean sel = (selectedHandlerSlot == slotId);
            int bg = sel ? RustUiHelper.COLOR_SELECTED_BLUE
                    : (hov ? RustUiHelper.COLOR_SLOT_HOVER
                    : (stack.isEmpty() ? RustUiHelper.COLOR_SLOT_EMPTY : RustUiHelper.COLOR_SLOT_OCCUPIED));
            DrawableHelper.fill(matrices, sx, armorSlotY, sx + L.eqSlotSize, armorSlotY + L.eqSlotSize, bg);
            RustUiHelper.drawBorder(matrices, sx, armorSlotY, L.eqSlotSize, L.eqSlotSize, sel ? 0xFFF7EBE1 : (hov ? 0x55F7EBE1 : 0x24F7EBE1));
            if (stack.isEmpty()) {
                Identifier icon = (i == 4) ? RustUiHelper.TEX_SHIELD : RustUiHelper.TEX_ARMOR;
                RustUiHelper.drawSprite(matrices, this.client, icon,
                        sx + (L.eqSlotSize - 10) / 2, armorSlotY + (L.eqSlotSize - 10) / 2, 10, 10, 0x33F7EBE1);
            } else {
                RustItemIcons.drawSlotItem(matrices, this.client, this.itemRenderer, stack, sx, armorSlotY, L.eqSlotSize);
            }
        }

        // 2. ОДЕЖДА (5 slots)
        DrawableHelper.fill(matrices, eqStartX, clothingHeaderY, eqStartX + totalEqW, clothingHeaderY + 11, RustUiHelper.COLOR_HEADER);
        sf.drawCenteredY(matrices, "ОДЕЖДА", eqStartX + 4, clothingHeaderY, 11, RustUiHelper.COLOR_TEXT_CREAM);
        for (int i = 0; i < CLOTHING_SLOTS.length; i++) {
            int slotId = CLOTHING_SLOTS[i];
            int sx = eqStartX + i * (L.eqSlotSize + L.eqGap);
            boolean hov = RustUiHelper.inRect(mouseX, mouseY, sx, clothingSlotY, L.eqSlotSize, L.eqSlotSize);
            if (hov) {
                hoveredHandlerSlot = slotId;
                hoveredEquipTip = "Одежда: " + CLOTHING_LABELS[i];
            }
            ItemStack stack = handler.getSlot(slotId).getStack();
            boolean sel = (selectedHandlerSlot == slotId);
            int bg = sel ? RustUiHelper.COLOR_SELECTED_BLUE
                    : (hov ? RustUiHelper.COLOR_SLOT_HOVER
                    : (stack.isEmpty() ? RustUiHelper.COLOR_SLOT_EMPTY : RustUiHelper.COLOR_SLOT_OCCUPIED));
            DrawableHelper.fill(matrices, sx, clothingSlotY, sx + L.eqSlotSize, clothingSlotY + L.eqSlotSize, bg);
            RustUiHelper.drawBorder(matrices, sx, clothingSlotY, L.eqSlotSize, L.eqSlotSize, sel ? 0xFFF7EBE1 : (hov ? 0x55F7EBE1 : 0x24F7EBE1));
            if (stack.isEmpty()) {
                RustUiHelper.drawSprite(matrices, this.client, RustUiHelper.TEX_ARMOR,
                        sx + (L.eqSlotSize - 10) / 2, clothingSlotY + (L.eqSlotSize - 10) / 2, 10, 10, 0x22F7EBE1);
            } else {
                RustItemIcons.drawSlotItem(matrices, this.client, this.itemRenderer, stack, sx, clothingSlotY, L.eqSlotSize);
            }
        }

        // ======================================================
        // 2. CENTER COLUMN: SELECTED ITEM + 6x4 INVENTORY (24)
        // ======================================================
        if (selectedHandlerSlot >= 0) {
            ItemStack selStack = handler.getSlot(selectedHandlerSlot).getStack();
            if (!selStack.isEmpty()) {
                drawSelectedItemWindow(matrices, L, selStack, mouseX, mouseY);
            } else {
                selectedHandlerSlot = -1;
            }
        }

        bf.draw(matrices, "ИНВЕНТАРЬ", L.gridStartX + 1, L.invSectionTopY + 1, RustUiHelper.COLOR_TEXT_CREAM);

        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                int idx = row * INV_COLS + col;
                int slotId = gridIndexToHandlerSlot(idx);
                int sx = L.gridStartX + col * (L.slotSize + L.slotGap);
                int sy = L.gridStartY + row * (L.slotSize + L.slotGap);

                boolean hov = RustUiHelper.inRect(mouseX, mouseY, sx, sy, L.slotSize, L.slotSize);
                if (hov) hoveredHandlerSlot = slotId;

                ItemStack stack = handler.getSlot(slotId).getStack();
                boolean sel = (selectedHandlerSlot == slotId);
                int bg = sel ? RustUiHelper.COLOR_SELECTED_BLUE
                        : (hov ? RustUiHelper.COLOR_SLOT_HOVER
                        : (stack.isEmpty() ? RustUiHelper.COLOR_SLOT_EMPTY : RustUiHelper.COLOR_SLOT_OCCUPIED));
                DrawableHelper.fill(matrices, sx, sy, sx + L.slotSize, sy + L.slotSize, bg);

                if (sel) {
                    RustUiHelper.drawBorder(matrices, sx, sy, L.slotSize, L.slotSize, 0xFFF7EBE1);
                } else {
                    RustUiHelper.drawBorder(matrices, sx, sy, L.slotSize, L.slotSize, hov ? 0x44F7EBE1 : 0x18F7EBE1);
                }

                if (!stack.isEmpty()) {
                    RustItemIcons.drawSlotItem(matrices, this.client, this.itemRenderer, stack, sx, sy, L.slotSize);
                    if (stack.isDamageable()) {
                        int maxDmg = stack.getMaxDamage();
                        int dmg = stack.getDamage();
                        float ratio = Math.max(0.0f, Math.min(1.0f, (maxDmg - dmg) / (float) maxDmg));
                        int barX = sx + 2;
                        int barY = sy + 2;
                        int barW = 2;
                        int barH = L.slotSize - 4;
                        DrawableHelper.fill(matrices, barX, barY, barX + barW, barY + barH, 0x99181614);
                        int filledH = Math.round(barH * ratio);
                        if (filledH > 0) {
                            int durColor = (ratio > 0.5f) ? 0xFF8DC63F : ((ratio > 0.2f) ? 0xFFE5A823 : 0xFFD3362D);
                            DrawableHelper.fill(matrices, barX, barY + (barH - filledH), barX + barW, barY + barH, durColor);
                        }
                    }
                }
            }
        }

        // ======================================================
        // 3. RIGHT COLUMN: БЫСТРЫЙ КРАФТ (Rust-style floating icons, no boxes)
        // ======================================================
        bf.draw(matrices, "БЫСТРЫЙ КРАФТ", L.qcStartX + 1, L.qcHeaderY + 1, RustUiHelper.COLOR_TEXT_CREAM);

        List<RustCraftingScreen.Recipe> quickRecipes = getQuickCraftRecipes();
        RustCraftingScreen.Recipe hoveredQuick = null;
        for (int i = 0; i < quickRecipes.size(); i++) {
            RustCraftingScreen.Recipe r = quickRecipes.get(i);
            int col = i % L.qcCols;
            int row = i / L.qcCols;
            int rx = L.qcStartX + col * (L.qcSlot + L.qcGap);
            int ry = L.qcStartY + row * (L.qcSlot + L.qcGap);
            if (ry + L.qcSlot > L.qcBottomY + 2) break;

            boolean hov = RustUiHelper.inRect(mouseX, mouseY, rx, ry, L.qcSlot, L.qcSlot);
            if (hov) {
                hoveredQuick = r;
                DrawableHelper.fill(matrices, rx, ry, rx + L.qcSlot, ry + L.qcSlot, 0x26F7EBE1);
            }

            RustItemIcons.drawRecipeIcon(matrices, this.client, this.itemRenderer, r.id, r.resultStack,
                    rx + 2, ry + 2, L.qcSlot - 4);
            if (r.isFavorite) {
                RustUiHelper.drawSprite(matrices, this.client, RustUiHelper.TEX_FAV_ACTIVE,
                        rx + L.qcSlot - 8, ry + 1, 7, 7, 0xFFFFD800);
            }
        }

        // ======================================================
        // 4. BOTTOM ROW: QUEUE, 6-SLOT BELT / HOTBAR, METABOLISM HUD
        // ======================================================
        RustUiHelper.drawCraftingQueue(matrices, this.client, mouseX, mouseY, L.col1X, screenH - 6, this.itemRenderer);
        int hotbarHover = RustUiHelper.drawHotbarAt(matrices, this.client, mouseX, mouseY,
                L.gridStartX, screenH, L.slotSize, L.slotGap, this.itemRenderer, true);
        if (hotbarHover >= 0) hoveredHandlerSlot = hotbarHover;
        RustUiHelper.drawMetabolismBars(matrices, this.client, screenW, screenH);

        // Cursor Stack under mouse or Tooltip
        ItemStack cursor = this.client.player.inventory.getCursorStack();
        if (!cursor.isEmpty()) {
            Identifier curTex = RustItemIcons.resolveStackIcon(cursor);
            if (curTex != null) {
                RustItemIcons.drawItemPng(matrices, this.client, curTex, mouseX - 11, mouseY - 11, 22);
            } else {
                this.itemRenderer.renderInGuiWithOverrides(cursor, mouseX - 8, mouseY - 8);
            }
            int curCount = RustStackUtil.getCount(cursor);
            if (curCount > 1) {
                sf.drawShadow(matrices, "x" + curCount, mouseX + 3, mouseY + 3, 0xFFF7EBE1);
            }
        } else if (hoveredQuick != null) {
            String tip = hoveredQuick.name + (RustCraftingScreen.canCraftRecipe(this.client.player, hoveredQuick, 1) ? " (ЛКМ - создать)" : " (мало ресурсов)");
            int tw = Math.round(sf.getWidth(tip)) + 10;
            int tx = Math.min(screenW - tw - 6, mouseX + 8);
            int ty = Math.max(6, mouseY - 16);
            DrawableHelper.fill(matrices, tx, ty, tx + tw, ty + 14, RustUiHelper.COLOR_SLOT_OCCUPIED);
            sf.drawCenteredY(matrices, tip, tx + 5, ty, 14, RustUiHelper.COLOR_TEXT_CREAM);
        } else if (hoveredHandlerSlot >= 0) {
            ItemStack hs = handler.getSlot(hoveredHandlerSlot).getStack();
            if (!hs.isEmpty()) {
                this.renderTooltip(matrices, hs, mouseX, mouseY);
            } else if (hoveredEquipTip != null) {
                int tw = Math.round(sf.getWidth(hoveredEquipTip)) + 12;
                int tx = Math.min(screenW - tw - 6, mouseX + 8);
                int ty = Math.max(6, mouseY - 16);
                DrawableHelper.fill(matrices, tx, ty, tx + tw, ty + 14, RustUiHelper.COLOR_SLOT_OCCUPIED);
                sf.drawCenteredY(matrices, hoveredEquipTip, tx + 6, ty, 14, RustUiHelper.COLOR_TEXT_CREAM);
            }
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawProtRow(MatrixStack matrices, int x, int y, int w, int h, String val, Identifier icon, boolean active) {
        RustUiHelper.drawSprite(matrices, this.client, RustUiHelper.TEX_STAT_WASH, x, y, w, h,
                active ? 0xD8141412 : 0x55141412);
        RustUiHelper.drawSprite(matrices, this.client, icon, x + w - 12, y + 2, 9, 9,
                active ? 0xCCF7EBE1 : 0x66F7EBE1);
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();
        float vw = bfSmall.getWidth(val);
        bfSmall.drawCenteredY(matrices, val, x + w - 16 - vw, y, h,
                active ? 0xCCF7EBE1 : 0x66F7EBE1);
    }

    private void drawSelectedItemWindow(MatrixStack matrices, Layout L, ItemStack stack, int mouseX, int mouseY) {
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();
        RustFontRenderer rf = RustFontRenderer.regular();
        RustFontRenderer sf = RustFontRenderer.small();

        DrawableHelper.fill(matrices, L.selX, L.selY, L.selX + L.selW, L.selY + L.selH, RustUiHelper.COLOR_SEL_PANEL_BG);
        DrawableHelper.fill(matrices, L.selX, L.selY, L.selX + L.selW, L.selY + 15, RustUiHelper.COLOR_SEL_HEADER_BG);

        String title = bfSmall.trim(stack.getName().getString().toUpperCase(), L.selW - 26);
        bfSmall.drawCenteredY(matrices, title, L.selX + 6, L.selY, 15, 0xFFF5F5F5);

        boolean closeHov = RustUiHelper.inRect(mouseX, mouseY, L.closeSelX, L.closeSelY, L.closeSelSize, L.closeSelSize);
        RustUiHelper.drawSprite(matrices, this.client, RustUiHelper.TEX_BTN_CLOSE,
                L.closeSelX + 2, L.closeSelY + 2, 9, 9, closeHov ? 0xFFFFFFFF : 0xBBF7EBE1);

        int iconBoxX = L.selX + 6;
        int iconBoxY = L.selY + 18;
        DrawableHelper.fill(matrices, iconBoxX, iconBoxY, iconBoxX + 26, iconBoxY + 26, 0x44000000);
        RustItemIcons.drawSlotItem(matrices, this.client, this.itemRenderer, stack, iconBoxX, iconBoxY, 26);

        int realCount = RustStackUtil.getCount(stack);
        int maxStack = RustStackUtil.isCustomStacked(stack) ? 1000 : stack.getMaxCount();
        rf.draw(matrices, "Количество: " + realCount + " / " + maxStack, iconBoxX + 32, iconBoxY + 2, RustUiHelper.COLOR_TEXT_CREAM);
        if (stack.isDamageable()) {
            int cur = stack.getMaxDamage() - stack.getDamage();
            sf.draw(matrices, "Прочность: " + cur + " / " + stack.getMaxDamage(), iconBoxX + 32, iconBoxY + 14, 0xFFB0C4A8);
        } else {
            sf.draw(matrices, "ЛКМ: взять | 1-6: на панель слотов", iconBoxX + 32, iconBoxY + 14, RustUiHelper.COLOR_TEXT_DIM);
        }

        if (realCount > 1) {
            boolean splitHov = RustUiHelper.inRect(mouseX, mouseY, L.splitX, L.splitY, L.splitW, L.splitH);
            DrawableHelper.fill(matrices, L.splitX, L.splitY, L.splitX + L.splitW, L.splitY + L.splitH,
                    splitHov ? 0xE0B0AC9A : RustUiHelper.COLOR_SEL_DROP_BG);
            String spLabel = "РАЗДЕЛИТЬ 1/2";
            sf.drawCenteredY(matrices, spLabel, L.splitX + (L.splitW - sf.getWidth(spLabel)) / 2f, L.splitY, L.splitH, 0xFFFFFFFF);
        }

        boolean dropHov = RustUiHelper.inRect(mouseX, mouseY, L.dropX, L.dropY, L.dropW, L.dropH);
        DrawableHelper.fill(matrices, L.dropX, L.dropY, L.dropX + L.dropW, L.dropY + L.dropH,
                dropHov ? 0xE0B0AC9A : RustUiHelper.COLOR_SEL_DROP_BG);
        RustUiHelper.drawSprite(matrices, this.client, RustUiHelper.TEX_SELECTED_DROP, L.dropX + 5, L.dropY + 4, 10, 10, 0xFFFFFFFF);
        bfSmall.drawCenteredY(matrices, "ВЫБРОСИТЬ", L.dropX + 18, L.dropY, L.dropH, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.client == null || this.client.player == null || this.client.interactionManager == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int screenW = this.width;
        int screenH = this.height;
        PlayerScreenHandler handler = this.client.player.playerScreenHandler;
        Layout L = computeLayout(screenW, screenH);

        if (button == 0 && RustUiHelper.hitTestTopSwitchButton(mouseX, mouseY, screenW)) {
            RustUiHelper.playClick(this.client);
            this.client.openScreen(new RustCraftingScreen());
            return true;
        }

        int qIdx = RustUiHelper.hitTestCraftingQueue(mouseX, mouseY, L.col1X, screenH - 6);
        if (qIdx >= 0 && qIdx < RustCraftingQueue.queue.size()) {
            RustCraftingQueue.queue.remove(qIdx);
            RustUiHelper.playClick(this.client);
            return true;
        }

        if (button == 0 && selectedHandlerSlot >= 0 && !handler.getSlot(selectedHandlerSlot).getStack().isEmpty()) {
            if (RustUiHelper.inRect(mouseX, mouseY, L.closeSelX, L.closeSelY, L.closeSelSize, L.closeSelSize)) {
                selectedHandlerSlot = -1;
                RustUiHelper.playClick(this.client);
                return true;
            }
            if (RustUiHelper.inRect(mouseX, mouseY, L.dropX, L.dropY, L.dropW, L.dropH)) {
                this.client.interactionManager.clickSlot(handler.syncId, selectedHandlerSlot, 1, SlotActionType.THROW, this.client.player);
                selectedHandlerSlot = -1;
                RustUiHelper.playClick(this.client);
                return true;
            }
            ItemStack selStack = handler.getSlot(selectedHandlerSlot).getStack();
            int realCount = RustStackUtil.getCount(selStack);
            if (realCount > 1 && RustUiHelper.inRect(mouseX, mouseY, L.splitX, L.splitY, L.splitW, L.splitH)) {
                if (RustStackUtil.isCustomStacked(selStack) && !this.client.isInSingleplayer()) {
                    RustWorldSync.sendSplitStack(selectedHandlerSlot);
                } else {
                    this.client.interactionManager.clickSlot(handler.syncId, selectedHandlerSlot, 1, SlotActionType.PICKUP, this.client.player);
                    for (int s = 9; s <= 32; s++) {
                        if (handler.getSlot(s).getStack().isEmpty()) {
                            this.client.interactionManager.clickSlot(handler.syncId, s, 0, SlotActionType.PICKUP, this.client.player);
                            break;
                        }
                    }
                }
                RustUiHelper.playClick(this.client);
                return true;
            }
        }

        // 1. Armor & Clothing slot clicks
        int clothingSlotY = L.eqY;
        int clothingHeaderY = clothingSlotY - 12;
        int armorSlotY = clothingHeaderY - L.eqSlotSize - 4;

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            int sx = L.eqStartX + i * (L.eqSlotSize + L.eqGap);
            if (RustUiHelper.inRect(mouseX, mouseY, sx, armorSlotY, L.eqSlotSize, L.eqSlotSize)) {
                handleSlotClick(handler, ARMOR_SLOTS[i], button);
                return true;
            }
        }

        for (int i = 0; i < CLOTHING_SLOTS.length; i++) {
            int sx = L.eqStartX + i * (L.eqSlotSize + L.eqGap);
            if (RustUiHelper.inRect(mouseX, mouseY, sx, clothingSlotY, L.eqSlotSize, L.eqSlotSize)) {
                handleSlotClick(handler, CLOTHING_SLOTS[i], button);
                return true;
            }
        }

        // Main 6 x 4 Inventory grid clicks (24 slots: 9..32)
        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                int idx = row * INV_COLS + col;
                int slotId = gridIndexToHandlerSlot(idx);
                int sx = L.gridStartX + col * (L.slotSize + L.slotGap);
                int sy = L.gridStartY + row * (L.slotSize + L.slotGap);
                if (RustUiHelper.inRect(mouseX, mouseY, sx, sy, L.slotSize, L.slotSize)) {
                    handleSlotClick(handler, slotId, button);
                    return true;
                }
            }
        }

        // 6-slot Hotbar clicks (slots 36..41) aligned directly under Inventory columns
        int hotbarY = screenH - L.slotSize - 8;
        for (int i = 0; i < RustUiHelper.HOTBAR_SLOTS; i++) {
            int sx = L.gridStartX + i * (L.slotSize + L.slotGap);
            if (RustUiHelper.inRect(mouseX, mouseY, sx, hotbarY, L.slotSize, L.slotSize)) {
                handleSlotClick(handler, 36 + i, button);
                return true;
            }
        }

        // Quick Craft clicks
        if (button == 0) {
            List<RustCraftingScreen.Recipe> quickRecipes = getQuickCraftRecipes();
            for (int i = 0; i < quickRecipes.size(); i++) {
                RustCraftingScreen.Recipe r = quickRecipes.get(i);
                int col = i % L.qcCols;
                int row = i / L.qcCols;
                int rx = L.qcStartX + col * (L.qcSlot + L.qcGap);
                int ry = L.qcStartY + row * (L.qcSlot + L.qcGap);
                if (ry + L.qcSlot > L.qcBottomY + 2) break;

                if (RustUiHelper.inRect(mouseX, mouseY, rx, ry, L.qcSlot, L.qcSlot)) {
                    if (RustCraftingScreen.canCraftRecipe(this.client.player, r, 1)) {
                        if (this.client.isInSingleplayer()) {
                            for (RustCraftingScreen.Ingredient ing : r.ingredients) {
                                RustCraftingScreen.deductPlayerResource(this.client.player, ing.mcItem, ing.amount);
                            }
                        }
                        ItemStack res = r.resultStack.copy();
                        res.setCount(1);
                        RustCraftingQueue.addTask(r.id, r.name, res, 1, r.craftSeconds);
                        RustUiHelper.playClick(this.client);
                    }
                    return true;
                }
            }
        }

        // Clicking empty space while holding a cursor stack throws the item into the world
        if (!this.client.player.inventory.getCursorStack().isEmpty()) {
            this.client.interactionManager.clickSlot(handler.syncId, -999, button == 1 ? 1 : 0, SlotActionType.PICKUP, this.client.player);
            RustUiHelper.playClick(this.client);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.client != null && this.client.player != null && this.client.interactionManager != null) {
            ItemStack cursor = this.client.player.inventory.getCursorStack();
            if (!cursor.isEmpty()) {
                Layout L = computeLayout(this.width, this.height);
                if (!isOverAnyInteractiveArea(mouseX, mouseY, L)) {
                    this.client.interactionManager.clickSlot(
                            this.client.player.playerScreenHandler.syncId,
                            -999,
                            button == 1 ? 1 : 0,
                            SlotActionType.PICKUP,
                            this.client.player
                    );
                    RustUiHelper.playClick(this.client);
                    return true;
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverAnyInteractiveArea(double mx, double my, Layout L) {
        if (RustUiHelper.hitTestTopSwitchButton(mx, my, this.width)) return true;
        int eqTop = L.eqY - L.eqSlotSize - 16;
        int eqW = 5 * (L.eqSlotSize + L.eqGap);
        if (RustUiHelper.inRect(mx, my, L.eqStartX, eqTop, eqW, L.eqY + L.eqSlotSize - eqTop)) return true;
        if (RustUiHelper.inRect(mx, my, L.gridStartX, L.gridStartY, L.gridW, L.gridH)) return true;
        int hotbarY = this.height - L.slotSize - 8;
        if (RustUiHelper.inRect(mx, my, L.gridStartX, hotbarY, L.gridW, L.slotSize)) return true;
        if (selectedHandlerSlot >= 0 && RustUiHelper.inRect(mx, my, L.selX, L.selY, L.selW, L.selH)) return true;
        if (!getQuickCraftRecipes().isEmpty()
                && RustUiHelper.inRect(mx, my, L.qcStartX, L.qcStartY, L.qcGridW, Math.max(0, L.qcBottomY - L.qcStartY))) {
            return true;
        }
        return false;
    }

    private void handleSlotClick(PlayerScreenHandler handler, int slotId, int button) {
        selectedHandlerSlot = slotId;
        ItemStack clicked = handler.getSlot(slotId).getStack();
        if (button == 1 && RustStackUtil.isCustomStacked(clicked) && RustStackUtil.getCount(clicked) > 1
                && this.client != null && !this.client.isInSingleplayer()
                && this.client.player.inventory.getCursorStack().isEmpty()) {
            RustWorldSync.sendSplitStack(slotId);
            RustUiHelper.playClick(this.client);
            return;
        }
        SlotActionType action = Screen.hasShiftDown() ? SlotActionType.QUICK_MOVE : SlotActionType.PICKUP;
        int clickData = (button == 1) ? 1 : 0;
        this.client.interactionManager.clickSlot(handler.syncId, slotId, clickData, action, this.client.player);
        RustUiHelper.playClick(this.client);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.client != null && this.client.player != null && this.client.interactionManager != null) {
            if (hoveredHandlerSlot >= 0 && keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_6) {
                int hotbarIdx = keyCode - GLFW.GLFW_KEY_1;
                this.client.interactionManager.clickSlot(
                        this.client.player.playerScreenHandler.syncId,
                        hoveredHandlerSlot,
                        hotbarIdx,
                        SlotActionType.SWAP,
                        this.client.player
                );
                RustUiHelper.playClick(this.client);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_Q) {
                this.client.openScreen(new RustCraftingScreen());
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_E || keyCode == GLFW.GLFW_KEY_ESCAPE
                    || this.client.options.keyInventory.matchesKey(keyCode, scanCode)) {
                this.onClose();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
