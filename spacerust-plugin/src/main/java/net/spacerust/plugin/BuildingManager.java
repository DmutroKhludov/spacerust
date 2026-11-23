package net.spacerust.plugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/**
 * Manages player-built 3D Rust bases (Twig -> Wood -> Stone -> Metal -> Armored/HQM):
 * - Grid-snapped Building Plan placements
 * - Hammer (Киянка) upgrades, rotation, repair & demolition
 * - Deployables & door placement on 3D foundations/floors
 * - Persistent storage in plugins/SpaceRustCore/buildings.yml
 */
public class BuildingManager implements Listener {

    public static class BuildingPiece {
        public final int id;
        public String type; // foundation, foundation_tri, wall, doorway, window, floor, floor_tri, halfwall, lowwall, roof, stairs
        public String tier; // twig, wood, stone, metal, armored
        public final String worldName;
        public double x, y, z;
        public float yaw;
        public int hp, maxHp;
        public final UUID owner;
        public final String ownerName;
        public final long placedAtMs;

        public BuildingPiece(int id, String type, String tier, String worldName,
                             double x, double y, double z, float yaw,
                             int hp, int maxHp, UUID owner, String ownerName, long placedAtMs) {
            this.id = id;
            this.type = type;
            this.tier = tier;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.hp = hp;
            this.maxHp = maxHp;
            this.owner = owner;
            this.ownerName = ownerName;
            this.placedAtMs = placedAtMs;
        }
    }

    private static final Set<String> VALID_TYPES = new HashSet<>(Arrays.asList(
            "foundation", "foundation_tri", "wall", "doorway", "window",
            "floor", "floor_tri", "halfwall", "lowwall", "roof", "stairs"
    ));

    private static final Set<String> VALID_TIERS = new HashSet<>(Arrays.asList(
            "twig", "wood", "stone", "metal", "armored"
    ));

    private static final Set<Material> ALLOWED_DEPLOYABLES = new HashSet<>(Arrays.asList(
            Material.OAK_DOOR, Material.IRON_DOOR, Material.CHEST, Material.BARREL,
            Material.FURNACE, Material.CAMPFIRE, Material.WHITE_BED, Material.CRAFTING_TABLE,
            Material.SMITHING_TABLE, Material.TORCH, Material.LADDER, Material.OAK_FENCE,
            Material.OAK_FENCE_GATE, Material.IRON_BARS, Material.OAK_TRAPDOOR,
            Material.LEVER, Material.TRIPWIRE_HOOK, Material.DAYLIGHT_DETECTOR, Material.NOTE_BLOCK
    ));

    private final SpaceRustPlugin plugin;
    private final List<BuildingPiece> pieces = new ArrayList<>();
    private final Map<Integer, BuildingPiece> byId = new HashMap<>();
    private int nextId = 1;
    private CraftingAndSyncManager syncManager;

    public BuildingManager(SpaceRustPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void setSyncManager(CraftingAndSyncManager syncManager) {
        this.syncManager = syncManager;
    }

    public static int tierRank(String tier) {
        if ("wood".equals(tier)) return 1;
        if ("stone".equals(tier)) return 2;
        if ("metal".equals(tier)) return 3;
        if ("armored".equals(tier)) return 4;
        return 0; // twig
    }

    public static int maxHpForTier(String tier) {
        if ("wood".equals(tier)) return 250;
        if ("stone".equals(tier)) return 500;
        if ("metal".equals(tier)) return 1000;
        if ("armored".equals(tier)) return 2000;
        return 10; // twig
    }

    public static Material costMaterialForTier(String tier) {
        if ("stone".equals(tier)) return Material.COBBLESTONE;
        if ("metal".equals(tier)) return Material.IRON_INGOT;
        if ("armored".equals(tier)) return Material.NETHERITE_INGOT;
        return Material.OAK_PLANKS; // twig or wood
    }

    public static int costAmount(String type, String tier) {
        boolean small = "halfwall".equals(type) || "lowwall".equals(type)
                || "foundation_tri".equals(type) || "floor_tri".equals(type);
        if ("wood".equals(tier)) return small ? 100 : 200;
        if ("stone".equals(tier)) return small ? 150 : 300;
        if ("metal".equals(tier)) return small ? 100 : 200;
        if ("armored".equals(tier)) return small ? 13 : 25;
        return small ? 25 : 50; // twig
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE) return;
        Material m = e.getBlockPlaced().getType();
        if (!ALLOWED_DEPLOYABLES.contains(m)) {
            e.setCancelled(true);
        }
    }

