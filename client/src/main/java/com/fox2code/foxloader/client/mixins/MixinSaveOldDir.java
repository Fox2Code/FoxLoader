package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.WorldProviderCustom;
import net.minecraft.src.game.level.WorldProvider;
import net.minecraft.src.game.level.WorldProviderHell;
import net.minecraft.src.game.level.chunk.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

@Mixin(SaveOldDir.class)
public abstract class MixinSaveOldDir  {


@Inject(method = "getChunkLoader",at = @At("HEAD"),cancellable = true)
public void getChunkLoader(WorldProvider worldProvider, CallbackInfoReturnable ci) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (worldProvider instanceof WorldProviderCustom){
            File save = (File) ((SaveOldDir)(Object)this).getClass().getMethod("getSaveDirectory").invoke(this);
            File file = new File(save, "DIM-"+((WorldProviderCustom) worldProvider).wpName);
            file.mkdirs();

            ci.setReturnValue(new RNThreegionChunkLoader(file));
            ci.cancel();
        }
    }
}
