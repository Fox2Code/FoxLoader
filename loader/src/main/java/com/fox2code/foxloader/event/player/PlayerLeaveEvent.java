package com.fox2code.foxloader.event.player;

import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.util.ChatColors;

/**
 * This event is called when a player leaves the world.
 * <p>
 * You can modify the player data during this stage and the changes will be saved on disk
 */
public final class PlayerLeaveEvent extends PlayerEvent {
    private String leaveMessage;

    public PlayerLeaveEvent(EntityPlayer entityPlayer) {
        super(entityPlayer);
        this.leaveMessage = ChatColors.YELLOW + entityPlayer.username + " left the game.";
    }

    public void setLeaveMessage(String leaveMessage) {
        this.leaveMessage = leaveMessage;
    }

    public String getLeaveMessage() {
        return this.leaveMessage;
    }
}
