package com.fox2code.foxloader.network;

import com.fox2code.foxloader.loader.ModContainer;

public interface NetworkConnection {
    /**
     * Send network data to remote player
     */
    default void sendNetworkData(ModContainer modContainer, byte[] data) { throw new RuntimeException(); }

    /**
     * Get the network player is available
     */
    default NetworkPlayer getNetworkPlayer() { throw new RuntimeException(); }

    /**
     * @return if the remote party has fox loader.
     */
    default boolean hasFoxLoader() { throw new RuntimeException(); }

    /**
     * @return if the player is currently connected
     */
    default boolean isConnected() { throw new RuntimeException(); }

    /**
     * Will kick the player.
     */
    default void kick(String message) { throw new RuntimeException(); }
}
