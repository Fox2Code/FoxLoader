package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ServerMod;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.RegisteredItemStack;
import com.fox2code.foxloader.server.network.NetServerHandlerAccessor;
import com.fox2code.foxloader.server.network.NetworkPlayerImpl;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.entity.player.EntityPlayerMP;
import net.minecraft.src.game.level.World;
import net.minecraft.src.server.packets.NetServerHandler;
import net.minecraft.src.server.packets.NetworkManager;
import net.minecraft.src.server.packets.Packet250PluginMessage;
import net.minecraft.src.server.packets.Packet3Chat;
import net.minecraft.src.server.player.PlayerController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityPlayerMP.class)
public class MixinEntityPlayerMP extends EntityPlayer implements NetworkPlayer, NetworkPlayerImpl {
    @Shadow public NetServerHandler playerNetServerHandler;
    @Shadow public MinecraftServer mcServer;
    @Shadow public PlayerController itemInWorldManager;

    public MixinEntityPlayerMP(World var1) {
        super(var1);
    }

    @Override
    public void teleportRegistered(double x, double y, double z) {
        this.playerNetServerHandler.teleportTo(x, y, z, this.rotationYaw, this.rotationPitch);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.SERVER_ONLY;
    }

    @Override
    public void sendNetworkData(ModContainer modContainer, byte[] data) {
        if (this.hasFoxLoader()) {
            this.sendNetworkDataRaw(modContainer.id, data);
        }
    }

    @Override
    public void displayChatMessage(String chatMessage) {
        if (chatMessage.indexOf('\n') == -1) {
            this.playerNetServerHandler.sendPacket(new Packet3Chat(chatMessage));
        } else {
            String[] splits = chatMessage.split("\\n");
            for (String split : splits) {
                this.playerNetServerHandler.sendPacket(new Packet3Chat(split));
            }
        }
    }

    @Override
    public boolean hasFoxLoader() {
        return ((NetServerHandlerAccessor) this.playerNetServerHandler).hasFoxLoader();
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
        ((NetServerHandlerAccessor) this.playerNetServerHandler).notifyHasFoxLoader();
    }

    @Override
    public void notifyClientHello() {
        ((NetServerHandlerAccessor) this.playerNetServerHandler).notifyClientHello();
    }

    @Override
    public boolean hasClientHello() {
        return ((NetServerHandlerAccessor) this.playerNetServerHandler).hasClientHello();
    }

    @Override
    public void sendNetworkDataRaw(String modContainer, byte[] data) {
        this.playerNetServerHandler.sendPacket(new Packet250PluginMessage(modContainer, data));
    }

    @Override
    public NetworkPlayerController getNetworkPlayerController() {
        return (NetworkPlayerController) this.itemInWorldManager;
    }

    @Override
    public boolean isConnected() {
        return (!this.playerNetServerHandler.connectionClosed) &&
                NetworkManager.isRunning(this.playerNetServerHandler.netManager);
    }

    @Override
    public RegisteredItemStack getRegisteredHeldItem() {
        return ServerMod.toRegisteredItemStack(this.inventory.getCurrentItem());
    }

    @Override
    public void sendPlayerThroughPortalRegistered() {
        this.mcServer.configManager.sendPlayerToOtherDimension(
                ServerMod.toEntityPlayerMP(this));
        this.inPortal = false;
    }
}
