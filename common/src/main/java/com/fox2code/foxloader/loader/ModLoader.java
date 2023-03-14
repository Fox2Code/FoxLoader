package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.launcher.*;
import com.fox2code.foxloader.loader.packet.ServerHello;
import com.fox2code.foxloader.loader.rebuild.ClassDataProvider;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

public class ModLoader {
    public static final boolean TEST_MODE = Boolean.getBoolean("foxloader.test-mode");
    private static final String INJECT_MOD = System.getProperty("foxloader.inject-mod");
    public static final boolean DEV_MODE = Boolean.getBoolean("foxloader.dev-mode");
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final File mods = new File(FoxLauncher.getGameDir(), "mods");
    public static final File modsVersioned = new File(mods, "ReIndev" + BuildConfig.REINDEV_VERSION);
    public static final File coremods = new File(FoxLauncher.getGameDir(), "coremods");
    public static final File config = new File(FoxLauncher.getGameDir(), "config");
    private static final boolean disableSpark = Boolean.getBoolean("foxloader.disable-spark");
    private static boolean launched = false, allModsLoaded = false;
    public static final String FOX_LOADER_MOD_ID = "foxloader";
    public static final String FOX_LOADER_VERSION = BuildConfig.FOXLOADER_VERSION;
    static final ModContainer foxLoader = new ModContainer(
            FoxLauncher.foxLoaderFile, FOX_LOADER_MOD_ID, "FoxLoader", FOX_LOADER_VERSION, false);
    static final ModContainer spark = new ModContainer(null, "spark", "Spark", FOX_LOADER_VERSION, false);
    static final LinkedList<File> coreMods = new LinkedList<>();
    // Use LinkedHashMap to keep track in which order mods were loaded.
    static final LinkedHashMap<String, ModContainer> modContainers = new LinkedHashMap<>();
    static Thread gameThread;
    public static final String FOX_LOADER_HEADER = "\0RFL";
    public static final int MAX_MOD_ID_LENGTH = 32;
    private static final Attributes.Name MOD_ID = new Attributes.Name("ModId");
    private static final Attributes.Name MOD_NAME = new Attributes.Name("ModName");
    private static final Attributes.Name MOD_VERSION = new Attributes.Name("ModVersion");
    private static final Attributes.Name PRE_PATCH = new Attributes.Name("PreClassTransformer");
    private static final Attributes.Name CLIENT_MOD = new Attributes.Name("ClientMod");
    private static final Attributes.Name SERVER_MOD = new Attributes.Name("ServerMod");
    private static final Attributes.Name COMMON_MOD = new Attributes.Name("CommonMod");
    private static final Attributes.Name CLIENT_MIXIN = new Attributes.Name("ClientMixin");
    private static final Attributes.Name SERVER_MIXIN = new Attributes.Name("ServerMixin");
    private static final Attributes.Name COMMON_MIXIN = new Attributes.Name("CommonMixin");
    static final ClassDataProvider classDataProvider;

    static {
        classDataProvider = new ClassDataProvider(FoxLauncher.getFoxClassLoader(), PreLoader::patchInternal);
        FoxLauncher.getFoxClassLoader().installWrappedExtensions(
                new FoxWrappedExtensions(classDataProvider, foxLoader.logger));
        if (FoxLauncher.getFoxClassLoader() != ModLoader.class.getClassLoader()) {
            throw new RuntimeException("Invalid class loader context!");
        }
    }

    static void initializeModdedInstance(boolean client) {
        if (launched) return; launched = true;
        if (!mods.exists() && !mods.mkdirs())
            throw new RuntimeException("Cannot create mods folder");
        if (!modsVersioned.exists() && !modsVersioned.mkdirs())
            throw new RuntimeException("Cannot create versioned mods folder");
        if (!coremods.exists() && !coremods.mkdirs())
            throw new RuntimeException("Cannot create coremods folder");
        modContainers.put(foxLoader.id, foxLoader);
        if (TEST_MODE) {
            foxLoader.logger.info("Skipping mod loading because we are in test mode.");
        } else {
            for (File coremod : Objects.requireNonNull(coremods.listFiles(
                    (dir, name) -> name.endsWith(".zip") || name.endsWith(".jar")))) {
                PreLoader.addCoreMod(coremod);
                coreMods.add(coremod);
            }
            if (!DEV_MODE) {
                PreLoader.loadPrePatches(client);
            }
            for (File mod : Objects.requireNonNull(mods.listFiles(
                    (dir, name) -> name.endsWith(".jar")))) {
                loadModContainerFrom(mod, false);
            }
            for (File mod : Objects.requireNonNull(modsVersioned.listFiles(
                    (dir, name) -> name.endsWith(".jar")))) {
                loadModContainerFrom(mod, false);
            }
        }
        // Inject mod is used by the gradle plugin to load dev mod
        if (DEV_MODE && INJECT_MOD != null && !INJECT_MOD.isEmpty()) {
            loadModContainerFrom(new File(INJECT_MOD).getAbsoluteFile(), true);
        }
        if (!modContainers.containsKey(spark.id) && !disableSpark &&
                DependencyHelper.loadDependencySafe(DependencyHelper.sparkDependency)) {
            foxLoader.logger.info("Injecting spark using FoxLoader adapter.");
            spark.clientModCls = "com.fox2code.foxloader.spark.FoxLoaderClientSparkPlugin";
            spark.serverModCls = "com.fox2code.foxloader.spark.FoxLoaderServerSparkPlugin";
            modContainers.put(spark.id, spark);
        }
        for (ModContainer modContainer : modContainers.values()) {
            try {
                modContainer.applyPrePatch();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Caused by the mod: " + modContainer.id, e);
            }
        }
        PreLoader.initializePrePatch(client);
        ModLoaderMixin.initializeMixin(client);
        for (ModContainer modContainer : modContainers.values()) {
            modContainer.applyModMixins(client);
        }
        FoxLauncher.getFoxClassLoader().allowLoadingGame();
    }

