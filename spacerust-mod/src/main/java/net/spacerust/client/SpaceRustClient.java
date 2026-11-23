package net.spacerust.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.spacerust.client.audio.MenuMusicPlayer;
import net.spacerust.client.config.RustSettings;
import net.spacerust.client.deployables.RustDeployableManager;
import net.spacerust.client.gui.RustCraftingQueue;
import net.spacerust.client.gui.RustMapScreen;
import net.spacerust.client.guns.GunManager;
import net.spacerust.client.guns.RustRecoilManager;
import net.spacerust.client.guns.RustShootingManager;
import net.spacerust.client.guns.RustViewModelAnimator;
import net.spacerust.client.hud.GunHudOverlay;
import net.spacerust.client.hud.RustCrosshairOverlay;
import net.spacerust.client.hud.RustHudOverlay;
import net.spacerust.client.voice.RustVoiceEngine;
import net.spacerust.client.world.OreNodeRenderer;
import net.spacerust.client.world.RustBuildingRenderer;
import net.spacerust.client.world.RustBuildingSystem;
import net.spacerust.client.world.RustCrateRenderer;
import net.spacerust.client.world.RustTreeRenderer;
import net.spacerust.client.world.RustWorldCollider;
import net.spacerust.client.world.RustWorldSync;
import org.lwjgl.glfw.GLFW;

public class SpaceRustClient implements ClientModInitializer {
    public static final String MOD_ID = "spacerust";

    public static final Identifier UI_CLICK_ID = new Identifier(MOD_ID, "ui_click");
    public static final SoundEvent UI_CLICK_SOUND = new SoundEvent(UI_CLICK_ID);

    public static final Identifier UI_HOVER_ID = new Identifier(MOD_ID, "ui_hover");
    public static final SoundEvent UI_HOVER_SOUND = new SoundEvent(UI_HOVER_ID);

    public static final Identifier MENU_MUSIC_CLASSIC_ID = new Identifier(MOD_ID, "menu_music_classic");
    public static final SoundEvent MENU_MUSIC_CLASSIC_SOUND = new SoundEvent(MENU_MUSIC_CLASSIC_ID);

    public static final Identifier MENU_MUSIC_SAFE_ZONE_ID = new Identifier(MOD_ID, "menu_music_safe_zone");
    public static final SoundEvent MENU_MUSIC_SAFE_ZONE_SOUND = new SoundEvent(MENU_MUSIC_SAFE_ZONE_ID);

    public static final java.util.Map<String, SoundEvent> WEAPON_SOUNDS = new java.util.HashMap<>();
    private static final String[] WEAPON_SOUND_KEYS = {
            "ak_shoot", "ak_reload", "ak_deploy",
            "revolver_shoot", "revolver_reload", "revolver_deploy",
            "shotgun_shoot", "shotgun_reload", "shotgun_deploy",
            "thompson_shoot", "thompson_reload", "thompson_deploy",
            "bolt_shoot", "bolt_reload", "bolt_deploy",
            "dry_fire", "rock_deploy", "axe_deploy", "pickaxe_deploy", "hammer_deploy",
            "syringe_deploy", "syringe_use", "bandage_deploy", "bandage_use",
            "torch_deploy", "torch_ignite", "torch_extinguish", "melee_swing", "hitmarker", "headshot",
            "ore_hit", "ore_hotspot_1", "ore_hotspot_2", "ore_break",
            "tree_hit", "tree_chop_1", "tree_chop_2", "tree_bonus_1", "tree_bonus_2", "tree_spray", "tree_fall",
            "build_place", "build_wood_upgrade", "build_stone_upgrade",
            "build_metal_upgrade", "build_hqm_upgrade", "build_destroy", "build_repair",
            "mp5_shoot", "mp5_reload", "lr300_shoot", "lr300_reload",
            "l96_shoot", "l96_reload", "python_shoot", "python_reload",
            "pump_shoot", "pump_reload", "m92_shoot", "m92_reload",
            "p250_shoot", "p250_reload", "sar_shoot", "sar_reload",
            "customsmg_shoot", "customsmg_reload",
            "crossbow_shoot", "crossbow_reload", "bow_shoot", "bow_draw_start",
            "rocket_shoot", "rocket_reload", "rocket_deploy"
    };

    public static KeyBinding keyMap;
    public static KeyBinding keyReload;
    public static KeyBinding keyVoice;
    public static KeyBinding keyQuickLoot;
    public static volatile boolean hideGuiHeldItem = false;

