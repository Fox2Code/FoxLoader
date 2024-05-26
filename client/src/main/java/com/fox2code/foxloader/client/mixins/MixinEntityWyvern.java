package com.fox2code.foxloader.client.mixins;

import net.minecraft.src.game.entity.monster.EntityWyvern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityWyvern.class)
public class MixinEntityWyvern {
    @Inject(method = "isCourseTraversable", at = @At("HEAD"), cancellable = true)
    public void hotfix_isCourseTraversable(double x, double y, double z, double xyzsqrt, CallbackInfoReturnable<Boolean> cir) {
        if (xyzsqrt > Short.MAX_VALUE) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
