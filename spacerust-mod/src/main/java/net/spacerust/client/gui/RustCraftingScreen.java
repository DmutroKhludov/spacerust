package net.spacerust.client.gui;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 1:1 Rust / CoilRust Crafting Screen matching gameui.crafting.prefab,
 * ui.blueprint.button.prefab, ui.blueprint.category.button.prefab,
 * and ui.blueprint.craft.row.prefab.
 */
public class RustCraftingScreen extends Screen {

    public enum Category {
        FAVORITES("ИЗБРАННОЕ", RustUiHelper.TEX_CAT_FAV),
        GENERAL("ОБЩЕЕ", RustUiHelper.TEX_CAT_COMMON),
        CONSTRUCTION("КОНСТРУКЦИИ", RustUiHelper.TEX_CAT_CONSTRUCTION),
        DEPLOYABLES("ОБЪЕКТЫ", RustUiHelper.TEX_CAT_ITEMS),
        RESOURCES("РЕСУРСЫ", RustUiHelper.TEX_CAT_COMMON),
        CLOTHING("ОДЕЖДА", RustUiHelper.TEX_CAT_COMMON),
        TOOLS("ИНСТРУМЕНТЫ", RustUiHelper.TEX_CAT_TOOLS),
        MEDICAL("МЕДИЦИНА", RustUiHelper.TEX_CAT_MEDICAL),
        WEAPONS("ОРУЖИЕ", RustUiHelper.TEX_CAT_WEAPONS),
        AMMO("БОЕПРИПАСЫ", RustUiHelper.TEX_CAT_AMMO),
        ELECTRICAL("ЭЛЕКТРИЧЕСТВО", RustUiHelper.TEX_CAT_ELECTRIC),
        OTHER("ПРОЧЕЕ", RustUiHelper.TEX_CAT_OTHER);

        public final String title;
        public final Identifier icon;

        Category(String title, Identifier icon) {
            this.title = title;
            this.icon = icon;
        }
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

        public String get3DModelName() {
            switch (this.id) {
                case "campfire": return "deploy_campfire";
                case "torch": return "held_torch";
                case "small_box": return "deploy_small_box";
                case "bed": return "deploy_sleeping_bag";
                case "build_plan":
                case "building_plan": return "held_plan";
                case "wood_hammer": return "held_hammer";
                case "wood_door": return "door_wood_0";
                case "sheet_door": return "door_metal_0";
                case "armored_door": return "door_armored_0";
                case "garage_door": return "garage_door_0";
                case "tool_cupboard": return "deploy_tool_cupboard";
                case "code_lock": return "lock_code_locked";
                case "key_lock": return "lock_key_locked";
                case "large_box": return "deploy_large_box";
                case "furnace": return "deploy_furnace";
                case "workbench_1": return "deploy_workbench1";
                case "workbench_2": return "deploy_workbench2";
                case "stone_hatchet": return "held_stone_hatchet";
                case "stone_pickaxe": return "held_stone_pickaxe";
                case "metal_hatchet": return "held_hatchet";
                case "metal_pickaxe": return "held_pickaxe";
                case "bandage": return "held_bandage";
                case "syringe": return "held_syringe";
                case "bow": return "held_hunting_bow";
                case "crossbow": return "held_crossbow";
                case "revolver": return "held_revolver";
                case "pipe_shotgun": return "held_doublebarrel";
                case "ak47": return "held_ak";
                case "thompson": return "held_thompson";
                case "sar": return "held_sar";
                case "mp5": return "held_mp5";
                case "bolt": return "held_bolt";
                case "pump_shotgun": return "held_pump_shotgun";
                case "c4": return "held_c4";
                default: return null;
            }
        }

