package net.spacerust.client.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.spacerust.client.SpaceRustClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides items held in the player's hands when rendering the 3D character preview inside inventory screens.
 */
@Mixin(HeldItemFeatureRenderer.class)
public class HeldItemFeatureRendererMixin {

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hideHeldItemInInventory(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                         int light, LivingEntity entity, float limbAngle, float limbDistance,
                                         float tickDelta, float animationProgress, float headYaw, float headPitch,
                                         CallbackInfo ci) {
        if (SpaceRustClient.hideGuiHeldItem) {
            ci.cancel();
        }
    }
}
