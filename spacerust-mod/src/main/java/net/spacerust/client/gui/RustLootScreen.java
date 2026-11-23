package net.spacerust.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.spacerust.client.SpaceRustClient;
import net.spacerust.client.world.RustWorldSync;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

/**
 * 1:1 Rust / CoilRust Loot Screen replacing vanilla chest / container GUI
 * (Screenshot: left 3D Player + stats + wear, center 6x4 Inventory + 6 Belt,
 * right "ОБЫСКАТЬ" container grid + bottom-right metabolism HUD).
 */
public class RustLootScreen extends GenericContainerScreen {

    private static final int[] EQUIP_SLOTS = {5, 6, 7, 8, 45};
    private static final int INV_COLS = 6;
    private static final int INV_ROWS = 4;

    private int hoveredHandlerSlot = -1;
    private int selectedHandlerSlot = -1;

    // Quick Loot (Hold key H / Middle Mouse + Hover -> lime-green radial fill)
    private int quickLootSlot = -1;
    private long quickLootStartMs = 0L;
    private int lastQuickLootedSlot = -1;
    private long lastQuickLootTimeMs = 0L;

    public RustLootScreen(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
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
        RustUiHelper.disableWorldBlur(this.client);
        super.removed(); // Sends closeHandledScreen packet cleanly to server
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        // Rendered completely in render()
    }

    private static class Layout {
        int containerX, containerW;
        int col1X, col1W;
        int slotSize, slotGap, gridW, gridH, gridStartX, gridStartY;
        int invSectionTopY, invBottomY;
        int eqSlotSize, eqGap, eqStartX, eqY;
        int entityScale, modelCenterX, previewBottomY;
        int lootX, lootW, lootCols, lootRows, lootSlot, lootGap, lootStartX, lootStartY, lootHeaderY;
        int selX, selY, selW, selH, dropX, dropY, dropW, dropH, closeSelX, closeSelY, closeSelSize;
    }

