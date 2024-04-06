package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.ModLoaderOptions;
import com.fox2code.foxloader.loader.ServerModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.GameRegistryServer;
import com.fox2code.foxloader.server.network.NetworkPlayerImpl;
import net.minecraft.src.game.entity.player.EntityPlayerMP;
import net.minecraft.src.server.packets.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetLoginHandler.class, priority = 0)
public abstract class MixinNetLoginHandler extends NetHandler {
    @Shadow private NetLoginHandler.LoginState state;
    @Shadow public NetworkManager netManager;
    @Unique private Packet250PluginMessage preemptive;
    @Unique private boolean isPreemptive;
    @Unique private boolean isModded;

    @Inject(at = @At("HEAD"), method = "handleHandshake")
    public void onHandshake(Packet2Handshake var1, CallbackInfo ci) {
        if (var1.username.endsWith(ModLoader.FOX_LOADER_HEADER)) {
            var1.username = var1.username.substring(
                    0, var1.username.length() -
                            ModLoader.FOX_LOADER_HEADER.length());
            this.isModded = true;
        }
    }

    @Redirect(method = "handleLogin", at = @At(value = "FIELD", target =
            "Lnet/minecraft/src/server/packets/NetLoginHandler;state:Lnet/minecraft/src/server/packets/NetLoginHandler$LoginState;"))
    public NetLoginHandler.LoginState onLogin(NetLoginHandler instance) {
        NetLoginHandler.LoginState state = this.state;
        if (state == NetLoginHandler.LoginState.HANDSHAKE) {
            this.state = NetLoginHandler.LoginState.LOGIN_IN;
            if (this.isModded && ModLoaderOptions.INSTANCE.preemptiveNetworking) {
                this.isPreemptive = true;
                this.netManager.addToSendQueue(new Packet250PluginMessage(
                        ModLoader.FOX_LOADER_MOD_ID, GameRegistryServer.INSTANCE.getServerHello()));
            }
        }
        return state;
    }

    @Redirect(method = "doLogin", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/src/game/entity/player/EntityPlayerMP;func_20057_k()V"))
    public void func_20057_kRedirect(EntityPlayerMP instance) {
        if (this.isModded) {
            ((NetworkPlayerImpl) instance).notifyHasFoxLoader();
            if (!this.isPreemptive) {
                GameRegistryServer.INSTANCE.sendRegistryData(instance);
            }
        }

        ServerModLoader.notifyNetworkPlayerJoined((NetworkPlayer) instance);
        instance.func_20057_k();
        if (this.preemptive != null) {
            ((NetworkPlayerImpl) instance).handlePreemptiveData(this.preemptive);
            this.preemptive = null;
        }
    }

    @Override
    public void handlePluginMessage(Packet250PluginMessage packet250) {
        if (this.state == NetLoginHandler.LoginState.LOGIN_IN &&
                this.preemptive == null && this.isModded && this.isPreemptive &&
                ModLoader.FOX_LOADER_MOD_ID.equals(packet250.channel)) {
            this.preemptive = packet250;
        } else this.registerPacket(packet250);
    }
}
