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

import com.fox2code.foxloader.container.ContainerManager;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ModLoaderInit;
import com.fox2code.foxloader.network.SidedMetadataAPI;
import com.fox2code.foxloader.registry.GameRegistry;
import com.fox2code.foxloader.server.NetworkKickHelper;
import com.fox2code.foxloader.utils.io.IOUtils;
import net.minecraft.common.networking.NetworkManager;
import net.minecraft.common.networking.Packet250PluginMessage;
import net.minecraft.common.networking.Packet255KickDisconnect;
import net.minecraft.server.networking.NetLoginHandler;

import java.io.*;
import java.util.logging.Level;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public final class LoaderNetworkManager {
    public static boolean strictNetworking = true;

    public static void executeClientPacketData(NetworkManager networkConnection, byte[] data) {
        try (DataInputStream dataInputStream =
                     new DataInputStream(new ByteArrayInputStream(data))) {
            int packetId = dataInputStream.readUnsignedByte();
            if (FoxLauncher.DEVELOPING_FOXLOADER) {
                ModLoaderInit.getModLoaderLogger().info("Executing client packet id: " + packetId);
            }
            //noinspection SwitchStatementWithTooFewBranches
            switch (packetId) {
                case 0:
                    if (networkConnection.flHelloHandler != null ||
                            !(networkConnection.getNetHandler() instanceof NetLoginHandler)) {
                        if (strictNetworking) {
                            networkConnection.addToSendQueue(new Packet255KickDisconnect("Outdated FoxLoader"));
                            networkConnection.serverShutdown();
                        }
                        return;
                    }
                    ClientHello clientHello = new ClientHello();
                    clientHello.readData(dataInputStream);
                    networkConnection.flHelloHandler = clientHello;
                    break;
            }
        } catch (Exception e) {
            ModLoaderInit.getModLoaderLogger().log(Level.WARNING, "Failed to read FoxLoader packet", e);
            if (FoxLauncher.DEVELOPING_FOXLOADER) {
                ModLoaderInit.getModLoaderLogger().info("Packet dump: " + IOUtils.toHex(data));
            }
            NetworkKickHelper.kickPlayer(networkConnection, "Server failed to read FoxLoader packet!");
        }
    }

    public static void sendClientPacketData(NetworkManager networkPlayer, FoxPacket foxPacket) {
        if (!foxPacket.client) {
            throw new IllegalArgumentException("Trying to send  " +
                    foxPacket.getClass().getSimpleName() + " as a client packet");
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeByte(foxPacket.id);
            foxPacket.writeData(dataOutputStream);
        } catch (IOException ignored) {}
        networkPlayer.addToSendQueue(new Packet250PluginMessage("foxloader", byteArrayOutputStream.toByteArray()));
    }

    public static void executeServerPacketData(NetworkManager networkConnection, byte[] data) {
        InputStream inputStream = new ByteArrayInputStream(data);
        try {
            int compressed = inputStream.read();
            ModLoaderInit.getModLoaderLogger().info("Compression: " + compressed);
            switch (compressed) {
                case 0:
                    break;
                case 1:
                    inputStream = new GZIPInputStream(inputStream);
                    break;
                case 2:
                    inputStream = new InflaterInputStream(inputStream);
                    break;
                default:
                    ModLoaderInit.getModLoaderLogger().log(Level.WARNING, "Unknown compression: " + compressed);
                    return;
            }
        } catch (IOException e) {
            ModLoaderInit.getModLoaderLogger().log(Level.SEVERE, "Failed to read server packet", e);
        }
        try (DataInputStream dataInputStream =
                     new DataInputStream(inputStream)) {
            int packetId = dataInputStream.readUnsignedByte();
            if (FoxLauncher.DEVELOPING_FOXLOADER) {
                ModLoaderInit.getModLoaderLogger().info("Executing server packet id: " + packetId);
            }

            switch (packetId) {
                case 0: {
                    ServerHello serverHello = new ServerHello();
                    serverHello.readData(dataInputStream);
                    sendClientPacketData(networkConnection,
                            new ClientHello(serverHello.getClientHelloVersion()));
                    break;
                }
                case 1: {
                    ServerRegistry serverRegistry = new ServerRegistry();
                    serverRegistry.readData(dataInputStream);
                    GameRegistry.Internal.initializeRemoteMappings(serverRegistry);
                    SidedMetadataAPI.Internal.setActiveMetaData(serverRegistry.metadata);
                    break;
                }
                case 2: {
                    ServerNoRegistry serverNoRegistry = new ServerNoRegistry();
                    serverNoRegistry.readData(dataInputStream);
                    GameRegistry.Internal.resetMappingsAsVanillaFL();
                    SidedMetadataAPI.Internal.setActiveMetaData(serverNoRegistry.metadata);
                    break;
                }
                case 3: {
                    ServerOpenContainer serverOpenContainer = new ServerOpenContainer();
                    serverOpenContainer.readData(dataInputStream);
                    ContainerManager.Internal.onReceivingContainerPacket(serverOpenContainer);
                    break;
                }
            }
        } catch (IOException e) {
            ModLoaderInit.getModLoaderLogger().log(Level.SEVERE, "Failed to read server packet", e);
        }
    }

    public static void sendServerPacketData(NetworkManager networkPlayer, FoxPacket foxPacket) {
        if (foxPacket.client) {
            throw new IllegalArgumentException("Trying to send  " +
                    foxPacket.getClass().getSimpleName() + " as a server packet");
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeByte(0); // Compression
            dataOutputStream.writeByte(foxPacket.id);
            foxPacket.writeData(dataOutputStream);
        } catch (IOException ignored) {}
        networkPlayer.addToSendQueue(new Packet250PluginMessage(
                "foxloader", byteArrayOutputStream.toByteArray()));
    }

    public static byte[] compileServerPacketData(FoxPacket foxPacket, int compression) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream headerOutputStream = new DataOutputStream(byteArrayOutputStream);
        OutputStream outputStream = byteArrayOutputStream;
        try {
            headerOutputStream.writeByte(compression);
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
        } catch (IOException e) {
            throw new RuntimeException("Failed to setup compression stream.", e);
        }

        try (DataOutputStream dataOutputStream = new DataOutputStream(outputStream)) {
            dataOutputStream.writeByte(foxPacket.id);
            foxPacket.writeData(dataOutputStream);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return byteArrayOutputStream.toByteArray();
    }
}
