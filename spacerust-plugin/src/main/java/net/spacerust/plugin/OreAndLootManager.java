package net.spacerust.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Manages Rust Ore Nodes (Stone/Metal/Sulfur 4 stages + hotspot),
 * roadside Barrels, tiered RT Loot Crates, and functional Recyclers.
 */
public class OreAndLootManager implements Listener {

    public static class OreNodeState {
        public final int id;
        public final String kind; // stone, metal, sulfur
        public final World world;
        public final int x, y, z;
        public int hp = 100;
        public double exactHp = 100.0;
        public double yieldCarry = 0.0;
        public int stage = 1; // 1..4
        public float hx, hy, hz;
        public int streak = 0;
        public long respawnAtMs = 0;

        public OreNodeState(int id, String kind, World world, int x, int y, int z, Random rng) {
            this.id = id;
            this.kind = kind;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            randomizeHotspot(rng, null);
        }

        public boolean isActive() {
            return respawnAtMs == 0 && hp > 0;
        }

        public static float getSurfaceRadius(int st) {
            if (st == 1) return 1.24f;
            if (st == 2) return 1.10f;
            if (st == 3) return 0.96f;
            return 0.80f;
        }

        public static float getSurfaceMaxY(int st) {
            if (st == 1) return 1.48f;
            if (st == 2) return 1.12f;
            if (st == 3) return 0.92f;
            return 0.55f;
        }

        public void randomizeHotspot(Random rng, Player facing) {
            double baseAng;
            if (facing != null) {
                double toPlayer = Math.atan2(facing.getLocation().getZ() - (z + 0.5),
                        facing.getLocation().getX() - (x + 0.5));
                double prevAng = Math.atan2(hz, hx);
                double ang = toPlayer + (rng.nextDouble() - 0.5) * 1.75; // +-50 deg facing miner
                // Shift noticeably away from previous hit spot on the boulder face
                if (Math.abs(Math.atan2(Math.sin(ang - prevAng), Math.cos(ang - prevAng))) < 0.35) {
                    ang += (rng.nextBoolean() ? 0.55 : -0.55);
                }
                baseAng = ang;
            } else {
                baseAng = rng.nextDouble() * Math.PI * 2.0;
            }
            float maxY = getSurfaceMaxY(stage);
            float hRatio = 0.34f + rng.nextFloat() * 0.40f;
            this.hy = Math.max(0.36f, maxY * hRatio);
            float taper = (float) Math.sqrt(Math.max(0.35, 1.0 - Math.pow(hRatio, 2) * 0.45));
            float surfR = getSurfaceRadius(stage) * taper + 0.11f;
            this.hx = (float) (Math.cos(baseAng) * surfR);
            this.hz = (float) (Math.sin(baseAng) * surfR);
        }
    }

    public static class RustTreeState {
        public final int id;
        public final String kind; // pine, birch, oak
        public final World world;
        public final int x, y, z;
        public final float radius;
        public final float height;
        public int hp = 100;
        public double exactHp = 100.0;
        public double yieldCarry = 0.0;
        public float xAngle;
        public float xY;
        public int streak = 0;
        public long respawnAtMs = 0;

        public RustTreeState(int id, String kind, World world, int x, int y, int z,
                             float radius, float height, Random rng) {
            this.id = id;
            this.kind = kind;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.height = height;
            this.xAngle = (float) (rng.nextDouble() * Math.PI * 2.0);
            this.xY = 1.05f + rng.nextFloat() * 0.45f;
        }

        public boolean isActive() {
            return respawnAtMs == 0 && hp > 0;
        }

        public void moveXMarker(Random rng, Player facing) {
            double shift = (0.70 + rng.nextDouble() * 0.55) * (rng.nextBoolean() ? 1.0 : -1.0); // 40..72 deg
            double next = this.xAngle + shift;
            if (facing != null) {
                double toPlayer = Math.atan2(facing.getLocation().getZ() - (z + 0.5),
                        facing.getLocation().getX() - (x + 0.5));
                double diff = Math.atan2(Math.sin(next - toPlayer), Math.cos(next - toPlayer));
                if (Math.abs(diff) > 1.35) {
                    next = toPlayer + (diff > 0 ? 0.95 : -0.95);
                }
            }
            this.xAngle = (float) next;
            this.xY = 0.85f + rng.nextFloat() * 0.75f;
        }
    }

    private final SpaceRustPlugin plugin;
    private final MonumentBuilder monuments;
    private CraftingAndSyncManager syncManager;
    public static NamespacedKey RCOUNT_KEY;
    private final Map<String, OreNodeState> nodeByBlock = new HashMap<>();
    private final Map<Integer, OreNodeState> nodeById = new HashMap<>();
    private final List<OreNodeState> allNodes = new ArrayList<>();
    private final Map<Integer, RustTreeState> treeById = new HashMap<>();
    private final Map<String, RustTreeState> vanillaTreeByBase = new HashMap<>();
    private final List<RustTreeState> allTrees = new ArrayList<>();
    private int nextVanillaTreeId = 100000;

    public void setSyncManager(CraftingAndSyncManager syncManager) {
        this.syncManager = syncManager;
    }
    private final Map<String, Long> barrelRespawns = new HashMap<>();
    private final Map<String, Long> crateLastFilled = new HashMap<>();
    private final Map<String, Long> crateRespawnAt = new HashMap<>();
    private final Map<String, MonumentBuilder.CrateSpot> crateByKey = new HashMap<>();
    private final Set<String> recyclerKeys = new HashSet<>();
    private final Map<UUID, Inventory> openRecyclers = new HashMap<>();
    private final Map<UUID, Long> lastFarmHitAt = new HashMap<>();
    private final Random rng = new Random();

    public OreAndLootManager(SpaceRustPlugin plugin, MonumentBuilder monuments) {
        this.plugin = plugin;
        this.monuments = monuments;
        RCOUNT_KEY = new NamespacedKey(plugin, "rcount");
        SpaceRustItemRegistry.init(plugin);
    }

    public List<OreNodeState> getAllNodes() {
        return allNodes;
    }

    public List<RustTreeState> getAllTrees() {
        return allTrees;
    }

    public void initNodesAndLoot(World world) {
        recyclerKeys.clear();
        for (Location l : monuments.getRecyclerLocations()) {
            recyclerKeys.add(blockKey(l));
        }

        crateByKey.clear();
        for (MonumentBuilder.CrateSpot spot : monuments.getCrateSpots()) {
            String k = blockKey(spot.loc);
            crateByKey.put(k, spot);
            refillCrate(spot);
        }

        for (Location bl : monuments.getBarrelSpots()) {
            spawnBarrelAt(bl);
        }

        if (allNodes.isEmpty() || allTrees.isEmpty()) {
            respawnAllOresAndTrees(world, allNodes.isEmpty(), allTrees.isEmpty());
        }
        purgeHostileMobs(world);
    }

    private boolean isHostileMob(Entity e) {
        return e instanceof Monster || e instanceof Slime || e instanceof Phantom
                || e instanceof Ghast || e instanceof Shulker;
    }

