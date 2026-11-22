package net.spacerust.client.mixin;

import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "calculateScaleFactor", at = @At("HEAD"), cancellable = true)
    private void lockScaleFactor(int guiScale, boolean forceUnicodeFont, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(2); // Lock GUI scale permanently to 2
    }
}
