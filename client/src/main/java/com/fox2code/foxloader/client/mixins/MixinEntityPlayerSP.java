package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.network.NetworkPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.player.EntityPlayerSP;
import net.minecraft.src.game.entity.Entity;
import net.minecraft.src.game.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP implements NetworkPlayer {

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.SINGLE_PLAYER;
    }

    @Override
    public void sendNetworkData(ModContainer modContainer, byte[] data) {}

    @Override
    public void displayChatMessage(String chatMessage) {
        Minecraft.getInstance().ingameGUI.addChatMessage(chatMessage);
    }

    @Override
    public boolean hasFoxLoader() {
        return true;
    }

    @Override
    public String getPlayerName() {
        return ((EntityPlayer) (Object) this).username;
    }

    @Override
    public boolean isOperator() {
        return ((Entity) (Object) this).worldObj.worldInfo.isCheatsEnabled();
    }

    @Override
    public void kick(String message) {
        throw new IllegalStateException("kick cannot be used client-side");
    }

    @Override
    public NetworkPlayerController getNetworkPlayerController() {
        return (NetworkPlayerController) Minecraft.getInstance().playerController;
    }
}
