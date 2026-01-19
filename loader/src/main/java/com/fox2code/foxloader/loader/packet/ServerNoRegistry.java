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
