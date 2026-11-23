package net.spacerust.client.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.spacerust.client.guns.RustViewModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void renderRust3DViewModel(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                       Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                       CallbackInfo ci) {
        if (hand == Hand.OFF_HAND) {
            ci.cancel();
            return;
        }
        if (hand == Hand.MAIN_HAND && RustViewModelRenderer.supports(item)) {
            if (RustViewModelRenderer.render(player, tickDelta, matrices, item, light)) {
                ci.cancel();
            }
        }
    }
}
