package net.spacerust.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps SpaceRust recipes, display names, and vanilla fallback items to
 * genuine 2D Rust PNG item sprites extracted into textures/gui/rust_items/.
 */
public final class RustItemIcons {

    private static final Map<String, Identifier> BY_ID = new HashMap<>();
    private static final Map<Item, Identifier> BY_ITEM = new HashMap<>();

    private static Identifier tex(String file) {
        return new Identifier("spacerust", "textures/gui/rust_items/" + file);
    }

    static {
        // Recipes
        BY_ID.put("campfire",       tex("campfire.png"));
        BY_ID.put("torch",          tex("torch.png"));
        BY_ID.put("small_box",      tex("box.wooden.png"));
        BY_ID.put("bed",            tex("sleepingbag.png"));
        BY_ID.put("build_plan",     tex("building.planner.png"));
        BY_ID.put("wood_hammer",    tex("hammer.png"));
        BY_ID.put("wood_door",      tex("door.hinged.wood.png"));
        BY_ID.put("sheet_door",     tex("door.hinged.metal.png"));
        BY_ID.put("code_lock",      tex("lock.code.png"));
        BY_ID.put("key_lock",       tex("lock.key.png"));
        BY_ID.put("wood_barricade", tex("barricade.wood.png"));
        BY_ID.put("wood_gate",      tex("gates.external.high.wood.png"));
        BY_ID.put("metal_bars",     tex("wall.window.bars.metal.png"));
        BY_ID.put("ladder",         tex("ladder.wooden.wall.png"));
        BY_ID.put("window_shutter", tex("shutter.wood.a.png"));
        BY_ID.put("furnace",        tex("furnace.png"));
        BY_ID.put("large_box",      tex("box.wooden.large.png"));
        BY_ID.put("workbench_1",    tex("workbench1.png"));
        BY_ID.put("workbench_2",    tex("workbench2.png"));
        BY_ID.put("gunpowder",      tex("gunpowder.png"));
        BY_ID.put("low_grade_fuel", tex("lowgradefuel.png"));
        BY_ID.put("burlap_shirt",   tex("burlap.shirt.png"));
        BY_ID.put("burlap_pants",   tex("burlap.trousers.png"));
        BY_ID.put("burlap_headwrap",tex("burlap.headwrap.png"));
        BY_ID.put("burlap_shoes",   tex("burlap.shoes.png"));
        BY_ID.put("burlap_gloves",  tex("burlap.gloves.png"));
        BY_ID.put("wood_helmet",    tex("wood.armor.helmet.png"));
        BY_ID.put("wood_armor",     tex("wood.armor.jacket.png"));
        BY_ID.put("wood_pants",     tex("wood.armor.pants.png"));
        BY_ID.put("coffeecan_helmet", tex("coffeecan.helmet.png"));
        BY_ID.put("roadsign_jacket",  tex("roadsign.jacket.png"));
        BY_ID.put("roadsign_kilt",    tex("roadsign.kilt.png"));
        BY_ID.put("roadsign_gloves",  tex("roadsign.gloves.png"));
        BY_ID.put("hoodie",           tex("hoodie.png"));
        BY_ID.put("pants",            tex("pants.png"));
        BY_ID.put("boots",            tex("shoes.boots.png"));
        BY_ID.put("tactical_gloves",  tex("tactical.gloves.png"));
        BY_ID.put("hazmat",           tex("hazmatsuit.png"));
        BY_ID.put("metal_facemask",   tex("metal.facemask.png"));
        BY_ID.put("metal_chest",    tex("metal.plate.torso.png"));
        BY_ID.put("stone_hatchet",  tex("stonehatchet.png"));
        BY_ID.put("stone_pickaxe",  tex("stone.pickaxe.png"));
        BY_ID.put("metal_hatchet",  tex("hatchet.png"));
        BY_ID.put("metal_pickaxe",  tex("pickaxe.png"));
        BY_ID.put("salvaged_icepick", tex("icepick.salvaged.png"));
        BY_ID.put("salvaged_axe",     tex("axe.salvaged.png"));
        BY_ID.put("jackhammer",       tex("jackhammer.png"));
        BY_ID.put("wooden_spear",     tex("spear.wooden.png"));
        BY_ID.put("stone_spear",      tex("spear.stone.png"));
        BY_ID.put("machete",          tex("machete.png"));
        BY_ID.put("bone_knife",       tex("knife.bone.png"));
        BY_ID.put("bandage",        tex("bandage.png"));
        BY_ID.put("syringe",        tex("syringe.medical.png"));
        BY_ID.put("bow",            tex("bow.hunting.png"));
        BY_ID.put("crossbow",       tex("crossbow.png"));
        BY_ID.put("eoka",           tex("pistol.eoka.png"));
        BY_ID.put("revolver",       tex("pistol.revolver.png"));
        BY_ID.put("p250",           tex("pistol.semiauto.png"));
        BY_ID.put("python",         tex("pistol.python.png"));
        BY_ID.put("m92",            tex("m92.png"));
        BY_ID.put("pipe_shotgun",   tex("shotgun.double.png"));
        BY_ID.put("pump_shotgun",   tex("shotgun.pump.png"));
        BY_ID.put("custom_smg",     tex("smg.2.png"));
        BY_ID.put("thompson",       tex("smg.thompson.png"));
        BY_ID.put("mp5",            tex("smg.mp5.png"));
        BY_ID.put("sar",            tex("rifle.semiauto.png"));
        BY_ID.put("ak47",           tex("rifle.ak.png"));
        BY_ID.put("lr300",          tex("rifle.lr300.png"));
        BY_ID.put("m39",            tex("rifle.m39.png"));
        BY_ID.put("bolt",           tex("rifle.bolt.png"));
        BY_ID.put("l96",            tex("rifle.l96.png"));
        BY_ID.put("satchel",        tex("explosive.satchel.png"));
        BY_ID.put("f1_grenade",     tex("grenade.f1.png"));
        BY_ID.put("c4",             tex("explosive.timed.png"));
        BY_ID.put("wooden_arrows",  tex("arrow.wooden.png"));
        BY_ID.put("ammo_9mm",       tex("ammo.pistol.png"));
        BY_ID.put("ammo_556",       tex("ammo.rifle.png"));
        BY_ID.put("ammo_12g",       tex("ammo.shotgun.png"));
        BY_ID.put("solar_panel",    tex("electric.solarpanel.large.png"));
        BY_ID.put("guitar",         tex("fun.guitar.png"));

        // Direct Minecraft item equivalents in SpaceRust
        BY_ITEM.put(Items.CAMPFIRE,           tex("campfire.png"));
        BY_ITEM.put(Items.TORCH,              tex("torch.png"));
        BY_ITEM.put(Items.CHEST,              tex("box.wooden.png"));
        BY_ITEM.put(Items.BARREL,             tex("box.wooden.large.png"));
        BY_ITEM.put(Items.WHITE_BED,          tex("sleepingbag.png"));
        BY_ITEM.put(Items.MAP,                tex("building.planner.png"));
        BY_ITEM.put(Items.WOODEN_AXE,         tex("hammer.png"));
        BY_ITEM.put(Items.OAK_DOOR,           tex("door.hinged.wood.png"));
        BY_ITEM.put(Items.IRON_DOOR,          tex("door.hinged.metal.png"));
        BY_ITEM.put(Items.LEVER,              tex("lock.code.png"));
        BY_ITEM.put(Items.TRIPWIRE_HOOK,      tex("lock.key.png"));
        BY_ITEM.put(Items.OAK_FENCE,          tex("barricade.wood.png"));
        BY_ITEM.put(Items.OAK_FENCE_GATE,     tex("gates.external.high.wood.png"));
        BY_ITEM.put(Items.IRON_BARS,          tex("wall.window.bars.metal.png"));
        BY_ITEM.put(Items.LADDER,             tex("ladder.wooden.wall.png"));
        BY_ITEM.put(Items.OAK_TRAPDOOR,       tex("shutter.wood.a.png"));
        BY_ITEM.put(Items.FURNACE,            tex("furnace.png"));
        BY_ITEM.put(Items.CRAFTING_TABLE,     tex("workbench1.png"));
        BY_ITEM.put(Items.SMITHING_TABLE,     tex("workbench2.png"));
        BY_ITEM.put(Items.GUNPOWDER,          tex("gunpowder.png"));
        BY_ITEM.put(Items.BLAZE_POWDER,       tex("lowgradefuel.png"));
        BY_ITEM.put(Items.LEATHER_CHESTPLATE, tex("burlap.shirt.png"));
        BY_ITEM.put(Items.LEATHER_LEGGINGS,   tex("burlap.trousers.png"));
        BY_ITEM.put(Items.LEATHER_HELMET,     tex("burlap.headwrap.png"));
        BY_ITEM.put(Items.LEATHER_BOOTS,      tex("burlap.shoes.png"));
        BY_ITEM.put(Items.CHAINMAIL_CHESTPLATE, tex("wood.armor.jacket.png"));
        BY_ITEM.put(Items.CHAINMAIL_HELMET,     tex("wood.armor.helmet.png"));
        BY_ITEM.put(Items.CHAINMAIL_LEGGINGS,   tex("wood.armor.pants.png"));
        BY_ITEM.put(Items.GOLDEN_HELMET,        tex("coffeecan.helmet.png"));
        BY_ITEM.put(Items.GOLDEN_CHESTPLATE,    tex("roadsign.jacket.png"));
        BY_ITEM.put(Items.GOLDEN_LEGGINGS,      tex("roadsign.kilt.png"));
        BY_ITEM.put(Items.IRON_CHESTPLATE,      tex("metal.plate.torso.png"));
        BY_ITEM.put(Items.IRON_HELMET,          tex("metal.facemask.png"));
        BY_ITEM.put(Items.IRON_LEGGINGS,        tex("pants.png"));
        BY_ITEM.put(Items.IRON_BOOTS,           tex("shoes.boots.png"));
        BY_ITEM.put(Items.DIAMOND_CHESTPLATE,   tex("hazmatsuit.png"));
        BY_ITEM.put(Items.NETHERITE_CHESTPLATE, tex("hoodie.png"));
        BY_ITEM.put(Items.RABBIT_HIDE,          tex("burlap.gloves.png"));
        BY_ITEM.put(Items.SCUTE,                tex("roadsign.gloves.png"));
        BY_ITEM.put(Items.PHANTOM_MEMBRANE,     tex("tactical.gloves.png"));
        BY_ITEM.put(Items.STONE_AXE,          tex("stonehatchet.png"));
        BY_ITEM.put(Items.STONE_PICKAXE,      tex("stone.pickaxe.png"));
        BY_ITEM.put(Items.IRON_AXE,           tex("hatchet.png"));
        BY_ITEM.put(Items.IRON_PICKAXE,       tex("pickaxe.png"));
        BY_ITEM.put(Items.DIAMOND_PICKAXE,    tex("icepick.salvaged.png"));
        BY_ITEM.put(Items.DIAMOND_AXE,        tex("axe.salvaged.png"));
        BY_ITEM.put(Items.GOLDEN_PICKAXE,     tex("jackhammer.png"));
        BY_ITEM.put(Items.STONE_SHOVEL,       tex("rock.png"));
        BY_ITEM.put(Items.STICK,              tex("spear.wooden.png"));
        BY_ITEM.put(Items.BLAZE_ROD,          tex("spear.stone.png"));
        BY_ITEM.put(Items.SHEARS,             tex("machete.png"));
        BY_ITEM.put(Items.BONE,               tex("knife.bone.png"));
        BY_ITEM.put(Items.PAPER,              tex("bandage.png"));
        BY_ITEM.put(Items.POTION,             tex("syringe.medical.png"));
        BY_ITEM.put(Items.BOW,                tex("bow.hunting.png"));
        BY_ITEM.put(Items.CROSSBOW,           tex("crossbow.png"));
        BY_ITEM.put(Items.WOODEN_HOE,         tex("pistol.eoka.png"));
        BY_ITEM.put(Items.STONE_HOE,          tex("pistol.semiauto.png"));
        BY_ITEM.put(Items.IRON_HOE,           tex("pistol.revolver.png"));
        BY_ITEM.put(Items.GOLDEN_HOE,         tex("pistol.python.png"));
        BY_ITEM.put(Items.DIAMOND_HOE,        tex("m92.png"));
        BY_ITEM.put(Items.NETHERITE_HOE,      tex("smg.2.png"));
        BY_ITEM.put(Items.IRON_SHOVEL,        tex("smg.mp5.png"));
        BY_ITEM.put(Items.DIAMOND_SWORD,      tex("smg.thompson.png"));
        BY_ITEM.put(Items.GOLDEN_SHOVEL,      tex("rifle.semiauto.png"));
        BY_ITEM.put(Items.DIAMOND_SHOVEL,     tex("shotgun.pump.png"));
        BY_ITEM.put(Items.WOODEN_SHOVEL,      tex("shotgun.double.png"));
        BY_ITEM.put(Items.IRON_SWORD,         tex("rifle.lr300.png"));
        BY_ITEM.put(Items.WOODEN_SWORD,       tex("rifle.m39.png"));
        BY_ITEM.put(Items.GOLDEN_SWORD,       tex("rifle.bolt.png"));
        BY_ITEM.put(Items.STONE_SWORD,        tex("rifle.l96.png"));
        BY_ITEM.put(Items.NETHERITE_SWORD,    tex("rifle.ak.png"));
        BY_ITEM.put(Items.FIRE_CHARGE,        tex("grenade.f1.png"));
        BY_ITEM.put(Items.REDSTONE_BLOCK,     tex("explosive.timed.png"));
        BY_ITEM.put(Items.TNT,                tex("explosive.satchel.png"));
        BY_ITEM.put(Items.ARROW,              tex("arrow.wooden.png"));
        BY_ITEM.put(Items.GOLD_NUGGET,        tex("ammo.pistol.png"));
        BY_ITEM.put(Items.IRON_NUGGET,        tex("ammo.rifle.png"));
        BY_ITEM.put(Items.BRICK,              tex("ammo.shotgun.png"));
        BY_ITEM.put(Items.DAYLIGHT_DETECTOR,  tex("electric.solarpanel.large.png"));
        BY_ITEM.put(Items.NOTE_BLOCK,         tex("fun.guitar.png"));

        // Resources
        BY_ITEM.put(Items.OAK_PLANKS,         tex("wood.png"));
        BY_ITEM.put(Items.OAK_LOG,            tex("wood.png"));
        BY_ITEM.put(Items.BIRCH_PLANKS,       tex("wood.png"));
        BY_ITEM.put(Items.BIRCH_LOG,          tex("wood.png"));
        BY_ITEM.put(Items.SPRUCE_PLANKS,      tex("wood.png"));
        BY_ITEM.put(Items.SPRUCE_LOG,         tex("wood.png"));
        BY_ITEM.put(Items.COBBLESTONE,        tex("stones.png"));
        BY_ITEM.put(Items.STONE,              tex("stones.png"));
        BY_ITEM.put(Items.IRON_INGOT,         tex("metal.fragments.png"));
        BY_ITEM.put(Items.IRON_ORE,           tex("metal.ore.png"));
        BY_ITEM.put(Items.NETHERITE_INGOT,    tex("metal.refined.png"));
        BY_ITEM.put(Items.GLOWSTONE_DUST,     tex("sulfur.png"));
        BY_ITEM.put(Items.GOLD_ORE,           tex("sulfur.ore.png"));
        BY_ITEM.put(Items.CHARCOAL,           tex("charcoal.png"));
        BY_ITEM.put(Items.COAL,               tex("coal.png"));
        BY_ITEM.put(Items.STRING,             tex("cloth.png"));
        BY_ITEM.put(Items.LEATHER,            tex("leather.png"));
        BY_ITEM.put(Items.EMERALD,            tex("scrap.png"));
        BY_ITEM.put(Items.REDSTONE,           tex("techparts.png"));
        BY_ITEM.put(Items.OBSIDIAN,           tex("riflebody.png"));
        BY_ITEM.put(Items.FLINT,              tex("metalpipe.png"));
        BY_ITEM.put(Items.FEATHER,            tex("gears.png"));
    }

