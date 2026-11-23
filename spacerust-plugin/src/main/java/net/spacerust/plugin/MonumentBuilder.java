package net.spacerust.plugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Generates iconic Rust RT Monuments, connecting roads, recyclers,
 * crate spawn points, and barrel spawn points around world spawn (0, 0).
 */
public class MonumentBuilder {

    public static class MonumentDef {
        public final String name;
        public final String type;
        public final int x;
        public final int z;
        public final int radius;
        public final int radLevel;

        public MonumentDef(String name, String type, int x, int z, int radius, int radLevel) {
            this.name = name;
            this.type = type;
            this.x = x;
            this.z = z;
            this.radius = radius;
            this.radLevel = radLevel;
        }
    }

    public enum LootTier { COMMON, TOOLS, FOOD, MILITARY, ELITE }

    public static class CrateSpot {
        public final Location loc;
        public final LootTier tier;

        public CrateSpot(Location loc, LootTier tier) {
            this.loc = loc;
            this.tier = tier;
        }
    }

    private final SpaceRustPlugin plugin;
    private final List<MonumentDef> monuments = new ArrayList<>();
    private final List<Location> recyclerLocations = new ArrayList<>();
    private final List<CrateSpot> crateSpots = new ArrayList<>();
    private final List<Location> barrelSpots = new ArrayList<>();

    public MonumentBuilder(SpaceRustPlugin plugin) {
        this.plugin = plugin;
        monuments.add(new MonumentDef("СУПЕРМАРКЕТ", "supermarket", 145, -95, 24, 0));
        monuments.add(new MonumentDef("ЗАПРАВКА OXUM'S", "gasstation", -135, -125, 22, 0));
        monuments.add(new MonumentDef("СФЕРА", "dome", 220, 185, 32, 15));
        monuments.add(new MonumentDef("АЭРОДРОМ", "airfield", -240, 170, 42, 10));
        monuments.add(new MonumentDef("МАЯК", "lighthouse", -210, -250, 20, 0));
        monuments.add(new MonumentDef("РАДАР", "radar", 30, -255, 32, 25));
    }

    public List<MonumentDef> getMonuments() {
        return Collections.unmodifiableList(monuments);
    }

    public List<Location> getRecyclerLocations() {
        return recyclerLocations;
    }

    public List<CrateSpot> getCrateSpots() {
        return crateSpots;
    }

    public List<Location> getBarrelSpots() {
        return barrelSpots;
    }

    public boolean isInsideAnyMonument(int x, int z, int extraPad) {
        for (MonumentDef m : monuments) {
            int dx = x - m.x;
            int dz = z - m.z;
            int r = m.radius + extraPad;
            if (dx * dx + dz * dz <= r * r) return true;
        }
        return false;
    }

    public int getRadiationAt(Location loc) {
        int maxRad = 0;
        for (MonumentDef m : monuments) {
            if (m.radLevel <= 0) continue;
            double dist = Math.hypot(loc.getX() - m.x, loc.getZ() - m.z);
            if (dist <= m.radius) {
                maxRad = Math.max(maxRad, m.radLevel);
            }
        }
        return maxRad;
    }

    public void ensureBuilt(World world, boolean forceRebuild) {
        File stateFile = new File(plugin.getDataFolder(), "monuments.yml");
        YamlConfiguration cfg = stateFile.exists() ? YamlConfiguration.loadConfiguration(stateFile) : new YamlConfiguration();

        boolean alreadyBuilt = cfg.getBoolean("built", false);
        boolean hasVariedCrates = cfg.getBoolean("varied_crates_v2", false);
        recyclerLocations.clear();
        crateSpots.clear();
        barrelSpots.clear();

        if (!alreadyBuilt || forceRebuild) {
            plugin.getLogger().info("[SpaceRustCore] Generating roads and 6 RT monuments...");
            for (MonumentDef m : monuments) {
                int y = findAverageSurfaceY(world, m.x, m.z);
                buildRoad(world, 0, 0, m.x, m.z);
                switch (m.type) {
                    case "supermarket": buildSupermarket(world, m.x, y, m.z); break;
                    case "gasstation":  buildGasStation(world, m.x, y, m.z); break;
                    case "dome":        buildDome(world, m.x, y, m.z); break;
                    case "airfield":    buildAirfield(world, m.x, y, m.z); break;
                    case "lighthouse":  buildLighthouse(world, m.x, y, m.z); break;
                    case "radar":       buildRadar(world, m.x, y, m.z); break;
                }
            }
            cfg.set("built", true);
            saveSpotsToConfig(cfg, world);
            try { cfg.save(stateFile); } catch (Exception ignored) {}
        } else {
            loadSpotsFromConfig(cfg, world);
        }
    }

