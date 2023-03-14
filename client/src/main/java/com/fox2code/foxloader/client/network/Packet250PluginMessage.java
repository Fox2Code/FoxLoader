package com.fox2code.foxloader.client.network;

import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.packets.NetHandler;
import net.minecraft.src.client.packets.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Packet250PluginMessage extends Packet {
    public static boolean isSupported;
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
        } else if (modId.equals("foxloader")) {
            if (!Packet250PluginMessage.isSupported) {
                System.out.println("Connected to modded server!");
                Packet250PluginMessage.isSupported = true;
            }
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
        NetworkPlayer networkPlayer = (NetworkPlayer)
                Minecraft.getInstance().thePlayer;
        ModContainer modContainer = ModLoader.getModContainer(modId);
        if (networkPlayer != null && modContainer != null && data != null) {
            modContainer.notifyReceiveServerPacket(networkPlayer, data);
        }
    }

    @Override
    public int getPacketSize() {
        return modId.length() + data.length + 4;
    }
}
