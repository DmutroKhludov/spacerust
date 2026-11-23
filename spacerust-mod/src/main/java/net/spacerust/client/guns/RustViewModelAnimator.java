package net.spacerust.client.guns;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.spacerust.client.SpaceRustClient;
import net.spacerust.client.SpaceRustItems;
import net.spacerust.client.gui.RustCraftingScreen;
import net.spacerust.client.world.RustWorldSync;

import java.util.Locale;

/**
 * Smooth frame-interpolated Rust first-person animation state machine:
 * - Deploy / Pull-out (wielding weapons & tools)
 * - Aim Down Sights (ADS hold Right-Click)
 * - Recoil kickback + Muzzle Flash + Camera Kick
 * - Reload animation + completion timer
 * - Melee tool chop / swing animation
 * - Medical Syringe / Bandage self-use animation
 * - Walk bob, idle breathing & tactical sprint cant
 */
public final class RustViewModelAnimator {

    private static long lastFrameNs = System.nanoTime();

    private static int lastSlot = -1;
    private static String lastItemId = "";

    public static float deployProgress = 1.0f;     // 0 = low hidden -> 1 = ready
    public static float adsProgress = 0.0f;        // 0 = hipfire -> 1 = centered iron sights
    public static float recoilKick = 0.0f;         // 0..1 sharp impulse
    public static float recoilYawJitter = 0.0f;    // -1..1 random shake
    public static float sprintProgress = 0.0f;     // 0 = ready -> 1 = lowered diagonal sprint
    public static float reloadProgress = 0.0f;     // 0 = not reloading, 0..1 = reloading
    public static float swingProgress = 0.0f;      // 0 = idle, 0..1 = tool/melee swing arc
    public static float medUseProgress = 0.0f;     // 0 = idle, 0..1 = inject/wrap animation
    public static float bowDrawProgress = 0.0f;    // 0 = relaxed, 0..1 = full Rust bow draw
    public static float walkPhase = 0.0f;
    public static float walkAmount = 0.0f;
    public static float idleTime = 0.0f;

    public static volatile boolean torchLit = false;
    public static volatile double torchLightX = 0.0;
    public static volatile double torchLightY = 0.0;
    public static volatile double torchLightZ = 0.0;
    private static int lastLitBlockX = Integer.MIN_VALUE;
    private static int lastLitBlockY = 0;
    private static int lastLitBlockZ = 0;
    private static long lastTorchRelightMs = 0L;

    public static long muzzleFlashUntilMs = 0L;
    private static boolean reloading = false;
    private static long reloadStartMs = 0L;
    private static GunManager.GunInfo reloadingGun = null;

    private static boolean swinging = false;
    private static long swingStartMs = 0L;

    private static boolean usingMed = false;
    private static long medStartMs = 0L;
    private static String medKind = "";

    private static boolean bowDrawing = false;
    private static boolean bowFiredThisHold = false;

    private static long lastDryFireMs = 0L;
    private static long lastAttackDownMs = 0L;

    private RustViewModelAnimator() {
    }

