/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
