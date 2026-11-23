package net.spacerust.client;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registry of all standalone SpaceRust items on client + dedicated "SpaceRust" Creative Tab.
 * Un-tagged vanilla items do not match SpaceRust definitions, keeping vanilla & SpaceRust items cleanly separated.
 */
public final class SpaceRustItems {

    public static final class ItemDef {
        public final String id;
        public final String displayName;
        public final Item carrier;
        public final int customModelData;
        public final Identifier iconTex;
        public final String viewModelKey;
        public final String gunKey;

        ItemDef(String id, String displayName, Item carrier, int customModelData,
                String iconFile, String viewModelKey, String gunKey) {
            this.id = id;
            this.displayName = displayName;
            this.carrier = carrier;
            this.customModelData = customModelData;
            this.iconTex = new Identifier("spacerust", "textures/gui/rust_items/" + iconFile);
            this.viewModelKey = viewModelKey;
            this.gunKey = gunKey;
        }
    }

    private static final Map<String, ItemDef> BY_ID = new LinkedHashMap<>();
    private static final Map<Integer, ItemDef> BY_CMD = new HashMap<>();
    private static final Map<String, ItemDef> BY_CUSTOM_NAME = new HashMap<>();

    static {
        // Starter, Tools & Building
        reg("rock", "Камень", Items.STONE_SHOVEL, 1001, "rock.png", "rock", null);
        reg("torch", "Факел", Items.HEART_OF_THE_SEA, 1002, "torch.png", "torch", null);
        reg("build_plan", "План постройки", Items.MAP, 1003, "building.planner.png", "plan", null);
        reg("wood_hammer", "Киянка", Items.WOODEN_AXE, 1004, "hammer.png", "hammer", null);
        reg("stone_hatchet", "Каменный топор", Items.STONE_AXE, 1005, "stonehatchet.png", "stoneaxe", null);
        reg("stone_pickaxe", "Каменная кирка", Items.STONE_PICKAXE, 1006, "stone.pickaxe.png", "stonepick", null);
        reg("metal_hatchet", "Металлический топор", Items.IRON_AXE, 1007, "hatchet.png", "axe", null);
        reg("metal_pickaxe", "Металлическая кирка", Items.IRON_PICKAXE, 1008, "pickaxe.png", "pickaxe", null);
        reg("salvaged_axe", "Самодельный топор", Items.DIAMOND_AXE, 1009, "axe.salvaged.png", "salvagedaxe", null);
        reg("salvaged_icepick", "Ледоруб", Items.DIAMOND_PICKAXE, 1010, "icepick.salvaged.png", "salvagedicepick", null);
        reg("jackhammer", "Отбойный молоток", Items.GOLDEN_PICKAXE, 1011, "jackhammer.png", "jackhammer", null);

        // Melee, Bows, Firearms, Explosives
        reg("wooden_spear", "Деревянное копьё", Items.STICK, 1012, "spear.wooden.png", "woodenspear", null);
        reg("stone_spear", "Каменное копьё", Items.BLAZE_ROD, 1013, "spear.stone.png", "stonespear", null);
        reg("machete", "Мачете", Items.SHEARS, 1014, "machete.png", "machete", null);
        reg("bone_knife", "Костяной нож", Items.BONE, 1015, "knife.bone.png", "boneknife", null);
        reg("bow", "Охотничий лук", Items.BOW, 1016, "bow.hunting.png", "bow", "bow");
        reg("crossbow", "Арбалет", Items.CROSSBOW, 1017, "crossbow.png", "crossbow", "crossbow");
        reg("eoka", "Самопал (Eoka)", Items.WOODEN_HOE, 1018, "pistol.eoka.png", "eoka", "eoka");
        reg("revolver", "Револьвер", Items.IRON_HOE, 1019, "pistol.revolver.png", "revolver", "revolver");
        reg("pipe_shotgun", "Двуствольный дробовик", Items.WOODEN_SHOVEL, 1020, "shotgun.double.png", "shotgun", "shotgun");
        reg("p250", "Пистолет P250", Items.STONE_HOE, 1021, "pistol.semiauto.png", "p250", "p250");
        reg("python", "Револьвер Python", Items.GOLDEN_HOE, 1022, "pistol.python.png", "python", "python");
        reg("m92", "Пистолет M92", Items.DIAMOND_HOE, 1023, "m92.png", "m92", "m92");
        reg("custom_smg", "Самодельный ПП", Items.NETHERITE_HOE, 1024, "smg.2.png", "customsmg", "customsmg");
        reg("thompson", "Томпсон", Items.DIAMOND_SWORD, 1025, "smg.thompson.png", "thompson", "thompson");
        reg("mp5", "Пистолет-пулемёт MP5", Items.IRON_SHOVEL, 1026, "smg.mp5.png", "mp5", "mp5");
        reg("sar", "Берданка (SAR)", Items.GOLDEN_SHOVEL, 1027, "rifle.semiauto.png", "sar", "sar");
        reg("pump_shotgun", "Помповый дробовик", Items.DIAMOND_SHOVEL, 1028, "shotgun.pump.png", "pump", "pump");
        reg("ak47", "Автомат AK-47", Items.NETHERITE_SWORD, 1029, "rifle.ak.png", "ak47", "ak47");
        reg("lr300", "Винтовка LR-300", Items.IRON_SWORD, 1030, "rifle.lr300.png", "lr300", "lr300");
        reg("m39", "Винтовка M39", Items.WOODEN_SWORD, 1031, "rifle.m39.png", "m39", "m39");
        reg("bolt", "Винтовка Bolt", Items.GOLDEN_SWORD, 1032, "rifle.bolt.png", "bolt", "bolt");
        reg("l96", "Снайперка L96", Items.STONE_SWORD, 1033, "rifle.l96.png", "l96", "l96");
        reg("hmlmg", "Самодельный ручной пулемёт", Items.NETHERITE_PICKAXE, 1101, "hmlmg.png", "hmlmg", "hmlmg");
        reg("rocket_launcher", "Ракетница", Items.NETHERITE_SHOVEL, 1102, "rocket.launcher.png", null, "rocket_launcher");
        reg("ammo_rocket", "Ракета", Items.FIREWORK_ROCKET, 1103, "ammo.rocket.basic.png", null, null);
        reg("satchel", "Бобовый заряд (Satchel)", Items.TNT, 1034, "explosive.satchel.png", "satchel", null);
        reg("f1_grenade", "Граната F1", Items.FIRE_CHARGE, 1035, "grenade.f1.png", "f1grenade", null);
        reg("c4", "Взрывчатка C4", Items.REDSTONE_BLOCK, 1036, "explosive.timed.png", null, null);

        // Medical & Ammo
        reg("bandage", "Бинт", Items.PAPER, 1037, "bandage.png", "bandage", null);
        reg("syringe", "Медицинский шприц", Items.POTION, 1038, "syringe.medical.png", "syringe", null);
        reg("wooden_arrows", "Деревянные стрелы", Items.ARROW, 1039, "arrow.wooden.png", null, null);
        reg("ammo_9mm", "Патроны 9мм", Items.GOLD_NUGGET, 1040, "ammo.pistol.png", null, null);
        reg("ammo_556", "Патроны 5.56мм", Items.IRON_NUGGET, 1041, "ammo.rifle.png", null, null);
        reg("ammo_12g", "Дробь 12 калибр", Items.BRICK, 1042, "ammo.shotgun.png", null, null);

        // Clothing & Armor
        reg("burlap_headwrap", "Повязка на голову", Items.LEATHER_HELMET, 1043, "burlap.headwrap.png", null, null);
        reg("burlap_shirt", "Рубашка из мешковины", Items.LEATHER_CHESTPLATE, 1044, "burlap.shirt.png", null, null);
        reg("burlap_pants", "Штаны из мешковины", Items.LEATHER_LEGGINGS, 1045, "burlap.trousers.png", null, null);
        reg("burlap_shoes", "Обувь из мешковины", Items.LEATHER_BOOTS, 1046, "burlap.shoes.png", null, null);
        reg("burlap_gloves", "Перчатки из мешковины", Items.RABBIT_HIDE, 1047, "burlap.gloves.png", null, null);
        reg("wood_helmet", "Деревянный шлем", Items.CHAINMAIL_HELMET, 1048, "wood.armor.helmet.png", null, null);
        reg("wood_armor", "Деревянная кираса", Items.CHAINMAIL_CHESTPLATE, 1049, "wood.armor.jacket.png", null, null);
        reg("wood_pants", "Деревянные поножи", Items.CHAINMAIL_LEGGINGS, 1050, "wood.armor.pants.png", null, null);
        reg("hoodie", "Толстовка (Худи)", Items.NETHERITE_CHESTPLATE, 1051, "hoodie.png", null, null);
        reg("pants", "Штаны", Items.IRON_LEGGINGS, 1052, "pants.png", null, null);
        reg("boots", "Кожаные ботинки", Items.IRON_BOOTS, 1053, "shoes.boots.png", null, null);
        reg("coffeecan_helmet", "Каска из кофейной банки", Items.GOLDEN_HELMET, 1054, "coffeecan.helmet.png", null, null);
        reg("roadsign_jacket", "Дорожная броня", Items.GOLDEN_CHESTPLATE, 1055, "roadsign.jacket.png", null, null);
        reg("roadsign_kilt", "Дорожный килт", Items.GOLDEN_LEGGINGS, 1056, "roadsign.kilt.png", null, null);
        reg("roadsign_gloves", "Дорожные перчатки", Items.SCUTE, 1057, "roadsign.gloves.png", null, null);
        reg("tactical_gloves", "Тактические перчатки", Items.PHANTOM_MEMBRANE, 1058, "tactical.gloves.png", null, null);
        reg("hazmat", "Костюм химзащиты (Хазмат)", Items.DIAMOND_CHESTPLATE, 1059, "hazmatsuit.png", null, null);
        reg("metal_facemask", "Металлическая маска", Items.IRON_HELMET, 1060, "metal.facemask.png", null, null);
        reg("metal_chest", "Металлическая кираса", Items.IRON_CHESTPLATE, 1061, "metal.plate.torso.png", null, null);

        // Deployables & Base
        reg("campfire", "Костер", Items.CAMPFIRE, 1062, "campfire.png", null, null);
        reg("small_box", "Деревянный ящик", Items.CHEST, 1063, "box.wooden.png", null, null);
        reg("bed", "Спальный мешок", Items.WHITE_BED, 1064, "sleepingbag.png", null, null);
        reg("wood_door", "Деревянная дверь", Items.OAK_DOOR, 1065, "door.hinged.wood.png", null, null);
        reg("sheet_door", "Металлическая дверь", Items.IRON_DOOR, 1066, "door.hinged.metal.png", null, null);
        reg("code_lock", "Кодовый замок", Items.LEVER, 1067, "lock.code.png", null, null);
        reg("key_lock", "Дверной замок", Items.TRIPWIRE_HOOK, 1068, "lock.key.png", null, null);
        reg("wood_barricade", "Деревянная баррикада", Items.OAK_FENCE, 1069, "barricade.wood.png", null, null);
        reg("wood_gate", "Деревянные ворота", Items.OAK_FENCE_GATE, 1070, "gates.external.high.wood.png", null, null);
        reg("metal_bars", "Металлическая решетка", Items.IRON_BARS, 1071, "wall.window.bars.metal.png", null, null);
        reg("ladder", "Деревянная лестница", Items.LADDER, 1072, "ladder.wooden.wall.png", null, null);
        reg("window_shutter", "Деревянные ставни", Items.OAK_TRAPDOOR, 1073, "shutter.wood.a.png", null, null);
        reg("furnace", "Печь", Items.FURNACE, 1074, "furnace.png", null, null);
        reg("large_box", "Большой сундук", Items.BARREL, 1075, "box.wooden.large.png", null, null);
        reg("workbench_1", "Верстак 1 уровня", Items.CRAFTING_TABLE, 1076, "workbench1.png", null, null);
        reg("workbench_2", "Верстак 2 уровня", Items.SMITHING_TABLE, 1077, "workbench2.png", null, null);
        reg("solar_panel", "Солнечная панель", Items.DAYLIGHT_DETECTOR, 1078, "electric.solarpanel.large.png", null, null);
        reg("guitar", "Акустическая гитара", Items.NOTE_BLOCK, 1079, "fun.guitar.png", null, null);

        // Resources, Components & Animal Drops
        reg("wood", "Дерево", Items.OAK_PLANKS, 1080, "wood.png", null, null);
        reg("stones", "Камень", Items.COBBLESTONE, 1081, "stones.png", null, null);
        reg("metal_fragments", "Фрагменты металла", Items.IRON_INGOT, 1082, "metal.fragments.png", null, null);
        reg("hqm", "МВК", Items.NETHERITE_INGOT, 1083, "metal.refined.png", null, null);
        reg("sulfur", "Сера", Items.GLOWSTONE_DUST, 1084, "sulfur.png", null, null);
        reg("charcoal", "Уголь", Items.CHARCOAL, 1085, "charcoal.png", null, null);
        reg("coal", "Топливо", Items.COAL, 1086, "coal.png", null, null);
        reg("gunpowder", "Порох", Items.GUNPOWDER, 1087, "gunpowder.png", null, null);
        reg("low_grade_fuel", "Топливо низкого качества", Items.BLAZE_POWDER, 1088, "lowgradefuel.png", null, null);
        reg("cloth", "Ткань", Items.STRING, 1089, "cloth.png", null, null);
        reg("leather", "Кожа", Items.LEATHER, 1090, "leather.png", null, null);
        reg("scrap", "Скрап", Items.EMERALD, 1091, "scrap.png", null, null);
        reg("metal_pipe", "Труба", Items.FLINT, 1092, "metalpipe.png", null, null);
        reg("gears", "Шестерня", Items.FEATHER, 1093, "gears.png", null, null);
        reg("spring", "Пружина", Items.TRIPWIRE_HOOK, 1094, "metalspring.png", null, null);
        reg("tech_trash", "Микросхема", Items.REDSTONE, 1095, "techparts.png", null, null);
        reg("rifle_body", "Корпус винтовки", Items.OBSIDIAN, 1096, "riflebody.png", null, null);
        reg("animal_fat", "Животный жир", Items.PORKCHOP, 1097, "fat.animal.png", null, null);
        reg("raw_meat", "Сырое мясо", Items.BEEF, 1098, "can.beans.png", null, null);
        reg("cooked_meat", "Жареное мясо", Items.COOKED_BEEF, 1099, "can.tuna.png", null, null);
        reg("apple", "Яблоко", Items.APPLE, 1100, "apple.png", null, null);
    }

