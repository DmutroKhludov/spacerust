package net.spacerust.client.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    // Hide vanilla hearts, food bar and armor
    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void hideVanillaStatusBars(MatrixStack matrices, CallbackInfo ci) {
        ci.cancel();
    }

    // Hide vanilla 9-slot hotbar in favor of Rust 5-slot belt HUD
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void hideVanillaHotbar(float tickDelta, MatrixStack matrices, CallbackInfo ci) {
        ci.cancel();
    }

    // Hide vanilla green XP bar
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void hideVanillaXpBar(MatrixStack matrices, int x, CallbackInfo ci) {
        ci.cancel();
    }

    // Hide vanilla mount jump bar
    @Inject(method = "renderMountJumpBar", at = @At("HEAD"), cancellable = true)
    private void hideVanillaMountBar(MatrixStack matrices, int x, CallbackInfo ci) {
        ci.cancel();
    }

    // Hide vanilla static crosshair in favor of Rust tactical dynamic crosshair
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void hideVanillaCrosshair(MatrixStack matrices, CallbackInfo ci) {
        ci.cancel();
    }
}
