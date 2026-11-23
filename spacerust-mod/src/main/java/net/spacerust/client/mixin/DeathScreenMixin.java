package net.spacerust.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.Text;
import net.spacerust.client.gui.RustDeathScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces vanilla DeathScreen with Rust Death & Respawn Map Screen.
 */
@Mixin(DeathScreen.class)
public class DeathScreenMixin {

    @Shadow
    @Final
    private Text message;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void openRustDeathScreen(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && !(mc.currentScreen instanceof RustDeathScreen)) {
            mc.openScreen(new RustDeathScreen(this.message));
            ci.cancel();
        }
    }
}
