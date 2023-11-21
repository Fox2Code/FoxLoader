package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.renderer.TextureExt;
import net.minecraft.src.client.renderer.Rect2i;
import net.minecraft.src.client.renderer.block.Texture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(Texture.class)
public abstract class MixinTexture implements TextureExt {
    @Shadow @Final private int textureTarget;

    @Shadow private ByteBuffer textureData;

    @Shadow @Final private int width;

    @Shadow private boolean autoCreate;

    @Shadow public abstract void uploadTexture();

    @Shadow private boolean textureNotModified;

    @Shadow public abstract void fillRect(Rect2i other, int color);

    @Shadow @Final private static IntBuffer DATA_BUFFER;

    @Inject(method = "uploadTexture()V", at = @At("HEAD"))
    public void onUploadTexture(CallbackInfo ci) {
        this.textureData.position(this.textureData.capacity());
    }

    @Override
    public void pushDynamicData(final int originX,final int originY, int[] renderingData) {
        if (this.textureTarget != 32879) {
            Rect2i rect = new Rect2i(originX, originY, 16, 16);
            this.fillRect(rect, 0);

            this.textureData.position(0);

            for(int w = rect.getRectY(); w < rect.getRectY() + rect.getRectHeight(); ++w) {
                int var5 = w * this.width * 4;

                for(int h = rect.getRectX(); h < rect.getRectX() + rect.getRectWidth(); ++h) {
                    int color = renderingData[(h - originX) + ((w - originY) * 16)];
                    this.textureData.put(var5 + h * 4 + 0, (byte)(color >> 24 & 0xFF));
                    this.textureData.put(var5 + h * 4 + 1, (byte)(color >> 16 & 0xFF));
                    this.textureData.put(var5 + h * 4 + 2, (byte)(color >> 8 & 0xFF));
                    this.textureData.put(var5 + h * 4 + 3, (byte)(color >> 0 & 0xFF));
                }
            }

            if (this.autoCreate) {
                this.uploadTexture();
            } else {
                this.textureNotModified = false;
            }
        }
    }
}
