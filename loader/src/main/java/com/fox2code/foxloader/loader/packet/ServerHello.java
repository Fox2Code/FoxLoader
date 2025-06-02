package com.fox2code.foxloader.loader.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class ServerHello extends FoxPacket {
    private int clientHelloVersion;

    public ServerHello() {
        super(0, false);
        this.clientHelloVersion = ClientHello.CLIENT_HELLO_VERSION;
    }

    public int getClientHelloVersion() {
        return this.clientHelloVersion;
    }

    @Override
    public void readData(DataInputStream dataInputStream) throws IOException {
        this.clientHelloVersion = dataInputStream.readInt();
    }

    @Override
    public void writeData(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(this.clientHelloVersion);
    }
}
