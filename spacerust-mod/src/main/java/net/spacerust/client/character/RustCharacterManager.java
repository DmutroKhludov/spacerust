package net.spacerust.client.character;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Generates deterministic randomized Rust-style characters and races per player.
 * Used both for in-game player skins and the main menu character avatar.
 */
public class RustCharacterManager {

    public enum Race {
        NORDIC("Нордик",
                new int[]{0xFFF2C8AD, 0xFFEBC0A2, 0xFFF5D0B8},
                new int[]{0xFFD9B26F, 0xFFB87333, 0xFF8D5B3A, 0xFF4A3728, 0xFF6B5344},
                new int[]{0xFF4E89AE, 0xFF4A7C59, 0xFF6E7F80, 0xFF5D4037}),
        SLAVIC("Славянин",
                new int[]{0xFFE8BA99, 0xFFDFAD8B, 0xFFEECCB2},
                new int[]{0xFFC7A876, 0xFF7B5438, 0xFF4F3828, 0xFF2D231D},
                new int[]{0xFF607D8B, 0xFF4E89AE, 0xFF5D4037, 0xFF556B2F}),
        MEDITERRANEAN("Южанин",
                new int[]{0xFFD49B72, 0xFFC78B62, 0xFFDCAA83},
                new int[]{0xFF3E2723, 0xFF261A16, 0xFF1C1817, 0xFF5A3B28},
                new int[]{0xFF4E342E, 0xFF3E2723, 0xFF556B2F, 0xFF2C1E18}),
        AFRICAN("Африканец",
                new int[]{0xFF7A4B2E, 0xFF633A22, 0xFF4D2B18, 0xFF8C583A},
                new int[]{0xFF181514, 0xFF221D1B, 0xFF121010},
                new int[]{0xFF2A1B14, 0xFF1E130E, 0xFF3A241A}),
        EAST_ASIAN("Азиат",
                new int[]{0xFFEBC298, 0xFFE2B485, 0xFFF0CDA6},
                new int[]{0xFF171618, 0xFF231F20, 0xFF2E2623},
                new int[]{0xFF221A17, 0xFF171312, 0xFF362720}),
        MIDDLE_EASTERN("Ближневосточный",
                new int[]{0xFFBA7E53, 0xFFA96D45, 0xFFC78D64},
                new int[]{0xFF1B1716, 0xFF2A1E1A, 0xFF3A2820},
                new int[]{0xFF2D1E18, 0xFF3B2820, 0xFF1D1512}),
        ISLANDER("Островитянин",
                new int[]{0xFFA66A46, 0xFF965B3A, 0xFFB87854},
                new int[]{0xFF1E1918, 0xFF2D211C, 0xFF3B2A22},
                new int[]{0xFF2B1D16, 0xFF3C281E, 0xFF1F1612});

        public final String displayName;
        public final int[] skinTones;
        public final int[] hairColors;
        public final int[] eyeColors;

        Race(String displayName, int[] skinTones, int[] hairColors, int[] eyeColors) {
            this.displayName = displayName;
            this.skinTones = skinTones;
            this.hairColors = hairColors;
            this.eyeColors = eyeColors;
        }
    }

    public static class CharacterProfile {
        public final String key;
        public final Race race;
        public final String model; // "default" or "slim"
        public final Identifier textureId;

        public CharacterProfile(String key, Race race, String model, Identifier textureId) {
            this.key = key;
            this.race = race;
            this.model = model;
            this.textureId = textureId;
        }
    }

    private static final Map<String, CharacterProfile> CACHE = new HashMap<>();
    private static final int[] SHORTS_COLORS = new int[]{
            0xFF4B4334, // Burlap brown
            0xFF3A4232, // Olive drab
            0xFF3B3D42, // Charcoal cloth
            0xFF5A4535, // Worn leather
            0xFF493933  // Rust sackcloth
    };

    public static CharacterProfile getLocalProfile() {
        MinecraftClient client = MinecraftClient.getInstance();
        String name = "Survivor";
        if (client != null && client.getSession() != null && client.getSession().getUsername() != null) {
            String u = client.getSession().getUsername().trim();
            if (!u.isEmpty()) {
                name = u;
            }
        }
        return getOrCreateProfile(name);
    }

    public static CharacterProfile getProfile(AbstractClientPlayerEntity player) {
        if (player != null && player.getGameProfile() != null) {
            String name = player.getGameProfile().getName();
            if (name != null && !name.trim().isEmpty()) {
                return getOrCreateProfile(name);
            }
            if (player.getUuid() != null) {
                return getOrCreateProfile(player.getUuid().toString());
            }
        }
        return getLocalProfile();
    }

