package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.server.network.NetServerHandlerAccessor;
import net.minecraft.src.game.entity.player.EntityPlayerMP;
import net.minecraft.src.server.packets.NetServerHandler;
import net.minecraft.src.server.packets.Packet250PluginMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetServerHandler.class)
public abstract class MixinNetServerHandler implements NetServerHandlerAccessor {
    @Shadow private EntityPlayerMP playerEntity;
    @Unique private boolean hasFoxLoader;
    @Unique private boolean hasClientHello;

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

    @Inject(method = "handlePluginMessage", at = @At("HEAD"))
    public void onHandlePluginMessage(Packet250PluginMessage packet250, CallbackInfo ci) {
        NetworkPlayer networkPlayer = (NetworkPlayer) this.playerEntity;
        // Stop client that are not modded from sending modded packets
        // Also do not allow modded packet when client didn't finish connecting yet.
        if (networkPlayer == null || !(this.hasClientHello || // Make exception for FoxLoader packets
                (this.hasFoxLoader && ModLoader.FOX_LOADER_MOD_ID.equals(packet250.channel)))) {
            return;
        }
        ModContainer modContainer = ModLoader.getModContainer(packet250.channel);
        if (modContainer != null && packet250.data != null) {
            modContainer.notifyReceiveClientPacket(networkPlayer, packet250.data);
        }
    }
}
