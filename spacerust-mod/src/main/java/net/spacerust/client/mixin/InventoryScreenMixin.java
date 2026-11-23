package net.spacerust.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.spacerust.client.SpaceRustClient;
import net.spacerust.client.gui.RustInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void replaceWithRustInventory(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null && !client.player.isCreative()) {
            client.openScreen(new RustInventoryScreen());
            ci.cancel();
        }
    }

    @Inject(method = "drawEntity", at = @At("HEAD"))
    private static void hideHeldItemBegin(int x, int y, int size, float mouseX, float mouseY,
                                          LivingEntity entity, CallbackInfo ci) {
        SpaceRustClient.hideGuiHeldItem = true;
    }

    @Inject(method = "drawEntity", at = @At("RETURN"))
    private static void hideHeldItemEnd(int x, int y, int size, float mouseX, float mouseY,
                                        LivingEntity entity, CallbackInfo ci) {
        SpaceRustClient.hideGuiHeldItem = false;
    }
}
