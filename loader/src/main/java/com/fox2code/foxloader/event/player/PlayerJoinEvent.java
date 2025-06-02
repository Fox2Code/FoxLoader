package com.fox2code.foxloader.event.player;

import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.util.ChatColors;

/**
 * This event is called when a player join the world.
 * <p>
 * This event is not cancellable, to kick player on join see {@link com.fox2code.foxloader.event.network.PlayerConnectEvent}
 */
public final class PlayerJoinEvent extends PlayerEvent {
    private String joinMessage;

    public PlayerJoinEvent(EntityPlayer entityPlayer) {
        super(entityPlayer);
        this.joinMessage = ChatColors.YELLOW + entityPlayer.username + " joined the game.";
    }

    public void setJoinMessage(String joinMessage) {
        this.joinMessage = joinMessage;
    }

    public String getJoinMessage() {
        return this.joinMessage;
    }
}
