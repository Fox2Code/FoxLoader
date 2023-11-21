package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.renderer.TextureDynamic;
import net.minecraft.src.client.renderer.block.TextureStitched;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureStitched.class)
public class MixinTextureStitched {
    @Inject(method = "makeTextureStitched", at = @At("HEAD"), cancellable = true)
    private static void makeTextureDynamic(String par0Str, CallbackInfoReturnable<TextureStitched> cir) {
        if (par0Str != null && TextureDynamic.isDynamicTexName(par0Str)) {
            cir.setReturnValue(new TextureDynamic(par0Str));
        }
    }
}
