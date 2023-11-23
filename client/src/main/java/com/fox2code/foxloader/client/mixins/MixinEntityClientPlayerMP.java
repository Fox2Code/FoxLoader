package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.network.NetClientHandlerExtensions;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.network.NetworkPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.Session;
import net.minecraft.src.client.packets.NetClientHandler;
import net.minecraft.src.client.packets.Packet250PluginMessage;
import net.minecraft.src.client.player.EntityClientPlayerMP;
import net.minecraft.src.client.player.EntityPlayerSP;
import net.minecraft.src.game.level.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityClientPlayerMP.class)
public class MixinEntityClientPlayerMP extends EntityPlayerSP implements NetworkPlayer {
    @Shadow public NetClientHandler sendQueue;

    public MixinEntityClientPlayerMP(Minecraft var1, World var2, Session var3, int var4) {
        super(var1, var2, var3, var4);
    }

    @Inject(at = @At("RETURN"), method = "<init>")
    public void onNewMixinEntityClientPlayerMP(
            Minecraft var1, World var2, Session var3, NetClientHandler var4, CallbackInfo ci) {

    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.CLIENT_ONLY;
    }

    @Override
    public void sendNetworkData(ModContainer modContainer, byte[] data) {
        sendQueue.addToSendQueue(new Packet250PluginMessage(modContainer.id, data));
    }

    @Override
    public void displayChatMessage(String chatMessage) {
        if (chatMessage.indexOf('\n') == -1) {
            Minecraft.getInstance().ingameGUI.addChatMessage(chatMessage);
        } else {
            String[] splits = chatMessage.split("\\n");
            for (String split : splits) {
                Minecraft.getInstance().ingameGUI.addChatMessage(split);
            }
        }
    }

    @Override
    public boolean hasFoxLoader() {
        return sendQueue != null && ((NetClientHandlerExtensions) sendQueue).isFoxLoader();
    }

    @Override
    public String getPlayerName() {
        return this.username;
    }

    @Override
    public boolean isOperator() {
        return false;
    }

    @Override
    public boolean isConnected() {
        return sendQueue != null && Minecraft.getInstance().isMultiplayerWorld() &&
                !((NetClientHandlerExtensions) sendQueue).isDisconnected();
    }

    @Override
    public void sendPlayerThroughPortalRegistered() {
        throw new RuntimeException("No authority over player");
    }
}
