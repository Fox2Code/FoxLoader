package com.fox2code.foxloader.loader.packet;

import net.minecraft.common.networking.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Indicate no new content was registered, this packet doesn't change across FoxLoader versions.
 */
public final class ServerNoRegistry extends FoxPacket {
    public HashMap<String, String> metadata;

    public ServerNoRegistry() {
        super(2, false);
    }

    public ServerNoRegistry(HashMap<String, String> metadata) {
        super(2, false);
        this.metadata = metadata;
    }

    @Override
    public void readData(DataInputStream dataInputStream) throws IOException {
        int entries = dataInputStream.readUnsignedShort();
        metadata = new HashMap<>(entries);
        while (entries-->0) {
            metadata.put(Packet.readString(dataInputStream, Short.MAX_VALUE),
                    Packet.readString(dataInputStream, Short.MAX_VALUE));
        }
    }

    @Override
    public void writeData(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeShort(this.metadata.size());
        for (Map.Entry<String, String> metadata : this.metadata.entrySet()) {
            Packet.writeString(metadata.getKey(), dataOutputStream);
            Packet.writeString(metadata.getValue(), dataOutputStream);
        }
    }
}
