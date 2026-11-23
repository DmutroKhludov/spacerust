package net.spacerust.client.building;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.spacerust.client.SpaceRustClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

public class RustBuildingManager {

    public static class BuildingPieceData {
        public String id;
        public RustBuildingPiece piece;
        public RustBuildingTier tier;
        public double x, y, z;
        public float yaw;
        public float health;
        public float maxHealth;
        public long placedTime;

        public BuildingPieceData() {}

        public BuildingPieceData(RustBuildingPiece piece, RustBuildingTier tier, double x, double y, double z, float yaw) {
            this.id = UUID.randomUUID().toString();
            this.piece = piece;
            this.tier = tier;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.health = tier.maxHealth;
            this.maxHealth = tier.maxHealth;
            this.placedTime = System.currentTimeMillis();
        }

        public Box getBoundingBox() {
            return piece.createBoundingBox(x, y, z, yaw);
        }

        public List<Box> getCollisionBoxes() {
            return piece.getCollisionBoxes(x, y, z, yaw);
        }
    }

    public static final List<BuildingPieceData> pieces = new ArrayList<>();
    public static RustBuildingPiece selectedPiece = RustBuildingPiece.FOUNDATION;
    public static float previewYaw = 0f;
    public static Vec3d previewPos = null;
    public static BuildingPieceData hoveredBuilding = null;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File saveFile = null;

    public static void init() {
        File dir = new File(MinecraftClient.getInstance().runDirectory, ".spacerust");
        if (!dir.exists()) dir.mkdirs();
        saveFile = new File(dir, "buildings.json");
        load();
    }