    public static void updateFrame(MinecraftClient client) {
        long nowNs = System.nanoTime();
        float dt = Math.max(0.0005f, Math.min(0.05f, (nowNs - lastFrameNs) / 1_000_000_000.0f));
        lastFrameNs = nowNs;
        idleTime += dt;

        // Deploy smooth cubic ease
        if (deployProgress < 1.0f) {
            deployProgress = Math.min(1.0f, deployProgress + dt / 0.36f);
        }

        // Hunting Bow smooth RMB draw & LMB/release fire (authentic Rust bow feel)
        GunManager.GunInfo heldGun = GunManager.getHeldGun(client);
        boolean holdingBow = heldGun != null && "bow".equals(heldGun.key);
        boolean usePressed = client != null && client.currentScreen == null
                && client.player != null && client.options.keyUse.isPressed();
        if (holdingBow && client != null && client.player != null) {
            if (usePressed) {
                if (!bowFiredThisHold && deployProgress > 0.45f) {
                    if (hasArrowOrCreative(client)) {
                        if (!bowDrawing) {
                            bowDrawing = true;
                            playSound(client, "bow_draw_start", 0.72f, 1.0f);
                        }
                        bowDrawProgress = Math.min(1.0f, bowDrawProgress + dt / 0.80f);
                    } else if (System.currentTimeMillis() - lastDryFireMs > 320L) {
                        lastDryFireMs = System.currentTimeMillis();
                        bowFiredThisHold = true;
                        playSound(client, "dry_fire", 0.55f);
                    }
                }
            } else {
                if (bowDrawing) {
                    bowDrawing = false;
                    if (bowDrawProgress >= 0.42f && !bowFiredThisHold) {
                        fireBowArrow(client, bowDrawProgress);
                    }
                }
                bowFiredThisHold = false;
                bowDrawProgress = Math.max(0.0f, bowDrawProgress - dt * 6.5f);
            }
        } else {
            bowDrawing = false;
            bowDrawProgress = 0.0f;
            if (!usePressed) bowFiredThisHold = false;
        }

        // ADS spring
        boolean wantAds = holdingBow
                ? (bowDrawing && bowDrawProgress > 0.05f)
                : (usePressed
                && !reloading
                && !usingMed
                && heldGun != null
                && deployProgress > 0.65f);
        GunManager.isAiming = wantAds;
        float adsTarget = holdingBow ? bowDrawProgress : (wantAds ? 1.0f : 0.0f);
        adsProgress += (adsTarget - adsProgress) * Math.min(1.0f, dt * 15.0f);
        if (Math.abs(adsProgress - adsTarget) < 0.002f) adsProgress = adsTarget;

        // Recoil spring decay
        recoilKick *= Math.exp(-dt * 18.0f);
        recoilYawJitter *= Math.exp(-dt * 16.0f);
        if (recoilKick < 0.001f) recoilKick = 0.0f;

        // Sprint cant interpolation
        boolean sprinting = client != null && client.player != null
                && client.player.isSprinting() && !wantAds && !swinging && !reloading && !usingMed;
        float spTarget = sprinting ? 1.0f : 0.0f;
        sprintProgress += (spTarget - sprintProgress) * Math.min(1.0f, dt * 10.0f);

        // Walk bob accumulation
        float speed = 0.0f;
        if (client != null && client.player != null && client.player.isOnGround()) {
            double vx = client.player.getVelocity().x;
            double vz = client.player.getVelocity().z;
            speed = (float) Math.min(1.0, Math.sqrt(vx * vx + vz * vz) * 6.5);
        }
        walkAmount += (speed - walkAmount) * Math.min(1.0f, dt * 10.0f);
        if (walkAmount > 0.02f) {
            walkPhase += dt * (sprinting ? 12.0f : 8.5f);
        }

        long nowMs = System.currentTimeMillis();

        // Reload progression
        if (reloading && reloadingGun != null) {
            float dur = Math.max(600f, reloadingGun.reloadMs);
            reloadProgress = Math.min(1.0f, (nowMs - reloadStartMs) / dur);
            if (reloadProgress >= 1.0f) {
                reloading = false;
                reloadProgress = 0.0f;
                GunManager.reload(reloadingGun);
                reloadingGun = null;
            }
        } else {
            reloadProgress = 0.0f;
        }

        // Melee / tool swing progression
        if (swinging) {
            float dur = 360.0f;
            swingProgress = Math.min(1.0f, (nowMs - swingStartMs) / dur);
            if (swingProgress >= 1.0f) {
                swinging = false;
                swingProgress = 0.0f;
            }
        } else {
            swingProgress = 0.0f;
        }

        // Medical syringe / bandage progression
        if (usingMed) {
            float dur = 950.0f;
            medUseProgress = Math.min(1.0f, (nowMs - medStartMs) / dur);
            if (medUseProgress >= 1.0f) {
                usingMed = false;
                medUseProgress = 0.0f;
                if (!medKind.isEmpty()) {
                    RustWorldSync.sendRaw("USE_MED|" + medKind);
                    medKind = "";
                }
            }
        } else {
            medUseProgress = 0.0f;
        }
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            lastSlot = -1;
            lastItemId = "";
            reloading = false;
            usingMed = false;
            GunManager.isAiming = false;
            return;
        }

        int slot = client.player.inventory.selectedSlot;
        ItemStack stack = client.player.getMainHandStack();
        String itemKey = stack.isEmpty() ? "" : (stack.getItem().toString() + ":" + stack.getName().getString());

        if (slot != lastSlot || !itemKey.equals(lastItemId)) {
            lastSlot = slot;
            lastItemId = itemKey;
            onItemDeployed(client, stack);
        }

        // Track hand swing from external sources (hitting blocks/ores/entities)
        if (client.player.handSwinging && client.player.handSwingTicks == 0 && !swinging) {
            GunManager.GunInfo heldGun = GunManager.getHeldGun(client);
            if (heldGun == null) {
                triggerMeleeSwing(client, stack);
            }
        }

        // Full-auto firing while holding Left Mouse Button (AK-47 / Thompson / MP5)
        if (client.currentScreen == null && client.options.keyAttack.isPressed()) {
            GunManager.GunInfo gun = GunManager.getHeldGun(client);
            if (gun != null && gun.automatic) {
                tryShoot(client, gun);
            }
        }

