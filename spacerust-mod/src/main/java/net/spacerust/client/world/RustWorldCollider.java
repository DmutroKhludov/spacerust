package net.spacerust.client.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.spacerust.client.guns.RustViewModelAnimator;

import java.util.List;
import java.util.Locale;

/**
 * Physical 3D collider and crosshair raycaster for 3D Rust Ore Nodes (stages 1..4)
 * and 3D Rust Trees. Prevents walking through nodes/trunks and maps left-click swings
 * anywhere on the boulder/trunk directly to the glowing star / painted red X minigame.
 */
public final class RustWorldCollider {

    private static final double PLAYER_HALF_W = 0.30;
    private static long lastFarmSwingMs = 0L;
    private static long lastLocalOreSfxMs = 0L;
    private static long lastLocalTreeSfxMs = 0L;
    private static int localOreStreak = 0;
    private static int localTreeStreak = 0;

    private RustWorldCollider() {}

    public static boolean wasLocalSfxRecent(String kind) {
        long now = System.currentTimeMillis();
        if ("ore".equals(kind)) return now - lastLocalOreSfxMs < 320L;
        if ("tree".equals(kind)) return now - lastLocalTreeSfxMs < 320L;
        return false;
    }

    public static float getOreRadius(int stage) {
        if (stage == 1) return 1.24f;
        if (stage == 2) return 1.10f;
        if (stage == 3) return 0.96f;
        return 0.80f;
    }

    public static float getOreHeight(int stage) {
        if (stage == 1) return 1.48f;
        if (stage == 2) return 1.12f;
        if (stage == 3) return 0.92f;
        return 0.55f;
    }

    /**
     * Smoothly resolves horizontal + dome-top physical collision for the local player
     * after each movement step so boulders and tree trunks feel 100% solid.
     */
    public static void resolvePlayerCollision(Entity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || entity != mc.player || entity.isSpectator()) {
            return;
        }

        double px = entity.getX();
        double py = entity.getY();
        double pz = entity.getZ();
        Vec3d vel = entity.getVelocity();
        double vx = vel.x, vy = vel.y, vz = vel.z;
        boolean modifiedPos = false;
        boolean modifiedVel = false;

        // 1. Solid 3D Ore Boulders (stages 1..4)
        List<RustWorldSync.OreNode> nodes = RustWorldSync.oreNodes;
        if (nodes != null && !nodes.isEmpty()) {
            for (RustWorldSync.OreNode node : nodes) {
                double dx = px - node.x;
                double dz = pz - node.z;
                double distSq = dx * dx + dz * dz;
                if (distSq > 9.0) continue;

                float rBase = getOreRadius(node.stage);
                float hTop = getOreHeight(node.stage);
                if (py + entity.getHeight() < node.y - 0.1 || py > node.y + hTop + 0.15) continue;

                double dist = Math.sqrt(distSq);

                // Allow jumping/walking cleanly onto top of the rock dome
                if (py >= node.y + hTop - 0.42 && dist < rBase * 0.82 && vy <= 0.08) {
                    py = Math.max(py, node.y + hTop - 0.03);
                    vy = 0.0;
                    entity.setOnGround(true);
                    entity.fallDistance = 0.0f;
                    modifiedPos = true;
                    modifiedVel = true;
                    continue;
                }

                double relH = Math.max(0.0, Math.min(1.0, (py - node.y) / hTop));
                double domeTaper = Math.sqrt(Math.max(0.28, 1.0 - relH * relH * 0.55));
                double effR = rBase * domeTaper + PLAYER_HALF_W;

                if (dist < effR && py < node.y + hTop - 0.10) {
                    double nx = (dist > 1e-4) ? (dx / dist) : 1.0;
                    double nz = (dist > 1e-4) ? (dz / dist) : 0.0;
                    px = node.x + nx * effR;
                    pz = node.z + nz * effR;
                    modifiedPos = true;

                    double inward = vx * nx + vz * nz;
                    if (inward < 0.0) {
                        vx -= inward * nx;
                        vz -= inward * nz;
                        modifiedVel = true;
                    }
                }
            }
        }