    private int findAverageSurfaceY(World world, int cx, int cz) {
        int sum = 0;
        int[][] off = {{0,0}, {8,0}, {-8,0}, {0,8}, {0,-8}};
        for (int[] o : off) {
            sum += Math.max(63, world.getHighestBlockYAt(cx + o[0], cz + o[1]));
        }
        return Math.max(64, Math.min(92, sum / off.length));
    }

    private void gradePad(World world, int cx, int baseY, int cz, int halfX, int halfZ, Material floorMat) {
        for (int dx = -halfX - 2; dx <= halfX + 2; dx++) {
            for (int dz = -halfZ - 2; dz <= halfZ + 2; dz++) {
                int x = cx + dx, z = cz + dz;
                for (int y = baseY - 3; y <= baseY; y++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (y == baseY && Math.abs(dx) <= halfX && Math.abs(dz) <= halfZ) {
                        b.setType(floorMat, false);
                    } else if (b.getType().isAir() || !b.getType().isSolid()) {
                        b.setType(Material.DIRT, false);
                    }
                }
                for (int y = baseY + 1; y <= baseY + 20; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }

    private void buildRoad(World world, int x0, int z0, int x1, int z1) {
        double dist = Math.hypot(x1 - x0, z1 - z0);
        int steps = (int) Math.ceil(dist);
        Random rng = new Random(x1 * 31L + z1 * 17L);

        for (int s = 18; s < steps - 14; s++) {
            double t = s / (double) steps;
            int rx = (int) Math.round(x0 + (x1 - x0) * t);
            int rz = (int) Math.round(z0 + (z1 - z0) * t);

            for (int ox = -2; ox <= 2; ox++) {
                for (int oz = -2; oz <= 2; oz++) {
                    if (ox * ox + oz * oz > 5) continue;
                    int x = rx + ox, z = rz + oz;
                    int y = Math.max(63, findGroundSurfaceY(world, x, z));
                    Material mat = (rng.nextInt(4) == 0) ? Material.COBBLESTONE : Material.GRAVEL;
                    world.getBlockAt(x, y, z).setType(mat, false);
                    for (int cy = y + 1; cy <= y + 4; cy++) {
                        Block ab = world.getBlockAt(x, cy, z);
                        if (!ab.getType().isAir()) {
                            ab.setType(Material.AIR, false);
                        }
                    }
                }
            }

            // Place road barrels (and occasional roadside crates!) every 26 blocks on roadside
            if (s % 26 == 0) {
                int bx = rx + (rng.nextBoolean() ? 3 : -3);
                int bz = rz + (rng.nextBoolean() ? 3 : -3);
                int by = Math.max(63, findGroundSurfaceY(world, bx, bz)) + 1;
                if (s % 78 == 0) {
                    LootTier roadTier = rng.nextBoolean() ? LootTier.TOOLS : LootTier.FOOD;
                    addCrate(world, bx, by, bz, roadTier);
                } else {
                    barrelSpots.add(new Location(world, bx, by, bz));
                }
            }
        }
    }

    public static int findGroundSurfaceY(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        while (y > 58) {
            Material m = world.getBlockAt(x, y, z).getType();
            String n = m.name();
            if (m.isAir() || n.contains("LEAVES") || n.contains("LOG") || !m.isSolid()) {
                y--;
            } else {
                break;
            }
        }
        return y;
    }

    private void addRecycler(World world, int x, int y, int z) {
        world.getBlockAt(x, y, z).setType(Material.SMITHING_TABLE, false);
        recyclerLocations.add(new Location(world, x, y, z));
    }

    private void addCrate(World world, int x, int y, int z, LootTier tier) {
        world.getBlockAt(x, y, z).setType(Material.CHEST, false);
        crateSpots.add(new CrateSpot(new Location(world, x, y, z), tier));
    }

    private void box(World world, int x1, int y1, int z1, int x2, int y2, int z2, Material m) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    world.getBlockAt(x, y, z).setType(m, false);
    }

    // 1. SUPERMARKET
    private void buildSupermarket(World world, int cx, int y, int cz) {
        gradePad(world, cx, y, cz, 14, 12, Material.LIGHT_GRAY_CONCRETE);
        // Walls
        box(world, cx - 10, y + 1, cz - 8, cx + 10, y + 6, cz + 8, Material.GRAY_TERRACOTTA);
        box(world, cx - 9, y + 1, cz - 7, cx + 9, y + 5, cz + 7, Material.AIR);
        // Glass front + Doorway
        box(world, cx - 6, y + 2, cz - 8, cx + 6, y + 4, cz - 8, Material.GLASS_PANE);
        box(world, cx - 1, y + 1, cz - 8, cx + 1, y + 3, cz - 8, Material.AIR);
        // Sign roof stripe
        box(world, cx - 10, y + 5, cz - 9, cx + 10, y + 6, cz - 9, Material.CYAN_CONCRETE);
        // Shelves inside
        box(world, cx - 6, y + 1, cz - 2, cx - 5, y + 3, cz + 3, Material.DARK_OAK_PLANKS);
        box(world, cx + 1, y + 1, cz - 2, cx + 2, y + 3, cz + 3, Material.DARK_OAK_PLANKS);
        // Back room divider
        box(world, cx + 5, y + 1, cz - 7, cx + 5, y + 5, cz + 4, Material.STONE_BRICKS);
        addRecycler(world, cx + 8, y + 1, cz - 6);
        addCrate(world, cx - 6, y + 2, cz + 5, LootTier.FOOD);
        addCrate(world, cx + 8, y + 1, cz + 5, LootTier.MILITARY);
        addCrate(world, cx, y + 7, cz, LootTier.TOOLS);
        world.getBlockAt(cx - 5, y + 5, cz).setType(Material.LANTERN, false);
        world.getBlockAt(cx + 3, y + 5, cz).setType(Material.LANTERN, false);
    }

    // 2. OXUM'S GAS STATION
    private void buildGasStation(World world, int cx, int y, int cz) {
        gradePad(world, cx, y, cz, 13, 12, Material.GRAY_CONCRETE);
        // Canopy overhang
        box(world, cx - 8, y + 5, cz - 8, cx + 2, y + 6, cz - 1, Material.WHITE_CONCRETE);
        box(world, cx - 8, y + 6, cz - 8, cx + 2, y + 6, cz - 8, Material.RED_CONCRETE);
        // Pillars & fuel pumps
        box(world, cx - 7, y + 1, cz - 7, cx - 7, y + 4, cz - 7, Material.IRON_BLOCK);
        box(world, cx + 1, y + 1, cz - 7, cx + 1, y + 4, cz - 7, Material.IRON_BLOCK);
        box(world, cx - 4, y + 1, cz - 5, cx - 4, y + 2, cz - 5, Material.RED_CONCRETE);
        box(world, cx - 1, y + 1, cz - 5, cx - 1, y + 2, cz - 5, Material.RED_CONCRETE);
        // Station building & Garage
        box(world, cx - 7, y + 1, cz + 1, cx + 8, y + 5, cz + 8, Material.BRICKS);
        box(world, cx - 6, y + 1, cz + 2, cx + 7, y + 4, cz + 7, Material.AIR);
        box(world, cx - 3, y + 1, cz + 1, cx - 2, y + 3, cz + 1, Material.AIR);
        addRecycler(world, cx + 6, y + 1, cz + 6);
        addCrate(world, cx - 5, y + 1, cz + 6, LootTier.TOOLS);
        addCrate(world, cx + 1, y + 1, cz + 6, LootTier.MILITARY);
        addCrate(world, cx - 5, y + 7, cz + 4, LootTier.FOOD);
    }

    // 3. DOME (СФЕРА)
    private void buildDome(World world, int cx, int y, int cz) {
        gradePad(world, cx, y, cz, 16, 16, Material.STONE_BRICKS);
        int R = 14;
        for (int dx = -R; dx <= R; dx++) {
            for (int dy = 0; dy <= R; dy++) {
                for (int dz = -R; dz <= R; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (d >= R - 0.8 && d <= R + 0.6) {
                        boolean rib = (dx % 4 == 0) || (dz % 4 == 0) || (dy % 4 == 0);
                        Material mat = rib ? Material.GRAY_CONCRETE : Material.IRON_BARS;
                        world.getBlockAt(cx + dx, y + 1 + dy, cz + dz).setType(mat, false);
                    }
                }
            }
        }
        // Entrance archway
        box(world, cx - 2, y + 1, cz - R, cx + 2, y + 4, cz - R + 2, Material.AIR);
        // Spiral catwalk inside up to roof platform
        for (int step = 0; step < 36; step++) {
            double ang = step * 0.35;
            int sy = y + 2 + step / 3;
            int sx = cx + (int) Math.round(Math.cos(ang) * 7.5);
            int sz = cz + (int) Math.round(Math.sin(ang) * 7.5);
            box(world, sx - 1, sy, sz - 1, sx + 1, sy, sz + 1, Material.SMOOTH_STONE_SLAB);
        }
        // Summit platform + crates
        box(world, cx - 4, y + R + 1, cz - 4, cx + 4, y + R + 1, cz + 4, Material.IRON_BLOCK);
        addCrate(world, cx - 2, y + R + 2, cz, LootTier.MILITARY);
        addCrate(world, cx + 2, y + R + 2, cz, LootTier.ELITE);
        addCrate(world, cx, y + R + 2, cz + 2, LootTier.TOOLS);
        addCrate(world, cx, y + R + 2, cz - 2, LootTier.ELITE);
    }

    // 4. AIRFIELD (АЭРОДРОМ)
    private void buildAirfield(World world, int cx, int y, int cz) {
        gradePad(world, cx, y, cz, 26, 18, Material.BLACK_CONCRETE);
        // Runway markings
        for (int rx = -22; rx <= 22; rx += 4) {
            world.getBlockAt(cx + rx, y, cz + 8).setType(Material.WHITE_CONCRETE, false);
        }
        // Two Metal Hangars
        for (int hx : new int[]{cx - 12, cx + 10}) {
            box(world, hx - 6, y + 1, cz - 12, hx + 6, y + 7, cz - 2, Material.IRON_BLOCK);
            box(world, hx - 5, y + 1, cz - 11, hx + 5, y + 6, cz - 2, Material.AIR);
            addCrate(world, hx - 3, y + 1, cz - 10, LootTier.MILITARY);
            addCrate(world, hx + 3, y + 1, cz - 10, LootTier.TOOLS);
        }
        // Control Tower & Recycler
        box(world, cx - 2, y + 1, cz - 14, cx + 2, y + 11, cz - 10, Material.STONE_BRICKS);
        box(world, cx - 1, y + 9, cz - 13, cx + 1, y + 10, cz - 11, Material.GLASS);
        addRecycler(world, cx, y + 1, cz - 4);
        addCrate(world, cx, y + 12, cz - 12, LootTier.ELITE);
    }

    // 5. LIGHTHOUSE (МАЯК)
    private void buildLighthouse(World world, int cx, int y, int cz) {
        gradePad(world, cx, y, cz, 10, 10, Material.COBBLESTONE);
        for (int h = 1; h <= 18; h++) {
            int r = (h > 13) ? 3 : 4;
            Material wall = (h % 4 < 2) ? Material.RED_CONCRETE : Material.WHITE_CONCRETE;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d = Math.hypot(dx, dz);
                    if (d >= r - 0.9 && d <= r + 0.5) {
                        world.getBlockAt(cx + dx, y + h, cz + dz).setType(h > 15 ? Material.GLASS : wall, false);
                    }
                }
            }
            // Ladder inside shaft
            world.getBlockAt(cx, y + h, cz).setType(Material.IRON_BLOCK, false);
            world.getBlockAt(cx, y + h, cz - 1).setType(Material.LADDER, false);
        }
        box(world, cx - 1, y + 1, cz - 4, cx + 1, y + 3, cz - 3, Material.AIR);
        world.getBlockAt(cx, y + 17, cz).setType(Material.GLOWSTONE, false);
        addRecycler(world, cx + 6, y + 1, cz);
        addCrate(world, cx - 2, y + 19, cz, LootTier.FOOD);
        addCrate(world, cx + 2, y + 19, cz, LootTier.MILITARY);
    }

