package net.spacerust.client.config;

import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Persistent CoilRust-compatible client & voice settings saved in config/spacerust_settings.properties.
 */
public final class RustSettings {

    private static final RustSettings INSTANCE = new RustSettings();

    // ИГРОВОЙ ПРОЦЕСС & ЗВУК
    public boolean showHitmarker = true;
    public float explosionShake = 0.25f;
    public boolean menuMusicEnabled = true;
    public int menuMusicTrack = 0; // 0 = Classic, 1 = Safe Zone
    public boolean inGameMusicEnabled = true;
    public float uiVolume = 0.45f; // 0.0 .. 1.0 (UI click/hover volume)

    // ЭКРАН
    public int resolutionIndex = 0; // 0 = Текущее, 1 = 800x600, 2 = 1280x720, 3 = 1600x900, 4 = 1920x1080

    // ГРАФИКА
    public int grassMode = 2; // 0 = ВЫКЛ, 1 = БЫСТРАЯ, 2 = ГУСТАЯ

    // ЭФФЕКТЫ ИЗОБРАЖЕНИЯ
    public float saturation = 1.0f;
    public boolean postProcessing = true;
    public boolean vignette = true;
    public float vignetteStrength = 0.0f;
    public boolean colorCorrection = true;
    public float colorCorrectionStrength = 0.0f;
    public boolean expFog = true;
    public boolean modelLighting = true;
    public int worldStyle = 2; // 0 = Производительность, 1 = Баланс, 2 = Качество
    public boolean wind = true;
    public boolean showFps = true;

    // ИНТЕРФЕЙС
    public int guiScaleOverride = 2; // 0 = АВТО, 1 = МАЛЕНЬКИЙ, 2 = СРЕДНИЙ, 3 = БОЛЬШОЙ

    // VOICE CHAT
    public boolean voiceEnabled = true;
    public String voiceInputDevice = "";
    public String voiceOutputDevice = "";
    public float voicePlayerVolume = 1.0f;
    public float voiceMicGain = 1.0f;
    public float voiceNoiseGate = 0.012f;

    private boolean loaded = false;

    private RustSettings() {
    }

    public static RustSettings get() {
        if (!INSTANCE.loaded) {
            INSTANCE.load();
        }
        return INSTANCE;
    }

    private File getConfigFile() {
        MinecraftClient client = MinecraftClient.getInstance();
        File base = (client != null && client.runDirectory != null) ? client.runDirectory : new File(".");
        File cfgDir = new File(base, "config");
        if (!cfgDir.exists()) {
            cfgDir.mkdirs();
        }
        return new File(cfgDir, "spacerust_settings.properties");
    }

