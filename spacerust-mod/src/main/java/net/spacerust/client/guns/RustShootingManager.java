package net.spacerust.client.guns;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.spacerust.client.building.RustBuildingManager;
import net.spacerust.client.hud.RustCrosshairOverlay;
import net.spacerust.client.sound.RustSoundManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RustShootingManager {

    private static final Map<RustWeapon, Integer> AMMO_MAP = new HashMap<>();
    private static long lastShootTime = 0;
    private static boolean isLeftMouseDown = false;
    public static boolean isAiming = false;
    public static float adsProgress = 0f; // 0.0 (hip) to 1.0 (ADS)

    public static RustWeapon getHeldWeapon(MinecraftClient client) {
        if (client.player == null) return null;
        ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty()) return null;

        String name = held.getName().getString().toLowerCase();
        for (RustWeapon w : RustWeapon.values()) {
            if (name.contains(w.profileName) || name.contains(w.name().toLowerCase()) || name.contains(w.title.toLowerCase())) {
                return w;
            }
        }
        // Fallbacks for test items
        if (held.getItem() == Items.CROSSBOW || name.contains("калаш") || name.contains("ак")) {
            return RustWeapon.AK47;
        } else if (name.contains("томпсон") || name.contains("thompson")) {
            return RustWeapon.THOMPSON;
        } else if (name.contains("болт") || name.contains("bolt")) {
            return RustWeapon.BOLT;
        } else if (held.getItem() == Items.BOW || name.contains("револьвер")) {
            return RustWeapon.REVOLVER;
        }
        return null;
    }

    public static int getAmmo(RustWeapon weapon) {
        return AMMO_MAP.getOrDefault(weapon, weapon.maxAmmo);
    }

    public static void setAmmo(RustWeapon weapon, int ammo) {
        AMMO_MAP.put(weapon, Math.max(0, Math.min(weapon.maxAmmo, ammo)));
    }

    public static void onAttackPressed(MinecraftClient client) {
        RustWeapon weapon = getHeldWeapon(client);
        if (weapon != null) {
            isLeftMouseDown = true;
            tryFire(client, weapon);
        }
    }

    public static void onAttackReleased() {
        isLeftMouseDown = false;
    }

    public static void tick(MinecraftClient client) {
        RustRecoilManager.tick(client);

        RustWeapon weapon = getHeldWeapon(client);
        if (weapon == null) {
            isAiming = false;
            adsProgress = Math.max(0f, adsProgress - 0.2f);
            return;
        }

        // Aim Down Sights (Right Mouse Button)
        boolean rmb = client.options.keyUse.isPressed();
        isAiming = rmb;
        if (isAiming) {
            adsProgress = Math.min(1.0f, adsProgress + 0.18f);
        } else {
            adsProgress = Math.max(0f, adsProgress - 0.18f);
        }

        // Automatic weapon continuous fire
        if (isLeftMouseDown && weapon.isAutomatic) {
            tryFire(client, weapon);
        }
    }

    public static boolean tryFire(MinecraftClient client, RustWeapon weapon) {
        if (client.player == null || client.world == null) return false;

        long now = System.currentTimeMillis();
        if (now - lastShootTime < weapon.fireDelayMs) {
            return false;
        }

        int ammo = getAmmo(weapon);
        if (ammo <= 0) {
            // Empty click sound
            RustSoundManager.play("keylock_lock", 0.6f, 1.8f);
            lastShootTime = now;
            return false;
        }

        lastShootTime = now;
        setAmmo(weapon, ammo - 1);

        // 1. Play authentic gunshot sound
        RustSoundManager.play(weapon.shootSound, 1.0f, 0.95f + (float) Math.random() * 0.1f);

        // 2. Apply Rust Recoil Kick
        RustRecoilManager.onShoot(client, weapon, isAiming);

        // 3. Bullet Raycasting & Hit Registration
        int pellets = weapon.isShotgun() ? 10 : 1;
        float damagePerPellet = weapon.baseDamage / (weapon.isShotgun() ? 1 : 1);

        for (int p = 0; p < pellets; p++) {
            fireRay(client, weapon, damagePerPellet, isAiming);
        }

        return true;
    }

    private static void fireRay(MinecraftClient client, RustWeapon weapon, float damage, boolean isAiming) {
        PlayerEntity player = client.player;
        if (player == null || client.world == null) return;

        Vec3d camPos = player.getCameraPosVec(1.0f);
        Vec3d lookVec = player.getRotationVec(1.0f);

        // Spread calculation
        float spread = isAiming ? 0.008f : 0.025f;
        if (weapon.isShotgun()) spread = isAiming ? 0.045f : 0.075f;

        Vec3d spreadVec = lookVec.add(
                (Math.random() - 0.5) * spread,
                (Math.random() - 0.5) * spread,
                (Math.random() - 0.5) * spread
        ).normalize();

        double reach = 200.0;
        Vec3d endVec = camPos.add(spreadVec.multiply(reach));

        // A) Raycast against living entities / other players
        Entity hitEntity = null;
        Vec3d hitPos = null;
        double closestDist = reach * reach;

        Box searchBox = player.getBoundingBox().stretch(spreadVec.multiply(reach)).expand(1.0);
        for (Entity e : client.world.getOtherEntities(player, searchBox, target -> target.isAlive() && !target.isSpectator())) {
            Box box = e.getBoundingBox().expand(0.3);
            Optional<Vec3d> clip = box.raycast(camPos, endVec);
            if (clip.isPresent()) {
                double dist = camPos.squaredDistanceTo(clip.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    hitEntity = e;
                    hitPos = clip.get();
                }
            }
        }

        // B) Check block hit distance
        HitResult blockHit = client.world.raycast(new net.minecraft.world.RaycastContext(
                camPos, endVec,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                player
        ));

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            double bDist = camPos.squaredDistanceTo(blockHit.getPos());
            if (bDist < closestDist) {
                closestDist = bDist;
                hitEntity = null;
                hitPos = blockHit.getPos();
            }
        }

        // C) Check building piece hit
        RustBuildingManager.BuildingPieceData hitPiece = null;
        for (RustBuildingManager.BuildingPieceData b : RustBuildingManager.pieces) {
            Box box = b.getBoundingBox();
            Optional<Vec3d> clip = box.raycast(camPos, endVec);
            if (clip.isPresent()) {
                double dist = camPos.squaredDistanceTo(clip.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    hitEntity = null;
                    hitPiece = b;
                    hitPos = clip.get();
                }
            }
        }

        // D) Check deployables hit (Doors, Tool Cupboard, Boxes)
        net.spacerust.client.deployables.RustDeployableData hitDeployable = null;
        for (net.spacerust.client.deployables.RustDeployableData d : net.spacerust.client.deployables.RustDeployableManager.deployables) {
            Box box = d.getBoundingBox();
            Optional<Vec3d> clip = box.raycast(camPos, endVec);
            if (clip.isPresent()) {
                double dist = camPos.squaredDistanceTo(clip.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    hitEntity = null;
                    hitPiece = null;
                    hitDeployable = d;
                    hitPos = clip.get();
                }
            }
        }

        // Handle Hit
        if (hitEntity != null && hitPos != null) {
            boolean isHeadshot = (hitPos.y >= hitEntity.getY() + hitEntity.getHeight() * 0.72);
            float finalDmg = isHeadshot ? damage * 2.0f : damage;

            // Trigger hitmarker overlay
            RustCrosshairOverlay.triggerHitmarker(isHeadshot);

            // Play hit sounds
            if (isHeadshot) {
                RustSoundManager.play("inventory_pickup_metal", 1.0f, 1.6f); // Rust headshot crunch
            } else {
                RustSoundManager.play("ui_click", 0.8f, 1.4f); // Body hit tick
            }

            // Spawn blood particles
            for (int i = 0; i < 5; i++) {
                client.world.addParticle(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z,
                        (Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.2);
            }

            // Attack entity (client prediction & server sync)
            if (client.interactionManager != null) {
                client.interactionManager.attackEntity(player, hitEntity);
            }
        } else if (hitDeployable != null && hitPos != null) {
            // Damage door or container
            hitDeployable.health = Math.max(0, hitDeployable.health - damage * 0.25f);
            RustSoundManager.play(hitDeployable.type.isDoor ? "wood_door_close" : "inventory_pickup_wood", 0.8f, 1.2f);
            if (hitDeployable.health <= 0) {
                net.spacerust.client.deployables.RustDeployableManager.deployables.remove(hitDeployable);
                RustSoundManager.play("debris_wood", 1.0f, 0.9f);
            }
            net.spacerust.client.deployables.RustDeployableManager.save();
        } else if (hitPiece != null && hitPos != null) {
            // Raid / damage building piece
            hitPiece.health = Math.max(0, hitPiece.health - damage * 0.1f);
            RustSoundManager.play("inventory_pickup_stone", 0.7f, 1.2f);
            RustBuildingManager.save();
        } else if (hitPos != null) {
            // Spark / dust at block hit
            client.world.addParticle(ParticleTypes.SMOKE, hitPos.x, hitPos.y, hitPos.z, 0, 0.05, 0);
        }
    }

    public static void reload(MinecraftClient client) {
        RustWeapon weapon = getHeldWeapon(client);
        if (weapon == null || client.player == null) return;

        int current = getAmmo(weapon);
        if (current >= weapon.maxAmmo) return; // Magazine full

        // Play reload sound
        RustSoundManager.play(weapon.reloadSound, 1.0f, 1.0f);
        setAmmo(weapon, weapon.maxAmmo);
    }
}
