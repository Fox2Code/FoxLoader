package com.fox2code.foxloader.loader.packet;

import com.fox2code.foxloader.registry.RegistryEntry;
import net.minecraft.common.networking.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class ServerRegistry extends FoxPacket {
    public static final int SERVER_REGISTRY_VERSION = 0;
    public static final int SERVER_REGISTRY_VERSION_BACK_COMPAT = 0;
    public HashMap<String, RegistryEntry> registryEntries;
    public HashMap<String, RegistryEntry> entityEntries;
    public HashMap<String, String> metadata;

    public ServerRegistry() {
        super(1, false);
    }

    public ServerRegistry(HashMap<String, RegistryEntry> registryEntries,
                          HashMap<String, RegistryEntry> entityEntries,
                          HashMap<String, String> metadata) {
        super(1, false);
        this.registryEntries = registryEntries;
        this.entityEntries = entityEntries;
        this.metadata = metadata;
    }

    @Override
    public void readData(DataInputStream dataInputStream) throws IOException {
        int serverRegistryVersion = dataInputStream.readUnsignedShort();
        int serverRegistryVersionBackCompat = dataInputStream.readUnsignedShort();
        if (serverRegistryVersionBackCompat > SERVER_REGISTRY_VERSION) {
            throw new IOException("Back compat doesn't allow us to read packet: " + serverRegistryVersionBackCompat);
        }
        registryEntries = RegistryEntry.readEntries(dataInputStream);
        entityEntries = RegistryEntry.readEntries(dataInputStream);
        int entries = dataInputStream.readUnsignedShort();
        metadata = new HashMap<>(entries);
        while (entries-->0) {
            metadata.put(Packet.readString(dataInputStream, Short.MAX_VALUE),
                    Packet.readString(dataInputStream, Short.MAX_VALUE));
        }
    }

    @Override
    public void writeData(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeShort(SERVER_REGISTRY_VERSION);
        dataOutputStream.writeShort(SERVER_REGISTRY_VERSION_BACK_COMPAT);
        RegistryEntry.writeEntries(dataOutputStream, this.registryEntries);
        RegistryEntry.writeEntries(dataOutputStream, this.entityEntries);
        dataOutputStream.writeShort(this.metadata.size());
        for (Map.Entry<String, String> metadata : this.metadata.entrySet()) {
            Packet.writeString(metadata.getKey(), dataOutputStream);
            Packet.writeString(metadata.getValue(), dataOutputStream);
        }
    }
}
