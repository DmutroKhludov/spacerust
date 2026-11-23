package net.spacerust.client.deployables;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.spacerust.client.building.RbmModel;
import net.spacerust.client.building.RustBuildingManager;
import net.spacerust.client.building.RustBuildingPiece;
import net.spacerust.client.building.RustBuildingTier;
import net.spacerust.client.sound.RustSoundManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

public class RustDeployableManager {

    public static final List<RustDeployableData> deployables = new ArrayList<>();
    public static RustDeployableData hoveredDeployable = null;
    public static Vec3d previewPos = null;
    public static float previewYaw = 0f;
    public static RustDeployableType heldDeployableType = null;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File saveFile = null;
    private static long lastDecayTick = 0;

    public static void init() {
        File dir = new File(MinecraftClient.getInstance().runDirectory, ".spacerust");
        if (!dir.exists()) dir.mkdirs();
        saveFile = new File(dir, "deployables.json");
        load();
    }

    public static synchronized void load() {
        if (saveFile == null || !saveFile.exists()) return;
        try (FileReader reader = new FileReader(saveFile)) {
            Type listType = new TypeToken<ArrayList<RustDeployableData>>(){}.getType();
            List<RustDeployableData> loaded = GSON.fromJson(reader, listType);
            if (loaded != null) {
                deployables.clear();
                deployables.addAll(loaded);
                System.out.println("[SpaceRust] Loaded " + deployables.size() + " deployables from disk.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void save() {
        if (saveFile == null) return;
        try (FileWriter writer = new FileWriter(saveFile)) {
            GSON.toJson(deployables, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static RustDeployableType getHeldDeployable(MinecraftClient client) {
        if (client.player == null) return null;
        ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty()) return null;

        String name = held.getName().getString().toLowerCase();

        // 1. Check exact enum names or Russian titles
        for (RustDeployableType t : RustDeployableType.values()) {
            if (name.contains(t.name().toLowerCase()) || name.contains(t.title.toLowerCase()) || name.contains(t.modelName.toLowerCase())) {
                return t;
            }
        }

        // 2. Fallbacks based on item type and keyword matching
        if (held.getItem() == Items.OAK_DOOR || name.contains("wood_door") || name.contains("деревянная дверь")) {
            return RustDeployableType.DOOR_WOOD;
        } else if (held.getItem() == Items.IRON_DOOR || name.contains("sheet_door") || name.contains("металлическая дверь")) {
            return RustDeployableType.DOOR_METAL;
        } else if (name.contains("armored_door") || name.contains("бронированная дверь")) {
            return RustDeployableType.DOOR_ARMORED;
        } else if (held.getItem() == Items.IRON_TRAPDOOR || name.contains("garage_door") || name.contains("гаражная дверь")) {
            return RustDeployableType.GARAGE_DOOR;
        } else if (held.getItem() == Items.BOOKSHELF || name.contains("шкаф") || name.contains("cupboard")) {
            return RustDeployableType.TOOL_CUPBOARD;
        } else if (held.getItem() == Items.CHEST || name.contains("small_box") || name.contains("маленький ящик")) {
            return RustDeployableType.SMALL_BOX;
        } else if (held.getItem() == Items.BARREL || name.contains("large_box") || name.contains("большой ящик") || name.contains("большой сундук")) {
            return RustDeployableType.LARGE_BOX;
        } else if (held.getItem() == Items.LEVER || name.contains("code_lock") || name.contains("кодовый замок") || name.contains("код")) {
            return RustDeployableType.CODE_LOCK;
        } else if (held.getItem() == Items.TRIPWIRE_HOOK || name.contains("key_lock") || name.contains("соло замок") || name.contains("замок")) {
            return RustDeployableType.KEY_LOCK;
        } else if (held.getItem() == Items.CAMPFIRE || name.contains("костер") || name.contains("campfire")) {
            return RustDeployableType.CAMPFIRE;
        } else if (held.getItem() == Items.FURNACE || name.contains("печь") || name.contains("furnace")) {
            return RustDeployableType.FURNACE;
        } else if (held.getItem() == Items.WHITE_BED || name.contains("спальник") || name.contains("спальный мешок") || name.contains("sleeping_bag")) {
            return RustDeployableType.SLEEPING_BAG;
        } else if (name.contains("верстак 1") || name.contains("workbench 1") || (held.getItem() == Items.CRAFTING_TABLE && name.contains("верстак 1"))) {
            return RustDeployableType.WORKBENCH_1;
        } else if (name.contains("верстак 2") || name.contains("workbench 2") || (held.getItem() == Items.SMITHING_TABLE && name.contains("верстак 2"))) {
            return RustDeployableType.WORKBENCH_2;
        } else if (name.contains("верстак 3") || name.contains("workbench 3")) {
            return RustDeployableType.WORKBENCH_3;
        }
        return null;
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        // 1. Animate open/close progress for doors
        for (RustDeployableData d : deployables) {
            if (d.type.isDoor) {
                float targetProg = d.isOpen ? 1.0f : 0.0f;
                if (d.openProgress < targetProg) {
                    d.openProgress = Math.min(targetProg, d.openProgress + 0.15f);
                } else if (d.openProgress > targetProg) {
                    d.openProgress = Math.max(targetProg, d.openProgress - 0.15f);
                }
            }
        }

        // 2. Hover detection (4.5 block reach for interact)
        hoveredDeployable = null;
        Vec3d camPos = client.player.getCameraPosVec(1.0f);
        Vec3d lookVec = client.player.getRotationVec(1.0f);
        Vec3d reachVec = camPos.add(lookVec.multiply(4.5));

        double closestDist = Double.MAX_VALUE;
        for (RustDeployableData d : deployables) {
            Box box = d.getBoundingBox();
            Optional<Vec3d> hit = box.raycast(camPos, reachVec);
            if (hit.isPresent()) {
                double dist = camPos.squaredDistanceTo(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    hoveredDeployable = d;
                }
            }
        }

        // 3. Held Deployable Hologram Preview
        heldDeployableType = getHeldDeployable(client);
        if (heldDeployableType != null) {
            calculatePreview(client, heldDeployableType);
        } else {
            previewPos = null;
        }

        // 4. Physical Collision with closed doors, boxes, and TC
        handleCollision(client.player);

        // 5. Decay System Tick (Every 5 seconds)
        long now = System.currentTimeMillis();
        if (now - lastDecayTick > 5000) {
            lastDecayTick = now;
            tickDecay();
        }
    }

    private static void calculatePreview(MinecraftClient client, RustDeployableType type) {
        Vec3d camPos = client.player.getCameraPosVec(1.0f);

        // A) Single Doors snap strictly into DOORWAY
        if (type.isDoor && !type.isDoubleDoor && type != RustDeployableType.GARAGE_DOOR) {
            double bestDist = 4.0;
            RustBuildingManager.BuildingPieceData bestPiece = null;
            for (RustBuildingManager.BuildingPieceData b : RustBuildingManager.pieces) {
                if (b.piece == RustBuildingPiece.DOORWAY) {
                    double dist = new Vec3d(b.x, b.y, b.z).distanceTo(camPos);
                    if (dist < bestDist) {
                        boolean hasDoor = false;
                        for (RustDeployableData d : deployables) {
                            if (d.type.isDoor && Math.hypot(d.x - b.x, d.z - b.z) < 0.5 && Math.abs(d.y - b.y) < 1.0) {
                                hasDoor = true;
                                break;
                            }
                        }
                        if (!hasDoor) {
                            bestDist = dist;
                            bestPiece = b;
                        }
                    }
                }
            }
            if (bestPiece != null) {
                previewPos = new Vec3d(bestPiece.x, bestPiece.y, bestPiece.z);
                previewYaw = bestPiece.yaw;
                return;
            }
            previewPos = null;
            return;
        }

        // B) Double Doors and Garage Doors snap strictly into FRAME
        if (type.isDoubleDoor || type == RustDeployableType.GARAGE_DOOR) {
            double bestDist = 4.0;
            RustBuildingManager.BuildingPieceData bestPiece = null;
            for (RustBuildingManager.BuildingPieceData b : RustBuildingManager.pieces) {
                if (b.piece == RustBuildingPiece.FRAME) {
                    double dist = new Vec3d(b.x, b.y, b.z).distanceTo(camPos);
                    if (dist < bestDist) {
                        boolean hasDoor = false;
                        for (RustDeployableData d : deployables) {
                            if (d.type.isDoor && Math.hypot(d.x - b.x, d.z - b.z) < 0.5 && Math.abs(d.y - b.y) < 1.0) {
                                hasDoor = true;
                                break;
                            }
                        }
                        if (!hasDoor) {
                            bestDist = dist;
                            bestPiece = b;
                        }
                    }
                }
            }
            if (bestPiece != null) {
                previewPos = new Vec3d(bestPiece.x, bestPiece.y, bestPiece.z);
                previewYaw = bestPiece.yaw;
                return;
            }
            previewPos = null;
            return;
        }

        // C) Locks snap onto Doors or Large Boxes
        if (type == RustDeployableType.KEY_LOCK || type == RustDeployableType.CODE_LOCK) {
            if (hoveredDeployable != null && (hoveredDeployable.type.isDoor || hoveredDeployable.type == RustDeployableType.LARGE_BOX)) {
                if (hoveredDeployable.lockType == RustDeployableData.LockType.NONE) {
                    previewPos = new Vec3d(hoveredDeployable.x, hoveredDeployable.y, hoveredDeployable.z);
                    previewYaw = hoveredDeployable.yaw;
                    return;
                }
            }
            previewPos = null;
            return;
        }

        // D) Deployables (TC, Boxes) snap onto Foundations, Floors, or Ground
        HitResult hit = client.player.raycast(5.0, 1.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bHit = (BlockHitResult) hit;
            Vec3d target = bHit.getPos();
            previewPos = new Vec3d(
                    Math.round(target.x * 2.0) / 2.0,
                    target.y,
                    Math.round(target.z * 2.0) / 2.0
            );
            float rawYaw = client.player.yaw;
            previewYaw = Math.round(rawYaw / 45.0f) * 45.0f;
        } else {
            previewPos = null;
        }
    }

    public static boolean tryPlaceHeld(MinecraftClient client) {
        if (heldDeployableType == null || previewPos == null || client.player == null) return false;

        // Attach Lock
        if (heldDeployableType == RustDeployableType.KEY_LOCK || heldDeployableType == RustDeployableType.CODE_LOCK) {
            if (hoveredDeployable != null && (hoveredDeployable.type.isDoor || hoveredDeployable.type == RustDeployableType.LARGE_BOX)) {
                hoveredDeployable.lockType = (heldDeployableType == RustDeployableType.KEY_LOCK) ?
                        RustDeployableData.LockType.KEY_LOCK : RustDeployableData.LockType.CODE_LOCK;
                hoveredDeployable.isLocked = true;
                hoveredDeployable.ownerUuid = client.player.getUuidAsString();
                hoveredDeployable.authorizedPlayers.add(client.player.getUuidAsString());
                save();
                RustSoundManager.play("door_lock_install", 1.0f, 1.0f);
                consumeHeld(client);
                return true;
            }
            return false;
        }

        // Place Deployable / Door
        RustDeployableData data = new RustDeployableData(
                heldDeployableType,
                previewPos.x, previewPos.y, previewPos.z,
                previewYaw,
                client.player.getUuidAsString()
        );
        deployables.add(data);
        save();
        RustSoundManager.play("inventory_pickup_wood", 1.0f, 0.9f);
        consumeHeld(client);
        return true;
    }

    private static void consumeHeld(MinecraftClient client) {
        if (client.player == null || client.player.isCreative()) return;
        client.player.getMainHandStack().decrement(1);
    }

    public static void interact(MinecraftClient client) {
        if (client.player == null || hoveredDeployable == null) return;
        RustDeployableData d = hoveredDeployable;
        String playerUuid = client.player.getUuidAsString();

        // 1. Tool Cupboard
        if (d.type == RustDeployableType.TOOL_CUPBOARD) {
            client.openScreen(new RustToolCupboardScreen(d));
            RustSoundManager.play("door_knock", 1.0f, 1.0f);
            return;
        }

        // 2. Small / Large Box
        if (d.type == RustDeployableType.SMALL_BOX || d.type == RustDeployableType.LARGE_BOX) {
            if (!d.isAuthorized(playerUuid)) {
                if (d.lockType == RustDeployableData.LockType.CODE_LOCK) {
                    client.openScreen(new RustCodeLockScreen(d));
                } else {
                    RustSoundManager.play("door_denied", 1.0f, 1.0f);
                }
                return;
            }
            client.openScreen(new RustContainerScreen(d));
            RustSoundManager.play(d.type.openSound, 1.0f, 1.0f);
            return;
        }

        // 3. Doors (Open / Close or Code Lock Screen)
        if (d.type.isDoor) {
            if (!d.isAuthorized(playerUuid)) {
                if (d.lockType == RustDeployableData.LockType.CODE_LOCK) {
                    client.openScreen(new RustCodeLockScreen(d));
                } else {
                    RustSoundManager.play("door_denied", 1.0f, 1.0f);
                }
                return;
            }

            // Toggle Open / Close
            d.isOpen = !d.isOpen;
            save();
            String snd = d.isOpen ? d.type.openSound : d.type.closeSound;
            if (snd != null) RustSoundManager.play(snd, 1.0f, 1.0f);
        }
    }

    private static void handleCollision(PlayerEntity player) {
        if (player == null) return;
        Box pBox = player.getBoundingBox();

        for (RustDeployableData d : deployables) {
            Box bBox = d.getBoundingBox();
            if (pBox.intersects(bBox)) {
                double topY = bBox.maxY;
                if (player.getY() >= topY - 0.35 && player.getY() <= topY + 0.4 && player.getVelocity().y <= 0) {
                    if (player.getY() < topY) {
                        player.setPosition(player.getX(), topY, player.getZ());
                        player.setVelocity(player.getVelocity().x, Math.max(0, player.getVelocity().y), player.getVelocity().z);
                        player.setOnGround(true);
                    }
                } else if (player.getY() < topY - 0.35) {
                    pushPlayerOutOfBox(player, pBox, bBox);
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

    private static void tickDecay() {
        long now = System.currentTimeMillis();
        // Check decay on all placed building pieces
        for (Iterator<RustBuildingManager.BuildingPieceData> it = RustBuildingManager.pieces.iterator(); it.hasNext(); ) {
            RustBuildingManager.BuildingPieceData b = it.next();

            // Find closest TC within 30 blocks
            RustDeployableData tc = null;
            double closestDistSq = 30.0 * 30.0;
            for (RustDeployableData d : deployables) {
                if (d.type == RustDeployableType.TOOL_CUPBOARD) {
                    double distSq = (b.x - d.x) * (b.x - d.x) + (b.z - d.z) * (b.z - d.z);
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        tc = d;
                    }
                }
            }

            boolean isProtected = (tc != null && tc.protectedUntil > now);
            if (!isProtected) {
                // Decay damage based on tier
                float decayRate = (b.tier == RustBuildingTier.TWIG) ? 1.5f :
                        ((b.tier == RustBuildingTier.WOOD) ? 0.8f :
                                ((b.tier == RustBuildingTier.STONE) ? 0.4f :
                                        ((b.tier == RustBuildingTier.METAL) ? 0.2f : 0.1f)));
                b.health -= decayRate;
                if (b.health <= 0) {
                    it.remove();
                    RustSoundManager.playAt("inventory_pickup_stone", b.x, b.y, b.z, 1.0f, 0.7f);
                }
            }
        }
        RustBuildingManager.save();
    }

    private static void renderLock(MatrixStack matrices, VertexConsumerProvider consumers, int light, RustDeployableData.LockType lockType, double lx, double ly, double lz) {
        String lockModel = (lockType == RustDeployableData.LockType.CODE_LOCK) ? "lock_code_locked" : "lock_key_locked";
        RbmModel lModel = RbmModel.get(lockModel);
        if (lModel != null) {
            matrices.push();
            matrices.translate(lx, ly, lz);
            matrices.scale(0.85f, 0.85f, 0.85f);
            lModel.render(matrices, consumers, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
            matrices.pop();
        }
    }

    public static void renderAll(MatrixStack matrices, VertexConsumerProvider consumers, net.minecraft.client.render.Camera camera, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d camPos = camera.getPos();

        // 1. Render all placed deployables
        for (RustDeployableData d : deployables) {
            int light = 0x00F000F0;
            if (client.world != null) {
                light = WorldRenderer.getLightmapCoordinates(client.world, new BlockPos(d.x, d.y + 0.5, d.z));
            }

            matrices.push();
            matrices.translate(d.x - camPos.x, d.y - camPos.y, d.z - camPos.z);
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-d.yaw));

            if (d.type == RustDeployableType.GARAGE_DOOR) {
                // Garage Door Frame
                RbmModel frame = RbmModel.get("garage_door_frame");
                if (frame != null) frame.render(matrices, consumers, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);

                // Garage Door Roll-up
                matrices.push();
                matrices.translate(0, d.openProgress * 2.4, 0);
                RbmModel door = RbmModel.get("garage_door_0");
                if (door != null) door.render(matrices, consumers, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
                if (d.lockType != RustDeployableData.LockType.NONE) {
                    renderLock(matrices, consumers, light, d.lockType, 0, 0.45, 0.12);
                }
                matrices.pop();

            } else if (d.type.isDoubleDoor) {
                // Double door frame
                String frameName = (d.type == RustDeployableType.DOOR_DOUBLE_ARMORED) ? "door_double_armored_frame" :
                        ((d.type == RustDeployableType.DOOR_DOUBLE_METAL) ? "door_double_metal_frame" : "door_double_wood_frame");
                RbmModel frame = RbmModel.get(frameName);
                if (frame != null) frame.render(matrices, consumers, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);

                String leaf0 = (d.type == RustDeployableType.DOOR_DOUBLE_ARMORED) ? "door_double_armored_0" :
                        ((d.type == RustDeployableType.DOOR_DOUBLE_METAL) ? "door_double_metal_0" : "door_double_wood_0");
                String leaf1 = (d.type == RustDeployableType.DOOR_DOUBLE_ARMORED) ? "door_double_armored_1" :
                        ((d.type == RustDeployableType.DOOR_DOUBLE_METAL) ? "door_double_metal_1" : "door_double_wood_1");

                // Right Leaf (hinge at +1.20)
                matrices.push();
                matrices.translate(1.20, 0, 0);
                if (d.openProgress > 0f) {
                    matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(d.openProgress * 90f));
                }
                matrices.translate(-1.20, 0, 0);
                RbmModel m0 = RbmModel.get(leaf0);
                if (m0 != null) m0.render(matrices, consumers, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
                if (d.lockType != RustDeployableData.LockType.NONE) {
                    renderLock(matrices, consumers, light, d.lockType, 0.15, 1.10, 0.10);
                }
                matrices.pop();

                // Left Leaf (hinge at -1.20)
                matrices.push();
                matrices.translate(-1.20, 0, 0);
                if (d.openProgress > 0f) {
                    matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-d.openProgress * 90f));
                }
                matrices.translate(1.20, 0, 0);
                RbmModel m1 = RbmModel.get(leaf1);
                if (m1 != null) m1.render(matrices, consumers, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
                matrices.pop();

            } else if (d.type.isDoor) {
                // Single Door (hinge at -0.60)
                matrices.push();
                matrices.translate(-0.60, 0, 0);
                if (d.openProgress > 0f) {
                    matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-d.openProgress * 90f));
                }
                RbmModel model = RbmModel.get(d.type.modelName);
                if (model != null) model.render(matrices, consumers, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
                if (d.lockType != RustDeployableData.LockType.NONE) {
                    renderLock(matrices, consumers, light, d.lockType, 1.05, 1.10, 0.12);
                }
                matrices.pop();

            } else if (d.type == RustDeployableType.LARGE_BOX) {
                RbmModel model = RbmModel.get(d.type.modelName);
                if (model != null) model.render(matrices, consumers, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
                if (d.lockType != RustDeployableData.LockType.NONE) {
                    renderLock(matrices, consumers, light, d.lockType, 0, 0.45, 0.52);
                }

            } else {
                // Tool Cupboard, Small Box
                RbmModel model = RbmModel.get(d.type.modelName);
                if (model != null) model.render(matrices, consumers, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
            }

            matrices.pop();
        }

        // 2. Render Hologram Preview for held deployable
        if (heldDeployableType != null && previewPos != null) {
            matrices.push();
            matrices.translate(previewPos.x - camPos.x, previewPos.y - camPos.y, previewPos.z - camPos.z);
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-previewYaw));

            if (heldDeployableType == RustDeployableType.GARAGE_DOOR) {
                RbmModel frame = RbmModel.get("garage_door_frame");
                if (frame != null) frame.render(matrices, consumers, 0x00F000F0, OverlayTexture.DEFAULT_UV, 0.25f, 0.75f, 1.0f, 0.55f);
                RbmModel door = RbmModel.get("garage_door_0");
                if (door != null) door.render(matrices, consumers, 0x00F000F0, OverlayTexture.DEFAULT_UV, 0.25f, 0.75f, 1.0f, 0.55f);
            } else if (heldDeployableType.isDoubleDoor) {
                String frameName = (heldDeployableType == RustDeployableType.DOOR_DOUBLE_ARMORED) ? "door_double_armored_frame" :
                        ((heldDeployableType == RustDeployableType.DOOR_DOUBLE_METAL) ? "door_double_metal_frame" : "door_double_wood_frame");
                RbmModel frame = RbmModel.get(frameName);
                if (frame != null) frame.render(matrices, consumers, 0x00F000F0, OverlayTexture.DEFAULT_UV, 0.25f, 0.75f, 1.0f, 0.55f);
                String leaf0 = (heldDeployableType == RustDeployableType.DOOR_DOUBLE_ARMORED) ? "door_double_armored_0" :
                        ((heldDeployableType == RustDeployableType.DOOR_DOUBLE_METAL) ? "door_double_metal_0" : "door_double_wood_0");
                String leaf1 = (heldDeployableType == RustDeployableType.DOOR_DOUBLE_ARMORED) ? "door_double_armored_1" :
                        ((heldDeployableType == RustDeployableType.DOOR_DOUBLE_METAL) ? "door_double_metal_1" : "door_double_wood_1");
                RbmModel m0 = RbmModel.get(leaf0);
                if (m0 != null) m0.render(matrices, consumers, 0x00F000F0, OverlayTexture.DEFAULT_UV, 0.25f, 0.75f, 1.0f, 0.55f);
                RbmModel m1 = RbmModel.get(leaf1);
                if (m1 != null) m1.render(matrices, consumers, 0x00F000F0, OverlayTexture.DEFAULT_UV, 0.25f, 0.75f, 1.0f, 0.55f);
            } else if (heldDeployableType.isDoor) {
                matrices.push();
                matrices.translate(-0.60, 0, 0);
                RbmModel model = RbmModel.get(heldDeployableType.modelName);
                if (model != null) model.render(matrices, consumers, 0x00F000F0, OverlayTexture.DEFAULT_UV, 0.25f, 0.75f, 1.0f, 0.55f);
                matrices.pop();
            } else {
                RbmModel pModel = RbmModel.get(heldDeployableType.modelName);
                if (pModel != null) {
                    pModel.render(matrices, consumers, 0x00F000F0, OverlayTexture.DEFAULT_UV, 0.25f, 0.75f, 1.0f, 0.55f);
                }
            }

            matrices.pop();
        }
    }
}
