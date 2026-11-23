package net.spacerust.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.spacerust.client.SpaceRustClient;
import net.spacerust.client.gui.RustPauseScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redirects Escape pause screen to RustPauseScreen unless explicitly bypassed.
 */
@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {

    @Shadow
    @Final
    private boolean showMenu;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void openRustPauseMenu(CallbackInfo ci) {
        if (!this.showMenu) return;
        if (SpaceRustClient.allowVanillaPauseOnce) {
            SpaceRustClient.allowVanillaPauseOnce = false;
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && !(mc.currentScreen instanceof RustPauseScreen)) {
            mc.setScreen(new RustPauseScreen());
            ci.cancel();
        }
    }
}
