package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.launcher.utils.IOUtils;
import com.fox2code.foxloader.launcher.utils.NetUtils;
import com.fox2code.foxloader.loader.packet.ClientHello;
import com.fox2code.foxloader.loader.packet.FoxPacket;
import com.fox2code.foxloader.loader.packet.ServerHello;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.network.io.NetworkDataInputStream;
import com.fox2code.foxloader.network.io.NetworkDataOutputStream;

import java.io.*;
import java.util.logging.Level;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class LoaderNetworkManager {
    static void executeClientPacketData(NetworkPlayer networkPlayer, byte[] data) {
        try (DataInputStream dataInputStream =
                     new NetworkDataInputStream(new ByteArrayInputStream(data))) {
            int packetId = dataInputStream.readUnsignedByte();
            //noinspection SwitchStatementWithTooFewBranches
            switch (packetId) {
                case 0:
                    // Support early version of client hello
                    if (data.length == 1) break;
                    ClientHello clientHello = new ClientHello();
                    clientHello.readData(dataInputStream);
                    ModLoader.foxLoader.getMod()
                            .loaderHandleClientHello(
                                    networkPlayer, clientHello);
                    break;
            }
        } catch (IOException ignored) {}
    }

    static void sendClientPacketData(NetworkPlayer networkPlayer, FoxPacket foxPacket) {
        if (!foxPacket.client) {
            throw new IllegalArgumentException("Trying to send  " +
                    foxPacket.getClass().getSimpleName() + " as a client packet");
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new NetworkDataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeByte(foxPacket.id);
            foxPacket.writeData(dataOutputStream);
        } catch (IOException ignored) {}
        networkPlayer.sendNetworkData(ModLoader.foxLoader, byteArrayOutputStream.toByteArray());
    }

    static void executeServerPacketData(NetworkPlayer networkPlayer, byte[] data) {
        InputStream inputStream = new ByteArrayInputStream(data);
        try {
            int compressed = inputStream.read();
            ModLoader.foxLoader.logger.info("Compression: " + compressed);
            switch (compressed) {
                case 0:
                    break;
                case 1:
                    inputStream = new GZIPInputStream(inputStream);
                    break;
                case 2:
                    inputStream = new DeflaterInputStream(inputStream);
                    break;
                default:
                    ModLoader.foxLoader.logger.log(Level.WARNING, "Unknown compression: " + compressed);
                    return;
            }
        } catch (IOException e) {
            ModLoader.foxLoader.logger.log(Level.SEVERE, "Failed to read server packet", e);
        }
        try (DataInputStream dataInputStream =
                     new NetworkDataInputStream(inputStream)) {
            int packetId = dataInputStream.readUnsignedByte();
            ModLoader.foxLoader.logger.info("PacketID: " + packetId);

            if (packetId == 120) {
                StringBuilder stringBuilder = new StringBuilder();
                try {
                    while (true)
                        stringBuilder.append(Integer.toHexString(dataInputStream.readUnsignedByte())).append(" ");
                } catch (Exception e) {
                    ModLoader.foxLoader.logger.info("PacketOfDeath: " + stringBuilder);
                    stringBuilder.setLength(0);
                    for (byte b : data) {
                        stringBuilder.append(String.format("%02X", b));
                    }
                    ModLoader.foxLoader.logger.info("RAW Data: " + stringBuilder);
                    System.exit(1);
                }
            }
            //noinspection SwitchStatementWithTooFewBranches
            switch (packetId) {
                case 0:
                    ServerHello serverHello = new ServerHello();
                    serverHello.readData(dataInputStream);
                    ModLoader.foxLoader.getMod()
                            .loaderHandleServerHello(
                                    networkPlayer, serverHello);
                    break;
            }
        } catch (IOException e) {
            ModLoader.foxLoader.logger.log(Level.SEVERE, "Failed to read server packet", e);
        }
    }

    static void sendServerPacketData(NetworkPlayer networkPlayer, FoxPacket foxPacket) {
        if (foxPacket.client) {
            throw new IllegalArgumentException("Trying to send  " +
                    foxPacket.getClass().getSimpleName() + " as a server packet");
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new NetworkDataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeByte(0); // Compression
            dataOutputStream.writeByte(foxPacket.id);
            foxPacket.writeData(dataOutputStream);
        } catch (IOException ignored) {}
        networkPlayer.sendNetworkData(ModLoader.foxLoader, byteArrayOutputStream.toByteArray());
    }

    @SuppressWarnings("SameParameterValue")
    static byte[] compileServerPacketData(FoxPacket foxPacket, int compression) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream outputStream = byteArrayOutputStream;
        byteArrayOutputStream.write(compression);
        try {
            switch (compression) {
                case 0:
                    break;
                case 1:
                    outputStream = new GZIPOutputStream(outputStream);
                    break;
                case 2:
                    outputStream = new DeflaterOutputStream(outputStream);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown compression: " + compression);
            }
        } catch (IOException ignored) {}

        try (DataOutputStream dataOutputStream = new DataOutputStream(outputStream)) {
            dataOutputStream.writeByte(foxPacket.id);
            foxPacket.writeData(dataOutputStream);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return byteArrayOutputStream.toByteArray();
    }
}
