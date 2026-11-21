package net.spacerust.client.guns;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.spacerust.client.hud.RustCrosshairOverlay;

import java.util.HashMap;
import java.util.Map;

public class GunManager {

    public static class GunInfo {
        public final String name;
        public final String ammoType;
        public final int maxAmmo;
        public final int fireRateMs;
        public final float damage;
        public final float recoilPitch;
        public final float recoilYaw;

        public GunInfo(String name, String ammoType, int maxAmmo, int fireRateMs, float damage, float recoilPitch, float recoilYaw) {
            this.name = name;
            this.ammoType = ammoType;
            this.maxAmmo = maxAmmo;
            this.fireRateMs = fireRateMs;
            this.damage = damage;
            this.recoilPitch = recoilPitch;
            this.recoilYaw = recoilYaw;
        }
    }

    public static final Map<String, GunInfo> GUN_REGISTRY = new HashMap<>();
    static {
        GUN_REGISTRY.put("ak47", new GunInfo("АК-47", "5.56 мм", 30, 133, 40.0f, -2.4f, 0.8f));
        GUN_REGISTRY.put("thompson", new GunInfo("Томпсон", "9 мм", 20, 110, 28.0f, -1.6f, 0.5f));
        GUN_REGISTRY.put("revolver", new GunInfo("Револьвер", "9 мм", 8, 300, 35.0f, -3.2f, 0.4f));
        GUN_REGISTRY.put("bolt", new GunInfo("Винтовка Bolt", "5.56 мм", 4, 1200, 85.0f, -5.5f, 0.2f));
    }

    // Current player gun state
    public static int currentAmmo = 30;
    public static long lastShotTime = 0;
    public static boolean isAiming = false; // ADS

    public static GunInfo getHeldGun(MinecraftClient client) {
        if (client.player == null) return null;
        ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty()) return null;

        String name = held.getName().getString().toLowerCase();
        // Match either custom name or item type (like Bow, Crossbow, Diamond Sword re-skinned)
        if (name.contains("ak") || name.contains("калаш") || held.getItem() == Items.CROSSBOW) {
            return GUN_REGISTRY.get("ak47");
        } else if (name.contains("thompson") || name.contains("томпсон")) {
            return GUN_REGISTRY.get("thompson");
        } else if (name.contains("revolver") || name.contains("револьвер") || held.getItem() == Items.BOW) {
            return GUN_REGISTRY.get("revolver");
        } else if (name.contains("bolt") || name.contains("болт")) {
            return GUN_REGISTRY.get("bolt");
        }

        return null;
    }

    public static void shoot(MinecraftClient client, GunInfo gun) {
        long now = System.currentTimeMillis();
        if (now - lastShotTime < gun.fireRateMs) {
            return; // Fire rate limit
        }

        if (currentAmmo <= 0) {
            // Play empty click
            return;
        }

        lastShotTime = now;
        currentAmmo--;

        // 1. Recoil kick simulation
        float pitchRecoil = gun.recoilPitch;
        float yawRecoil = (float) ((Math.random() - 0.5) * gun.recoilYaw * 2);
        if (client.player != null) {
            client.player.pitch += pitchRecoil;
            client.player.yaw += yawRecoil;
        }

        // 2. Trigger hitmarker on crosshair for feedback
        RustCrosshairOverlay.triggerHitmarker(false);
    }

    public static void reload(GunInfo gun) {
        currentAmmo = gun.maxAmmo;
    }
}
