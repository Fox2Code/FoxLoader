package com.fox2code.foxloader.registry;

import net.minecraft.common.networking.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

public final class RegistryEntry {
    public final short realId;
    public final String name;

    public RegistryEntry(short realId, String name) {
        this.realId = realId;
        this.name = name;
    }

    public RegistryEntry(DataInputStream dataInputStream) throws IOException {
        this.realId = dataInputStream.readShort();
        this.name = Packet.readString(dataInputStream, 128);
    }

    public void write(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeShort(this.realId);
        Packet.writeString(this.name, dataOutputStream);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegistryEntry that = (RegistryEntry) o;
        return this.realId == that.realId && this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = this.realId;
        result = 31 * result + this.name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RegistryEntry{" +
                "realId=" + realId +
                ", name='" + name + '\'' +
                '}';
    }


    public static HashMap<String, RegistryEntry> readEntries(DataInputStream dataInputStream) throws IOException {
        int entriesCount = dataInputStream.readUnsignedShort();
        HashMap<String, RegistryEntry> entries = new HashMap<>(entriesCount);
        while (entriesCount-- > 0) {
            RegistryEntry registryEntry = new RegistryEntry(dataInputStream);
            entries.put(registryEntry.name, registryEntry);
        }
        return entries;
    }

    public static void writeEntries(DataOutputStream dataOutputStream,
                                     HashMap<String, RegistryEntry> registryEntries) throws IOException {
        if (registryEntries == null) {
            dataOutputStream.writeShort(0);
            return;
        }
        dataOutputStream.writeShort(registryEntries.size());
        for (RegistryEntry registryEntry : registryEntries.values()) {
            registryEntry.write(dataOutputStream);
        }
    }
}
