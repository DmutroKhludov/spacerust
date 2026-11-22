package net.spacerust.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.spacerust.client.gui.RustServerBrowserScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void redirectToRustBrowser(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && !(client.currentScreen instanceof RustServerBrowserScreen)) {
            RustServerBrowserScreen.currentTab = RustServerBrowserScreen.MenuTab.HOME;
            client.openScreen(new RustServerBrowserScreen());
            ci.cancel();
        }
    }
}