    public static CharacterProfile getProfile(com.mojang.authlib.GameProfile profile) {
        if (profile != null) {
            if (profile.getName() != null && !profile.getName().trim().isEmpty()) {
                return getOrCreateProfile(profile.getName());
            }
            if (profile.getId() != null) {
                return getOrCreateProfile(profile.getId().toString());
            }
        }
        return getLocalProfile();
    }

    public static synchronized CharacterProfile getOrCreateProfile(String rawKey) {
        String key = (rawKey == null || rawKey.trim().isEmpty()) ? "survivor" : rawKey.trim().toLowerCase(Locale.ROOT);
        CharacterProfile existing = CACHE.get(key);
        if (existing != null) {
            return existing;
        }

        long seed = fnv1a64(key);
        Random rng = new Random(seed);

        Race[] races = Race.values();
        Race race = races[rng.nextInt(races.length)];
        int skinBase = race.skinTones[rng.nextInt(race.skinTones.length)];
        int hairColor = race.hairColors[rng.nextInt(race.hairColors.length)];
        int eyeColor = race.eyeColors[rng.nextInt(race.eyeColors.length)];

        int hairStyle = rng.nextInt(6); // 0=bald, 1=buzz, 2=short messy, 3=side part, 4=mohawk, 5=long back
        int beardStyle = rng.nextInt(4); // 0=none, 1=stubble, 2=goatee, 3=full beard
        boolean isSlim = rng.nextInt(10) < 3; // 30% slim build
        String model = isSlim ? "slim" : "default";
        int shortsColor = SHORTS_COLORS[rng.nextInt(SHORTS_COLORS.length)];

        NativeImage skinImg = buildSkinImage(rng, skinBase, hairColor, eyeColor, hairStyle, beardStyle, isSlim, shortsColor);

        String safeHash = Long.toHexString(seed);
        Identifier texId = new Identifier("spacerust", "dynamic_char_" + safeHash);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getTextureManager() != null) {
            client.getTextureManager().registerTexture(texId, new NativeImageBackedTexture(skinImg));
        } else {
            skinImg.close();
        }

