package net.spacerust.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.spacerust.client.hud.RustHudOverlay;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class RustCraftingScreen extends Screen {

    public enum Category {
        FAVORITES("ИЗБРАННОЕ"),
        GENERAL("ОБЩЕЕ"),
        CONSTRUCTION("КОНСТРУКЦИИ"),
        DEPLOYABLES("ОБЪЕКТЫ"),
        RESOURCES("РЕСУРСЫ"),
        CLOTHING("ОДЕЖДА"),
        TOOLS("ИНСТРУМЕНТЫ"),
        MEDICAL("МЕДИЦИНА"),
        WEAPONS("ОРУЖИЕ"),
        AMMO("БОЕПРИПАСЫ"),
        ELECTRICAL("ЭЛЕКТРИЧЕСТВО"),
        OTHER("ПРОЧЕЕ");

        public final String title;
        Category(String title) { this.title = title; }
    }

    public static class Ingredient {
        public final String name;
        public final int amount;
        public final Item mcItem;

        public Ingredient(String name, int amount, Item mcItem) {
            this.name = name;
            this.amount = amount;
            this.mcItem = mcItem;
        }
    }

    public static class Recipe {
        public final String id;
        public final String name;
        public final Category category;
        public final ItemStack resultStack;
        public final String description;
        public final float craftSeconds;
        public final List<Ingredient> ingredients;
        public boolean isFavorite = false;

        public Recipe(String id, String name, Category category, Item item, String desc, float seconds, List<Ingredient> ingredients) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.resultStack = new ItemStack(item);
            this.description = desc;
            this.craftSeconds = seconds;
            this.ingredients = ingredients;
        }
    }

    public static final List<Recipe> ALL_RECIPES = new ArrayList<>();
    static {
        // GENERAL
        ALL_RECIPES.add(new Recipe("campfire", "Костер", Category.GENERAL, Items.CAMPFIRE,
                "Базовый источник тепла и приготовления сырого мяса.", 5.0f,
                Arrays.asList(new Ingredient("Дерево", 100, Items.OAK_PLANKS))));
        ALL_RECIPES.add(new Recipe("torch", "Факел", Category.GENERAL, Items.TORCH,
                "Освещает территорию в ночное время суток.", 2.0f,
                Arrays.asList(new Ingredient("Дерево", 30, Items.OAK_PLANKS), new Ingredient("Ткань", 5, Items.STRING))));
        ALL_RECIPES.add(new Recipe("small_box", "Деревянный ящик", Category.GENERAL, Items.CHEST,
                "Маленький сундук для хранения до 12 слотов лута.", 10.0f,
                Arrays.asList(new Ingredient("Дерево", 100, Items.OAK_PLANKS))));
        ALL_RECIPES.add(new Recipe("bed", "Спальный мешок", Category.GENERAL, Items.WHITE_BED,
                "Точка возрождения после смерти. Перезарядка 5 минут.", 10.0f,
                Arrays.asList(new Ingredient("Ткань", 30, Items.STRING))));

        // CONSTRUCTION (Matches user screenshot: 11 recipes!)
        ALL_RECIPES.add(new Recipe("build_plan", "План постройки", Category.CONSTRUCTION, Items.MAP,
                "План постройки. Используется для создания баз - взяв в руки, нажмите на правую кнопку мыши для просмотра опций.", 30.0f,
                Arrays.asList(new Ingredient("Дерево", 20, Items.OAK_PLANKS))));
        ALL_RECIPES.add(new Recipe("wood_hammer", "Строительный молоток", Category.CONSTRUCTION, Items.WOODEN_AXE,
                "Инструмент для улучшения стен базы до дерева, камня, металла или МВК.", 5.0f,
                Arrays.asList(new Ingredient("Дерево", 100, Items.OAK_PLANKS))));
        ALL_RECIPES.add(new Recipe("wood_door", "Деревянная дверь", Category.CONSTRUCTION, Items.OAK_DOOR,
                "Базовая входная дверь для закрытия проемов дома.", 15.0f,
                Arrays.asList(new Ingredient("Дерево", 300, Items.OAK_PLANKS))));
        ALL_RECIPES.add(new Recipe("sheet_door", "Металлическая дверь", Category.CONSTRUCTION, Items.IRON_DOOR,
                "Прочная дверь из фрагментов металла, защищает от петард и начальных рейдов.", 30.0f,
                Arrays.asList(new Ingredient("Фрагменты металла", 150, Items.IRON_INGOT))));
        ALL_RECIPES.add(new Recipe("code_lock", "Кодовый замок", Category.CONSTRUCTION, Items.LEVER,
                "Электронный замок с 4-значным PIN-кодом для защиты дверей и ящиков.", 15.0f,
                Arrays.asList(new Ingredient("Фрагменты металла", 100, Items.IRON_INGOT))));
        ALL_RECIPES.add(new Recipe("key_lock", "Дверной замок", Category.CONSTRUCTION, Items.TRIPWIRE_HOOK,
                "Простой механический замок с ключом для закрытия дверей.", 5.0f,
                Arrays.asList(new Ingredient("Дерево", 100, Items.OAK_PLANKS))));
        ALL_RECIPES.add(new Recipe("wood_barricade", "Деревянная баррикада", Category.CONSTRUCTION, Items.OAK_FENCE,
                "Заграждение из колючих деревянных кольев для защиты периметра базы.", 10.0f,
                Arrays.asList(new Ingredient("Дерево", 200, Items.OAK_PLANKS))));
        ALL_RECIPES.add(new Recipe("wood_gate", "Деревянные ворота", Category.CONSTRUCTION, Items.OAK_FENCE_GATE,
                "Массивные внешние ворота для высокого забора вокруг базы.", 30.0f,
                Arrays.asList(new Ingredient("Дерево", 500, Items.OAK_PLANKS))));
        ALL_RECIPES.add(new Recipe("metal_bars", "Металлическая решетка", Category.CONSTRUCTION, Items.IRON_BARS,
                "Защитная решетка для оконных проемов с возможностью отстрела рейдов.", 15.0f,
                Arrays.asList(new Ingredient("Фрагменты металла", 50, Items.IRON_INGOT))));
        ALL_RECIPES.add(new Recipe("ladder", "Деревянная лестница", Category.CONSTRUCTION, Items.LADDER,
                "Лестница для вертикального перемещения по этажам и крыше базы.", 5.0f,
                Arrays.asList(new Ingredient("Дерево", 100, Items.OAK_PLANKS), new Ingredient("Веревка", 2, Items.STRING))));
        ALL_RECIPES.add(new Recipe("window_shutter", "Деревянные ставни", Category.CONSTRUCTION, Items.OAK_TRAPDOOR,
                "Защитные ставни для окон первого и второго этажей дома.", 10.0f,
                Arrays.asList(new Ingredient("Дерево", 200, Items.OAK_PLANKS))));

        // DEPLOYABLES
        ALL_RECIPES.add(new Recipe("furnace", "Печь", Category.DEPLOYABLES, Items.FURNACE,
                "Выплавляет металл, серу и МВК из сырой руды с помощью дерева.", 15.0f,
                Arrays.asList(new Ingredient("Камень", 200, Items.COBBLESTONE), new Ingredient("Дерево", 100, Items.OAK_PLANKS), new Ingredient("Топливо", 50, Items.COAL))));
        ALL_RECIPES.add(new Recipe("large_box", "Большой сундук", Category.DEPLOYABLES, Items.BARREL,
                "Вместительный ящик на 30 ячеек хранения для ресурсов и брони.", 15.0f,
                Arrays.asList(new Ingredient("Дерево", 250, Items.OAK_PLANKS), new Ingredient("Фрагменты металла", 50, Items.IRON_INGOT))));
        ALL_RECIPES.add(new Recipe("workbench_1", "Верстак 1 уровня", Category.DEPLOYABLES, Items.CRAFTING_TABLE,
                "Базовый верстак для изучения револьверов, двустволок и инструментов.", 30.0f,
                Arrays.asList(new Ingredient("Дерево", 500, Items.OAK_PLANKS), new Ingredient("Фрагменты металла", 100, Items.IRON_INGOT), new Ingredient("Скрап", 50, Items.EMERALD))));
        ALL_RECIPES.add(new Recipe("workbench_2", "Верстак 2 уровня", Category.DEPLOYABLES, Items.SMITHING_TABLE,
                "Продвинутый верстак для создания полуавтоматических винтовок, Томпсона и C4.", 60.0f,
                Arrays.asList(new Ingredient("Фрагменты металла", 500, Items.IRON_INGOT), new Ingredient("МВК", 20, Items.NETHERITE_INGOT), new Ingredient("Скрап", 500, Items.EMERALD))));

        // RESOURCES
        ALL_RECIPES.add(new Recipe("gunpowder", "Порох", Category.RESOURCES, Items.GUNPOWDER,
                "Ингредиент для крафта патронов, гранат, ракет и C4.", 2.0f,
                Arrays.asList(new Ingredient("Уголь", 20, Items.CHARCOAL), new Ingredient("Сера", 30, Items.GLOWSTONE_DUST))));
        ALL_RECIPES.add(new Recipe("low_grade_fuel", "Топливо низкого качества", Category.RESOURCES, Items.BLAZE_POWDER,
                "Горючая смесь из животного жира и ткани для печей и транспорта.", 2.0f,
                Arrays.asList(new Ingredient("Ткань", 10, Items.STRING), new Ingredient("Жир", 30, Items.LEATHER))));

        // CLOTHING
        ALL_RECIPES.add(new Recipe("burlap_shirt", "Рубашка из мешковины", Category.CLOTHING, Items.LEATHER_CHESTPLATE,
                "Начальная одежда выжившего для защиты от холода и ветра.", 5.0f,
                Arrays.asList(new Ingredient("Ткань", 40, Items.STRING))));
        ALL_RECIPES.add(new Recipe("burlap_pants", "Штаны из мешковины", Category.CLOTHING, Items.LEATHER_LEGGINGS,
                "Начальные штаны из грубой ткани.", 5.0f,
                Arrays.asList(new Ingredient("Ткань", 30, Items.STRING))));
        ALL_RECIPES.add(new Recipe("wood_armor", "Деревянная кираса", Category.CLOTHING, Items.CHAINMAIL_CHESTPLATE,
                "Броня из деревянных досок. Поглощает урон от стрел и дроби.", 10.0f,
                Arrays.asList(new Ingredient("Дерево", 200, Items.OAK_PLANKS), new Ingredient("Ткань", 30, Items.STRING))));
        ALL_RECIPES.add(new Recipe("metal_chest", "Металлическая кираса", Category.CLOTHING, Items.IRON_CHESTPLATE,
                "Топовая броня для PVP перестрелок с защитой от пуль винтовок.", 30.0f,
                Arrays.asList(new Ingredient("МВК", 25, Items.NETHERITE_INGOT), new Ingredient("Кожа", 50, Items.LEATHER))));

        // TOOLS
        ALL_RECIPES.add(new Recipe("stone_hatchet", "Каменный топор", Category.TOOLS, Items.STONE_AXE,
                "Инструмент для быстрой добычи дерева и разделки туш животных.", 5.0f,
                Arrays.asList(new Ingredient("Дерево", 200, Items.OAK_PLANKS), new Ingredient("Камень", 100, Items.COBBLESTONE))));
        ALL_RECIPES.add(new Recipe("stone_pickaxe", "Каменная кирка", Category.TOOLS, Items.STONE_PICKAXE,
                "Инструмент для эффективного фарма каменных, железных и серных жил.", 5.0f,
                Arrays.asList(new Ingredient("Дерево", 200, Items.OAK_PLANKS), new Ingredient("Камень", 100, Items.COBBLESTONE))));
        ALL_RECIPES.add(new Recipe("metal_hatchet", "Металлический топор", Category.TOOLS, Items.IRON_AXE,
                "Профессиональный топор для мгновенной рубки деревьев.", 15.0f,
                Arrays.asList(new Ingredient("Дерево", 100, Items.OAK_PLANKS), new Ingredient("Фрагменты металла", 75, Items.IRON_INGOT))));
        ALL_RECIPES.add(new Recipe("metal_pickaxe", "Металлическая кирка", Category.TOOLS, Items.IRON_PICKAXE,
                "Профессиональная кирка с максимальным коэффициентом добычи руды.", 15.0f,
                Arrays.asList(new Ingredient("Дерево", 100, Items.OAK_PLANKS), new Ingredient("Фрагменты металла", 125, Items.IRON_INGOT))));

        // MEDICAL
        ALL_RECIPES.add(new Recipe("bandage", "Бинт", Category.MEDICAL, Items.PAPER,
                "Останавливает кровотечение и мгновенно восстанавливает 5 HP.", 3.0f,
                Arrays.asList(new Ingredient("Ткань", 10, Items.STRING))));
        ALL_RECIPES.add(new Recipe("syringe", "Медицинский шприц", Category.MEDICAL, Items.POTION,
                "Мгновенно восстанавливает 15 HP и еще 20 HP в течение 5 секунд.", 8.0f,
                Arrays.asList(new Ingredient("Ткань", 15, Items.STRING), new Ingredient("Фрагменты металла", 10, Items.IRON_INGOT), new Ingredient("Топливо", 10, Items.BLAZE_POWDER))));

        // WEAPONS
        ALL_RECIPES.add(new Recipe("bow", "Охотничий лук", Category.WEAPONS, Items.BOW,
                "Бесшумное оружие дальнего боя. Наносит высокий урон в голову.", 10.0f,
                Arrays.asList(new Ingredient("Дерево", 200, Items.OAK_PLANKS), new Ingredient("Ткань", 50, Items.STRING))));
        ALL_RECIPES.add(new Recipe("crossbow", "Арбалет", Category.WEAPONS, Items.CROSSBOW,
                "Смертоносный арбалет с высокой точностью и скоростью полета стрелы.", 15.0f,
                Arrays.asList(new Ingredient("Дерево", 200, Items.OAK_PLANKS), new Ingredient("Фрагменты металла", 75, Items.IRON_INGOT), new Ingredient("Веревка", 2, Items.STRING))));
        ALL_RECIPES.add(new Recipe("revolver", "Револьвер", Category.WEAPONS, Items.IRON_HOE,
                "Надежное стартовое огнестрельное оружие на 8 патронов 9 мм.", 15.0f,
                Arrays.asList(new Ingredient("Фрагменты металла", 75, Items.IRON_INGOT), new Ingredient("Труба", 1, Items.LEVER), new Ingredient("Пружина", 1, Items.TRIPWIRE_HOOK))));
        ALL_RECIPES.add(new Recipe("pipe_shotgun", "Двуствольный дробовик", Category.WEAPONS, Items.WOODEN_SHOVEL,
                "Разрушительный урон в упор дробью 12 калибра.", 20.0f,
                Arrays.asList(new Ingredient("Фрагменты металла", 150, Items.IRON_INGOT), new Ingredient("Трубы", 2, Items.LEVER))));
        ALL_RECIPES.add(new Recipe("ak47", "Автомат AK-47", Category.WEAPONS, Items.NETHERITE_SWORD,
                "Штурмовая винтовка на 30 патронов 5.56 мм. Легендарное оружие Rust.", 30.0f,
                Arrays.asList(new Ingredient("МВК", 50, Items.NETHERITE_INGOT), new Ingredient("Дерево", 200, Items.OAK_PLANKS), new Ingredient("Корпус винтовки", 1, Items.OBSIDIAN), new Ingredient("Пружины", 4, Items.TRIPWIRE_HOOK))));
        ALL_RECIPES.add(new Recipe("c4", "Взрывчатка C4 (Timed Explosive)", Category.WEAPONS, Items.REDSTONE_BLOCK,
                "Мощный кумулятивный заряд с таймером для пробития каменных и металлических стен.", 30.0f,
                Arrays.asList(new Ingredient("Взрывчатка", 20, Items.TNT), new Ingredient("Ткань", 5, Items.STRING), new Ingredient("Схема", 1, Items.REDSTONE))));

        // AMMO
        ALL_RECIPES.add(new Recipe("wooden_arrows", "Деревянные стрелы (x2)", Category.AMMO, Items.ARROW,
                "Стрелы для лука и арбалета с каменным наконечником.", 3.0f,
                Arrays.asList(new Ingredient("Дерево", 25, Items.OAK_PLANKS), new Ingredient("Камень", 10, Items.COBBLESTONE))));
        ALL_RECIPES.add(new Recipe("ammo_9mm", "Патроны 9мм (x3)", Category.AMMO, Items.GOLD_NUGGET,
                "Пистолетные патроны для револьвера и пистолетов.", 5.0f,
                Arrays.asList(new Ingredient("Фрагменты металла", 10, Items.IRON_INGOT), new Ingredient("Порох", 10, Items.GUNPOWDER))));
        ALL_RECIPES.add(new Recipe("ammo_556", "Патроны 5.56мм (x3)", Category.AMMO, Items.IRON_NUGGET,
                "Винтовочные патроны для автоматов AK-47 и болтовок.", 5.0f,
                Arrays.asList(new Ingredient("Фрагменты металла", 15, Items.IRON_INGOT), new Ingredient("Порох", 20, Items.GUNPOWDER))));

        // ELECTRICAL
        ALL_RECIPES.add(new Recipe("solar_panel", "Солнечная панель", Category.ELECTRICAL, Items.DAYLIGHT_DETECTOR,
                "Генерирует электроэнергию в светлое время суток.", 15.0f,
                Arrays.asList(new Ingredient("МВК", 10, Items.NETHERITE_INGOT), new Ingredient("Фрагменты металла", 50, Items.IRON_INGOT))));

        // OTHER
        ALL_RECIPES.add(new Recipe("guitar", "Акустическая гитара", Category.OTHER, Items.NOTE_BLOCK,
                "Музыкальный инструмент для игры у костра с друзьями.", 10.0f,
                Arrays.asList(new Ingredient("Дерево", 100, Items.OAK_PLANKS), new Ingredient("Ткань", 10, Items.STRING))));
    }

    private Category selectedCategory = Category.CONSTRUCTION;
    private Recipe selectedRecipe;
    private int craftQuantity = 1;
    private String searchQuery = "";
    private boolean isSearchFocused = false;

    public RustCraftingScreen() {
        super(new LiteralText("Rust Crafting"));
        List<Recipe> initList = getFilteredRecipes();
        this.selectedRecipe = initList.isEmpty() ? null : initList.get(0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<Recipe> getFilteredRecipes() {
        List<Recipe> list = new ArrayList<>();
        String query = searchQuery.trim().toLowerCase();

        for (Recipe r : ALL_RECIPES) {
            boolean matchesCat = false;
            if (selectedCategory == Category.FAVORITES) {
                matchesCat = r.isFavorite;
            } else {
                matchesCat = r.category == selectedCategory;
            }

            if (matchesCat) {
                if (query.isEmpty() || r.name.toLowerCase().contains(query) || r.description.toLowerCase().contains(query)) {
                    list.add(r);
                }
            }
        }
        return list;
    }

    private int getCategoryCount(Category cat) {
        if (cat == Category.FAVORITES) {
            int count = 0;
            for (Recipe r : ALL_RECIPES) if (r.isFavorite) count++;
            return count;
        }
        int count = 0;
        for (Recipe r : ALL_RECIPES) {
            if (r.category == cat) count++;
        }
        return count;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // Dark translucent background vignette
        this.renderBackground(matrices);

        int screenW = this.width;
        int screenH = this.height;

        // TOP HEADER TAB: [->] ИНВЕНТАРЬ
        int topTabW = 120;
        int topTabH = 18;
        int topTabX = (screenW - topTabW) / 2;
        int topTabY = 4;
        DrawableHelper.fill(matrices, topTabX, topTabY, topTabX + topTabW, topTabY + topTabH, 0xDD222730);
        DrawableHelper.fill(matrices, topTabX, topTabY, topTabX + topTabW, topTabY + 1, 0x44FFFFFF);
        this.textRenderer.drawWithShadow(matrices, "➔  ИНВЕНТАРЬ", topTabX + 16, topTabY + 5, 0xFFE0E5EC);

        // MAIN MODAL WINDOW DIMENSIONS (Matching the layout in screenshot)
        int winX = 24;
        int winY = 24;
        int winW = screenW - 48;
        int winH = screenH - 80;

        // Background Modal Panel
        DrawableHelper.fill(matrices, winX, winY, winX + winW, winY + winH, 0xD4161A20);
        DrawableHelper.fill(matrices, winX, winY, winX + winW, winY + 1, 0x22FFFFFF);
        DrawableHelper.fill(matrices, winX, winY + winH - 1, winX + winW, winY + winH, 0x22FFFFFF);
        DrawableHelper.fill(matrices, winX, winY, winX + 1, winY + winH, 0x22FFFFFF);
        DrawableHelper.fill(matrices, winX + winW - 1, winY, winX + winW, winY + winH, 0x22FFFFFF);

        // ==========================================
        // 1. LEFT SIDEBAR: CATEGORIES
        // ==========================================
        int catColW = 92;
        int catColX = winX;
        int catColY = winY;

        DrawableHelper.fill(matrices, catColX + catColW, winY, catColX + catColW + 1, winY + winH, 0x22FFFFFF);

        Category[] cats = Category.values();
        int catItemH = 16;
        int currentCatY = catColY + 8;

        for (Category cat : cats) {
            boolean isSelected = cat == selectedCategory;
            boolean isHovered = mouseX >= catColX && mouseX <= catColX + catColW && mouseY >= currentCatY && mouseY <= currentCatY + catItemH;

            int count = getCategoryCount(cat);

            if (isSelected) {
                // RUST DISTINCTIVE SOLID BLUE ACTIVE BAR (Exact color from user screenshot #2f7eb2)
                DrawableHelper.fill(matrices, catColX, currentCatY, catColX + catColW, currentCatY + catItemH, 0xFF2F7EB2);
                this.textRenderer.drawWithShadow(matrices, cat.title, catColX + 6, currentCatY + 4, 0xFFFFFFFF);
                if (count > 0) {
                    String cntStr = String.valueOf(count);
                    int cntW = this.textRenderer.getWidth(cntStr);
                    this.textRenderer.drawWithShadow(matrices, cntStr, catColX + catColW - cntW - 6, currentCatY + 4, 0xFFFFFFFF);
                }
            } else {
                if (isHovered) {
                    DrawableHelper.fill(matrices, catColX, currentCatY, catColX + catColW, currentCatY + catItemH, 0x33FFFFFF);
                }
                this.textRenderer.drawWithShadow(matrices, cat.title, catColX + 6, currentCatY + 4, 0x88AAAAAA);
                if (count > 0) {
                    String cntStr = String.valueOf(count);
                    int cntW = this.textRenderer.getWidth(cntStr);
                    this.textRenderer.drawWithShadow(matrices, cntStr, catColX + catColW - cntW - 6, currentCatY + 4, 0x55AAAAAA);
                }
            }

            currentCatY += catItemH + 2;
        }

        // ==========================================
        // 2. MIDDLE COLUMN: ITEM RECIPE GRID & SEARCH
        // ==========================================
        int gridX = catColX + catColW + 1;
        int gridW = (int) ((winW - catColW) * 0.44f);
        int gridH = winH - 26; // Room for search at bottom

        // Vertical separator line between grid and details
        DrawableHelper.fill(matrices, gridX + gridW, winY, gridX + gridW + 1, winY + winH, 0x22FFFFFF);

        List<Recipe> recipes = getFilteredRecipes();

        int slotSize = 34;
        int slotPadding = 4;
        int cols = Math.max(3, (gridW - 16) / (slotSize + slotPadding));

        int startSlotX = gridX + 12;
        int startSlotY = winY + 12;

        for (int i = 0; i < recipes.size(); i++) {
            Recipe r = recipes.get(i);
            int col = i % cols;
            int row = i / cols;

            int sx = startSlotX + col * (slotSize + slotPadding);
            int sy = startSlotY + row * (slotSize + slotPadding);

            if (sy + slotSize > winY + gridH) break; // clip if overflowing

            boolean isHovered = mouseX >= sx && mouseX <= sx + slotSize && mouseY >= sy && mouseY <= sy + slotSize;
            boolean isSelected = r == selectedRecipe;

            // Slot background
            if (isSelected) {
                DrawableHelper.fill(matrices, sx, sy, sx + slotSize, sy + slotSize, 0x552F7EB2);
                DrawableHelper.fill(matrices, sx, sy, sx + slotSize, sy + 1, 0xFF2F7EB2);
                DrawableHelper.fill(matrices, sx, sy + slotSize - 1, sx + slotSize, sy + slotSize, 0xFF2F7EB2);
                DrawableHelper.fill(matrices, sx, sy, sx + 1, sy + slotSize, 0xFF2F7EB2);
                DrawableHelper.fill(matrices, sx + slotSize - 1, sy, sx + slotSize, sy + slotSize, 0xFF2F7EB2);
            } else if (isHovered) {
                DrawableHelper.fill(matrices, sx, sy, sx + slotSize, sy + slotSize, 0x33FFFFFF);
            } else {
                DrawableHelper.fill(matrices, sx, sy, sx + slotSize, sy + slotSize, 0x22181C22);
            }

            // Render Minecraft item stack icon in 3D centered
            this.itemRenderer.renderInGuiWithOverrides(r.resultStack, sx + (slotSize - 16) / 2, sy + (slotSize - 16) / 2);
        }

        // SEARCH BAR AT BOTTOM OF MIDDLE GRID
        int searchBoxX = gridX + 12;
        int searchBoxY = winY + winH - 22;
        int searchBoxW = gridW - 24;
        int searchBoxH = 16;

        DrawableHelper.fill(matrices, searchBoxX, searchBoxY, searchBoxX + searchBoxW, searchBoxY + searchBoxH, 0xAA101318);
        DrawableHelper.fill(matrices, searchBoxX, searchBoxY, searchBoxX + searchBoxW, searchBoxY + 1, isSearchFocused ? 0xFF2F7EB2 : 0x22FFFFFF);

        if (searchQuery.isEmpty()) {
            this.textRenderer.drawWithShadow(matrices, "Поиск...", searchBoxX + 6, searchBoxY + 4, 0x55AAAAAA);
        } else {
            String cursor = (System.currentTimeMillis() / 500 % 2 == 0 && isSearchFocused) ? "|" : "";
            this.textRenderer.drawWithShadow(matrices, searchQuery + cursor, searchBoxX + 6, searchBoxY + 4, 0xFFFFFFFF);
        }

        // ==========================================
        // 3. RIGHT COLUMN: RECIPE DETAILS & CRAFT ACTION
        // ==========================================
        int detailX = gridX + gridW + 1;
        int detailW = winW - catColW - gridW - 2;

        if (selectedRecipe != null) {
            int curY = winY + 14;

            // Header Row: Big Icon + Title + Stopwatch + Quantity
            this.itemRenderer.renderInGuiWithOverrides(selectedRecipe.resultStack, detailX + 16, curY + 2);

            this.textRenderer.drawWithShadow(matrices, selectedRecipe.name, detailX + 38, curY + 6, 0xFFFFFFFF);

            // Stopwatch Time + Quantity on far right
            String timerStr = "⏱  " + String.format(Locale.US, "%.1f", selectedRecipe.craftSeconds);
            int timerW = this.textRenderer.getWidth(timerStr);
            this.textRenderer.drawWithShadow(matrices, timerStr, detailX + detailW - timerW - 32, curY + 6, 0xCCFFFFFF);
            this.textRenderer.drawWithShadow(matrices, "⚪ 1", detailX + detailW - 26, curY + 6, 0x88AAAAAA);

            // Favorite button
            String favText = selectedRecipe.isFavorite ? "★  В избранном" : "☆  Избранное";
            this.textRenderer.drawWithShadow(matrices, favText, detailX + 16, curY + 26, selectedRecipe.isFavorite ? 0xFFFFD54F : 0x77AAAAAA);

            // Multiline Description
            int descY = curY + 42;
            List<String> descLines = wrapText(selectedRecipe.description, detailW - 32);
            for (String line : descLines) {
                this.textRenderer.drawWithShadow(matrices, line, detailX + 16, descY, 0x88B0B8C4);
                descY += 11;
            }

            // ==========================================
            // REQUIREMENTS TABLE (Exact match to screenshot)
            // ==========================================
            int tableY = winY + winH - 96;
            int tableH = 14;

            // Table Header Bar (Dark slate grey)
            DrawableHelper.fill(matrices, detailX + 12, tableY, detailX + detailW - 12, tableY + tableH, 0xDD12151B);
            this.textRenderer.drawWithShadow(matrices, "СУММА", detailX + 24, tableY + 3, 0x77AAAAAA);
            this.textRenderer.drawWithShadow(matrices, "ТИП ПРЕДМЕТА", detailX + 70, tableY + 3, 0x77AAAAAA);
            this.textRenderer.drawWithShadow(matrices, "ВСЕГО", detailX + detailW - 110, tableY + 3, 0x77AAAAAA);
            this.textRenderer.drawWithShadow(matrices, "ИМЕЕТСЯ", detailX + detailW - 55, tableY + 3, 0x77AAAAAA);

            // Table Rows
            int rowY = tableY + tableH + 4;
            boolean canCraft = true;

            for (Ingredient ing : selectedRecipe.ingredients) {
                int totalRequired = ing.amount * craftQuantity;
                int playerHas = countPlayerResource(this.client.player, ing.mcItem);

                if (playerHas < totalRequired) {
                    canCraft = false;
                }

                // Amount per unit
                this.textRenderer.drawWithShadow(matrices, String.valueOf(ing.amount), detailX + 26, rowY + 3, 0xCCFFFFFF);

                // Resource Name
                this.textRenderer.drawWithShadow(matrices, ing.name, detailX + 70, rowY + 3, 0xFFE0E5EC);

                // Total Required
                this.textRenderer.drawWithShadow(matrices, String.valueOf(totalRequired), detailX + detailW - 105, rowY + 3, 0xCCFFFFFF);

                // Player Has (Green if enough, Red if insufficient)
                int hasColor = playerHas >= totalRequired ? 0xFF73C05A : 0xFFE0462C;
                this.textRenderer.drawWithShadow(matrices, String.valueOf(playerHas), detailX + detailW - 50, rowY + 3, hasColor);

                rowY += 15;
            }

            // ==========================================
            // QUANTITY STEPPER + "СОЗДАТЬ" BUTTON
            // ==========================================
            int actionY = winY + winH - 26;

            // [-] button
            int btnMinusX = detailX + 12;
            int btnSize = 18;
            DrawableHelper.fill(matrices, btnMinusX, actionY, btnMinusX + btnSize, actionY + btnSize, 0xCC2A303C);
            this.textRenderer.drawWithShadow(matrices, "-", btnMinusX + 6, actionY + 5, 0xFFFFFFFF);

            // Quantity Display Box
            int qtyBoxX = btnMinusX + btnSize + 4;
            int qtyBoxW = 32;
            DrawableHelper.fill(matrices, qtyBoxX, actionY, qtyBoxX + qtyBoxW, actionY + btnSize, 0xAA12151B);
            String qtyStr = String.valueOf(craftQuantity);
            int qtyW = this.textRenderer.getWidth(qtyStr);
            this.textRenderer.drawWithShadow(matrices, qtyStr, qtyBoxX + (qtyBoxW - qtyW) / 2, actionY + 5, 0xFFFFFFFF);

            // [+] button
            int btnPlusX = qtyBoxX + qtyBoxW + 4;
            DrawableHelper.fill(matrices, btnPlusX, actionY, btnPlusX + btnSize, actionY + btnSize, 0xCC2A303C);
            this.textRenderer.drawWithShadow(matrices, "+", btnPlusX + 6, actionY + 5, 0xFFFFFFFF);

            // [>>] (Max craftable) button
            int btnMaxX = btnPlusX + btnSize + 4;
            int btnMaxW = 22;
            DrawableHelper.fill(matrices, btnMaxX, actionY, btnMaxX + btnMaxW, actionY + btnSize, 0xCC2A303C);
            this.textRenderer.drawWithShadow(matrices, "⏭", btnMaxX + 5, actionY + 5, 0xFFFFFFFF);

            // "СОЗДАТЬ" Button on far right
            int btnCraftW = 86;
            int btnCraftH = 22;
            int btnCraftX = detailX + detailW - btnCraftW - 12;
            int btnCraftY = actionY - 2;

            int craftBtnColor = canCraft ? 0xDD2A303C : 0x551E222A;
            boolean isCraftHovered = mouseX >= btnCraftX && mouseX <= btnCraftX + btnCraftW && mouseY >= btnCraftY && mouseY <= btnCraftY + btnCraftH;
            if (canCraft && isCraftHovered) craftBtnColor = 0xFF3E4758;

            DrawableHelper.fill(matrices, btnCraftX, btnCraftY, btnCraftX + btnCraftW, btnCraftY + btnCraftH, craftBtnColor);
            DrawableHelper.fill(matrices, btnCraftX, btnCraftY, btnCraftX + btnCraftW, btnCraftY + 1, 0x33FFFFFF);

            int craftTxtW = this.textRenderer.getWidth("СОЗДАТЬ");
            int craftTxtColor = canCraft ? 0xFFFFFFFF : 0x55AAAAAA;
            this.textRenderer.drawWithShadow(matrices, "СОЗДАТЬ", btnCraftX + (btnCraftW - craftTxtW) / 2, btnCraftY + 7, craftTxtColor);
        }

        // ==========================================
        // 4. BOTTOM-LEFT: ОЧЕРЕДЬ КРАФТА
        // ==========================================
        int queueY = screenH - 52;
        this.textRenderer.drawWithShadow(matrices, "ОЧЕРЕДЬ КРАФТА", winX + 2, queueY, 0x44FFFFFF);

        // Render queue boxes if any
        if (!RustCraftingQueue.queue.isEmpty()) {
            int qx = winX + 2;
            int qBoxY = queueY + 12;
            for (int i = 0; i < RustCraftingQueue.queue.size() && i < 4; i++) {
                RustCraftingQueue.CraftTask task = RustCraftingQueue.queue.get(i);
                int qW = 28;
                int qH = 28;
                DrawableHelper.fill(matrices, qx, qBoxY, qx + qW, qBoxY + qH, 0xDD1A1E24);
                this.itemRenderer.renderInGuiWithOverrides(task.resultItem, qx + 6, qBoxY + 6);

                if (i == 0) {
                    // Current countdown timer
                    String timeStr = String.format(Locale.US, "%.1fs", task.remainingSeconds);
                    this.textRenderer.drawWithShadow(matrices, timeStr, qx + 2, qBoxY + qH - 8, 0xFFFFA726);
                }
                qx += qW + 4;
            }
        }

        // ==========================================
        // 5. BOTTOM-CENTER: 6-SLOT RUST HOTBAR
        // ==========================================
        int hotbarSlotSize = 22;
        int hotbarW = 6 * hotbarSlotSize;
        int hotbarX = (screenW - hotbarW) / 2;
        int hotbarY = screenH - 30;

        PlayerInventory inv = this.client.player.inventory;
        for (int i = 0; i < 6; i++) {
            int sx = hotbarX + i * hotbarSlotSize;
            DrawableHelper.fill(matrices, sx, hotbarY, sx + hotbarSlotSize - 1, hotbarY + hotbarSlotSize - 1, 0xAA181C22);
            DrawableHelper.fill(matrices, sx, hotbarY, sx + hotbarSlotSize - 1, hotbarY + 1, 0x22FFFFFF);

            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()) {
                this.itemRenderer.renderInGuiWithOverrides(stack, sx + 3, hotbarY + 3);
                this.itemRenderer.renderGuiItemOverlay(this.textRenderer, stack, sx + 3, hotbarY + 3);
            }
        }

        // ==========================================
        // 6. BOTTOM-RIGHT: RUST METABOLISM HUD (Match screenshot + 55, 💧 64, 🍗 66)
        // ==========================================
        int hudW = 100;
        int hudX = screenW - hudW - 14;
        int hudY = screenH - 42;

        // Health
        int hp = (int) (this.client.player.getHealth() * 5);
        this.textRenderer.drawWithShadow(matrices, "+  " + hp, hudX + 4, hudY, 0xFF73C05A);

        // Water
        this.textRenderer.drawWithShadow(matrices, "💧  230", hudX + 4, hudY + 12, 0xFF29B6F6);

        // Food
        int hunger = this.client.player.getHungerManager().getFoodLevel() * 25;
        this.textRenderer.drawWithShadow(matrices, "🍗  " + hunger, hudX + 4, hudY + 24, 0xFFFFB74D);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private int countPlayerResource(PlayerEntity player, Item target) {
        if (player == null) return 0;
        int count = 0;

        // Smart resource matching
        for (int i = 0; i < player.inventory.size(); i++) {
            ItemStack stack = player.inventory.getStack(i);
            if (stack.isEmpty()) continue;

            if (target == Items.OAK_PLANKS) {
                // Any wood logs or planks count as wood in Rust
                if (stack.getItem() == Items.OAK_PLANKS || stack.getItem() == Items.BIRCH_PLANKS ||
                    stack.getItem() == Items.SPRUCE_PLANKS || stack.getItem() == Items.OAK_LOG ||
                    stack.getItem() == Items.BIRCH_LOG || stack.getItem() == Items.SPRUCE_LOG) {
                    count += stack.getCount();
                }
            } else if (target == Items.COBBLESTONE) {
                if (stack.getItem() == Items.COBBLESTONE || stack.getItem() == Items.STONE) {
                    count += stack.getCount();
                }
            } else if (stack.getItem() == target) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void deductPlayerResource(PlayerEntity player, Item target, int needed) {
        if (player == null || player.isCreative()) return;
        int remaining = needed;

        for (int i = 0; i < player.inventory.size() && remaining > 0; i++) {
            ItemStack stack = player.inventory.getStack(i);
            if (stack.isEmpty()) continue;

            boolean match = false;
            if (target == Items.OAK_PLANKS) {
                match = (stack.getItem() == Items.OAK_PLANKS || stack.getItem() == Items.BIRCH_PLANKS ||
                         stack.getItem() == Items.SPRUCE_PLANKS || stack.getItem() == Items.OAK_LOG);
            } else if (target == Items.COBBLESTONE) {
                match = (stack.getItem() == Items.COBBLESTONE || stack.getItem() == Items.STONE);
            } else {
                match = (stack.getItem() == target);
            }

            if (match) {
                int take = Math.min(stack.getCount(), remaining);
                stack.decrement(take);
                remaining -= take;
            }
        }
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();

        for (String w : words) {
            if (this.textRenderer.getWidth(cur + " " + w) > maxWidth) {
                lines.add(cur.toString());
                cur = new StringBuilder(w);
            } else {
                if (cur.length() > 0) cur.append(" ");
                cur.append(w);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int screenW = this.width;
        int screenH = this.height;

        // Top Inventory button
        int topTabW = 120;
        int topTabH = 18;
        int topTabX = (screenW - topTabW) / 2;
        if (mouseX >= topTabX && mouseX <= topTabX + topTabW && mouseY >= 4 && mouseY <= 22) {
            this.client.openScreen(new InventoryScreen(this.client.player));
            return true;
        }

        int winX = 24;
        int winY = 24;
        int winW = screenW - 48;
        int winH = screenH - 80;

        int catColW = 92;
        int catColX = winX;
        int catColY = winY;

        // 1. Check Category Click
        int currentCatY = catColY + 8;
        int catItemH = 16;
        for (Category cat : Category.values()) {
            if (mouseX >= catColX && mouseX <= catColX + catColW && mouseY >= currentCatY && mouseY <= currentCatY + catItemH) {
                this.selectedCategory = cat;
                List<Recipe> list = getFilteredRecipes();
                if (!list.isEmpty()) {
                    this.selectedRecipe = list.get(0);
                    this.craftQuantity = 1;
                }
                this.client.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.4f, 1.0f);
                return true;
            }
            currentCatY += catItemH + 2;
        }

        // 2. Check Middle Grid Click
        int gridX = catColX + catColW + 1;
        int gridW = (int) ((winW - catColW) * 0.44f);
        int gridH = winH - 26;

        List<Recipe> recipes = getFilteredRecipes();
        int slotSize = 34;
        int slotPadding = 4;
        int cols = Math.max(3, (gridW - 16) / (slotSize + slotPadding));
        int startSlotX = gridX + 12;
        int startSlotY = winY + 12;

        for (int i = 0; i < recipes.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int sx = startSlotX + col * (slotSize + slotPadding);
            int sy = startSlotY + row * (slotSize + slotPadding);

            if (sy + slotSize > winY + gridH) break;

            if (mouseX >= sx && mouseX <= sx + slotSize && mouseY >= sy && mouseY <= sy + slotSize) {
                this.selectedRecipe = recipes.get(i);
                this.craftQuantity = 1;
                this.client.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.4f, 1.0f);
                return true;
            }
        }

        // 3. Check Search Box Click
        int searchBoxX = gridX + 12;
        int searchBoxY = winY + winH - 22;
        int searchBoxW = gridW - 24;
        int searchBoxH = 16;
        if (mouseX >= searchBoxX && mouseX <= searchBoxX + searchBoxW && mouseY >= searchBoxY && mouseY <= searchBoxY + searchBoxH) {
            this.isSearchFocused = true;
            return true;
        } else {
            this.isSearchFocused = false;
        }

        // 4. Check Right Details Click
        int detailX = gridX + gridW + 1;
        int detailW = winW - catColW - gridW - 2;

        if (selectedRecipe != null) {
            // Favorite toggle
            if (mouseX >= detailX + 16 && mouseX <= detailX + 120 && mouseY >= winY + 38 && mouseY <= winY + 54) {
                selectedRecipe.isFavorite = !selectedRecipe.isFavorite;
                this.client.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.4f, 1.2f);
                return true;
            }

            int actionY = winY + winH - 26;
            int btnMinusX = detailX + 12;
            int btnSize = 18;

            // [-] button
            if (mouseX >= btnMinusX && mouseX <= btnMinusX + btnSize && mouseY >= actionY && mouseY <= actionY + btnSize) {
                if (craftQuantity > 1) {
                    craftQuantity--;
                    this.client.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.3f, 1.0f);
                }
                return true;
            }

            // [+] button
            int qtyBoxX = btnMinusX + btnSize + 4;
            int qtyBoxW = 32;
            int btnPlusX = qtyBoxX + qtyBoxW + 4;
            if (mouseX >= btnPlusX && mouseX <= btnPlusX + btnSize && mouseY >= actionY && mouseY <= actionY + btnSize) {
                craftQuantity++;
                this.client.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.3f, 1.0f);
                return true;
            }

            // [>>] Max button
            int btnMaxX = btnPlusX + btnSize + 4;
            int btnMaxW = 22;
            if (mouseX >= btnMaxX && mouseX <= btnMaxX + btnMaxW && mouseY >= actionY && mouseY <= actionY + btnSize) {
                int maxCan = 99;
                for (Ingredient ing : selectedRecipe.ingredients) {
                    int has = countPlayerResource(this.client.player, ing.mcItem);
                    maxCan = Math.min(maxCan, has / ing.amount);
                }
                craftQuantity = Math.max(1, maxCan);
                this.client.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.4f, 1.1f);
                return true;
            }

            // "СОЗДАТЬ" button
            int btnCraftW = 86;
            int btnCraftH = 22;
            int btnCraftX = detailX + detailW - btnCraftW - 12;
            int btnCraftY = actionY - 2;

            if (mouseX >= btnCraftX && mouseX <= btnCraftX + btnCraftW && mouseY >= btnCraftY && mouseY <= btnCraftY + btnCraftH) {
                // Check materials
                boolean canCraft = true;
                for (Ingredient ing : selectedRecipe.ingredients) {
                    if (countPlayerResource(this.client.player, ing.mcItem) < ing.amount * craftQuantity) {
                        canCraft = false;
                        break;
                    }
                }

                if (canCraft || this.client.player.isCreative()) {
                    // Deduct resources
                    for (Ingredient ing : selectedRecipe.ingredients) {
                        deductPlayerResource(this.client.player, ing.mcItem, ing.amount * craftQuantity);
                    }

                    // Add to Rust Crafting Queue
                    ItemStack res = selectedRecipe.resultStack.copy();
                    res.setCount(craftQuantity);
                    RustCraftingQueue.addTask(selectedRecipe.name, res, craftQuantity, selectedRecipe.craftSeconds);

                    // Sound
                    this.client.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.6f, 1.2f);
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        if (isSearchFocused) {
            searchQuery += chr;
            return true;
        }
        return super.charTyped(chr, keyCode);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isSearchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                isSearchFocused = false;
                return true;
            }
        }

        // Q, E or ESC toggles the menu closed!
        if (keyCode == GLFW.GLFW_KEY_Q || keyCode == GLFW.GLFW_KEY_E || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
