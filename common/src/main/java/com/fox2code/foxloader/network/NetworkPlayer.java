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
    default ConnectionType getConnectionType() { throw new RuntimeException(); }

    /**
     * Send network data to remote player, if {@link #hasFoxLoader()} return false,
     * this method do not perform any specific actions.
     */
    default void sendNetworkData(ModContainer modContainer, byte[] data) { throw new RuntimeException(); }

    /**
     * Send/Display chat message to the user screen.
     */
    default void displayChatMessage(String chatMessage) { throw new RuntimeException(); }

    /**
     * @return if the remote party has the mod loader,
     * always return true for a single player world.
     */
    default boolean hasFoxLoader() { throw new RuntimeException(); }

    /**
     * @return the player name
     */
    default String getPlayerName() { throw new RuntimeException(); }

    /**
     * @return if the player as operator permission.
     *
     * Always false client-side when connected to a server.
     */
    default boolean isOperator() { throw new RuntimeException(); }

    enum ConnectionType {
        SINGLE_PLAYER(true, true), CLIENT_ONLY(true, false), SERVER_ONLY(false, true);

        public final boolean isClient, isServer;

        ConnectionType(boolean isClient, boolean isServer) {
            this.isClient = isClient;
            this.isServer = isServer;
        }
    }
}