        public float get3DModelScale() {
            switch (this.id) {
                case "campfire": return 55.0f;
                case "furnace": return 30.0f;
                case "small_box": return 50.0f;
                case "large_box": return 35.0f;
                case "tool_cupboard": return 26.0f;
                case "wood_door":
                case "sheet_door":
                case "armored_door": return 26.0f;
                case "garage_door": return 22.0f;
                case "code_lock":
                case "key_lock": return 110.0f;
                case "build_plan":
                case "building_plan": return 70.0f;
                case "wood_hammer": return 110.0f;
                case "revolver": return 130.0f;
                case "ak47":
                case "thompson":
                case "sar":
                case "mp5":
                case "bolt":
                case "pump_shotgun":
                case "pipe_shotgun": return 95.0f;
                case "stone_hatchet":
                case "stone_pickaxe":
                case "metal_hatchet":
                case "metal_pickaxe": return 85.0f;
                case "bandage":
                case "syringe": return 110.0f;
                default: return 50.0f;
            }
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

        // CONSTRUCTION
        ALL_RECIPES.add(new Recipe("build_plan", "План постройки", Category.CONSTRUCTION, Items.MAP,
                "План постройки. Используется для создания баз — взяв в руки, нажмите ПКМ для просмотра опций.", 30.0f,
                Arrays.asList(new Ingredient("Дерево", 20, Items.OAK_PLANKS))));
        ALL_RECIPES.add(new Recipe("wood_hammer", "Строительный молоток", Category.CONSTRUCTION, Items.WOODEN_AXE,
                "Инструмент для улучшения стен базы до дерева, камня, металла или МВК.", 5.0f,
                Arrays.asList(new Ingredient("Дерево", 100, Items.OAK_PLANKS))));
        ALL_RECIPES.add(new Recipe("wood_door", "Деревянная дверь", Category.CONSTRUCTION, Items.OAK_DOOR,
                "Базовая входная дверь для закрытия проемов дома.", 15.0f,
                Arrays.asList(new Ingredient("Дерево", 300, Items.OAK_PLANKS))));
        ALL_RECIPES.add(new Recipe("sheet_door", "Металлическая дверь", Category.CONSTRUCTION, Items.IRON_DOOR,
                "Прочная дверь из фрагментов металла, защищает от начальных рейдов.", 30.0f,
                Arrays.asList(new Ingredient("Фрагменты металла", 150, Items.IRON_INGOT))));
        ALL_RECIPES.add(new Recipe("armored_door", "Бронированная дверь", Category.CONSTRUCTION, Items.NETHERITE_BLOCK,
                "Тяжелая бронированная дверь из МВК. Выдерживает несколько ракет и C4.", 45.0f,
                Arrays.asList(new Ingredient("МВК", 25, Items.NETHERITE_INGOT), new Ingredient("Шестеренки", 5, Items.TRIPWIRE_HOOK))));
        ALL_RECIPES.add(new Recipe("garage_door", "Гаражная дверь", Category.CONSTRUCTION, Items.IRON_TRAPDOOR,
                "Роллетные ворота для широких проемов (Frame). Открываются вверх.", 30.0f,
                Arrays.asList(new Ingredient("Фрагменты металла", 300, Items.IRON_INGOT), new Ingredient("Шестеренки", 2, Items.TRIPWIRE_HOOK))));
        ALL_RECIPES.add(new Recipe("tool_cupboard", "Шкаф с инструментами", Category.CONSTRUCTION, Items.BOOKSHELF,
                "Главный объект базы. Защищает постройки от гниения и дает право на строительство.", 30.0f,
                Arrays.asList(new Ingredient("Дерево", 1000, Items.OAK_PLANKS))));
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
        ALL_RECIPES.add(new Recipe("thompson", "Томпсон", Category.WEAPONS, Items.IRON_SWORD,
                "Пистолет-пулемет на 20 патронов 9 мм с отличной скорострельностью.", 25.0f,
                Arrays.asList(new Ingredient("Фрагменты металла", 250, Items.IRON_INGOT), new Ingredient("Корпус SMG", 1, Items.IRON_BLOCK), new Ingredient("Пружина", 1, Items.TRIPWIRE_HOOK))));
        ALL_RECIPES.add(new Recipe("sar", "Винтовка SAR", Category.WEAPONS, Items.STONE_HOE,
                "Полуавтоматическая винтовка на 16 патронов 5.56 мм. Идеальна для средних дистанций.", 30.0f,
                Arrays.asList(new Ingredient("Фрагменты металла", 450, Items.IRON_INGOT), new Ingredient("Корпус", 1, Items.COBBLESTONE), new Ingredient("Труба", 1, Items.LEVER))));
        ALL_RECIPES.add(new Recipe("mp5", "Пистолет-пулемет MP5A4", Category.WEAPONS, Items.GOLDEN_SWORD,
                "Военный пистолет-пулемет на 30 патронов с высокой точностью.", 30.0f,
                Arrays.asList(new Ingredient("МВК", 15, Items.NETHERITE_INGOT), new Ingredient("Корпус SMG", 1, Items.IRON_BLOCK), new Ingredient("Пружины", 2, Items.TRIPWIRE_HOOK))));
        ALL_RECIPES.add(new Recipe("bolt", "Винтовка Bolt Action", Category.WEAPONS, Items.DIAMOND_HOE,
                "Мощная снайперская винтовка с колоссальным уроном в голову.", 40.0f,
                Arrays.asList(new Ingredient("МВК", 30, Items.NETHERITE_INGOT), new Ingredient("Корпус винтовки", 1, Items.OBSIDIAN), new Ingredient("Трубы", 3, Items.LEVER))));
        ALL_RECIPES.add(new Recipe("pump_shotgun", "Помповый дробовик", Category.WEAPONS, Items.STONE_SHOVEL,
                "Классический дробовик на 6 патронов с сокрушительной убойной силой в упор.", 20.0f,
                Arrays.asList(new Ingredient("Фрагменты металла", 150, Items.IRON_INGOT), new Ingredient("Трубы", 2, Items.LEVER))));
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
        RustFontRenderer.ensureLoaded();
        List<Recipe> initList = getFilteredRecipes();
        this.selectedRecipe = initList.isEmpty() ? null : initList.get(0);
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

    private List<Recipe> getFilteredRecipes() {
        List<Recipe> list = new ArrayList<>();
        String query = searchQuery.trim().toLowerCase(Locale.ROOT);

        for (Recipe r : ALL_RECIPES) {
            boolean matchesCat = (selectedCategory == Category.FAVORITES) ? r.isFavorite : (r.category == selectedCategory);
            if (matchesCat) {
                if (query.isEmpty()
                        || r.name.toLowerCase(Locale.ROOT).contains(query)
                        || r.description.toLowerCase(Locale.ROOT).contains(query)) {
                    list.add(r);
                }
            }
        }
        return list;
    }

    private int getCategoryCount(Category cat) {
        int c = 0;
        for (Recipe r : ALL_RECIPES) {
            if ((cat == Category.FAVORITES && r.isFavorite) || r.category == cat) {
                c++;
            }
        }
        return c;
    }

    private static class Layout {
        int panelX, panelY, panelW, panelH;
        int leftX, leftW, bpH;
        int catX, catW, catRowH;
        int gridX, gridW, slotSize, slotGap, cols;
        int searchX, searchY, searchW, searchH;
        int rightX, rightW;
        int favX, favY, favW, favH;
        int btnSubX, btnAddX, btnMaxX, qtyX, qtyW, actionY, btnSize, btnMaxW;
        int btnCraftX, btnCraftY, btnCraftW, btnCraftH;
    }

    private Layout computeLayout(int screenW, int screenH) {
        Layout L = new Layout();
        // Match Rust gameui.crafting.prefab centered proportions (above belt/hotbar)
        L.panelW = Math.min(screenW - 24, 620);
        L.panelX = (screenW - L.panelW) / 2;
        L.panelY = 32;
        L.panelH = screenH - 76; // leaves room for bottom hotbar/HUD bars

        L.leftX = L.panelX;
        L.leftW = Math.max(210, (int) (L.panelW * 0.45f));
        L.bpH = L.panelH - 52; // leaves bottom 52px for Queue inside Left pane

        L.catX = L.leftX;
        L.catW = 94;
        L.catRowH = Math.max(14, Math.min(18, (L.bpH - 2) / Category.values().length));

        L.gridX = L.catX + L.catW + 3;
        L.gridW = L.leftW - L.catW - 3;
        L.slotSize = 32;
        L.slotGap = 3;
        L.cols = Math.max(3, (L.gridW - 8) / (L.slotSize + L.slotGap));

        L.searchW = L.gridW;
        L.searchH = 16;
        L.searchX = L.gridX;
        L.searchY = L.panelY + L.bpH - L.searchH;

        L.rightX = L.leftX + L.leftW + 6;
        L.rightW = L.panelW - L.leftW - 6;

        // Favorite toggle inside header icon area
        L.favX = L.rightX + 8;
        L.favY = L.panelY + 42;
        L.favW = 36;
        L.favH = 12;

        // Footer controls
        L.btnSize = 18;
        L.btnMaxW = 28;
        L.qtyW = 28;
        L.actionY = L.panelY + L.panelH - 24;
        L.btnSubX = L.rightX + 8;
        L.qtyX = L.btnSubX + L.btnSize + 2;
        L.btnAddX = L.qtyX + L.qtyW + 2;
        L.btnMaxX = L.btnAddX + L.btnSize + 3;

        L.btnCraftW = 96;
        L.btnCraftH = 20;
        L.btnCraftX = L.rightX + L.rightW - L.btnCraftW - 8;
        L.btnCraftY = L.actionY - 1;

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
        Layout L = computeLayout(screenW, screenH);

        // Top Switch Button: [ ИНВЕНТАРЬ ]
        RustUiHelper.drawTopSwitchButton(matrices, this.client, mouseX, mouseY, screenW, true);

        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();
        RustFontRenderer rf = RustFontRenderer.regular();
        RustFontRenderer sf = RustFontRenderer.small();

        // ==========================================
        // 1. LEFT: CATEGORIES (UI.Blueprint.Category.Button)
        // ==========================================
        DrawableHelper.fill(matrices, L.catX, L.panelY, L.catX + L.catW, L.panelY + L.bpH, 0x0BF7EBE1);

        int catY = L.panelY;
        for (Category cat : Category.values()) {
            boolean isSel = (cat == selectedCategory);
            boolean isHov = RustUiHelper.inRect(mouseX, mouseY, L.catX, catY, L.catW, L.catRowH);

            int bg = isSel ? RustUiHelper.COLOR_CAT_ACTIVE
                    : (isHov ? 0x1AF7EBE1 : 0x0AF7EBE1);
            DrawableHelper.fill(matrices, L.catX, catY, L.catX + L.catW, catY + L.catRowH - 1, bg);

            // Real Rust category sprite icon
            int iconS = Math.min(11, L.catRowH - 4);
            RustUiHelper.drawSprite(matrices, this.client, cat.icon,
                    L.catX + 4, catY + (L.catRowH - 1 - iconS) / 2, iconS, iconS,
                    isSel ? 0xFFF7EBE1 : 0x65F7EBE1);

            int textColor = isSel ? 0xFFF7EBE1 : (isHov ? 0xEEF7EBE1 : 0xB8F7EBE1);
            bfSmall.drawCenteredY(matrices, bfSmall.trim(cat.title, L.catW - 34), L.catX + 18, catY, L.catRowH - 1, textColor);

            String cnt = String.valueOf(getCategoryCount(cat));
            float cw = sf.getWidth(cnt);
            sf.drawCenteredY(matrices, cnt, L.catX + L.catW - cw - 5, catY, L.catRowH - 1, isSel ? 0xFFF7EBE1 : 0x77F7EBE1);

            catY += L.catRowH;
        }

        // ==========================================
        // 2. MIDDLE: BLUEPRINT TILES + SEARCH
        // ==========================================
        DrawableHelper.fill(matrices, L.gridX, L.panelY, L.gridX + L.gridW, L.searchY - 2, 0x0AF7EBE1);

        List<Recipe> recipes = getFilteredRecipes();
        int gridPadX = (L.gridW - (L.cols * L.slotSize + (L.cols - 1) * L.slotGap)) / 2;
        int gridStartY = L.panelY + 4;
        Recipe hoveredRecipe = null;

        for (int i = 0; i < recipes.size(); i++) {
            Recipe r = recipes.get(i);
            int col = i % L.cols;
            int row = i / L.cols;
            int sx = L.gridX + gridPadX + col * (L.slotSize + L.slotGap);
            int sy = gridStartY + row * (L.slotSize + L.slotGap);
            if (sy + L.slotSize > L.searchY - 3) break;

            boolean isSel = (selectedRecipe == r);
            boolean isHov = RustUiHelper.inRect(mouseX, mouseY, sx, sy, L.slotSize, L.slotSize);
            if (isHov) hoveredRecipe = r;

            boolean craftable = canCraftRecipe(this.client.player, r, 1);

            // UI.Blueprint.Button visual style
            DrawableHelper.fill(matrices, sx, sy, sx + L.slotSize, sy + L.slotSize,
                    isSel ? RustUiHelper.COLOR_CAT_ACTIVE
                            : (isHov ? RustUiHelper.COLOR_SLOT_HOVER : RustUiHelper.COLOR_SLOT_OCCUPIED));
            if (craftable && !isSel) {
                DrawableHelper.fill(matrices, sx, sy, sx + L.slotSize, sy + L.slotSize, 0x227BC950);
            }

            this.itemRenderer.renderInGuiWithOverrides(r.resultStack, sx + (L.slotSize - 16) / 2, sy + (L.slotSize - 16) / 2);

            if (!craftable) {
                DrawableHelper.fill(matrices, sx, sy, sx + L.slotSize, sy + L.slotSize, 0x451D1B18);
            }

            if (r.isFavorite) {
                RustUiHelper.drawSprite(matrices, this.client, RustUiHelper.TEX_FAV_ACTIVE,
                        sx + L.slotSize - 9, sy + 2, 7, 7, 0xFFE6E000);
            }
        }

        // Bottom Search field inside Blueprints column
        DrawableHelper.fill(matrices, L.searchX, L.searchY, L.searchX + L.searchW, L.searchY + L.searchH,
                isSearchFocused ? 0x28F7EBE1 : 0x14F7EBE1);
        if (searchQuery.isEmpty()) {
            rf.drawCenteredY(matrices, isSearchFocused ? "|" : "Поиск...", L.searchX + 6, L.searchY, L.searchH, 0x66F7EBE1);
        } else {
            String cur = (System.currentTimeMillis() / 500 % 2 == 0 && isSearchFocused) ? "|" : "";
            rf.drawCenteredY(matrices, rf.trim(searchQuery, L.searchW - 20) + cur,
                    L.searchX + 6, L.searchY, L.searchH, RustUiHelper.COLOR_TEXT_CREAM);
        }
        RustUiHelper.drawSprite(matrices, this.client, RustUiHelper.TEX_SEARCH,
                L.searchX + L.searchW - 13, L.searchY + 3, 9, 9, 0x80F7EBE1);

        // ==========================================
        // 3. LEFT BOTTOM: CRAFTING QUEUE
        // ==========================================
        RustUiHelper.drawCraftingQueue(matrices, this.client, mouseX, mouseY, L.leftX, L.panelY + L.panelH, this.itemRenderer);

        // ==========================================
        // 4. RIGHT: SELECTED BLUEPRINT (gameui.crafting.prefab)
        // ==========================================
        DrawableHelper.fill(matrices, L.rightX, L.panelY, L.rightX + L.rightW, L.panelY + L.panelH, RustUiHelper.COLOR_SURFACE);

        if (selectedRecipe != null) {
            // Top Information Row
            int hdrY = L.panelY + 6;
            int iconBoxX = L.rightX + 8;
            int iconBoxW = 36;
            DrawableHelper.fill(matrices, iconBoxX, hdrY, iconBoxX + iconBoxW, hdrY + 34, 0x641D201F);
            this.itemRenderer.renderInGuiWithOverrides(selectedRecipe.resultStack, iconBoxX + 10, hdrY + 9);

            // Toggle Favourite button right below icon
            boolean favHov = RustUiHelper.inRect(mouseX, mouseY, L.favX, L.favY, L.favW, L.favH);
            DrawableHelper.fill(matrices, L.favX, L.favY, L.favX + L.favW, L.favY + L.favH,
                    favHov ? 0x881D201F : 0x641D201F);
            RustUiHelper.drawSprite(matrices, this.client,
                    selectedRecipe.isFavorite ? RustUiHelper.TEX_FAV_ACTIVE : RustUiHelper.TEX_FAV_INACTIVE,
                    L.favX + 13, L.favY + 2, 8, 8,
                    selectedRecipe.isFavorite ? 0xFFE6E000 : 0xBBF7EBE1);

            // Item Title
            RustFontRenderer.boldLarge().draw(matrices,
                    RustFontRenderer.boldLarge().trim(selectedRecipe.name.toUpperCase(), L.rightW - 110),
                    iconBoxX + iconBoxW + 10, hdrY + 4, RustUiHelper.COLOR_TEXT_CREAM);
            sf.draw(matrices, selectedRecipe.category.title,
                    iconBoxX + iconBoxW + 10, hdrY + 20, RustUiHelper.COLOR_TEXT_DIM);

            // Right Stats chips (Time & Amount)
            int chipW = 46;
            int chipX = L.rightX + L.rightW - chipW - 8;
            DrawableHelper.fill(matrices, chipX, hdrY, chipX + chipW, hdrY + 15, 0x641D201F);
            RustUiHelper.drawSprite(matrices, this.client, RustUiHelper.TEX_STOPWATCH, chipX + 4, hdrY + 3, 9, 9, RustUiHelper.COLOR_TEXT_CREAM);
            String timeStr = String.format(Locale.US, "%.0fс", selectedRecipe.craftSeconds);
            bfSmall.drawCenteredY(matrices, timeStr, chipX + 16, hdrY, 15, RustUiHelper.COLOR_TEXT_CREAM);

            DrawableHelper.fill(matrices, chipX, hdrY + 17, chipX + chipW, hdrY + 32, 0x641D201F);
            bfSmall.drawCenteredY(matrices, "x" + craftQuantity, chipX + 16, hdrY + 17, 15, RustUiHelper.COLOR_TEXT_CREAM);

            // Description & 3D Rotating Model Box
            int hdrRowY = L.actionY - 76;
            int descTop = hdrY + 38;
            int descH = Math.max(50, hdrRowY - descTop - 6);
            DrawableHelper.fill(matrices, L.rightX + 8, descTop, L.rightX + L.rightW - 8, descTop + descH, 0x0BF7EBE1);

            // 3D Rotating Model of the item (1:1 with Rust)
            String m3d = selectedRecipe.get3DModelName();
            if (m3d != null) {
                int previewCenterX = L.rightX + L.rightW / 2;
                int previewCenterY = descTop + (descH / 2) - 10;
                float rotY = (System.currentTimeMillis() % 6000L) / 6000.0f * 360.0f;
                RustUiHelper.render3DModelInGui(previewCenterX, previewCenterY, selectedRecipe.get3DModelScale(), rotY, m3d);
            }

            // Item Description text beneath 3D preview
            int lineY = descTop + descH - 16;
            for (String line : wrapText(selectedRecipe.description, L.rightW - 28, rf)) {
                rf.draw(matrices, line, L.rightX + 14, lineY, RustUiHelper.COLOR_TEXT_CREAM);
                lineY += 11;
            }

            // Ingredients Table (UI.Blueprint.Craft.Row exact widths & colors)
            int tableW = L.rightW - 16;
            int tx = L.rightX + 8;
            int colAmtW = 42;
            int colNeedW = 48;
            int colHaveW = 48;
            int colNameW = tableW - colAmtW - colNeedW - colHaveW - 3;

            drawTableRow(matrices, tx, hdrRowY, 13, colAmtW, colNameW, colNeedW, colHaveW,
                    "КОЛ-ВО", "ПРЕДМЕТ", "НУЖНО", "ЕСТЬ",
                    0xAA1D201F, 0x80F7EBE1, 0x80F7EBE1, 0x80F7EBE1);

            int rowY = hdrRowY + 14;
            boolean canCraft = true;
            for (Ingredient ing : selectedRecipe.ingredients) {
                int totalRequired = ing.amount * craftQuantity;
                int playerHas = countPlayerResource(this.client.player, ing.mcItem);
                boolean enough = playerHas >= totalRequired || this.client.player.isCreative();
                if (!enough) canCraft = false;

                drawTableRow(matrices, tx, rowY, 14, colAmtW, colNameW, colNeedW, colHaveW,
                        String.valueOf(ing.amount),
                        ing.name,
                        String.valueOf(totalRequired),
                        String.valueOf(playerHas),
                        RustUiHelper.COLOR_ROW_BG,
                        0xBCF7EBE1,
                        RustUiHelper.COLOR_GOLD_TEXT,
                        enough ? 0xBCF7EBE1 : 0xFFE55343);
                rowY += 15;
            }

            // Footer: [-] Qty [+] [MAX] and [ СОЗДАТЬ ]
            drawIconStepBtn(matrices, mouseX, mouseY, L.btnSubX, L.actionY, L.btnSize, L.btnSize, RustUiHelper.TEX_BTN_SUBTRACT);

            DrawableHelper.fill(matrices, L.qtyX, L.actionY, L.qtyX + L.qtyW, L.actionY + L.btnSize, RustUiHelper.COLOR_ROW_BG);
            String qStr = String.valueOf(craftQuantity);
            bfSmall.drawCenteredY(matrices, qStr, L.qtyX + (L.qtyW - bfSmall.getWidth(qStr)) / 2f, L.actionY, L.btnSize, RustUiHelper.COLOR_TEXT_CREAM);

            drawIconStepBtn(matrices, mouseX, mouseY, L.btnAddX, L.actionY, L.btnSize, L.btnSize, RustUiHelper.TEX_BTN_ADD);

            boolean maxHov = RustUiHelper.inRect(mouseX, mouseY, L.btnMaxX, L.actionY, L.btnMaxW, L.btnSize);
            DrawableHelper.fill(matrices, L.btnMaxX, L.actionY, L.btnMaxX + L.btnMaxW, L.actionY + L.btnSize,
                    maxHov ? 0xCC2C302E : RustUiHelper.COLOR_ROW_BG);
            sf.drawCenteredY(matrices, "MAX", L.btnMaxX + (L.btnMaxW - sf.getWidth("MAX")) / 2f, L.actionY, L.btnSize, RustUiHelper.COLOR_TEXT_CREAM);

            boolean craftHov = RustUiHelper.inRect(mouseX, mouseY, L.btnCraftX, L.btnCraftY, L.btnCraftW, L.btnCraftH);
            int craftBg = canCraft
                    ? (craftHov ? RustUiHelper.COLOR_CRAFT_BTN_HOV : RustUiHelper.COLOR_CRAFT_BTN)
                    : 0x661D201F;
            DrawableHelper.fill(matrices, L.btnCraftX, L.btnCraftY, L.btnCraftX + L.btnCraftW, L.btnCraftY + L.btnCraftH, craftBg);

            String cLabel = "СОЗДАТЬ";
            RustFontRenderer bf = RustFontRenderer.bold();
            bf.drawCenteredY(matrices, cLabel,
                    L.btnCraftX + (L.btnCraftW - bf.getWidth(cLabel)) / 2f,
                    L.btnCraftY, L.btnCraftH,
                    canCraft ? 0xFFF7EBE1 : 0x55F7EBE1);
        }

        // ==========================================
        // 5. BOTTOM: BELT / HOTBAR + METABOLISM BARS
        // ==========================================
        RustUiHelper.drawHotbar(matrices, this.client, mouseX, mouseY, screenW, screenH, this.itemRenderer, false);
        RustUiHelper.drawMetabolismBars(matrices, this.client, screenW, screenH);

        if (hoveredRecipe != null) {
            int tw = Math.round(sf.getWidth(hoveredRecipe.name)) + 10;
            int tx = Math.min(screenW - tw - 6, mouseX + 8);
            int ty = Math.max(6, mouseY - 16);
            DrawableHelper.fill(matrices, tx, ty, tx + tw, ty + 14, RustUiHelper.COLOR_SLOT_OCCUPIED);
            sf.drawCenteredY(matrices, hoveredRecipe.name, tx + 5, ty, 14, RustUiHelper.COLOR_TEXT_CREAM);
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawTableRow(MatrixStack matrices, int x, int y, int h,
                              int w1, int w2, int w3, int w4,
                              String c1, String c2, String c3, String c4,
                              int cellBg, int colText, int colName, int colHave) {
        RustFontRenderer sf = RustFontRenderer.small();
        int x1 = x;
        int x2 = x1 + w1 + 1;
        int x3 = x2 + w2 + 1;
        int x4 = x3 + w3 + 1;

        DrawableHelper.fill(matrices, x1, y, x1 + w1, y + h, cellBg);
        DrawableHelper.fill(matrices, x2, y, x2 + w2, y + h, cellBg);
        DrawableHelper.fill(matrices, x3, y, x3 + w3, y + h, cellBg);
        DrawableHelper.fill(matrices, x4, y, x4 + w4, y + h, cellBg);

        sf.drawCenteredY(matrices, c1, x1 + (w1 - sf.getWidth(c1)) / 2f, y, h, colText);
        sf.drawCenteredY(matrices, sf.trim(c2, w2 - 8), x2 + 4, y, h, colName);
        sf.drawCenteredY(matrices, c3, x3 + (w3 - sf.getWidth(c3)) / 2f, y, h, colText);
        sf.drawCenteredY(matrices, c4, x4 + (w4 - sf.getWidth(c4)) / 2f, y, h, colHave);
    }

    private void drawIconStepBtn(MatrixStack matrices, int mouseX, int mouseY, int x, int y, int w, int h, Identifier icon) {
        boolean hov = RustUiHelper.inRect(mouseX, mouseY, x, y, w, h);
        DrawableHelper.fill(matrices, x, y, x + w, y + h, hov ? 0xCC2C302E : RustUiHelper.COLOR_ROW_BG);
        RustUiHelper.drawSprite(matrices, this.client, icon, x + 4, y + 4, w - 8, h - 8, RustUiHelper.COLOR_TEXT_CREAM);
    }

    public static boolean canCraftRecipe(PlayerEntity player, Recipe r, int qty) {
        if (player == null || r == null) return false;
        if (player.isCreative()) return true;
        for (Ingredient ing : r.ingredients) {
            if (countPlayerResource(player, ing.mcItem) < ing.amount * qty) {
                return false;
            }
        }
        return true;
    }

    public static int countPlayerResource(PlayerEntity player, Item target) {
        if (player == null) return 0;
        int count = 0;
        for (int i = 0; i < player.inventory.size(); i++) {
            ItemStack stack = player.inventory.getStack(i);
            if (stack.isEmpty()) continue;

            if (target == Items.OAK_PLANKS) {
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

    public static void deductPlayerResource(PlayerEntity player, Item target, int needed) {
        if (player == null || player.isCreative()) return;
        int remaining = needed;
        for (int i = 0; i < player.inventory.size() && remaining > 0; i++) {
            ItemStack stack = player.inventory.getStack(i);
            if (stack.isEmpty()) continue;

            boolean match;
            if (target == Items.OAK_PLANKS) {
                match = (stack.getItem() == Items.OAK_PLANKS || stack.getItem() == Items.BIRCH_PLANKS ||
                         stack.getItem() == Items.SPRUCE_PLANKS || stack.getItem() == Items.OAK_LOG ||
                         stack.getItem() == Items.BIRCH_LOG || stack.getItem() == Items.SPRUCE_LOG);
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

    private List<String> wrapText(String text, float maxWidth, RustFontRenderer font) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            if (font.getWidth(cur + " " + w) > maxWidth) {
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
        Layout L = computeLayout(screenW, screenH);

        // Top Switch Button -> Inventory
        if (RustUiHelper.hitTestTopSwitchButton(mouseX, mouseY, screenW)) {
            RustUiHelper.playClick(this.client);
            this.client.openScreen(new RustInventoryScreen());
            return true;
        }

        // Queue click -> cancel task
        int qIdx = RustUiHelper.hitTestCraftingQueue(mouseX, mouseY, L.leftX, L.panelY + L.panelH);
        if (qIdx >= 0 && qIdx < RustCraftingQueue.queue.size()) {
            RustCraftingQueue.queue.remove(qIdx);
            RustUiHelper.playClick(this.client);
            return true;
        }

        // Categories clicks
        int catY = L.panelY;
        for (Category cat : Category.values()) {
            if (RustUiHelper.inRect(mouseX, mouseY, L.catX, catY, L.catW, L.catRowH)) {
                selectedCategory = cat;
                List<Recipe> list = getFilteredRecipes();
                selectedRecipe = list.isEmpty() ? null : list.get(0);
                craftQuantity = 1;
                RustUiHelper.playClick(this.client);
                return true;
            }
            catY += L.catRowH;
        }

        // Blueprints clicks
        List<Recipe> recipes = getFilteredRecipes();
        int gridPadX = (L.gridW - (L.cols * L.slotSize + (L.cols - 1) * L.slotGap)) / 2;
        int gridStartY = L.panelY + 4;
        for (int i = 0; i < recipes.size(); i++) {
            Recipe r = recipes.get(i);
            int col = i % L.cols;
            int row = i / L.cols;
            int sx = L.gridX + gridPadX + col * (L.slotSize + L.slotGap);
            int sy = gridStartY + row * (L.slotSize + L.slotGap);
            if (sy + L.slotSize > L.searchY - 3) break;

            if (RustUiHelper.inRect(mouseX, mouseY, sx, sy, L.slotSize, L.slotSize)) {
                selectedRecipe = r;
                craftQuantity = 1;
                isSearchFocused = false;
                RustUiHelper.playClick(this.client);
                return true;
            }
        }

        // Search Box
        isSearchFocused = RustUiHelper.inRect(mouseX, mouseY, L.searchX, L.searchY, L.searchW, L.searchH);
        if (isSearchFocused) return true;

        // Right Panel Actions
        if (selectedRecipe != null && this.client != null && this.client.player != null) {
            if (RustUiHelper.inRect(mouseX, mouseY, L.favX, L.favY, L.favW, L.favH)) {
                selectedRecipe.isFavorite = !selectedRecipe.isFavorite;
                RustUiHelper.playClick(this.client);
                return true;
            }
            if (RustUiHelper.inRect(mouseX, mouseY, L.btnSubX, L.actionY, L.btnSize, L.btnSize)) {
                if (craftQuantity > 1) {
                    craftQuantity--;
                    RustUiHelper.playClick(this.client);
                }
                return true;
            }
            if (RustUiHelper.inRect(mouseX, mouseY, L.btnAddX, L.actionY, L.btnSize, L.btnSize)) {
                craftQuantity++;
                RustUiHelper.playClick(this.client);
                return true;
            }
            if (RustUiHelper.inRect(mouseX, mouseY, L.btnMaxX, L.actionY, L.btnMaxW, L.btnSize)) {
                int maxCan = 99;
                for (Ingredient ing : selectedRecipe.ingredients) {
                    int has = countPlayerResource(this.client.player, ing.mcItem);
                    maxCan = Math.min(maxCan, has / ing.amount);
                }
                craftQuantity = Math.max(1, maxCan);
                RustUiHelper.playClick(this.client);
                return true;
            }
            if (RustUiHelper.inRect(mouseX, mouseY, L.btnCraftX, L.btnCraftY, L.btnCraftW, L.btnCraftH)) {
                if (canCraftRecipe(this.client.player, selectedRecipe, craftQuantity)) {
                    for (Ingredient ing : selectedRecipe.ingredients) {
                        deductPlayerResource(this.client.player, ing.mcItem, ing.amount * craftQuantity);
                    }
                    ItemStack res = selectedRecipe.resultStack.copy();
                    res.setCount(craftQuantity);
                    RustCraftingQueue.addTask(selectedRecipe.name, res, craftQuantity, selectedRecipe.craftSeconds);
                    RustUiHelper.playClick(this.client);
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        if (isSearchFocused && chr >= 32 && chr != 127) {
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

        if (keyCode == GLFW.GLFW_KEY_E) {
            this.client.openScreen(new RustInventoryScreen());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_Q || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
