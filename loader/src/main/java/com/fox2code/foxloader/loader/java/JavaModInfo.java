package com.fox2code.foxloader.loader.java;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ModInfo;
import com.fox2code.foxloader.utils.io.JarUtils;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public final class JavaModInfo extends ModInfo {
    private static final Attributes.Name MOD_CLASS_TRANSFORMER = new Attributes.Name("ModClassTransformer");
    private static final Attributes.Name MOD_LOADING_PLUGIN = new Attributes.Name("ModLoadingPlugin");
    private static final Attributes.Name MOD_JITPACK = new Attributes.Name("ModJitPack");
    private static final Attributes.Name MOD_MIXIN = new Attributes.Name("ModMixin");
    private static final Attributes.Name MOD_MAIN = new Attributes.Name("ModMain");
    private static final Attributes.Name FOR_FOX_LOADER_VERSION = new Attributes.Name("For-FoxLoader-Version");
    public static final JavaModInfo FOX_LOADER_MOD_INFO;

    static {
        try {
            FOX_LOADER_MOD_INFO = new JavaModInfo(
                    FoxLauncher.foxLoaderFile, null, "foxloader", "FoxLoader", BuildConfig.FOXLOADER_VERSION,
                    "ReIndev mod loader with foxes!!!", "Fox2Code", "assets/foxloader/icon.png",
                    "any", false, Long.MAX_VALUE, "com.fox2code.foxloader.loader.ModLoader");
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public final String forFoxLoaderVersion;
    public final String classTransformer;
    public final String loadingPlugin;
    public final String jitPack;
    public final String mixin;
    public final String main;
    public final boolean builtin;
    final boolean explicitMixin;

    JavaModInfo(File file, String jarPath) throws IOException {
        this(file, jarPath, JarUtils.getManifest(file, jarPath).getMainAttributes());
    }

    private JavaModInfo(File file, String jarPath, Attributes mainAttributes) throws IOException {
        super(file, jarPath, mainAttributes);
        this.forFoxLoaderVersion = mainAttributes.getValue(FOR_FOX_LOADER_VERSION);
        this.classTransformer = mainAttributes.getValue(MOD_CLASS_TRANSFORMER);
        this.loadingPlugin = mainAttributes.getValue(MOD_LOADING_PLUGIN);
        this.jitPack = mainAttributes.getValue(MOD_JITPACK);
        String modMixin = mainAttributes.getValue(MOD_MIXIN);
        this.mixin = modMixin != null ? modMixin :
                this.id + ".mixins.json";
        this.explicitMixin = modMixin != null;
        this.main = mainAttributes.getValue(MOD_MAIN);
        this.builtin = false;
    }

    JavaModInfo(File file, String jarPath, String id, String name, String version, String description, String authors, String iconPath,
                        String environment, boolean unofficial, long loadOrderPriority, String main) throws IOException {
        super(file, jarPath, id, name, version, description, authors, iconPath, environment, unofficial, loadOrderPriority);
        this.forFoxLoaderVersion = BuildConfig.FOXLOADER_VERSION;
        this.classTransformer = null;
        this.loadingPlugin = null;
        this.jitPack = null;
        if ("foxloader".equals(id)) {
            this.mixin = "foxloader.mixins.json";
            this.explicitMixin = true;
        } else {
            this.mixin = null;
            this.explicitMixin = false;
        }
        this.main = main;
        this.builtin = true;
    }

    @Override
    public final boolean isJavaArchive() {
        return true;
    }
}
