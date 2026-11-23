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
import net.spacerust.client.building.RustBuildingHudOverlay;
import net.spacerust.client.building.RustBuildingManager;
import net.spacerust.client.gui.RustCraftingScreen;
import net.spacerust.client.gui.RustCraftingQueue;
import net.spacerust.client.gui.RustUiHelper;
import net.spacerust.client.guns.GunManager;
import net.spacerust.client.hud.GunHudOverlay;
import net.spacerust.client.hud.RustCrosshairOverlay;
import net.spacerust.client.hud.RustHudOverlay;
import org.lwjgl.glfw.GLFW;

public class SpaceRustClient implements ClientModInitializer {
    public static final String MOD_ID = "spacerust";
    public static final Identifier UI_CLICK_ID = new Identifier(MOD_ID, "ui_click");
    public static final SoundEvent UI_CLICK_SOUND = new SoundEvent(UI_CLICK_ID);
    public static KeyBinding keyMap;
    public static KeyBinding keyReload;

    @Override
    public void onInitializeClient() {
        System.out.println("[SpaceRust] Initializing RustMe-like Client Engine for Minecraft 1.16.5...");
        Registry.register(Registry.SOUND_EVENT, UI_CLICK_ID, UI_CLICK_SOUND);

        // 1. Register Rust Keybindings (Q remains standard Minecraft item drop)
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

        // 2. Register Custom Rust HUD & Building Overlays
        HudRenderCallback.EVENT.register(new RustHudOverlay());
        HudRenderCallback.EVENT.register(new RustCrosshairOverlay());
        HudRenderCallback.EVENT.register(new GunHudOverlay());
        HudRenderCallback.EVENT.register(new RustBuildingHudOverlay());

        // 3. Register Rust Building World Renderer (models & hologram preview)
        RustBuildingManager.init();
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            RustBuildingManager.renderAll(context.matrixStack(), context.consumers(), context.camera(), context.tickDelta());
        });

        // 4. Register Keybind Tick Check & Building Tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Tick crafting queue in background
            RustCraftingQueue.tick(client);

            // Tick building manager (snapping, hologram, hover, collision)
            RustBuildingManager.tick(client);

            // Map on G
            while (keyMap.wasPressed()) {
                if (client.player != null) {
                    client.player.sendChatMessage("/map");
                }
            }

            // Reload / Rotate on R
            while (keyReload.wasPressed()) {
                if (client.player != null) {
                    if (RustBuildingManager.isHoldingBuildingPlan(client)) {
                        RustBuildingManager.previewYaw = (RustBuildingManager.previewYaw + 90f) % 360f;
                        RustUiHelper.playClick(client);
                    } else {
                        GunManager.GunInfo gun = GunManager.getHeldGun(client);
                        if (gun != null) {
                            GunManager.reload(gun);
                            client.player.sendChatMessage("/reload"); // Server sync if plugin exists
                        }
                    }
                }
            }
        });

        System.out.println("[SpaceRust] Rust Client Engine Loaded Successfully!");
    }
}
