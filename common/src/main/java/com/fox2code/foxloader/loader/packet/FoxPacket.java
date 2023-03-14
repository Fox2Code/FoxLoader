package com.fox2code.foxloader.loader.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class FoxPacket {
    public final int id;
    // Is packet sent by client?
    public final boolean client;

    FoxPacket(int id, boolean client) {
        this.id = id;
        this.client = client;
    }

    public abstract void readData(DataInputStream dataInputStream) throws IOException;

    public abstract void writeData(DataOutputStream dataInputStream) throws IOException;
}
