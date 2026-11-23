package net.spacerust.client.world;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.spacerust.client.gui.RustCraftingQueue;
import net.spacerust.client.guns.RustViewModelAnimator;
import net.spacerust.client.hud.RustHudOverlay;
import net.spacerust.client.hud.RustResourceFeed;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-server sync over plugin messaging channel "spacerust:sync".
 * Receives RT monuments, 3D ore nodes, radiation/metabolism stats,
 * and sends craft requests to the Purpur server plugin.
 */
public final class RustWorldSync {

    public static final Identifier SYNC_CHANNEL = new Identifier("spacerust", "sync");

    public static class Monument {
        public final String name;
        public final String type;
        public final int x;
        public final int z;
        public final int radius;
        public final int radLevel;

        public Monument(String name, String type, int x, int z, int radius, int radLevel) {
            this.name = name;
            this.type = type;
            this.x = x;
            this.z = z;
            this.radius = radius;
            this.radLevel = radLevel;
        }
    }

    public static class OreNode {
        public final int id;
        public final String kind; // "stone", "metal", "sulfur"
        public final int stage;   // 1..4
        public final double x, y, z;
        public final float hx, hy, hz;

        public OreNode(int id, String kind, int stage, double x, double y, double z, float hx, float hy, float hz) {
            this.id = id;
            this.kind = kind;
            this.stage = Math.max(1, Math.min(4, stage));
            this.x = x;
            this.y = y;
            this.z = z;
            this.hx = hx;
            this.hy = hy;
            this.hz = hz;
        }
    }

    public static class RustCrate {
        public final String tier; // common, tools, food, military, elite
        public final int x, y, z;

        public RustCrate(String tier, int x, int y, int z) {
            this.tier = tier;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class RustTree {
        public final int id;
        public final String kind; // pine, birch, oak
        public final double x, y, z;
        public final float radius, height, xAngle, xY;

        public RustTree(int id, String kind, double x, double y, double z,
                        float radius, float height, float xAngle, float xY) {
            this.id = id;
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.height = height;
            this.xAngle = xAngle;
            this.xY = xY;
        }
    }

    public static volatile List<Monument> monuments = Collections.emptyList();
    public static volatile List<OreNode> oreNodes = Collections.emptyList();
    public static volatile List<RustTree> trees = Collections.emptyList();
    public static volatile List<RustCrate> crates = Collections.emptyList();
    public static volatile Map<Long, RustCrate> crateByPos = Collections.emptyMap();
    public static volatile boolean serverSynced = false;
    private static int ticksSinceJoin = 0;
    private static boolean wasInWorld = false;

    private RustWorldSync() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SYNC_CHANNEL, (client, handler, buf, responseSender) -> {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            String payload = new String(bytes, StandardCharsets.UTF_8);
            client.execute(() -> handleMessage(payload));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean inWorld = (client.player != null && client.world != null);
            if (!inWorld) {
                wasInWorld = false;
                serverSynced = false;
                ticksSinceJoin = 0;
                monuments = Collections.emptyList();
                oreNodes = Collections.emptyList();
                trees = Collections.emptyList();
                crates = Collections.emptyList();
                crateByPos = Collections.emptyMap();
                RustResourceFeed.clear();
                RustBuildingSystem.clear();
                RustCraftingQueue.clear();
                return;
            }
            if (!wasInWorld) {
                wasInWorld = true;
                ticksSinceJoin = 0;
            }
            ticksSinceJoin++;
            if (!serverSynced && (ticksSinceJoin == 10 || ticksSinceJoin == 40 || ticksSinceJoin == 100)) {
                sendRaw("REQUEST_SYNC");
            }
        });
    }

