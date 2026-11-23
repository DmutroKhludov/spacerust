package net.spacerust.client.guns;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.spacerust.client.SpaceRustItems;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages Rust firearm metadata, per-slot ammo magazines, recoil specs, and ADS zoom.
 */
public class GunManager {

    public static class GunInfo {
        public final String key;
        public final String name;
        public final String ammoType;
        public final int maxAmmo;
        public final int fireRateMs;
        public final int reloadMs;
        public final float damage;
        public final float recoilPitch;
        public final float recoilYaw;
        public final float adsZoomFov;
        public final boolean automatic;

        public GunInfo(String key, String name, String ammoType, int maxAmmo, int fireRateMs, int reloadMs,
                       float damage, float recoilPitch, float recoilYaw, float adsZoomFov, boolean automatic) {
            this.key = key;
            this.name = name;
            this.ammoType = ammoType;
            this.maxAmmo = maxAmmo;
            this.fireRateMs = fireRateMs;
            this.reloadMs = reloadMs;
            this.damage = damage;
            this.recoilPitch = recoilPitch;
            this.recoilYaw = recoilYaw;
            this.adsZoomFov = adsZoomFov;
            this.automatic = automatic;
        }
    }

    public static final Map<String, GunInfo> GUN_REGISTRY = new HashMap<>();
    static {
        GUN_REGISTRY.put("ak47", new GunInfo("ak47", "АК-47", "5.56 мм", 30, 133, 1900, 10.0f, -2.1f, 0.75f, 18.0f, true));
        GUN_REGISTRY.put("hmlmg", new GunInfo("hmlmg", "Самодельный пулемёт", "5.56 мм", 60, 125, 2700, 11.5f, -2.2f, 0.65f, 16.0f, true));
        GUN_REGISTRY.put("lr300", new GunInfo("lr300", "Винтовка LR-300", "5.56 мм", 30, 120, 1750, 9.5f, -1.5f, 0.45f, 20.0f, true));
        GUN_REGISTRY.put("sar", new GunInfo("sar", "Берданка (SAR)", "5.56 мм", 16, 175, 1650, 9.0f, -2.2f, 0.35f, 22.0f, false));
        GUN_REGISTRY.put("m39", new GunInfo("m39", "Винтовка M39", "5.56 мм", 20, 200, 1800, 11.0f, -2.0f, 0.30f, 26.0f, false));
        GUN_REGISTRY.put("l96", new GunInfo("l96", "Снайперка L96", "5.56 мм", 5, 1100, 2100, 22.0f, -4.6f, 0.25f, 38.0f, false));
        GUN_REGISTRY.put("bolt", new GunInfo("bolt", "Винтовка Bolt", "5.56 мм", 4, 1150, 2100, 19.0f, -5.2f, 0.3f, 34.0f, false));
        GUN_REGISTRY.put("mp5", new GunInfo("mp5", "Пистолет-пулемёт MP5", "9 мм", 30, 100, 1600, 7.2f, -1.3f, 0.45f, 16.0f, true));
        GUN_REGISTRY.put("thompson", new GunInfo("thompson", "Томпсон", "9 мм", 20, 115, 1650, 7.5f, -1.5f, 0.55f, 14.0f, true));
        GUN_REGISTRY.put("customsmg", new GunInfo("customsmg", "Самодельный ПП", "9 мм", 24, 96, 1550, 6.2f, -1.4f, 0.65f, 14.0f, true));
        GUN_REGISTRY.put("p250", new GunInfo("p250", "Пистолет P250", "9 мм", 10, 165, 1350, 8.0f, -1.9f, 0.38f, 13.0f, false));
        GUN_REGISTRY.put("m92", new GunInfo("m92", "Пистолет M92", "9 мм", 15, 150, 1400, 8.5f, -1.8f, 0.32f, 14.0f, false));
        GUN_REGISTRY.put("revolver", new GunInfo("revolver", "Револьвер", "9 мм", 8, 250, 1500, 9.0f, -2.8f, 0.45f, 12.0f, false));
        GUN_REGISTRY.put("python", new GunInfo("python", "Револьвер Python", "9 мм", 6, 220, 1650, 12.0f, -3.4f, 0.40f, 16.0f, false));
        GUN_REGISTRY.put("pump", new GunInfo("pump", "Помповый дробовик", "12 калибр", 6, 520, 1900, 17.5f, -4.2f, 0.9f, 12.0f, false));
        GUN_REGISTRY.put("shotgun", new GunInfo("shotgun", "Двуствольный дробовик", "12 калибр", 2, 420, 1700, 18.0f, -4.4f, 1.1f, 10.0f, false));
        GUN_REGISTRY.put("eoka", new GunInfo("eoka", "Самопал (Eoka)", "12 калибр", 1, 900, 1300, 16.0f, -5.0f, 1.5f, 8.0f, false));
        GUN_REGISTRY.put("crossbow", new GunInfo("crossbow", "Арбалет", "Стрела", 1, 800, 1400, 12.0f, -1.8f, 0.2f, 15.0f, false));
        GUN_REGISTRY.put("bow", new GunInfo("bow", "Охотничий лук", "Стрела", 1, 600, 900, 10.0f, -1.2f, 0.2f, 12.0f, false));
        GUN_REGISTRY.put("rocket_launcher", new GunInfo("rocket_launcher", "Ракетница", "Ракета", 1, 1500, 2600, 32.0f, -6.5f, 1.2f, 10.0f, false));
    }

    public static int currentAmmo = 30;
    public static long lastShotTime = 0;
    public static boolean isAiming = false;
    public static String selectedAmmoMode = "standard";
    private static final Map<Integer, Integer> SLOT_AMMO = new HashMap<>();

    public static GunInfo getHeldGun(MinecraftClient client) {
        if (client == null || client.player == null) return null;
        return getGunForStack(client.player.getMainHandStack());
    }

    public static GunInfo getGunForStack(ItemStack stack) {
        SpaceRustItems.ItemDef def = SpaceRustItems.getDef(stack);
        if (def != null && def.gunKey != null) {
            return GUN_REGISTRY.get(def.gunKey);
        }
        return null;
    }

    public static void syncSelectedSlot(MinecraftClient client) {
        if (client == null || client.player == null) return;
        GunInfo gun = getHeldGun(client);
        if (gun == null) return;
        int slot = client.player.inventory.selectedSlot;
        currentAmmo = SLOT_AMMO.computeIfAbsent(slot, s -> gun.maxAmmo);
        if (currentAmmo > gun.maxAmmo) {
            currentAmmo = gun.maxAmmo;
            SLOT_AMMO.put(slot, currentAmmo);
        }
    }

    public static void consumeOneAmmo(MinecraftClient client) {
        if (currentAmmo > 0) currentAmmo--;
        if (client != null && client.player != null) {
            SLOT_AMMO.put(client.player.inventory.selectedSlot, currentAmmo);
        }
    }

    public static void reload(GunInfo gun) {
        if (gun == null) return;
        currentAmmo = gun.maxAmmo;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            SLOT_AMMO.put(client.player.inventory.selectedSlot, currentAmmo);
        }
    }
}
