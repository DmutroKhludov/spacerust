package net.spacerust.plugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Canonical registry of all standalone SpaceRust items on Purpur 1.16.5.
 * Each item owns a unique id, Russian display name, carrier Material, CustomModelData (1001..1100),
 * and PersistentDataContainer tag ("spacerust:id") so SpaceRust items stay distinct from vanilla items.
 */
public final class SpaceRustItemRegistry {

    public static final class ItemDef {
        public final String id;
        public final String name;
        public final Material material;
        public final int customModelData;

        ItemDef(String id, String name, Material material, int customModelData) {
            this.id = id;
            this.name = name;
            this.material = material;
            this.customModelData = customModelData;
        }
    }

    private static final Map<String, ItemDef> BY_ID = new LinkedHashMap<>();
    private static final Map<Integer, ItemDef> BY_CMD = new HashMap<>();
    private static final Map<String, ItemDef> BY_MAT_AND_NAME = new HashMap<>();
    private static final Map<Material, ItemDef> FIRST_BY_MAT = new HashMap<>();
    public static NamespacedKey SPACERUST_ID_KEY;

    static {
        // Starter & Tools & Building
        reg("rock", "Камень", Material.STONE_SHOVEL, 1001);
        reg("torch", "Факел", Material.HEART_OF_THE_SEA, 1002);
        reg("build_plan", "План постройки", Material.MAP, 1003);
        reg("wood_hammer", "Киянка", Material.WOODEN_AXE, 1004);
        reg("stone_hatchet", "Каменный топор", Material.STONE_AXE, 1005);
        reg("stone_pickaxe", "Каменная кирка", Material.STONE_PICKAXE, 1006);
        reg("metal_hatchet", "Металлический топор", Material.IRON_AXE, 1007);
        reg("metal_pickaxe", "Металлическая кирка", Material.IRON_PICKAXE, 1008);
        reg("salvaged_axe", "Самодельный топор", Material.DIAMOND_AXE, 1009);
        reg("salvaged_icepick", "Ледоруб", Material.DIAMOND_PICKAXE, 1010);
        reg("jackhammer", "Отбойный молоток", Material.GOLDEN_PICKAXE, 1011);

        // Melee, Bows, Firearms, Explosives
        reg("wooden_spear", "Деревянное копьё", Material.STICK, 1012);
        reg("stone_spear", "Каменное копьё", Material.BLAZE_ROD, 1013);
        reg("machete", "Мачете", Material.SHEARS, 1014);
        reg("bone_knife", "Костяной нож", Material.BONE, 1015);
        reg("bow", "Охотничий лук", Material.BOW, 1016);
        reg("crossbow", "Арбалет", Material.CROSSBOW, 1017);
        reg("eoka", "Самопал (Eoka)", Material.WOODEN_HOE, 1018);
        reg("revolver", "Револьвер", Material.IRON_HOE, 1019);
        reg("pipe_shotgun", "Двуствольный дробовик", Material.WOODEN_SHOVEL, 1020);
        reg("p250", "Пистолет P250", Material.STONE_HOE, 1021);
        reg("python", "Револьвер Python", Material.GOLDEN_HOE, 1022);
        reg("m92", "Пистолет M92", Material.DIAMOND_HOE, 1023);
        reg("custom_smg", "Самодельный ПП", Material.NETHERITE_HOE, 1024);
        reg("thompson", "Томпсон", Material.DIAMOND_SWORD, 1025);
        reg("mp5", "Пистолет-пулемёт MP5", Material.IRON_SHOVEL, 1026);
        reg("sar", "Берданка (SAR)", Material.GOLDEN_SHOVEL, 1027);
        reg("pump_shotgun", "Помповый дробовик", Material.DIAMOND_SHOVEL, 1028);
        reg("ak47", "Автомат AK-47", Material.NETHERITE_SWORD, 1029);
        reg("lr300", "Винтовка LR-300", Material.IRON_SWORD, 1030);
        reg("m39", "Винтовка M39", Material.WOODEN_SWORD, 1031);
        reg("bolt", "Винтовка Bolt", Material.GOLDEN_SWORD, 1032);
        reg("l96", "Снайперка L96", Material.STONE_SWORD, 1033);
        reg("hmlmg", "Самодельный ручной пулемёт", Material.NETHERITE_PICKAXE, 1101);
        reg("rocket_launcher", "Ракетница", Material.NETHERITE_SHOVEL, 1102);
        reg("ammo_rocket", "Ракета", Material.FIREWORK_ROCKET, 1103);
        reg("satchel", "Бобовый заряд (Satchel)", Material.TNT, 1034);
        reg("f1_grenade", "Граната F1", Material.FIRE_CHARGE, 1035);
        reg("c4", "Взрывчатка C4", Material.REDSTONE_BLOCK, 1036);

        // Medical & Ammo
        reg("bandage", "Бинт", Material.PAPER, 1037);
        reg("syringe", "Медицинский шприц", Material.POTION, 1038);
        reg("wooden_arrows", "Деревянные стрелы", Material.ARROW, 1039);
        reg("ammo_9mm", "Патроны 9мм", Material.GOLD_NUGGET, 1040);
        reg("ammo_556", "Патроны 5.56мм", Material.IRON_NUGGET, 1041);
        reg("ammo_12g", "Дробь 12 калибр", Material.BRICK, 1042);

        // Clothing & Armor
        reg("burlap_headwrap", "Повязка на голову", Material.LEATHER_HELMET, 1043);
        reg("burlap_shirt", "Рубашка из мешковины", Material.LEATHER_CHESTPLATE, 1044);
        reg("burlap_pants", "Штаны из мешковины", Material.LEATHER_LEGGINGS, 1045);
        reg("burlap_shoes", "Обувь из мешковины", Material.LEATHER_BOOTS, 1046);
        reg("burlap_gloves", "Перчатки из мешковины", Material.RABBIT_HIDE, 1047);
        reg("wood_helmet", "Деревянный шлем", Material.CHAINMAIL_HELMET, 1048);
        reg("wood_armor", "Деревянная кираса", Material.CHAINMAIL_CHESTPLATE, 1049);
        reg("wood_pants", "Деревянные поножи", Material.CHAINMAIL_LEGGINGS, 1050);
        reg("hoodie", "Толстовка (Худи)", Material.NETHERITE_CHESTPLATE, 1051);
        reg("pants", "Штаны", Material.IRON_LEGGINGS, 1052);
        reg("boots", "Кожаные ботинки", Material.IRON_BOOTS, 1053);
        reg("coffeecan_helmet", "Каска из кофейной банки", Material.GOLDEN_HELMET, 1054);
        reg("roadsign_jacket", "Дорожная броня", Material.GOLDEN_CHESTPLATE, 1055);
        reg("roadsign_kilt", "Дорожный килт", Material.GOLDEN_LEGGINGS, 1056);
        reg("roadsign_gloves", "Дорожные перчатки", Material.SCUTE, 1057);
        reg("tactical_gloves", "Тактические перчатки", Material.PHANTOM_MEMBRANE, 1058);
        reg("hazmat", "Костюм химзащиты (Хазмат)", Material.DIAMOND_CHESTPLATE, 1059);
        reg("metal_facemask", "Металлическая маска", Material.IRON_HELMET, 1060);
        reg("metal_chest", "Металлическая кираса", Material.IRON_CHESTPLATE, 1061);

        // Deployables & Base
        reg("campfire", "Костер", Material.CAMPFIRE, 1062);
        reg("small_box", "Деревянный ящик", Material.CHEST, 1063);
        reg("bed", "Спальный мешок", Material.WHITE_BED, 1064);
        reg("wood_door", "Деревянная дверь", Material.OAK_DOOR, 1065);
        reg("sheet_door", "Металлическая дверь", Material.IRON_DOOR, 1066);
        reg("code_lock", "Кодовый замок", Material.LEVER, 1067);
        reg("key_lock", "Дверной замок", Material.TRIPWIRE_HOOK, 1068);
        reg("wood_barricade", "Деревянная баррикада", Material.OAK_FENCE, 1069);
        reg("wood_gate", "Деревянные ворота", Material.OAK_FENCE_GATE, 1070);
        reg("metal_bars", "Металлическая решетка", Material.IRON_BARS, 1071);
        reg("ladder", "Деревянная лестница", Material.LADDER, 1072);
        reg("window_shutter", "Деревянные ставни", Material.OAK_TRAPDOOR, 1073);
        reg("furnace", "Печь", Material.FURNACE, 1074);
        reg("large_box", "Большой сундук", Material.BARREL, 1075);
        reg("workbench_1", "Верстак 1 уровня", Material.CRAFTING_TABLE, 1076);
        reg("workbench_2", "Верстак 2 уровня", Material.SMITHING_TABLE, 1077);
        reg("solar_panel", "Солнечная панель", Material.DAYLIGHT_DETECTOR, 1078);
        reg("guitar", "Акустическая гитара", Material.NOTE_BLOCK, 1079);

        // Resources, Components & Animal Hunting Drops
        reg("wood", "Дерево", Material.OAK_PLANKS, 1080);
        reg("stones", "Камень", Material.COBBLESTONE, 1081);
        reg("metal_fragments", "Фрагменты металла", Material.IRON_INGOT, 1082);
        reg("hqm", "МВК", Material.NETHERITE_INGOT, 1083);
        reg("sulfur", "Сера", Material.GLOWSTONE_DUST, 1084);
        reg("charcoal", "Уголь", Material.CHARCOAL, 1085);
        reg("coal", "Топливо", Material.COAL, 1086);
        reg("gunpowder", "Порох", Material.GUNPOWDER, 1087);
        reg("low_grade_fuel", "Топливо низкого качества", Material.BLAZE_POWDER, 1088);
        reg("cloth", "Ткань", Material.STRING, 1089);
        reg("leather", "Кожа", Material.LEATHER, 1090);
        reg("scrap", "Скрап", Material.EMERALD, 1091);
        reg("metal_pipe", "Труба", Material.FLINT, 1092);
        reg("gears", "Шестерня", Material.FEATHER, 1093);
        reg("spring", "Пружина", Material.TRIPWIRE_HOOK, 1094);
        reg("tech_trash", "Микросхема", Material.REDSTONE, 1095);
        reg("rifle_body", "Корпус винтовки", Material.OBSIDIAN, 1096);
        reg("animal_fat", "Животный жир", Material.PORKCHOP, 1097);
        reg("raw_meat", "Сырое мясо", Material.BEEF, 1098);
        reg("cooked_meat", "Жареное мясо", Material.COOKED_BEEF, 1099);
        reg("apple", "Яблоко", Material.APPLE, 1100);
    }

