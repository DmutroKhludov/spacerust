package net.spacerust.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.spacerust.client.gui.RustCraftingScreen;
import net.spacerust.client.gui.RustCraftingQueue;
import net.spacerust.client.guns.GunManager;
import net.spacerust.client.hud.GunHudOverlay;
import net.spacerust.client.hud.RustCrosshairOverlay;
import net.spacerust.client.hud.RustHudOverlay;
import org.lwjgl.glfw.GLFW;

public class SpaceRustClient implements ClientModInitializer {
    public static final String MOD_ID = "spacerust";
    public static KeyBinding keyMap;
    public static KeyBinding keyReload;

    @Override
    public void onInitializeClient() {
        System.out.println("[SpaceRust] Initializing RustMe-like Client Engine for Minecraft 1.16.5...");

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

        // 2. Register Custom Rust HUD Overlays
        HudRenderCallback.EVENT.register(new RustHudOverlay());
        HudRenderCallback.EVENT.register(new RustCrosshairOverlay());
        HudRenderCallback.EVENT.register(new GunHudOverlay());

        // 3. Register Keybind Tick Check
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Tick crafting queue in background
            RustCraftingQueue.tick(client);

            // Map on G
            while (keyMap.wasPressed()) {
                if (client.player != null) {
                    client.player.sendChatMessage("/map");
                }
            }

            // Reload on R
            while (keyReload.wasPressed()) {
                if (client.player != null) {
                    GunManager.GunInfo gun = GunManager.getHeldGun(client);
                    if (gun != null) {
                        GunManager.reload(gun);
                        client.player.sendChatMessage("/reload"); // Server sync if plugin exists
                    }
                }
            }
        });

        System.out.println("[SpaceRust] Rust Client Engine Loaded Successfully!");
    }
}
