package net.spacerust.client.guns;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class RustRecoilManager {

    public static class RecoilProfile {
        public float pitchMin = 2.0f;
        public float pitchMax = 3.0f;
        public float yawMin = -1.0f;
        public float yawMax = 1.0f;
        public float ads = 0.6f;
        public int shots = 30;
    }

    private static final Map<String, RecoilProfile> PROFILES = new HashMap<>();
    private static int currentBurstShot = 0;
    private static long lastShotTime = 0;
    private static float accumulatedPitchRecovery = 0f;

    public static void init() {
        try (InputStream in = RustRecoilManager.class.getResourceAsStream("/assets/spacerust/data/recoil-profiles.json")) {
            if (in != null) {
                Type type = new TypeToken<Map<String, JsonObject>>(){}.getType();
                Map<String, JsonObject> raw = new Gson().fromJson(new InputStreamReader(in), type);
                for (Map.Entry<String, JsonObject> e : raw.entrySet()) {
                    JsonObject obj = e.getValue();
                    RecoilProfile p = new RecoilProfile();
                    if (obj.has("pitchMin")) p.pitchMin = obj.get("pitchMin").getAsFloat();
                    if (obj.has("pitchMax")) p.pitchMax = obj.get("pitchMax").getAsFloat();
                    if (obj.has("yawMin")) p.yawMin = obj.get("yawMin").getAsFloat();
                    if (obj.has("yawMax")) p.yawMax = obj.get("yawMax").getAsFloat();
                    if (obj.has("ads")) p.ads = obj.get("ads").getAsFloat();
                    if (obj.has("shots")) p.shots = obj.get("shots").getAsInt();
                    PROFILES.put(e.getKey(), p);
                }
                System.out.println("[SpaceRust] Loaded " + PROFILES.size() + " weapon recoil profiles.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static RecoilProfile getProfile(RustWeapon weapon) {
        if (PROFILES.containsKey(weapon.profileName)) {
            return PROFILES.get(weapon.profileName);
        }
        RecoilProfile fallback = new RecoilProfile();
        fallback.pitchMin = 2.0f;
        fallback.pitchMax = 3.0f;
        fallback.yawMin = -1.0f;
        fallback.yawMax = 1.0f;
        return fallback;
    }

    public static void onShoot(MinecraftClient client, RustWeapon weapon, boolean isAiming) {
        long now = System.currentTimeMillis();
        if (now - lastShotTime > 450) {
            currentBurstShot = 0;
            accumulatedPitchRecovery = 0f;
        }
        lastShotTime = now;
        currentBurstShot++;

        RecoilProfile profile = getProfile(weapon);
        float progress = Math.min(1.0f, (float) currentBurstShot / (float) Math.max(1, profile.shots));

        float basePitch = profile.pitchMin + (float) Math.random() * (profile.pitchMax - profile.pitchMin);
        float baseYaw = profile.yawMin + (float) Math.random() * (profile.yawMax - profile.yawMin);

        // Pattern curves: higher shots drift more
        float pitchKick = basePitch * (0.85f + 0.3f * progress);
        float yawKick = baseYaw * (0.8f + 0.4f * progress);

        if (isAiming) {
            pitchKick *= profile.ads;
            yawKick *= profile.ads;
        }

        if (client.player != null) {
            // Negative pitch in Minecraft pitches camera UP
            client.player.pitch -= pitchKick;
            client.player.yaw += yawKick;
            accumulatedPitchRecovery += pitchKick * 0.4f; // Recovers 40% of pitch
        }
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null) return;
        long now = System.currentTimeMillis();
        // Recoil recovery: smoothly return aim after burst ends
        if (now - lastShotTime > 120 && accumulatedPitchRecovery > 0.05f) {
            float step = accumulatedPitchRecovery * 0.15f;
            client.player.pitch += step;
            accumulatedPitchRecovery -= step;
        }
    }
}