    private Layout computeLayout(int screenW, int screenH, int containerSlots) {
        Layout L = new Layout();
        // Align 6x4 Inventory pixel-for-pixel above 6-slot active belt
        L.slotGap = RustUiHelper.HOTBAR_GAP;
        L.slotSize = RustUiHelper.getHotbarSlotSize(screenH);
        int hotbarTopY = screenH - L.slotSize - 8;

        L.gridW = INV_COLS * L.slotSize + (INV_COLS - 1) * L.slotGap;
        L.gridH = INV_ROWS * L.slotSize + (INV_ROWS - 1) * L.slotGap;
        L.gridStartX = (screenW - L.gridW) / 2;

        L.containerW = Math.min(screenW - 16, Math.max(590, (int) (screenW * 0.92f)));
        L.containerX = (screenW - L.containerW) / 2;

        L.col1W = Math.max(120, Math.min(200, L.gridStartX - 20));
        L.col1X = Math.max(8, L.gridStartX - L.col1W - 14);

        L.invBottomY = hotbarTopY - 10;
        L.gridStartY = L.invBottomY - L.gridH;
        L.invSectionTopY = L.gridStartY - 15;

        // 5 Wear slots underneath character feet
        L.eqSlotSize = Math.max(22, Math.min(32, (L.col1W - 12) / 5));
        L.eqGap = 2;
        int eqTotalW = EQUIP_SLOTS.length * L.eqSlotSize + (EQUIP_SLOTS.length - 1) * L.eqGap;
        L.eqStartX = L.col1X + (L.col1W - eqTotalW) / 2;
        L.eqY = L.invBottomY - L.eqSlotSize;

        L.previewBottomY = L.eqY - 10;
        L.entityScale = Math.max(44, Math.min(78, (int) (screenH * 0.18f)));
        L.modelCenterX = L.col1X + (int) (L.col1W * 0.34f);

        // Selected item inspector above Inventory
        L.selW = L.gridW;
        L.selX = L.gridStartX;
        L.selH = Math.max(60, Math.min(82, L.invSectionTopY - 24));
        L.selY = L.invSectionTopY - L.selH - 6;
        L.closeSelSize = 13;
        L.closeSelX = L.selX + L.selW - L.closeSelSize - 2;
        L.closeSelY = L.selY + 1;
        L.dropW = 72;
        L.dropH = 18;
        L.dropX = L.selX + L.selW - L.dropW - 6;
        L.dropY = L.selY + L.selH - L.dropH - 5;

        // Right Column: ОБЫСКАТЬ (Loot container grid)
        L.lootX = L.gridStartX + L.gridW + 14;
        L.lootW = Math.max(150, screenW - L.lootX - 10);
        L.lootCols = (containerSlots <= 9) ? containerSlots : 6;
        if (L.lootCols > 6 && containerSlots <= 9) L.lootCols = 6;
        L.lootRows = Math.max(1, (containerSlots + L.lootCols - 1) / L.lootCols);

        L.lootGap = 2;
        int availLootH = (screenH - 58) - 44; // keep clear above bottom-right metabolism HUD
        int maxByH = (availLootH - (L.lootRows - 1) * L.lootGap) / L.lootRows;
        int maxByW = (L.lootW - (L.lootCols - 1) * L.lootGap) / L.lootCols;
        L.lootSlot = Math.max(20, Math.min(34, Math.min(maxByH, maxByW)));

        int totalLootH = L.lootRows * L.lootSlot + (L.lootRows - 1) * L.lootGap;
        L.lootStartX = L.lootX;
        L.lootStartY = Math.max(40, (screenH - 58) - totalLootH);
        L.lootHeaderY = L.lootStartY - 26;

        return L;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        DrawableHelper.fill(matrices, 0, 0, this.width, this.height, RustUiHelper.COLOR_BACKDROP);
        if (this.client == null || this.client.player == null) return;

        int screenW = this.width;
        int screenH = this.height;
        int containerSlots = this.handler.getRows() * 9;
        hoveredHandlerSlot = -1;
        int hoveredRectX = 0, hoveredRectY = 0, hoveredRectSize = 0;
        Layout L = computeLayout(screenW, screenH, containerSlots);

        RustFontRenderer bfLarge = RustFontRenderer.boldLarge();
        RustFontRenderer bf = RustFontRenderer.bold();
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();
        RustFontRenderer sf = RustFontRenderer.small();

        // ======================================================
        // 1. LEFT COLUMN: 3D MODEL + NICKNAME + 4 PROT ROWS + WEAR
        // ======================================================
        InventoryScreen.drawEntity(
                L.modelCenterX,
                L.previewBottomY,
                L.entityScale,
                (float) (L.modelCenterX - mouseX),
                (float) (L.previewBottomY - L.entityScale * 1.55f - mouseY),
                this.client.player
        );

        int statRight = L.col1X + L.col1W - 2;
        int statW = Math.min(96, (int) (L.col1W * 0.54f));
        int statX = statRight - statW;
        int modelHeadY = Math.max(26, L.previewBottomY - (int) (L.entityScale * 1.85f));

        String playerName = this.client.player.getName().getString();
        String dispName = bf.trim(playerName, statW + 14);
        bf.draw(matrices, dispName, statRight - bf.getWidth(dispName), modelHeadY, RustUiHelper.COLOR_TEXT_CREAM);

        int armorPts = this.client.player.getArmor();
        int expProt  = Math.min(50, armorPts * 2);
        int coldProt = Math.min(50, armorPts + 4);
        int radProt  = Math.min(50, armorPts * 2);
        int biteProt = Math.min(60, armorPts * 3);

        int rowY = modelHeadY + 18;
        drawProtRow(matrices, statX, rowY,      statW, 13, expProt  + " %", RustUiHelper.TEX_PROT_EXPLOSION, expProt  > 0);
        drawProtRow(matrices, statX, rowY + 15, statW, 13, coldProt + " %", RustUiHelper.TEX_PROT_COLD,      coldProt > 0);
        drawProtRow(matrices, statX, rowY + 30, statW, 13, radProt  + " %", RustUiHelper.TEX_PROT_RAD,       radProt  > 0);
        drawProtRow(matrices, statX, rowY + 45, statW, 13, biteProt + " %", RustUiHelper.TEX_PROT_BITE,      biteProt > 0);

        PlayerScreenHandler psh = this.client.player.playerScreenHandler;
        for (int i = 0; i < EQUIP_SLOTS.length; i++) {
            int sx = L.eqStartX + i * (L.eqSlotSize + L.eqGap);
            ItemStack st = psh.getSlot(EQUIP_SLOTS[i]).getStack();
            int bg = st.isEmpty() ? RustUiHelper.COLOR_SLOT_EMPTY : RustUiHelper.COLOR_SLOT_OCCUPIED;
            DrawableHelper.fill(matrices, sx, L.eqY, sx + L.eqSlotSize, L.eqY + L.eqSlotSize, bg);
            if (st.isEmpty()) {
                RustUiHelper.drawSprite(matrices, this.client,
                        i == 4 ? RustUiHelper.TEX_SHIELD : RustUiHelper.TEX_ARMOR,
                        sx + (L.eqSlotSize - 11) / 2, L.eqY + (L.eqSlotSize - 11) / 2, 11, 11, 0x33F7EBE1);
            } else {
                RustItemIcons.drawSlotItem(matrices, this.client, this.itemRenderer, st, sx, L.eqY, L.eqSlotSize);
            }
        }

        // ======================================================
        // 2. CENTER COLUMN: SELECTED ITEM + 6x4 INVENTORY (24 SLOTS)
        // ======================================================
        if (selectedHandlerSlot >= 0 && selectedHandlerSlot < this.handler.slots.size()) {
            ItemStack selStack = this.handler.getSlot(selectedHandlerSlot).getStack();
            if (!selStack.isEmpty()) {
                drawSelectedItemWindow(matrices, L, selStack, mouseX, mouseY);
            } else {
                selectedHandlerSlot = -1;
            }
        }

        bf.draw(matrices, "ИНВЕНТАРЬ", L.gridStartX + 1, L.invSectionTopY + 1, RustUiHelper.COLOR_TEXT_CREAM);

        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                int slotId = containerSlots + row * INV_COLS + col;
                int sx = L.gridStartX + col * (L.slotSize + L.slotGap);
                int sy = L.gridStartY + row * (L.slotSize + L.slotGap);

                boolean hov = RustUiHelper.inRect(mouseX, mouseY, sx, sy, L.slotSize, L.slotSize);
                if (hov) {
                    hoveredHandlerSlot = slotId;
                    hoveredRectX = sx;
                    hoveredRectY = sy;
                    hoveredRectSize = L.slotSize;
                }

                ItemStack stack = this.handler.getSlot(slotId).getStack();
                boolean sel = (selectedHandlerSlot == slotId);
                int bg = sel ? RustUiHelper.COLOR_SELECTED_BLUE
                        : (hov ? RustUiHelper.COLOR_SLOT_HOVER
                        : (stack.isEmpty() ? RustUiHelper.COLOR_SLOT_EMPTY : RustUiHelper.COLOR_SLOT_OCCUPIED));
                DrawableHelper.fill(matrices, sx, sy, sx + L.slotSize, sy + L.slotSize, bg);

                if (!stack.isEmpty()) {
                    RustItemIcons.drawSlotItem(matrices, this.client, this.itemRenderer, stack, sx, sy, L.slotSize);
                }
            }
        }

        // ======================================================
        // 3. RIGHT COLUMN: ОБЫСКАТЬ (CONTAINER LOOT GRID)
        // ======================================================
        int lootGridW = L.lootCols * L.lootSlot + (L.lootCols - 1) * L.lootGap;
        bfLarge.draw(matrices, "ОБЫСКАТЬ", L.lootStartX, L.lootHeaderY, RustUiHelper.COLOR_TEXT_CREAM);

        String boxTitle = this.title.getString().replaceAll("§.", "");
        if ("Chest".equalsIgnoreCase(boxTitle) || "Large Chest".equalsIgnoreCase(boxTitle)) {
            boxTitle = containerSlots > 27 ? "Большой деревянный ящик" : "Деревянный ящик";
        }
        DrawableHelper.fill(matrices, L.lootStartX, L.lootHeaderY + 13, L.lootStartX + lootGridW, L.lootHeaderY + 24, RustUiHelper.COLOR_HEADER);
        bfSmall.drawCenteredY(matrices, bfSmall.trim(boxTitle, lootGridW - 8),
                L.lootStartX + 4, L.lootHeaderY + 13, 11, RustUiHelper.COLOR_TEXT_CREAM);

        for (int i = 0; i < containerSlots; i++) {
            int col = i % L.lootCols;
            int row = i / L.lootCols;
            int rx = L.lootStartX + col * (L.lootSlot + L.lootGap);
            int ry = L.lootStartY + row * (L.lootSlot + L.lootGap);

            boolean hov = RustUiHelper.inRect(mouseX, mouseY, rx, ry, L.lootSlot, L.lootSlot);
            if (hov) {
                hoveredHandlerSlot = i;
                hoveredRectX = rx;
                hoveredRectY = ry;
                hoveredRectSize = L.lootSlot;
            }

            ItemStack stack = this.handler.getSlot(i).getStack();
            boolean sel = (selectedHandlerSlot == i);
            int bg = sel ? RustUiHelper.COLOR_SELECTED_BLUE
                    : (hov ? RustUiHelper.COLOR_SLOT_HOVER
                    : (stack.isEmpty() ? RustUiHelper.COLOR_SLOT_EMPTY : RustUiHelper.COLOR_SLOT_OCCUPIED));
            DrawableHelper.fill(matrices, rx, ry, rx + L.lootSlot, ry + L.lootSlot, bg);

            if (!stack.isEmpty()) {
                RustItemIcons.drawSlotItem(matrices, this.client, this.itemRenderer, stack, rx, ry, L.lootSlot);
            }
        }

        // ======================================================
        // 4. BOTTOM: 6-SLOT HOTBAR + METABOLISM HUD
        // ======================================================
        int hotbarHover = RustUiHelper.drawHotbarAt(matrices, this.client, mouseX, mouseY,
                L.gridStartX, screenH, L.slotSize, L.slotGap, this.itemRenderer, true);
        if (hotbarHover >= 36 && hotbarHover < 36 + RustUiHelper.HOTBAR_SLOTS) {
            int hbIdx = hotbarHover - 36;
            hoveredHandlerSlot = containerSlots + 27 + hbIdx;
            hoveredRectX = L.gridStartX + hbIdx * (L.slotSize + L.slotGap);
            hoveredRectY = screenH - L.slotSize - 8;
            hoveredRectSize = L.slotSize;
        }
        RustUiHelper.drawMetabolismBars(matrices, this.client, screenW, screenH);

        // Quick Loot hint right under crate grid
        int lootBottomY = L.lootStartY + L.lootRows * L.lootSlot + (L.lootRows - 1) * L.lootGap;
        String qlKeyName = SpaceRustClient.keyQuickLoot != null
                ? SpaceRustClient.keyQuickLoot.getBoundKeyLocalizedText().getString().toUpperCase()
                : "H";
        sf.draw(matrices, qlKeyName + " + наведение / Shift+ЛКМ — быстрый лут", L.lootStartX, lootBottomY + 4, RustUiHelper.COLOR_TEXT_DIM);

        // Cursor Stack under mouse or Tooltip (hide tooltip while holding Quick Loot so circle stays clear)
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
        } else if (!isQuickLootHeld() && hoveredHandlerSlot >= 0 && hoveredHandlerSlot < this.handler.slots.size()) {
            ItemStack hs = this.handler.getSlot(hoveredHandlerSlot).getStack();
            if (!hs.isEmpty()) {
                this.renderTooltip(matrices, hs, mouseX, mouseY);
            }
        }

        // Quick Loot circular radial indicator & auto-transfer (always rendered on top)
        updateAndDrawQuickLoot(matrices, hoveredRectX, hoveredRectY, hoveredRectSize, cursor);
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
        DrawableHelper.fill(matrices, iconBoxX, iconBoxY, iconBoxX + 24, iconBoxY + 24, 0x44000000);
        RustItemIcons.drawSlotItem(matrices, this.client, this.itemRenderer, stack, iconBoxX, iconBoxY, 24);

        int realCount = RustStackUtil.getCount(stack);
        rf.draw(matrices, "Кол-во: " + realCount, iconBoxX + 30, iconBoxY + 2, RustUiHelper.COLOR_TEXT_CREAM);
        sf.draw(matrices, "Shift+ЛКМ: быстро забрать", iconBoxX + 30, iconBoxY + 13, RustUiHelper.COLOR_TEXT_DIM);

        boolean dropHov = RustUiHelper.inRect(mouseX, mouseY, L.dropX, L.dropY, L.dropW, L.dropH);
        DrawableHelper.fill(matrices, L.dropX, L.dropY, L.dropX + L.dropW, L.dropY + L.dropH,
                dropHov ? 0xE0B0AC9A : RustUiHelper.COLOR_SEL_DROP_BG);
        RustUiHelper.drawSprite(matrices, this.client, RustUiHelper.TEX_SELECTED_DROP, L.dropX + 5, L.dropY + 4, 10, 10, 0xFFFFFFFF);
        bfSmall.drawCenteredY(matrices, "ВЫБРОСИТЬ", L.dropX + 18, L.dropY, L.dropH, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.client == null || this.client.player == null || this.client.interactionManager == null) {
            return true;
        }
        int screenW = this.width;
        int screenH = this.height;
        int containerSlots = this.handler.getRows() * 9;
        Layout L = computeLayout(screenW, screenH, containerSlots);

        if (button == 0 && selectedHandlerSlot >= 0 && selectedHandlerSlot < this.handler.slots.size()) {
            if (RustUiHelper.inRect(mouseX, mouseY, L.closeSelX, L.closeSelY, L.closeSelSize, L.closeSelSize)) {
                selectedHandlerSlot = -1;
                RustUiHelper.playClick(this.client);
                return true;
            }
            if (RustUiHelper.inRect(mouseX, mouseY, L.dropX, L.dropY, L.dropW, L.dropH)) {
                this.client.interactionManager.clickSlot(this.handler.syncId, selectedHandlerSlot, 1, SlotActionType.THROW, this.client.player);
                selectedHandlerSlot = -1;
                RustUiHelper.playClick(this.client);
                return true;
            }
        }

        // Inventory 6x4 clicks
        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                int slotId = containerSlots + row * INV_COLS + col;
                int sx = L.gridStartX + col * (L.slotSize + L.slotGap);
                int sy = L.gridStartY + row * (L.slotSize + L.slotGap);
                if (RustUiHelper.inRect(mouseX, mouseY, sx, sy, L.slotSize, L.slotSize)) {
                    clickSlot(slotId, button);
                    return true;
                }
            }
        }

        // Container Loot slots clicks
        for (int i = 0; i < containerSlots; i++) {
            int col = i % L.lootCols;
            int row = i / L.lootCols;
            int rx = L.lootStartX + col * (L.lootSlot + L.lootGap);
            int ry = L.lootStartY + row * (L.lootSlot + L.lootGap);
            if (RustUiHelper.inRect(mouseX, mouseY, rx, ry, L.lootSlot, L.lootSlot)) {
                clickSlot(i, button);
                return true;
            }
        }

        // 6-slot Hotbar clicks aligned directly under Inventory columns
        int hotbarY = screenH - L.slotSize - 8;
        for (int i = 0; i < RustUiHelper.HOTBAR_SLOTS; i++) {
            int sx = L.gridStartX + i * (L.slotSize + L.slotGap);
            if (RustUiHelper.inRect(mouseX, mouseY, sx, hotbarY, L.slotSize, L.slotSize)) {
                clickSlot(containerSlots + 27 + i, button);
                return true;
            }
        }

        // Click in empty space while holding a cursor stack throws item into the world
        if (!this.client.player.inventory.getCursorStack().isEmpty()) {
            this.client.interactionManager.clickSlot(this.handler.syncId, -999, button == 1 ? 1 : 0, SlotActionType.PICKUP, this.client.player);
            RustUiHelper.playClick(this.client);
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.client != null && this.client.player != null && this.client.interactionManager != null) {
            if (!this.client.player.inventory.getCursorStack().isEmpty()) {
                int containerSlots = this.handler.getRows() * 9;
                Layout L = computeLayout(this.width, this.height, containerSlots);
                if (!isOverInteractiveArea(mouseX, mouseY, L)) {
                    this.client.interactionManager.clickSlot(this.handler.syncId, -999, button == 1 ? 1 : 0, SlotActionType.PICKUP, this.client.player);
                    RustUiHelper.playClick(this.client);
                    return true;
                }
            }
        }
        return true;
    }

    private boolean isOverInteractiveArea(double mx, double my, Layout L) {
        if (RustUiHelper.inRect(mx, my, L.gridStartX, L.gridStartY, L.gridW, L.gridH)) return true;
        int hotbarY = this.height - L.slotSize - 8;
        if (RustUiHelper.inRect(mx, my, L.gridStartX, hotbarY, L.gridW, L.slotSize)) return true;
        int lootGridW = L.lootCols * L.lootSlot + (L.lootCols - 1) * L.lootGap;
        int lootGridH = L.lootRows * L.lootSlot + (L.lootRows - 1) * L.lootGap;
        if (RustUiHelper.inRect(mx, my, L.lootStartX, L.lootStartY, lootGridW, lootGridH)) return true;
        if (selectedHandlerSlot >= 0 && RustUiHelper.inRect(mx, my, L.selX, L.selY, L.selW, L.selH)) return true;
        return false;
    }

    private void clickSlot(int slotId, int button) {
        selectedHandlerSlot = slotId;
        if (Screen.hasShiftDown()) {
            RustWorldSync.sendLootMove(slotId);
            RustUiHelper.playClick(this.client);
            return;
        }
        int clickData = (button == 1) ? 1 : 0;
        this.client.interactionManager.clickSlot(this.handler.syncId, slotId, clickData, SlotActionType.PICKUP, this.client.player);
        RustUiHelper.playClick(this.client);
    }

    private boolean isQuickLootHeld() {
        if (this.client == null || this.client.getWindow() == null) return false;
        long win = this.client.getWindow().getHandle();
        if (GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS
                || GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_4) == GLFW.GLFW_PRESS) {
            return true;
        }
        if (SpaceRustClient.keyQuickLoot != null) {
            InputUtil.Key bound = InputUtil.fromTranslationKey(SpaceRustClient.keyQuickLoot.getBoundKeyTranslationKey());
            if (bound.getCategory() == InputUtil.Type.MOUSE) {
                return GLFW.glfwGetMouseButton(win, bound.getCode()) == GLFW.GLFW_PRESS;
            }
            if (bound.getCode() > 0 && GLFW.glfwGetKey(win, bound.getCode()) == GLFW.GLFW_PRESS) {
                return true;
            }
        }
        return GLFW.glfwGetKey(win, GLFW.GLFW_KEY_H) == GLFW.GLFW_PRESS;
    }

    private void updateAndDrawQuickLoot(MatrixStack matrices, int slotX, int slotY, int slotSize, ItemStack cursor) {
        if (!cursor.isEmpty() || !isQuickLootHeld() || hoveredHandlerSlot < 0 || hoveredHandlerSlot >= this.handler.slots.size()) {
            quickLootSlot = -1;
            quickLootStartMs = 0L;
            if (!isQuickLootHeld()) {
                lastQuickLootedSlot = -1;
            }
            return;
        }
        ItemStack stack = this.handler.getSlot(hoveredHandlerSlot).getStack();
        if (stack.isEmpty()) {
            quickLootSlot = -1;
            quickLootStartMs = 0L;
            return;
        }
        long now = System.currentTimeMillis();
        if (hoveredHandlerSlot == lastQuickLootedSlot && (now - lastQuickLootTimeMs) < 350L) {
            return;
        }
        if (quickLootSlot != hoveredHandlerSlot) {
            quickLootSlot = hoveredHandlerSlot;
            quickLootStartMs = now;
        }

        float durationMs = 190.0f;
        float progress = Math.max(0.08f, Math.min(1.0f, (now - quickLootStartMs) / durationMs));
        float cx = slotX + slotSize * 0.5f;
        float cy = slotY + slotSize * 0.5f;
        float radius = Math.max(8.0f, slotSize * 0.36f);

        drawQuickLootCircle(matrices, cx, cy, radius, progress);

        if (progress >= 1.0f) {
            RustWorldSync.sendLootMove(quickLootSlot);
            lastQuickLootedSlot = quickLootSlot;
            lastQuickLootTimeMs = now;
            quickLootSlot = -1;
            quickLootStartMs = 0L;
        }
    }

    private void drawQuickLootCircle(MatrixStack matrices, float cx, float cy, float radius, float progress) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.disableAlphaTest();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);

        matrices.push();
        matrices.translate(0.0, 0.0, 350.0);
        Matrix4f mat = matrices.peek().getModel();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        // Outer dark circular backdrop
        int segs = 36;
        buf.begin(GL11.GL_TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, cx, cy, 0f).color(0.06f, 0.10f, 0.04f, 0.80f).next();
        for (int i = 0; i <= segs; i++) {
            double ang = -(Math.PI * 2.0 * i) / segs; // CCW in screen Y-down
            buf.vertex(mat, cx + (float) Math.cos(ang) * (radius + 1.6f), cy + (float) Math.sin(ang) * (radius + 1.6f), 0f)
               .color(0.06f, 0.10f, 0.04f, 0.80f).next();
        }
        tess.draw();

        // Lime-green clockwise radial fill matching Rust / CoilRust quickloot disc
        int fillSegs = Math.max(1, Math.round(segs * progress));
        buf.begin(GL11.GL_TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, cx, cy, 1f).color(0.56f, 0.92f, 0.14f, 0.96f).next();
        for (int i = 0; i <= fillSegs; i++) {
            double ang = -Math.PI / 2.0 + (Math.PI * 2.0 * i) / segs;
            buf.vertex(mat, cx + (float) Math.cos(ang) * radius, cy + (float) Math.sin(ang) * radius, 1f)
               .color(0.56f, 0.92f, 0.14f, 0.96f).next();
        }
        tess.draw();

        matrices.pop();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.client != null && this.client.player != null && this.client.interactionManager != null) {
            if (hoveredHandlerSlot >= 0 && keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_6) {
                int hotbarIdx = keyCode - GLFW.GLFW_KEY_1;
                this.client.interactionManager.clickSlot(
                        this.handler.syncId,
                        hoveredHandlerSlot,
                        hotbarIdx,
                        SlotActionType.SWAP,
                        this.client.player
                );
                RustUiHelper.playClick(this.client);
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
