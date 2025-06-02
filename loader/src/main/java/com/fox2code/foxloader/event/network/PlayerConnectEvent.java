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
package com.fox2code.foxloader.event.network;

import com.fox2code.foxevents.Event;
import com.fox2code.foxloader.loader.packet.ClientHello;
import net.minecraft.common.networking.NetworkManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This event is only called server side when a player attempt to connect.
 * <p>
 * At this point the player entity is not initialized, see {@link com.fox2code.foxloader.event.player.PlayerJoinEvent}
 */
public final class PlayerConnectEvent extends Event implements Event.Cancellable {
    private final NetworkManager networkManager;
    private final ClientHello clientHello;
    private final String username;
    private final boolean incompatibleClient;
    private String cancelMessage;

    public PlayerConnectEvent(NetworkManager networkManager, String username, boolean incompatibleClient) {
        this.networkManager = networkManager;
        if (networkManager.flHelloHandler instanceof ClientHello) {
            this.clientHello = (ClientHello) networkManager.flHelloHandler;
        } else {
            this.clientHello = null;
        }
        this.username = username;
        this.incompatibleClient = incompatibleClient;
        this.cancelMessage = "Event cancelled";
    }

    @NotNull public NetworkManager getNetworkManager() {
        return this.networkManager;
    }

    @Nullable public ClientHello getClientHello() {
        return this.clientHello;
    }

    @NotNull public String getUsername() {
        return this.username;
    }

    /**
     * @return true if the client cannot join due to being vanilla.
     */
    public boolean isIncompatibleClient() {
        return this.incompatibleClient;
    }

    @NotNull public String getCancelMessage() {
        return this.cancelMessage;
    }

    public void setCancelMessage(@Nullable String cancelMessage) {
        this.cancelMessage = cancelMessage == null ? "Event cancelled" : cancelMessage;
    }

    public void cancel(@Nullable String cancelMessage) {
        this.cancelMessage = cancelMessage == null ? "Event cancelled" : cancelMessage;
        this.setCancelled(true);
    }
}
