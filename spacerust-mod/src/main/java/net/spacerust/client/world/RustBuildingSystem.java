package net.spacerust.client.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.spacerust.client.gui.RustCraftingScreen;

import java.util.*;

/**
 * Client-side 3D Rust Building System (Building Plan + Киянка/Hammer):
 * - Holds synced BuildingPiece list from Purpur server
 * - Computes magnetic Rust 3.0m grid snap sockets for Foundations, Walls, Doorways, Windows,
 *   Floors, Halfwalls, Lowwalls, Roofs, Stairs, and Triangles
 * - Handles raycasting against placed pieces (building snap, Hammer upgrade/repair, weapon hits)
 */
public final class RustBuildingSystem {

    public static class BuildingPiece {
        public final int id;
        public final String type;
        public String tier; // twig, wood, stone, metal, armored
        public final double x, y, z;
        public float yaw;
        public int hp, maxHp;
        public final String ownerName;

        public BuildingPiece(int id, String type, String tier, double x, double y, double z,
                             float yaw, int hp, int maxHp, String ownerName) {
            this.id = id;
            this.type = type;
            this.tier = tier;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.hp = hp;
            this.maxHp = maxHp;
            this.ownerName = ownerName;
        }
    }

    public static class BuildHit {
        public final BuildingPiece piece;
        public final double t;
        public final Vec3d hitPos;

        public BuildHit(BuildingPiece piece, double t, Vec3d hitPos) {
            this.piece = piece;
            this.t = t;
            this.hitPos = hitPos;
        }
    }

    public static class SnapTarget {
        public final String type;
        public final double x, y, z;
        public final float yaw;
        public final boolean valid;
        public final int woodCost;

