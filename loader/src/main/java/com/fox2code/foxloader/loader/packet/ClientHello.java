package com.fox2code.foxloader.loader.packet;

import com.fox2code.foxloader.dependencies.DependencyFileInfo;
import com.fox2code.foxloader.dependencies.DependencyHelper;
import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.FileInfo;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ModInfo;
import net.minecraft.common.networking.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientHello extends FoxPacket {
    public static final int CLIENT_HELLO_VERSION = 0;
    private int clientHelloVersion;
    private int serverRegistryVersion;
    private String foxLoaderVersion;
    private final ArrayList<FileInfo> clientClassPathData;

    public ClientHello() {
        super(0, true);
        this.clientHelloVersion = CLIENT_HELLO_VERSION;
        this.serverRegistryVersion = ServerRegistry.SERVER_REGISTRY_VERSION;
        this.foxLoaderVersion = BuildConfig.FOXLOADER_VERSION;
        this.clientClassPathData = new ArrayList<>(FoxLauncher.getFoxClassLoader().loadingClassPath());
    }

    public ClientHello setClientHelloVersion(int clientHelloVersion) {
        this.clientHelloVersion = Math.min(clientHelloVersion, CLIENT_HELLO_VERSION);
        return this;
    }

    public int getClientHelloVersion() {
        return this.clientHelloVersion;
    }

    public List<FileInfo> getClientClassPathData() {
        return Collections.unmodifiableList(this.clientClassPathData);
    }

    public String getFoxLoaderVersion() {
        return this.foxLoaderVersion;
    }

    @Override
    public void readData(DataInputStream dataInputStream) throws IOException {
        int clientHelloVersion = dataInputStream.readInt();
        if (clientHelloVersion > CLIENT_HELLO_VERSION) {
            throw new RuntimeException(
                    "Client hello version more recent than server: " + clientHelloVersion + " (Code: C)");
        }
        this.clientHelloVersion = clientHelloVersion;
        this.serverRegistryVersion = dataInputStream.readInt();
        this.foxLoaderVersion = Packet.readString(dataInputStream, 256);
        this.clientClassPathData.clear();
        int fileCount = dataInputStream.readUnsignedShort();
        while (fileCount-->0) {
            int type = dataInputStream.readUnsignedByte();
            FileInfo fileInfo;
            switch (type) {
                default: {
                    throw new RuntimeException("Server is outdated??? (Code: C)");
                }
                case 0: {
                    fileInfo = new FileInfo(dataInputStream);
                    break;
                }
                case 1: {
                    fileInfo = new DependencyFileInfo(dataInputStream);
                    break;
                }
                case 2: {
                    fileInfo = new ModInfo(dataInputStream);
                    break;
                }
            }
            this.clientClassPathData.add(fileInfo);
        }
    }

    @Override
    public void writeData(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(CLIENT_HELLO_VERSION);
        dataOutputStream.writeInt(this.serverRegistryVersion);
        dataOutputStream.writeShort(this.clientClassPathData.size());
        byte[] hashCache = new byte[32];
        for (FileInfo fileInfo : this.clientClassPathData) {
            if (fileInfo instanceof ModInfo) {
                ModInfo modInfo = (ModInfo) fileInfo;
                dataOutputStream.writeByte(2);
                writeStringSafest(dataOutputStream, modInfo.jarPath, true);
                fileInfo.emitSha256(hashCache);
                dataOutputStream.write(hashCache);
                writeStringSafest(dataOutputStream, modInfo.fileName, true);
                writeStringSafest(dataOutputStream, modInfo.id, false);
                writeStringSafest(dataOutputStream, modInfo.name, true);
                writeStringSafest(dataOutputStream, modInfo.version, true);
                writeStringSafest(dataOutputStream, modInfo.description, true);
                writeStringSafest(dataOutputStream, modInfo.authors, true);
                writeStringSafest(dataOutputStream, modInfo.iconPath, true);
                writeStringSafest(dataOutputStream, modInfo.environment, true);
                dataOutputStream.writeBoolean(modInfo.unofficial);
                dataOutputStream.writeLong(modInfo.loadOrderPriority);
            } else if (fileInfo instanceof DependencyFileInfo) {
                DependencyHelper.Dependency dependency =
                        ((DependencyFileInfo) fileInfo).getDependency();
                dataOutputStream.writeByte(1);
                writeStringSafest(dataOutputStream, fileInfo.jarPath, true);
                fileInfo.emitSha256(hashCache);
                dataOutputStream.write(hashCache);
                writeStringSafest(dataOutputStream, fileInfo.fileName, true);
                writeStringSafest(dataOutputStream, dependency.name, true);
                writeStringSafest(dataOutputStream, dependency.repository, true);
                writeStringSafest(dataOutputStream, dependency.classCheck, true);
                writeStringSafest(dataOutputStream, dependency.fallbackUrl, true);
                writeStringSafest(dataOutputStream, dependency.sha256Sum, false);
                dataOutputStream.writeShort(dependency.javaSupport);
            } else  {
                dataOutputStream.writeByte(0);
                writeStringSafest(dataOutputStream, fileInfo.jarPath, true);
                fileInfo.emitSha256(hashCache);
                dataOutputStream.write(hashCache);
                writeStringSafest(dataOutputStream, fileInfo.fileName, true);
            }
        }
    }

    private static void writeStringSafest(DataOutputStream dataOutputStream, String data, boolean trim) throws IOException {
        if (data == null || data.isEmpty()) {
            dataOutputStream.writeByte(0);
            return;
        }
        if (data.length() >= 256) {
            if (trim) {
                data = data.substring(0, 255);
            } else {
                throw new IllegalArgumentException("String too long: \"" + data + "\"");
            }
        }
        dataOutputStream.writeByte(data.length());
        dataOutputStream.writeChars(data);
    }
}
