package net.spacerust.client.mixin;

import net.minecraft.client.sound.MusicTracker;
import net.minecraft.sound.MusicSound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicTracker.class)
public abstract class MusicTrackerMixin {

    @Shadow
    public abstract void stop();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void replaceVanillaMusicWithRust(CallbackInfo ci) {
        this.stop();
        ci.cancel();
    }

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaPlay(MusicSound type, CallbackInfo ci) {
        this.stop();
        ci.cancel();
    }
}
