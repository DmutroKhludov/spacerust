package net.spacerust.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Handles Plugin Messaging ("spacerust:sync") between Purpur server & SpaceRust client mod:
 * - Broadcasts RT Monument list & coordinates for RustMapScreen (G)
 * - Streams active nearby 3D Ore Nodes for OreNodeRenderer
 * - Calculates zone radiation & stats for RustHudOverlay
 * - Validates & executes crafting requests from RustCraftingScreen / RustCraftingQueue
 */
public class CraftingAndSyncManager implements PluginMessageListener, Listener {

    private static class ServerRecipe {
        final String name;
        final Material result;
        final List<Map.Entry<Material, Integer>> ingredients = new ArrayList<>();

        ServerRecipe(String name, Material result, Object... ingPairs) {
            this.name = name;
            this.result = result;
            for (int i = 0; i + 1 < ingPairs.length; i += 2) {
                ingredients.add(new AbstractMap.SimpleEntry<>((Material) ingPairs[i], (Integer) ingPairs[i + 1]));
            }
        }
    }

    private final SpaceRustPlugin plugin;
    private final MonumentBuilder monuments;
    private final OreAndLootManager oreManager;
    private final BuildingManager buildingManager;
    private final Map<String, ServerRecipe> recipes = new HashMap<>();
    private final Map<UUID, Integer> playerRad = new HashMap<>();

    public CraftingAndSyncManager(SpaceRustPlugin plugin, MonumentBuilder monuments,
                                  OreAndLootManager oreManager, BuildingManager buildingManager) {
        this.plugin = plugin;
        this.monuments = monuments;
        this.oreManager = oreManager;
        this.buildingManager = buildingManager;
        registerRecipes();
    }