    private SpaceRustItemRegistry() {}

    private static void reg(String id, String name, Material mat, int cmd) {
        ItemDef def = new ItemDef(id, name, mat, cmd);
        BY_ID.put(id, def);
        BY_CMD.put(cmd, def);
        BY_MAT_AND_NAME.put(mat.name() + "|" + name.toLowerCase(Locale.ROOT), def);
        FIRST_BY_MAT.putIfAbsent(mat, def);
    }

    public static void init(SpaceRustPlugin plugin) {
        SPACERUST_ID_KEY = new NamespacedKey(plugin, "id");
    }

    public static ItemDef get(String id) {
        return id == null ? null : BY_ID.get(id.toLowerCase(Locale.ROOT));
    }

    public static Collection<ItemDef> all() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    public static ItemDef find(Material m, String rawName) {
        if (m == null) return null;
        String clean = rawName == null ? "" : rawName.replace("§f", "").trim().toLowerCase(Locale.ROOT);
        ItemDef exact = BY_MAT_AND_NAME.get(m.name() + "|" + clean);
        if (exact != null) return exact;
        if (m == Material.TORCH && "факел".equals(clean)) return BY_ID.get("torch");
        if (m == Material.STONE) return BY_ID.get("stones");
        if (m == Material.OAK_LOG || m == Material.BIRCH_PLANKS || m == Material.BIRCH_LOG
                || m == Material.SPRUCE_PLANKS || m == Material.SPRUCE_LOG) {
            return BY_ID.get("wood");
        }
        return FIRST_BY_MAT.get(m);
    }

    public static String getId(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return null;
        if (stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                if (SPACERUST_ID_KEY != null) {
                    String tagId = meta.getPersistentDataContainer().get(SPACERUST_ID_KEY, PersistentDataType.STRING);
                    if (tagId != null && !tagId.isEmpty()) return tagId;
                }
                if (meta.hasCustomModelData()) {
                    ItemDef d = BY_CMD.get(meta.getCustomModelData());
                    if (d != null) return d.id;
                }
                if (meta.hasDisplayName()) {
                    ItemDef d = find(stack.getType(), meta.getDisplayName());
                    if (d != null) return d.id;
                }
            }
        }
        return null;
    }

    public static ItemStack stamp(ItemStack stack, ItemDef def) {
        if (stack == null || def == null) return stack;
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f" + def.name);
            meta.setCustomModelData(def.customModelData);
            if (SPACERUST_ID_KEY != null) {
                meta.getPersistentDataContainer().set(SPACERUST_ID_KEY, PersistentDataType.STRING, def.id);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