        // Dynamic torch world light position update & subtle flame particles
        if (torchLit) {
            boolean stillTorch = "torch".equals(SpaceRustItems.getId(stack));
            if (!stillTorch) {
                setTorchLit(client, false);
            } else {
                updateTorchWorldLight(client, false);
                if (client.player.age % 3 == 0 && Math.random() < 0.7) {
                    Vec3d look = client.player.getRotationVec(1.0f).normalize();
                    Vec3d right = look.crossProduct(new Vec3d(0, 1, 0)).normalize();
                    Vec3d tip = client.player.getCameraPosVec(1.0f)
                            .add(look.multiply(0.55))
                            .add(right.multiply(0.24))
                            .add(0, -0.05, 0);
                    client.world.addParticle(ParticleTypes.FLAME,
                            tip.x + (Math.random() - 0.5) * 0.03,
                            tip.y + Math.random() * 0.04,
                            tip.z + (Math.random() - 0.5) * 0.03,
                            0.0, 0.015, 0.0);
                }
            }
        }
    }

    private static void onItemDeployed(MinecraftClient client, ItemStack stack) {
        deployProgress = 0.0f;
        adsProgress = 0.0f;
        reloading = false;
        reloadProgress = 0.0f;
        reloadingGun = null;
        usingMed = false;
        medUseProgress = 0.0f;
        swinging = false;
        swingProgress = 0.0f;
        bowDrawing = false;
        bowDrawProgress = 0.0f;
        bowFiredThisHold = false;
        GunManager.syncSelectedSlot(client);

        if (stack == null || stack.isEmpty()) {
            setTorchLit(client, false);
            return;
        }
        String vm = RustViewModelRenderer.resolveViewModelKey(stack);
        if (!"torch".equals(vm) && torchLit) {
            setTorchLit(client, false);
        }
        if ("ak47".equals(vm) || "hmlmg".equals(vm) || "lr300".equals(vm) || "sar".equals(vm) || "m39".equals(vm)) playSound(client, "ak_deploy", 0.55f);
        else if ("revolver".equals(vm) || "python".equals(vm) || "p250".equals(vm) || "m92".equals(vm)) playSound(client, "revolver_deploy", 0.55f);
        else if ("shotgun".equals(vm) || "pump".equals(vm) || "eoka".equals(vm)) playSound(client, "shotgun_deploy", 0.55f);
        else if ("thompson".equals(vm) || "mp5".equals(vm) || "customsmg".equals(vm)) playSound(client, "thompson_deploy", 0.55f);
        else if ("bolt".equals(vm) || "l96".equals(vm)) playSound(client, "bolt_deploy", 0.55f);
        else if ("rock".equals(vm)) playSound(client, "rock_deploy", 0.45f);
        else if ("axe".equals(vm) || "stoneaxe".equals(vm) || "salvagedaxe".equals(vm) || "machete".equals(vm) || "boneknife".equals(vm)) playSound(client, "axe_deploy", 0.45f);
        else if ("pickaxe".equals(vm) || "stonepick".equals(vm) || "salvagedicepick".equals(vm) || "jackhammer".equals(vm)) playSound(client, "pickaxe_deploy", 0.45f);
        else if ("hammer".equals(vm)) playSound(client, "hammer_deploy", 0.45f);
        else if ("plan".equals(vm) || "woodenspear".equals(vm) || "stonespear".equals(vm)) playSound(client, "axe_deploy", 0.30f);
        else if ("syringe".equals(vm)) playSound(client, "syringe_deploy", 0.45f);
        else if ("bandage".equals(vm)) playSound(client, "bandage_deploy", 0.45f);
        else if ("torch".equals(vm)) playSound(client, "torch_deploy", 0.45f);
        else if ("rocket_launcher".equals(SpaceRustItems.getId(stack))) playSound(client, "rocket_deploy", 0.55f);
    }

    public static void tryShoot(MinecraftClient client, GunManager.GunInfo gun) {
        if (client == null || client.player == null || gun == null) return;
        if (reloading || deployProgress < 0.55f) return;

        long now = System.currentTimeMillis();
        if (now - GunManager.lastShotTime < gun.fireRateMs) {
            return;
        }

        if (GunManager.currentAmmo <= 0) {
            if (now - lastDryFireMs > 280L) {
                lastDryFireMs = now;
                playSound(client, "dry_fire", 0.55f);
            }
            return;
        }

        GunManager.lastShotTime = now;
        GunManager.consumeOneAmmo(client);

        // Kick viewmodel + muzzle flash
        recoilKick = Math.min(1.45f, recoilKick + (gun.key.equals("shotgun") || gun.key.equals("bolt") ? 1.1f : 0.72f));
        recoilYawJitter = (float) ((Math.random() - 0.5) * 2.0);
        muzzleFlashUntilMs = now + 52L;

        // Smooth camera kick (less recoil while ADS)
        float adsScale = GunManager.isAiming ? 0.65f : 1.0f;
        client.player.pitch += gun.recoilPitch * adsScale;
        client.player.yaw += (float) ((Math.random() - 0.5) * gun.recoilYaw * 2.0 * adsScale);

        // Gunshot sound
        String shotSnd = gun.key + "_shoot";
        if ("ak47".equals(gun.key) || "hmlmg".equals(gun.key)) shotSnd = "ak_shoot";
        else if ("rocket_launcher".equals(gun.key)) shotSnd = "rocket_shoot";
        else if ("eoka".equals(gun.key)) shotSnd = "shotgun_shoot";
        else if ("m39".equals(gun.key)) shotSnd = "sar_shoot";
        if (!SpaceRustClient.WEAPON_SOUNDS.containsKey(shotSnd)) shotSnd = "revolver_shoot";
        playSound(client, shotSnd, 0.72f);

        // Visual bullet tracer streaks (matches Rust combat tracer trails)
        if (client.world != null) {
            Vec3d eye = client.player.getCameraPosVec(1.0f);
            Vec3d look = client.player.getRotationVec(1.0f).normalize();
            boolean rocket = "rocket_launcher".equals(gun.key);
            int steps = rocket ? 10 : 6;
            for (int i = 1; i <= steps; i++) {
                double dist = 1.4 + i * (rocket ? 2.2 : 2.6);
                Vec3d pt = eye.add(look.multiply(dist));
                client.world.addParticle(
                        rocket ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.CRIT,
                        pt.x, pt.y - 0.04, pt.z,
                        look.x * 0.4, look.y * 0.4, look.z * 0.4);
                if (rocket || "incendiary".equals(GunManager.selectedAmmoMode)) {
                    client.world.addParticle(ParticleTypes.FLAME, pt.x, pt.y - 0.04, pt.z, 0, 0, 0);
                }
            }
        }

        // Server hitscan / rocket raytrace & ammo mode sync
        RustWorldSync.sendRaw("SHOOT|" + gun.key + "|" + (GunManager.isAiming ? "1" : "0") + "|" + GunManager.selectedAmmoMode);
    }

    public static void startReload(MinecraftClient client, GunManager.GunInfo gun) {
        if (client == null || gun == null || reloading) return;
        if (GunManager.currentAmmo >= gun.maxAmmo) return;
        reloading = true;
        reloadStartMs = System.currentTimeMillis();
        reloadingGun = gun;
        adsProgress = 0.0f;
        GunManager.isAiming = false;

        String relSnd = gun.key + "_reload";
        if ("ak47".equals(gun.key) || "hmlmg".equals(gun.key)) relSnd = "ak_reload";
        else if ("rocket_launcher".equals(gun.key)) relSnd = "rocket_reload";
        else if ("eoka".equals(gun.key) || "bow".equals(gun.key)) relSnd = "crossbow_reload";
        else if ("m39".equals(gun.key)) relSnd = "sar_reload";
        if (!SpaceRustClient.WEAPON_SOUNDS.containsKey(relSnd)) relSnd = "bolt_reload";
        playSound(client, relSnd, 0.65f);
    }

    public static void triggerMeleeSwing(MinecraftClient client, ItemStack stack) {
        long now = System.currentTimeMillis();
        if (swinging && now - swingStartMs < 180L) return;
        swinging = true;
        swingStartMs = now;
        if (stack != null && !stack.isEmpty()) {
            playSound(client, "melee_swing", 0.35f);
        }
    }

    public static boolean tryUseMedical(MinecraftClient client, ItemStack stack) {
        if (client == null || stack == null || stack.isEmpty() || usingMed) return false;
        String vm = RustViewModelRenderer.resolveViewModelKey(stack);
        if (!"syringe".equals(vm) && !"bandage".equals(vm)) return false;

        usingMed = true;
        medStartMs = System.currentTimeMillis();
        medKind = vm;
        playSound(client, "syringe".equals(vm) ? "syringe_use" : "bandage_use", 0.55f);
        return true;
    }

    public static float getAdsFovZoom(MinecraftClient client) {
        if (adsProgress <= 0.005f) return 0.0f;
        GunManager.GunInfo gun = GunManager.getHeldGun(client);
        float zoom = (gun != null) ? gun.adsZoomFov : 14.0f;
        return zoom * adsProgress;
    }

    public static void playSound(MinecraftClient client, String key, float volume) {
        playSound(client, key, volume, 0.96f + (float) Math.random() * 0.08f);
    }

    public static void playSound(MinecraftClient client, String key, float volume, float pitch) {
        if (client == null || client.getSoundManager() == null) return;
        SoundEvent ev = SpaceRustClient.WEAPON_SOUNDS.get(key);
        if (ev != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(ev, pitch, volume));
        }
    }

    public static void toggleTorch(MinecraftClient client) {
        if (client == null || client.player == null) return;
        if (!"torch".equals(SpaceRustItems.getId(client.player.getMainHandStack()))) return;
        boolean next = !torchLit;
        setTorchLit(client, next);
        recoilKick = Math.min(1.0f, recoilKick + 0.30f);
        playSound(client, next ? "torch_ignite" : "torch_extinguish", 0.72f, 0.96f + (float) Math.random() * 0.08f);
    }

    private static void setTorchLit(MinecraftClient client, boolean lit) {
        boolean changed = (torchLit != lit);
        torchLit = lit;
        if (lit) {
            updateTorchWorldLight(client, true);
        } else if (changed) {
            if (client != null && client.worldRenderer != null && lastLitBlockX != Integer.MIN_VALUE) {
                int r = 17;
                client.worldRenderer.scheduleBlockRenders(
                        lastLitBlockX - r, lastLitBlockY - r, lastLitBlockZ - r,
                        lastLitBlockX + r, lastLitBlockY + r, lastLitBlockZ + r);
            }
            lastLitBlockX = Integer.MIN_VALUE;
        }
    }

    private static void updateTorchWorldLight(MinecraftClient client, boolean force) {
        if (client == null || client.player == null || client.worldRenderer == null) return;
        Vec3d eye = client.player.getCameraPosVec(1.0f);
        torchLightX = eye.x;
        torchLightY = eye.y;
        torchLightZ = eye.z;
        int bx = (int) Math.floor(eye.x);
        int by = (int) Math.floor(eye.y);
        int bz = (int) Math.floor(eye.z);
        long now = System.currentTimeMillis();
        int dx = bx - lastLitBlockX;
        int dy = by - lastLitBlockY;
        int dz = bz - lastLitBlockZ;
        boolean movedEnough = lastLitBlockX == Integer.MIN_VALUE || (dx * dx + dy * dy + dz * dz) >= 3;
        if (force || (movedEnough && now - lastTorchRelightMs >= 130L)) {
            int r = 17;
            if (lastLitBlockX != Integer.MIN_VALUE) {
                client.worldRenderer.scheduleBlockRenders(
                        lastLitBlockX - r, lastLitBlockY - r, lastLitBlockZ - r,
                        lastLitBlockX + r, lastLitBlockY + r, lastLitBlockZ + r);
            }
            client.worldRenderer.scheduleBlockRenders(bx - r, by - r, bz - r, bx + r, by + r, bz + r);
            lastLitBlockX = bx;
            lastLitBlockY = by;
            lastLitBlockZ = bz;
            lastTorchRelightMs = now;
        }
    }

    public static boolean isTorchLightActive() {
        return torchLit;
    }

    public static boolean isTorchLitForRender() {
        return torchLit;
    }

    public static boolean hasArrowOrCreative(MinecraftClient client) {
        if (client == null || client.player == null) return false;
        return client.player.isCreative() || RustCraftingScreen.countPlayerResource(client.player, Items.ARROW) > 0;
    }

    public static boolean canShootBow() {
        return bowDrawing && bowDrawProgress >= 0.42f && !bowFiredThisHold;
    }

    public static void tryShootBowFromLmb(MinecraftClient client) {
        if (!canShootBow()) return;
        bowDrawing = false;
        bowFiredThisHold = true;
        float power = bowDrawProgress;
        bowDrawProgress = 0.0f;
        fireBowArrow(client, power);
    }

    private static void fireBowArrow(MinecraftClient client, float power) {
        if (client == null || client.player == null) return;
        float clamped = Math.max(0.42f, Math.min(1.0f, power));
        playSound(client, "bow_shoot", 0.78f, 0.94f + clamped * 0.16f);
        recoilKick = Math.min(1.25f, recoilKick + 0.65f + 0.35f * clamped);
        recoilYawJitter = (float) ((Math.random() - 0.5) * 0.6);
        client.player.pitch -= 1.0f * clamped;
        RustWorldSync.sendRaw("BOW_SHOOT|" + String.format(Locale.US, "%.2f", clamped));
    }
}
