package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.commands.WorldReplace;
import com.fox2code.foxloader.commands.WorldSet;
import com.fox2code.foxloader.launcher.*;
import com.fox2code.foxloader.launcher.utils.AsyncItrLinkedList;
import com.fox2code.foxloader.launcher.utils.SourceUtil;
import com.fox2code.foxloader.loader.packet.ServerHello;
import com.fox2code.foxloader.loader.rebuild.ClassDataProvider;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.CommandCompat;
import com.fox2code.foxloader.registry.RegisteredEntity;
import com.fox2code.foxloader.registry.RegisteredItemStack;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.semver4j.Semver;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

public class ModLoader extends Mod {
    public static final boolean I_AM_EXPERIMENTAL = false; // Show text on client main menu
    public static final boolean TEST_MODE = Boolean.getBoolean("foxloader.test-mode");
    private static final String INJECT_MOD = System.getProperty("foxloader.inject-mod");
    public static final boolean DEV_MODE = Boolean.getBoolean("foxloader.dev-mode");
    public static final boolean PREFILL_CACHE = DEV_MODE || Boolean.getBoolean("foxloader.prefill-cache");
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final File mods = new File(FoxLauncher.getGameDir(), "mods");
    public static final File modsVersioned = new File(mods, "ReIndev" + BuildConfig.REINDEV_VERSION);
    public static final File coremods = new File(FoxLauncher.getGameDir(), "coremods");
    public static final File config = new File(FoxLauncher.getGameDir(), "config");
    static final File updateTmp = new File(config, "update-tmp");
    private static final boolean disableSpark = Boolean.getBoolean("foxloader.disable-spark");
    private static boolean launched = false, allModsLoaded = false;
    public static final String FOX_LOADER_MOD_ID = "foxloader";
    public static final String FOX_LOADER_VERSION = BuildConfig.FOXLOADER_VERSION;
    static final ModContainer foxLoader = new ModContainer(
            FoxLauncher.foxLoaderFile, FOX_LOADER_MOD_ID, "FoxLoader", FOX_LOADER_VERSION,
            "ReIndev mod loader with foxes!!!", "com.github.Fox2Code.FoxLoader:final", false);
    // https://www.jitpack.io/com/github/Fox2Code/FoxLoader/final/0.3.0/final-0.3.0.pom
    // https://www.jitpack.io/com/github/Fox2Code/FoxLoader/final/maven-metadata.xml
    static final LinkedList<File> coreMods = new LinkedList<>();
    // Use LinkedHashMap to keep track in which order mods were loaded.
    static final LinkedHashMap<String, ModContainer> modContainers = new LinkedHashMap<>();
    static Thread gameThread;
    public static final String FOX_LOADER_HEADER = "\0RFL";
    public static final int MAX_MOD_ID_LENGTH = 32;
    private static final Manifest nullManifest = new Manifest();
    private static final Attributes.Name MOD_ID = new Attributes.Name("ModId");
    private static final Attributes.Name MOD_NAME = new Attributes.Name("ModName");
    private static final Attributes.Name MOD_VERSION = new Attributes.Name("ModVersion");
    private static final Attributes.Name MOD_DESC = new Attributes.Name("ModDesc");
    private static final Attributes.Name PRE_PATCH = new Attributes.Name("PreClassTransformer");
    private static final Attributes.Name CLIENT_MOD = new Attributes.Name("ClientMod");
    private static final Attributes.Name SERVER_MOD = new Attributes.Name("ServerMod");
    private static final Attributes.Name COMMON_MOD = new Attributes.Name("CommonMod");
    private static final Attributes.Name CLIENT_MIXIN = new Attributes.Name("ClientMixin");
    private static final Attributes.Name SERVER_MIXIN = new Attributes.Name("ServerMixin");
    private static final Attributes.Name COMMON_MIXIN = new Attributes.Name("CommonMixin");
    private static final Attributes.Name MOD_JITPACK = new Attributes.Name("ModJitPack");
    private static final Attributes.Name UNOFFICIAL = new Attributes.Name("Unofficial");
    private static final Semver INITIAL_SEMVER = new Semver("1.0.0");
    static final ClassDataProvider classDataProvider;

