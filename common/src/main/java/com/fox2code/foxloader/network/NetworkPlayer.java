package com.fox2code.foxloader.network;

import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.registry.RegisteredCommandSender;
import com.fox2code.foxloader.registry.RegisteredEntityLiving;
import com.fox2code.foxloader.registry.RegisteredItemStack;

public interface NetworkPlayer extends RegisteredEntityLiving, RegisteredCommandSender {
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
     * @return if the remote party has the mod loader,
     * always return true for a single player world.
     */
    default boolean hasFoxLoader() { throw new RuntimeException(); }

    /**
     * @return the player name
     */
    default String getPlayerName() { throw new RuntimeException(); }

    /**
     * Will kick the player.
     *
     * @throws IllegalStateException if used client side.
     */
    default void kick(String message) { throw new RuntimeException(); }

    /**
     * Get the network player controller.
     */
    default NetworkPlayerController getNetworkPlayerController() { throw new RuntimeException(); }

    /**
     * @return if the player is currently connected
     */
    default boolean isConnected() { throw new RuntimeException(); }

    /**
     * @return the player current held item or null.
     */
    default RegisteredItemStack getRegisteredHeldItem() { throw new RuntimeException(); }

    /**
     * Call code to switch player between nether and overworld, if
     * the player is in neither, it will send the player to the nether.
     * <p>
     * This will also make a new nether portal if needed
     */
    default void sendPlayerThroughPortalRegistered() { throw new RuntimeException(); }

    enum ConnectionType {
        SINGLE_PLAYER(true, true), CLIENT_ONLY(true, false), SERVER_ONLY(false, true);

        public final boolean isClient, isServer;

        ConnectionType(boolean isClient, boolean isServer) {
            this.isClient = isClient;
            this.isServer = isServer;
        }
    }

    interface NetworkPlayerController {
        default boolean hasCreativeModeRegistered() { throw new RuntimeException(); }
        default boolean hasSelection() { throw new RuntimeException(); }
        default int getMinX() { throw new RuntimeException(); }
        default int getMaxX() { throw new RuntimeException(); }
        default int getMinY() { throw new RuntimeException(); }
        default int getMaxY() { throw new RuntimeException(); }
        default int getMinZ() { throw new RuntimeException(); }
        default int getMaxZ() { throw new RuntimeException(); }
    }
}
