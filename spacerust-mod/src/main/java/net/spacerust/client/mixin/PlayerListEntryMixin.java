package net.spacerust.client.mixin;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;
import net.spacerust.client.character.RustCharacterManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public class PlayerListEntryMixin {

    @Inject(method = "hasSkinTexture", at = @At("HEAD"), cancellable = true)
    private void onHasSkinTexture(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method = "getSkinTexture", at = @At("HEAD"), cancellable = true)
    private void onGetSkinTexture(CallbackInfoReturnable<Identifier> cir) {
        PlayerListEntry self = (PlayerListEntry) (Object) this;
        cir.setReturnValue(RustCharacterManager.getProfile(self.getProfile()).textureId);
    }

    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void onGetModel(CallbackInfoReturnable<String> cir) {
        PlayerListEntry self = (PlayerListEntry) (Object) this;
        cir.setReturnValue(RustCharacterManager.getProfile(self.getProfile()).model);
    }
}
