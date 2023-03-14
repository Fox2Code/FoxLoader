package com.fox2code.foxloader.server.network;

import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.GameRegistryServer;
import net.minecraft.src.server.packets.NetHandler;
import net.minecraft.src.server.packets.NetServerHandler;
import net.minecraft.src.server.packets.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Packet250PluginMessage extends Packet {
    public String modId;
    public byte[] data;

    public Packet250PluginMessage() {}

    public Packet250PluginMessage(String modId, byte[] data) {
        if (data.length > Short.MAX_VALUE) {
            throw new IllegalArgumentException("data is too large, size is " +
                    data.length + " when max is " + Short.MAX_VALUE);
        }
        this.modId = modId;
        this.data = data;
    }

    @Override
    public void readPacketData(DataInputStream dataInputStream) throws IOException {
        modId = readString(dataInputStream, ModLoader.MAX_MOD_ID_LENGTH);
        int len = dataInputStream.readShort();
        if (len < 0 || dataInputStream.read(this.data = new byte[len]) != len) {
            data = null; // Skip malformed packets
        }
    }

    @Override
    public void writePacketData(DataOutputStream dataOutputStream) throws IOException {
        writeString(modId, dataOutputStream);
        dataOutputStream.writeShort(data.length);
        dataOutputStream.write(data);
    }

    @Override
    public void processPacket(NetHandler netHandler) {
        if (!(netHandler instanceof NetServerHandlerAccessor)) return;
        NetworkPlayer networkPlayer = (NetworkPlayer)
                ((NetServerHandlerAccessor) netHandler).getPlayerEntity();
        if (networkPlayer == null || !networkPlayer.hasFoxLoader()) {
            return; // Stop client that are not modded from sending modded packets
        }
        ModContainer modContainer = ModLoader.getModContainer(modId);
        if (modContainer != null && data != null) {
            modContainer.notifyReceiveClientPacket(networkPlayer, data);
        }
    }

    @Override
    public int getPacketSize() {
        return modId.length() + data.length + 4;
    }
}
