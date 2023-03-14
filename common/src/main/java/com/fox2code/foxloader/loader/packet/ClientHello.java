package com.fox2code.foxloader.loader.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

public final class ClientHello extends FoxPacket {
    public static final int CLIENT_HELLO_VERSION = 0;

    public ArrayList<ClientModData> clientModsData;

    public ClientHello() {
        super(0, true);
    }

    public ClientHello(ArrayList<ClientModData> clientModsData) {
        super(0, true);
        this.clientModsData = clientModsData;
    }

    @Override
    public void readData(DataInputStream dataInputStream) throws IOException {
        int version = dataInputStream.readUnsignedShort();
        int count = dataInputStream.readUnsignedShort();
        this.clientModsData = new ArrayList<>(count);
        while (count-->0) {
            this.clientModsData.add(new ClientModData(dataInputStream));
        }
    }

    @Override
    public void writeData(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeShort(CLIENT_HELLO_VERSION);
        dataOutputStream.writeShort(this.clientModsData.size());
        for (ClientModData clientModData : this.clientModsData) {
            clientModData.write(dataOutputStream);
        }
    }

    public static final class ClientModData {
        public final String modId, version, name;
        public final byte[] sha256;

        public ClientModData(String modId, byte[] sha256, String version, String name) {
            this.modId = modId;
            this.sha256 = sha256;
            this.version = version;
            this.name = name;
        }

        public ClientModData(DataInputStream dataInputStream) throws IOException {
            this.modId = dataInputStream.readUTF();
            this.sha256 = new byte[32];
            if (dataInputStream.read(this.sha256) != 32) {
                throw new EOFException();
            }
            this.version = dataInputStream.readUTF();
            String name = dataInputStream.readUTF();
            if (name.isEmpty()) name = this.modId;
            this.name = name;
        }

        public void write(DataOutputStream dataOutputStream) throws IOException {
            dataOutputStream.writeUTF(this.modId);
            dataOutputStream.write(this.sha256);
            dataOutputStream.writeUTF(this.version);
            dataOutputStream.writeUTF( // Only send name if necessary.
                    this.modId.equals(this.name) ? "": this.name);
        }

        public boolean isCoreMod() {
            return this.version.isEmpty();
        }
    }
}
