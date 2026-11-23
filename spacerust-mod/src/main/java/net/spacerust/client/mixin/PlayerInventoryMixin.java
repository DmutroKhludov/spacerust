package net.spacerust.client.mixin;

import net.minecraft.entity.player.PlayerInventory;
import net.spacerust.client.gui.RustUiHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    @Shadow
    public int selectedSlot;

    @Inject(method = "scrollInHotbar", at = @At("HEAD"), cancellable = true)
    private void onScrollInHotbar(double scrollAmount, CallbackInfo ci) {
        int i = (int) Math.signum(scrollAmount);
        this.selectedSlot -= i;
        while (this.selectedSlot < 0) {
            this.selectedSlot += RustUiHelper.HOTBAR_SLOTS;
        }
        while (this.selectedSlot >= RustUiHelper.HOTBAR_SLOTS) {
            this.selectedSlot -= RustUiHelper.HOTBAR_SLOTS;
        }
        ci.cancel();
    }
}