    private void registerRecipes() {
        reg("campfire", "Костер", Material.CAMPFIRE, Material.OAK_PLANKS, 100);
        reg("torch", "Факел", Material.HEART_OF_THE_SEA, Material.OAK_PLANKS, 30, Material.STRING, 5);
        reg("small_box", "Деревянный ящик", Material.CHEST, Material.OAK_PLANKS, 100);
        reg("bed", "Спальный мешок", Material.WHITE_BED, Material.STRING, 30);

        reg("build_plan", "План постройки", Material.MAP, Material.OAK_PLANKS, 20);
        reg("wood_hammer", "Киянка", Material.WOODEN_AXE, Material.OAK_PLANKS, 100);
        reg("wood_door", "Деревянная дверь", Material.OAK_DOOR, Material.OAK_PLANKS, 300);
        reg("sheet_door", "Металлическая дверь", Material.IRON_DOOR, Material.IRON_INGOT, 150);
        reg("code_lock", "Кодовый замок", Material.LEVER, Material.IRON_INGOT, 100);
        reg("key_lock", "Дверной замок", Material.TRIPWIRE_HOOK, Material.OAK_PLANKS, 100);
        reg("wood_barricade", "Деревянная баррикада", Material.OAK_FENCE, Material.OAK_PLANKS, 200);
        reg("wood_gate", "Деревянные ворота", Material.OAK_FENCE_GATE, Material.OAK_PLANKS, 500);
        reg("metal_bars", "Металлическая решетка", Material.IRON_BARS, Material.IRON_INGOT, 50);
        reg("ladder", "Деревянная лестница", Material.LADDER, Material.OAK_PLANKS, 100, Material.STRING, 2);
        reg("window_shutter", "Деревянные ставни", Material.OAK_TRAPDOOR, Material.OAK_PLANKS, 200);

        reg("furnace", "Печь", Material.FURNACE, Material.COBBLESTONE, 200, Material.OAK_PLANKS, 100, Material.COAL, 50);
        reg("large_box", "Большой сундук", Material.BARREL, Material.OAK_PLANKS, 250, Material.IRON_INGOT, 50);
        reg("workbench_1", "Верстак 1 уровня", Material.CRAFTING_TABLE, Material.OAK_PLANKS, 500, Material.IRON_INGOT, 100, Material.EMERALD, 50);
        reg("workbench_2", "Верстак 2 уровня", Material.SMITHING_TABLE, Material.IRON_INGOT, 500, Material.NETHERITE_INGOT, 20, Material.EMERALD, 500);

        reg("gunpowder", "Порох", Material.GUNPOWDER, Material.CHARCOAL, 20, Material.GLOWSTONE_DUST, 30);
        reg("low_grade_fuel", "Топливо низкого качества", Material.BLAZE_POWDER, Material.STRING, 10, Material.LEATHER, 30);

        reg("burlap_headwrap", "Повязка на голову", Material.LEATHER_HELMET, Material.STRING, 10);
        reg("burlap_shirt", "Рубашка из мешковины", Material.LEATHER_CHESTPLATE, Material.STRING, 40);
        reg("burlap_pants", "Штаны из мешковины", Material.LEATHER_LEGGINGS, Material.STRING, 30);
        reg("burlap_shoes", "Обувь из мешковины", Material.LEATHER_BOOTS, Material.STRING, 15);
        reg("burlap_gloves", "Перчатки из мешковины", Material.RABBIT_HIDE, Material.STRING, 10);
        reg("wood_helmet", "Деревянный шлем", Material.CHAINMAIL_HELMET, Material.OAK_PLANKS, 150, Material.STRING, 15);
        reg("wood_armor", "Деревянная кираса", Material.CHAINMAIL_CHESTPLATE, Material.OAK_PLANKS, 200, Material.STRING, 30);
        reg("wood_pants", "Деревянные поножи", Material.CHAINMAIL_LEGGINGS, Material.OAK_PLANKS, 200, Material.STRING, 1);
        reg("hoodie", "Толстовка (Худи)", Material.NETHERITE_CHESTPLATE, Material.STRING, 80);
        reg("pants", "Штаны", Material.IRON_LEGGINGS, Material.STRING, 70);
        reg("boots", "Кожаные ботинки", Material.IRON_BOOTS, Material.LEATHER, 20, Material.IRON_INGOT, 15);
        reg("coffeecan_helmet", "Каска из кофейной банки", Material.GOLDEN_HELMET, Material.STRING, 15, Material.IRON_INGOT, 60);
        reg("roadsign_jacket", "Дорожная броня", Material.GOLDEN_CHESTPLATE, Material.LEATHER, 40, Material.EMERALD, 20);
        reg("roadsign_kilt", "Дорожный килт", Material.GOLDEN_LEGGINGS, Material.LEATHER, 20, Material.EMERALD, 15);
        reg("roadsign_gloves", "Дорожные перчатки", Material.SCUTE, Material.LEATHER, 10, Material.STRING, 15);
        reg("tactical_gloves", "Тактические перчатки", Material.PHANTOM_MEMBRANE, Material.STRING, 30, Material.LEATHER, 20);
        reg("hazmat", "Костюм химзащиты (Хазмат)", Material.DIAMOND_CHESTPLATE, Material.STRING, 30, Material.NETHERITE_INGOT, 8);
        reg("metal_facemask", "Металлическая маска", Material.IRON_HELMET, Material.NETHERITE_INGOT, 15, Material.LEATHER, 50);
        reg("metal_chest", "Металлическая кираса", Material.IRON_CHESTPLATE, Material.NETHERITE_INGOT, 25, Material.LEATHER, 50);

        reg("stone_hatchet", "Каменный топор", Material.STONE_AXE, Material.OAK_PLANKS, 200, Material.COBBLESTONE, 100);
        reg("stone_pickaxe", "Каменная кирка", Material.STONE_PICKAXE, Material.OAK_PLANKS, 200, Material.COBBLESTONE, 100);
        reg("metal_hatchet", "Металлический топор", Material.IRON_AXE, Material.OAK_PLANKS, 100, Material.IRON_INGOT, 75);
        reg("metal_pickaxe", "Металлическая кирка", Material.IRON_PICKAXE, Material.OAK_PLANKS, 100, Material.IRON_INGOT, 125);
        reg("salvaged_axe", "Самодельный топор", Material.DIAMOND_AXE, Material.FLINT, 1, Material.IRON_INGOT, 100);
        reg("salvaged_icepick", "Ледоруб", Material.DIAMOND_PICKAXE, Material.FLINT, 1, Material.IRON_INGOT, 100);
        reg("jackhammer", "Отбойный молоток", Material.GOLDEN_PICKAXE, Material.NETHERITE_INGOT, 15, Material.IRON_INGOT, 150);

        reg("bandage", "Бинт", Material.PAPER, Material.STRING, 10);
        reg("syringe", "Медицинский шприц", Material.POTION, Material.STRING, 15, Material.IRON_INGOT, 10, Material.BLAZE_POWDER, 10);

        reg("wooden_spear", "Деревянное копьё", Material.STICK, Material.OAK_PLANKS, 300);
        reg("stone_spear", "Каменное копьё", Material.BLAZE_ROD, Material.OAK_PLANKS, 300, Material.COBBLESTONE, 100);
        reg("machete", "Мачете", Material.SHEARS, Material.OAK_PLANKS, 100, Material.IRON_INGOT, 40);
        reg("bow", "Охотничий лук", Material.BOW, Material.OAK_PLANKS, 200, Material.STRING, 50);
        reg("crossbow", "Арбалет", Material.CROSSBOW, Material.OAK_PLANKS, 200, Material.IRON_INGOT, 75, Material.STRING, 2);
        reg("eoka", "Самопал (Eoka)", Material.WOODEN_HOE, Material.OAK_PLANKS, 75, Material.IRON_INGOT, 30);
        reg("revolver", "Револьвер", Material.IRON_HOE, Material.IRON_INGOT, 125, Material.FLINT, 1);
        reg("pipe_shotgun", "Двуствольный дробовик", Material.WOODEN_SHOVEL, Material.IRON_INGOT, 175, Material.FLINT, 2);
        reg("p250", "Пистолет P250", Material.STONE_HOE, Material.IRON_INGOT, 150, Material.FLINT, 1, Material.TRIPWIRE_HOOK, 1);
        reg("python", "Револьвер Python", Material.GOLDEN_HOE, Material.NETHERITE_INGOT, 10, Material.FLINT, 3, Material.TRIPWIRE_HOOK, 1);
        reg("custom_smg", "Самодельный ПП", Material.NETHERITE_HOE, Material.NETHERITE_INGOT, 8, Material.IRON_INGOT, 100);
        reg("thompson", "Томпсон", Material.DIAMOND_SWORD, Material.NETHERITE_INGOT, 10, Material.OAK_PLANKS, 100, Material.TRIPWIRE_HOOK, 1);
        reg("mp5", "Пистолет-пулемёт MP5", Material.IRON_SHOVEL, Material.NETHERITE_INGOT, 15, Material.TRIPWIRE_HOOK, 2);
        reg("sar", "Берданка (SAR)", Material.GOLDEN_SHOVEL, Material.IRON_INGOT, 450, Material.TRIPWIRE_HOOK, 1);
        reg("pump_shotgun", "Помповый дробовик", Material.DIAMOND_SHOVEL, Material.NETHERITE_INGOT, 15, Material.FLINT, 2);
        reg("ak47", "Автомат AK-47", Material.NETHERITE_SWORD, Material.NETHERITE_INGOT, 50, Material.OAK_PLANKS, 200, Material.OBSIDIAN, 1, Material.TRIPWIRE_HOOK, 4);
        reg("lr300", "Винтовка LR-300", Material.IRON_SWORD, Material.NETHERITE_INGOT, 60, Material.EMERALD, 250);
        reg("m39", "Винтовка M39", Material.WOODEN_SWORD, Material.NETHERITE_INGOT, 50, Material.TRIPWIRE_HOOK, 2);
        reg("bolt", "Винтовка Bolt", Material.GOLDEN_SWORD, Material.NETHERITE_INGOT, 30, Material.FLINT, 3);
        reg("l96", "Снайперка L96", Material.STONE_SWORD, Material.NETHERITE_INGOT, 80, Material.EMERALD, 500);
        reg("hmlmg", "Самодельный ручной пулемёт", Material.NETHERITE_PICKAXE, Material.NETHERITE_INGOT, 60, Material.OBSIDIAN, 1, Material.TRIPWIRE_HOOK, 3);
        reg("rocket_launcher", "Ракетница", Material.NETHERITE_SHOVEL, Material.NETHERITE_INGOT, 40, Material.FLINT, 6);
        reg("ammo_rocket", "Ракета", Material.FIREWORK_ROCKET, Material.GUNPOWDER, 150, Material.FLINT, 2, Material.IRON_INGOT, 100);
        reg("satchel", "Бобовый заряд (Satchel)", Material.TNT, Material.GUNPOWDER, 120, Material.IRON_INGOT, 80, Material.STRING, 1);
        reg("f1_grenade", "Граната F1", Material.FIRE_CHARGE, Material.GUNPOWDER, 30, Material.IRON_INGOT, 25);
        reg("c4", "Взрывчатка C4", Material.REDSTONE_BLOCK, Material.TNT, 20, Material.STRING, 5, Material.REDSTONE, 1);

        reg("wooden_arrows", "Деревянные стрелы", Material.ARROW, Material.OAK_PLANKS, 25, Material.COBBLESTONE, 10);
        reg("ammo_9mm", "Патроны 9мм", Material.GOLD_NUGGET, Material.IRON_INGOT, 10, Material.GUNPOWDER, 10);
        reg("ammo_556", "Патроны 5.56мм", Material.IRON_NUGGET, Material.IRON_INGOT, 15, Material.GUNPOWDER, 20);
        reg("ammo_12g", "Дробь 12 калибр", Material.BRICK, Material.IRON_INGOT, 10, Material.GUNPOWDER, 15);

        reg("solar_panel", "Солнечная панель", Material.DAYLIGHT_DETECTOR, Material.NETHERITE_INGOT, 10, Material.IRON_INGOT, 50);
        reg("guitar", "Акустическая гитара", Material.NOTE_BLOCK, Material.OAK_PLANKS, 100, Material.STRING, 10);
    }

