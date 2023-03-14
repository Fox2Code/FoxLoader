package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.registry.GameRegistryClient;
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
    public void onInit(int id, CallbackInfo ci) {
        this.chanceToEncourageFire = GameRegistryClient.chanceToEncourageFire;
        this.abilityToCatchFire = GameRegistryClient.abilityToCatchFire;
    }
}
