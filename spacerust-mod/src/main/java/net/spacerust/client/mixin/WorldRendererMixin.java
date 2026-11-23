package net.spacerust.client.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.spacerust.client.guns.RustViewModelAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds dynamic warm block light around the player while holding a lit SpaceRust Torch.
 * Reads only lock-free volatile fields so ChunkBuilder worker threads stay thread-safe.
 */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(
            method = "getLightmapCoordinates(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void injectDynamicTorchLight(BlockRenderView world, BlockState state, BlockPos pos,
                                                CallbackInfoReturnable<Integer> cir) {
        if (!RustViewModelAnimator.isTorchLightActive() || pos == null) return;

        double dx = (pos.getX() + 0.5) - RustViewModelAnimator.torchLightX;
        double dy = (pos.getY() + 0.5) - RustViewModelAnimator.torchLightY;
        double dz = (pos.getZ() + 0.5) - RustViewModelAnimator.torchLightZ;
        double dSq = dx * dx + dy * dy + dz * dz;
        if (dSq >= 256.0) return;

        int dynBlock = Math.max(0, Math.min(15, (int) Math.round(15.5 - Math.sqrt(dSq) * 0.96)));
        int packed = cir.getReturnValueI();
        int origBlock = (packed >> 4) & 0xF;
        if (dynBlock > origBlock) {
            cir.setReturnValue((packed & 0xFFFF000F) | (dynBlock << 4));
        }
    }
}
