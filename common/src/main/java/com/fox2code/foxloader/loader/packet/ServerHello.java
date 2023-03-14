package com.fox2code.foxloader.loader.packet;

import com.fox2code.foxloader.registry.RegistryEntry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

public final class ServerHello extends FoxPacket {
    public static final int SERVER_HELLO_VERSION = 0;

    public HashMap<String, RegistryEntry> registryEntries;

    public ServerHello() {
        super(0, false);
    }

    public ServerHello(HashMap<String, RegistryEntry> registryEntries) {
        super(0, false);
        this.registryEntries = registryEntries;
    }

    @Override
    public void readData(DataInputStream dataInputStream) throws IOException {
        int serverHelloVersion = dataInputStream.readUnsignedShort();
        if (serverHelloVersion > SERVER_HELLO_VERSION) {
            throw new RuntimeException("Client is out of date, please update FoxLoader");
        }
        int entries = dataInputStream.readUnsignedShort();
        registryEntries = new HashMap<>(entries);
        while (entries-->0) {
            RegistryEntry registryEntry = new RegistryEntry(
                    dataInputStream.readShort(), dataInputStream.readShort(),
                    dataInputStream.readUTF(), dataInputStream.readUTF());
            registryEntries.put(registryEntry.name, registryEntry);
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
    }
}
