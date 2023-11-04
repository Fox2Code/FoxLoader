package com.fox2code.foxloader.server.network;

public interface NetworkPlayerImpl {
    void notifyHasFoxLoader();
    void sendNetworkDataRaw(String modContainer, byte[] data);
    void notifyClientHello();
    boolean hasClientHello();
}
