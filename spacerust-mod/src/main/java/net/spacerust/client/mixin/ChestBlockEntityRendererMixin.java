package net.spacerust.client.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.block.ChestAnimationProgress;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.ChestBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.spacerust.client.world.RustWorldSync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlockEntityRenderer.class)
public class ChestBlockEntityRendererMixin<T extends BlockEntity & ChestAnimationProgress> {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hideVanillaChestOnRustCrates(T entity, float tickDelta, MatrixStack matrices,
                                              VertexConsumerProvider vertexConsumers, int light, int overlay, CallbackInfo ci) {
        if (entity != null && RustWorldSync.hasCrateAt(entity.getPos())) {
            ci.cancel();
        }
    }
}
