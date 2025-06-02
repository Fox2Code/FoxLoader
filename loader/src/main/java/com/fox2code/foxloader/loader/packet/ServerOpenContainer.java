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
        if (dataInputStream.readBoolean()) {
            this.x = dataInputStream.readInt();
            this.y = dataInputStream.readInt();
            this.z = dataInputStream.readInt();
        } else {
            this.x = 0;
            this.y = 0;
            this.z = 0;
        }
    }

    @Override
    public void writeData(DataOutputStream dataOutputStream) throws IOException {
        Packet.writeString(this.containerID, dataOutputStream);
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
