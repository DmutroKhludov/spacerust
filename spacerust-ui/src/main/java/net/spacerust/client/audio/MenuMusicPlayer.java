package net.spacerust.client.audio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.spacerust.client.SpaceRustClient;
import net.spacerust.client.config.RustSettings;
import net.spacerust.client.gui.RustServerBrowserScreen;

/**
 * Plays SpaceRust soundtrack in Main Menu and In-Game.
 */
public final class MenuMusicPlayer {

    private static RustMusicSound activeInstance;
    private static int activeTrack = -1;
    private static boolean activeModeInGame = false;
    private static int inGameIndex = 0;
    private static int inGameCooldownTicks = 60;

    private MenuMusicPlayer() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.getSoundManager() == null) {
            return;
        }
        RustSettings cfg = RustSettings.get();
        float musicVol = client.options.getSoundVolume(SoundCategory.MUSIC) * client.options.getSoundVolume(SoundCategory.MASTER);

        if (client.getMusicTracker() != null) {
            client.getMusicTracker().stop();
        }

        boolean inWorld = (client.world != null && client.player != null);
        boolean inMenu = !inWorld && (
                client.currentScreen instanceof RustServerBrowserScreen
                        || client.currentScreen instanceof KeybindsScreen
                        || client.currentScreen instanceof LanguageOptionsScreen
        );

        SoundManager sm = client.getSoundManager();

        if (inMenu) {
            inGameCooldownTicks = 60;
            boolean shouldPlay = cfg.menuMusicEnabled && musicVol > 0.001f;
            if (!shouldPlay) {
                stop(client);
                return;
            }
            if (activeInstance == null || activeInstance.isDone() || !sm.isPlaying(activeInstance)
                    || activeModeInGame || activeTrack != cfg.menuMusicTrack) {
                stop(client);
                activeModeInGame = false;
                activeTrack = cfg.menuMusicTrack;
                SoundEvent ev = (activeTrack == 1)
                        ? SpaceRustClient.MENU_MUSIC_SAFE_ZONE_SOUND
                        : SpaceRustClient.MENU_MUSIC_CLASSIC_SOUND;
                activeInstance = new RustMusicSound(ev, true, 0.65f, false);
                sm.play(activeInstance);
            }
            return;
        }

        if (inWorld) {
            boolean shouldPlay = cfg.inGameMusicEnabled && musicVol > 0.001f;
            if (!shouldPlay) {
                stop(client);
                return;
            }
            if (activeInstance != null && !activeModeInGame) {
                stop(client);
            }
            if (activeInstance != null && !activeInstance.isDone() && sm.isPlaying(activeInstance)) {
                return;
            }
            if (activeInstance != null) {
                activeInstance = null;
                inGameCooldownTicks = 240;
            }
            if (inGameCooldownTicks > 0) {
                inGameCooldownTicks--;
                return;
            }
            activeModeInGame = true;
            activeTrack = inGameIndex;
            SoundEvent ev = (inGameIndex % 2 == 0)
                    ? SpaceRustClient.MENU_MUSIC_SAFE_ZONE_SOUND
                    : SpaceRustClient.MENU_MUSIC_CLASSIC_SOUND;
            inGameIndex = (inGameIndex + 1) % 2;
            activeInstance = new RustMusicSound(ev, false, 0.55f, true);
            sm.play(activeInstance);
            return;
        }

        stop(client);
    }

    public static void stop(MinecraftClient client) {
        if (activeInstance != null) {
            activeInstance.stopNow();
            if (client != null && client.getSoundManager() != null) {
                client.getSoundManager().stop(activeInstance);
            }
            activeInstance = null;
        }
        activeTrack = -1;
    }

    private static class RustMusicSound extends MovingSoundInstance {
        private final boolean expectInWorld;

        RustMusicSound(SoundEvent sound, boolean repeat, float volume, boolean expectInWorld) {
            super(sound, SoundCategory.MUSIC, SoundInstance.createRandom());
            this.expectInWorld = expectInWorld;
            this.repeat = repeat;
            this.repeatDelay = 0;
            this.volume = volume;
            this.pitch = 1.0f;
            this.attenuationType = AttenuationType.NONE;
            this.relative = true;
        }

        @Override
        public void tick() {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                this.setDone();
                return;
            }
            boolean inWorld = (client.world != null && client.player != null);
            if (expectInWorld) {
                if (!inWorld || !RustSettings.get().inGameMusicEnabled) {
                    this.setDone();
                }
            } else {
                if (inWorld || !RustSettings.get().menuMusicEnabled) {
                    this.setDone();
                }
            }
        }

        void stopNow() {
            this.setDone();
        }
    }
}
