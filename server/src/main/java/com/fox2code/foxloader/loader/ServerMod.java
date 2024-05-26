package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.RegisteredItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.game.entity.player.EntityPlayerMP;
import net.minecraft.src.game.item.ItemStack;

import java.util.List;

public interface ServerMod extends Mod.SidedMod {
    static MinecraftServer getGameInstance() {
        return MinecraftServer.getInstance();
    }

    static NetworkPlayer toNetworkPlayer(EntityPlayerMP entityPlayerMP) {
        return (NetworkPlayer) entityPlayerMP;
    }

    static EntityPlayerMP toEntityPlayerMP(NetworkPlayer networkPlayer) {
        return (EntityPlayerMP) networkPlayer;
    }

    static List<EntityPlayerMP> getOnlinePlayers() {
        return getGameInstance().configManager.playerEntities;
    }

    @SuppressWarnings("unchecked")
    static List<? extends NetworkPlayer> getOnlineNetworkPlayers() {
        return (List<? extends NetworkPlayer>) (Object) getGameInstance().configManager.playerEntities;
    }

    @SuppressWarnings("DataFlowIssue")
    static ItemStack toItemStack(RegisteredItemStack registeredItemStack) {
        return (ItemStack) (Object) registeredItemStack;
    }

    static RegisteredItemStack toRegisteredItemStack(ItemStack registeredItemStack) {
        return (RegisteredItemStack) (Object) registeredItemStack;
    }
}
