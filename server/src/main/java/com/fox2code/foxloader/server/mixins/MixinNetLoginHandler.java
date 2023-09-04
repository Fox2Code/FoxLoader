package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.ServerModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.GameRegistryServer;
import com.fox2code.foxloader.server.network.NetworkPlayerImpl;
import net.minecraft.src.game.entity.player.EntityPlayerMP;
import net.minecraft.src.server.packets.NetLoginHandler;
import net.minecraft.src.server.packets.Packet2Handshake;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetLoginHandler.class, priority = 0)
public abstract class MixinNetLoginHandler {
    @Unique private boolean isModded;

    @Inject(at = @At("HEAD"), method = "handleHandshake")
    public void onHandshake(Packet2Handshake var1, CallbackInfo ci) {
        if (var1.username.endsWith(ModLoader.FOX_LOADER_HEADER)) {
            var1.username = var1.username.substring(
                    0, var1.username.length() -
                            ModLoader.FOX_LOADER_HEADER.length());
            isModded = true;
        }
    }

    @Redirect(method = "doLogin", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/src/game/entity/player/EntityPlayerMP;func_20057_k()V"))
    public void func_20057_kRedirect(EntityPlayerMP instance) {
        if (isModded) {
            ((NetworkPlayerImpl) instance).notifyHasFoxLoader();
            GameRegistryServer.INSTANCE.sendRegistryData(instance);
        }

        ServerModLoader.notifyNetworkPlayerJoined((NetworkPlayer) instance);
        instance.func_20057_k();
    }
}