    private void reg(String id, String name, Material result, Object... pairs) {
        recipes.put(id, new ServerRecipe(name, result, pairs));
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!p.hasPlayedBefore() && isInventoryEmpty(p.getInventory())) {
            giveStarterKit(p);
        } else {
            consolidatePlayerInventory(p);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> sendMonuments(p), 20L);
    }

    private void consolidatePlayerInventory(Player p) {
        PlayerInventory inv = p.getInventory();
        List<ItemStack> relocated = new ArrayList<>();
        for (int s = 0; s <= 40; s++) {
            ItemStack st = inv.getItem(s);
            if (st == null || st.getType() == Material.AIR) continue;
            // Upgrade legacy SpaceRust Torch (Material.TORCH) -> unplaceable HEART_OF_THE_SEA torch
            if (st.getType() == Material.TORCH && st.hasItemMeta() && st.getItemMeta().hasDisplayName()
                    && st.getItemMeta().getDisplayName().replace("§f", "").trim().equalsIgnoreCase("факел")) {
                st = OreAndLootManager.createItem("torch", st.getAmount());
                inv.setItem(s, st);
            }
            // Stamp legacy SpaceRust items cleanly with SpaceRustId + CustomModelData
            if (st.hasItemMeta() && !st.getItemMeta().hasCustomModelData()) {
                SpaceRustItemRegistry.ItemDef def = SpaceRustItemRegistry.find(st.getType(),
                        st.getItemMeta().hasDisplayName() ? st.getItemMeta().getDisplayName() : null);
                if (def != null) {
                    SpaceRustItemRegistry.stamp(st, def);
                    inv.setItem(s, st);
                }
            }
            // Convert legacy stacks or items sitting in unused hotbar slots (6..8); keep clothing slots 33..35 untouched!
            if ((s >= 6 && s <= 8) || OreAndLootManager.isStackableResource(st.getType())) {
                int real = OreAndLootManager.getRealCount(st);
                String name = OreAndLootManager.getDefaultResourceName(st);
                ItemStack copy = OreAndLootManager.isStackableResource(st.getType())
                        ? OreAndLootManager.named(st.getType(), name, real)
                        : st.clone();
                inv.setItem(s, null);
                relocated.add(copy);
            }
        }
        for (ItemStack it : relocated) {
            OreAndLootManager.giveToPlayer(p, it);
        }
        p.updateInventory();
    }

    private boolean isInventoryEmpty(PlayerInventory inv) {
        for (ItemStack it : inv.getContents()) {
            if (it != null && it.getType() != Material.AIR) return false;
        }
        return true;
    }

    public void giveStarterKit(Player p) {
        p.getInventory().setItem(0, OreAndLootManager.createItem("rock", 1));
        p.getInventory().setItem(1, OreAndLootManager.createItem("torch", 1));
    }

    public void broadcastMonuments() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendMonuments(p);
        }
    }

    public void sendMonuments(Player p) {
        StringBuilder sb = new StringBuilder("MONUMENTS|");
        for (MonumentBuilder.MonumentDef m : monuments.getMonuments()) {
            sb.append(m.name).append(",")
              .append(m.type).append(",")
              .append(m.x).append(",")
              .append(m.z).append(",")
              .append(m.radius).append(",")
              .append(m.radLevel).append(";");
        }
        sendToPlayer(p, sb.toString());
    }

    public void tickPlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            // 1. Update Radiation
            int zoneRad = monuments.getRadiationAt(p.getLocation());
            int armorProt = getArmorPiecesCount(p) * 5;
            int effZone = Math.max(0, zoneRad - armorProt);
            int curRad = playerRad.getOrDefault(p.getUniqueId(), 0);
            if (effZone > 0) {
                curRad = Math.min(100, curRad + Math.max(1, effZone / 5));
                if (curRad >= 35 && p.getHealth() > 2.0) {
                    p.damage(0.8);
                }
            } else {
                curRad = Math.max(0, curRad - 2);
            }
            playerRad.put(p.getUniqueId(), curRad);

            sendToPlayer(p, "STATS|" + curRad + ";50;0;230");

            // 2. Sync nearby active 3D Ore Nodes & 3D Rust Trees
            sendNodesAndTrees(p);

            // 3. Sync nearby RT Loot Crates (rarity + coordinates within 92 blocks, only active chests)
            sendCrates(p);

            // 4. Sync nearby player-built 3D Rust structures
            if (buildingManager != null) {
                buildingManager.sendBuildings(p);
            }
        }
    }

    public void sendNodesAndTrees(Player p) {
        Location pl = p.getLocation();
        StringBuilder nsb = new StringBuilder("NODES|");
        int sent = 0;
        for (OreAndLootManager.OreNodeState node : oreManager.getAllNodes()) {
            if (!node.isActive() || node.world != p.getWorld()) continue;
            double dx = node.x - pl.getX();
            double dz = node.z - pl.getZ();
            if (dx * dx + dz * dz <= 92.0 * 92.0) {
                nsb.append(node.id).append(",")
                   .append(node.kind).append(",")
                   .append(node.stage).append(",")
                   .append(node.x + 0.5).append(",")
                   .append(node.y).append(",")
                   .append(node.z + 0.5).append(",")
                   .append(String.format(Locale.US, "%.2f,%.2f,%.2f", node.hx, node.hy, node.hz))
                   .append(";");
                if (++sent >= 65) break;
            }
        }
        sendToPlayer(p, nsb.toString());

        StringBuilder tsb = new StringBuilder("TREES|");
        int tSent = 0;
        for (OreAndLootManager.RustTreeState tree : oreManager.getAllTrees()) {
            if (!tree.isActive() || tree.world != p.getWorld()) continue;
            double dx = tree.x - pl.getX();
            double dz = tree.z - pl.getZ();
            if (dx * dx + dz * dz <= 96.0 * 96.0) {
                tsb.append(tree.id).append(",")
                   .append(tree.kind).append(",")
                   .append(tree.x + 0.5).append(",")
                   .append(tree.y).append(",")
                   .append(tree.z + 0.5).append(",")
                   .append(String.format(Locale.US, "%.2f,%.2f,%.3f,%.2f",
                           tree.radius, tree.height, tree.xAngle, tree.xY))
                   .append(";");
                if (++tSent >= 75) break;
            }
        }
        sendToPlayer(p, tsb.toString());
    }

    public void broadcastTreeFall(int treeId, double fallYawDeg) {
        String msg = String.format(Locale.US, "TREE_FALL|%d|%.1f", treeId, fallYawDeg);
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendToPlayer(p, msg);
        }
    }

    public void sendCrates(Player p) {
        Location pl = p.getLocation();
        StringBuilder csb = new StringBuilder("CRATES|");
        for (MonumentBuilder.CrateSpot spot : monuments.getCrateSpots()) {
            if (spot.loc.getWorld() != p.getWorld()) continue;
            if (spot.loc.getBlock().getType() != Material.CHEST) continue;
            double dx = spot.loc.getX() - pl.getX();
            double dz = spot.loc.getZ() - pl.getZ();
            if (dx * dx + dz * dz <= 92.0 * 92.0) {
                csb.append(spot.tier.name().toLowerCase(Locale.ROOT)).append(",")
                   .append(spot.loc.getBlockX()).append(",")
                   .append(spot.loc.getBlockY()).append(",")
                   .append(spot.loc.getBlockZ()).append(";");
            }
        }
        sendToPlayer(p, csb.toString());
    }

    private boolean rngGeigerSound() {
        return Math.random() < 0.45;
    }

    private int getArmorPiecesCount(Player p) {
        int c = 0;
        PlayerInventory inv = p.getInventory();
        for (ItemStack it : inv.getArmorContents()) {
            if (it != null && it.getType() != Material.AIR) c++;
        }
        for (int s : new int[]{33, 34, 35}) {
            ItemStack it = inv.getItem(s);
            if (it != null && it.getType() != Material.AIR) c++;
        }
        return c;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!SpaceRustPlugin.SYNC_CHANNEL.equals(channel) || message == null) return;
        String msg = new String(message, StandardCharsets.UTF_8);

        if ("REQUEST_SYNC".equals(msg)) {
            sendMonuments(player);
            sendNodesAndTrees(player);
            sendCrates(player);
            if (buildingManager != null) {
                buildingManager.sendBuildings(player);
            }
            return;
        }
        if (msg.startsWith("BUILD_PLACE|") && buildingManager != null) {
            String[] p = msg.split("\\|");
            if (p.length >= 6) {
                try {
                    buildingManager.handlePlace(player, p[1],
                            Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                            Double.parseDouble(p[4]), Float.parseFloat(p[5]));
                } catch (NumberFormatException ignored) {}
            }
            return;
        }
        if (msg.startsWith("BUILD_UPGRADE|") && buildingManager != null) {
            String[] p = msg.split("\\|");
            if (p.length >= 3) {
                try {
                    buildingManager.handleUpgrade(player, Integer.parseInt(p[1]), p[2]);
                } catch (NumberFormatException ignored) {}
            }
            return;
        }
        if (msg.startsWith("BUILD_ROTATE|") && buildingManager != null) {
            try {
                buildingManager.handleRotate(player, Integer.parseInt(msg.substring(13).trim()));
            } catch (NumberFormatException ignored) {}
            return;
        }
        if (msg.startsWith("BUILD_DEMOLISH|") && buildingManager != null) {
            try {
                buildingManager.handleDemolish(player, Integer.parseInt(msg.substring(15).trim()));
            } catch (NumberFormatException ignored) {}
            return;
        }
        if (msg.startsWith("BUILD_REPAIR|") && buildingManager != null) {
            try {
                buildingManager.handleRepair(player, Integer.parseInt(msg.substring(13).trim()));
            } catch (NumberFormatException ignored) {}
            return;
        }
        if (msg.startsWith("DEPLOY_PLACE|") && buildingManager != null) {
            String[] p = msg.split("\\|");
            if (p.length >= 5) {
                try {
                    buildingManager.handleDeployPlace(player,
                            Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                            Integer.parseInt(p[3]), Float.parseFloat(p[4]));
                } catch (NumberFormatException ignored) {}
            }
            return;
        }
        if (msg.startsWith("FARM_HIT|")) {
            String[] p = msg.split("\\|");
            if (p.length >= 6) {
                try {
                    String kind = p[1];
                    int targetId = Integer.parseInt(p[2]);
                    double hx = Double.parseDouble(p[3]);
                    double hy = Double.parseDouble(p[4]);
                    double hz = Double.parseDouble(p[5]);
                    oreManager.handleFarmHit(player, kind, targetId, hx, hy, hz, this);
                } catch (NumberFormatException ignored) {}
            }
            return;
        }
        if (msg.startsWith("SPLIT|")) {
            try {
                int hs = Integer.parseInt(msg.substring(6).trim());
                handleSplit(player, hs);
            } catch (NumberFormatException ignored) {}
            return;
        }
        if (msg.startsWith("LOOT_MOVE|")) {
            try {
                int rawSlot = Integer.parseInt(msg.substring(10).trim());
                org.bukkit.inventory.Inventory topInv = player.getOpenInventory().getTopInventory();
                OreAndLootManager.transferContainerSlotRust(oreManager, player, topInv, rawSlot);
                sendCrates(player);
            } catch (NumberFormatException ignored) {}
            return;
        }
        if (msg.startsWith("SHOOT|")) {
            String[] p = msg.split("\\|");
            if (p.length >= 2) {
                String mode = p.length >= 4 ? p[3] : "standard";
                handleGunShoot(player, p[1], p.length >= 3 && "1".equals(p[2]), mode);
            }
            return;
        }
        if (msg.startsWith("BOW_SHOOT|")) {
            float power = 1.0f;
            try {
                power = Float.parseFloat(msg.substring(10).trim());
            } catch (NumberFormatException ignored) {}
            handleBowShoot(player, power);
            return;
        }
        if (msg.startsWith("USE_MED|")) {
            String kind = msg.substring(8).trim();
            handleUseMedical(player, kind);
            return;
        }
        if (msg.startsWith("CRAFT|")) {
            String[] p = msg.split("\\|");
            if (p.length >= 3) {
                String id = p[1];
                int qty = 1;
                try { qty = Math.max(1, Math.min(64, Integer.parseInt(p[2]))); } catch (NumberFormatException ignored) {}
                handleCraft(player, id, qty);
            }
        }
    }

    private void handleSplit(Player p, int handlerSlot) {
        int bk = handlerSlotToBukkit(handlerSlot);
        if (bk < 0 || bk > 40) return;
        PlayerInventory inv = p.getInventory();
        ItemStack st = inv.getItem(bk);
        if (st == null || st.getType() == Material.AIR) return;
        int cur = OreAndLootManager.getRealCount(st);
        if (cur <= 1) return;

        int take = cur / 2;
        int keep = cur - take;
        OreAndLootManager.setRealCount(st, keep);
        inv.setItem(bk, st);

        ItemStack half = st.clone();
        OreAndLootManager.setRealCount(half, take);
        boolean placed = false;
        int[] targets = new int[30];
        int idx = 0;
        for (int s = 9; s <= 32; s++) targets[idx++] = s;
        for (int s = 0; s <= 5; s++) targets[idx++] = s;
        for (int s : targets) {
            if (inv.getItem(s) == null || inv.getItem(s).getType() == Material.AIR) {
                inv.setItem(s, half);
                placed = true;
                break;
            }
        }
        if (!placed) {
            OreAndLootManager.giveToPlayer(p, half);
        }
        p.updateInventory();
    }

    private int handlerSlotToBukkit(int hs) {
        if (hs >= 36 && hs <= 44) return hs - 36;
        if (hs >= 9 && hs <= 35) return hs;
        if (hs == 45) return 40;
        if (hs >= 5 && hs <= 8) return 39 - (hs - 5);
        return -1;
    }

    private void handleCraft(Player p, String recipeId, int qty) {
        ServerRecipe r = recipes.get(recipeId);
        if (r == null) return;

        if (p.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            for (Map.Entry<Material, Integer> ing : r.ingredients) {
                if (countMaterial(p, ing.getKey()) < ing.getValue() * qty) {
                    return;
                }
            }
            for (Map.Entry<Material, Integer> ing : r.ingredients) {
                deductMaterial(p, ing.getKey(), ing.getValue() * qty);
            }
        }
        ItemStack crafted = SpaceRustItemRegistry.get(recipeId) != null
                ? OreAndLootManager.createItem(recipeId, qty)
                : OreAndLootManager.named(r.result, r.name, qty);
        OreAndLootManager.giveToPlayer(p, crafted);
        sendToPlayer(p, "GAIN|" + r.name.toUpperCase(Locale.ROOT) + "|" + qty);
        p.updateInventory();
    }

    public boolean consumeResource(Player p, Material target, int needed) {
        if (p == null) return false;
        if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) return true;
        if (countMaterial(p, target) < needed) return false;
        deductMaterial(p, target, needed);
        p.updateInventory();
        return true;
    }

    private int countMaterial(Player p, Material target) {
        int count = 0;
        for (ItemStack st : p.getInventory().getContents()) {
            if (st != null && matchesResource(st.getType(), target)) {
                count += OreAndLootManager.getRealCount(st);
            }
        }
        return count;
    }

    private void deductMaterial(Player p, Material target, int needed) {
        int rem = needed;
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length && rem > 0; i++) {
            ItemStack st = contents[i];
            if (st != null && matchesResource(st.getType(), target)) {
                int cur = OreAndLootManager.getRealCount(st);
                int take = Math.min(cur, rem);
                if (cur - take <= 0) {
                    p.getInventory().setItem(i, null);
                } else {
                    OreAndLootManager.setRealCount(st, cur - take);
                    p.getInventory().setItem(i, st);
                }
                rem -= take;
            }
        }
    }

    private boolean matchesResource(Material actual, Material target) {
        if (target == Material.OAK_PLANKS) {
            return actual == Material.OAK_PLANKS || actual == Material.BIRCH_PLANKS || actual == Material.SPRUCE_PLANKS
                    || actual == Material.OAK_LOG || actual == Material.BIRCH_LOG || actual == Material.SPRUCE_LOG;
        }
        if (target == Material.COBBLESTONE) {
            return actual == Material.COBBLESTONE || actual == Material.STONE;
        }
        return actual == target;
    }

    public void sendToPlayer(Player p, String msg) {
        try {
            p.sendPluginMessage(plugin, SpaceRustPlugin.SYNC_CHANNEL, msg.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {}
    }

    private void handleGunShoot(Player p, String gunKey, boolean isAds, String ammoMode) {
        if (p == null || !p.isOnline()) return;
        Location eye = p.getEyeLocation();
        org.bukkit.World world = eye.getWorld();
        if (world == null) return;

        if ("rocket_launcher".equals(gunKey)) {
            if (!consumeResource(p, Material.FIREWORK_ROCKET, 1)) return;
            org.bukkit.util.Vector rDir = eye.getDirection().normalize();
            double speed = "hv".equals(ammoMode) ? 95.0 : 65.0;
            org.bukkit.util.RayTraceResult rBlock = world.rayTraceBlocks(eye, rDir, speed, org.bukkit.FluidCollisionMode.NEVER, true);
            double dist = (rBlock != null && rBlock.getHitPosition() != null)
                    ? rBlock.getHitPosition().distance(eye.toVector()) : speed;
            org.bukkit.util.RayTraceResult rEnt = world.rayTraceEntities(eye, rDir, dist, 0.45,
                    ent -> ent != p && ent instanceof org.bukkit.entity.LivingEntity);
            Location impact = (rEnt != null && rEnt.getHitPosition() != null)
                    ? rEnt.getHitPosition().toLocation(world)
                    : (rBlock != null && rBlock.getHitPosition() != null ? rBlock.getHitPosition().toLocation(world) : eye.clone().add(rDir.clone().multiply(dist)));

            // Rocket smoke trail + fiery impact blast (terrain-safe explosion)
            for (double d = 1.5; d < dist; d += 2.0) {
                world.spawnParticle(org.bukkit.Particle.CAMPFIRE_COSY_SMOKE, eye.clone().add(rDir.clone().multiply(d)), 2, 0.08, 0.08, 0.08, 0.01);
            }
            world.spawnParticle(org.bukkit.Particle.EXPLOSION_HUGE, impact, 2, 0.4, 0.4, 0.4, 0.0);
            world.spawnParticle(org.bukkit.Particle.FLAME, impact, 28, 1.4, 0.8, 1.4, 0.08);
            world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 2.2f, 0.85f);
            double rad = "hv".equals(ammoMode) ? 4.5 : 6.0;
            for (org.bukkit.entity.Entity e : world.getNearbyEntities(impact, rad, rad, rad)) {
                if (e instanceof org.bukkit.entity.LivingEntity && e != p) {
                    double ed = Math.max(0.25, 1.0 - e.getLocation().distance(impact) / rad);
                    ((org.bukkit.entity.LivingEntity) e).damage(24.0 * ed, p);
                    if ("incendiary".equals(ammoMode)) e.setFireTicks(100);
                }
            }
            return;
        }

        double maxRange = ("shotgun".equals(gunKey) || "pump".equals(gunKey) || "eoka".equals(gunKey)) ? 28.0
                : (("bolt".equals(gunKey) || "l96".equals(gunKey)) ? 140.0 : 90.0);
        double baseDamage = ("l96".equals(gunKey)) ? 18.0
                : (("bolt".equals(gunKey) || "shotgun".equals(gunKey) || "pump".equals(gunKey)) ? 15.0
                : ("eoka".equals(gunKey) ? 13.0
                : ("python".equals(gunKey) || "m39".equals(gunKey) ? 10.5
                : (("ak47".equals(gunKey) || "hmlmg".equals(gunKey) || "lr300".equals(gunKey) || "sar".equals(gunKey)) ? 8.5
                : (("revolver".equals(gunKey) || "p250".equals(gunKey) || "m92".equals(gunKey)) ? 7.2 : 6.0)))));
        if ("hv".equals(ammoMode)) baseDamage *= 1.12;

        // Play nearby audible shot for other players + subtle smoke particle at muzzle
        org.bukkit.util.Vector dir = eye.getDirection().normalize();
        Location muzzle = eye.clone().add(dir.clone().multiply(0.8));
        world.spawnParticle(org.bukkit.Particle.SMOKE_NORMAL, muzzle, 3, 0.05, 0.05, 0.05, 0.02);
        for (Player other : world.getPlayers()) {
            if (!other.getUniqueId().equals(p.getUniqueId()) && other.getLocation().distanceSquared(eye) <= 48.0 * 48.0) {
                other.playSound(eye, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, "shotgun".equals(gunKey) ? 0.6f : 1.15f);
            }
        }

        org.bukkit.util.RayTraceResult blockHit = world.rayTraceBlocks(eye, dir, maxRange, org.bukkit.FluidCollisionMode.NEVER, true);
        double blockDist = (blockHit != null && blockHit.getHitPosition() != null)
                ? blockHit.getHitPosition().distance(eye.toVector())
                : maxRange;

        org.bukkit.util.RayTraceResult entHit = world.rayTraceEntities(eye, dir, blockDist, 0.25,
                ent -> ent != p && ent instanceof org.bukkit.entity.LivingEntity);
        if (entHit != null && entHit.getHitEntity() instanceof org.bukkit.entity.LivingEntity) {
            org.bukkit.entity.LivingEntity victim = (org.bukkit.entity.LivingEntity) entHit.getHitEntity();
            double hitY = entHit.getHitPosition().getY();
            double eyeY = victim.getEyeLocation().getY();
            boolean headshot = Math.abs(hitY - eyeY) <= 0.34;
            double dmg = headshot ? baseDamage * 1.55 : baseDamage;
            victim.damage(dmg, p);
            if ("incendiary".equals(ammoMode)) {
                victim.setFireTicks(80);
                world.spawnParticle(org.bukkit.Particle.FLAME, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.03);
            } else if ("explosive".equals(ammoMode)) {
                world.spawnParticle(org.bukkit.Particle.EXPLOSION_LARGE, victim.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                world.playSound(victim.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.1f);
            }
            sendToPlayer(p, "HIT|" + (headshot ? "1" : "0"));
        } else if (blockHit != null && blockHit.getHitPosition() != null) {
            Location impact = blockHit.getHitPosition().toLocation(world);
            if ("explosive".equals(ammoMode)) {
                world.spawnParticle(org.bukkit.Particle.EXPLOSION_LARGE, impact, 1, 0, 0, 0, 0);
                world.playSound(impact, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.9f, 1.2f);
            } else {
                world.spawnParticle(org.bukkit.Particle.BLOCK_CRACK, impact, 5, 0.1, 0.1, 0.1, Material.STONE.createBlockData());
            }
        }
    }

    private void handleUseMedical(Player p, String kind) {
        if (p == null || !p.isOnline()) return;
        ItemStack held = p.getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) return;

        boolean isSyringe = "syringe".equals(kind) && held.getType() == Material.POTION;
        boolean isBandage = "bandage".equals(kind) && held.getType() == Material.PAPER;
        if (!isSyringe && !isBandage) return;

        double heal = isSyringe ? 8.0 : 3.0;
        double maxHp = p.getMaxHealth();
        p.setHealth(Math.min(maxHp, p.getHealth() + heal));

        if (p.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            if (held.getAmount() <= 1) {
                p.getInventory().setItemInMainHand(null);
            } else {
                held.setAmount(held.getAmount() - 1);
                p.getInventory().setItemInMainHand(held);
            }
            p.updateInventory();
        }
    }

    private void handleBowShoot(Player p, float rawPower) {
        if (p == null || !p.isOnline()) return;
        String heldId = SpaceRustItemRegistry.getId(p.getInventory().getItemInMainHand());
        if (!"bow".equals(heldId) && p.getInventory().getItemInMainHand().getType() != Material.BOW) return;

        if (!consumeResource(p, Material.ARROW, 1)) return;

        float power = Math.max(0.35f, Math.min(1.0f, rawPower));
        Location eye = p.getEyeLocation();
        org.bukkit.util.Vector dir = eye.getDirection().normalize();
        org.bukkit.entity.Arrow arrow = p.launchProjectile(org.bukkit.entity.Arrow.class, dir.multiply(1.45 + power * 2.15));
        arrow.setCritical(power >= 0.88f);
        arrow.setDamage(2.5 + power * 4.5);
        arrow.setPickupStatus(org.bukkit.entity.AbstractArrow.PickupStatus.ALLOWED);

        org.bukkit.World world = eye.getWorld();
        if (world != null) {
            for (Player other : world.getPlayers()) {
                if (!other.getUniqueId().equals(p.getUniqueId()) && other.getLocation().distanceSquared(eye) <= 32.0 * 32.0) {
                    other.playSound(eye, Sound.ENTITY_ARROW_SHOOT, 0.85f, 0.95f + power * 0.25f);
                }
            }
        }
    }
}
