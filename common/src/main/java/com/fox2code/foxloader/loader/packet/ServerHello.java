package com.fox2code.foxloader.loader.packet;

import com.fox2code.foxloader.registry.RegistryEntry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class ServerHello extends FoxPacket {
    public static final int SERVER_HELLO_VERSION = 0;
    private static final int SERVER_HELLO_VERSION_NEXT = 1;

    public HashMap<String, RegistryEntry> registryEntries;
    public HashMap<String, String> metadata;

    public ServerHello() {
        super(0, false);
    }

    public ServerHello(HashMap<String, RegistryEntry> registryEntries,
                       HashMap<String, String> metadata) {
        super(0, false);
        this.registryEntries = registryEntries;
        this.metadata = metadata;
    }

    @Override
    public void readData(DataInputStream dataInputStream) throws IOException {
        int serverHelloVersion = dataInputStream.readUnsignedShort();
        if (serverHelloVersion >= SERVER_HELLO_VERSION_NEXT &&
                // Next field is how much backward compatible is the packet
                dataInputStream.readUnsignedShort() > SERVER_HELLO_VERSION_NEXT) {
            throw new RuntimeException("Client is critically out of date, please update FoxLoader");
        }
        int entries = dataInputStream.readUnsignedShort();
        registryEntries = new HashMap<>(entries);
        while (entries-->0) {
            RegistryEntry registryEntry = new RegistryEntry(
                    dataInputStream.readShort(), dataInputStream.readShort(),
                    dataInputStream.readUTF(), dataInputStream.readUTF());
            registryEntries.put(registryEntry.name, registryEntry);
        }
        if (dataInputStream.available() == 0) {
            metadata = new HashMap<>();
            return;
        }
        entries = dataInputStream.readUnsignedShort();
        metadata = new HashMap<>();
        while (entries-->0) {
            metadata.put(dataInputStream.readUTF(), dataInputStream.readUTF());
        }
    }

    @Override
    public void writeData(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeShort(SERVER_HELLO_VERSION);
        dataOutputStream.writeShort(this.registryEntries.size());
        for (RegistryEntry registryEntry : registryEntries.values()) {
            dataOutputStream.writeShort(registryEntry.realId);
            dataOutputStream.writeByte(registryEntry.fallbackId);
            dataOutputStream.writeUTF(registryEntry.name);
            dataOutputStream.writeUTF(registryEntry.fallbackDisplayName);
        }
        dataOutputStream.writeShort(this.metadata.size());
        for (Map.Entry<String, String> metadata : this.metadata.entrySet()) {
            dataOutputStream.writeUTF(metadata.getKey());
            dataOutputStream.writeUTF(metadata.getValue());
        }
    }
}