    public static synchronized void load() {
        if (saveFile == null || !saveFile.exists()) return;
        try (FileReader reader = new FileReader(saveFile)) {
            Type listType = new TypeToken<ArrayList<BuildingPieceData>>(){}.getType();
            List<BuildingPieceData> loaded = GSON.fromJson(reader, listType);
            if (loaded != null) {
                pieces.clear();
                pieces.addAll(loaded);
                System.out.println("[SpaceRust] Loaded " + pieces.size() + " building pieces from disk.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void save() {
        if (saveFile == null) return;
        try (FileWriter writer = new FileWriter(saveFile)) {
            GSON.toJson(pieces, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isHoldingBuildingPlan(MinecraftClient client) {
        if (client.player == null) return false;
        ItemStack stack = client.player.getMainHandStack();
        if (stack.isEmpty()) return false;
        String name = stack.getName().getString().toLowerCase();
        return stack.getItem() == Items.PAPER || stack.getItem() == Items.MAP || stack.getItem() == Items.FILLED_MAP
                || name.contains("план") || name.contains("постройк") || name.contains("building") || name.contains("чертеж");
    }

    public static boolean isHoldingHammer(MinecraftClient client) {
        if (client.player == null) return false;
        ItemStack stack = client.player.getMainHandStack();
        if (stack.isEmpty()) return false;
        String name = stack.getName().getString().toLowerCase();
        return stack.getItem() == Items.WOODEN_AXE || stack.getItem() == Items.WOODEN_SHOVEL
                || name.contains("киян") || name.contains("молот") || name.contains("hammer");
    }

    public static Vec3d getAimTarget(MinecraftClient client, double maxReach) {
        if (client.player == null) return null;
        Vec3d camPos = client.player.getCameraPosVec(1.0f);
        Vec3d lookVec = client.player.getRotationVec(1.0f);
        Vec3d reachVec = camPos.add(lookVec.multiply(maxReach));

        Vec3d closestHit = null;
        double closestDistSq = maxReach * maxReach;

        // 1. Raycast against placed building pieces
        for (BuildingPieceData b : pieces) {
            Box box = b.getBoundingBox();
            Optional<Vec3d> hit = box.raycast(camPos, reachVec);
            if (hit.isPresent()) {
                double distSq = camPos.squaredDistanceTo(hit.get());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closestHit = hit.get();
                }
            }
        }

        // 2. Raycast against vanilla blocks
        HitResult bHit = client.player.raycast(maxReach, 1.0f, false);
        if (bHit.getType() == HitResult.Type.BLOCK) {
            double distSq = camPos.squaredDistanceTo(bHit.getPos());
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closestHit = bHit.getPos();
            }
        }

        // 3. Fallback: looking into air (forward projection along look vector)
        if (closestHit == null) {
            closestHit = camPos.add(lookVec.multiply(4.5));
        }

        return closestHit;
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        // 1. Hover Hit-testing for Hammer
        hoveredBuilding = null;
        Vec3d camPos = client.player.getCameraPosVec(1.0f);
        Vec3d lookVec = client.player.getRotationVec(1.0f);
        Vec3d reachVec = camPos.add(lookVec.multiply(6.0));

        double closestDist = Double.MAX_VALUE;
        for (BuildingPieceData b : pieces) {
            Box box = b.getBoundingBox();
            Optional<Vec3d> hit = box.raycast(camPos, reachVec);
            if (hit.isPresent()) {
                double dist = camPos.squaredDistanceTo(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    hoveredBuilding = b;
                }
            }
        }

        // 2. Hologram Preview Position calculation for Building Plan
        if (isHoldingBuildingPlan(client)) {
            Vec3d target = getAimTarget(client, 8.0);
            if (target != null) {
                previewPos = calculateSnap(target, selectedPiece, previewYaw);
            } else {
                previewPos = null;
            }
        } else {
            previewPos = null;
        }

        // 3. Player Physical Collision with Placed Buildings
        handlePlayerCollision(client.player);
    }

    private static Vec3d calculateSnap(Vec3d hitPos, RustBuildingPiece piece, float yaw) {
        double snapDist = 3.5;
        Vec3d bestSnap = null;
        Float bestYaw = null;
        double bestDist = snapDist;

        for (BuildingPieceData b : pieces) {
            Vec3d pieceCenter = new Vec3d(b.x, b.y, b.z);
            double dist = pieceCenter.distanceTo(hitPos);
            if (dist > 7.0) continue;

            // --- FLOOR & FLOOR_TRIANGLE (Ceilings / Roofs) ---
            if (piece == RustBuildingPiece.FLOOR || piece == RustBuildingPiece.FLOOR_TRIANGLE) {
                // A) Snap directly above foundation (room ceiling)
                if (b.piece.isFoundation) {
                    Vec3d ceilPos = new Vec3d(b.x, b.y + 3.5, b.z);
                    double d = ceilPos.distanceTo(hitPos);
                    if (d < bestDist) {
                        bestDist = d;
                        bestSnap = ceilPos;
                    }
                }
                // B) Snap to top of walls (covers the room on either side of wall)
                else if (b.piece.isWall) {
                    double topY = b.y + b.piece.height;
                    float normYaw = (b.yaw % 360f + 360f) % 360f;
                    Vec3d side1, side2;
                    if ((normYaw > 45 && normYaw < 135) || (normYaw > 225 && normYaw < 315)) {
                        side1 = new Vec3d(b.x + 1.5, topY, b.z);
                        side2 = new Vec3d(b.x - 1.5, topY, b.z);
                    } else {
                        side1 = new Vec3d(b.x, topY, b.z + 1.5);
                        side2 = new Vec3d(b.x, topY, b.z - 1.5);
                    }
                    double d1 = side1.distanceTo(hitPos);
                    double d2 = side2.distanceTo(hitPos);
                    if (d1 < bestDist && d1 <= d2) {
                        bestDist = d1;
                        bestSnap = side1;
                    } else if (d2 < bestDist) {
                        bestDist = d2;
                        bestSnap = side2;
                    }
                }
                // C) Snap to edges of existing floors
                else if (b.piece == RustBuildingPiece.FLOOR) {
                    Vec3d[] floorEdges = {
                            new Vec3d(b.x, b.y, b.z - 3.0),
                            new Vec3d(b.x, b.y, b.z + 3.0),
                            new Vec3d(b.x - 3.0, b.y, b.z),
                            new Vec3d(b.x + 3.0, b.y, b.z)
                    };
                    for (Vec3d edge : floorEdges) {
                        double d = edge.distanceTo(hitPos);
                        if (d < bestDist) {
                            bestDist = d;
                            bestSnap = edge;
                        }
                    }
                }
            }
            // --- WALLS (Wall, Doorway, Window, Frame, Half Wall, Low Wall) ---
            else if (piece.isWall) {
                if (b.piece.isFoundation) {
                    // Connects to 4 edges of foundation with auto yaw
                    Vec3d[] edges = {
                            new Vec3d(b.x, b.y + 0.5, b.z - 1.5), // North
                            new Vec3d(b.x, b.y + 0.5, b.z + 1.5), // South
                            new Vec3d(b.x - 1.5, b.y + 0.5, b.z), // West
                            new Vec3d(b.x + 1.5, b.y + 0.5, b.z)  // East
                    };
                    float[] yaws = { 0f, 180f, 90f, 270f };
                    for (int i = 0; i < edges.length; i++) {
                        Vec3d edge = edges[i];
                        double d = edge.distanceTo(hitPos);
                        if (d < bestDist) {
                            bestDist = d;
                            bestSnap = edge;
                            bestYaw = yaws[i];
                        }
                    }
                } else if (b.piece == RustBuildingPiece.FLOOR) {
                    // Wall on 2nd floor (edges of floor)
                    Vec3d[] edges = {
                            new Vec3d(b.x, b.y, b.z - 1.5), // North
                            new Vec3d(b.x, b.y, b.z + 1.5), // South
                            new Vec3d(b.x - 1.5, b.y, b.z), // West
                            new Vec3d(b.x + 1.5, b.y, b.z)  // East
                    };
                    float[] yaws = { 0f, 180f, 90f, 270f };
                    for (int i = 0; i < edges.length; i++) {
                        Vec3d edge = edges[i];
                        double d = edge.distanceTo(hitPos);
                        if (d < bestDist) {
                            bestDist = d;
                            bestSnap = edge;
                            bestYaw = yaws[i];
                        }
                    }
                } else if (b.piece.isWall) {
                    // Wall on top of wall (2nd floor)
                    Vec3d top = new Vec3d(b.x, b.y + b.piece.height, b.z);
                    double d = top.distanceTo(hitPos);
                    if (d < bestDist) {
                        bestDist = d;
                        bestSnap = top;
                        bestYaw = b.yaw;
                    }
                }
            }
            // --- FOUNDATION ---
            else if (piece.isFoundation) {
                if (b.piece.isFoundation) {
                    Vec3d[] foundEdges = {
                            new Vec3d(b.x, b.y, b.z - 3.0),
                            new Vec3d(b.x, b.y, b.z + 3.0),
                            new Vec3d(b.x - 3.0, b.y, b.z),
                            new Vec3d(b.x + 3.0, b.y, b.z)
                    };
                    for (Vec3d edge : foundEdges) {
                        double d = edge.distanceTo(hitPos);
                        if (d < bestDist) {
                            bestDist = d;
                            bestSnap = edge;
                        }
                    }
                }
            }
        }

        if (bestSnap != null) {
            if (bestYaw != null) {
                previewYaw = bestYaw;
            }
            return bestSnap;
        }

        // Default: snap to terrain surface
        double gx = Math.round(hitPos.x * 2.0) / 2.0;
        double gy = Math.floor(hitPos.y * 2.0) / 2.0;
        double gz = Math.round(hitPos.z * 2.0) / 2.0;
        return new Vec3d(gx, gy, gz);
    }

    private static long lastPlaceTime = 0;

    public static boolean tryPlaceCurrentPreview(MinecraftClient client) {
        if (!isHoldingBuildingPlan(client) || previewPos == null) return false;

        long now = System.currentTimeMillis();
        if (now - lastPlaceTime < 250) return false;

        // Prevent duplicate placement on same position
        for (BuildingPieceData existing : pieces) {
            if (existing.piece == selectedPiece
                    && Math.abs(existing.x - previewPos.x) < 0.15
                    && Math.abs(existing.y - previewPos.y) < 0.15
                    && Math.abs(existing.z - previewPos.z) < 0.15) {
                return false;
            }
        }
        lastPlaceTime = now;

        // Deduct 20 Wood from inventory
        if (!hasResource(client.player, Items.OAK_PLANKS, 20) && !hasResource(client.player, Items.OAK_WOOD, 20)) {
            // Creative or allow placement with whatever wood is available
        }
        consumeResource(client.player, Items.OAK_PLANKS, 20);

        BuildingPieceData piece = new BuildingPieceData(
                selectedPiece,
                RustBuildingTier.TWIG,
                previewPos.x, previewPos.y, previewPos.z,
                previewYaw
        );
        pieces.add(piece);
        save();

        if (client.world != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_WOOD_PLACE, 1.0f));
        }
        return true;
    }

    public static boolean tryUpgradePiece(MinecraftClient client, BuildingPieceData piece, RustBuildingTier targetTier) {
        if (piece == null || targetTier == null) return false;

        consumeResource(client.player, targetTier.materialItem, targetTier.upgradeCost);
        piece.tier = targetTier;
        piece.maxHealth = targetTier.maxHealth;
        piece.health = targetTier.maxHealth;
        save();

        if (client.world != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_ANVIL_USE, 1.0f));
        }
        return true;
    }

