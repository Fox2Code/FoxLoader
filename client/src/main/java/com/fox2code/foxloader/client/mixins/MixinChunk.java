package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.loader.ClientModLoader;
import com.fox2code.foxloader.registry.GameRegistryClient;
import net.minecraft.src.game.level.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Chunk.class)
public class MixinChunk {
    @Shadow public short[] blocks;

    @Inject(method = "setChunkData", at = @At("RETURN"))
    public void onSetChunkData(byte[] data, int mix, int miy, int miz, int max, int may, int maz, boolean init, int progress, CallbackInfoReturnable<Integer> cir) {
        if (this.blocks != null) {
            for (int var17 = mix; var17 < max; ++var17) {
                for (int var31 = miy; var31 < may; ++var31) {
                    for (int zter = miz; zter < maz; ++zter) {
                        int pia = var17 << 8 | zter << 4 | var31;
                        this.blocks[pia] = GameRegistryClient.blockIdMappingIn[this.blocks[pia]];
                    }
                }
            }
        }
    }
}