        // 2. Solid 3D Rust Tree Trunks
        List<RustWorldSync.RustTree> trees = RustWorldSync.trees;
        if (trees != null && !trees.isEmpty()) {
            for (RustWorldSync.RustTree tree : trees) {
                if ("vanilla".equals(tree.kind)) continue; // Vanilla blocks already have solid collision
                if (py + entity.getHeight() < tree.y - 0.15 || py > tree.y + tree.height) continue;
                double dx = px - tree.x;
                double dz = pz - tree.z;
                double distSq = dx * dx + dz * dz;
                double effR = tree.radius + PLAYER_HALF_W;
                if (distSq < effR * effR) {
                    double dist = Math.sqrt(distSq);
                    double nx = (dist > 1e-4) ? (dx / dist) : 1.0;
                    double nz = (dist > 1e-4) ? (dz / dist) : 0.0;
                    px = tree.x + nx * effR;
                    pz = tree.z + nz * effR;
                    modifiedPos = true;

                    double inward = vx * nx + vz * nz;
                    if (inward < 0.0) {
                        vx -= inward * nx;
                        vz -= inward * nz;
                        modifiedVel = true;
                    }
                }
            }
        }

        // 3. Solid 3D Rust Player Buildings (Foundations, Floors, Walls, Doorways, Windows, Stairs)
        List<RustBuildingSystem.BuildingPiece> buildings = RustBuildingSystem.pieces;
        if (buildings != null && !buildings.isEmpty()) {
            double pHeight = entity.getHeight();
            for (RustBuildingSystem.BuildingPiece bp : buildings) {
                double dx = px - bp.x;
                double dz = pz - bp.z;
                if (dx * dx + dz * dz > 28.0) continue;

                double rad = Math.toRadians(bp.yaw);
                double sin = Math.sin(rad), cos = Math.cos(rad);
                // World -> local
                double lx = dx * cos - dz * sin;
                double lz = dx * sin + dz * cos;
                double ly = py - bp.y;

                double[] st = {px, py, pz, vx, vy, vz, 0.0};
                if ("foundation".equals(bp.type)) {
                    collideFoundation(entity, lx, ly, lz, cos, sin, bp.y, -1.48, -2.5, -1.48, 1.48, 0.06, 1.48, pHeight, st);
                } else if ("foundation_tri".equals(bp.type)) {
                    if (insideTriangleLocal(lx, lz, 0.28)) {
                        collideFoundation(entity, lx, ly, lz, cos, sin, bp.y, -1.48, -2.5, 0.02, 1.48, 0.06, 2.58, pHeight, st);
                    }
                } else if ("floor".equals(bp.type)) {
                    collideFlatSlab(entity, lx, ly, lz, cos, sin, bp.y, -1.50, -0.06, -1.50, 1.50, 0.11, 1.50, false, pHeight, st);
                } else if ("floor_tri".equals(bp.type)) {
                    if (insideTriangleLocal(lx, lz, 0.25)) {
                        collideFlatSlab(entity, lx, ly, lz, cos, sin, bp.y, -1.50, -0.06, -0.02, 1.50, 0.11, 2.60, false, pHeight, st);
                    }
                } else if ("wall".equals(bp.type)) {
                    collideLocalSolidBox(lx, ly, lz, cos, sin, -0.14, 0.0, -1.52, 0.14, 3.02, 1.52, pHeight, st);
                } else if ("halfwall".equals(bp.type)) {
                    collideLocalSolidBox(lx, ly, lz, cos, sin, -0.14, 0.0, -1.52, 0.14, 1.52, 1.52, pHeight, st);
                } else if ("lowwall".equals(bp.type)) {
                    collideLocalSolidBox(lx, ly, lz, cos, sin, -0.14, 0.0, -1.52, 0.14, 1.02, 1.52, pHeight, st);
                } else if ("doorway".equals(bp.type)) {
                    // Wide clean doorway opening (-0.70..+0.70 -> 0.80m clear passage); never snag center passage
                    if (Math.abs(lz) >= 0.68) {
                        if (lz < 0) collideLocalSolidBox(lx, ly, lz, cos, sin, -0.13, 0.0, -1.50, 0.13, 3.02, -0.70, pHeight, st);
                        else collideLocalSolidBox(lx, ly, lz, cos, sin, -0.13, 0.0, 0.70, 0.13, 3.02, 1.50, pHeight, st);
                    }
                    collideLocalSolidBox(lx, ly, lz, cos, sin, -0.13, 2.20, -0.70, 0.13, 3.02, 0.70, pHeight, st);
                } else if ("window".equals(bp.type)) {
                    collideLocalSolidBox(lx, ly, lz, cos, sin, -0.14, 0.0, -1.52, 0.14, 1.05, 1.52, pHeight, st);
                    collideLocalSolidBox(lx, ly, lz, cos, sin, -0.14, 2.15, -1.52, 0.14, 3.02, 1.52, pHeight, st);
                    collideLocalSolidBox(lx, ly, lz, cos, sin, -0.14, 1.05, -1.52, 0.14, 2.15, -0.75, pHeight, st);
                    collideLocalSolidBox(lx, ly, lz, cos, sin, -0.14, 1.05, 0.75, 0.14, 2.15, 1.52, pHeight, st);
                } else if ("stairs".equals(bp.type) || "roof".equals(bp.type)) {
                    if (Math.abs(lx) <= 1.52 && Math.abs(lz) <= 1.52) {
                        double t = Math.max(0.0, Math.min(1.0, (1.50 - lz) / 3.0));
                        double rampY = bp.y + t * 3.0;
                        if (py >= rampY - 0.85 && py <= rampY + 0.55 && vy <= 0.12) {
                            st[1] = Math.max(st[1], rampY);
                            st[4] = Math.max(0.0, st[4]);
                            entity.setOnGround(true);
                            entity.fallDistance = 0.0f;
                            st[6] = 1.0;
                        }
                    }
                }

                if (st[6] > 0.5) {
                    px = st[0]; py = st[1]; pz = st[2];
                    vx = st[3]; vy = st[4]; vz = st[5];
                    modifiedPos = true;
                    modifiedVel = true;
                }
            }
        }