    @Override
    public void onInitializeClient() {
        System.out.println("[SpaceRust] Initializing Rust/CoilRust Client Engine for Minecraft 1.16.5...");
        RustSettings.get();
        SpaceRustItems.register();

        Registry.register(Registry.SOUND_EVENT, UI_CLICK_ID, UI_CLICK_SOUND);
        Registry.register(Registry.SOUND_EVENT, UI_HOVER_ID, UI_HOVER_SOUND);
        Registry.register(Registry.SOUND_EVENT, MENU_MUSIC_CLASSIC_ID, MENU_MUSIC_CLASSIC_SOUND);
        Registry.register(Registry.SOUND_EVENT, MENU_MUSIC_SAFE_ZONE_ID, MENU_MUSIC_SAFE_ZONE_SOUND);
        for (String key : WEAPON_SOUND_KEYS) {
            Identifier sid = new Identifier(MOD_ID, key);
            SoundEvent ev = new SoundEvent(sid);
            Registry.register(Registry.SOUND_EVENT, sid, ev);
            WEAPON_SOUNDS.put(key, ev);
        }

        // 1. Register Rust Keybindings (Q remains standard craft trigger via KeyboardMixin)
        keyMap = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.spacerust.map",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.spacerust"
        ));

        keyReload = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.spacerust.reload",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.spacerust"
        ));

        keyVoice = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.spacerust.voice",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.spacerust"
        ));

        keyQuickLoot = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.spacerust.quickloot",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.spacerust"
        ));

        // 2. Register Custom Rust HUD Overlays, Voice Chat & World Systems
        HudRenderCallback.EVENT.register(new RustHudOverlay());
        HudRenderCallback.EVENT.register(new RustCrosshairOverlay());
        HudRenderCallback.EVENT.register(new GunHudOverlay());
        RustWorldSync.register();
        OreNodeRenderer.register();
        RustBuildingRenderer.register();
        RustTreeRenderer.register();
        RustCrateRenderer.register();
        RustVoiceEngine.get().register();
        RustDeployableManager.init();
        RustRecoilManager.init();

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            RustDeployableManager.renderAll(context.matrixStack(), context.consumers(), context.camera(), context.tickDelta());
        });

        // 3. Register Keybind & Menu Music Tick Check
        final int[] prevHotbarSlot = {0};
        final long[] rHoldStartMs = {0L};
        final boolean[] rOpenedWheel = {false};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Loop CoilRust music cleanly in main menu and in-game
            MenuMusicPlayer.tick(client);

            // Tick crafting queue & weapon/tool animations
            RustCraftingQueue.tick(client);
            RustViewModelAnimator.tick(client);

            // Support continuous mining / chopping by holding LMB on 3D Ore Nodes & Rust Trees (never steal animal hits)
            if (client.currentScreen == null && client.player != null && !client.player.isSpectator()
                    && client.options.keyAttack.isPressed() && GunManager.getHeldGun(client) == null
                    && (client.crosshairTarget == null || client.crosshairTarget.getType() != net.minecraft.util.hit.HitResult.Type.ENTITY)) {
                RustWorldCollider.tryRaycastFarmHit(client, true);
            }

            // Clamp Rust hotbar strictly to 6 slots (0..5) with seamless scroll wrap
            if (client.player != null && !client.player.isCreative()) {
                int s = client.player.inventory.selectedSlot;
                if (s > 5) {
                    if (prevHotbarSlot[0] == 0 && s == 8) {
                        s = 5;
                    } else if (prevHotbarSlot[0] == 5 && s == 6) {
                        s = 0;
                    } else {
                        s = Math.min(5, Math.max(0, s));
                    }
                    client.player.inventory.selectedSlot = s;
                }
                prevHotbarSlot[0] = client.player.inventory.selectedSlot;
            }

            // Tick deployables manager (doors, locks, TC, boxes, decay, interaction)
            RustDeployableManager.tick(client);

            // Tick shooting manager (continuous auto-fire, recoil recovery, ADS)
            RustShootingManager.tick(client);
            if (!client.options.keyAttack.isPressed()) {
                RustShootingManager.onAttackReleased();
            }

            // Map on G
            while (keyMap.wasPressed()) {
                if (client.player != null) {
                    if (client.currentScreen instanceof RustMapScreen) {
                        client.openScreen(null);
                    } else if (client.currentScreen == null) {
                        client.openScreen(new RustMapScreen());
                    }
                }
            }

            // Hold R (>220ms) for Ammo Wheel, quick tap R to reload / rotate blueprint
            while (keyReload.wasPressed()) {
                if (client.player != null) {
                    if (RustBuildingSystem.isHoldingPlan(client)) {
                        RustBuildingSystem.rotateBlueprint(client);
                    } else if (GunManager.getHeldGun(client) != null) {
                        rHoldStartMs[0] = System.currentTimeMillis();
                        rOpenedWheel[0] = false;
                    } else if (RustShootingManager.getHeldWeapon(client) != null) {
                        RustShootingManager.reload(client);
                    }
                }
            }
            if (client.player != null && rHoldStartMs[0] > 0L) {
                GunManager.GunInfo heldGun = GunManager.getHeldGun(client);
                if (keyReload.isPressed() && !rOpenedWheel[0] && heldGun != null
                        && System.currentTimeMillis() - rHoldStartMs[0] >= 220L
                        && client.currentScreen == null) {
                    rOpenedWheel[0] = true;
                    client.openScreen(new net.spacerust.client.gui.RustAmmoWheelScreen(heldGun));
                } else if (!keyReload.isPressed()) {
                    if (!rOpenedWheel[0] && heldGun != null) {
                        RustViewModelAnimator.startReload(client, heldGun);
                    }
                    rHoldStartMs[0] = 0L;
                    rOpenedWheel[0] = false;
                }
            }
        });
    }
}
