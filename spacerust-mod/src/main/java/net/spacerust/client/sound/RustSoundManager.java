package net.spacerust.client.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

public class RustSoundManager {
    private static final Map<String, SoundEvent> SOUND_CACHE = new HashMap<>();

    public static SoundEvent getSound(String name) {
        if (!SOUND_CACHE.containsKey(name)) {
            Identifier id = new Identifier("spacerust", name);
            SOUND_CACHE.put(name, new SoundEvent(id));
        }
        return SOUND_CACHE.get(name);
    }

    public static void play(String name) {
        play(name, 1.0f, 1.0f);
    }

    public static void play(String name, float volume, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            SoundEvent event = getSound(name);
            client.getSoundManager().play(PositionedSoundInstance.master(event, pitch, volume));
        }
    }

    public static void playAt(String name, double x, double y, double z, float volume, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            SoundEvent event = getSound(name);
            client.getSoundManager().play(new PositionedSoundInstance(
                    event.getId(),
                    net.minecraft.sound.SoundCategory.PLAYERS,
                    volume, pitch, false, 0,
                    PositionedSoundInstance.AttenuationType.LINEAR,
                    x, y, z, false
            ));
        }
    }
}