    public synchronized void load() {
        loaded = true;
        File file = getConfigFile();
        if (!file.isFile()) {
            return;
        }
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
            showHitmarker = getBool(props, "showHitmarker", showHitmarker);
            explosionShake = clamp(getFloat(props, "explosionShake", explosionShake), 0f, 1f);
            menuMusicEnabled = getBool(props, "menuMusicEnabled", menuMusicEnabled);
            menuMusicTrack = Math.max(0, Math.min(1, getInt(props, "menuMusicTrack", menuMusicTrack)));
            inGameMusicEnabled = getBool(props, "inGameMusicEnabled", inGameMusicEnabled);
            uiVolume = clamp(getFloat(props, "uiVolume", uiVolume), 0f, 1f);
            resolutionIndex = Math.max(0, Math.min(4, getInt(props, "resolutionIndex", resolutionIndex)));
            grassMode = Math.max(0, Math.min(2, getInt(props, "grassMode", grassMode)));
            saturation = clamp(getFloat(props, "saturation", saturation), 0f, 2f);
            postProcessing = getBool(props, "postProcessing", postProcessing);
            vignette = getBool(props, "vignette", vignette);
            vignetteStrength = clamp(getFloat(props, "vignetteStrength", vignetteStrength), 0f, 1f);
            colorCorrection = getBool(props, "colorCorrection", colorCorrection);
            colorCorrectionStrength = clamp(getFloat(props, "colorCorrectionStrength", colorCorrectionStrength), 0f, 1f);
            expFog = getBool(props, "expFog", expFog);
            modelLighting = getBool(props, "modelLighting", modelLighting);
            worldStyle = Math.max(0, Math.min(2, getInt(props, "worldStyle", worldStyle)));
            wind = getBool(props, "wind", wind);
            showFps = getBool(props, "showFps", showFps);
            guiScaleOverride = Math.max(0, Math.min(3, getInt(props, "guiScaleOverride", guiScaleOverride)));
            voiceEnabled = getBool(props, "voiceEnabled", voiceEnabled);
            voiceInputDevice = props.getProperty("voiceInputDevice", "");
            voiceOutputDevice = props.getProperty("voiceOutputDevice", "");
            voicePlayerVolume = clamp(getFloat(props, "voicePlayerVolume", voicePlayerVolume), 0f, 2f);
            voiceMicGain = clamp(getFloat(props, "voiceMicGain", voiceMicGain), 0.2f, 3f);
            voiceNoiseGate = clamp(getFloat(props, "voiceNoiseGate", voiceNoiseGate), 0f, 0.12f);
        } catch (Exception ignored) {
        }
    }

    public synchronized void save() {
        File file = getConfigFile();
        Properties props = new Properties();
        props.setProperty("showHitmarker", String.valueOf(showHitmarker));
        props.setProperty("explosionShake", String.valueOf(explosionShake));
        props.setProperty("menuMusicEnabled", String.valueOf(menuMusicEnabled));
        props.setProperty("menuMusicTrack", String.valueOf(menuMusicTrack));
        props.setProperty("inGameMusicEnabled", String.valueOf(inGameMusicEnabled));
        props.setProperty("uiVolume", String.valueOf(uiVolume));
        props.setProperty("resolutionIndex", String.valueOf(resolutionIndex));
        props.setProperty("grassMode", String.valueOf(grassMode));
        props.setProperty("saturation", String.valueOf(saturation));
        props.setProperty("postProcessing", String.valueOf(postProcessing));
        props.setProperty("vignette", String.valueOf(vignette));
        props.setProperty("vignetteStrength", String.valueOf(vignetteStrength));
        props.setProperty("colorCorrection", String.valueOf(colorCorrection));
        props.setProperty("colorCorrectionStrength", String.valueOf(colorCorrectionStrength));
        props.setProperty("expFog", String.valueOf(expFog));
        props.setProperty("modelLighting", String.valueOf(modelLighting));
        props.setProperty("worldStyle", String.valueOf(worldStyle));
        props.setProperty("wind", String.valueOf(wind));
        props.setProperty("showFps", String.valueOf(showFps));
        props.setProperty("guiScaleOverride", String.valueOf(guiScaleOverride));
        props.setProperty("voiceEnabled", String.valueOf(voiceEnabled));
        props.setProperty("voiceInputDevice", voiceInputDevice == null ? "" : voiceInputDevice);
        props.setProperty("voiceOutputDevice", voiceOutputDevice == null ? "" : voiceOutputDevice);
        props.setProperty("voicePlayerVolume", String.valueOf(voicePlayerVolume));
        props.setProperty("voiceMicGain", String.valueOf(voiceMicGain));
        props.setProperty("voiceNoiseGate", String.valueOf(voiceNoiseGate));
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "SpaceRust CoilRust-style settings");
        } catch (Exception ignored) {
        }
    }

    private static boolean getBool(Properties p, String k, boolean def) {
        String v = p.getProperty(k);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }

    private static int getInt(Properties p, String k, int def) {
        String v = p.getProperty(k);
        if (v == null) return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static float getFloat(Properties p, String k, float def) {
        String v = p.getProperty(k);
        if (v == null) return def;
        try {
            return Float.parseFloat(v.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
