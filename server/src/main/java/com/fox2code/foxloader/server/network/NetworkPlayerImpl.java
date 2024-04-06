package com.fox2code.foxloader.server.network;

import net.minecraft.src.server.packets.Packet250PluginMessage;

public interface NetworkPlayerImpl {
    void notifyHasFoxLoader();
    void sendNetworkDataRaw(String modContainer, byte[] data);
    void notifyClientHello();
    boolean hasClientHello();
    void handlePreemptiveData(Packet250PluginMessage pluginMessage);
}
