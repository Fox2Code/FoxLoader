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

    @NotNull
    public NetworkManager getNetworkManager() {
        return this.networkManager;
    }

    @Nullable
    public ClientHello getClientHello() {
        return this.clientHello;
    }

    @NotNull
    public String getUsername() {
        return this.username;
    }

    /**
     * @return true if the client cannot join due to being vanilla.
     */
    public boolean isIncompatibleClient() {
        return this.incompatibleClient;
    }

    @NotNull
    public String getCancelMessage() {
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
