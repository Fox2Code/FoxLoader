package com.fox2code.foxloader.network;

import com.fox2code.foxloader.loader.ModContainer;

public interface NetworkPlayer {
    /**
     * Just empty byte array to reduce allocations.
     */
    byte[] NULL_DATA = new byte[0];

    /**
     * Get the connection type of the object.
     */
    ConnectionType getConnectionType();

    /**
     * Send network data to remote player, if {@link #hasFoxLoader()} return false,
     * this method do not perform any specific actions.
     */
    void sendNetworkData(ModContainer modContainer, byte[] data);

    /**
     * Send/Display chat message to the user screen.
     */
    void displayChatMessage(String chatMessage);

    /**
     * @return if the remote party has the mod loader,
     * always return true for a single player world.
     */
    boolean hasFoxLoader();

    /**
     * @return the player name
     */
    String getPlayerName();

    /**
     * @return if the player as operator permission.
     *
     * Always false client-side when connected to a server.
     */
    boolean isOperator();

    enum ConnectionType {
        SINGLE_PLAYER(true, true), CLIENT_ONLY(true, false), SERVER_ONLY(false, true);

        public final boolean isClient, isServer;

        ConnectionType(boolean isClient, boolean isServer) {
            this.isClient = isClient;
            this.isServer = isServer;
        }
    }
}
