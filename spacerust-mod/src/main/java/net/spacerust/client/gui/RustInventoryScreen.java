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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 1:1 Rust / CoilRust style Inventory screen following inventory_source.json
 * and description-window-layout.json:
 * - Warm cream-glass translucent slots (247,235,225) & dark olive occupied cells (43,41,36)
 * - Left: Player nickname, protection stat_wash row, 3D model, 5 wear/offhand slots
 * - Center: Selected Item window (Drop + Split 1/2) above 9x3 Inventory grid
 * - Right: Quick Craft bottom-aligned panel
 * - Bottom: Crafting Queue, 9-slot Belt/Hotbar, CoilRust metabolism HUD bars.
 */
public class RustInventoryScreen extends Screen {

    // Armor layer: Helmet (5), Chest (6), Legs (7), Boots (8), Hands/Shield (45)
    public static final int[] ARMOR_SLOTS = {5, 6, 7, 8, 45};
    public static final String[] ARMOR_LABELS = {"ШЛЕМ", "ГРУДЬ", "НОГИ", "ОБУВЬ", "РУКИ"};

    // Clothing layer: Head (33), Torso (34), Pants (35), Gloves (42), Boots/Feet (43)
    public static final int[] CLOTHING_SLOTS = {33, 34, 35, 42, 43};
    public static final String[] CLOTHING_LABELS = {"ГОЛОВА", "ТОРС", "ШТАНЫ", "ПЕРЧАТКИ", "ОБУВЬ"};

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
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        if (this.client != null && this.client.player != null && this.client.interactionManager != null) {
            ItemStack cursor = this.client.player.inventory.getCursorStack();
            if (!cursor.isEmpty()) {
                PlayerScreenHandler handler = this.client.player.playerScreenHandler;
                for (int s = 9; s <= 44; s++) {
                    if (handler.getSlot(s).getStack().isEmpty()) {
                        this.client.interactionManager.clickSlot(handler.syncId, s, 0, SlotActionType.PICKUP, this.client.player);
                        break;
                    }
                }
            }
        }
    }

    private List<RustCraftingScreen.Recipe> getQuickCraftRecipes() {
        List<RustCraftingScreen.Recipe> craftable = new ArrayList<>();
        List<RustCraftingScreen.Recipe> others = new ArrayList<>();
        for (RustCraftingScreen.Recipe r : RustCraftingScreen.ALL_RECIPES) {
            if (RustCraftingScreen.canCraftRecipe(this.client != null ? this.client.player : null, r, 1)) {
                craftable.add(r);
            } else {
                others.add(r);
            }
        }
        List<RustCraftingScreen.Recipe> out = new ArrayList<>(craftable);
        out.addAll(others);
        return out;
    }

    // Shared layout metrics so render() and mouseClicked() always align identically
    private static class Layout {
        int containerX, containerW;
        int col1X, col1W;
        int slotSize, slotGap, gridW, gridStartX, gridStartY;
        int invSectionTopY, invBottomY;
        int qcX, qcW, qcSlot, qcGap, qcCols, qcGridW, qcStartX, qcStartY, qcHeaderY, qcBottomY;
        int eqSlotSize, eqGap, eqStartX, eqY;
        int selX, selY, selW, selH;
        int dropX, dropY, dropW, dropH;
        int splitX, splitY, splitW, splitH;
        int closeSelX, closeSelY, closeSelSize;
    }

    private Layout computeLayout(int screenW, int screenH) {
        Layout L = new Layout();
        L.slotGap = 4;
        L.slotSize = 48; // 48px slot size for 24-slot inventory (matches the 48px hotbar!)
        int availGridH = screenH - RustUiHelper.HOTBAR_SLOT_SIZE - 20 - 70;
        if ((availGridH - 3 * L.slotGap) / 4 < L.slotSize) {
            L.slotSize = Math.max(36, (availGridH - 3 * L.slotGap) / 4);
        }
        L.gridW = 6 * L.slotSize + 5 * L.slotGap;
        // Center the 24-slot main grid directly aligned with the 6-slot hotbar
        L.gridStartX = (screenW - L.gridW) / 2;

        // Anchor bottom of Inventory cleanly above the 6-slot Belt/Hotbar
        L.invBottomY = screenH - RustUiHelper.HOTBAR_SLOT_SIZE - 20;
        int gridTotalH = 4 * L.slotSize + 3 * L.slotGap;
        L.gridStartY = L.invBottomY - gridTotalH;
        L.invSectionTopY = L.gridStartY - 18;

        // Column 1 (Left): Player Name, Stats, 3D Model, Armor & Clothing
        int spacing = 16;
        L.col1W = Math.max(160, Math.min(220, L.gridStartX - spacing - 10));
        L.col1X = Math.max(10, L.gridStartX - spacing - L.col1W);

        // Armor & Clothing 5 slots aligned in Column 1
        L.eqSlotSize = Math.max(26, Math.min(36, (L.col1W - 16) / 5));
        L.eqGap = 2;
        int eqTotalW = 5 * L.eqSlotSize + 4 * L.eqGap;
        L.eqStartX = L.col1X + (L.col1W - eqTotalW) / 2;
        L.eqY = L.invBottomY - L.eqSlotSize;

        // Selected Item window directly above ИНВЕНТАРЬ header
        L.selW = L.gridW;
        L.selX = L.gridStartX;
        L.selH = Math.min(84, Math.max(64, L.invSectionTopY - 34));
        L.selY = L.invSectionTopY - L.selH - 6;
        L.closeSelSize = 13;
        L.closeSelX = L.selX + L.selW - L.closeSelSize - 2;
        L.closeSelY = L.selY + 1;

        L.dropW = 68;
        L.dropH = 18;
        L.dropX = L.selX + L.selW - L.dropW - 6;
        L.dropY = L.selY + L.selH - L.dropH - 6;

        L.splitW = 68;
        L.splitH = 18;
        L.splitX = L.dropX - L.splitW - 5;
        L.splitY = L.dropY;

        // Right Column: Quick Craft
        L.qcX = L.gridStartX + L.gridW + spacing;
        L.qcW = Math.max(100, Math.min(220, screenW - 10 - L.qcX));
        L.qcSlot = Math.max(28, Math.min(36, L.slotSize - 8));
        L.qcGap = 2;
        L.qcCols = Math.max(3, Math.min(5, (L.qcW + L.qcGap) / (L.qcSlot + L.qcGap)));
        L.qcGridW = L.qcCols * L.qcSlot + (L.qcCols - 1) * L.qcGap;
        L.qcStartX = L.qcX;
        L.qcBottomY = L.invBottomY;
        int qcRowsVisible = Math.max(3, Math.min(6, (L.qcBottomY - 46) / (L.qcSlot + L.qcGap)));
        int qcGridH = qcRowsVisible * L.qcSlot + (qcRowsVisible - 1) * L.qcGap;
        L.qcStartY = L.qcBottomY - qcGridH;
        L.qcHeaderY = L.qcStartY - 16;

        return L;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // Subtle desaturated Rust backdrop overlay
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
        RustFontRenderer rf = RustFontRenderer.regular();
        RustFontRenderer sf = RustFontRenderer.small();

        // Top Switch Button: [ КРАФТ ]
        RustUiHelper.drawTopSwitchButton(matrices, this.client, mouseX, mouseY, screenW, false);

        // ======================================================
        // 1. LEFT COLUMN: PLAYER NAME, STAT_WASH, 3D MODEL, ARMOR & CLOTHING
        // ======================================================
        String playerName = this.client.player.getName().getString().toUpperCase();
        float nameW = bf.getWidth(playerName);
        bf.draw(matrices, playerName, L.col1X + (L.col1W - nameW) / 2f, 34, RustUiHelper.COLOR_TEXT_CREAM);

        // Armor Protection stat_wash row
        int armorPts = this.client.player.getArmor();
        int statY = 50;
        int statW = 54;
        int statH = 14;
        int statX = L.col1X + 4;
        RustUiHelper.drawSprite(matrices, this.client, RustUiHelper.TEX_STAT_WASH, statX, statY, statW, statH,
                armorPts > 0 ? 0xCCF7EBE1 : 0x26F7EBE1);
        RustUiHelper.drawSprite(matrices, this.client, RustUiHelper.TEX_ARMOR, statX + 3, statY + 3, 8, 8,
                armorPts > 0 ? 0xFFF7EBE1 : 0x44F7EBE1);
        bfSmall.drawCenteredY(matrices, armorPts > 0 ? (armorPts * 5 + "%") : "0",
                statX + 14, statY, statH, armorPts > 0 ? RustUiHelper.COLOR_TEXT_CREAM : 0x55F7EBE1);

        // Layer 2: ОДЕЖДА (Clothing: 5 slots)
        int clothingSlotY = L.eqY;
        int clothingHeaderY = clothingSlotY - 14;

        // Layer 1: БРОНЯ (Armor: 5 slots)
        int armorSlotY = clothingHeaderY - L.eqSlotSize - 4;
        int armorHeaderY = armorSlotY - 14;

        // 3D Player Entity Preview (positioned above the armor layer)
        int previewCenterX = L.col1X + L.col1W / 2;
        int previewBottomY = armorHeaderY - 8;
        int previewAvailH = Math.max(50, previewBottomY - 50);
        int entityScale = Math.max(22, Math.min(40, (int) (previewAvailH * 0.42f)));
        InventoryScreen.drawEntity(
                previewCenterX,
                previewBottomY,
                entityScale,
                (float) (previewCenterX - mouseX),
                (float) (previewBottomY - entityScale * 1.6f - mouseY),
                this.client.player
        );

        int totalEqW = 5 * L.eqSlotSize + 4 * L.eqGap;
        int eqStartX = L.col1X + (L.col1W - totalEqW) / 2;
        hoveredEquipTip = null;

        // 1. БРОНЯ
        DrawableHelper.fill(matrices, eqStartX, armorHeaderY, eqStartX + totalEqW, armorHeaderY + 12, RustUiHelper.COLOR_HEADER);
        bfSmall.drawCenteredY(matrices, "БРОНЯ", eqStartX + 4, armorHeaderY, 12, RustUiHelper.COLOR_TEXT_CREAM);

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
                        sx + (L.eqSlotSize - 12) / 2, armorSlotY + (L.eqSlotSize - 12) / 2, 12, 12, 0x33F7EBE1);
                sf.drawCenteredY(matrices, ARMOR_LABELS[i].substring(0, 1), sx + 3, armorSlotY + 1, 8, 0x44F7EBE1);
            } else {
                int itemOff = (L.eqSlotSize - 16) / 2;
                this.itemRenderer.renderInGuiWithOverrides(stack, sx + itemOff, armorSlotY + itemOff);
                this.itemRenderer.renderGuiItemOverlay(this.client.textRenderer, stack, sx + itemOff, armorSlotY + itemOff);
            }
        }

        // 2. ОДЕЖДА
        DrawableHelper.fill(matrices, eqStartX, clothingHeaderY, eqStartX + totalEqW, clothingHeaderY + 12, RustUiHelper.COLOR_HEADER);
        bfSmall.drawCenteredY(matrices, "ОДЕЖДА", eqStartX + 4, clothingHeaderY, 12, RustUiHelper.COLOR_TEXT_CREAM);

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
                        sx + (L.eqSlotSize - 12) / 2, clothingSlotY + (L.eqSlotSize - 12) / 2, 12, 12, 0x22F7EBE1);
                sf.drawCenteredY(matrices, CLOTHING_LABELS[i].substring(0, 1), sx + 3, clothingSlotY + 1, 8, 0x33F7EBE1);
            } else {
                int itemOff = (L.eqSlotSize - 16) / 2;
                this.itemRenderer.renderInGuiWithOverrides(stack, sx + itemOff, clothingSlotY + itemOff);
                this.itemRenderer.renderGuiItemOverlay(this.client.textRenderer, stack, sx + itemOff, clothingSlotY + itemOff);
            }
        }

        // ======================================================
        // 2. CENTER COLUMN: SELECTED ITEM PANEL + 24 INVENTORY
        // ======================================================
        if (selectedHandlerSlot >= 0) {
            ItemStack selStack = handler.getSlot(selectedHandlerSlot).getStack();
            if (!selStack.isEmpty()) {
                drawSelectedItemWindow(matrices, L, selStack, mouseX, mouseY);
            } else {
                selectedHandlerSlot = -1;
            }
        }

        // Header bar "ИНВЕНТАРЬ (24)"
        DrawableHelper.fill(matrices, L.gridStartX, L.invSectionTopY, L.gridStartX + L.gridW, L.invSectionTopY + 14, RustUiHelper.COLOR_HEADER);
        bfSmall.drawCenteredY(matrices, "ИНВЕНТАРЬ (24)", L.gridStartX + 6, L.invSectionTopY, 14, RustUiHelper.COLOR_TEXT_CREAM);

        // 6 x 4 Inventory grid (24 slots: 9..32)
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 6; col++) {
                int slotId = 9 + row * 6 + col;
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
                    int itemOff = (L.slotSize - 16) / 2;
                    this.itemRenderer.renderInGuiWithOverrides(stack, sx + itemOff, sy + itemOff);
                    this.itemRenderer.renderGuiItemOverlay(this.client.textRenderer, stack, sx + itemOff, sy + itemOff);

                    // Vertical durability bar on left edge
                    if (stack.isDamageable()) {
                        int maxDmg = stack.getMaxDamage();
                        int dmg = stack.getDamage();
                        float ratio = Math.max(0.0f, Math.min(1.0f, (maxDmg - dmg) / (float) maxDmg));
                        int barX = sx + 2;
                        int barY = sy + 2;
                        int barW = 3;
                        int barH = L.slotSize - 4;
                        DrawableHelper.fill(matrices, barX, barY, barX + barW, barY + barH, 0x99181614);
                        int filledH = Math.round(barH * ratio);
                        if (filledH > 0) {
                            int durColor = (ratio > 0.5f) ? 0xFF8DC63F : ((ratio > 0.2f) ? 0xFFE5A823 : 0xFFD3362D);
                            DrawableHelper.fill(matrices, barX, barY + (barH - filledH), barX + barW, barY + barH, durColor);
                        }
                    }

                    if (stack.getCount() > 1) {
                        String countStr = "x" + stack.getCount();
                        float tw = bfSmall.getWidth(countStr);
                        bfSmall.draw(matrices, countStr, sx + L.slotSize - tw - 3, sy + L.slotSize - 11, 0xFFF7EBE1);
                    }
                }
            }
        }

        // ======================================================
        // 3. RIGHT COLUMN: БЫСТРЫЙ КРАФТ
        // ======================================================
        DrawableHelper.fill(matrices, L.qcStartX, L.qcHeaderY, L.qcStartX + L.qcGridW, L.qcHeaderY + 13, RustUiHelper.COLOR_HEADER);
        bfSmall.drawCenteredY(matrices, "БЫСТРЫЙ КРАФТ", L.qcStartX + 5, L.qcHeaderY, 13, RustUiHelper.COLOR_TEXT_CREAM);

        List<RustCraftingScreen.Recipe> quickRecipes = getQuickCraftRecipes();
        RustCraftingScreen.Recipe hoveredQuick = null;
        for (int i = 0; i < quickRecipes.size(); i++) {
            RustCraftingScreen.Recipe r = quickRecipes.get(i);
            int col = i % L.qcCols;
            int row = i / L.qcCols;
            int rx = L.qcStartX + col * (L.qcSlot + L.qcGap);
            int ry = L.qcStartY + row * (L.qcSlot + L.qcGap);
            if (ry + L.qcSlot > L.qcBottomY + 2) break;

            boolean craftable = RustCraftingScreen.canCraftRecipe(this.client.player, r, 1);
            boolean hov = RustUiHelper.inRect(mouseX, mouseY, rx, ry, L.qcSlot, L.qcSlot);
            if (hov) hoveredQuick = r;

            int bg = craftable ? (hov ? 0xEB4A5B2C : 0xE8323B22) : (hov ? RustUiHelper.COLOR_SLOT_HOVER : RustUiHelper.COLOR_SLOT_EMPTY);
            DrawableHelper.fill(matrices, rx, ry, rx + L.qcSlot, ry + L.qcSlot, bg);

            int itemOff = (L.qcSlot - 16) / 2;
            this.itemRenderer.renderInGuiWithOverrides(r.resultStack, rx + itemOff, ry + itemOff);
            if (!craftable) {
                DrawableHelper.fill(matrices, rx, ry, rx + L.qcSlot, ry + L.qcSlot, 0x55181614);
            }
        }

        // ======================================================
        // 4. BOTTOM ROW: QUEUE, HOTBAR (BELT), METABOLISM BARS
        // ======================================================
        RustUiHelper.drawCraftingQueue(matrices, this.client, mouseX, mouseY, L.col1X, screenH - 6, this.itemRenderer);
        int hotbarHover = RustUiHelper.drawHotbar(matrices, this.client, mouseX, mouseY, screenW, screenH, this.itemRenderer, true);
        if (hotbarHover >= 0) hoveredHandlerSlot = hotbarHover;
        RustUiHelper.drawMetabolismBars(matrices, this.client, screenW, screenH);

        // Cursor Stack under mouse or Tooltip
        ItemStack cursor = this.client.player.inventory.getCursorStack();
        if (!cursor.isEmpty()) {
            this.itemRenderer.renderInGuiWithOverrides(cursor, mouseX - 8, mouseY - 8);
            this.itemRenderer.renderGuiItemOverlay(this.client.textRenderer, cursor, mouseX - 8, mouseY - 8);
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

    private void drawSelectedItemWindow(MatrixStack matrices, Layout L, ItemStack stack, int mouseX, int mouseY) {
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();
        RustFontRenderer rf = RustFontRenderer.regular();
        RustFontRenderer sf = RustFontRenderer.small();

        // Olive dark window background (#3a3d35)
        DrawableHelper.fill(matrices, L.selX, L.selY, L.selX + L.selW, L.selY + L.selH, RustUiHelper.COLOR_SEL_PANEL_BG);
        // Header bar (rgba(136,132,116,0.56))
        DrawableHelper.fill(matrices, L.selX, L.selY, L.selX + L.selW, L.selY + 15, RustUiHelper.COLOR_SEL_HEADER_BG);

        String title = bfSmall.trim(stack.getName().getString().toUpperCase(), L.selW - 26);
        bfSmall.drawCenteredY(matrices, title, L.selX + 6, L.selY, 15, 0xFFF5F5F5);

        // Close X
        boolean closeHov = RustUiHelper.inRect(mouseX, mouseY, L.closeSelX, L.closeSelY, L.closeSelSize, L.closeSelSize);
        RustUiHelper.drawSprite(matrices, this.client, RustUiHelper.TEX_BTN_CLOSE,
                L.closeSelX + 2, L.closeSelY + 2, 9, 9, closeHov ? 0xFFFFFFFF : 0xBBF7EBE1);

        // Item icon on left inside selected window
        int iconBoxX = L.selX + 6;
        int iconBoxY = L.selY + 20;
        DrawableHelper.fill(matrices, iconBoxX, iconBoxY, iconBoxX + 26, iconBoxY + 26, 0x44000000);
        this.itemRenderer.renderInGuiWithOverrides(stack, iconBoxX + 5, iconBoxY + 5);

        // Item info text next to icon
        String stackInfo = "Количество: " + stack.getCount() + " / Макс: " + stack.getMaxCount();
        rf.draw(matrices, stackInfo, iconBoxX + 32, iconBoxY + 2, RustUiHelper.COLOR_TEXT_CREAM);
        if (stack.isDamageable()) {
            int cur = stack.getMaxDamage() - stack.getDamage();
            sf.draw(matrices, "Прочность: " + cur + " / " + stack.getMaxDamage(), iconBoxX + 32, iconBoxY + 14, 0xFFB0C4A8);
        } else {
            sf.draw(matrices, "ЛКМ: взять | ПКМ: половина | 1-9: в хотбар", iconBoxX + 32, iconBoxY + 14, RustUiHelper.COLOR_TEXT_DIM);
        }

        // Bottom actions row: [ РАЗДЕЛИТЬ 1/2 ] (if count > 1) and [ ВЫБРОСИТЬ ]
        if (stack.getCount() > 1) {
            boolean splitHov = RustUiHelper.inRect(mouseX, mouseY, L.splitX, L.splitY, L.splitW, L.splitH);
            DrawableHelper.fill(matrices, L.splitX, L.splitY, L.splitX + L.splitW, L.splitY + L.splitH,
                    splitHov ? 0xE0B0AC9A : RustUiHelper.COLOR_SEL_DROP_BG);
            String spLabel = "РАЗДЕЛИТЬ 1/2";
            float spW = sf.getWidth(spLabel);
            sf.drawCenteredY(matrices, spLabel, L.splitX + (L.splitW - spW) / 2f, L.splitY, L.splitH, 0xFFFFFFFF);
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

        // Top Switch Button -> Crafting
        if (button == 0 && RustUiHelper.hitTestTopSwitchButton(mouseX, mouseY, screenW)) {
            RustUiHelper.playClick(this.client);
            this.client.openScreen(new RustCraftingScreen());
            return true;
        }

        // Crafting Queue cancel
        int qIdx = RustUiHelper.hitTestCraftingQueue(mouseX, mouseY, L.col1X, screenH - 6);
        if (qIdx >= 0 && qIdx < RustCraftingQueue.queue.size()) {
            RustCraftingQueue.queue.remove(qIdx);
            RustUiHelper.playClick(this.client);
            return true;
        }

        // Selected Item window controls
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
            if (selStack.getCount() > 1 && RustUiHelper.inRect(mouseX, mouseY, L.splitX, L.splitY, L.splitW, L.splitH)) {
                // Split stack 1/2 into first free inventory slot (or cursor if full)
                this.client.interactionManager.clickSlot(handler.syncId, selectedHandlerSlot, 1, SlotActionType.PICKUP, this.client.player);
                for (int s = 9; s <= 35; s++) {
                    if (handler.getSlot(s).getStack().isEmpty()) {
                        this.client.interactionManager.clickSlot(handler.syncId, s, 0, SlotActionType.PICKUP, this.client.player);
                        break;
                    }
                }
                RustUiHelper.playClick(this.client);
                return true;
            }
        }

        // 1. Armor & Clothing slot clicks
        int clothingSlotY = L.eqY;
        int clothingHeaderY = clothingSlotY - 14;
        int armorSlotY = clothingHeaderY - L.eqSlotSize - 4;
        int totalEqW = 5 * L.eqSlotSize + 4 * L.eqGap;
        int eqStartX = L.col1X + (L.col1W - totalEqW) / 2;

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            int sx = eqStartX + i * (L.eqSlotSize + L.eqGap);
            if (RustUiHelper.inRect(mouseX, mouseY, sx, armorSlotY, L.eqSlotSize, L.eqSlotSize)) {
                handleSlotClick(handler, ARMOR_SLOTS[i], button);
                return true;
            }
        }

        for (int i = 0; i < CLOTHING_SLOTS.length; i++) {
            int sx = eqStartX + i * (L.eqSlotSize + L.eqGap);
            if (RustUiHelper.inRect(mouseX, mouseY, sx, clothingSlotY, L.eqSlotSize, L.eqSlotSize)) {
                handleSlotClick(handler, CLOTHING_SLOTS[i], button);
                return true;
            }
        }

        // 2. Main 6x4 Inventory clicks (24 slots: 9..32)
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 6; col++) {
                int slotId = 9 + row * 6 + col;
                int sx = L.gridStartX + col * (L.slotSize + L.slotGap);
                int sy = L.gridStartY + row * (L.slotSize + L.slotGap);
                if (RustUiHelper.inRect(mouseX, mouseY, sx, sy, L.slotSize, L.slotSize)) {
                    handleSlotClick(handler, slotId, button);
                    return true;
                }
            }
        }

        // 3. Hotbar clicks (6 active slots: 36..41)
        int hotbarX = RustUiHelper.getHotbarStartX(screenW);
        int hotbarY = screenH - RustUiHelper.HOTBAR_SLOT_SIZE - 10;
        for (int i = 0; i < RustUiHelper.HOTBAR_SLOTS; i++) {
            int sx = hotbarX + i * (RustUiHelper.HOTBAR_SLOT_SIZE + RustUiHelper.HOTBAR_GAP);
            if (RustUiHelper.inRect(mouseX, mouseY, sx, hotbarY, RustUiHelper.HOTBAR_SLOT_SIZE, RustUiHelper.HOTBAR_SLOT_SIZE)) {
                handleSlotClick(handler, 36 + i, button);
                return true;
            }
        }

        // 4. Quick Craft clicks
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
                        for (RustCraftingScreen.Ingredient ing : r.ingredients) {
                            RustCraftingScreen.deductPlayerResource(this.client.player, ing.mcItem, ing.amount);
                        }
                        ItemStack res = r.resultStack.copy();
                        res.setCount(1);
                        RustCraftingQueue.addTask(r.name, res, 1, r.craftSeconds);
                        RustUiHelper.playClick(this.client);
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleSlotClick(PlayerScreenHandler handler, int slotId, int button) {
        selectedHandlerSlot = slotId;
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
