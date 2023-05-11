package com.fox2code.foxloader.client.mixins;

import net.minecraft.src.client.gui.Container;
import net.minecraft.src.game.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Container.class)
public class MixinContainer {
    @Inject(method = "onCraftGuiClosed", at = @At("HEAD"), cancellable = true)
    public void hotfix_onCraftGuiClosedHook(EntityPlayer var1, CallbackInfo ci) {
        if (var1 == null || var1.inventory == null) {
            ci.cancel();
        }
    }
}