    public static void rotatePiece(BuildingPieceData piece) {
        if (piece == null) return;
        piece.yaw = (piece.yaw + 180f) % 360f;
        save();
    }

    public static void demolishPiece(MinecraftClient client, BuildingPieceData piece) {
        if (piece == null) return;
        pieces.remove(piece);
        save();
        if (client.world != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_WOOD_BREAK, 1.0f));
        }
    }

    public static void repairPiece(MinecraftClient client, BuildingPieceData piece) {
        if (piece == null) return;
        piece.health = piece.maxHealth;
        save();
        if (client.world != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_WOOD_HIT, 1.0f));
        }
    }

    private static boolean hasResource(PlayerEntity player, net.minecraft.item.Item item, int amount) {
        if (player == null || player.isCreative()) return true;
        int count = 0;
        for (ItemStack s : player.inventory.main) {
            if (s.getItem() == item) count += s.getCount();
        }
        return count >= amount;
    }

    private static void consumeResource(PlayerEntity player, net.minecraft.item.Item item, int amount) {
        if (player == null || player.isCreative()) return;
        int remaining = amount;
        for (int i = 0; i < player.inventory.main.size(); i++) {
            ItemStack s = player.inventory.main.get(i);
            if (s.getItem() == item) {
                int take = Math.min(remaining, s.getCount());
                s.decrement(take);
                remaining -= take;
                if (remaining <= 0) break;
            }
        }
    }

    private static void handlePlayerCollision(PlayerEntity player) {
        if (player == null) return;
        Box pBox = player.getBoundingBox();

        for (BuildingPieceData b : pieces) {
            for (Box bBox : b.getCollisionBoxes()) {
                if (pBox.intersects(bBox)) {
                    double topY = bBox.maxY;
                    // Player standing / landing on top of this collision surface:
                    // (foundation top, floor surface, low wall top, half wall top, wall top)
                    if (player.getY() >= topY - 0.35 && player.getY() <= topY + 0.4 && player.getVelocity().y <= 0) {
                        if (player.getY() < topY) {
                            player.setPosition(player.getX(), topY, player.getZ());
                            player.setVelocity(player.getVelocity().x, Math.max(0, player.getVelocity().y), player.getVelocity().z);
                            player.setOnGround(true);
                        }
                    } else if (player.getY() < topY - 0.35) {
                        // Walking into side of box: push out horizontally with zero penetration
                        pushPlayerOutOfBox(player, pBox, bBox);
                    }
                }
            }
        }
    }

    private static void pushPlayerOutOfBox(PlayerEntity player, Box pBox, Box bBox) {
        double overlapX1 = pBox.maxX - bBox.minX;
        double overlapX2 = bBox.maxX - pBox.minX;
        double overlapZ1 = pBox.maxZ - bBox.minZ;
        double overlapZ2 = bBox.maxZ - pBox.minZ;

        double minOverlapX = (overlapX1 < overlapX2) ? -overlapX1 : overlapX2;
        double minOverlapZ = (overlapZ1 < overlapZ2) ? -overlapZ1 : overlapZ2;

        if (Math.abs(minOverlapX) < Math.abs(minOverlapZ)) {
            player.setPosition(player.getX() + minOverlapX, player.getY(), player.getZ());
            if (minOverlapX * player.getVelocity().x < 0) {
                player.setVelocity(0, player.getVelocity().y, player.getVelocity().z);
            }
        } else {
            player.setPosition(player.getX(), player.getY(), player.getZ() + minOverlapZ);
            if (minOverlapZ * player.getVelocity().z < 0) {
                player.setVelocity(player.getVelocity().x, player.getVelocity().y, 0);
            }
        }
    }

    public static void renderAll(MatrixStack matrices, VertexConsumerProvider consumers, net.minecraft.client.render.Camera camera, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d camPos = camera.getPos();

        // 1. Render all placed building pieces
        for (BuildingPieceData b : pieces) {
            String modelName = b.piece.getModelName(b.tier);
            RbmModel model = RbmModel.get(modelName);
            if (model != null) {
                int light = 0x00F000F0;
                if (client.world != null) {
                    light = net.minecraft.client.render.WorldRenderer.getLightmapCoordinates(
                            client.world,
                            new net.minecraft.util.math.BlockPos(b.x, b.y + 0.5, b.z)
                    );
                }
                matrices.push();
                matrices.translate(b.x - camPos.x, b.y - camPos.y, b.z - camPos.z);
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-b.yaw));
                model.render(matrices, consumers, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
                matrices.pop();
            }
        }

        // 2. Render Hologram Preview for Building Plan
        if (isHoldingBuildingPlan(client) && previewPos != null) {
            String previewModelName = selectedPiece.getModelName(RustBuildingTier.TWIG);
            RbmModel pModel = RbmModel.get(previewModelName);
            if (pModel != null) {
                matrices.push();
                matrices.translate(previewPos.x - camPos.x, previewPos.y - camPos.y, previewPos.z - camPos.z);
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-previewYaw));
                // Rust signature holographic cyan glow (r=0.25, g=0.75, b=1.0, alpha=0.55)
                pModel.render(matrices, consumers, 0x00F000F0, OverlayTexture.DEFAULT_UV, 0.25f, 0.75f, 1.0f, 0.55f);
                matrices.pop();
            }
        }

        // 3. Immediately flush buffers so all building quads draw with world matrices & depth test
        if (consumers instanceof VertexConsumerProvider.Immediate) {
            ((VertexConsumerProvider.Immediate) consumers).draw();
        }
    }
}