    public void load() {
        pieces.clear();
        byId.clear();
        File file = new File(plugin.getDataFolder(), "buildings.yml");
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = cfg.getConfigurationSection("pieces");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            int id = s.getInt("id", nextId);
            String type = s.getString("type", "foundation");
            String tier = s.getString("tier", "twig");
            String worldName = s.getString("world", "world");
            double x = s.getDouble("x");
            double y = s.getDouble("y");
            double z = s.getDouble("z");
            float yaw = (float) s.getDouble("yaw", 0.0);
            int maxHp = maxHpForTier(tier);
            int hp = Math.max(1, Math.min(maxHp, s.getInt("hp", maxHp)));
            UUID owner = null;
            try { owner = UUID.fromString(s.getString("owner", "")); } catch (Exception ignored) {}
            String ownerName = s.getString("ownerName", "Player");
            long placedAt = s.getLong("placedAt", System.currentTimeMillis());

            BuildingPiece bp = new BuildingPiece(id, type, tier, worldName, x, y, z, yaw, hp, maxHp, owner, ownerName, placedAt);
            pieces.add(bp);
            byId.put(bp.id, bp);
            if (id >= nextId) nextId = id + 1;
        }
        plugin.getLogger().info("[SpaceRustCore] Loaded " + pieces.size() + " building pieces.");
    }

    public void save() {
        File file = new File(plugin.getDataFolder(), "buildings.yml");
        YamlConfiguration cfg = new YamlConfiguration();
        for (BuildingPiece bp : pieces) {
            String path = "pieces.p" + bp.id;
            cfg.set(path + ".id", bp.id);
            cfg.set(path + ".type", bp.type);
            cfg.set(path + ".tier", bp.tier);
            cfg.set(path + ".world", bp.worldName);
            cfg.set(path + ".x", bp.x);
            cfg.set(path + ".y", bp.y);
            cfg.set(path + ".z", bp.z);
            cfg.set(path + ".yaw", bp.yaw);
            cfg.set(path + ".hp", bp.hp);
            cfg.set(path + ".owner", bp.owner != null ? bp.owner.toString() : "");
            cfg.set(path + ".ownerName", bp.ownerName);
            cfg.set(path + ".placedAt", bp.placedAtMs);
        }
        try {
            cfg.save(file);
        } catch (Exception ex) {
            plugin.getLogger().warning("[SpaceRustCore] Failed saving buildings.yml: " + ex.getMessage());
        }
    }

    public void sendBuildings(Player p) {
        if (p == null || syncManager == null) return;
        Location pl = p.getLocation();
        String wName = pl.getWorld() != null ? pl.getWorld().getName() : "world";
        StringBuilder sb = new StringBuilder("BUILD_SYNC|");
        int count = 0;
        for (BuildingPiece bp : pieces) {
            if (!bp.worldName.equals(wName)) continue;
            double dx = bp.x - pl.getX();
            double dz = bp.z - pl.getZ();
            if (dx * dx + dz * dz <= 115.0 * 115.0) {
                sb.append(bp.id).append(",")
                  .append(bp.type).append(",")
                  .append(bp.tier).append(",")
                  .append(String.format(Locale.US, "%.2f,%.2f,%.2f,%.1f", bp.x, bp.y, bp.z, bp.yaw)).append(",")
                  .append(bp.hp).append(",")
                  .append(bp.maxHp).append(",")
                  .append(bp.ownerName.replace(",", "").replace(";", ""))
                  .append(";");
                if (++count >= 240) break;
            }
        }
        syncManager.sendToPlayer(p, sb.toString());
    }

    public void broadcastBuildings() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendBuildings(p);
        }
    }

    public void handlePlace(Player p, String type, double x, double y, double z, float yaw) {
        if (p == null || !VALID_TYPES.contains(type)) return;
        ItemStack held = p.getInventory().getItemInMainHand();
        if (p.getGameMode() != GameMode.CREATIVE && (held == null || held.getType() != Material.MAP)) {
            return;
        }
        Location pl = p.getEyeLocation();
        double distSq = Math.pow(x - pl.getX(), 2) + Math.pow(y - pl.getY(), 2) + Math.pow(z - pl.getZ(), 2);
        if (distSq > 9.5 * 9.5) return;

        String wName = p.getWorld().getName();
        boolean isTri = type.endsWith("_tri");
        double cxNew = isTri ? (x + 0.8660254 * Math.sin(Math.toRadians(yaw))) : x;
        double czNew = isTri ? (z + 0.8660254 * Math.cos(Math.toRadians(yaw))) : z;
        for (BuildingPiece ex : pieces) {
            if (!ex.worldName.equals(wName)) continue;
            if (ex.type.equals(type)) {
                double exCx = isTri ? (ex.x + 0.8660254 * Math.sin(Math.toRadians(ex.yaw))) : ex.x;
                double exCz = isTri ? (ex.z + 0.8660254 * Math.cos(Math.toRadians(ex.yaw))) : ex.z;
                if (Math.abs(exCx - cxNew) < 0.35 && Math.abs(ex.y - y) < 0.35 && Math.abs(exCz - czNew) < 0.35) {
                    return;
                }
            }
        }

        int cost = costAmount(type, "twig");
        Material mat = Material.OAK_PLANKS;
        if (p.getGameMode() != GameMode.CREATIVE && syncManager != null) {
            if (!syncManager.consumeResource(p, mat, cost)) {
                return;
            }
        }

        BuildingPiece bp = new BuildingPiece(
                nextId++, type, "twig", wName,
                x, y, z, normalizeYaw(yaw),
                10, 10, p.getUniqueId(), p.getName(), System.currentTimeMillis()
        );
        pieces.add(bp);
        byId.put(bp.id, bp);
        save();

        p.getWorld().spawnParticle(Particle.BLOCK_CRACK, x, y + 0.5, z, 14, 0.45, 0.35, 0.45, Material.OAK_PLANKS.createBlockData());
        broadcastFx(p.getWorld(), x, y, z, "build_place");
        broadcastBuildings();
    }

    public void handleUpgrade(Player p, int id, String targetTier) {
        BuildingPiece bp = byId.get(id);
        if (p == null || bp == null || !VALID_TIERS.contains(targetTier)) return;
        ItemStack held = p.getInventory().getItemInMainHand();
        if (p.getGameMode() != GameMode.CREATIVE && (held == null || held.getType() != Material.WOODEN_AXE)) {
            return;
        }
        if (p.getEyeLocation().distanceSquared(new Location(p.getWorld(), bp.x, bp.y + 0.8, bp.z)) > 7.5 * 7.5) {
            return;
        }
        if (tierRank(targetTier) <= tierRank(bp.tier)) return;

        int cost = costAmount(bp.type, targetTier);
        Material mat = costMaterialForTier(targetTier);
        if (p.getGameMode() != GameMode.CREATIVE && syncManager != null) {
            if (!syncManager.consumeResource(p, mat, cost)) {
                return;
            }
        }

        bp.tier = targetTier;
        bp.maxHp = maxHpForTier(targetTier);
        bp.hp = bp.maxHp;
        save();

        String sfx = "build_wood_upgrade";
        Material pMat = Material.OAK_PLANKS;
        if ("stone".equals(targetTier)) { sfx = "build_stone_upgrade"; pMat = Material.COBBLESTONE; }
        else if ("metal".equals(targetTier)) { sfx = "build_metal_upgrade"; pMat = Material.IRON_BLOCK; }
        else if ("armored".equals(targetTier)) { sfx = "build_hqm_upgrade"; pMat = Material.NETHERITE_BLOCK; }

        p.getWorld().spawnParticle(Particle.BLOCK_CRACK, bp.x, bp.y + 0.7, bp.z, 18, 0.5, 0.5, 0.5, pMat.createBlockData());
        broadcastFx(p.getWorld(), bp.x, bp.y, bp.z, sfx);
        broadcastBuildings();
    }

    public void handleRotate(Player p, int id) {
        BuildingPiece bp = byId.get(id);
        if (p == null || bp == null) return;
        boolean isOwner = (bp.owner == null) || bp.owner.equals(p.getUniqueId())
                || (bp.ownerName != null && bp.ownerName.equalsIgnoreCase(p.getName()));
        if (!isOwner && !p.isOp()) return;

        if ("wall".equals(bp.type) || "doorway".equals(bp.type) || "window".equals(bp.type)
                || "halfwall".equals(bp.type) || "lowwall".equals(bp.type)) {
            // Flip walls 180 degrees in-place (swap inner soft-side & outer hard-side!)
            bp.yaw = normalizeYaw(bp.yaw + 180.0f);
        } else if (bp.type.endsWith("_tri")) {
            // Rotate triangles 120 degrees around their centroid so footprint stays locked in socket
            double cx = bp.x + 0.8660254 * Math.sin(Math.toRadians(bp.yaw));
            double cz = bp.z + 0.8660254 * Math.cos(Math.toRadians(bp.yaw));
            bp.yaw = normalizeYaw(bp.yaw + 120.0f);
            bp.x = cx - 0.8660254 * Math.sin(Math.toRadians(bp.yaw));
            bp.z = cz - 0.8660254 * Math.cos(Math.toRadians(bp.yaw));
        } else {
            // Square floor/ceiling, roof, stairs, foundation rotate 90 degrees around center
            bp.yaw = normalizeYaw(bp.yaw + 90.0f);
        }
        save();
        broadcastFx(p.getWorld(), bp.x, bp.y, bp.z, "build_repair");
        broadcastBuildings();
    }

    public void handleDemolish(Player p, int id) {
        BuildingPiece bp = byId.get(id);
        if (p == null || bp == null) return;
        if (bp.owner != null && !bp.owner.equals(p.getUniqueId()) && !p.isOp()) return;
        pieces.remove(bp);
        byId.remove(id);
        save();
        p.getWorld().spawnParticle(Particle.BLOCK_CRACK, bp.x, bp.y + 0.7, bp.z, 20, 0.5, 0.5, 0.5, Material.OAK_PLANKS.createBlockData());
        broadcastFx(p.getWorld(), bp.x, bp.y, bp.z, "build_destroy");
        broadcastBuildings();
    }

    public void handleRepair(Player p, int id) {
        BuildingPiece bp = byId.get(id);
        if (p == null || bp == null || bp.hp >= bp.maxHp) return;
        Material mat = costMaterialForTier(bp.tier);
        int repairCost = Math.max(5, costAmount(bp.type, bp.tier) / 8);
        if (p.getGameMode() != GameMode.CREATIVE && syncManager != null) {
            if (!syncManager.consumeResource(p, mat, repairCost)) {
                return;
            }
        }
        bp.hp = Math.min(bp.maxHp, bp.hp + Math.max(20, bp.maxHp / 4));
        save();
        broadcastFx(p.getWorld(), bp.x, bp.y, bp.z, "build_repair");
        broadcastBuildings();
    }

    public void handleDeployPlace(Player p, int bx, int by, int bz, float yaw) {
        if (p == null) return;
        ItemStack held = p.getInventory().getItemInMainHand();
        if (held == null || !ALLOWED_DEPLOYABLES.contains(held.getType())) return;
        World w = p.getWorld();
        Location pl = p.getEyeLocation();
        if (pl.distanceSquared(new Location(w, bx + 0.5, by + 0.5, bz + 0.5)) > 7.0 * 7.0) return;

        Material m = held.getType();
        Block target = w.getBlockAt(bx, by, bz);
        if (!target.getType().isAir() && target.getType() != Material.GRASS && target.getType() != Material.TALL_GRASS) {
            return;
        }
        if (m == Material.OAK_DOOR || m == Material.IRON_DOOR) {
            Block top = w.getBlockAt(bx, by + 1, bz);
            if (!top.getType().isAir()) return;
            BlockFace facing = yawToFace(yaw);
            target.setType(m, false);
            Door bottomData = (Door) Bukkit.createBlockData(m);
            bottomData.setHalf(org.bukkit.block.data.Bisected.Half.BOTTOM);
            bottomData.setFacing(facing);
            target.setBlockData(bottomData, false);

            top.setType(m, false);
            Door topData = (Door) Bukkit.createBlockData(m);
            topData.setHalf(org.bukkit.block.data.Bisected.Half.TOP);
            topData.setFacing(facing);
            top.setBlockData(topData, false);
        } else {
            target.setType(m, false);
        }

        if (p.getGameMode() != GameMode.CREATIVE) {
            if (held.getAmount() <= 1) p.getInventory().setItemInMainHand(null);
            else {
                held.setAmount(held.getAmount() - 1);
                p.getInventory().setItemInMainHand(held);
            }
            p.updateInventory();
        }
        broadcastFx(w, bx + 0.5, by + 0.5, bz + 0.5, "build_place");
    }

    private void broadcastFx(World world, double x, double y, double z, String sfx) {
        if (syncManager == null) return;
        String msg = String.format(Locale.US, "BUILD_FX|%s|%.2f|%.2f|%.2f", sfx, x, y, z);
        for (Player other : world.getPlayers()) {
            if (other.getLocation().distanceSquared(new Location(world, x, y, z)) <= 48.0 * 48.0) {
                syncManager.sendToPlayer(other, msg);
            }
        }
    }

    private static float normalizeYaw(float y) {
        float r = y % 360.0f;
        return r < 0 ? r + 360.0f : r;
    }

    private static BlockFace yawToFace(float yaw) {
        float y = normalizeYaw(yaw);
        if (y >= 45 && y < 135) return BlockFace.WEST;
        if (y >= 135 && y < 225) return BlockFace.NORTH;
        if (y >= 225 && y < 315) return BlockFace.EAST;
        return BlockFace.SOUTH;
    }
}
