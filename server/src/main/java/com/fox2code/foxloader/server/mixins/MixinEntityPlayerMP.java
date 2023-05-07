package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.server.network.NetworkPlayerImpl;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.entity.player.EntityPlayerMP;
import net.minecraft.src.game.level.World;
import net.minecraft.src.server.packets.NetServerHandler;
import net.minecraft.src.server.packets.Packet250PluginMessage;
import net.minecraft.src.server.packets.Packet3Chat;
import net.minecraft.src.server.player.PlayerController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityPlayerMP.class)
public class MixinEntityPlayerMP extends EntityPlayer implements NetworkPlayer, NetworkPlayerImpl {
    @Shadow public NetServerHandler playerNetServerHandler;
    @Shadow public MinecraftServer mcServer;
    @Shadow public PlayerController itemInWorldManager;
    @Unique private boolean hasFoxLoader;

    public MixinEntityPlayerMP(World var1) {
        super(var1);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.SERVER_ONLY;
    }

    @Override
    public void sendNetworkData(ModContainer modContainer, byte[] data) {
        this.sendNetworkDataRaw(modContainer.id, data);
    }

    @Override
    public void displayChatMessage(String chatMessage) {
        this.playerNetServerHandler.sendPacket(new Packet3Chat(chatMessage));
    }

    @Override
    public boolean hasFoxLoader() {
        return this.hasFoxLoader;
    }

    @Override
    public String getPlayerName() {
        return this.username;
    }

    @Override
    public boolean isOperator() {
        return this.mcServer.configManager.isOp(this.username);
    }

    @Override
    public void kick(String message) {
        this.playerNetServerHandler.kickPlayer(message);
    }

    @Override
    public void notifyHasFoxLoader() {
        this.hasFoxLoader = true;
    }

    @Override
    public void sendNetworkDataRaw(String modContainer, byte[] data) {
        if (this.hasFoxLoader) {
            this.playerNetServerHandler.sendPacket(new Packet250PluginMessage(modContainer, data));
        }
    }

    @Override
    public NetworkPlayerController getNetworkPlayerController() {
        return (NetworkPlayerController) this.itemInWorldManager;
    }
}