        if (modifiedPos) {
            entity.setPosition(px, py, pz);
        }
        if (modifiedVel) {
            entity.setVelocity(vx, vy, vz);
        }
    }

    private static boolean insideTriangleLocal(double lx, double lz, double pad) {
        if (lz < -pad || lz > 2.60 + pad) return false;
        double halfW = Math.max(0.25, 1.50 * (1.0 - lz / 2.60)) + pad;
        return Math.abs(lx) <= halfW;
    }

    private static void collideFoundation(Entity entity, double lx, double ly, double lz,
                                          double cos, double sin, double worldY,
                                          double minX, double minY, double minZ,
                                          double maxX, double maxY, double maxZ,
                                          double pHeight, double[] st) {
        if (lx >= minX - 0.14 && lx <= maxX + 0.14 && lz >= minZ - 0.14 && lz <= maxZ + 0.14) {
            double topY = worldY + maxY;
            if (st[1] >= topY - 0.85 && st[1] <= topY + 0.18 && st[4] <= 0.10) {
                st[1] = topY;
                st[4] = 0.0;
                entity.setOnGround(true);
                entity.fallDistance = 0.0f;
                st[6] = 1.0;
                return;
            }
        }
        // Only push horizontally off foundation outer skirts when feet are actually BELOW deck level
        if (ly < -0.16) {
            collideLocalSolidBox(lx, ly, lz, cos, sin, minX, minY, minZ, maxX, maxY - 0.08, maxZ, pHeight, st);
        }
    }

    private static void collideFlatSlab(Entity entity, double lx, double ly, double lz,
                                        double cos, double sin, double worldY,
                                        double minX, double minY, double minZ,
                                        double maxX, double maxY, double maxZ,
                                        boolean stepUp, double pHeight, double[] st) {
        if (lx >= minX - 0.15 && lx <= maxX + 0.15 && lz >= minZ - 0.15 && lz <= maxZ + 0.15) {
            double topY = worldY + maxY;
            if (st[1] >= topY - (stepUp ? 0.72 : 0.38) && st[1] <= topY + 0.16 && st[4] <= 0.08) {
                st[1] = topY;
                st[4] = 0.0;
                entity.setOnGround(true);
                entity.fallDistance = 0.0f;
                st[6] = 1.0;
                return;
            }
        }
        collideLocalSolidBox(lx, ly, lz, cos, sin, minX, minY, minZ, maxX, maxY, maxZ, pHeight, st);
    }

    private static void collideLocalSolidBox(double lx, double ly, double lz,
                                             double cos, double sin,
                                             double minX, double minY, double minZ,
                                             double maxX, double maxY, double maxZ,
                                             double pHeight, double[] st) {
        if (ly + pHeight <= minY + 0.04 || ly >= maxY - 0.04) return;
        double eMinX = minX - PLAYER_HALF_W, eMaxX = maxX + PLAYER_HALF_W;
        double eMinZ = minZ - PLAYER_HALF_W, eMaxZ = maxZ + PLAYER_HALF_W;
        if (lx <= eMinX || lx >= eMaxX || lz <= eMinZ || lz >= eMaxZ) return;

        double penLeft = lx - eMinX;
        double penRight = eMaxX - lx;
        double penBack = lz - eMinZ;
        double penFront = eMaxZ - lz;
        double minPen = Math.min(Math.min(penLeft, penRight), Math.min(penBack, penFront));

        double plx = 0.0, plz = 0.0;
        if (minPen == penLeft) plx = -penLeft;
        else if (minPen == penRight) plx = penRight;
        else if (minPen == penBack) plz = -penBack;
        else plz = penFront;

        // Local push -> world push (inverse transpose rotateY)
        double pwx = plx * cos + plz * sin;
        double pwz = -plx * sin + plz * cos;
        st[0] += pwx;
        st[2] += pwz;

        double len = Math.hypot(pwx, pwz);
        if (len > 1e-5) {
            double nx = pwx / len, nz = pwz / len;
            double inward = st[3] * nx + st[5] * nz;
            if (inward < 0.0) {
                st[3] -= inward * nx;
                st[5] -= inward * nz;
            }
        }
        st[6] = 1.0;
    }

    /**
     * Raycasts from camera eye onto nearby 3D Ore Nodes & Rust Trees on left-click.
     * Returns true if a Rust farm node/tree was hit (canceling vanilla block-breaking).
     */
    public static boolean tryRaycastFarmHit(MinecraftClient mc, boolean fromHold) {
        if (mc == null || mc.player == null || mc.world == null) return false;
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            return false;
        }

        long now = System.currentTimeMillis();
        long minCooldown = fromHold ? 400L : 260L;
        if (now - lastFarmSwingMs < minCooldown) {
            return !fromHold && isLookingAtNodeOrTree(mc);
        }

        Vec3d eye = mc.player.getCameraPosVec(1.0f);
        Vec3d dir = mc.player.getRotationVec(1.0f).normalize();
        double maxDist = 4.25;
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            maxDist = Math.min(maxDist, mc.crosshairTarget.getPos().distanceTo(eye) + 0.25);
        }

        String bestKind = null;
        int bestId = -1;
        double bestT = maxDist;
        Vec3d bestHit = null;
        boolean hitSpotBonus = false;

        // 1. Ray vs 3D Ore Node Ellipsoids + protruding Glowing Star
        List<RustWorldSync.OreNode> nodes = RustWorldSync.oreNodes;
        if (nodes != null) {
            for (RustWorldSync.OreNode node : nodes) {
                float r = getOreRadius(node.stage);
                float h = getOreHeight(node.stage);
                double cy = node.y + h * 0.42;
                double ry = Math.max(0.45, h * 0.62);

                double t = intersectEllipsoid(eye, dir, node.x, cy, node.z, r + 0.08, ry, r + 0.08);
                if (t > 0.0 && t < bestT) {
                    bestT = t;
                    bestKind = "ore";
                    bestId = node.id;
                    bestHit = eye.add(dir.multiply(t));
                    hitSpotBonus = false;
                }

                // Test sphere around glowing hotspot star so outer crust never blocks star aim
                double tStar = intersectEllipsoid(eye, dir,
                        node.x + node.hx, node.y + node.hy, node.z + node.hz,
                        0.42, 0.42, 0.42);
                if (tStar > 0.0 && tStar <= maxDist + 0.35 && (bestKind == null || ("ore".equals(bestKind) && tStar <= bestT + 0.85))) {
                    bestT = Math.min(bestT, tStar);
                    bestKind = "ore";
                    bestId = node.id;
                    bestHit = new Vec3d(node.x + node.hx, node.y + node.hy, node.z + node.hz);
                    hitSpotBonus = true;
                }
            }
        }

        // 2. Ray vs 3D Rust Tree Trunks + Red X marker
        List<RustWorldSync.RustTree> trees = RustWorldSync.trees;
        if (trees != null) {
            for (RustWorldSync.RustTree tree : trees) {
                double t = intersectVerticalCylinder(eye, dir, tree.x, tree.z,
                        tree.radius + 0.10, tree.y - 0.1, tree.y + tree.height);
                if (t > 0.0 && t < bestT) {
                    bestT = t;
                    bestKind = "tree";
                    bestId = tree.id;
                    bestHit = eye.add(dir.multiply(t));
                    hitSpotBonus = false;
                }

                // Sphere on painted Red X marker on trunk
                float xSurfR = "vanilla".equals(tree.kind) ? 0.54f : (tree.radius + 0.03f);
                double xPosX = tree.x + Math.cos(tree.xAngle) * xSurfR;
                double xPosY = tree.y + tree.xY;
                double xPosZ = tree.z + Math.sin(tree.xAngle) * xSurfR;
                double tX = intersectEllipsoid(eye, dir, xPosX, xPosY, xPosZ, 0.40, 0.40, 0.40);
                if (tX > 0.0 && tX <= maxDist + 0.35 && (bestKind == null || ("tree".equals(bestKind) && tX <= bestT + 0.65))) {
                    bestT = Math.min(bestT, tX);
                    bestKind = "tree";
                    bestId = tree.id;
                    bestHit = new Vec3d(xPosX, xPosY, xPosZ);
                    hitSpotBonus = true;
                }
            }
        }

        if (bestKind != null && bestHit != null) {
            lastFarmSwingMs = now;
            RustViewModelAnimator.triggerMeleeSwing(mc, mc.player.getMainHandStack());
            if ("ore".equals(bestKind)) {
                lastLocalOreSfxMs = now;
                RustViewModelAnimator.playSound(mc, "ore_hit", 0.65f);
                if (hitSpotBonus) {
                    localOreStreak = Math.min(6, localOreStreak + 1);
                    String hsKey = (localOreStreak % 2 == 1) ? "ore_hotspot_1" : "ore_hotspot_2";
                    RustViewModelAnimator.playSound(mc, hsKey, 1.0f, 0.92f + localOreStreak * 0.085f);
                    OreNodeRenderer.triggerHotspotSpark(bestHit.x, bestHit.y, bestHit.z);
                } else {
                    localOreStreak = 0;
                }
            } else {
                lastLocalTreeSfxMs = now;
                RustViewModelAnimator.playSound(mc, Math.random() < 0.5 ? "tree_chop_1" : "tree_chop_2", 0.65f);
                RustTreeRenderer.triggerTreeShake(bestHit.x, bestHit.z);
                if (hitSpotBonus) {
                    localTreeStreak = Math.min(6, localTreeStreak + 1);
                    String tbKey = (localTreeStreak % 2 == 1) ? "tree_bonus_1" : "tree_bonus_2";
                    RustViewModelAnimator.playSound(mc, tbKey, 0.95f, 0.92f + localTreeStreak * 0.085f);
                    RustViewModelAnimator.playSound(mc, "tree_spray", 0.35f, 1.0f);
                } else {
                    localTreeStreak = 0;
                }
            }
            RustWorldSync.sendRaw(String.format(Locale.US, "FARM_HIT|%s|%d|%.3f|%.3f|%.3f",
                    bestKind, bestId, bestHit.x, bestHit.y, bestHit.z));
            return true;
        }
        return false;
    }

    private static boolean isLookingAtNodeOrTree(MinecraftClient mc) {
        Vec3d eye = mc.player.getCameraPosVec(1.0f);
        Vec3d dir = mc.player.getRotationVec(1.0f).normalize();
        double maxDist = 4.25;
        List<RustWorldSync.OreNode> nodes = RustWorldSync.oreNodes;
        if (nodes != null) {
            for (RustWorldSync.OreNode n : nodes) {
                float r = getOreRadius(n.stage), h = getOreHeight(n.stage);
                double t = intersectEllipsoid(eye, dir, n.x, n.y + h * 0.42, n.z, r + 0.08, Math.max(0.45, h * 0.62), r + 0.08);
                if (t > 0.0 && t <= maxDist) return true;
            }
        }
        List<RustWorldSync.RustTree> trees = RustWorldSync.trees;
        if (trees != null) {
            for (RustWorldSync.RustTree tr : trees) {
                double t = intersectVerticalCylinder(eye, dir, tr.x, tr.z, tr.radius + 0.10, tr.y - 0.1, tr.y + tr.height);
                if (t > 0.0 && t <= maxDist) return true;
            }
        }
        return false;
    }

    private static double intersectEllipsoid(Vec3d eye, Vec3d dir,
                                             double cx, double cy, double cz,
                                             double rx, double ry, double rz) {
        double ox = (eye.x - cx) / rx;
        double oy = (eye.y - cy) / ry;
        double oz = (eye.z - cz) / rz;
        double dx = dir.x / rx;
        double dy = dir.y / ry;
        double dz = dir.z / rz;

        double a = dx * dx + dy * dy + dz * dz;
        double b = 2.0 * (ox * dx + oy * dy + oz * dz);
        double c = ox * ox + oy * oy + oz * oz - 1.0;
        if (c <= 0.0) return 0.15; // eye slightly inside outer margin
        double disc = b * b - 4.0 * a * c;
        if (disc < 0.0 || a < 1e-8) return -1.0;
        double sq = Math.sqrt(disc);
        double t1 = (-b - sq) / (2.0 * a);
        if (t1 > 0.0) return t1;
        double t2 = (-b + sq) / (2.0 * a);
        return (t2 > 0.0) ? t2 : -1.0;
    }

    private static double intersectVerticalCylinder(Vec3d eye, Vec3d dir,
                                                    double cx, double cz, double r,
                                                    double minY, double maxY) {
        double ox = eye.x - cx;
        double oz = eye.z - cz;
        double a = dir.x * dir.x + dir.z * dir.z;
        double b = 2.0 * (ox * dir.x + oz * dir.z);
        double c = ox * ox + oz * oz - r * r;
        if (c <= 0.0) {
            return (eye.y >= minY && eye.y <= maxY) ? 0.15 : -1.0;
        }
        if (a < 1e-8) return -1.0;
        double disc = b * b - 4.0 * a * c;
        if (disc < 0.0) return -1.0;
        double sq = Math.sqrt(disc);
        double t = (-b - sq) / (2.0 * a);
        if (t < 0.0) t = (-b + sq) / (2.0 * a);
        if (t <= 0.0) return -1.0;
        double hitY = eye.y + dir.y * t;
        if (hitY < minY || hitY > maxY) return -1.0;
        return t;
    }
}
