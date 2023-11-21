package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.renderer.TextureDynamic;
import net.minecraft.src.client.renderer.block.Texture;
import net.minecraft.src.client.renderer.block.TextureManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;

@Mixin(TextureManager.class)
public abstract class MixinTextureManager {
    @Shadow protected abstract String getBasename(String s);

    @Shadow public abstract Texture createEmptyTexture(String filename, int type, int w, int h, int format);

    @Inject(method = "createTexture", at = @At("HEAD"), cancellable = true)
    public void createTexture(String s, CallbackInfoReturnable<List<Texture>> cir) {
        if (TextureDynamic.isDynamicTexName(s)) {
            cir.setReturnValue(Collections.singletonList(
                    this.createEmptyTexture(this.getBasename(s), 2, 16, 16, GL11.GL_RGBA)));
        }
    }
}
