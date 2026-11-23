package net.spacerust.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.spacerust.client.gui.RustSettingsScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public class OptionsScreenMixin {

    @Shadow
    @Final
    private Screen parent;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void openRustSettingsScreen(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && !(client.currentScreen instanceof RustSettingsScreen)) {
            client.openScreen(new RustSettingsScreen(this.parent));
            ci.cancel();
        }
    }
}
