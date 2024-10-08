package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.WorldProviderCustom;
import net.minecraft.client.Minecraft;
import net.minecraft.src.game.level.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldProvider.class)
public class MixinWorldProvider {
    @Inject(method = "getProviderForDimension",at = @At("HEAD"),cancellable = true)
    private static void getProviderForDimension(int arg0, CallbackInfoReturnable ci) throws NoSuchFieldException, IllegalAccessException {
        String str= (String) Minecraft.theMinecraft.getClass().getField("Dim").get(Minecraft.theMinecraft);
        ci.setReturnValue(WorldProviderCustom.getProviderForDimensioncustom(str,arg0));
        ci.cancel();
    }
}
