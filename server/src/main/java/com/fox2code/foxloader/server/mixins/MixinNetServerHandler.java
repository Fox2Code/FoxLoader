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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetServerHandler.class)
public class MixinNetServerHandler implements NetServerHandlerAccessor {
    @Shadow private EntityPlayerMP playerEntity;

    @Override
    public EntityPlayerMP getPlayerEntity() {
        return this.playerEntity;
    }

    @Inject(method = "handlePluginMessage", at = @At("HEAD"))
    public void onHandlePluginMessage(Packet250PluginMessage packet250, CallbackInfo ci) {
        NetworkPlayer networkPlayer = (NetworkPlayer) this.playerEntity;
        if (networkPlayer == null || !networkPlayer.hasFoxLoader()) {
            return; // Stop client that are not modded from sending modded packets
        }
        ModContainer modContainer = ModLoader.getModContainer(packet250.channel);
        if (modContainer != null && packet250.data != null) {
            modContainer.notifyReceiveClientPacket(networkPlayer, packet250.data);
        }
    }
}
