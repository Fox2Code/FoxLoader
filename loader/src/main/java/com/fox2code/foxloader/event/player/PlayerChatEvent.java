package com.fox2code.foxloader.event.player;

import com.fox2code.foxevents.Event;
import net.minecraft.common.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class PlayerChatEvent extends PlayerEvent implements Event.Cancellable {
    private final String chatMessage;
    private String formattedMessage;

    public PlayerChatEvent(EntityPlayer entityPlayer, String chatMessage) {
        super(entityPlayer);
        this.chatMessage = Objects.requireNonNull(chatMessage, "chatMessage");
        this.formattedMessage = "<" + entityPlayer.username + "> " + chatMessage;
    }

    @NotNull
    public String getChatMessage() {
        return this.chatMessage;
    }

    @NotNull
    public String getFormattedMessage() {
        return this.formattedMessage;
    }

    public void setFormattedMessage(@NotNull String formattedMessage) {
        this.formattedMessage = Objects.requireNonNull(formattedMessage, "formattedMessage");
    }
}
