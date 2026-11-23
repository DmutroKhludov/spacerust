package net.spacerust.plugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Main SpaceRust Purpur 1.16.5 Plugin entry point.
 */
public class SpaceRustPlugin extends JavaPlugin implements TabCompleter {

    public static final String SYNC_CHANNEL = "spacerust:sync";
    public static final String VOICE_CHANNEL = "spacerust:voice";

    private MonumentBuilder monumentBuilder;
    private OreAndLootManager oreAndLootManager;
    private BuildingManager buildingManager;
    private CraftingAndSyncManager syncManager;
    private VoiceChatManager voiceChatManager;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        this.monumentBuilder = new MonumentBuilder(this);
        this.oreAndLootManager = new OreAndLootManager(this, this.monumentBuilder);
        this.buildingManager = new BuildingManager(this);
        this.syncManager = new CraftingAndSyncManager(this, this.monumentBuilder, this.oreAndLootManager, this.buildingManager);
        this.oreAndLootManager.setSyncManager(this.syncManager);
        this.buildingManager.setSyncManager(this.syncManager);
        this.voiceChatManager = new VoiceChatManager(this);

        Bukkit.getMessenger().registerOutgoingPluginChannel(this, SYNC_CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, SYNC_CHANNEL, this.syncManager);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, VOICE_CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, VOICE_CHANNEL, this.voiceChatManager);

        Bukkit.getPluginManager().registerEvents(this.oreAndLootManager, this);
        Bukkit.getPluginManager().registerEvents(this.buildingManager, this);
        Bukkit.getPluginManager().registerEvents(this.syncManager, this);

        // Build monuments & seed initial ore nodes once world is loaded
        Bukkit.getScheduler().runTaskLater(this, () -> {
            World world = Bukkit.getWorlds().get(0);
            this.monumentBuilder.ensureBuilt(world, false);
            this.oreAndLootManager.initNodesAndLoot(world);
        }, 20L);

        // Periodic sync (radiation, nearby 3D ore nodes, crate/barrel respawn)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            this.oreAndLootManager.tickRespawns();
            this.syncManager.tickPlayers();
        }, 20L, 10L);

        getLogger().info("[SpaceRustCore] Loaded RT Monuments, 3D Ore Nodes, Loot & Client Sync!");
    }

    @Override
    public void onDisable() {
        if (this.oreAndLootManager != null) {
            this.oreAndLootManager.saveState();
        }
        if (this.buildingManager != null) {
            this.buildingManager.save();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("rustbuild".equalsIgnoreCase(command.getName())) {
            World world = (sender instanceof Player) ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);
            sender.sendMessage("§e[SpaceRust] Строю дороги, 6 РТ-монументов, ящики и камни руды...");
            this.monumentBuilder.ensureBuilt(world, true);
            this.oreAndLootManager.respawnAllOresAndTrees(world, true, true);
            this.syncManager.broadcastMonuments();
            sender.sendMessage("§a[SpaceRust] Готово! Все РТ и руды сгенерированы вокруг (0, 0). Нажмите G в игре для карты.");
            return true;
        }
        if ("map".equalsIgnoreCase(command.getName())) {
            sender.sendMessage("§6=== КАРТА РТ SPACERUST (клавиша G в игре) ===");
            for (MonumentBuilder.MonumentDef m : this.monumentBuilder.getMonuments()) {
                String rad = m.radLevel > 0 ? " §c[РАД " + m.radLevel + "]" : " §a[Без рад.]";
                sender.sendMessage("§f • §b" + m.name + " §7(X: " + m.x + ", Z: " + m.z + ")" + rad);
            }
            return true;
        }
        if ("ruststarter".equalsIgnoreCase(command.getName()) && sender instanceof Player) {
            this.syncManager.giveStarterKit((Player) sender);
            sender.sendMessage("§a[SpaceRust] Выдан стартовый набор (Камень + Факел).");
            return true;
        }
        if ("rustgive".equalsIgnoreCase(command.getName()) && sender instanceof Player) {
            Player p = (Player) sender;
            if (args.length < 1 || "list".equalsIgnoreCase(args[0])) {
                StringBuilder ids = new StringBuilder("§6[SpaceRust] Предметы: §f");
                int i = 0;
                for (SpaceRustItemRegistry.ItemDef def : SpaceRustItemRegistry.all()) {
                    if (i++ > 0) ids.append("§7, §f");
                    ids.append(def.id);
                }
                p.sendMessage(ids.toString());
                return true;
            }
            SpaceRustItemRegistry.ItemDef def = SpaceRustItemRegistry.get(args[0]);
            if (def == null) {
                p.sendMessage("§c[SpaceRust] Неизвестный предмет: " + args[0] + " (§7/rustgive list§c)");
                return true;
            }
            int qty = 1;
            if (args.length >= 2) {
                try { qty = Math.max(1, Math.min(1000, Integer.parseInt(args[1]))); } catch (NumberFormatException ignored) {}
            }
            OreAndLootManager.giveToPlayer(p, OreAndLootManager.createItem(def.id, qty));
            p.updateInventory();
            p.sendMessage("§a[SpaceRust] Выдано: §f" + def.name + " §7x" + qty);
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ("rustgive".equalsIgnoreCase(command.getName()) && args.length == 1) {
            String pref = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            if ("list".startsWith(pref)) out.add("list");
            for (SpaceRustItemRegistry.ItemDef def : SpaceRustItemRegistry.all()) {
                if (def.id.startsWith(pref)) out.add(def.id);
            }
            return out;
        }
        return Collections.emptyList();
    }
}