    // 6. RADAR DISH (РАДАР)
    private void buildRadar(World world, int cx, int y, int cz) {
        gradePad(world, cx, y, cz, 14, 14, Material.STONE_BRICKS);
        // Underground-style Concrete Bunker
        box(world, cx - 6, y + 1, cz - 6, cx + 6, y + 5, cz + 6, Material.GRAY_CONCRETE);
        box(world, cx - 5, y + 1, cz - 5, cx + 5, y + 4, cz + 5, Material.AIR);
        box(world, cx - 1, y + 1, cz - 6, cx + 1, y + 3, cz - 6, Material.AIR);
        // Dish Tower on roof
        for (int dy = 6; dy <= 16; dy++) {
            world.getBlockAt(cx - 2, y + dy, cz - 2).setType(Material.IRON_BARS, false);
            world.getBlockAt(cx + 2, y + dy, cz - 2).setType(Material.IRON_BARS, false);
            world.getBlockAt(cx - 2, y + dy, cz + 2).setType(Material.IRON_BARS, false);
            world.getBlockAt(cx + 2, y + dy, cz + 2).setType(Material.IRON_BARS, false);
        }
        box(world, cx - 4, y + 17, cz - 4, cx + 4, y + 17, cz + 4, Material.WHITE_CONCRETE);
        addCrate(world, cx - 4, y + 1, cz + 4, LootTier.MILITARY);
        addCrate(world, cx + 4, y + 1, cz + 4, LootTier.ELITE);
        addCrate(world, cx, y + 6, cz, LootTier.ELITE);
    }