    public static final ItemGroup SPACERUST_GROUP = FabricItemGroupBuilder
            .create(new Identifier("spacerust", "items"))
            .icon(() -> createStack("ak47", 1))
            .appendItems(stacks -> {
                for (ItemDef def : BY_ID.values()) {
                    stacks.add(createStack(def.id, 1));
                }
            })
            .build();

    private SpaceRustItems() {}

    public static void register() {
        // Ensures static ItemGroup initialization during ClientModInitializer
    }

    private static void reg(String id, String displayName, Item carrier, int cmd,
                            String iconFile, String viewModelKey, String gunKey) {
        ItemDef def = new ItemDef(id, displayName, carrier, cmd, iconFile, viewModelKey, gunKey);
        BY_ID.put(id, def);
        BY_CMD.put(cmd, def);
        BY_CUSTOM_NAME.put(displayName.toLowerCase(Locale.ROOT), def);
    }

    public static ItemStack createStack(String id, int count) {
        ItemDef def = BY_ID.get(id);
        if (def == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(def.carrier, Math.max(1, Math.min(64, count)));
        NbtCompound tag = stack.getOrCreateTag();
        tag.putString("SpaceRustId", def.id);
        tag.putInt("CustomModelData", def.customModelData);
        stack.setCustomName(new LiteralText("§f" + def.displayName));
        return stack;
    }

    public static ItemDef getDefById(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    public static ItemDef getDef(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        NbtCompound tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("SpaceRustId")) {
                ItemDef d = BY_ID.get(tag.getString("SpaceRustId"));
                if (d != null) return d;
            }
            if (tag.contains("CustomModelData")) {
                ItemDef d = BY_CMD.get(tag.getInt("CustomModelData"));
                if (d != null) return d;
            }
        }
        // Only match custom-named legacy SpaceRust stacks; leave unmodified vanilla items separate!
        if (stack.hasCustomName()) {
            String raw = stack.getName().getString().replace("§f", "").trim().toLowerCase(Locale.ROOT);
            if ("камень".equals(raw)) {
                return BY_ID.get(stack.getItem() == Items.STONE_SHOVEL ? "rock" : "stones");
            }
            ItemDef exact = BY_CUSTOM_NAME.get(raw);
            if (exact != null) return exact;
        }
        return null;
    }

    public static String getId(ItemStack stack) {
        ItemDef def = getDef(stack);
        return def != null ? def.id : null;
    }
}
