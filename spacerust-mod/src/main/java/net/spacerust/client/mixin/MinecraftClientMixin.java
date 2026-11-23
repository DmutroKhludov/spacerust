package net.spacerust.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.spacerust.client.building.RustBuildingManager;
import net.spacerust.client.building.RustBuildingPlanScreen;
import net.spacerust.client.building.RustHammerScreen;
import net.spacerust.client.deployables.RustDeployableManager;
import net.spacerust.client.guns.RustShootingManager;
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

        // 1. If holding Lock, prioritize attaching lock to hovered door or box
        if (RustDeployableManager.heldDeployableType == net.spacerust.client.deployables.RustDeployableType.KEY_LOCK ||
            RustDeployableManager.heldDeployableType == net.spacerust.client.deployables.RustDeployableType.CODE_LOCK) {
            if (RustDeployableManager.tryPlaceHeld(client)) {
                ci.cancel();
                return;
            }
        }

        // 2. Interact with Doors, Locks, Tool Cupboard, Boxes
        if (RustDeployableManager.hoveredDeployable != null) {
            RustDeployableManager.interact(client);
            ci.cancel();
            return;
        }

        // 3. Place held Deployables (Doors, TC, Boxes)
        if (RustDeployableManager.heldDeployableType != null) {
            if (RustDeployableManager.tryPlaceHeld(client)) {
                ci.cancel();
                return;
            } else if (RustDeployableManager.previewPos != null) {
                ci.cancel();
                return;
            }
        }

        // 3. Building Plan & Hammer Radial Wheels
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

        // 1. Rust Gun Shooting
        if (RustShootingManager.getHeldWeapon(client) != null) {
            RustShootingManager.onAttackPressed(client);
            ci.cancel();
            return;
        }

        // 2. Place held Deployables
        if (RustDeployableManager.heldDeployableType != null) {
            if (RustDeployableManager.tryPlaceHeld(client)) {
                ci.cancel();
                return;
            } else if (RustDeployableManager.previewPos != null) {
                ci.cancel();
                return;
            }
        }

        // 3. Building Plan Placement & Hammer Repair
        if (RustBuildingManager.isHoldingBuildingPlan(client)) {
            if (RustBuildingManager.tryPlaceCurrentPreview(client)) {
                ci.cancel();
            } else if (RustBuildingManager.previewPos != null) {
                ci.cancel();
            }
        } else if (RustBuildingManager.isHoldingHammer(client) && RustBuildingManager.hoveredBuilding != null) {
            RustBuildingManager.repairPiece(client, RustBuildingManager.hoveredBuilding);
            ci.cancel();
        }
    }
}

