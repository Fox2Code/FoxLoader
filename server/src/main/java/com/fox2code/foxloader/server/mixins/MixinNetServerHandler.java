package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkConnection;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.server.network.NetServerHandlerAccessor;
import net.minecraft.src.game.entity.player.EntityPlayerMP;
import net.minecraft.src.server.ServerConfigurationManager;
import net.minecraft.src.server.packets.NetServerHandler;
import net.minecraft.src.server.packets.Packet;
import net.minecraft.src.server.packets.Packet250PluginMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetServerHandler.class)
public abstract class MixinNetServerHandler implements NetServerHandlerAccessor, NetworkConnection {
    @Shadow private EntityPlayerMP playerEntity;
    @Shadow public boolean connectionClosed;

    @Shadow public abstract void sendPacket(Packet var1);

    @Shadow public abstract void kickPlayer(String var1);

    @Unique private boolean hasFoxLoader;
    @Unique private boolean hasClientHello;
    @Unique private String kickMessage;

    @Override
    public EntityPlayerMP getPlayerEntity() {
        return this.playerEntity;
    }

    @Override
    public boolean hasFoxLoader() {
        return this.hasFoxLoader;
    }

    @Override
    public void notifyHasFoxLoader() {
        this.hasFoxLoader = true;
    }

    @Override
    public boolean hasClientHello() {
        return this.hasClientHello;
    }

    @Override
    public void notifyClientHello() {
        this.hasClientHello = true;
    }

    @Override
    public boolean isConnected() {
        return !this.connectionClosed;
    }

    @Override
    public void sendNetworkData(ModContainer modContainer, byte[] data) {
        this.sendPacket(new Packet250PluginMessage(modContainer.id, data));
    }

    @Override
    public NetworkPlayer getNetworkPlayer() {
        return (NetworkPlayer) this.playerEntity;
    }

    @Override
    public void kick(String message) {
        if (!this.connectionClosed) {
            this.kickPlayer(message);
        }
    }

    @Inject(method = "handlePluginMessage", at = @At("HEAD"))
    public void onHandlePluginMessage(Packet250PluginMessage packet250, CallbackInfo ci) {
        // Stop client that are not modded from sending modded packets
        // Also do not allow modded packet when client didn't finish connecting yet.
        if (this.playerEntity == null || !(this.hasClientHello || // Make exception for FoxLoader packets
                (this.hasFoxLoader && ModLoader.FOX_LOADER_MOD_ID.equals(packet250.channel)))) {
            return;
        }
        ModContainer modContainer = ModLoader.getModContainer(packet250.channel);
        if (modContainer != null && packet250.data != null) {
            modContainer.notifyReceiveClientPacket(this, packet250.data);
        }
    }

    @Inject(method = "kickPlayer", at = @At("HEAD"))
    public void onKickPlayer(String var1, CallbackInfo ci) {
        this.kickMessage = var1;
    }

    @Redirect(method = "kickPlayer", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/src/server/ServerConfigurationManager;sendPacketToAllPlayers(Lnet/minecraft/src/server/packets/Packet;)V"))
    public void kickPlayerHook(ServerConfigurationManager instance, Packet packet) {
        if (!ModLoader.Internal.notifyNetworkPlayerDisconnected((NetworkPlayer) this.playerEntity, this.kickMessage)) {
            instance.sendPacketToAllPlayers(packet);
        }
    }

    @Redirect(method = "handleErrorMessage", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/src/server/ServerConfigurationManager;sendPacketToAllPlayers(Lnet/minecraft/src/server/packets/Packet;)V"))
    public void handleErrorMessageHook(ServerConfigurationManager instance, Packet packet) {
        if (!ModLoader.Internal.notifyNetworkPlayerDisconnected((NetworkPlayer) this.playerEntity, null)) {
            instance.sendPacketToAllPlayers(packet);
        }
    }
}
