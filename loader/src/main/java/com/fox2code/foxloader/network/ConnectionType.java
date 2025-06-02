package com.fox2code.foxloader.network;

public enum ConnectionType {
    SINGLE_PLAYER(true, true), CLIENT_ONLY(true, false), SERVER_ONLY(false, true), NONE(false, false);

    public final boolean isClient, isServer;

    ConnectionType(boolean isClient, boolean isServer) {
        this.isClient = isClient;
        this.isServer = isServer;
    }
}
