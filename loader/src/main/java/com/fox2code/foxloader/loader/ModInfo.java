package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.launcher.FileInfo;
import com.fox2code.flexver.FlexVer;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.logging.Level;

public class ModInfo extends FileInfo implements IMixinConfigSource {
    private static final Attributes.Name MOD_ID = new Attributes.Name("ModId");
    private static final Attributes.Name MOD_NAME = new Attributes.Name("ModName");
    private static final Attributes.Name MOD_VERSION = new Attributes.Name("ModVersion");
    private static final Attributes.Name MOD_DESC = new Attributes.Name("ModDesc");
    private static final Attributes.Name MOD_AUTHORS = new Attributes.Name("ModAuthors");
    private static final Attributes.Name MOD_ICON = new Attributes.Name("ModIcon");
    private static final Attributes.Name MOD_ENVIRONMENT = new Attributes.Name("ModEnvironment");
    private static final Attributes.Name UNOFFICIAL = new Attributes.Name("Unofficial");
    private static final Attributes.Name LOAD_ORDER_PRIORITY = new Attributes.Name("LoadOrderPriority");

    public final String id;
    public final String name;
    public final String version;
    public final FlexVer flexver;
    public final String description;
    public final String authors;
    public final String iconPath;
    public final String environment;
    public final boolean unofficial;
    public final long loadOrderPriority;

    public ModInfo(File file, String jarPath, String id, String name, String version, String description, String authors,
                   String iconPath, String environment, boolean unofficial, long loadOrderPriority) throws IOException {
        super(file, jarPath);
        this.id = id;
        this.name = name;
        this.version = version;
        this.flexver = FlexVer.parse(version);
        this.description = description;
        this.authors = authors;
        this.iconPath = iconPath;
        this.environment = environment;
        this.unofficial = unofficial;
        this.loadOrderPriority = loadOrderPriority;
    }

    public ModInfo(DataInputStream dataInputStream) throws IOException {
        super(dataInputStream);
        this.id = readStringSafest(dataInputStream, "");
        this.name = readStringSafest(dataInputStream, this.id);
        this.version = readStringSafest(dataInputStream, "undefined");
        this.flexver = FlexVer.parse(this.version);
        this.description = readStringSafest(dataInputStream, "Missing mod description");
        this.authors = readStringSafest(dataInputStream, "Unknown");
        this.iconPath = readStringSafest(dataInputStream, null);
        this.environment = readStringSafest(dataInputStream, null);
        this.unofficial = dataInputStream.readBoolean();
        this.loadOrderPriority = dataInputStream.readLong();
    }

    protected ModInfo(File file, String jarPath, Attributes mainAttributes) throws IOException {
        super(file, jarPath);
        this.id = mainAttributes.getValue(MOD_ID);
        String name = mainAttributes.getValue(MOD_NAME);
        this.name = name != null ? name : this.id;
        String version = mainAttributes.getValue(MOD_VERSION);
        if (version == null || version.isEmpty()) {
            version = "1.0.0";
        }
        this.version = version;
        this.flexver = FlexVer.parse(version);
        String description = mainAttributes.getValue(MOD_DESC);
        description = description != null ?
                description.replace('\t', '\n') :
                "Missing mod description";
        if (description.indexOf('\r') != -1) {
            // Emulate modern java behaviour of not wanting to deal with carriage returns
            throw new IOException("invalid manifest format (line 0)");
        }
        this.description = description;
        String authors = mainAttributes.getValue(MOD_AUTHORS);
        this.authors = authors != null ? authors : "Unknown";
        this.iconPath = mainAttributes.getValue(MOD_ICON);
        String environment = mainAttributes.getValue(MOD_ENVIRONMENT);
        this.environment = environment != null &&
                !environment.isEmpty() ? environment : "any";
        this.unofficial = Boolean.parseBoolean(mainAttributes.getValue(UNOFFICIAL));
        String priorityText = mainAttributes.getValue(LOAD_ORDER_PRIORITY);
        long priority = 0;
        if (priorityText != null && !priorityText.isEmpty()) {
            try {
                priority = Long.parseLong(priorityText);
            } catch (Exception ignored) {
            }
        }
        this.loadOrderPriority = priority;
    }

    @Override
    public final String getId() {
        return this.id;
    }

    @Override
    public final String getDescription() {
        return this.description;
    }
}
