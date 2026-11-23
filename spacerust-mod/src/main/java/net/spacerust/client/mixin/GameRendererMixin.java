package net.spacerust.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.spacerust.client.guns.RustViewModelAnimator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void applyRustAdsZoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (changingFov) {
            float zoom = RustViewModelAnimator.getAdsFovZoom(this.client);
            if (zoom > 0.05f) {
                cir.setReturnValue(Math.max(20.0, cir.getReturnValue() - zoom));
            }
        }
    }
}
