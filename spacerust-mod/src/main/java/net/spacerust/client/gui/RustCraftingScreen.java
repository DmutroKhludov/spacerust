package net.spacerust.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.List;

public class RustCraftingScreen extends Screen {

    public enum Category {
        WEAPONS("ОРУЖИЕ"),
        CONSTRUCTION("КОНСТРУКЦИИ"),
        MEDICAL("МЕДИЦИНА"),
        TOOLS("ИНСТРУМЕНТЫ");

        public final String title;
        Category(String title) { this.title = title; }
    }

    public static class CraftRecipe {
        public final String name;
        public final Category category;
        public final String description;
        public final int craftTimeSeconds;
        public final List<String> requirements;

        public CraftRecipe(String name, Category category, String description, int craftTimeSeconds, List<String> requirements) {
            this.name = name;
            this.category = category;
            this.description = description;
            this.craftTimeSeconds = craftTimeSeconds;
            this.requirements = requirements;
        }
    }

    // Static registry of recipes
    public static final List<CraftRecipe> RECIPES = new ArrayList<>();
    static {
        // WEAPONS
        List<String> akReq = new ArrayList<>();
        akReq.add("50 Металл высокого кач. (МВК)");
        akReq.add("200 Дерево");
        akReq.add("1 Корпус винтовки");
        akReq.add("4 Пружины");
        RECIPES.add(new CraftRecipe("Автомат AK-47", Category.WEAPONS, "Мощная штурмовая винтовка калибра 5.56 мм. Высокий урон и контролируемая отдача.", 30, akReq));

        List<String> revReq = new ArrayList<>();
        revReq.add("75 Фрагменты металла");
        revReq.add("1 Труба");
        revReq.add("1 Пружина");
        RECIPES.add(new CraftRecipe("Револьвер", Category.WEAPONS, "Надежное стартовое оружие на 8 патронов 9 мм.", 15, revReq));

        List<String> bowReq = new ArrayList<>();
        bowReq.add("200 Дерево");
        bowReq.add("50 Ткань");
        RECIPES.add(new CraftRecipe("Охотничий лук", Category.WEAPONS, "Бесшумное оружие для начального выживания.", 10, bowReq));

        // CONSTRUCTION
        List<String> planReq = new ArrayList<>();
        planReq.add("20 Дерево");
        RECIPES.add(new CraftRecipe("План постройки", Category.CONSTRUCTION, "Чертеж для возведения фундамента, стен, дверных проемов и крыш.", 5, planReq));

        List<String> hammerReq = new ArrayList<>();
        hammerReq.add("100 Дерево");
        RECIPES.add(new CraftRecipe("Строительный молоток", Category.CONSTRUCTION, "Инструмент для улучшения конструкций до дерева, камня, металла и МВК.", 5, hammerReq));

        List<String> lockReq = new ArrayList<>();
        lockReq.add("100 Фрагменты металла");
        RECIPES.add(new CraftRecipe("Кодовый замок", Category.CONSTRUCTION, "Электронный замок с 4-значным PIN-кодом для защиты базы.", 10, lockReq));

        // MEDICAL
        List<String> bandageReq = new ArrayList<>();
        bandageReq.add("10 Ткань");
        RECIPES.add(new CraftRecipe("Бинт", Category.MEDICAL, "Останавливает кровотечение и мгновенно восстанавливает 5 HP.", 3, bandageReq));

        List<String> syringeReq = new ArrayList<>();
        syringeReq.add("15 Ткань");
        syringeReq.add("10 Металл");
        syringeReq.add("10 Топливо низкого кач.");
        RECIPES.add(new CraftRecipe("Медицинский шприц", Category.MEDICAL, "Восстанавливает 15 HP сразу и 20 HP в течение 5 секунд.", 8, syringeReq));

        // TOOLS
        List<String> bagReq = new ArrayList<>();
        bagReq.add("30 Ткань");
        RECIPES.add(new CraftRecipe("Спальный мешок", Category.TOOLS, "Точка возрождения после смерти. Перезарядка 5 минут.", 10, bagReq));
    }

