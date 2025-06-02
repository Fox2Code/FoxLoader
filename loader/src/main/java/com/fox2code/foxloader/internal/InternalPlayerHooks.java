/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
