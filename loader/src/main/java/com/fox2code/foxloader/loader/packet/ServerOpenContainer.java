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

public final class ServerOpenContainer extends FoxPacket {
    public String containerID;
    public int windowID;
    public boolean block;
    public int x, y, z;

    ServerOpenContainer() {
        super(3, false);
    }

    public ServerOpenContainer(String containerID, int windowID) {
        super(3, false);
        this.containerID = containerID;
        this.windowID = windowID;
        this.block = false;
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    public ServerOpenContainer(String containerID, int windowID, int x, int y, int z) {
        super(3, false);
        this.containerID = containerID;
        this.windowID = windowID;
        this.block = true;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void readData(DataInputStream dataInputStream) throws IOException {
        this.containerID = Packet.readString(dataInputStream, 128);
        this.windowID = dataInputStream.readInt();
        if (dataInputStream.readBoolean()) {
            this.x = dataInputStream.readInt();
            this.y = dataInputStream.readInt();
            this.z = dataInputStream.readInt();
            this.block = true;
        } else {
            this.block = false;
            this.x = 0;
            this.y = 0;
            this.z = 0;
        }
    }

    @Override
    public void writeData(DataOutputStream dataOutputStream) throws IOException {
        Packet.writeString(this.containerID, dataOutputStream);
        dataOutputStream.writeInt(this.windowID);
        if (this.block) {
            dataOutputStream.writeBoolean(true);
            dataOutputStream.writeInt(this.x);
            dataOutputStream.writeInt(this.y);
            dataOutputStream.writeInt(this.z);
        } else {
            dataOutputStream.writeBoolean(false);
        }
    }
}