    private Category selectedCategory = Category.WEAPONS;
    private CraftRecipe selectedRecipe = RECIPES.get(0);
    private int selectedRecipeIndex = 0;

    public RustCraftingScreen() {
        super(new LiteralText("Rust Crafting"));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Do not freeze game in multiplayer!
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // Dark translucent vignette background
        this.renderBackground(matrices);

        int screenW = this.width;
        int screenH = this.height;

        int panelW = 460;
        int panelH = 280;
        int startX = (screenW - panelW) / 2;
        int startY = (screenH - panelH) / 2;

        // 1. MAIN PANEL BACKGROUND
        DrawableHelper.fill(matrices, startX, startY, startX + panelW, startY + panelH, 0xEE12161f);
        // Border highlight
        DrawableHelper.fill(matrices, startX, startY, startX + panelW, startY + 1, 0xFFe0462c);
        DrawableHelper.fill(matrices, startX, startY, startX + 1, startY + panelH, 0x33FFFFFF);
        DrawableHelper.fill(matrices, startX + panelW - 1, startY, startX + panelW, startY + panelH, 0x33FFFFFF);
        DrawableHelper.fill(matrices, startX, startY + panelH - 1, startX + panelW, startY + panelH, 0x33FFFFFF);

        // 2. HEADER: TITLE & CATEGORY TABS
        this.textRenderer.draw(matrices, "МЕНЮ СОЗДАНИЯ ПРЕДМЕТОВ", startX + 14, startY + 12, 0xFFFFFFFF);

        int tabX = startX + 14;
        int tabY = startY + 28;
        for (Category cat : Category.values()) {
            boolean isActive = cat == selectedCategory;
            int btnW = this.textRenderer.getWidth(cat.title) + 12;

            if (isActive) {
                DrawableHelper.fill(matrices, tabX, tabY, tabX + btnW, tabY + 16, 0xFFe0462c);
                this.textRenderer.draw(matrices, cat.title, tabX + 6, tabY + 4, 0xFFFFFFFF);
            } else {
                DrawableHelper.fill(matrices, tabX, tabY, tabX + btnW, tabY + 16, 0x33202634);
                this.textRenderer.draw(matrices, cat.title, tabX + 6, tabY + 4, 0x88FFFFFF);
            }
            tabX += btnW + 6;
        }

        // Horizontal separator
        DrawableHelper.fill(matrices, startX + 14, startY + 48, startX + panelW - 14, startY + 49, 0x22FFFFFF);

        // 3. LEFT COLUMN: RECIPE LIST
        int listX = startX + 14;
        int listY = startY + 56;
        int listW = 160;

        List<CraftRecipe> currentList = getRecipesForCategory(selectedCategory);
        for (int i = 0; i < currentList.size(); i++) {
            CraftRecipe recipe = currentList.get(i);
            boolean isSel = recipe == selectedRecipe;

            int itemY = listY + (i * 24);
            if (isSel) {
                DrawableHelper.fill(matrices, listX, itemY, listX + listW, itemY + 20, 0x55e0462c);
                DrawableHelper.fill(matrices, listX, itemY, listX + 2, itemY + 20, 0xFFe0462c);
                this.textRenderer.draw(matrices, recipe.name, listX + 8, itemY + 6, 0xFFFFFFFF);
            } else {
                DrawableHelper.fill(matrices, listX, itemY, listX + listW, itemY + 20, 0x221a202c);
                this.textRenderer.draw(matrices, recipe.name, listX + 8, itemY + 6, 0xAAFFFFFF);
            }
        }

        // Vertical separator
        DrawableHelper.fill(matrices, startX + 182, startY + 54, startX + 183, startY + panelH - 14, 0x22FFFFFF);

        // 4. RIGHT COLUMN: DETAILS & CRAFT BUTTON
        if (selectedRecipe != null) {
            int detailX = startX + 196;
            int detailY = startY + 56;

            // Title
            this.textRenderer.draw(matrices, selectedRecipe.name, detailX, detailY, 0xFFFFFFFF);
            
            // Description (draw line)
            this.textRenderer.draw(matrices, selectedRecipe.description, detailX, detailY + 14, 0x88AAAAAA);

            // Craft time
            String timeStr = "Время создания: " + selectedRecipe.craftTimeSeconds + " сек";
            this.textRenderer.draw(matrices, timeStr, detailX, detailY + 34, 0xFFff9800);

            // Required materials title
            this.textRenderer.draw(matrices, "ТРЕБУЕМЫЕ РЕСУРСЫ:", detailX, detailY + 54, 0xBBFFFFFF);

            int reqY = detailY + 68;
            for (String req : selectedRecipe.requirements) {
                this.textRenderer.draw(matrices, "• " + req, detailX + 4, reqY, 0xCCDDDDDD);
                reqY += 14;
            }

            // Big «СОЗДАТЬ» Button
            int btnCraftX = startX + panelW - 130;
            int btnCraftY = startY + panelH - 42;
            int btnCraftW = 116;
            int btnCraftH = 28;

            DrawableHelper.fill(matrices, btnCraftX, btnCraftY, btnCraftX + btnCraftW, btnCraftY + btnCraftH, 0xFFe0462c);
            int txtW = this.textRenderer.getWidth("СОЗДАТЬ");
            this.textRenderer.draw(matrices, "СОЗДАТЬ", btnCraftX + (btnCraftW - txtW) / 2, btnCraftY + 10, 0xFFFFFFFF);
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    private List<CraftRecipe> getRecipesForCategory(Category cat) {
        List<CraftRecipe> res = new ArrayList<>();
        for (CraftRecipe r : RECIPES) {
            if (r.category == cat) res.add(r);
        }
        return res;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int screenW = this.width;
        int screenH = this.height;
        int panelW = 460;
        int panelH = 280;
        int startX = (screenW - panelW) / 2;
        int startY = (screenH - panelH) / 2;

        // 1. Check Tabs Click
        int tabX = startX + 14;
        int tabY = startY + 28;
        for (Category cat : Category.values()) {
            int btnW = this.textRenderer.getWidth(cat.title) + 12;
            if (mouseX >= tabX && mouseX <= tabX + btnW && mouseY >= tabY && mouseY <= tabY + 16) {
                this.selectedCategory = cat;
                List<CraftRecipe> list = getRecipesForCategory(cat);
                if (!list.isEmpty()) {
                    this.selectedRecipe = list.get(0);
                }
                return true;
            }
            tabX += btnW + 6;
        }

        // 2. Check Recipe List Click
        int listX = startX + 14;
        int listY = startY + 56;
        int listW = 160;
        List<CraftRecipe> currentList = getRecipesForCategory(selectedCategory);
        for (int i = 0; i < currentList.size(); i++) {
            int itemY = listY + (i * 24);
            if (mouseX >= listX && mouseX <= listX + listW && mouseY >= itemY && mouseY <= itemY + 20) {
                this.selectedRecipe = currentList.get(i);
                return true;
            }
        }

        // 3. Check Craft Button Click
        int btnCraftX = startX + panelW - 130;
        int btnCraftY = startY + panelH - 42;
        int btnCraftW = 116;
        int btnCraftH = 28;
        if (mouseX >= btnCraftX && mouseX <= btnCraftX + btnCraftW && mouseY >= btnCraftY && mouseY <= btnCraftY + btnCraftH) {
            if (selectedRecipe != null && this.client != null && this.client.player != null) {
                // Send crafting trigger to server or local queue
                this.client.player.sendChatMessage("/craft " + selectedRecipe.name);
                // Also close screen or queue
                this.onClose();
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
