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
            return piece.createBoundingBox(x, y, z);
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
            HitResult hit = client.player.raycast(7.0, 1.0f, false);
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult bHit = (BlockHitResult) hit;
                Vec3d target = bHit.getPos();
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
        double snapDist = 2.2;
        BuildingPieceData bestPiece = null;
        Vec3d bestSnap = null;
        double bestDist = snapDist;

        for (BuildingPieceData b : pieces) {
            Vec3d pieceCenter = new Vec3d(b.x, b.y, b.z);
            double dist = pieceCenter.distanceTo(hitPos);
            if (dist > 5.0) continue;

            if (piece.isWall) {
                if (b.piece.isFoundation) {
                    // Wall connects to 4 edges of foundation
                    Vec3d[] edges = {
                            new Vec3d(b.x, b.y + 0.5, b.z - 1.5), // North
                            new Vec3d(b.x, b.y + 0.5, b.z + 1.5), // South
                            new Vec3d(b.x - 1.5, b.y + 0.5, b.z), // West
                            new Vec3d(b.x + 1.5, b.y + 0.5, b.z)  // East
                    };
                    for (Vec3d edge : edges) {
                        double d = edge.distanceTo(hitPos);
                        if (d < bestDist) {
                            bestDist = d;
                            bestSnap = edge;
                        }
                    }
                } else if (b.piece.isWall) {
                    // Wall on top of wall
                    Vec3d top = new Vec3d(b.x, b.y + 3.0, b.z);
                    double d = top.distanceTo(hitPos);
                    if (d < bestDist) {
                        bestDist = d;
                        bestSnap = top;
                    }
                }
            } else if (piece == RustBuildingPiece.FLOOR || piece == RustBuildingPiece.FLOOR_TRIANGLE) {
                if (b.piece.isWall) {
                    Vec3d top = new Vec3d(b.x, b.y + 3.0, b.z);
                    double d = top.distanceTo(hitPos);
                    if (d < bestDist) {
                        bestDist = d;
                        bestSnap = top;
                    }
                } else if (b.piece == RustBuildingPiece.FLOOR) {
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
            } else if (piece.isFoundation) {
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
            return bestSnap;
        }

        // Default: snap to terrain surface
        double gx = Math.round(hitPos.x * 2.0) / 2.0;
        double gy = Math.floor(hitPos.y * 2.0) / 2.0;
        double gz = Math.round(hitPos.z * 2.0) / 2.0;
        return new Vec3d(gx, gy, gz);
    }

    public static boolean tryPlaceCurrentPreview(MinecraftClient client) {
        if (!isHoldingBuildingPlan(client) || previewPos == null) return false;

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
            Box bBox = b.getBoundingBox();
            if (pBox.intersects(bBox)) {
                // Standing on foundation / floor top
                if (b.piece.isFoundation || b.piece == RustBuildingPiece.FLOOR) {
                    double topY = b.y + b.piece.height;
                    if (player.getY() >= topY - 0.4 && player.getVelocity().y <= 0) {
                        player.setPosition(player.getX(), topY, player.getZ());
                        player.setOnGround(true);
                        player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z);
                    }
                }
                // Wall pushing (cannot pass through wall)
                else if (b.piece.isWall) {
                    Vec3d pPos = player.getPos();
                    Vec3d wCenter = new Vec3d(b.x, b.y, b.z);
                    Vec3d diff = pPos.subtract(wCenter);
                    if (Math.abs(diff.x) > Math.abs(diff.z)) {
                        double pushX = Math.signum(diff.x) * (b.piece.width / 2.0 + 0.35);
                        player.setPosition(b.x + pushX, player.getY(), player.getZ());
                    } else {
                        double pushZ = Math.signum(diff.z) * (b.piece.depth / 2.0 + 0.35);
                        player.setPosition(player.getX(), player.getY(), b.z + pushZ);
                    }
                }
            }
        }
    }

    public static void renderAll(MatrixStack matrices, VertexConsumerProvider consumers, net.minecraft.client.render.Camera camera, float tickDelta) {
        Vec3d camPos = camera.getPos();

        // 1. Render all placed building pieces
        for (BuildingPieceData b : pieces) {
            String modelName = b.piece.getModelName(b.tier);
            RbmModel model = RbmModel.get(modelName);
            if (model != null) {
                matrices.push();
                matrices.translate(b.x - camPos.x, b.y - camPos.y, b.z - camPos.z);
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-b.yaw));
                model.render(matrices, consumers, 0x00F000F0, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
                matrices.pop();
            }
        }

        // 2. Render Hologram Preview for Building Plan
        MinecraftClient client = MinecraftClient.getInstance();
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
    }
}
