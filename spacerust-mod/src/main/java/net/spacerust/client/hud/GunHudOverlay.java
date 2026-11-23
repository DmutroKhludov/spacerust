package net.spacerust.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Disabled floating ammo widget: Rust shows ammo directly on active weapon slots in the belt.
 */
public class GunHudOverlay implements HudRenderCallback {
    @Override
    public void onHudRender(MatrixStack matrices, float tickDelta) {
        // Intentionally blank: ammo count renders on active hotbar weapon slots only.
    }
}
