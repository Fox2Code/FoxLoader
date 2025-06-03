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
package com.fox2code.foxloader.event.player;

import com.fox2code.foxevents.Event;
import net.minecraft.common.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class PlayerChatEvent extends PlayerEvent implements Event.Cancellable {
    private final String chatMessage;
    private String formattedMessage;

    public PlayerChatEvent(@NotNull EntityPlayer entityPlayer,@NotNull String chatMessage) {
        super(entityPlayer);
        this.chatMessage = Objects.requireNonNull(chatMessage, "chatMessage");
        this.formattedMessage = "<" + entityPlayer.username + "> " + chatMessage;
    }

    @NotNull public String getChatMessage() {
        return this.chatMessage;
    }

    @NotNull public String getFormattedMessage() {
        return this.formattedMessage;
    }

    public void setFormattedMessage(@NotNull String formattedMessage) {
        this.formattedMessage = Objects.requireNonNull(formattedMessage, "formattedMessage");
    }
}