    private RustItemIcons() {}

    public static Identifier resolveRecipeIcon(String recipeId, ItemStack stack) {
        if (recipeId != null && BY_ID.containsKey(recipeId)) {
            return BY_ID.get(recipeId);
        }
        return resolveStackIcon(stack);
    }

    public static Identifier resolveStackIcon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        net.spacerust.client.SpaceRustItems.ItemDef def = net.spacerust.client.SpaceRustItems.getDef(stack);
        if (def != null) return def.iconTex;
        // Leave unmodified vanilla items with their vanilla sprites in Creative / inventory
        if (!stack.hasCustomName()) return null;
        String name = stack.getName().getString().toLowerCase(Locale.ROOT);
        if (name.contains("lr-300") || name.contains("lr300")) return tex("rifle.lr300.png");
        if (name.contains("l96")) return tex("rifle.l96.png");
        if (name.contains("m39")) return tex("rifle.m39.png");
        if (name.contains("бердан") || name.contains("sar")) return tex("rifle.semiauto.png");
        if (name.contains("bolt") || name.contains("болт")) return tex("rifle.bolt.png");
        if (name.contains("mp5") || name.contains("мп5")) return tex("smg.mp5.png");
        if (name.contains("томпсон") || name.contains("thompson")) return tex("smg.thompson.png");
        if (name.contains("самодельный пп")) return tex("smg.2.png");
        if (name.contains("помп")) return tex("shotgun.pump.png");
        if (name.contains("python")) return tex("pistol.python.png");
        if (name.contains("m92")) return tex("m92.png");
        if (name.contains("p250")) return tex("pistol.semiauto.png");
        if (name.contains("самопал") || name.contains("eoka")) return tex("pistol.eoka.png");
        if (name.contains("ледоруб")) return tex("icepick.salvaged.png");
        if (name.contains("самодельный топор")) return tex("axe.salvaged.png");
        if (name.contains("отбойный")) return tex("jackhammer.png");
        if (name.contains("деревянное коп")) return tex("spear.wooden.png");
        if (name.contains("каменное коп")) return tex("spear.stone.png");
        if (name.contains("мачете")) return tex("machete.png");
        if (name.contains("костяной нож")) return tex("knife.bone.png");
        if (name.contains("бобов") || name.contains("сачел")) return tex("explosive.satchel.png");
        if (name.contains("граната")) return tex("grenade.f1.png");
        if (name.contains("хазмат") || name.contains("химзащит")) return tex("hazmatsuit.png");
        if (name.contains("толстовка") || name.contains("худи")) return tex("hoodie.png");
        if (name.contains("кожаные ботинки")) return tex("shoes.boots.png");
        if (name.contains("дорожная броня")) return tex("roadsign.jacket.png");
        if (name.contains("дорожный килт")) return tex("roadsign.kilt.png");
        if (name.contains("дорожные перчатки")) return tex("roadsign.gloves.png");
        if (name.contains("тактические перчатки")) return tex("tactical.gloves.png");
        if (name.contains("перчатки из мешковин")) return tex("burlap.gloves.png");
        if (name.contains("кофейн")) return tex("coffeecan.helmet.png");
        if (name.contains("деревянный шлем")) return tex("wood.armor.helmet.png");
        if (name.contains("деревянные понож")) return tex("wood.armor.pants.png");
        if (name.contains("дробь") || name.contains("12 калибр")) return tex("ammo.shotgun.png");
        if (name.contains("калаш") || name.contains("ak-47") || name.contains("ak47")) return tex("rifle.ak.png");
        if (name.contains("револьвер")) return tex("pistol.revolver.png");
        if (name.contains("дробовик")) return tex("shotgun.double.png");
        if (name.contains("бинт")) return tex("bandage.png");
        if (name.contains("шприц")) return tex("syringe.medical.png");
        if (name.contains("сера")) return tex("sulfur.png");
        if (name.contains("серная руда")) return tex("sulfur.ore.png");
        if (name.contains("железная руда") || name.contains("металл руда")) return tex("metal.ore.png");
        if (name.contains("скрап") || name.contains("металлолом")) return tex("scrap.png");
        if (name.contains("камень")) return tex("stones.png");
        if (name.contains("дерево")) return tex("wood.png");
        if (name.contains("ткань")) return tex("cloth.png");
        if (name.contains("план построй")) return tex("building.planner.png");
        if (name.contains("молоток") || name.contains("киянка")) return tex("hammer.png");
        if (name.contains("переработчик")) return tex("workbench2.png");
        if (name.contains("шестер")) return tex("gears.png");
        if (name.contains("труб")) return tex("metalpipe.png");
        if (name.contains("пружин")) return tex("metalspring.png");
        if (name.contains("микросхем")) return tex("techparts.png");
        return null;
    }

    public static void drawRecipeIcon(MatrixStack matrices, MinecraftClient client, ItemRenderer itemRenderer,
                                      String recipeId, ItemStack fallbackStack, int x, int y, int size) {
        Identifier id = resolveRecipeIcon(recipeId, fallbackStack);
        if (id != null) {
            drawItemPng(matrices, client, id, x, y, size);
        } else if (fallbackStack != null && !fallbackStack.isEmpty()) {
            itemRenderer.renderInGuiWithOverrides(fallbackStack, x + (size - 16) / 2, y + (size - 16) / 2);
        }
    }

    public static void drawSlotItem(MatrixStack matrices, MinecraftClient client, ItemRenderer itemRenderer,
                                    ItemStack stack, int slotX, int slotY, int slotSize) {
        if (stack == null || stack.isEmpty()) return;

        // Vertical left durability bar (Rust style)
        if (stack.isDamageable() && stack.isDamaged()) {
            int barX = slotX + 1;
            int barY = slotY + 2;
            int barH = slotSize - 4;
            float frac = Math.max(0f, Math.min(1f,
                    (stack.getMaxDamage() - stack.getDamage()) / (float) stack.getMaxDamage()));
            int fillH = Math.max(1, Math.round(barH * frac));
            int col = frac > 0.45f ? 0xFF72B842 : (frac > 0.2f ? 0xFFE6A128 : 0xFFE04536);
            DrawableHelper.fill(matrices, barX, barY, barX + 2, barY + barH, 0x88111111);
            DrawableHelper.fill(matrices, barX, barY + (barH - fillH), barX + 2, barY + barH, col);
        }

        Identifier id = resolveStackIcon(stack);
        int pad = Math.max(2, slotSize / 8);
        int iconSize = slotSize - pad * 2;
        if (id != null) {
            drawItemPng(matrices, client, id, slotX + pad, slotY + pad, iconSize);
        } else {
            int off = (slotSize - 16) / 2;
            itemRenderer.renderInGuiWithOverrides(stack, slotX + off, slotY + off);
        }

        int count = RustStackUtil.getCount(stack);
        if (count > 1) {
            String cnt = "x" + count;
            RustFontRenderer sf = slotSize >= 32 ? RustFontRenderer.boldSmall() : RustFontRenderer.small();
            float w = sf.getWidth(cnt);
            sf.drawShadow(matrices, cnt, slotX + slotSize - w - 2, slotY + slotSize - 9, 0xFFF7EBE1);
        }
    }

    public static void drawItemPng(MatrixStack matrices, MinecraftClient client, Identifier tex, int x, int y, int size) {
        if (client == null || tex == null) return;
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        client.getTextureManager().bindTexture(tex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        DrawableHelper.drawTexture(matrices, x, y, size, size, 0.0f, 0.0f, 1, 1, 1, 1);
    }
}
