package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.registry.GameRegistryServer;
import net.minecraft.src.game.block.BlockFire;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockFire.class)
public class MixinBlockFire {
    @Shadow private int[] chanceToEncourageFire;
    @Shadow private int[] abilityToCatchFire;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(int var1, int var2, CallbackInfo ci) {
        this.chanceToEncourageFire = GameRegistryServer.chanceToEncourageFire;
        this.abilityToCatchFire = GameRegistryServer.abilityToCatchFire;
    }
}
