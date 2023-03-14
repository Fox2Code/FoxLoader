package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.ServerModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.GameRegistryServer;
import com.fox2code.foxloader.server.network.LoginState;
import com.fox2code.foxloader.server.network.NetworkPlayerImpl;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.game.entity.player.EntityPlayerMP;
import net.minecraft.src.server.packets.NetLoginHandler;
import net.minecraft.src.server.packets.Packet1Login;
import net.minecraft.src.server.packets.Packet254ServerPing;
import net.minecraft.src.server.packets.Packet2Handshake;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Socket;

@Mixin(value = NetLoginHandler.class, priority = 0)
public abstract class MixinNetLoginHandler {
    @Shadow private Packet1Login packet1login;

    /**
     * Used to improve legacy login security, and mitigate DoS attacks.
     */
    @Unique private LoginState state;
    @Unique private boolean isModded;

    @Shadow public abstract void kickUser(String var1);
    @Shadow public abstract void doLogin(Packet1Login var1);

    @Inject(at = @At("RETURN"), method = "<init>")
    public void onInit(MinecraftServer var1, Socket var2, String var3, CallbackInfo ci) {
        this.state = LoginState.PRE_HANDSHAKE;
    }

    @Inject(at = @At("RETURN"), method = "tryLogin")
    public void onTryLogin(CallbackInfo ci) {
        if (this.packet1login != null) {
            this.doLogin(this.packet1login);
            this.packet1login = null;
        }
    }

    @Inject(at = @At("HEAD"), method = "kickUser")
    public void onKick(String var1, CallbackInfo ci) {
        this.state = LoginState.KICKED;
    }

    @Inject(at = @At("HEAD"), method = "handleHandshake", cancellable = true)
    public void onHandshake(Packet2Handshake var1, CallbackInfo ci) {
        if (this.state != LoginState.PRE_HANDSHAKE) {
            this.kickUser("Invalid login packet order!");
            ci.cancel();
            return;
        }
        this.state = LoginState.HANDSHAKE;
        if (var1.username.endsWith(ModLoader.FOX_LOADER_HEADER)) {
            var1.username = var1.username.substring(
                    0, var1.username.length() -
                            ModLoader.FOX_LOADER_HEADER.length());
            isModded = true;
        }
    }

    @Inject(at = @At("HEAD"), method = "handleLogin", cancellable = true)
    public void onLogin(Packet1Login var1, CallbackInfo ci) {
        if (this.state != LoginState.HANDSHAKE) {
            kickUser("Invalid login packet order!");
            ci.cancel();
            return;
        }
        this.state = LoginState.LOGIN_IN;
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

    @Inject(at = @At("HEAD"), method = "handleServerPing", cancellable = true)
    public void onServerPing(Packet254ServerPing packet254serverping, CallbackInfo ci) {
        if (this.state != LoginState.KICKED) {
            this.state = LoginState.KICKED;
        } else {
            ci.cancel();
        }
    }
}
