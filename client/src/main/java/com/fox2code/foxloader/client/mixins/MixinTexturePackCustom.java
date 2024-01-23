package com.fox2code.foxloader.client.mixins;

import net.minecraft.src.client.renderer.block.TexturePackCustom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TexturePackCustom.class)
public class MixinTexturePackCustom {
    @Unique private Boolean compatibilityStatus;

    @Inject(method = "isCompatible", at = @At("HEAD"), cancellable = true)
    public void hotfix_isCompatibleHead(CallbackInfoReturnable<Boolean> cir) {
        if (this.compatibilityStatus != null) {
            cir.setReturnValue(this.compatibilityStatus);
        }
    }

    @Inject(method = "isCompatible", at = @At("RETURN"))
    public void hotfix_isCompatibleReturn(CallbackInfoReturnable<Boolean> cir) {
        this.compatibilityStatus = cir.getReturnValue();
    }
}
