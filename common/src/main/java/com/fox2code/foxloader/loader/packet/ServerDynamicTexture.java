package com.fox2code.foxloader.loader.packet;

import com.fox2code.foxloader.launcher.utils.BufferedImageHelper;
import com.fox2code.foxloader.loader.ModLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ServerDynamicTexture extends FoxPacket {
    private static final int TEX_LEN = 16 * 16;
    public static final int SERVER_DYN_MAX_ID = 32;

    public int slot;
    public int[] texture;

    public ServerDynamicTexture() {
        super(1, false);
        this.texture = new int[TEX_LEN];
    }

    public ServerDynamicTexture(int slot, int[] texture) {
        super(1, false);
        if (texture.length != TEX_LEN)
            throw new IllegalArgumentException();
        this.slot = slot;
        this.texture = texture;
    }

    @Override
    public void readData(DataInputStream dataInputStream) throws IOException {
        // In the future we may add more formats for server side content
        if (dataInputStream.readUnsignedByte() != 0) return;
        this.slot = dataInputStream.readUnsignedByte();
        for (int i = 0; i < TEX_LEN; i++) {
            this.texture[i] = dataInputStream.readInt();
        }
    }

    @Override
    public void writeData(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeByte(0);
        dataOutputStream.writeByte(this.slot);
        for (int i = 0; i < TEX_LEN; i++) {
            dataOutputStream.writeInt(this.texture[i]);
        }
    }

    public static List<ServerDynamicTexture> readFromWorld(File file) throws IOException {
        return Helper.readFromWorld(file);
    }

    private static class Helper {
        static List<ServerDynamicTexture> readFromWorld(File file) throws IOException {
            File foxLoader = new File(file, "foxloader");
            File content = new File(foxLoader, "content");
            if (!content.isDirectory() && !content.mkdirs())
                return Collections.emptyList();
            ArrayList<ServerDynamicTexture> serverDynamicTextures = new ArrayList<>();
            for (int i = 0; i < SERVER_DYN_MAX_ID; i++) {
                File contentPng = new File(content, i + ".png");
                if (contentPng.exists()) {
                    BufferedImage bufferedImage = ImageIO.read(contentPng);
                    if (bufferedImage.getWidth() != 16 ||
                            bufferedImage.getHeight() != 16) {
                        ModLoader.getModLoaderLogger().warning("Invalid dynamic content image size: " +
                                bufferedImage.getWidth() + "x" + bufferedImage.getHeight() + " (Expected 16x16)");
                    } else {
                        serverDynamicTextures.add(new ServerDynamicTexture(i,
                                BufferedImageHelper.toSafe16x16Data(bufferedImage)));
                    }
                }
            }
            return serverDynamicTextures.isEmpty() ? Collections.emptyList() :
                    Collections.unmodifiableList(serverDynamicTextures);
        }
    }
}
