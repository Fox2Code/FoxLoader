package com.fox2code.foxloader.internal;

import com.fox2code.foxevents.EventHolder;
import com.fox2code.foxloader.event.player.PlayerChatEvent;
import com.fox2code.foxloader.event.player.PlayerJoinEvent;
import com.fox2code.foxloader.event.player.PlayerLeaveEvent;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.networking.Packet3Chat;
import net.minecraft.server.MinecraftServer;

public final class InternalPlayerHooks {
    private static final EventHolder<PlayerChatEvent> PLAYER_CHAT_EVENT =
            EventHolder.getHolderFromEvent(PlayerChatEvent.class);
    private static final EventHolder<PlayerJoinEvent> PLAYER_JOIN_EVENT =
            EventHolder.getHolderFromEvent(PlayerJoinEvent.class);
    private static final EventHolder<PlayerLeaveEvent> PLAYER_LEAVE_EVENT =
            EventHolder.getHolderFromEvent(PlayerLeaveEvent.class);

    private InternalPlayerHooks() {}

    public static String sendPlayerChatEvent(EntityPlayer entityPlayer, String message) {
        if (PLAYER_CHAT_EVENT.isEmpty()) return "<" + entityPlayer.username + "> " + message;
        PlayerChatEvent playerChatEvent = new PlayerChatEvent(entityPlayer, message);
        PLAYER_CHAT_EVENT.callEvent(playerChatEvent);
        return playerChatEvent.isCancelled() ? null : playerChatEvent.getFormattedMessage();
    }

    public static void sendPlayerJoinEvent(MinecraftServer minecraftServer, EntityPlayer entityPlayer) {
        if (entityPlayer == null || entityPlayer.worldObj == null || entityPlayer.worldObj.isRemote) return;
        PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(entityPlayer);
        PLAYER_JOIN_EVENT.callEvent(playerJoinEvent);
        if (minecraftServer != null && playerJoinEvent.getJoinMessage() != null) {
            minecraftServer.configManager.sendPacketToAllPlayers(new Packet3Chat(playerJoinEvent.getJoinMessage()));
        }
    }

    public static void sendPlayerLeaveEvent(MinecraftServer minecraftServer, EntityPlayer entityPlayer) {
        if (entityPlayer == null || entityPlayer.worldObj == null || entityPlayer.worldObj.isRemote) return;
        PlayerLeaveEvent playerJoinEvent = new PlayerLeaveEvent(entityPlayer);
        PLAYER_LEAVE_EVENT.callEvent(playerJoinEvent);
        if (minecraftServer != null && playerJoinEvent.getLeaveMessage() != null) {
            minecraftServer.configManager.sendPacketToAllPlayers(new Packet3Chat(playerJoinEvent.getLeaveMessage()));
        }
    }
}
