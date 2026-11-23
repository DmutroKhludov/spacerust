package net.spacerust.client.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import net.spacerust.client.world.RustWorldCollider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Resolves solid physical collision between the local player and 3D Rust Ore Nodes & Trees.
 */
@Mixin(Entity.class)
public class EntityCollisionMixin {

    @Inject(method = "move", at = @At("TAIL"))
    private void onEntityMoveTail(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        RustWorldCollider.resolvePlayerCollision((Entity) (Object) this);
    }
}