    public void purgeHostileMobs(World world) {
        for (Entity ent : world.getEntities()) {
            if (isHostileMob(ent)) ent.remove();
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (isHostileMob(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        for (Entity ent : e.getChunk().getEntities()) {
            if (isHostileMob(ent)) ent.remove();
        }
        int cx = e.getChunk().getX();
        int cz = e.getChunk().getZ();
        World w = e.getWorld();
        long now = System.currentTimeMillis();
        for (MonumentBuilder.CrateSpot spot : crateByKey.values()) {
            if (spot.loc == null || spot.loc.getWorld() != w) continue;
            if ((spot.loc.getBlockX() >> 4) != cx || (spot.loc.getBlockZ() >> 4) != cz) continue;
            String key = blockKey(spot.loc);
            long respawnAt = crateRespawnAt.getOrDefault(key, 0L);
            if (respawnAt > 0L) {
                if (now >= respawnAt) {
                    crateRespawnAt.remove(key);
                    refillCrate(spot);
                }
                continue;
            }
            long last = crateLastFilled.getOrDefault(key, 0L);
            if (last == 0L || now - last >= 240_000L) {
                refillCrate(spot);
            }
        }
        for (Location bl : monuments.getBarrelSpots()) {
            if (bl == null || bl.getWorld() != w) continue;
            if ((bl.getBlockX() >> 4) != cx || (bl.getBlockZ() >> 4) != cz) continue;
            String key = blockKey(bl);
            long respawnAt = barrelRespawns.getOrDefault(key, 0L);
            if (respawnAt == 0L || now >= respawnAt) {
                barrelRespawns.remove(key);
                spawnBarrelAt(bl);
            }
        }
        for (OreNodeState node : allNodes) {
            if (node.world == w && (node.x >> 4) == cx && (node.z >> 4) == cz) {
                placeNodeCore(node);
            }
        }
    }

    private boolean initialSpawnRunning = false;

    public void respawnAllOres(World world) {
        respawnAllOresAndTrees(world, true, false);
    }

    public void respawnAllTrees(World world) {
        respawnAllOresAndTrees(world, false, true);
    }

    public void respawnAllOresAndTrees(World world, boolean doOres, boolean doTrees) {
        if (doOres) {
            for (OreNodeState old : allNodes) {
                if (world.isChunkLoaded(old.x >> 4, old.z >> 4)) {
                    Block b = world.getBlockAt(old.x, old.y, old.z);
                    if (isOreCoreMat(b.getType())) b.setType(Material.AIR, false);
                }
            }
            allNodes.clear();
            nodeByBlock.clear();
            nodeById.clear();
        }
        if (doTrees) {
            allTrees.clear();
            treeById.clear();
        }
        startBatchedSpawn(world, doOres, doTrees);
    }

    private void startBatchedSpawn(World world, boolean doOres, boolean doTrees) {
        if (initialSpawnRunning) return;
        final int[] oreAttempts = {0};
        final int[] treeAttempts = {0};
        final int[] tickCount = {0};
        final int[] oreId = {allNodes.size() + 1};
        final int[] treeId = {allTrees.size() + 1};
        final boolean[] needOres = {doOres};
        final boolean[] needTrees = {doTrees};
        final Set<Long> queuedChunks = new HashSet<>();
        initialSpawnRunning = true;

        new org.bukkit.scheduler.BukkitRunnable() {
            private void enqueueChunkOnce(int cx, int cz, int[] tickQueues) {
                if (tickQueues[0] >= 6) return;
                long key = (((long) cx) << 32) ^ (cz & 0xffffffffL);
                if (queuedChunks.add(key)) {
                    world.getChunkAtAsync(cx, cz);
                    tickQueues[0]++;
                }
            }

            private int[] pickCoords(org.bukkit.Chunk[] loaded, int[] tickQueues) {
                double angle = rng.nextDouble() * Math.PI * 2.0;
                double dist = 16 + rng.nextDouble() * 230;
                int x = (int) Math.round(Math.cos(angle) * dist);
                int z = (int) Math.round(Math.sin(angle) * dist);
                if (world.isChunkLoaded(x >> 4, z >> 4)) {
                    return new int[]{x, z};
                }
                enqueueChunkOnce(x >> 4, z >> 4, tickQueues);
                if (loaded != null && loaded.length > 0) {
                    org.bukkit.Chunk c = loaded[rng.nextInt(loaded.length)];
                    int lx = (c.getX() << 4) + rng.nextInt(16);
                    int lz = (c.getZ() << 4) + rng.nextInt(16);
                    double d2 = lx * (double) lx + lz * (double) lz;
                    if (d2 >= 16.0 * 16.0 && d2 <= 250.0 * 250.0) {
                        return new int[]{lx, lz};
                    }
                }
                return null;
            }

            @Override
            public void run() {
                tickCount[0]++;
                long deadlineNs = System.nanoTime() + 15_000_000L; // <=15ms per tick to avoid Watchdog stalls
                org.bukkit.Chunk[] loaded = world.getLoadedChunks();
                int[] tickQueues = {0};
                int oreSkips = 0;
                int treeSkips = 0;

                while (needOres[0] && allNodes.size() < 150 && oreAttempts[0] < 3600 && oreSkips < 64 && System.nanoTime() < deadlineNs) {
                    int[] xz = pickCoords(loaded, tickQueues);
                    if (xz == null) {
                        oreSkips++;
                        continue;
                    }
                    int x = xz[0], z = xz[1];
                    oreAttempts[0]++;
                    if (monuments.isInsideAnyMonument(x, z, 6)) continue;

                    int surfaceY = MonumentBuilder.findGroundSurfaceY(world, x, z);
                    if (surfaceY < 62 || surfaceY > 120) continue;
                    Material ground = world.getBlockAt(x, surfaceY, z).getType();
                    if (ground == Material.WATER || ground == Material.LAVA || !ground.isSolid()) continue;
                    int y = surfaceY + 1;
                    Material cur = world.getBlockAt(x, y, z).getType();
                    if (!cur.isAir() && cur.isSolid()) continue;

                    String kind;
                    int roll = rng.nextInt(10);
                    if (roll < 4) kind = "stone";
                    else if (roll < 7) kind = "metal";
                    else kind = "sulfur";

                    OreNodeState node = new OreNodeState(oreId[0]++, kind, world, x, y, z, rng);
                    placeNodeCore(node);
                    allNodes.add(node);
                    nodeById.put(node.id, node);
                    nodeByBlock.put(blockKey(world, x, y, z), node);
                }
                if (needOres[0] && (allNodes.size() >= 150 || oreAttempts[0] >= 3600 || tickCount[0] >= 400)) {
                    needOres[0] = false;
                    plugin.getLogger().info("[SpaceRustCore] Spawned " + allNodes.size() + " 3D Rust ore nodes.");
                }

                while (needTrees[0] && allTrees.size() < 160 && treeAttempts[0] < 4500 && treeSkips < 64 && System.nanoTime() < deadlineNs) {
                    int[] xz = pickCoords(loaded, tickQueues);
                    if (xz == null) {
                        treeSkips++;
                        continue;
                    }
                    int x = xz[0], z = xz[1];
                    treeAttempts[0]++;
                    if (monuments.isInsideAnyMonument(x, z, 9)) continue;

                    int surfaceY = MonumentBuilder.findGroundSurfaceY(world, x, z);
                    if (surfaceY < 62 || surfaceY > 120) continue;
                    Material ground = world.getBlockAt(x, surfaceY, z).getType();
                    boolean fertileSoil = (ground == Material.GRASS_BLOCK || ground == Material.DIRT
                            || ground == Material.PODZOL || ground == Material.COARSE_DIRT
                            || (ground == Material.SAND && rng.nextInt(5) == 0));
                    if (!fertileSoil) continue;
                    int y = surfaceY + 1;
                    Material cur = world.getBlockAt(x, y, z).getType();
                    if (!cur.isAir() && cur.isSolid()) continue;

                    boolean tooClose = false;
                    for (OreNodeState on : allNodes) {
                        double dx = on.x - x, dz = on.z - z;
                        if (dx * dx + dz * dz < 14.0) { tooClose = true; break; }
                    }
                    if (tooClose) continue;
                    for (RustTreeState tr : allTrees) {
                        double dx = tr.x - x, dz = tr.z - z;
                        if (dx * dx + dz * dz < 22.0) { tooClose = true; break; }
                    }
                    if (tooClose) continue;

                    int roll = rng.nextInt(10);
                    String kind;
                    float radius;
                    float height;
                    if (roll < 5) {
                        kind = "pine"; radius = 0.50f; height = 7.4f;
                    } else if (roll < 8) {
                        kind = "birch"; radius = 0.44f; height = 6.5f;
                    } else {
                        kind = "oak"; radius = 0.56f; height = 6.1f;
                    }
                    RustTreeState tree = new RustTreeState(treeId[0]++, kind, world, x, y, z, radius, height, rng);
                    allTrees.add(tree);
                    treeById.put(tree.id, tree);
                }
                if (needTrees[0] && (allTrees.size() >= 160 || treeAttempts[0] >= 4500 || tickCount[0] >= 400)) {
                    needTrees[0] = false;
                    plugin.getLogger().info("[SpaceRustCore] Spawned " + allTrees.size() + " 3D Rust trees.");
                }

                if (!needOres[0] && !needTrees[0]) {
                    initialSpawnRunning = false;
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static boolean isChunkReady(Location loc) {
        return loc != null && loc.getWorld() != null
                && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    private void placeNodeCore(OreNodeState node) {
        // Leave air so smaller stage meshes never poke vanilla blocks; physics & raycast handled smoothly
        if (node.world == null || !node.world.isChunkLoaded(node.x >> 4, node.z >> 4)) return;
        Block b = node.world.getBlockAt(node.x, node.y, node.z);
        if (isOreCoreMat(b.getType())) {
            b.setType(Material.AIR, false);
        }
    }

    private boolean isOreCoreMat(Material m) {
        return m == Material.COBBLESTONE || m == Material.IRON_ORE || m == Material.GOLD_ORE;
    }

    private void spawnBarrelAt(Location loc) {
        if (!isChunkReady(loc)) return;
        Block b = loc.getBlock();
        Material barrelMat = rng.nextBoolean() ? Material.BLUE_TERRACOTTA : Material.BROWN_TERRACOTTA;
        b.setType(barrelMat, false);
    }

    public void tickRespawns() {
        long now = System.currentTimeMillis();
        for (OreNodeState node : allNodes) {
            if (node.respawnAtMs > 0 && now >= node.respawnAtMs) {
                node.respawnAtMs = 0;
                node.hp = 100;
                node.exactHp = 100.0;
                node.yieldCarry = 0.0;
                node.stage = 1;
                node.streak = 0;
                node.randomizeHotspot(rng, null);
                placeNodeCore(node);
            }
        }
        for (RustTreeState tree : allTrees) {
            if (!"vanilla".equals(tree.kind) && tree.respawnAtMs > 0 && now >= tree.respawnAtMs) {
                tree.respawnAtMs = 0;
                tree.hp = 100;
                tree.exactHp = 100.0;
                tree.yieldCarry = 0.0;
                tree.streak = 0;
                tree.xAngle = (float) (rng.nextDouble() * Math.PI * 2.0);
                tree.xY = 1.05f + rng.nextFloat() * 0.45f;
            }
        }
        Iterator<Map.Entry<String, Long>> bi = barrelRespawns.entrySet().iterator();
        while (bi.hasNext()) {
            Map.Entry<String, Long> e = bi.next();
            if (now >= e.getValue()) {
                for (Location bl : monuments.getBarrelSpots()) {
                    if (blockKey(bl).equals(e.getKey())) {
                        spawnBarrelAt(bl);
                        break;
                    }
                }
                bi.remove();
            }
        }
        for (Map.Entry<String, MonumentBuilder.CrateSpot> entry : crateByKey.entrySet()) {
            String key = entry.getKey();
            MonumentBuilder.CrateSpot spot = entry.getValue();
            long respawnAt = crateRespawnAt.getOrDefault(key, 0L);
            if (respawnAt > 0L) {
                if (now >= respawnAt) {
                    crateRespawnAt.remove(key);
                    crateLastFilled.remove(key);
                    refillCrate(spot);
                }
                continue;
            }
            long last = crateLastFilled.getOrDefault(key, 0L);
            if (last > 0L && now - last >= 240_000L && isChunkReady(spot.loc)) {
                refillCrate(spot);
            }
        }
    }

    private void refillCrate(MonumentBuilder.CrateSpot spot) {
        if (!isChunkReady(spot.loc)) return;
        Block b = spot.loc.getBlock();
        if (b.getType() != Material.CHEST) b.setType(Material.CHEST, false);
        if (!(b.getState() instanceof Chest)) return;
        Chest chest = (Chest) b.getState();
        chest.setCustomName(getCrateDisplayName(spot.tier));
        chest.update(true, false);
        Inventory inv = chest.getBlockInventory();
        inv.clear();

        if (spot.tier == MonumentBuilder.LootTier.ELITE) {
            int er = rng.nextInt(4);
            if (er == 0) inv.setItem(10, named(Material.NETHERITE_SWORD, "Автомат AK-47", 1));
            else if (er == 1) inv.setItem(10, named(Material.IRON_SWORD, "Винтовка LR-300", 1));
            else if (er == 2) inv.setItem(10, named(Material.STONE_SWORD, "Снайперка L96", 1));
            else inv.setItem(10, named(Material.WOODEN_SWORD, "Винтовка M39", 1));
            inv.setItem(11, named(Material.IRON_NUGGET, "Патроны 5.56мм", 18 + rng.nextInt(13)));
            inv.setItem(12, named(Material.POTION, "Медицинский шприц", 2));
            inv.setItem(13, named(Material.EMERALD, "Скрап", 25 + rng.nextInt(16)));
            inv.setItem(14, named(Material.DIAMOND_CHESTPLATE, "Костюм химзащиты (Хазмат)", 1));
            inv.setItem(15, named(Material.REDSTONE_BLOCK, "Взрывчатка C4", 1));
            inv.setItem(16, named(Material.NETHERITE_INGOT, "МВК", 12 + rng.nextInt(9)));
        } else if (spot.tier == MonumentBuilder.LootTier.MILITARY) {
            int mr = rng.nextInt(6);
            if (mr == 0) inv.setItem(10, named(Material.IRON_SHOVEL, "Пистолет-пулемёт MP5", 1));
            else if (mr == 1) inv.setItem(10, named(Material.DIAMOND_SWORD, "Томпсон", 1));
            else if (mr == 2) inv.setItem(10, named(Material.GOLDEN_SHOVEL, "Берданка (SAR)", 1));
            else if (mr == 3) inv.setItem(10, named(Material.DIAMOND_SHOVEL, "Помповый дробовик", 1));
            else if (mr == 4) inv.setItem(10, named(Material.GOLDEN_HOE, "Револьвер Python", 1));
            else inv.setItem(10, named(Material.STONE_HOE, "Пистолет P250", 1));
            inv.setItem(11, named(Material.GOLD_NUGGET, "Патроны 9мм", 12 + rng.nextInt(9)));
            inv.setItem(12, named(rng.nextBoolean() ? Material.GOLDEN_CHESTPLATE : Material.GOLDEN_HELMET,
                    rng.nextBoolean() ? "Дорожная броня" : "Каска из кофейной банки", 1));
            inv.setItem(13, named(Material.POTION, "Медицинский шприц", 2));
            inv.setItem(14, named(Material.TRIPWIRE_HOOK, "Пружина", 1 + rng.nextInt(2)));
            inv.setItem(15, named(Material.EMERALD, "Скрап", 15 + rng.nextInt(11)));
            inv.setItem(16, named(Material.IRON_INGOT, "Фрагменты металла", 60 + rng.nextInt(40)));
        } else if (spot.tier == MonumentBuilder.LootTier.TOOLS) {
            int tr = rng.nextInt(5);
            if (tr == 0) inv.setItem(10, named(Material.DIAMOND_PICKAXE, "Ледоруб", 1));
            else if (tr == 1) inv.setItem(10, named(Material.DIAMOND_AXE, "Самодельный топор", 1));
            else if (tr == 2) inv.setItem(10, named(Material.GOLDEN_PICKAXE, "Отбойный молоток", 1));
            else if (tr == 3) inv.setItem(10, named(Material.IRON_PICKAXE, "Металлическая кирка", 1));
            else inv.setItem(10, named(Material.IRON_AXE, "Металлический топор", 1));
            inv.setItem(11, named(Material.FEATHER, "Шестерня", 2 + rng.nextInt(2)));
            inv.setItem(13, named(Material.FLINT, "Труба", 2 + rng.nextInt(2)));
            inv.setItem(14, named(Material.IRON_INGOT, "Фрагменты металла", 50 + rng.nextInt(50)));
            inv.setItem(16, named(Material.EMERALD, "Скрап", 10 + rng.nextInt(9)));
        } else if (spot.tier == MonumentBuilder.LootTier.FOOD) {
            inv.setItem(10, named(Material.APPLE, "Яблоко", 3 + rng.nextInt(3)));
            inv.setItem(12, named(Material.COOKED_BEEF, "Жареное мясо", 2 + rng.nextInt(3)));
            inv.setItem(13, named(Material.PAPER, "Бинт", 2 + rng.nextInt(2)));
            inv.setItem(15, named(Material.STRING, "Ткань", 15 + rng.nextInt(16)));
            inv.setItem(16, named(Material.EMERALD, "Скрап", 6 + rng.nextInt(6)));
        } else {
            inv.setItem(11, named(Material.EMERALD, "Скрап", 8 + rng.nextInt(8)));
            inv.setItem(12, named(Material.STRING, "Ткань", 15 + rng.nextInt(15)));
            inv.setItem(14, named(Material.PAPER, "Бинт", 2));
            inv.setItem(15, named(Material.FLINT, "Труба", 1 + rng.nextInt(2)));
        }
        crateLastFilled.put(blockKey(spot.loc), System.currentTimeMillis());
    }

    public static String getCrateDisplayName(MonumentBuilder.LootTier tier) {
        if (tier == MonumentBuilder.LootTier.ELITE) return "Элитный ящик";
        if (tier == MonumentBuilder.LootTier.MILITARY) return "Военный ящик";
        if (tier == MonumentBuilder.LootTier.TOOLS) return "Ящик с инструментами";
        if (tier == MonumentBuilder.LootTier.FOOD) return "Пищевой ящик";
        return "Обычный деревянный ящик";
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) e.getEntity();

        if (e.getDamager() instanceof Player && e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            Player p = (Player) e.getDamager();
            ItemStack held = p.getInventory().getItemInMainHand();
            String id = SpaceRustItemRegistry.getId(held);
            double rustDmg = meleeDamageForItem(id);
            if (rustDmg > 0.0) {
                e.setDamage(rustDmg);
            }
            if (syncManager != null) {
                syncManager.sendToPlayer(p, "HIT|0");
            }
            return;
        }

        if (e.getDamager() instanceof Arrow) {
            Arrow arrow = (Arrow) e.getDamager();
            if (arrow.getShooter() instanceof Player) {
                Player shooter = (Player) arrow.getShooter();
                double dy = Math.abs(arrow.getLocation().getY() - victim.getEyeLocation().getY());
                boolean headshot = dy <= 0.42;
                if (headshot) {
                    e.setDamage(e.getDamage() * 1.45);
                }
                if (syncManager != null) {
                    syncManager.sendToPlayer(shooter, "HIT|" + (headshot ? "1" : "0"));
                }
            }
        }
    }

    private static double meleeDamageForItem(String id) {
        if (id == null) return 0.0;
        switch (id) {
            case "machete": return 10.0;
            case "stone_spear": return 8.5;
            case "salvaged_axe":
            case "salvaged_icepick": return 8.0;
            case "wooden_spear":
            case "bone_knife": return 7.0;
            case "metal_hatchet":
            case "metal_pickaxe": return 7.0;
            case "stone_hatchet":
            case "stone_pickaxe": return 6.0;
            case "rock": return 5.0;
            case "torch": return 3.5;
            case "wood_hammer": return 3.0;
            default: return 0.0;
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        ItemStack inHand = e.getItemInHand();
        if ("torch".equals(SpaceRustItemRegistry.getId(inHand))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Animals)) return;
        e.getDrops().clear();
        String n = e.getEntityType().name();
        boolean large = n.equals("COW") || n.equals("HORSE") || n.equals("POLAR_BEAR") || n.equals("DONKEY") || n.equals("MULE");
        boolean small = n.equals("CHICKEN") || n.equals("RABBIT") || n.equals("OCELOT") || n.equals("CAT") || n.equals("PARROT");
        int meat = small ? 1 : (large ? 3 + rng.nextInt(2) : 2 + rng.nextInt(2));
        int cloth = small ? 3 + rng.nextInt(4) : (large ? 12 + rng.nextInt(8) : 7 + rng.nextInt(6));
        int leather = small ? 2 + rng.nextInt(3) : (large ? 8 + rng.nextInt(6) : 5 + rng.nextInt(4));
        int fat = small ? 2 + rng.nextInt(3) : (large ? 10 + rng.nextInt(6) : 6 + rng.nextInt(5));

        e.getDrops().add(createItem("raw_meat", meat));
        e.getDrops().add(createItem("cloth", cloth));
        e.getDrops().add(createItem("leather", leather));
        e.getDrops().add(createItem("animal_fat", fat));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Player p = e.getPlayer();
        String key = blockKey(b.getLocation());
        if (nodeByBlock.containsKey(key) || crateByKey.containsKey(key) || recyclerKeys.contains(key)) {
            e.setCancelled(true);
            return;
        }
        for (Location bl : monuments.getBarrelSpots()) {
            if (blockKey(bl).equals(key)) {
                e.setCancelled(true);
                breakBarrel(b, p);
                return;
            }
        }
        // In Rust, terrain and world blocks cannot be broken in Survival
        if (p.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        String key = blockKey(b.getLocation());
        Player p = e.getPlayer();

        // Hit an Ore Node (Left Click)
        if (e.getAction() == Action.LEFT_CLICK_BLOCK && nodeByBlock.containsKey(key)) {
            e.setCancelled(true);
            if (!checkFarmCooldown(p)) return;
            OreNodeState node = nodeByBlock.get(key);
            Location hl = p.getEyeLocation().add(p.getLocation().getDirection().multiply(2.0));
            hitOreNode(node, p, hl.getX(), hl.getY(), hl.getZ(), syncManager);
            return;
        }

        // Hit a Road Barrel (Left Click)
        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            for (Location bl : monuments.getBarrelSpots()) {
                if (blockKey(bl).equals(key)) {
                    e.setCancelled(true);
                    breakBarrel(b, p);
                    return;
                }
            }
            if (isTreeLog(b.getType())) {
                e.setCancelled(true);
                if (!checkFarmCooldown(p)) return;
                RustTreeState vTree = findOrRegisterVanillaTree(b, p);
                if (vTree != null) {
                    Location hl = p.getEyeLocation().add(p.getLocation().getDirection().multiply(2.0));
                    double hx = Math.max(b.getX(), Math.min(b.getX() + 1.0, hl.getX()));
                    double hy = Math.max(b.getY(), Math.min(b.getY() + 1.0, hl.getY()));
                    double hz = Math.max(b.getZ(), Math.min(b.getZ() + 1.0, hl.getZ()));
                    hitTree(vTree, p, hx, hy, hz, syncManager);
                }
                return;
            }
        }

        // Open Recycler (Right Click)
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && recyclerKeys.contains(key)) {
            e.setCancelled(true);
            openRecyclerGui(p);
        }
    }

    private boolean checkFarmCooldown(Player p) {
        long now = System.currentTimeMillis();
        long prev = lastFarmHitAt.getOrDefault(p.getUniqueId(), 0L);
        if (now - prev < 230L) {
            return false;
        }
        lastFarmHitAt.put(p.getUniqueId(), now);
        return true;
    }

    public void handleFarmHit(Player p, String kind, int targetId, double hitX, double hitY, double hitZ,
                              CraftingAndSyncManager sync) {
        if (p == null || !checkFarmCooldown(p)) return;
        if ("ore".equals(kind)) {
            OreNodeState node = nodeById.get(targetId);
            if (node == null || !node.isActive() || node.world != p.getWorld()) return;
            Location pl = p.getEyeLocation();
            double dx = pl.getX() - (node.x + 0.5);
            double dy = pl.getY() - (node.y + 0.7);
            double dz = pl.getZ() - (node.z + 0.5);
            if (dx * dx + dy * dy + dz * dz > 6.5 * 6.5) return;
            hitOreNode(node, p, hitX, hitY, hitZ, sync);
        } else if ("tree".equals(kind)) {
            RustTreeState tree = treeById.get(targetId);
            if (tree == null || !tree.isActive() || tree.world != p.getWorld()) return;
            Location pl = p.getEyeLocation();
            double dx = pl.getX() - (tree.x + 0.5);
            double dz = pl.getZ() - (tree.z + 0.5);
            if (dx * dx + dz * dz > 6.2 * 6.2 || Math.abs(pl.getY() - tree.y) > 7.0) return;
            hitTree(tree, p, hitX, hitY, hitZ, sync);
        }
    }

    private static double rustHotspotMultiplier(int streak) {
        // Authentic Rust progression: 1.5x -> 2.0x -> 2.5x -> 3.0x cap
        return Math.min(3.0, 1.0 + 0.5 * Math.max(1, streak));
    }

    private static double oreToolBaseDmg(Material tool) {
        if (tool == Material.GOLDEN_PICKAXE) return 14.0; // Jackhammer: ~3 hits with hotspots
        if (tool == Material.DIAMOND_PICKAXE) return 9.0; // Salvaged Icepick: 5 hits with hotspots (matches Rust)
        if (tool == Material.IRON_PICKAXE) return 6.7;  // ~15 hits normal, 6 hits all hotspots
        if (tool == Material.STONE_PICKAXE) return 4.5; // ~23 hits normal, 8-9 hits all hotspots
        if (tool == Material.STONE_SHOVEL) return 2.8;  // Starter Rock: ~36 hits normal, 13-14 hits hotspots
        return 2.2;
    }

    private static int oreToolPool(Material tool, String kind) {
        boolean maxEfficiency = (tool == Material.IRON_PICKAXE || tool == Material.DIAMOND_PICKAXE || tool == Material.GOLDEN_PICKAXE);
        if ("metal".equals(kind)) {
            if (maxEfficiency) return 600;
            if (tool == Material.STONE_PICKAXE) return 485;
            if (tool == Material.STONE_SHOVEL) return 220;
            return 160;
        }
        if ("sulfur".equals(kind)) {
            if (maxEfficiency) return 300;
            if (tool == Material.STONE_PICKAXE) return 245;
            if (tool == Material.STONE_SHOVEL) return 112;
            return 85;
        }
        // Stone node: Metal Pickaxe / Icepick / Jackhammer 1,000 | Stone Pickaxe 794 | Starter Rock 375
        if (maxEfficiency) return 1000;
        if (tool == Material.STONE_PICKAXE) return 794;
        if (tool == Material.STONE_SHOVEL) return 375;
        return 280;
    }

    private static double treeToolBaseDmg(Material tool) {
        if (tool == Material.DIAMOND_AXE) return 4.6;   // Salvaged Axe: ~22 hits normal, 9-10 hits all X
        if (tool == Material.IRON_AXE) return 3.4;      // Hatchet: ~30 hits normal, 11-12 hits all X
        if (tool == Material.STONE_AXE) return 2.6;     // Stone Hatchet: ~39 hits normal, 15 hits all X
        if (tool == Material.STONE_SHOVEL) return 2.0;  // Starter Rock: ~50 hits normal, 19 hits all X
        return 1.6;
    }

    private static int treeToolPool(Material tool) {
        if (tool == Material.DIAMOND_AXE) return 1000;
        if (tool == Material.IRON_AXE) return 867;
        if (tool == Material.STONE_AXE) return 810;
        if (tool == Material.STONE_SHOVEL) return 375;
        return 240;
    }

    private void hitOreNode(OreNodeState node, Player p, double hitX, double hitY, double hitZ,
                            CraftingAndSyncManager sync) {
        if (!node.isActive()) return;

        ItemStack held = p.getInventory().getItemInMainHand();
        Material tool = held.getType();
        double baseDmg = oreToolBaseDmg(tool);
        int pool = oreToolPool(tool, node.kind);

        // Exact 3D glowing star ("звёздочка") hotspot minigame
        double sx = (node.x + 0.5) + node.hx;
        double sy = node.y + node.hy;
        double sz = (node.z + 0.5) + node.hz;
        double dHx = hitX - sx;
        double dHy = hitY - sy;
        double dHz = hitZ - sz;
        org.bukkit.util.Vector look = p.getEyeLocation().getDirection().normalize();
        org.bukkit.util.Vector toStar = new org.bukkit.util.Vector(
                sx - p.getEyeLocation().getX(),
                sy - p.getEyeLocation().getY(),
                sz - p.getEyeLocation().getZ()
        );
        double lookDot = toStar.lengthSquared() > 0.01 ? look.dot(toStar.normalize()) : 0.0;
        boolean hotspotHit = (dHx * dHx + dHy * dHy + dHz * dHz) <= (0.48 * 0.48) || lookDot >= 0.988;

        double mult;
        if (hotspotHit) {
            node.streak = Math.min(6, node.streak + 1);
            mult = rustHotspotMultiplier(node.streak);
        } else {
            node.streak = 0;
            mult = 1.0;
        }

        double dmg = baseDmg * mult;
        double hpBefore = node.exactHp;
        double hpAfter = Math.max(0.0, hpBefore - dmg);
        double frac = (hpBefore - hpAfter) / 100.0;
        double rawYield = frac * pool + node.yieldCarry;
        int yield = (int) Math.floor(rawYield + 1e-6);
        node.yieldCarry = rawYield - yield;

        node.exactHp = hpAfter;
        node.hp = (int) Math.ceil(hpAfter - 1e-6);
        node.stage = Math.min(4, 1 + (int) ((100.0 - Math.max(0.0, hpAfter)) / 25.0));
        if (hotspotHit || rng.nextInt(3) == 0) {
            node.randomizeHotspot(rng, p);
        }

        if (hotspotHit) {
            node.world.spawnParticle(Particle.CRIT, hitX, hitY, hitZ, 18, 0.22, 0.22, 0.22, 0.15);
        } else {
            node.world.spawnParticle(Particle.BLOCK_CRACK, hitX, hitY, hitZ, 10, 0.22, 0.22, 0.22,
                    Material.STONE.createBlockData());
        }

        boolean broke = hpAfter <= 0.001;
        if (broke) {
            int rem = (int) Math.round(node.yieldCarry);
            yield += rem;
            node.yieldCarry = 0.0;
            node.exactHp = 0.0;
            node.hp = 0;
            node.streak = 0;
            node.respawnAtMs = System.currentTimeMillis() + 90_000L;
            node.world.getBlockAt(node.x, node.y, node.z).setType(Material.AIR, false);
        }

        CraftingAndSyncManager effSync = (sync != null) ? sync : syncManager;
        if ("metal".equals(node.kind)) {
            giveWithToast(p, named(Material.IRON_INGOT, "Фрагменты металла", yield), "ЖЕЛЕЗНАЯ РУДА", yield, effSync);
            if (broke && (tool == Material.IRON_PICKAXE || tool == Material.STONE_PICKAXE)) {
                giveWithToast(p, named(Material.NETHERITE_INGOT, "МВК", 2), "МВК", 2, effSync);
            }
        } else if ("sulfur".equals(node.kind)) {
            giveWithToast(p, named(Material.GLOWSTONE_DUST, "Сера", yield), "СЕРНАЯ РУДА", yield, effSync);
        } else {
            giveWithToast(p, named(Material.COBBLESTONE, "Камень", yield), "КАМЕНЬ", yield, effSync);
        }

        if (effSync != null) {
            effSync.sendToPlayer(p, String.format(Locale.US, "FARM_FX|ore|%d|%d|%.2f|%.2f|%.2f|%d",
                    hotspotHit ? 1 : 0, node.streak, hitX, hitY, hitZ, broke ? 1 : 0));
            effSync.sendNodesAndTrees(p);
        }
    }

    private void giveWithToast(Player p, ItemStack stack, String toastLabel, int amount, CraftingAndSyncManager sync) {
        if (amount <= 0) return;
        giveToPlayer(p, stack);
        if (sync != null) {
            sync.sendToPlayer(p, "GAIN|" + toastLabel + "|" + amount);
        }
    }

    private void hitTree(RustTreeState tree, Player p, double hitX, double hitY, double hitZ,
                         CraftingAndSyncManager sync) {
        if (!tree.isActive()) return;

        ItemStack held = p.getInventory().getItemInMainHand();
        Material tool = held.getType();
        double baseDmg = treeToolBaseDmg(tool);
        int pool = treeToolPool(tool);

        // Exact 3D painted red X ("крестик") minigame on tree trunk
        double xSurfR = "vanilla".equals(tree.kind) ? 0.54 : tree.radius;
        double xWorldX = (tree.x + 0.5) + Math.cos(tree.xAngle) * xSurfR;
        double xWorldY = tree.y + tree.xY;
        double xWorldZ = (tree.z + 0.5) + Math.sin(tree.xAngle) * xSurfR;
        double dx = hitX - xWorldX;
        double dy = hitY - xWorldY;
        double dz = hitZ - xWorldZ;
        boolean xHit = (dx * dx + dy * dy + dz * dz) <= (0.46 * 0.46);

        double mult;
        if (xHit) {
            tree.streak = Math.min(6, tree.streak + 1);
            mult = rustHotspotMultiplier(tree.streak);
            tree.moveXMarker(rng, p);
        } else {
            tree.streak = 0;
            mult = 1.0;
        }

        double dmg = baseDmg * mult;
        double hpBefore = tree.exactHp;
        double hpAfter = Math.max(0.0, hpBefore - dmg);
        double frac = (hpBefore - hpAfter) / 100.0;
        double rawYield = frac * pool + tree.yieldCarry;
        int yield = (int) Math.floor(rawYield + 1e-6);
        tree.yieldCarry = rawYield - yield;

        tree.exactHp = hpAfter;
        tree.hp = (int) Math.ceil(hpAfter - 1e-6);
        tree.world.spawnParticle(
                Particle.BLOCK_CRACK,
                hitX, hitY, hitZ,
                xHit ? 16 : 9,
                0.22, 0.22, 0.22,
                xHit ? Material.RED_WOOL.createBlockData() : Material.OAK_LOG.createBlockData()
        );

        boolean fell = hpAfter <= 0.001;
        if (fell) {
            yield += (int) Math.round(tree.yieldCarry);
            tree.yieldCarry = 0.0;
            tree.exactHp = 0.0;
            tree.hp = 0;
            tree.streak = 0;
            if ("vanilla".equals(tree.kind)) {
                tree.respawnAtMs = Long.MAX_VALUE;
                clearVanillaTreeBlocks(tree.world, tree.x, tree.y, tree.z);
            } else {
                tree.respawnAtMs = System.currentTimeMillis() + 150_000L;
            }
        }

        giveWithToast(p, named(Material.OAK_PLANKS, "Дерево", yield), "ДЕРЕВО", yield, sync != null ? sync : syncManager);

        if (sync != null) {
            sync.sendToPlayer(p, String.format(Locale.US, "FARM_FX|tree|%d|%d|%.2f|%.2f|%.2f|%d",
                    xHit ? 1 : 0, tree.streak, hitX, hitY, hitZ, fell ? 1 : 0));
            if (fell) {
                double fallYaw = Math.toDegrees(Math.atan2(
                        (tree.z + 0.5) - p.getLocation().getZ(),
                        (tree.x + 0.5) - p.getLocation().getX()
                ));
                sync.broadcastTreeFall(tree.id, fallYaw);
            }
            sync.sendNodesAndTrees(p);
        }
    }

    private static boolean isTreeLog(Material m) {
        return m == Material.OAK_LOG || m == Material.BIRCH_LOG || m == Material.SPRUCE_LOG
                || m == Material.DARK_OAK_LOG || m == Material.ACACIA_LOG || m == Material.JUNGLE_LOG;
    }

    private RustTreeState findOrRegisterVanillaTree(Block hitBlock, Player facing) {
        World w = hitBlock.getWorld();
        int bx = hitBlock.getX();
        int by = hitBlock.getY();
        int bz = hitBlock.getZ();
        while (by > 2 && isTreeLog(w.getBlockAt(bx, by - 1, bz).getType())) {
            by--;
        }
        String key = blockKey(w, bx, by, bz);
        RustTreeState existing = vanillaTreeByBase.get(key);
        if (existing != null && existing.isActive()) {
            return existing;
        }
        RustTreeState st = new RustTreeState(nextVanillaTreeId++, "vanilla", w, bx, by, bz, 0.50f, 5.5f, rng);
        if (facing != null) {
            st.xAngle = (float) Math.atan2(facing.getLocation().getZ() - (bz + 0.5),
                    facing.getLocation().getX() - (bx + 0.5));
        }
        st.xY = 1.15f;
        vanillaTreeByBase.put(key, st);
        allTrees.add(st);
        treeById.put(st.id, st);
        return st;
    }

    private void clearVanillaTreeBlocks(World w, int bx, int by, int bz) {
        // Flood-fill only logs directly connected to this specific trunk so adjacent trees remain untouched
        List<Block> logsToRemove = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        ArrayDeque<Block> q = new ArrayDeque<>();
        Block base = w.getBlockAt(bx, by, bz);
        q.add(base);
        visited.add(bx + ":" + by + ":" + bz);

        while (!q.isEmpty() && logsToRemove.size() < 28) {
            Block cur = q.poll();
            if (!isTreeLog(cur.getType())) continue;
            logsToRemove.add(cur);
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int nx = cur.getX() + dx;
                        int ny = cur.getY() + dy;
                        int nz = cur.getZ() + dz;
                        if (Math.abs(nx - bx) > 2 || Math.abs(nz - bz) > 2 || ny < by) continue;
                        String k = nx + ":" + ny + ":" + nz;
                        if (visited.add(k)) {
                            Block nb = w.getBlockAt(nx, ny, nz);
                            if (isTreeLog(nb.getType())) {
                                q.add(nb);
                            }
                        }
                    }
                }
            }
        }

        for (Block lb : logsToRemove) {
            lb.setType(Material.AIR, false);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        Block lf = w.getBlockAt(lb.getX() + dx, lb.getY() + dy, lb.getZ() + dz);
                        Material m = lf.getType();
                        if (m == Material.OAK_LEAVES || m == Material.BIRCH_LEAVES || m == Material.SPRUCE_LEAVES
                                || m == Material.DARK_OAK_LEAVES || m == Material.ACACIA_LEAVES || m == Material.JUNGLE_LEAVES) {
                            lf.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }
    }

    private void breakBarrel(Block b, Player p) {
        if (b.getType() == Material.AIR) return;
        b.setType(Material.AIR, false);
        b.getWorld().playSound(b.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 0.8f);
        b.getWorld().spawnParticle(Particle.BLOCK_CRACK, b.getLocation().add(0.5, 0.5, 0.5), 16, 0.3, 0.3, 0.3, Material.OAK_PLANKS.createBlockData());

        int scrapAmt = 2 + rng.nextInt(3);
        giveWithToast(p, named(Material.EMERALD, "Скрап", scrapAmt), "СКРАП", scrapAmt, syncManager);
        int r = rng.nextInt(4);
        if (r == 0) giveWithToast(p, named(Material.STRING, "Ткань", 10), "ТКАНЬ", 10, syncManager);
        else if (r == 1) giveWithToast(p, named(Material.FLINT, "Труба", 1), "ТРУБА", 1, syncManager);
        else if (r == 2) giveWithToast(p, named(Material.FEATHER, "Шестерня", 1), "ШЕСТЕРНЯ", 1, syncManager);
        else giveWithToast(p, named(Material.TRIPWIRE_HOOK, "Пружина", 1), "ПРУЖИНА", 1, syncManager);

        barrelRespawns.put(blockKey(b.getLocation()), System.currentTimeMillis() + 180_000L);
    }

    private void openRecyclerGui(Player p) {
        Inventory gui = Bukkit.createInventory(null, 9, "§8ПЕРЕРАБОТЧИК [Клик: переработать]");
        ItemStack btn = named(Material.LIME_WOOL, "§aПЕРЕРАБОТАТЬ ВСЁ", 1);
        gui.setItem(8, btn);
        openRecyclers.put(p.getUniqueId(), gui);
        p.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        // Support clean Shift+Click transfer and 1000-stack merging across chests & Player Inventory
        Inventory gui = openRecyclers.get(p.getUniqueId());
        if (gui == null || e.getInventory() != gui) {
            Inventory topInv = e.getView().getTopInventory();
            if (e.isShiftClick() && topInv != null && !(topInv instanceof PlayerInventory) && e.getRawSlot() >= 0) {
                e.setCancelled(true);
                transferContainerSlotRust(this, p, topInv, e.getRawSlot());
                return;
            }
            if (e.getClickedInventory() != null && e.getCursor() != null && e.getCurrentItem() != null) {
                ItemStack cur = e.getCursor();
                ItemStack target = e.getCurrentItem();
                if (!cur.getType().isAir() && !target.getType().isAir()
                        && isStackableResource(cur.getType()) && canMergeStacks(cur, target)) {
                    int cAmt = getRealCount(cur);
                    int tAmt = getRealCount(target);
                    if (tAmt < 1000) {
                        e.setCancelled(true);
                        int add = Math.min(1000 - tAmt, cAmt);
                        setRealCount(target, tAmt + add);
                        int rem = cAmt - add;
                        if (rem <= 0) {
                            p.setItemOnCursor(null);
                        } else {
                            setRealCount(cur, rem);
                            p.setItemOnCursor(cur);
                        }
                        p.updateInventory();
                    }
                }
            }
            if (topInv != null && topInv.getHolder() instanceof Chest) {
                scheduleCheckEmptyCrate(topInv);
            }
            return;
        }

        if (e.getRawSlot() == 8) {
            e.setCancelled(true);
            int totalScrap = 0, totalMetal = 0, totalWood = 0, totalCloth = 0;
            for (int i = 0; i < 8; i++) {
                ItemStack is = gui.getItem(i);
                if (is == null || is.getType() == Material.AIR) continue;
                int cnt = getRealCount(is);
                Material m = is.getType();
                if (m == Material.FLINT || m == Material.FEATHER || m == Material.TRIPWIRE_HOOK || m == Material.REDSTONE) {
                    totalScrap += 5 * cnt;
                    totalMetal += 15 * cnt;
                } else if (m == Material.IRON_DOOR || m == Material.IRON_HOE || m == Material.NETHERITE_SWORD || m == Material.IRON_CHESTPLATE) {
                    totalScrap += 10 * cnt;
                    totalMetal += 45 * cnt;
                } else if (m == Material.STRING || m == Material.OAK_FENCE) {
                    totalCloth += 5 * cnt;
                    totalWood += 20 * cnt;
                } else {
                    totalScrap += 2 * cnt;
                    totalMetal += 10 * cnt;
                }
                gui.setItem(i, null);
            }
            if (totalScrap > 0) giveWithToast(p, named(Material.EMERALD, "Скрап", totalScrap), "СКРАП", totalScrap, syncManager);
            if (totalMetal > 0) giveWithToast(p, named(Material.IRON_INGOT, "Фрагменты металла", totalMetal), "ФРАГМЕНТЫ МЕТАЛЛА", totalMetal, syncManager);
            if (totalWood > 0)  giveWithToast(p, named(Material.OAK_PLANKS, "Дерево", totalWood), "ДЕРЕВО", totalWood, syncManager);
            if (totalCloth > 0) giveWithToast(p, named(Material.STRING, "Ткань", totalCloth), "ТКАНЬ", totalCloth, syncManager);
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1.3f);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        ItemStack st = e.getItem().getItemStack();
        if (isStackableResource(st.getType())) {
            e.setCancelled(true);
            e.getItem().remove();
            int amt = getRealCount(st);
            String label = getDefaultResourceName(st);
            giveToPlayer(p, named(st.getType(), label, amt));
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.4f, 1.4f);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        Inventory gui = openRecyclers.remove(p.getUniqueId());
        if (gui != null && e.getInventory() == gui) {
            for (int i = 0; i < 8; i++) {
                ItemStack is = gui.getItem(i);
                if (is != null && is.getType() != Material.AIR) {
                    giveToPlayer(p, is);
                }
            }
            return;
        }
        checkAndDespawnEmptyCrate(e.getInventory());
    }

    public void scheduleCheckEmptyCrate(Inventory inv) {
        Bukkit.getScheduler().runTask(plugin, () -> checkAndDespawnEmptyCrate(inv));
    }

    public void checkAndDespawnEmptyCrate(Inventory inv) {
        if (inv == null || !(inv.getHolder() instanceof Chest)) return;
        Chest chest = (Chest) inv.getHolder();
        Block b = chest.getBlock();
        if (b.getType() != Material.CHEST) return;
        String key = blockKey(b.getLocation());
        if (!crateByKey.containsKey(key)) return;

        for (ItemStack it : inv.getContents()) {
            if (it != null && it.getType() != Material.AIR) {
                return;
            }
        }

        // Crate has been completely looted -> close viewers cleanly & remove block until respawn
        for (org.bukkit.entity.HumanEntity viewer : new ArrayList<>(inv.getViewers())) {
            viewer.closeInventory();
        }
        b.setType(Material.AIR, false);
        b.getWorld().spawnParticle(Particle.BLOCK_CRACK, b.getLocation().add(0.5, 0.35, 0.5),
                6, 0.25, 0.15, 0.25, Material.OAK_PLANKS.createBlockData());
        crateRespawnAt.put(key, System.currentTimeMillis() + 180_000L);
    }

    public static boolean isStackableResource(Material m) {
        return m == Material.COBBLESTONE || m == Material.STONE
                || m == Material.OAK_PLANKS || m == Material.BIRCH_PLANKS || m == Material.SPRUCE_PLANKS
                || m == Material.OAK_LOG || m == Material.BIRCH_LOG || m == Material.SPRUCE_LOG
                || m == Material.IRON_INGOT || m == Material.NETHERITE_INGOT
                || m == Material.GLOWSTONE_DUST || m == Material.CHARCOAL || m == Material.COAL
                || m == Material.GUNPOWDER || m == Material.STRING || m == Material.LEATHER
                || m == Material.EMERALD || m == Material.BLAZE_POWDER
                || m == Material.PORKCHOP || m == Material.BEEF || m == Material.COOKED_BEEF || m == Material.APPLE
                || m == Material.ARROW || m == Material.GOLD_NUGGET || m == Material.IRON_NUGGET
                || m == Material.FLINT || m == Material.FEATHER || m == Material.TRIPWIRE_HOOK || m == Material.REDSTONE;
    }

    public static String getDefaultResourceName(ItemStack st) {
        if (st.hasItemMeta() && st.getItemMeta().hasDisplayName()) {
            return st.getItemMeta().getDisplayName().replace("§f", "");
        }
        Material m = st.getType();
        if (m == Material.COBBLESTONE || m == Material.STONE) return "Камень";
        if (m == Material.OAK_PLANKS || m == Material.OAK_LOG || m == Material.BIRCH_PLANKS || m == Material.BIRCH_LOG || m == Material.SPRUCE_PLANKS || m == Material.SPRUCE_LOG) return "Дерево";
        if (m == Material.IRON_INGOT) return "Фрагменты металла";
        if (m == Material.NETHERITE_INGOT) return "МВК";
        if (m == Material.GLOWSTONE_DUST) return "Сера";
        if (m == Material.CHARCOAL) return "Уголь";
        if (m == Material.COAL) return "Топливо";
        if (m == Material.GUNPOWDER) return "Порох";
        if (m == Material.STRING) return "Ткань";
        if (m == Material.LEATHER) return "Кожа";
        if (m == Material.EMERALD) return "Скрап";
        if (m == Material.BLAZE_POWDER) return "Топливо низкого качества";
        if (m == Material.PORKCHOP) return "Животный жир";
        if (m == Material.BEEF) return "Сырое мясо";
        if (m == Material.COOKED_BEEF) return "Жареное мясо";
        if (m == Material.GOLD_NUGGET) return "Патроны 9мм";
        if (m == Material.IRON_NUGGET) return "Патроны 5.56мм";
        if (m == Material.ARROW) return "Деревянные стрелы";
        return st.getType().name();
    }

    public static int getRealCount(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return 0;
        if (RCOUNT_KEY != null && stack.hasItemMeta()) {
            Integer v = stack.getItemMeta().getPersistentDataContainer().get(RCOUNT_KEY, PersistentDataType.INTEGER);
            if (v != null && v > 0) return v * Math.max(1, stack.getAmount());
        }
        return stack.getAmount();
    }

    public static void setRealCount(ItemStack stack, int count) {
        if (stack == null || stack.getType() == Material.AIR) return;
        int clamped = Math.max(1, Math.min(1000, count));
        if (isStackableResource(stack.getType()) && RCOUNT_KEY != null) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(RCOUNT_KEY, PersistentDataType.INTEGER, clamped);
                meta.setLore(Collections.singletonList("§7Количество: §e" + clamped + " §8/ 1000"));
                stack.setItemMeta(meta);
            }
            stack.setAmount(1);
        } else {
            stack.setAmount(Math.min(64, clamped));
        }
    }

    public static boolean canMergeStacks(ItemStack a, ItemStack b) {
        if (a == null || b == null || a.getType() != b.getType()) return false;
        String na = a.hasItemMeta() && a.getItemMeta().hasDisplayName() ? a.getItemMeta().getDisplayName() : "";
        String nb = b.hasItemMeta() && b.getItemMeta().hasDisplayName() ? b.getItemMeta().getDisplayName() : "";
        return na.equals(nb);
    }

    public static ItemStack createItem(String id, int count) {
        SpaceRustItemRegistry.ItemDef def = SpaceRustItemRegistry.get(id);
        if (def == null) return new ItemStack(Material.STONE, 1);
        ItemStack stack = new ItemStack(def.material, 1);
        SpaceRustItemRegistry.stamp(stack, def);
        if (isStackableResource(def.material) && count > 0) {
            setRealCount(stack, Math.min(1000, count));
        } else {
            stack.setAmount(Math.max(1, Math.min(64, count)));
        }
        return stack;
    }

    public static ItemStack named(Material m, String name, int count) {
        ItemStack stack = new ItemStack(m, 1);
        SpaceRustItemRegistry.ItemDef def = SpaceRustItemRegistry.find(m, name);
        if (def != null) {
            SpaceRustItemRegistry.stamp(stack, def);
        } else {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§f" + name);
                stack.setItemMeta(meta);
            }
        }
        if (isStackableResource(m) && count > 0) {
            setRealCount(stack, Math.min(1000, count));
        } else {
            stack.setAmount(Math.max(1, Math.min(64, count)));
        }
        return stack;
    }

    public static void giveToPlayer(Player p, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        PlayerInventory inv = p.getInventory();
        int remaining = getRealCount(item);
        if (isStackableResource(item.getType())) {
            // 1. Top up existing stacks up to 1000 strictly inside 6 belt slots (0..5) + 24 inventory slots (9..32)
            int[] order = new int[30];
            int idx = 0;
            for (int s = 0; s <= 5; s++) order[idx++] = s;
            for (int s = 9; s <= 32; s++) order[idx++] = s;

            for (int s : order) {
                if (remaining <= 0) break;
                ItemStack cur = inv.getItem(s);
                if (cur != null && canMergeStacks(cur, item)) {
                    int have = getRealCount(cur);
                    if (have < 1000) {
                        int add = Math.min(1000 - have, remaining);
                        setRealCount(cur, have + add);
                        inv.setItem(s, cur);
                        remaining -= add;
                    }
                }
            }

            // 2. Allocate fresh 1000-stacks in 24 main slots (9..32), then 6 belt slots (0..5)
            int[] allocOrder = new int[30];
            idx = 0;
            for (int s = 9; s <= 32; s++) allocOrder[idx++] = s;
            for (int s = 0; s <= 5; s++) allocOrder[idx++] = s;

            for (int s : allocOrder) {
                if (remaining <= 0) break;
                ItemStack cur = inv.getItem(s);
                if (cur == null || cur.getType() == Material.AIR) {
                    int give = Math.min(1000, remaining);
                    ItemStack fresh = item.clone();
                    setRealCount(fresh, give);
                    inv.setItem(s, fresh);
                    remaining -= give;
                }
            }

            while (remaining > 0) {
                int drop = Math.min(1000, remaining);
                ItemStack fresh = item.clone();
                setRealCount(fresh, drop);
                p.getWorld().dropItemNaturally(p.getLocation(), fresh);
                remaining -= drop;
            }
            return;
        }

        // Non-resource item: place strictly inside 24 main slots (9..32) or 6 belt slots (0..5)
        for (int s = 9; s <= 32; s++) {
            if (inv.getItem(s) == null || inv.getItem(s).getType() == Material.AIR) {
                inv.setItem(s, item);
                return;
            }
        }
        for (int s = 0; s <= 5; s++) {
            if (inv.getItem(s) == null || inv.getItem(s).getType() == Material.AIR) {
                inv.setItem(s, item);
                return;
            }
        }
        p.getWorld().dropItemNaturally(p.getLocation(), item);
    }

    public static void transferContainerSlotRust(OreAndLootManager mgr, Player p, Inventory topInv, int rawSlot) {
        if (p == null || topInv == null || topInv instanceof PlayerInventory) return;
        PlayerInventory inv = p.getInventory();
        int topSize = topInv.getSize();

        if (rawSlot >= 0 && rawSlot < topSize) {
            ItemStack src = topInv.getItem(rawSlot);
            if (src == null || src.getType().isAir()) return;

            boolean stackable1k = isStackableResource(src.getType());
            int maxStack = stackable1k ? 1000 : Math.max(1, src.getMaxStackSize());
            int remaining = stackable1k ? getRealCount(src) : src.getAmount();

            int[] order = new int[30];
            int idx = 0;
            for (int s = 9; s <= 32; s++) order[idx++] = s;
            for (int s = 0; s <= 5; s++) order[idx++] = s;

            // 1. Merge with existing stacks in visible 6x4 Inventory + 6 Hotbar
            if (maxStack > 1) {
                for (int s : order) {
                    if (remaining <= 0) break;
                    ItemStack cur = inv.getItem(s);
                    if (cur != null && !cur.getType().isAir() && canMergeStacks(cur, src) && cur.isSimilar(src)) {
                        int have = stackable1k ? getRealCount(cur) : cur.getAmount();
                        if (have < maxStack) {
                            int add = Math.min(maxStack - have, remaining);
                            if (stackable1k) setRealCount(cur, have + add);
                            else cur.setAmount(have + add);
                            inv.setItem(s, cur);
                            remaining -= add;
                        }
                    } else if (stackable1k && cur != null && !cur.getType().isAir() && canMergeStacks(cur, src)) {
                        int have = getRealCount(cur);
                        if (have < 1000) {
                            int add = Math.min(1000 - have, remaining);
                            setRealCount(cur, have + add);
                            inv.setItem(s, cur);
                            remaining -= add;
                        }
                    }
                }
            }

            // 2. Place remaining into first empty visible slot (left-to-right, top-to-bottom 9..32, then 0..5)
            for (int s : order) {
                if (remaining <= 0) break;
                ItemStack cur = inv.getItem(s);
                if (cur == null || cur.getType().isAir()) {
                    int give = Math.min(maxStack, remaining);
                    ItemStack fresh = src.clone();
                    if (stackable1k) setRealCount(fresh, give);
                    else fresh.setAmount(give);
                    inv.setItem(s, fresh);
                    remaining -= give;
                }
            }

            if (remaining <= 0) {
                topInv.setItem(rawSlot, null);
            } else {
                ItemStack remStack = src.clone();
                if (stackable1k) setRealCount(remStack, remaining);
                else remStack.setAmount(remaining);
                topInv.setItem(rawSlot, remStack);
            }
            p.updateInventory();
            if (mgr != null) {
                mgr.checkAndDespawnEmptyCrate(topInv);
            }
            return;
        }

        if (rawSlot >= topSize && rawSlot < topSize + 36) {
            int rel = rawSlot - topSize;
            int bk = (rel < 27) ? (9 + rel) : (rel - 27);
            ItemStack src = inv.getItem(bk);
            if (src == null || src.getType().isAir()) return;

            boolean stackable1k = isStackableResource(src.getType());
            int maxStack = stackable1k ? 1000 : Math.max(1, src.getMaxStackSize());
            int remaining = stackable1k ? getRealCount(src) : src.getAmount();

            if (maxStack > 1) {
                for (int s = 0; s < topSize && remaining > 0; s++) {
                    ItemStack cur = topInv.getItem(s);
                    if (cur != null && !cur.getType().isAir() && canMergeStacks(cur, src) && (stackable1k || cur.isSimilar(src))) {
                        int have = stackable1k ? getRealCount(cur) : cur.getAmount();
                        if (have < maxStack) {
                            int add = Math.min(maxStack - have, remaining);
                            if (stackable1k) setRealCount(cur, have + add);
                            else cur.setAmount(have + add);
                            topInv.setItem(s, cur);
                            remaining -= add;
                        }
                    }
                }
            }
            for (int s = 0; s < topSize && remaining > 0; s++) {
                ItemStack cur = topInv.getItem(s);
                if (cur == null || cur.getType().isAir()) {
                    int give = Math.min(maxStack, remaining);
                    ItemStack fresh = src.clone();
                    if (stackable1k) setRealCount(fresh, give);
                    else fresh.setAmount(give);
                    topInv.setItem(s, fresh);
                    remaining -= give;
                }
            }

            if (remaining <= 0) {
                inv.setItem(bk, null);
            } else {
                ItemStack remStack = src.clone();
                if (stackable1k) setRealCount(remStack, remaining);
                else remStack.setAmount(remaining);
                inv.setItem(bk, remStack);
            }
            p.updateInventory();
        }
    }

    public void saveState() {}

    private static String blockKey(Location l) {
        return blockKey(l.getWorld(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    private static String blockKey(World w, int x, int y, int z) {
        return (w != null ? w.getName() : "w") + ":" + x + ":" + y + ":" + z;
    }
}
