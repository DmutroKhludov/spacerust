package net.spacerust.client.mixin;

import net.minecraft.client.util.Window;
import net.spacerust.client.config.RustSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "calculateScaleFactor", at = @At("HEAD"), cancellable = true)
    private void applyRustGuiScale(int guiScale, boolean forceUnicodeFont, CallbackInfoReturnable<Integer> cir) {
        int overrideScale = RustSettings.get().guiScaleOverride;
        if (overrideScale > 0) {
            cir.setReturnValue(overrideScale);
        }
    }
}