        public SnapTarget(String type, double x, double y, double z, float yaw, boolean valid, int woodCost) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.valid = valid;
            this.woodCost = woodCost;
        }
    }

    public static volatile List<BuildingPiece> pieces = Collections.emptyList();
    public static String selectedType = "foundation";
    public static int rotStep = 0; // manual R rotation offset
    private static int localNextId = 10000;
    private static long lastActionMs = 0L;

    private static final List<BuildingPiece> SINGLEPLAYER_PIECES = new ArrayList<>();

    private RustBuildingSystem() {}

    public static void clear() {
        pieces = Collections.emptyList();
        SINGLEPLAYER_PIECES.clear();
    }

    public static boolean isHoldingPlan(MinecraftClient mc) {
        if (mc == null || mc.player == null) return false;
        return "build_plan".equals(net.spacerust.client.SpaceRustItems.getId(mc.player.getMainHandStack()));
    }

    public static boolean isHoldingHammer(MinecraftClient mc) {
        if (mc == null || mc.player == null) return false;
        return "wood_hammer".equals(net.spacerust.client.SpaceRustItems.getId(mc.player.getMainHandStack()));
    }

    public static boolean isHoldingDeployable(MinecraftClient mc) {
        if (mc == null || mc.player == null) return false;
        String id = net.spacerust.client.SpaceRustItems.getId(mc.player.getMainHandStack());
        if (id == null) return false;
        return "wood_door".equals(id) || "sheet_door".equals(id) || "small_box".equals(id) || "large_box".equals(id)
                || "furnace".equals(id) || "campfire".equals(id) || "bed".equals(id)
                || "workbench_1".equals(id) || "workbench_2".equals(id)
                || "ladder".equals(id) || "wood_barricade".equals(id) || "wood_gate".equals(id)
                || "metal_bars".equals(id) || "window_shutter".equals(id) || "code_lock".equals(id)
                || "key_lock".equals(id) || "solar_panel".equals(id) || "guitar".equals(id);
    }

    public static void rotateBlueprint(MinecraftClient mc) {
        rotStep = (rotStep + 1) % 12;
        if (mc != null && mc.player != null) {
            net.spacerust.client.gui.RustUiHelper.playClick(mc);
        }
    }

    public static int woodCostFor(String type) {
        boolean small = "halfwall".equals(type) || "lowwall".equals(type)
                || "foundation_tri".equals(type) || "floor_tri".equals(type);
        return small ? 25 : 50;
    }

    public static int upgradeCost(String type, String tier) {
        boolean small = "halfwall".equals(type) || "lowwall".equals(type)
                || "foundation_tri".equals(type) || "floor_tri".equals(type);
        if ("wood".equals(tier)) return small ? 100 : 200;
        if ("stone".equals(tier)) return small ? 150 : 300;
        if ("metal".equals(tier)) return small ? 100 : 200;
        if ("armored".equals(tier)) return small ? 13 : 25;
        return woodCostFor(type);
    }

    public static String displayTypeName(String type) {
        if ("foundation".equals(type)) return "ФУНДАМЕНТ";
        if ("foundation_tri".equals(type)) return "ТРЕУГ. ФУНДАМЕНТ";
        if ("wall".equals(type)) return "СТЕНА";
        if ("doorway".equals(type)) return "ДВЕРНОЙ ПРОЕМ";
        if ("window".equals(type)) return "ОКНО";
        if ("floor".equals(type)) return "ПОЛ / ПОТОЛОК";
        if ("floor_tri".equals(type)) return "ТРЕУГ. ПОЛ";
        if ("halfwall".equals(type)) return "ПОЛУСТЕНА";
        if ("lowwall".equals(type)) return "НИЗКАЯ СТЕНА";
        if ("roof".equals(type)) return "КРЫША";
        if ("ramp".equals(type)) return "РАМПА";
        if ("stairs".equals(type)) return "ЛЕСТНИЦА";
        return type.toUpperCase(Locale.ROOT);
    }

    public static String displayTierName(String tier) {
        if ("wood".equals(tier)) return "ДЕРЕВО";
        if ("stone".equals(tier)) return "КАМЕНЬ";
        if ("metal".equals(tier)) return "МЕТАЛЛ";
        if ("armored".equals(tier)) return "МВК";
        return "СОЛОМА";
    }

    public static boolean canAffordTwig(PlayerEntity player, String type) {
        if (player == null) return false;
        if (player.isCreative()) return true;
        return RustCraftingScreen.countPlayerResource(player, Items.OAK_PLANKS) >= woodCostFor(type);
    }

    public static void placeCurrentBlueprint(MinecraftClient mc) {
        if (mc == null || mc.player == null) return;
        long now = System.currentTimeMillis();
        if (now - lastActionMs < 180L) return;

        SnapTarget snap = computeSnapTarget(mc);
        if (snap == null || !snap.valid) return;
        lastActionMs = now;

        net.spacerust.client.guns.RustViewModelAnimator.playSound(mc, "build_place", 0.75f);
        if (mc.isInSingleplayer()) {
            if (!mc.player.isCreative()) {
                RustCraftingScreen.deductPlayerResource(mc.player, Items.OAK_PLANKS, snap.woodCost);
            }
            BuildingPiece bp = new BuildingPiece(localNextId++, snap.type, "twig",
                    snap.x, snap.y, snap.z, snap.yaw, 10, 10, mc.player.getName().getString());
            SINGLEPLAYER_PIECES.add(bp);
            pieces = new ArrayList<>(SINGLEPLAYER_PIECES);
        } else {
            RustWorldSync.sendRaw(String.format(Locale.US, "BUILD_PLACE|%s|%.2f|%.2f|%.2f|%.1f",
                    snap.type, snap.x, snap.y, snap.z, snap.yaw));
        }
    }

    public static void upgradePiece(MinecraftClient mc, int id, String targetTier) {
        if (mc == null || mc.player == null) return;
        if (mc.isInSingleplayer()) {
            for (BuildingPiece bp : SINGLEPLAYER_PIECES) {
                if (bp.id == id) {
                    bp.tier = targetTier;
                    bp.maxHp = "wood".equals(targetTier) ? 250 : ("stone".equals(targetTier) ? 500 : ("metal".equals(targetTier) ? 1000 : 2000));
                    bp.hp = bp.maxHp;
                    pieces = new ArrayList<>(SINGLEPLAYER_PIECES);
                    break;
                }
            }
        } else {
            RustWorldSync.sendRaw("BUILD_UPGRADE|" + id + "|" + targetTier);
        }
    }

    public static void rotatePiece(MinecraftClient mc, int id) {
        if (mc == null) return;
        if (mc.isInSingleplayer()) {
            for (BuildingPiece bp : SINGLEPLAYER_PIECES) {
                if (bp.id == id) {
                    bp.yaw = (bp.yaw + (bp.type.endsWith("_tri") ? 60f : 90f)) % 360f;
                    pieces = new ArrayList<>(SINGLEPLAYER_PIECES);
                    break;
                }
            }
        } else {
            RustWorldSync.sendRaw("BUILD_ROTATE|" + id);
        }
    }

    public static void demolishPiece(MinecraftClient mc, int id) {
        if (mc == null) return;
        if (mc.isInSingleplayer()) {
            SINGLEPLAYER_PIECES.removeIf(p -> p.id == id);
            pieces = new ArrayList<>(SINGLEPLAYER_PIECES);
        } else {
            RustWorldSync.sendRaw("BUILD_DEMOLISH|" + id);
        }
    }

    public static void repairPiece(MinecraftClient mc, int id) {
        if (mc == null || mc.player == null) return;
        long now = System.currentTimeMillis();
        if (now - lastActionMs < 240L) return;
        lastActionMs = now;
        net.spacerust.client.guns.RustViewModelAnimator.triggerMeleeSwing(mc, mc.player.getMainHandStack());
        RustWorldSync.sendRaw("BUILD_REPAIR|" + id);
    }

    public static boolean tryDeployOnBuilding(MinecraftClient mc) {
        if (mc == null || mc.player == null) return false;
        if (!isHoldingDeployable(mc)) return false;

        Vec3d eye = mc.player.getCameraPosVec(1.0f);
        Vec3d dir = mc.player.getRotationVec(1.0f).normalize();
        BuildHit bh = raycastBuildings(eye, dir, 5.2);
        if (bh == null) return false;

        // Only handle custom placement when looking at a 3D Rust building piece (foundation, floor, doorway)
        double distBlock = 999.0;
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            distBlock = mc.crosshairTarget.getPos().distanceTo(eye);
        }
        if (bh.t > distBlock + 0.05) return false;

        int bx = (int) Math.floor(bh.hitPos.x);
        int bz = (int) Math.floor(bh.hitPos.z);
        int by;
        if ("doorway".equals(bh.piece.type)) {
            bx = (int) Math.floor(bh.piece.x);
            bz = (int) Math.floor(bh.piece.z);
            by = (int) Math.ceil(bh.piece.y - 0.1);
        } else {
            by = (int) Math.ceil(bh.piece.y + 0.05);
        }
        RustWorldSync.sendRaw(String.format(Locale.US, "DEPLOY_PLACE|%d|%d|%d|%.1f",
                bx, by, bz, mc.player.yaw));
        return true;
    }

    public static SnapTarget computeSnapTarget(MinecraftClient mc) {
        if (mc == null || mc.player == null || mc.world == null) return null;
        String type = selectedType;
        Vec3d eye = mc.player.getCameraPosVec(1.0f);
        Vec3d dir = mc.player.getRotationVec(1.0f).normalize();

        double maxReach = 6.5;
        Vec3d aim = eye.add(dir.multiply(4.2));
        BuildHit bHit = raycastBuildings(eye, dir, maxReach);
        double bestT = maxReach;
        if (bHit != null) {
            bestT = bHit.t;
            aim = bHit.hitPos;
        }
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            double bt = mc.crosshairTarget.getPos().distanceTo(eye);
            if (bt < bestT) {
                bestT = bt;
                aim = mc.crosshairTarget.getPos();
            }
        }

        List<BuildingPiece> list = pieces;
        boolean affordable = canAffordTwig(mc.player, type);
        int cost = woodCostFor(type);

        // 1. Foundations snap horizontally to adjacent foundations or anchor to terrain grid
        if ("foundation".equals(type) || "foundation_tri".equals(type)) {
            double bestDistSq = 3.6 * 3.6;
            double sx = 0, sy = 0, sz = 0;
            float syaw = "foundation_tri".equals(type) ? (rotStep % 6) * 60f : (rotStep % 4) * 90f;
            boolean snapped = false;

            if (list != null) {
                boolean wantTri = "foundation_tri".equals(type);
                for (BuildingPiece p : list) {
                    if (!"foundation".equals(p.type) && !"foundation_tri".equals(p.type)) continue;
                    List<double[]> candidates = new ArrayList<>();
                    if ("foundation".equals(p.type)) {
                        addSquareEdgeSockets(candidates, p, p.y, wantTri, (rotStep % 4) * 90f);
                    } else {
                        addTriEdgeSockets(candidates, p, p.y, wantTri);
                    }
                    for (double[] c : candidates) {
                        double cx = c[0], cy = c[1], cz = c[2];
                        float cyaw = normalizeYaw((float) c[3]);
                        if (hasSamePieceAt(list, type, cx, cy, cz, cyaw)) continue;
                        double d2 = sqDist(aim, focusX(type, cx, cyaw), cy, focusZ(type, cz, cyaw));
                        if (d2 < bestDistSq) {
                            bestDistSq = d2;
                            sx = cx; sy = cy; sz = cz;
                            syaw = cyaw;
                            snapped = true;
                        }
                    }
                }
            }
            if (!snapped) {
                // Free terrain placement on 3m grid if pointing at block
                if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult bhr = (BlockHitResult) mc.crosshairTarget;
                    sx = Math.round(bhr.getPos().x / 3.0) * 3.0;
                    sz = Math.round(bhr.getPos().z / 3.0) * 3.0;
                    sy = bhr.getPos().y + 0.35;
                    syaw = "foundation_tri".equals(type) ? (rotStep % 6) * 60f : (rotStep % 4) * 90f;
                    snapped = !hasSamePieceAt(list, type, sx, sy, sz, syaw);
                }
            }
            return new SnapTarget(type, sx, sy, sz, syaw, snapped && affordable, cost);
        }

        // 2. Walls, Doorways, Windows, Halfwalls, Lowwalls snap to foundation/floor edges or on top of walls
        if ("wall".equals(type) || "doorway".equals(type) || "window".equals(type)
                || "halfwall".equals(type) || "lowwall".equals(type)) {
            double bestDistSq = 3.6 * 3.6;
            double sx = aim.x, sy = aim.y, sz = aim.z;
            float syaw = (rotStep % 4) * 90f;
            boolean snapped = false;
            float flip = ((rotStep % 2) == 1) ? 180f : 0f;

            if (list != null) {
                for (BuildingPiece p : list) {
                    if ("foundation".equals(p.type) || "floor".equals(p.type)) {
                        double wy = p.y + 0.08;
                        double[][] localEdges = {
                                {1.5, 0.0, 0.0},
                                {-1.5, 0.0, 180.0},
                                {0.0, 1.5, 90.0},
                                {0.0, -1.5, 270.0}
                        };
                        double rad = Math.toRadians(p.yaw);
                        double sin = Math.sin(rad), cos = Math.cos(rad);
                        for (double[] le : localEdges) {
                            double wx = p.x + le[0] * cos + le[1] * sin;
                            double wz = p.z - le[0] * sin + le[1] * cos;
                            if (hasWallAt(list, wx, wy, wz)) continue;
                            double d2 = sqDist(aim, wx, wy + 1.2, wz);
                            if (d2 < bestDistSq) {
                                bestDistSq = d2;
                                sx = wx; sy = wy; sz = wz;
                                syaw = normalizeYaw((float) (p.yaw + le[2] + flip));
                                snapped = true;
                            }
                        }
                    } else if ("wall".equals(p.type) || "doorway".equals(p.type) || "window".equals(p.type) || "halfwall".equals(p.type)) {
                        double topY = p.y + ("halfwall".equals(p.type) ? 1.5 : 3.0);
                        if (!hasWallAt(list, p.x, topY, p.z)) {
                            double d2 = sqDist(aim, p.x, topY + 1.2, p.z);
                            if (d2 < bestDistSq) {
                                bestDistSq = d2;
                                sx = p.x; sy = topY; sz = p.z;
                                syaw = normalizeYaw(p.yaw + flip);
                                snapped = true;
                            }
                        }
                    } else if ("foundation_tri".equals(p.type) || "floor_tri".equals(p.type)) {
                        double wy = p.y + 0.08;
                        double[][] localEdges = {
                                {0.0, 0.0, 90.0},
                                {-0.75, 1.299038, 30.0},
                                {0.75, 1.299038, 150.0}
                        };
                        double rad = Math.toRadians(p.yaw);
                        double sin = Math.sin(rad), cos = Math.cos(rad);
                        for (double[] le : localEdges) {
                            double wx = p.x + le[0] * cos + le[1] * sin;
                            double wz = p.z - le[0] * sin + le[1] * cos;
                            if (hasWallAt(list, wx, wy, wz)) continue;
                            double d2 = sqDist(aim, wx, wy + 1.2, wz);
                            if (d2 < bestDistSq) {
                                bestDistSq = d2;
                                sx = wx; sy = wy; sz = wz;
                                syaw = normalizeYaw((float) (p.yaw + le[2] + flip));
                                snapped = true;
                            }
                        }
                    }
                }
            }
            return new SnapTarget(type, sx, sy, sz, syaw, snapped && affordable, cost);
        }

        // 3. Floors / Roofs snap onto wall tops (+3.0 above foundation) or adjacent floor edges
        if ("floor".equals(type) || "floor_tri".equals(type) || "roof".equals(type)) {
            double bestDistSq = 4.2 * 4.2;
            double sx = aim.x, sy = aim.y, sz = aim.z;
            float syaw = (rotStep % 4) * 90f;
            boolean snapped = false;

            if (list != null) {
                boolean wantTri = "floor_tri".equals(type);
                for (BuildingPiece p : list) {
                    List<double[]> sockets = new ArrayList<>();
                    if ("wall".equals(p.type) || "doorway".equals(p.type) || "window".equals(p.type) || "halfwall".equals(p.type)) {
                        double ty = p.y + ("halfwall".equals(p.type) ? 1.5 : 3.0);
                        if (wantTri) {
                            sockets.add(new double[]{p.x, ty, p.z, normalizeYaw(p.yaw + 90f)});
                            sockets.add(new double[]{p.x, ty, p.z, normalizeYaw(p.yaw + 270f)});
                        } else {
                            double rad = Math.toRadians(p.yaw);
                            double nx = Math.cos(rad) * 1.5;
                            double nz = -Math.sin(rad) * 1.5;
                            float yawOpt = "roof".equals(type) ? (rotStep % 4) * 90f : normalizeYaw(p.yaw + (rotStep % 4) * 90f);
                            sockets.add(new double[]{p.x + nx, ty, p.z + nz, yawOpt});
                            sockets.add(new double[]{p.x - nx, ty, p.z - nz, yawOpt});
                        }
                    } else if ("floor".equals(p.type) && !"roof".equals(type)) {
                        addSquareEdgeSockets(sockets, p, p.y, wantTri, normalizeYaw(p.yaw + (rotStep % 4) * 90f));
                    } else if ("floor_tri".equals(p.type) && !"roof".equals(type)) {
                        addTriEdgeSockets(sockets, p, p.y, wantTri);
                    } else if ("foundation".equals(p.type) && "roof".equals(type)) {
                        sockets.add(new double[]{p.x, p.y + 3.08, p.z, (rotStep % 4) * 90f});
                    }
                    for (double[] s : sockets) {
                        float cyaw = normalizeYaw((float) s[3]);
                        if (hasSamePieceAt(list, type, s[0], s[1], s[2], cyaw)) continue;
                        double fx = focusX(type, s[0], cyaw);
                        double fz = focusZ(type, s[2], cyaw);
                        double d2 = sqDist(aim, fx, s[1], fz);
                        if (d2 < bestDistSq) {
                            bestDistSq = d2;
                            sx = s[0]; sy = s[1]; sz = s[2];
                            syaw = cyaw;
                            snapped = true;
                        }
                    }
                }
            }
            return new SnapTarget(type, sx, sy, sz, syaw, snapped && affordable, cost);
        }

        // 4. Stairs / Ramp snap inside any square foundation or floor cell
        if ("stairs".equals(type) || "ramp".equals(type)) {
            double bestDistSq = 3.5 * 3.5;
            double sx = aim.x, sy = aim.y, sz = aim.z;
            float syaw = (rotStep % 4) * 90f;
            boolean snapped = false;
            if (list != null) {
                for (BuildingPiece p : list) {
                    if (!"foundation".equals(p.type) && !"floor".equals(p.type)) continue;
                    double ty = p.y + 0.08;
                    if (hasSamePieceAt(list, type, p.x, ty, p.z, syaw)) continue;
                    double d2 = sqDist(aim, p.x, ty + 0.5, p.z);
                    if (d2 < bestDistSq) {
                        bestDistSq = d2;
                        sx = p.x; sy = ty; sz = p.z;
                        snapped = true;
                    }
                }
            }
            return new SnapTarget(type, sx, sy, sz, syaw, snapped && affordable, cost);
        }

        return null;
    }

    public static BuildHit raycastBuildings(Vec3d eye, Vec3d dir, double maxDist) {
        List<BuildingPiece> list = pieces;
        if (list == null || list.isEmpty()) return null;

        BuildHit best = null;
        double bestT = maxDist;
        for (BuildingPiece p : list) {
            double rad = Math.toRadians(p.yaw);
            double sin = Math.sin(rad), cos = Math.cos(rad);
            Vec3d relEye = eye.subtract(p.x, p.y, p.z);
            // World -> local (inverse rotateY by yaw)
            double lx = relEye.x * cos - relEye.z * sin;
            double ly = relEye.y;
            double lz = relEye.x * sin + relEye.z * cos;
            double ldx = dir.x * cos - dir.z * sin;
            double ldy = dir.y;
            double ldz = dir.x * sin + dir.z * cos;

            double t = Double.POSITIVE_INFINITY;
            if ("foundation".equals(p.type)) {
                t = intersectAABB(lx, ly, lz, ldx, ldy, ldz, -1.55, -2.5, -1.55, 1.55, 0.12, 1.55);
            } else if ("foundation_tri".equals(p.type)) {
                t = intersectAABB(lx, ly, lz, ldx, ldy, ldz, -1.5, -2.5, -0.05, 1.5, 0.12, 2.60);
            } else if ("floor".equals(p.type)) {
                t = intersectAABB(lx, ly, lz, ldx, ldy, ldz, -1.52, -0.12, -1.52, 1.52, 0.18, 1.52);
            } else if ("floor_tri".equals(p.type)) {
                t = intersectAABB(lx, ly, lz, ldx, ldy, ldz, -1.5, -0.12, -0.02, 1.5, 0.18, 2.60);
            } else if ("wall".equals(p.type)) {
                t = intersectAABB(lx, ly, lz, ldx, ldy, ldz, -0.14, 0.0, -1.52, 0.14, 3.0, 1.52);
            } else if ("halfwall".equals(p.type)) {
                t = intersectAABB(lx, ly, lz, ldx, ldy, ldz, -0.14, 0.0, -1.52, 0.14, 1.5, 1.52);
            } else if ("lowwall".equals(p.type)) {
                t = intersectAABB(lx, ly, lz, ldx, ldy, ldz, -0.14, 0.0, -1.52, 0.14, 1.0, 1.52);
            } else if ("doorway".equals(p.type)) {
                double t1 = intersectAABB(lx, ly, lz, ldx, ldy, ldz, -0.14, 0.0, -1.52, 0.14, 3.0, -0.58);
                double t2 = intersectAABB(lx, ly, lz, ldx, ldy, ldz, -0.14, 0.0, 0.58, 0.14, 3.0, 1.52);
                double t3 = intersectAABB(lx, ly, lz, ldx, ldy, ldz, -0.14, 2.15, -0.58, 0.14, 3.0, 0.58);
                t = Math.min(t1, Math.min(t2, t3));
            } else if ("window".equals(p.type)) {
                double t1 = intersectAABB(lx, ly, lz, ldx, ldy, ldz, -0.14, 0.0, -1.52, 0.14, 1.05, 1.52);
                double t2 = intersectAABB(lx, ly, lz, ldx, ldy, ldz, -0.14, 2.15, -1.52, 0.14, 3.0, 1.52);
                double t3 = intersectAABB(lx, ly, lz, ldx, ldy, ldz, -0.14, 1.05, -1.52, 0.14, 2.15, -0.75);
                double t4 = intersectAABB(lx, ly, lz, ldx, ldy, ldz, -0.14, 1.05, 0.75, 0.14, 2.15, 1.52);
                t = Math.min(Math.min(t1, t2), Math.min(t3, t4));
            } else if ("roof".equals(p.type) || "stairs".equals(p.type) || "ramp".equals(p.type)) {
                t = intersectAABB(lx, ly, lz, ldx, ldy, ldz, -1.52, 0.0, -1.52, 1.52, 3.0, 1.52);
            }

            if (t > 0.05 && t < bestT) {
                bestT = t;
                best = new BuildHit(p, t, eye.add(dir.multiply(t)));
            }
        }
        return best;
    }

    private static double intersectAABB(double ox, double oy, double oz,
                                        double dx, double dy, double dz,
                                        double minX, double minY, double minZ,
                                        double maxX, double maxY, double maxZ) {
        double tMin = 0.0;
        double tMax = 1e9;
        double[] o = {ox, oy, oz};
        double[] d = {dx, dy, dz};
        double[] bMin = {minX, minY, minZ};
        double[] bMax = {maxX, maxY, maxZ};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-7) {
                if (o[i] < bMin[i] || o[i] > bMax[i]) return Double.POSITIVE_INFINITY;
            } else {
                double inv = 1.0 / d[i];
                double t1 = (bMin[i] - o[i]) * inv;
                double t2 = (bMax[i] - o[i]) * inv;
                if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMin > tMax) return Double.POSITIVE_INFINITY;
            }
        }
        return tMin > 0 ? tMin : Double.POSITIVE_INFINITY;
    }

    private static void addSquareEdgeSockets(List<double[]> out, BuildingPiece p, double targetY,
                                             boolean wantTri, float squareYaw) {
        double rad = Math.toRadians(p.yaw);
        double sin = Math.sin(rad), cos = Math.cos(rad);
        double dist = wantTri ? 1.5 : 3.0;
        double[][] dirs = {{0.0, 1.0, 0.0}, {1.0, 0.0, 90.0}, {0.0, -1.0, 180.0}, {-1.0, 0.0, 270.0}};
        for (double[] d : dirs) {
            double lx = d[0] * dist;
            double lz = d[1] * dist;
            double wx = p.x + lx * cos + lz * sin;
            double wz = p.z - lx * sin + lz * cos;
            float yaw = wantTri ? normalizeYaw((float) (p.yaw + d[2])) : squareYaw;
            out.add(new double[]{wx, targetY, wz, yaw});
        }
    }

    private static void addTriEdgeSockets(List<double[]> out, BuildingPiece p, double targetY, boolean wantTri) {
        double rad = Math.toRadians(p.yaw);
        double sin = Math.sin(rad), cos = Math.cos(rad);
        double[][] local = wantTri
                ? new double[][]{{0.0, 0.0, 180.0}, {-0.75, 1.299038, 300.0}, {0.75, 1.299038, 60.0}}
                : new double[][]{{0.0, -1.5, 0.0}, {-2.049038, 2.049038, 120.0}, {2.049038, 2.049038, 240.0}};
        for (double[] le : local) {
            double wx = p.x + le[0] * cos + le[1] * sin;
            double wz = p.z - le[0] * sin + le[1] * cos;
            out.add(new double[]{wx, targetY, wz, normalizeYaw((float) (p.yaw + le[2]))});
        }
    }

    private static boolean isTriPiece(String type) {
        return "foundation_tri".equals(type) || "floor_tri".equals(type);
    }

    private static double focusX(String type, double x, float yaw) {
        return isTriPiece(type) ? x + 0.8660254 * Math.sin(Math.toRadians(yaw)) : x;
    }

    private static double focusZ(String type, double z, float yaw) {
        return isTriPiece(type) ? z + 0.8660254 * Math.cos(Math.toRadians(yaw)) : z;
    }

    private static float normalizeYaw(float yaw) {
        float y = yaw % 360f;
        return y < 0 ? y + 360f : y;
    }

    private static boolean hasSamePieceAt(List<BuildingPiece> list, String type, double x, double y, double z, float yaw) {
        if (list == null) return false;
        double fx = focusX(type, x, yaw);
        double fz = focusZ(type, z, yaw);
        for (BuildingPiece p : list) {
            if (p.type.equals(type) && sqDist(new Vec3d(fx, y, fz),
                    focusX(p.type, p.x, p.yaw), p.y, focusZ(p.type, p.z, p.yaw)) < 0.45 * 0.45) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasWallAt(List<BuildingPiece> list, double x, double y, double z) {
        if (list == null) return false;
        for (BuildingPiece p : list) {
            if (("wall".equals(p.type) || "doorway".equals(p.type) || "window".equals(p.type)
                    || "halfwall".equals(p.type) || "lowwall".equals(p.type))
                    && sqDist(new Vec3d(x, y, z), p.x, p.y, p.z) < 0.35 * 0.35) {
                return true;
            }
        }
        return false;
    }

    private static double sqDist(Vec3d v, double x, double y, double z) {
        double dx = v.x - x, dy = v.y - y, dz = v.z - z;
        return dx * dx + dy * dy + dz * dz;
    }
}