    private void saveSpotsToConfig(YamlConfiguration cfg, World world) {
        List<String> recs = new ArrayList<>();
        for (Location l : recyclerLocations) recs.add(l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
        cfg.set("recyclers", recs);

        List<String> crates = new ArrayList<>();
        for (CrateSpot c : crateSpots) crates.add(c.loc.getBlockX() + "," + c.loc.getBlockY() + "," + c.loc.getBlockZ() + "," + c.tier.name());
        cfg.set("crates", crates);

        List<String> barrels = new ArrayList<>();
        for (Location l : barrelSpots) barrels.add(l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
        cfg.set("barrels", barrels);
    }

    private void loadSpotsFromConfig(YamlConfiguration cfg, World world) {
        for (String s : cfg.getStringList("recyclers")) {
            String[] p = s.split(",");
            if (p.length == 3) recyclerLocations.add(new Location(world, Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])));
        }
        for (String s : cfg.getStringList("crates")) {
            String[] p = s.split(",");
            if (p.length == 4) crateSpots.add(new CrateSpot(new Location(world, Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])), LootTier.valueOf(p[3])));
        }
        for (String s : cfg.getStringList("barrels")) {
            String[] p = s.split(",");
            if (p.length == 3) barrelSpots.add(new Location(world, Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])));
        }
    }
}
