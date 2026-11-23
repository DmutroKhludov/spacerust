package net.spacerust.client.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.spacerust.client.SpaceRustItems;
import net.spacerust.client.deployables.RustDeployableManager;
import net.spacerust.client.deployables.RustDeployableType;
import net.spacerust.client.gui.RustBuildWheelScreen;
import net.spacerust.client.gui.RustLootScreen;
import net.spacerust.client.guns.GunManager;
import net.spacerust.client.guns.RustShootingManager;
import net.spacerust.client.guns.RustViewModelAnimator;
import net.spacerust.client.world.RustBuildingSystem;
import net.spacerust.client.world.RustWorldCollider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @ModifyVariable(method = "openScreen", at = @At("HEAD"), argsOnly = true)
    private Screen replaceGenericContainerWithRustLoot(Screen screen) {
        if (screen instanceof GenericContainerScreen && !(screen instanceof RustLootScreen)) {
            GenericContainerScreen gcs = (GenericContainerScreen) screen;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.player != null) {
                return new RustLootScreen(gcs.getScreenHandler(), mc.player.inventory, gcs.getTitle());
            }
        }
        return screen;
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onRustAttack(CallbackInfo ci) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        if (mc.player == null) return;
        GunManager.GunInfo gun = GunManager.getHeldGun(mc);
        if (gun != null) {
            if ("bow".equals(gun.key)) {
                if (RustViewModelAnimator.canShootBow()) {
                    RustViewModelAnimator.tryShootBowFromLmb(mc);
                    ci.cancel();
                    return;
                }
            } else {
                RustViewModelAnimator.tryShoot(mc, gun);
                ci.cancel();
                return;
            }
        }
        if (RustShootingManager.getHeldWeapon(mc) != null) {
            RustShootingManager.onAttackPressed(mc);
            ci.cancel();
            return;
        }
        if (RustBuildingSystem.isHoldingPlan(mc)) {
            RustBuildingSystem.placeCurrentBlueprint(mc);
            ci.cancel();
            return;
        }
        if (RustBuildingSystem.isHoldingHammer(mc)) {
            RustBuildingSystem.BuildHit bh = RustBuildingSystem.raycastBuildings(
                    mc.player.getCameraPosVec(1.0f), mc.player.getRotationVec(1.0f).normalize(), 6.5);
            if (bh != null) {
                RustBuildingSystem.repairPiece(mc, bh.piece.id);
                ci.cancel();
                return;
            }
        }
        if (RustWorldCollider.tryRaycastFarmHit(mc, false)) {
            ci.cancel();
            return;
        }
        // Allow melee strikes to pass cleanly through non-solid grass/foliage and hit animals behind them
        if (mc.world != null && mc.interactionManager != null
                && (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY)) {
            boolean blockingSolid = false;
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                BlockPos bp = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
                blockingSolid = !mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty();
            }
            if (!blockingSolid) {
                Vec3d eye = mc.player.getCameraPosVec(1.0f);
                Vec3d look = mc.player.getRotationVec(1.0f).normalize();
                Vec3d end = eye.add(look.multiply(3.35));
                Box box = mc.player.getBoundingBox().stretch(look.multiply(3.35)).expand(0.75);
                EntityHitResult ehr = ProjectileUtil.raycast(
                        mc.player, eye, end, box,
                        ent -> ent instanceof LivingEntity && ent.isAlive() && !ent.isSpectator(),
                        3.35 * 3.35);
                if (ehr != null && ehr.getEntity() != null) {
                    mc.interactionManager.attackEntity(mc.player, ehr.getEntity());
                    mc.player.swingHand(Hand.MAIN_HAND);
                    RustViewModelAnimator.triggerMeleeSwing(mc, mc.player.getMainHandStack());
                    ci.cancel();
                    return;
                }
            }
        }
        RustViewModelAnimator.triggerMeleeSwing(mc, mc.player.getMainHandStack());
        // Disallow breaking regular blocks in Survival (allow barrels & natural tree logs only)
        if (!mc.player.isCreative() && mc.world != null
                && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            Block b = mc.world.getBlockState(((BlockHitResult) mc.crosshairTarget).getBlockPos()).getBlock();
            boolean farmBlock = (b == Blocks.BLUE_TERRACOTTA || b == Blocks.BROWN_TERRACOTTA || BlockTags.LOGS.contains(b));
            if (!farmBlock) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void preventVanillaBlockBreaking(boolean breaking, CallbackInfo ci) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        if (mc.player != null && !mc.player.isCreative()) {
            ci.cancel();
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void onRustItemUse(CallbackInfo ci) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        if (mc.player == null) return;
        ItemStack stack = mc.player.getMainHandStack();
        if ("torch".equals(SpaceRustItems.getId(stack))) {
            RustViewModelAnimator.toggleTorch(mc);
            ci.cancel();
            return;
        }
        GunManager.GunInfo gun = GunManager.getHeldGun(mc);
        if (gun != null && !"crossbow".equals(gun.key)) {
            // Holding RMB with gun (ADS) or hunting bow (smooth draw) is handled by RustViewModelAnimator
            ci.cancel();
            return;
        }
        if (RustBuildingSystem.isHoldingPlan(mc)) {
            mc.openScreen(new RustBuildWheelScreen(RustBuildWheelScreen.Mode.PLAN, null));
            ci.cancel();
            return;
        }
        if (RustBuildingSystem.isHoldingHammer(mc)) {
            RustBuildingSystem.BuildHit bh = RustBuildingSystem.raycastBuildings(
                    mc.player.getCameraPosVec(1.0f), mc.player.getRotationVec(1.0f).normalize(), 6.5);
            if (bh != null) {
                mc.openScreen(new RustBuildWheelScreen(RustBuildWheelScreen.Mode.HAMMER, bh.piece));
                ci.cancel();
                return;
            }
        }
        if (RustBuildingSystem.tryDeployOnBuilding(mc)) {
            ci.cancel();
            return;
        }
        if (RustViewModelAnimator.tryUseMedical(mc, stack)) {
            ci.cancel();
            return;
        }
        if (RustDeployableManager.heldDeployableType == RustDeployableType.KEY_LOCK
                || RustDeployableManager.heldDeployableType == RustDeployableType.CODE_LOCK) {
            if (RustDeployableManager.tryPlaceHeld(mc)) {
                ci.cancel();
                return;
            }
        }
        if (RustDeployableManager.hoveredDeployable != null) {
            RustDeployableManager.interact(mc);
            ci.cancel();
            return;
        }
        if (RustDeployableManager.heldDeployableType != null) {
            if (RustDeployableManager.tryPlaceHeld(mc) || RustDeployableManager.previewPos != null) {
                ci.cancel();
            }
        }
    }
}
