package net.spacerust.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.spacerust.client.building.RustBuildingManager;
import net.spacerust.client.building.RustBuildingPlanScreen;
import net.spacerust.client.building.RustHammerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void onDoItemUse(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.player == null) return;

        if (RustBuildingManager.isHoldingBuildingPlan(client)) {
            client.openScreen(new RustBuildingPlanScreen());
            ci.cancel();
        } else if (RustBuildingManager.isHoldingHammer(client) && RustBuildingManager.hoveredBuilding != null) {
            client.openScreen(new RustHammerScreen(RustBuildingManager.hoveredBuilding));
            ci.cancel();
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.player == null) return;

        if (RustBuildingManager.isHoldingBuildingPlan(client)) {
            if (RustBuildingManager.tryPlaceCurrentPreview(client)) {
                ci.cancel();
            }
        } else if (RustBuildingManager.isHoldingHammer(client) && RustBuildingManager.hoveredBuilding != null) {
            RustBuildingManager.repairPiece(client, RustBuildingManager.hoveredBuilding);
            ci.cancel();
        }
    }
}