    static void initializeMods(boolean client) {
        gameThread = Thread.currentThread();
        for (ModContainer modContainer : modContainers.values()) {
            try {
                modContainer.applyMod(client);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Caused by the mod: " + modContainer.id, e);
            }
        }
        for (ModContainer modContainer : modContainers.values()) {
            modContainer.notifyOnPreInit();
        }
        for (ModContainer modContainer : modContainers.values()) {
            modContainer.notifyOnInit();
        }
        allModsLoaded = true;
    }

    static void postInitializeMods() {
        for (ModContainer modContainer : modContainers.values()) {
            modContainer.notifyOnPostInit();
        }
    }

    private static void loadModContainerFrom(File file, boolean injected) {
        Manifest manifest;
        try (JarFile jarFile = new JarFile(file)) {
            manifest = jarFile.getManifest();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }
        Attributes attributes = manifest.getMainAttributes();
        String id = attributes.getValue(MOD_ID);
        String name = attributes.getValue(MOD_NAME);
        String version = attributes.getValue(MOD_VERSION);
        if (id == null || id.isEmpty()) {
            foxLoader.logger.warning("Unable to load " + file.getName() +
                    " because it doesn't have a mod-id (Is it a core mod?)");
            return;
        }
        if (FOX_LOADER_MOD_ID.equals(id) || "minecraft".equals(id) || "reindev".equals(id) || "null".equals(id)) {
            foxLoader.logger.warning("Unable to load " + file.getName() +
                    " because it used the reserved mod id: " + id);
            return;
        }
        if (name == null || name.isEmpty()) {
            name = id.substring(0, 1).toLowerCase(Locale.ROOT) + id.substring(1);
        }
        if (version == null) {
            version = "1.0";
        }
        ModContainer modContainer = modContainers.get(id);
        if (modContainer != null) {
            foxLoader.logger.warning("Unable to load " + file.getName() + " because " +
                    modContainer.file.getName() + " already uses the same mod id: " + id);
            return;
        }
        modContainer = new ModContainer(file, id, name, version, injected);
        modContainer.prePatch = attributes.getValue(PRE_PATCH);
        modContainer.clientModCls = attributes.getValue(CLIENT_MOD);
        modContainer.serverModCls = attributes.getValue(SERVER_MOD);
        modContainer.commonModCls = attributes.getValue(COMMON_MOD);
        modContainer.clientMixins = attributes.getValue(CLIENT_MIXIN);
        modContainer.serverMixins = attributes.getValue(SERVER_MIXIN);
        modContainer.commonMixins = attributes.getValue(COMMON_MIXIN);
        modContainers.put(id, modContainer);
        try {
            FoxLauncher.getFoxClassLoader().addURL(file.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new Error("What!", e);
        }
    }

    public static ModContainer getModContainer(String id) {
        return modContainers.get(id);
    }

    public static boolean areAllModsLoaded() {
        return allModsLoaded;
    }

    public static Thread getGameThread() {
        return gameThread;
    }

    public static Logger getModLoaderLogger() {
        return foxLoader.logger;
    }

    static final LinkedList<LifecycleListener> listeners = new LinkedList<>();

    public static class Internal {
        public static Properties fallbackTranslations = new Properties();

        public static void notifyOnTick() {
            for (ModContainer modContainer : modContainers.values()) {
                modContainer.notifyOnTick();
            }
        }

        public static void notifyOnServerStart(NetworkPlayer.ConnectionType connectionType) {
            for (LifecycleListener lifecycleListener : listeners) {
                lifecycleListener.onServerStart(connectionType);
            }
        }

        public static void notifyOnServerStop(NetworkPlayer.ConnectionType connectionType) {
            for (LifecycleListener lifecycleListener : listeners) {
                lifecycleListener.onServerStop(connectionType);
            }
        }

        public static byte[] compileServerHello(ServerHello serverHello) {
            return LoaderNetworkManager.compileServerPacketData(serverHello, 2);
        }
    }
}
