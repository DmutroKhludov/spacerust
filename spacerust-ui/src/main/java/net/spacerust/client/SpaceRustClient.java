package net.spacerust.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.spacerust.client.audio.MenuMusicPlayer;
import net.spacerust.client.config.RustSettings;

public class SpaceRustClient implements ClientModInitializer {
    public static final String MOD_ID = "spacerust";

    public static final Identifier UI_CLICK_ID = Identifier.of(MOD_ID, "ui_click");
    public static final SoundEvent UI_CLICK_SOUND = SoundEvent.of(UI_CLICK_ID);

    public static final Identifier UI_HOVER_ID = Identifier.of(MOD_ID, "ui_hover");
    public static final SoundEvent UI_HOVER_SOUND = SoundEvent.of(UI_HOVER_ID);

    public static final Identifier MENU_MUSIC_CLASSIC_ID = Identifier.of(MOD_ID, "menu_music_classic");
    public static final SoundEvent MENU_MUSIC_CLASSIC_SOUND = SoundEvent.of(MENU_MUSIC_CLASSIC_ID);

    public static final Identifier MENU_MUSIC_SAFE_ZONE_ID = Identifier.of(MOD_ID, "menu_music_safe_zone");
    public static final SoundEvent MENU_MUSIC_SAFE_ZONE_SOUND = SoundEvent.of(MENU_MUSIC_SAFE_ZONE_ID);

    public static boolean allowVanillaOptionsOnce = false;
    public static boolean allowVanillaPauseOnce = false;

    @Override
    public void onInitializeClient() {
        System.out.println("[SpaceRustUI] Initializing SpaceRust UI for Minecraft 1.21.11...");
        RustSettings.get();

        Registry.register(Registries.SOUND_EVENT, UI_CLICK_ID, UI_CLICK_SOUND);
        Registry.register(Registries.SOUND_EVENT, UI_HOVER_ID, UI_HOVER_SOUND);
        Registry.register(Registries.SOUND_EVENT, MENU_MUSIC_CLASSIC_ID, MENU_MUSIC_CLASSIC_SOUND);
        Registry.register(Registries.SOUND_EVENT, MENU_MUSIC_SAFE_ZONE_ID, MENU_MUSIC_SAFE_ZONE_SOUND);

        ClientTickEvents.END_CLIENT_TICK.register(MenuMusicPlayer::tick);
    }
}