        CharacterProfile profile = new CharacterProfile(key, race, model, texId);
        CACHE.put(key, profile);
        return profile;
    }

    private static NativeImage buildSkinImage(Random rng, int skinBase, int hairColor, int eyeColor,
                                              int hairStyle, int beardStyle, boolean isSlim, int shortsColor) {
        NativeImage img = new NativeImage(64, 64, false);
        // Clear transparent
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                img.setPixelColor(x, y, 0);
            }
        }

        int skinDark = shade(skinBase, 0.86f);
        int skinLight = shade(skinBase, 1.07f);
        int hairDark = shade(hairColor, 0.80f);
        int hairLight = shade(hairColor, 1.12f);
        int shortsDark = shade(shortsColor, 0.78f);
        int shortsLight = shade(shortsColor, 1.12f);

        // 1. HEAD (0,0)-(32,16)
        fillBoxSkin(img, rng, 0, 0, 8, 8, 8, skinBase);
        // Subtle forehead/cheek shading on head front (8,8)-(16,16)
        fillRect(img, 8, 8, 8, 2, skinLight);
        fillRect(img, 8, 15, 8, 1, skinDark);

        // Eyebrows
        int browColor = hairStyle == 0 ? shade(skinBase, 0.65f) : hairDark;
        fillRect(img, 9, 10, 2, 1, browColor);
        fillRect(img, 13, 10, 2, 1, browColor);

        // Eyes (y=11)
        setArgb(img, 9, 11, 0xFFF2F2F2);
        setArgb(img, 10, 11, eyeColor);
        setArgb(img, 13, 11, eyeColor);
        setArgb(img, 14, 11, 0xFFF2F2F2);

        // Nose bridge & mouth
        setArgb(img, 11, 12, skinDark);
        setArgb(img, 12, 12, skinDark);
        setArgb(img, 11, 14, shade(skinBase, 0.72f));
        setArgb(img, 12, 14, shade(skinBase, 0.72f));

        // Hair on Head + Hat Layer (32,0)-(64,16)
        drawHair(img, rng, hairStyle, hairColor, hairDark, hairLight, skinBase);

        // Beard / Stubble
        drawBeard(img, beardStyle, hairColor, hairDark, skinBase);

        // 2. TORSO (16,16)-(40,32)
        fillBoxSkin(img, rng, 16, 16, 8, 12, 4, skinBase);
        // Chest & abs shading on torso front (20,20)-(28,32)
        fillRect(img, 21, 21, 2, 2, skinLight);
        fillRect(img, 25, 21, 2, 2, skinLight);
        fillRect(img, 20, 23, 8, 1, skinDark);
        fillRect(img, 22, 24, 4, 3, shade(skinBase, 0.94f));
        // Survival shorts waistband on bottom of torso (y=28..31)
        fillShortsBand(img, 16, 28, 24, 4, shortsColor, shortsDark, shortsLight);
        fillRect(img, 20, 16, 8, 4, skinBase); // Top neck/shoulders
        fillRect(img, 28, 16, 8, 4, shortsDark); // Bottom of torso

        // 3. RIGHT LEG (0,16)-(16,32) & LEFT LEG (16,48)-(32,64)
        fillBoxSkin(img, rng, 0, 16, 4, 12, 4, skinBase);
        fillBoxSkin(img, rng, 16, 48, 4, 12, 4, skinBase);
        // Shorts upper legs (y+4 .. y+8 on leg texture)
        fillShortsBand(img, 0, 20, 16, 5, shortsColor, shortsDark, shortsLight);
        fillShortsBand(img, 16, 52, 16, 5, shortsColor, shortsDark, shortsLight);
        // Top of legs in shorts color
        fillRect(img, 4, 16, 4, 4, shortsDark);
        fillRect(img, 20, 48, 4, 4, shortsDark);
        // Rustic foot wraps at bottom of legs
        fillShortsBand(img, 0, 30, 16, 2, shortsDark, shade(shortsDark, 0.8f), shortsColor);
        fillShortsBand(img, 16, 62, 16, 2, shortsDark, shade(shortsDark, 0.8f), shortsColor);
        fillRect(img, 8, 16, 4, 4, shortsDark);
        fillRect(img, 24, 48, 4, 4, shortsDark);

        // 4. RIGHT ARM (40,16)-(56,32) & LEFT ARM (32,48)-(48,64)
        int armW = isSlim ? 3 : 4;
        fillBoxSkin(img, rng, 40, 16, armW, 12, 4, skinBase);
        fillBoxSkin(img, rng, 32, 48, armW, 12, 4, skinBase);
        // Wrist cloth wrap on right & left forearm
        int armSpan = armW * 2 + 8;
        fillShortsBand(img, 40, 28, armSpan, 2, shortsColor, shortsDark, shortsLight);
        fillShortsBand(img, 32, 60, armSpan, 2, shortsColor, shortsDark, shortsLight);

        return img;
    }

    private static void drawHair(NativeImage img, Random rng, int style, int hair, int hairDark, int hairLight, int skinBase) {
        if (style == 0) {
            // Bald / cleanly shaved head (tiny stubble dots on top)
            for (int y = 0; y < 8; y++) {
                for (int x = 8; x < 16; x++) {
                    if (rng.nextInt(4) == 0) {
                        setArgb(img, x, y, shade(skinBase, 0.90f));
                    }
                }
            }
            return;
        }
        // Top of head (8,0)-(16,8)
        for (int y = 0; y < 8; y++) {
            for (int x = 8; x < 16; x++) {
                if (style == 4 && (x < 11 || x > 12)) continue; // Mohawk
                int col = ((x + y) % 3 == 0) ? hairDark : (((x * 3 + y) % 5 == 0) ? hairLight : hair);
                setArgb(img, x, y, col);
                setArgb(img, x + 32, y, col); // Overlay volume
            }
        }
        // Back of head (24,8)-(32,16)
        int backDepth = (style == 5) ? 7 : (style == 1 ? 3 : 5);
        for (int y = 8; y < 8 + backDepth; y++) {
            for (int x = 24; x < 32; x++) {
                if (style == 4 && (x < 27 || x > 28)) continue;
                int col = ((x + y) % 3 == 0) ? hairDark : hair;
                setArgb(img, x, y, col);
                setArgb(img, x + 32, y, col);
            }
        }
        // Sides: Right (0,8)-(8,16) & Left (16,8)-(24,16)
        if (style != 4) {
            int sideDepth = (style == 1) ? 2 : 3;
            for (int y = 8; y < 8 + sideDepth; y++) {
                for (int x = 0; x < 8; x++) {
                    setArgb(img, x, y, hairDark);
                    setArgb(img, x + 16, y, hairDark);
                }
            }
            // Sideburns
            if (style >= 2) {
                fillRect(img, 6, 10, 2, 2, hairDark);
                fillRect(img, 16, 10, 2, 2, hairDark);
            }
        }
        // Front hairline (8,8)-(16,10)
        if (style == 1) {
            fillRect(img, 8, 8, 8, 1, hairDark);
        } else if (style == 2) {
            fillRect(img, 8, 8, 8, 2, hair);
            setArgb(img, 9, 9, skinBase);
            setArgb(img, 13, 9, skinBase);
            fillRect(img, 40, 8, 8, 2, hairLight);
        } else if (style == 3) {
            fillRect(img, 8, 8, 8, 2, hair);
            fillRect(img, 13, 9, 2, 1, skinBase);
            fillRect(img, 40, 8, 5, 2, hairLight);
        } else if (style == 4) {
            // Mohawk ridge on hat layer
            fillRect(img, 11, 8, 2, 2, hair);
            fillRect(img, 43, 8, 2, 2, hairLight);
        } else {
            fillRect(img, 8, 8, 8, 2, hairDark);
            fillRect(img, 40, 8, 8, 2, hair);
        }
    }

    private static void drawBeard(NativeImage img, int style, int hair, int hairDark, int skinBase) {
        if (style == 0) return;
        if (style == 1) {
            // Light stubble
            int stub = blend(skinBase, hairDark, 0.35f);
            fillRect(img, 9, 13, 1, 2, stub);
            fillRect(img, 14, 13, 1, 2, stub);
            fillRect(img, 10, 15, 4, 1, stub);
        } else if (style == 2) {
            // Goatee + mustache
            fillRect(img, 10, 13, 4, 1, hairDark);
            fillRect(img, 10, 15, 4, 1, hair);
            fillRect(img, 42, 15, 4, 1, hairDark);
        } else {
            // Full beard
            fillRect(img, 8, 13, 2, 3, hairDark);
            fillRect(img, 14, 13, 2, 3, hairDark);
            fillRect(img, 10, 13, 4, 1, hair);
            fillRect(img, 10, 15, 4, 1, hair);
            fillRect(img, 40, 14, 8, 2, hairDark);
        }
    }

    private static void fillBoxSkin(NativeImage img, Random rng, int u, int v, int w, int h, int d, int base) {
        int totalW = d * 2 + w * 2;
        int totalH = d + h;
        for (int py = 0; py < totalH; py++) {
            for (int px = 0; px < totalW; px++) {
                // Skip empty corners of box net
                if (py < d && (px < d || px >= d + w * 2)) continue;
                int jitter = rng.nextInt(9) - 4;
                int col = adjustBrightness(base, jitter);
                setArgb(img, u + px, v + py, col);
            }
        }
    }

    private static void fillShortsBand(NativeImage img, int x, int y, int w, int h, int cMain, int cDark, int cLight) {
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                int col = (py == 0) ? cDark : ((px + py) % 4 == 0 ? cLight : cMain);
                setArgb(img, x + px, y + py, col);
            }
        }
    }

    private static void fillRect(NativeImage img, int x, int y, int w, int h, int argb) {
        for (int py = y; py < y + h; py++) {
            for (int px = x; px < x + w; px++) {
                setArgb(img, px, py, argb);
            }
        }
    }

    private static void setArgb(NativeImage img, int x, int y, int argb) {
        if (x < 0 || x >= 64 || y < 0 || y >= 64) return;
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        int abgr = (a << 24) | (b << 16) | (g << 8) | r;
        img.setPixelColor(x, y, abgr);
    }

    private static int shade(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.max(0, Math.min(255, (int) (((argb >>> 16) & 0xFF) * factor)));
        int g = Math.max(0, Math.min(255, (int) (((argb >>> 8) & 0xFF) * factor)));
        int b = Math.max(0, Math.min(255, (int) ((argb & 0xFF) * factor)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int adjustBrightness(int argb, int delta) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.max(0, Math.min(255, ((argb >>> 16) & 0xFF) + delta));
        int g = Math.max(0, Math.min(255, ((argb >>> 8) & 0xFF) + delta));
        int b = Math.max(0, Math.min(255, (argb & 0xFF) + delta));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blend(int c1, int c2, float t) {
        int a = 0xFF;
        int r = (int) (((c1 >>> 16) & 0xFF) * (1 - t) + ((c2 >>> 16) & 0xFF) * t);
        int g = (int) (((c1 >>> 8) & 0xFF) * (1 - t) + ((c2 >>> 8) & 0xFF) * t);
        int b = (int) ((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static long fnv1a64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }
}
