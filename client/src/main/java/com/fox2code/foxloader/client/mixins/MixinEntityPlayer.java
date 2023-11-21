package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.RegisteredEntity;
import net.minecraft.src.game.entity.Entity;
import net.minecraft.src.game.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayer.class)
public class MixinEntityPlayer {
    @Inject(method = "useCurrentItemOnEntity", at = @At("HEAD"), cancellable = true)
    public void onUseCurrentItemOnEntity(Entity var1, CallbackInfo ci) {
        if (!(this instanceof NetworkPlayer)) return;
        NetworkPlayer networkPlayer = (NetworkPlayer) this;
        if (ModLoader.Internal.notifyPlayerUseItemOnEntity(networkPlayer,
                networkPlayer.getRegisteredHeldItem(), (RegisteredEntity) var1)) {
            ci.cancel();
        }
    }

    @Inject(method = "attackTargetEntityWithCurrentItem", at = @At("HEAD"), cancellable = true)
    public void onAttackTargetEntityWithCurrentItem(Entity var1, CallbackInfo ci) {
        if (!(this instanceof NetworkPlayer)) return;
        NetworkPlayer networkPlayer = (NetworkPlayer) this;
        if (ModLoader.Internal.notifyPlayerAttackEntity(networkPlayer,
                networkPlayer.getRegisteredHeldItem(), (RegisteredEntity) var1)) {
            ci.cancel();
        }
    }
}
