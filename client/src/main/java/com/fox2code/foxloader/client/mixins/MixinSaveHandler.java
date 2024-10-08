package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.WorldProviderCustom;
import net.minecraft.src.game.level.WorldProvider;
import net.minecraft.src.game.level.WorldProviderHell;
import net.minecraft.src.game.level.chunk.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(SaveHandler.class)
public abstract class MixinSaveHandler {
@Shadow @Final private File saveDirectory;

    @Inject(method = "getChunkLoader",at = @At("HEAD"),cancellable = true)
    public void getChunkLoader(WorldProvider worldProvider, CallbackInfoReturnable ci){
        if (worldProvider instanceof WorldProviderCustom){
            File file = new File(this.saveDirectory, ((WorldProviderCustom) worldProvider).wpName);
            file.mkdirs();
            ci.setReturnValue(new RNThreegionChunkLoader(file));
            ci.cancel();
        }
    }
}