    private static void handleMessage(String msg) {
        if (msg == null || msg.isEmpty()) return;
        int pipe = msg.indexOf('|');
        String cmd = pipe >= 0 ? msg.substring(0, pipe) : msg;
        String body = pipe >= 0 ? msg.substring(pipe + 1) : "";

        if ("MONUMENTS".equals(cmd)) {
            List<Monument> list = new ArrayList<>();
            String[] entries = body.split(";");
            for (String e : entries) {
                if (e.isEmpty()) continue;
                String[] p = e.split(",");
                if (p.length >= 6) {
                    try {
                        list.add(new Monument(
                                p[0], p[1],
                                Integer.parseInt(p[2]),
                                Integer.parseInt(p[3]),
                                Integer.parseInt(p[4]),
                                Integer.parseInt(p[5])
                        ));
                    } catch (NumberFormatException ignored) {}
                }
            }
            monuments = list;
            serverSynced = true;
        } else if ("NODES".equals(cmd)) {
            List<OreNode> list = new ArrayList<>();
            if (!body.isEmpty()) {
                for (String e : body.split(";")) {
                    if (e.isEmpty()) continue;
                    String[] p = e.split(",");
                    if (p.length >= 9) {
                        try {
                            list.add(new OreNode(
                                    Integer.parseInt(p[0]),
                                    p[1],
                                    Integer.parseInt(p[2]),
                                    Double.parseDouble(p[3]),
                                    Double.parseDouble(p[4]),
                                    Double.parseDouble(p[5]),
                                    Float.parseFloat(p[6]),
                                    Float.parseFloat(p[7]),
                                    Float.parseFloat(p[8])
                            ));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            oreNodes = list;
        } else if ("TREES".equals(cmd)) {
            List<RustTree> list = new ArrayList<>();
            if (!body.isEmpty()) {
                for (String e : body.split(";")) {
                    if (e.isEmpty()) continue;
                    String[] p = e.split(",");
                    if (p.length >= 9) {
                        try {
                            list.add(new RustTree(
                                    Integer.parseInt(p[0]),
                                    p[1],
                                    Double.parseDouble(p[2]),
                                    Double.parseDouble(p[3]),
                                    Double.parseDouble(p[4]),
                                    Float.parseFloat(p[5]),
                                    Float.parseFloat(p[6]),
                                    Float.parseFloat(p[7]),
                                    Float.parseFloat(p[8])
                            ));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            trees = list;
        } else if ("FARM_FX".equals(cmd)) {
            String[] p = body.split("\\|");
            if (p.length >= 7) {
                try {
                    String fKind = p[0];
                    boolean bonusHit = "1".equals(p[1]);
                    int streak = Integer.parseInt(p[2]);
                    double fx = Double.parseDouble(p[3]);
                    double fy = Double.parseDouble(p[4]);
                    double fz = Double.parseDouble(p[5]);
                    boolean finished = "1".equals(p[6]);
                    MinecraftClient mc = MinecraftClient.getInstance();
                    boolean localPlayed = RustWorldCollider.wasLocalSfxRecent(fKind);
                    if ("ore".equals(fKind)) {
                        if (!localPlayed) {
                            RustViewModelAnimator.playSound(mc, "ore_hit", 0.65f);
                        }
                        if (bonusHit) {
                            if (!localPlayed) {
                                String hsKey = (streak % 2 == 1) ? "ore_hotspot_1" : "ore_hotspot_2";
                                RustViewModelAnimator.playSound(mc, hsKey, 1.0f, 0.92f + Math.min(6, streak) * 0.085f);
                                OreNodeRenderer.triggerHotspotSpark(fx, fy, fz);
                            }
                            if (mc != null && mc.world != null) {
                                for (int i = 0; i < 10; i++) {
                                    mc.world.addParticle(ParticleTypes.CRIT, fx, fy, fz,
                                            (Math.random() - 0.5) * 0.35, 0.12 + Math.random() * 0.25, (Math.random() - 0.5) * 0.35);
                                }
                            }
                        }
                        if (finished) {
                            RustViewModelAnimator.playSound(mc, "ore_break", 0.78f);
                        }
                    } else if ("tree".equals(fKind)) {
                        if (!localPlayed) {
                            RustViewModelAnimator.playSound(mc, Math.random() < 0.5 ? "tree_chop_1" : "tree_chop_2", 0.65f);
                        }
                        RustTreeRenderer.triggerTreeShake(fx, fz);
                        if (bonusHit) {
                            if (!localPlayed) {
                                String tbKey = (streak % 2 == 1) ? "tree_bonus_1" : "tree_bonus_2";
                                RustViewModelAnimator.playSound(mc, tbKey, 0.95f, 0.92f + Math.min(6, streak) * 0.085f);
                                RustViewModelAnimator.playSound(mc, "tree_spray", 0.35f, 1.0f);
                            }
                            if (mc != null && mc.world != null) {
                                for (int i = 0; i < 8; i++) {
                                    mc.world.addParticle(ParticleTypes.CRIT, fx, fy, fz,
                                            (Math.random() - 0.5) * 0.3, 0.08 + Math.random() * 0.2, (Math.random() - 0.5) * 0.3);
                                }
                            }
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        } else if ("GAIN".equals(cmd)) {
            int sep = body.lastIndexOf('|');
            if (sep > 0) {
                try {
                    String label = body.substring(0, sep).trim();
                    int amt = Integer.parseInt(body.substring(sep + 1).trim());
                    RustResourceFeed.add(label, amt);
                } catch (NumberFormatException ignored) {}
            }
        } else if ("BUILD_SYNC".equals(cmd)) {
            List<RustBuildingSystem.BuildingPiece> list = new ArrayList<>();
            if (!body.isEmpty()) {
                for (String e : body.split(";")) {
                    if (e.isEmpty()) continue;
                    String[] p = e.split(",");
                    if (p.length >= 10) {
                        try {
                            list.add(new RustBuildingSystem.BuildingPiece(
                                    Integer.parseInt(p[0]), p[1], p[2],
                                    Double.parseDouble(p[3]), Double.parseDouble(p[4]),
                                    Double.parseDouble(p[5]), Float.parseFloat(p[6]),
                                    Integer.parseInt(p[7]), Integer.parseInt(p[8]), p[9]
                            ));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            RustBuildingSystem.pieces = list;
        } else if ("BUILD_FX".equals(cmd)) {
            String[] p = body.split("\\|");
            if (p.length >= 1) {
                RustViewModelAnimator.playSound(MinecraftClient.getInstance(), p[0], 0.78f);
            }
        } else if ("TREE_FALL".equals(cmd)) {
            String[] p = body.split("\\|");
            if (p.length >= 2) {
                try {
                    int treeId = Integer.parseInt(p[0]);
                    float yaw = Float.parseFloat(p[1]);
                    RustTreeRenderer.triggerFall(treeId, yaw);
                    RustViewModelAnimator.playSound(MinecraftClient.getInstance(), "tree_fall", 0.80f);
                } catch (NumberFormatException ignored) {}
            }
        } else if ("CRATES".equals(cmd)) {
            List<RustCrate> list = new ArrayList<>();
            Map<Long, RustCrate> map = new HashMap<>();
            if (!body.isEmpty()) {
                for (String e : body.split(";")) {
                    if (e.isEmpty()) continue;
                    String[] p = e.split(",");
                    if (p.length >= 4) {
                        try {
                            int x = Integer.parseInt(p[1]);
                            int y = Integer.parseInt(p[2]);
                            int z = Integer.parseInt(p[3]);
                            RustCrate c = new RustCrate(p[0], x, y, z);
                            list.add(c);
                            map.put(BlockPos.asLong(x, y, z), c);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            crates = list;
            crateByPos = map;
        } else if ("STATS".equals(cmd)) {
            String[] p = body.split(";");
            if (p.length >= 4) {
                try {
                    RustHudOverlay.radiation = Integer.parseInt(p[0]);
                    RustHudOverlay.comfort = Integer.parseInt(p[1]);
                    RustHudOverlay.isBleeding = "1".equals(p[2]);
                    RustHudOverlay.hydration = Integer.parseInt(p[3]);
                } catch (NumberFormatException ignored) {}
            }
        } else if ("HIT".equals(cmd)) {
            boolean hs = "1".equals(body.trim());
            net.spacerust.client.hud.RustCrosshairOverlay.triggerHitmarker(hs);
            if (net.spacerust.client.config.RustSettings.get().showHitmarker) {
                net.spacerust.client.guns.RustViewModelAnimator.playSound(
                        net.minecraft.client.MinecraftClient.getInstance(),
                        hs ? "headshot" : "hitmarker",
                        0.65f
                );
            }
        }
    }

    public static boolean hasCrateAt(BlockPos pos) {
        return pos != null && crateByPos.containsKey(pos.asLong());
    }

    public static void sendCraftComplete(String recipeId, int amount) {
        if (recipeId == null || recipeId.isEmpty()) return;
        sendRaw("CRAFT|" + recipeId + "|" + Math.max(1, amount));
    }

    public static void sendSplitStack(int handlerSlotId) {
        sendRaw("SPLIT|" + handlerSlotId);
    }

    public static void sendLootMove(int rawSlotId) {
        sendRaw("LOOT_MOVE|" + rawSlotId);
    }

    public static void sendRaw(String msg) {
        try {
            if (ClientPlayNetworking.canSend(SYNC_CHANNEL)) {
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                buf.writeBytes(msg.getBytes(StandardCharsets.UTF_8));
                ClientPlayNetworking.send(SYNC_CHANNEL, buf);
            }
        } catch (Throwable ignored) {
        }
    }
}