    static {
        classDataProvider = new ClassDataProvider(FoxLauncher.getFoxClassLoader(), PreLoader::patchInternal);
        FoxLauncher.getFoxClassLoader().installWrappedExtensions(
                new FoxWrappedExtensions(classDataProvider, foxLoader.logger));
        if (FoxLauncher.getFoxClassLoader() != ModLoader.class.getClassLoader()) {
            throw new RuntimeException("Invalid class loader context!");
        }
    }

    ModLoader() {}

    static void initializeModdedInstance(boolean client) {
        if (launched) return; launched = true;
        if (!mods.exists() && !mods.mkdirs())
            throw new RuntimeException("Cannot create mods folder");
        if (!modsVersioned.exists() && !modsVersioned.mkdirs())
            throw new RuntimeException("Cannot create versioned mods folder");
        if (!coremods.exists() && !coremods.mkdirs())
            throw new RuntimeException("Cannot create coremods folder");
        modContainers.put(foxLoader.id, foxLoader);
        foxLoader.logger.info("Running FoxLoader " + BuildConfig.FOXLOADER_VERSION);
        foxLoader.logger.info("Game directory: " + FoxLauncher.getGameDir().getAbsolutePath());
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
                loadModContainerFromJar(mod, false);
            }
            for (File mod : Objects.requireNonNull(mods.listFiles(
                    (dir, name) -> name.endsWith(".lua")))) {
                loadModContainerFromLua(mod);
            }
            for (File mod : Objects.requireNonNull(modsVersioned.listFiles(
                    (dir, name) -> name.endsWith(".jar")))) {
                loadModContainerFromJar(mod, false);
            }
            for (File mod : Objects.requireNonNull(modsVersioned.listFiles(
                    (dir, name) -> name.endsWith(".lua")))) {
                loadModContainerFromLua(mod);
            }
        }
        // Inject mod is used by the gradle plugin to load dev mod
        if (DEV_MODE && INJECT_MOD != null && !INJECT_MOD.isEmpty()) {
            loadModContainerFromJar(new File(INJECT_MOD).getAbsoluteFile(), true);
        }
        if (!modContainers.containsKey("spark") && !disableSpark &&
                DependencyHelper.loadDependencySafe(DependencyHelper.sparkDependency)) {
            foxLoader.logger.info("Injecting spark using FoxLoader adapter.");
            ModContainer spark = new ModContainer(SourceUtil.getSourceFileOfClassName(
                    "me.lucko.spark.common.SparkPlugin"), "spark", "Spark",
                    BuildConfig.SPARK_VERSION, "spark is a performance profiling mod.", null, true);
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
            ModContainer.setActiveModContainer(modContainer);
            try {
                modContainer.applyMod(client);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Caused by the mod: " + modContainer.id, e);
            }
        }
        for (ModContainer modContainer : modContainers.values()) {
            ModContainer.setActiveModContainer(modContainer);
            modContainer.notifyOnPreInit();
        }
        for (ModContainer modContainer : modContainers.values()) {
            ModContainer.setActiveModContainer(modContainer);
            modContainer.notifyOnInit();
        }
        ModContainer.setActiveModContainer(null);
        allModsLoaded = true;
    }

    static void postInitializeMods() {
        for (ModContainer modContainer : modContainers.values()) {
            ModContainer.setActiveModContainer(modContainer);
            modContainer.notifyOnPostInit();
        }
        ModContainer.setActiveModContainer(null);
    }

    @Override
    public void onPostInit() {
        CommandCompat.registerCommand(new WorldSet());
        CommandCompat.registerCommand(new WorldReplace());
    }

    private static void loadModContainerFromJar(File file, boolean injected) {
        Manifest manifest;
        try (JarFile jarFile = new JarFile(file)) {
            manifest = jarFile.getManifest();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }
        if (manifest == null) {
            manifest = nullManifest;
        }
        Attributes attributes = manifest.getMainAttributes();
        String id = attributes.getValue(MOD_ID);
        String name = attributes.getValue(MOD_NAME);
        String version = attributes.getValue(MOD_VERSION);
        String desc = attributes.getValue(MOD_DESC);
        if (id == null || id.isEmpty()) {
            foxLoader.logger.warning("Unable to load " + file.getName() +
                    " because it doesn't have a mod-id (Is it a core mod?)");
            return;
        }
        if (FOX_LOADER_MOD_ID.equals(id) || "minecraft".equals(id) || "reindev".equals(id) || "null".equals(id)) {
            if (injected && "null".equals(id)) { // Crash directly if we
                throw new RuntimeException("Please define a modId in gradle");
            }
            foxLoader.logger.warning("Unable to load " + file.getName() +
                    " because it used the reserved mod id: " + id);
            return;
        }
        if (id.length() > MAX_MOD_ID_LENGTH) {
            foxLoader.logger.warning("Unable to load " + file.getName() +
                    " because it use a too large mod id: " + id);
            return;
        }
        if (name == null || name.isEmpty()) {
            name = id.substring(0, 1).toLowerCase(Locale.ROOT) + id.substring(1);
        }
        if (version == null || (version = version.trim()).isEmpty()) {
            version = "1.0";
        }
        Semver semver = "1.0".equals(version) || "1.0.0".equals(version) ?
                INITIAL_SEMVER : Semver.parse(version);
        if (semver == null) {
            int verExt = version.indexOf('-');
            if (verExt != -1) {
                version = version.substring(0, verExt);
            }
            semver = Semver.coerce(version);
        }
        if (semver == null) {
            semver = INITIAL_SEMVER;
        }

        if (desc == null || desc.isEmpty()) {
            desc = "...";
        }
        ModContainer modContainer = modContainers.get(id);
        if (modContainer != null) {
            foxLoader.logger.warning("Unable to load " + file.getName() + " because " +
                    modContainer.file.getName() + " already uses the same mod id: " + id);
            return;
        }
        String jitpack = attributes.getValue(MOD_JITPACK);
        if (jitpack != null && jitpack.isEmpty()) jitpack = null;
        boolean unofficial = Boolean.parseBoolean(attributes.getValue(UNOFFICIAL));
        modContainer = new ModContainer(file, id, name, version, semver, desc, jitpack, unofficial, injected);
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

    private static final String MOD_ID_LUA_PREFIX = "-- modId: ";
    private static final String MOD_NAME_LUA_PREFIX = "-- modName: ";
    private static final String MOD_VERSION_LUA_PREFIX = "-- version: ";
    private static final String MOD_DESC_LUA_PREFIX = "-- description: ";
    private static final String MOD_UNOFFICIAL_LUA_PREFIX = "-- unofficial: ";

    private static void loadModContainerFromLua(File file) {
        String id = null;
        String name = null;
        String version = null;
        String desc = null;
        boolean unofficial = false;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = bufferedReader.readLine()) != null
                    && (line.equals("--") || line.startsWith("-- "))) {
                if (line.startsWith(MOD_ID_LUA_PREFIX)) {
                    id = line.substring(MOD_ID_LUA_PREFIX.length());
                } else if (line.startsWith(MOD_NAME_LUA_PREFIX)) {
                    name = line.substring(MOD_NAME_LUA_PREFIX.length());
                } else if (line.startsWith(MOD_VERSION_LUA_PREFIX)) {
                    version = line.substring(MOD_VERSION_LUA_PREFIX.length());
                } else if (line.startsWith(MOD_DESC_LUA_PREFIX)) {
                    desc = line.substring(MOD_DESC_LUA_PREFIX.length());
                } else if (line.startsWith(MOD_UNOFFICIAL_LUA_PREFIX)) {
                    unofficial = Boolean.parseBoolean(line.substring(
                            MOD_UNOFFICIAL_LUA_PREFIX.length()));
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }

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
        if (id.length() > MAX_MOD_ID_LENGTH) {
            foxLoader.logger.warning("Unable to load " + file.getName() +
                    " because it use a too large mod id: " + id);
            return;
        }
        if (name == null || name.isEmpty()) {
            name = id.substring(0, 1).toLowerCase(Locale.ROOT) + id.substring(1);
        }
        if (version == null || (version = version.trim()).isEmpty()) {
            version = "1.0";
        }
        Semver semver = "1.0".equals(version) || "1.0.0".equals(version) ?
                INITIAL_SEMVER : Semver.parse(version);
        if (semver == null) {
            int verExt = version.indexOf('-');
            if (verExt != -1) {
                version = version.substring(0, verExt);
            }
            semver = Semver.coerce(version);
        }
        if (semver == null) {
            semver = INITIAL_SEMVER;
        }

        if (desc == null || desc.isEmpty()) {
            desc = "...";
        }
        ModContainer modContainer = modContainers.get(id);
        if (modContainer != null) {
            foxLoader.logger.warning("Unable to load " + file.getName() + " because " +
                    modContainer.file.getName() + " already uses the same mod id: " + id);
            return;
        }
        modContainer = new ModContainer(file, id, name, version, semver, desc, null, unofficial, false);
        modContainers.put(id, modContainer);
    }

    public static boolean checkSemVerMismatch(String value, String accept) {
        return accept == null || !checkSemVerMatch(value, accept);
    }

    public static boolean checkSemVerMatch(String value, String accept) {
        if (accept == null) return true;
        if (value == null) return false;
        Semver semver = Semver.coerce(value);
        return value.equals(accept) || // Always check strict equality
                (semver != null && semver.satisfies(accept));
    }

    public static ModContainer getModContainer(String id) {
        return modContainers.get(id);
    }

    @NotNull
    public static Collection<ModContainer> getModContainers() {
        return Collections.unmodifiableCollection(modContainers.values());
    }

    public static boolean areAllModsLoaded() {
        return allModsLoaded;
    }

    public static Thread getGameThread() {
        return gameThread;
    }

    public static boolean isOnGameThread() {
        return Thread.currentThread() == gameThread;
    }

    public static Logger getModLoaderLogger() {
        return foxLoader.logger;
    }

    static final AsyncItrLinkedList<LifecycleListener> listeners = new AsyncItrLinkedList<>();

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

        public static boolean notifyPlayerStartBreakBlock(NetworkPlayer networkPlayer, RegisteredItemStack itemStack,
                                            int x, int y, int z, int facing) {
            boolean cancelled = false;
            for (ModContainer modContainer : modContainers.values()) {
                cancelled = modContainer.notifyPlayerStartBreakBlock(
                        networkPlayer, itemStack, x, y, z, facing, cancelled);
            }
            return cancelled;
        }

        public static boolean notifyPlayerBreakBlock(NetworkPlayer networkPlayer, RegisteredItemStack itemStack,
                                                          int x, int y, int z, int facing) {
            boolean cancelled = false;
            for (ModContainer modContainer : modContainers.values()) {
                cancelled = modContainer.notifyPlayerBreakBlock(
                        networkPlayer, itemStack, x, y, z, facing, cancelled);
            }
            return cancelled;
        }

        public static boolean notifyPlayerUseItem(NetworkPlayer networkPlayer, RegisteredItemStack itemStack) {
            boolean cancelled = false;
            for (ModContainer modContainer : modContainers.values()) {
                cancelled = modContainer.notifyPlayerUseItem(networkPlayer, itemStack, cancelled);
            }
            return cancelled;
        }

        public static boolean notifyPlayerUseItemOnBlock(NetworkPlayer networkPlayer, RegisteredItemStack itemStack,
                                                         int x, int y, int z, int facing,
                                                         float xOffset, float yOffset, float zOffset) {
            boolean cancelled = false;
            for (ModContainer modContainer : modContainers.values()) {
                cancelled = modContainer.notifyPlayerUseItemOnBlock(
                        networkPlayer, itemStack, x, y, z, facing, xOffset, yOffset, zOffset, cancelled);
            }
            return cancelled;
        }

        public static boolean notifyPlayerUseItemOnEntity(
                NetworkPlayer networkPlayer, RegisteredItemStack itemStack, RegisteredEntity targetEntity) {
            boolean cancelled = false;
            for (ModContainer modContainer : modContainers.values()) {
                cancelled = modContainer.notifyPlayerUseItemOnEntity(
                        networkPlayer, itemStack, targetEntity, cancelled);
            }
            return cancelled;
        }

        public static boolean notifyPlayerAttackEntity(
                NetworkPlayer networkPlayer, RegisteredItemStack itemStack, RegisteredEntity targetEntity) {
            boolean cancelled = false;
            for (ModContainer modContainer : modContainers.values()) {
                cancelled = modContainer.notifyPlayerAttackEntity(
                        networkPlayer, itemStack, targetEntity, cancelled);
            }
            return cancelled;
        }
    }
}
