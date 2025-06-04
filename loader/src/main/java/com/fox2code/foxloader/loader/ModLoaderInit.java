/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
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
package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.dependencies.DependencyHelper;
import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.FileInfo;
import com.fox2code.foxloader.launcher.FoxClassLoader;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.early.EarlyLoader;
import com.fox2code.foxloader.loader.java.JavaLoadingPlugin;
import com.fox2code.foxloader.loader.java.JavaModInfo;
import com.fox2code.foxloader.patching.PreLoader;
import com.fox2code.foxloader.patching.mixin.MixinModLoader;
import com.fox2code.foxloader.utils.Platform;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_ClassLoader;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ran by {@link FoxLauncher} tp start the game
 */
public final class ModLoaderInit {
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String INJECT = FoxLauncher.DEV_MODE ? System.getProperty("foxloader.inject-mod") : null;
    private static final HashSet<String> RESERVED_MOD_IDS = new HashSet<>(Arrays.asList(
            "foxloader", "minecraft", "reindev", "lwjgl", "java", "null"
    ));
    static final File mods = new File(FoxLauncher.getGameDir(), "mods");
    static final File config = new File(FoxLauncher.getGameDir(), "config");
    static final ModContainer FOX_LOADER_CONTAINER = new ModContainer(
            JavaLoadingPlugin.JAVA_LOADING_PLUGIN, JavaModInfo.FOX_LOADER_MOD_INFO);
    static final LinkedHashMap<String, ModContainer> modContainers = new LinkedHashMap<>();
    private static final Collection<ModContainer> modContainnersCollection =
            Collections.unmodifiableCollection(modContainers.values());
    static boolean isClientDevModeImpl = true;

    static {
        ((LoadingPlugin) JavaLoadingPlugin.JAVA_LOADING_PLUGIN).javaModInfo = JavaModInfo.FOX_LOADER_MOD_INFO;
        FoxLauncher.getFoxClassLoader().injectMissingFileInfo(JavaModInfo.FOX_LOADER_MOD_INFO);
        assertValidModInfo(JavaModInfo.FOX_LOADER_MOD_INFO, true);
    }

    public static @NotNull Logger getModLoaderLogger() {
        return FOX_LOADER_CONTAINER.getLogger();
    }

    public static @NotNull org.slf4j.Logger getModLoaderSlf4jLogger() {
        return FOX_LOADER_CONTAINER.getSlf4jLogger();
    }

    public static ModContainer getModContainer(String modId) {
        return modContainers.get(modId);
    }

    @NotNull public static Collection<ModContainer> getModContainers() {
        if (modContainers.isEmpty()) {
            throw new IllegalStateException("getModContainers() called before loadModContainersWithLoaders()");
        }
        return modContainnersCollection;
    }

    public static void launchModdedClient(String[] args) throws Exception {
        getModLoaderLogger().info("Launching FoxLoader " + BuildConfig.FOXLOADER_VERSION + " client");
        isClientDevModeImpl = args.length < 2 || args[1].length() < 2;
        commonPreInitialize(true);
        try {
            Minecraft.main(args);
        } finally {
            System.out.flush();
            System.err.flush();
        }
    }

    public static void launchModdedServer(String[] args) throws Exception {
        getModLoaderLogger().info("Launching FoxLoader " + BuildConfig.FOXLOADER_VERSION + " server");
        commonPreInitialize(false);
        try {
            MinecraftServer.main(args);
        } finally {
            System.out.flush();
            System.err.flush();
        }
    }

    private static void commonPreInitialize(boolean client) throws Exception {
        if (BuildConfig.IS_DEV_BUILD) {
            getModLoaderLogger().warning("This is a development build of FoxLoader.");
        }
        J_L_ClassLoader.setClassloaderName("FoxLoader", FoxLauncher.getFoxClassLoader());
        if (FoxLauncher.wantJAnsi()) {
            DependencyHelper.loadDependency(DependencyHelper.jansi);
            try {
                AnsiConsole.systemInstall();
            } finally {
                FoxLauncher.notifyJAnsiInstalled();
            }
        } else {
            DependencyHelper.checkDependency(DependencyHelper.jansi);
        }
        PreLoader.initializePatching();
        DependencyHelper.loadModernJavaDependencies();
        MixinModLoader.notifyInitialized();
        if (client) {
            Platform.getPlatform().setupLwjgl2(
                    FoxLauncher.getFoxClassLoader(),
                    new File(FoxLauncher.getGameDir(), "natives"));
        }
        ModContainer.initializeUpdateManager();
        Collection<LoadingPlugin> loadingPlugins =
                loadModContainersWithLoaders();
        FoxLauncher.getFoxClassLoader().allowLoadingGame();
        EarlyLoader.earlyLoad();
        ModLoader.preInitializeMods(loadingPlugins);
    }

    private static void gatherJarInJarInfos(LinkedList<JarInJarInfo> jarInJarInfos, File file) {
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> a = jarFile.entries();
            while (a.hasMoreElements()) {
                JarEntry jarEntry = a.nextElement();
                String entryPath = jarEntry.getName();
                if (jarEntry.getSize() != 0 && entryPath.endsWith(".jar") &&
                        entryPath.startsWith("META-INF/jars/")) {
                    jarInJarInfos.add(new JarInJarInfo(file, entryPath));
                }
            }
        } catch (IOException e) {
            ModLoaderInit.getModLoaderLogger().log(Level.WARNING,
                    "Failed to check zip file for jar in jar!", e);
        }
    }

    private static boolean addEarlyModRegistryInfoIfAppropriate(
            LinkedHashMap<String, EarlyModRegistryInfo> earlyModRegistryInfos,
            EarlyModRegistryInfo earlyModRegistryInfo) {
        EarlyModRegistryInfo conflictingEarlyModRegistryInfo =
                earlyModRegistryInfos.get(earlyModRegistryInfo.modInfo.id);
        if (conflictingEarlyModRegistryInfo != null && (conflictingEarlyModRegistryInfo.loaded ||
                conflictingEarlyModRegistryInfo.modInfo.flexver.isGreaterOrEqual(
                        earlyModRegistryInfo.modInfo.flexver))) {
            return false;
        }
        earlyModRegistryInfos.put(earlyModRegistryInfo.modInfo.id, earlyModRegistryInfo);
        return true;
    }

    private static ArrayList<File> collectFilesToLoad() {
        FoxClassLoader foxClassLoader = FoxLauncher.getFoxClassLoader();
        ArrayList<File> filesToLoad = new ArrayList<>();
        for (File file: FoxLauncher.filesToLoad()) {
            if (file.exists() && !foxClassLoader.isFileInClassLoader(file)) {
                filesToLoad.add(file);
            }
        }
        return filesToLoad;
    }

    private static Collection<LoadingPlugin> loadModContainersWithLoaders() throws Exception {
        if (!modContainers.isEmpty()) {
            throw new IllegalStateException("Mods container were already loaded.");
        }
        if (!mods.exists() && !mods.mkdirs()) {
            throw new IOException("Can't create mod folder");
        }
        if (!config.exists() && !config.mkdirs()) {
            throw new IOException("Can't create config folder");
        }
        // Gather basic info and construct local variables
        ModInfo reIndevModInfo = JavaLoadingPlugin.JAVA_LOADING_PLUGIN.getReIndevModInfo();
        FoxLauncher.getFoxClassLoader().injectMissingFileInfo(reIndevModInfo);
        modContainers.put(reIndevModInfo.id, new ModContainer(
                JavaLoadingPlugin.JAVA_LOADING_PLUGIN, reIndevModInfo));
        modContainers.put(FOX_LOADER_CONTAINER.getModId(), FOX_LOADER_CONTAINER);
        ArrayList<JavaModInfo> loadersInfo = new ArrayList<>();
        HashMap<String, LoadingPlugin> loaders = new HashMap<>();
        HashSet<String> loadedBundles = new HashSet<>();
        LinkedHashMap<String, EarlyModRegistryInfo> earlyModRegistryInfos = new LinkedHashMap<>();
        LinkedHashMap<String, EarlyModRegistryInfo> earlyInjectedModRegistryInfos = new LinkedHashMap<>();
        LinkedList<File> files = new LinkedList<>(Arrays.asList(
                Objects.requireNonNull(mods.listFiles(File::isFile))));
        if (INJECT != null) {
            File toInject = new File(INJECT);
            if (toInject.exists()) {
                files.add(toInject);
            }
        }

        getModLoaderLogger().info("Files to load:" + collectFilesToLoad());
        files.addAll(collectFilesToLoad()); // <- Allow loading plugin to load classpath entries
        files.remove(reIndevModInfo.file); // <- ReIndev might be force added to the leftover files
        LinkedList<JarInJarInfo> jarInJarInfos = new LinkedList<>();
        // Gather all basic java mods.
        Iterator<File> fileIterator = files.iterator();
        while (fileIterator.hasNext()) {
            File file = fileIterator.next();
            if (file.isFile() && file.length() > 0) {
                gatherJarInJarInfos(jarInJarInfos, file);
            }
            JavaModInfo javaModInfo = (JavaModInfo)
                    JavaLoadingPlugin.JAVA_LOADING_PLUGIN.getModInfoHelper(file, null);
            if (javaModInfo != null) {
                assertValidModInfo(javaModInfo, false);
                if (earlyModRegistryInfos.containsKey(javaModInfo.id)) {
                    throw new RuntimeException("Duplicate mods with id " +
                            javaModInfo.id + " between \"" +
                            earlyModRegistryInfos.get(javaModInfo.id).modInfo.file.getName() +
                            "\" and  \"" + javaModInfo.file.getName());
                }
                if (javaModInfo.loadingPlugin != null &&
                        !javaModInfo.loadingPlugin.isEmpty()) {
                    loadersInfo.add(javaModInfo);
                }
                earlyModRegistryInfos.put(javaModInfo.id,
                        new EarlyModRegistryInfo(JavaLoadingPlugin.JAVA_LOADING_PLUGIN, javaModInfo, true, true));
                fileIterator.remove();
            }
        }
        Iterator<JarInJarInfo> jarInJarIterator = jarInJarInfos.iterator();
        while (jarInJarIterator.hasNext()) {
            JarInJarInfo jarInJarInfo = jarInJarIterator.next();
            JavaModInfo javaModInfo = (JavaModInfo)
                    JavaLoadingPlugin.JAVA_LOADING_PLUGIN.getModInfoHelper(
                            jarInJarInfo.file, jarInJarInfo.jarPath);
            if (javaModInfo != null) {
                assertValidModInfo(javaModInfo, false);
                EarlyModRegistryInfo conflictingEarlyRegistryModInfo =
                        earlyModRegistryInfos.get(javaModInfo.id);
                if (conflictingEarlyRegistryModInfo != null) {
                    if (conflictingEarlyRegistryModInfo.direct) {
                        continue; // Skip jar in jar mods with duplicate id
                    } else if (conflictingEarlyRegistryModInfo.modInfo.flexver.isGreaterOrEqual(javaModInfo.flexver)) {
                        continue; // Skip jar in jar mods that does not increase version
                    }
                    loadersInfo.remove((JavaModInfo) conflictingEarlyRegistryModInfo.modInfo);
                }
                if (javaModInfo.loadingPlugin != null &&
                        !javaModInfo.loadingPlugin.isEmpty()) {
                    loadersInfo.add(javaModInfo);
                }
                earlyModRegistryInfos.put(javaModInfo.id,
                        new EarlyModRegistryInfo(JavaLoadingPlugin.JAVA_LOADING_PLUGIN, javaModInfo, true, false));
                jarInJarInfo.registered = javaModInfo;
                jarInJarIterator.remove();
            }
        }
        // Construct loading plugins
        for (EarlyModRegistryInfo earlyModRegistryInfo : earlyModRegistryInfos.values()) {
            // Load dependencies bundles of potential loading plugins early.
            loadDependencyBundlesForMod(loadedBundles, earlyModRegistryInfo.modInfo);
            // Allow loading plugins to run
            FoxLauncher.getFoxClassLoader().addFileToClassLoader(earlyModRegistryInfo.modInfo);
        }
        loaders.put("java", JavaLoadingPlugin.JAVA_LOADING_PLUGIN); // Add java plugin to prevent duplicate id
        for (JavaModInfo javaModInfo : loadersInfo) {
            LoadingPlugin loadingPlugin = Class.forName(javaModInfo.loadingPlugin)
                    .asSubclass(LoadingPlugin.class).newInstance();
            final String loadingPluginId = loadingPlugin.getPluginId();
            if (loaders.containsKey(loadingPluginId)) {
                throw new RuntimeException("Duplicate loader with id " +
                        javaModInfo.id + " between \"" +
                        loaders.get(javaModInfo.id).javaModInfo.file.getName() +
                        "\" and  \"" + javaModInfo.file.getName());
            }
            loadingPlugin.javaModInfo = javaModInfo;
            loaders.put(loadingPluginId, loadingPlugin);
        }
        // Load non-foxloader mods
        loaders.remove("java"); // Remove Java plugin when loading mods outside of the java plugin
        fileIterator = files.iterator();
        while (fileIterator.hasNext()) {
            File file = fileIterator.next();
            ModInfo modInfo = null;
            LoadingPlugin loadingPlugin = null;
            for (LoadingPlugin loadingPluginLoop: loaders.values()) {
                modInfo = loadingPluginLoop.getModInfoHelper(file, null);
                if (modInfo != null) {
                    loadingPlugin = loadingPluginLoop;
                    break;
                }
            }
            if (modInfo != null) {
                assertValidModInfo(modInfo, false);
                if (addEarlyModRegistryInfoIfAppropriate(earlyModRegistryInfos,
                        new EarlyModRegistryInfo(loadingPlugin, modInfo, true, false))) {
                    fileIterator.remove();
                }
            }
        }
        jarInJarIterator = jarInJarInfos.iterator();
        while (jarInJarIterator.hasNext()) {
            JarInJarInfo jarInJarInfo = jarInJarIterator.next();
            ModInfo modInfo = null;
            LoadingPlugin loadingPlugin = null;
            for (LoadingPlugin loadingPluginLoop: loaders.values()) {
                modInfo = loadingPluginLoop.getModInfoHelper(
                        jarInJarInfo.file, jarInJarInfo.jarPath);
                if (modInfo != null) {
                    loadingPlugin = loadingPluginLoop;
                    break;
                }
            }
            if (modInfo != null) {
                assertValidModInfo(modInfo, false);
                if (addEarlyModRegistryInfoIfAppropriate(earlyModRegistryInfos,
                        new EarlyModRegistryInfo(loadingPlugin, modInfo, false, false))) {
                    jarInJarInfo.registered = modInfo;
                    jarInJarIterator.remove();
                }
            }
        }
        loaders.put("java", JavaLoadingPlugin.JAVA_LOADING_PLUGIN); // Add java plugin back for mod injection
        for (LoadingPlugin loadingPlugin: loaders.values()) {
            List<ModInfo> modInfos = loadingPlugin.getInjectedModInfoHelper();
            for (ModInfo modInfo : modInfos) {
                if (modInfo != null) {
                    assertValidModInfo(modInfo, false);
                    if (modInfo.file.getParent().equals(ModLoaderInit.mods.getPath())) {
                        ModLoaderInit.getModLoaderLogger().warning("Skipping injecting " +
                                idAndFile(modInfo) + " from " + idAndFile(loadingPlugin.javaModInfo) +
                                " as it is present in the mod folder and you must " +
                                "use the getModInfo callback for mods in the mods folder");
                        continue;
                    }
                    EarlyModRegistryInfo earlyModRegistryInfo = earlyModRegistryInfos.get(modInfo.id);
                    if (earlyModRegistryInfo != null) {
                        ModLoaderInit.getModLoaderLogger().info("Skipping injecting duplicate " +
                                idAndFile(modInfo) + " from " + idAndFile(loadingPlugin.javaModInfo) +
                                " due to being already loaded from " +
                                earlyModRegistryInfo.modInfo.fileName + " from loader " +
                                idAndFile(earlyModRegistryInfo.loadingPlugin.javaModInfo));
                        continue;
                    }
                    earlyModRegistryInfo = earlyInjectedModRegistryInfos.get(modInfo.id);
                    if (earlyModRegistryInfo != null) {
                        if (earlyModRegistryInfo.loaded) {
                            ModLoaderInit.getModLoaderLogger().info("Skipping injecting duplicate " +
                                    idAndFile(modInfo) + " from " + idAndFile(loadingPlugin.javaModInfo) +
                                    " due to being already injected from " +
                                    earlyModRegistryInfo.modInfo.fileName + " from loader " +
                                    idAndFile(earlyModRegistryInfo.loadingPlugin.javaModInfo));
                            continue;
                        } else if (earlyModRegistryInfo.modInfo.flexver.isGreaterOrEqual(modInfo.flexver)) {
                            continue;
                        }
                    }
                    ModLoaderInit.getModLoaderLogger().info("Injecting " + modInfo.id);
                    earlyInjectedModRegistryInfos.put(modInfo.id,
                            new EarlyModRegistryInfo(loadingPlugin, modInfo, false, false));
                }
            }
        }
        earlyModRegistryInfos.putAll(earlyInjectedModRegistryInfos);
        ArrayList<EarlyModRegistryInfo> sortedEarlyModRegistryInfo = new ArrayList<>(earlyModRegistryInfos.values());
        sortedEarlyModRegistryInfo.sort((o1, o2) -> Long.compare(
                o2.modInfo.loadOrderPriority, o1.modInfo.loadOrderPriority));
        for (EarlyModRegistryInfo earlyModRegistryInfo : sortedEarlyModRegistryInfo) {
            earlyModRegistryInfo.register(loadedBundles);
        }
        if (FoxLauncher.DEVELOPING_FOXLOADER && !files.isEmpty() &&
                !(files.size() == 1 && files.get(0).getName().equals("debugger-agent.jar"))) {
            throw new Error("Leftovers files detected: " + files);
        }
        ModLoaderInit.getModLoaderLogger().info(
                "Found " + modContainers.size() + " mods during initialization.");
        for (LoadingPlugin loadingPlugin : loaders.values()) {
            loadingPlugin.onModContainerListFinalized();
        }
        fileIterator = files.iterator();
        while (fileIterator.hasNext()) {
            FoxLauncher.getFoxClassLoader().injectMissingFileInfo(
                    new FileInfo(fileIterator.next()));
        }
        for (ModContainer modContainer : modContainers.values()) {
            modContainer.preLoadContainer();
        }
        for (LoadingPlugin loadingPlugin : loaders.values()) {
            loadingPlugin.onAllModContainersPreloaded();
        }
        if (FoxLauncher.DEVELOPING_FOXLOADER) {
            loadAllDependencyBundlesForDev(loadedBundles);
        }
        return loaders.values();
    }

    private static String idAndFile(ModInfo modInfo) {
        return modInfo.id + " (File: " + modInfo.fileName + ")";
    }

    private static void assertValidModInfo(ModInfo modInfo, boolean privileged) {
        if (!privileged && RESERVED_MOD_IDS.contains(modInfo.id)) {
            throw new RuntimeException(modInfo.file.getName() +
                    " has an reserved mod id: \"" + modInfo.id + "\"");
        }
        boolean invalid = false;
        for (int i = 0; i < modInfo.id.length(); i++) {
            char c = modInfo.id.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c == '_'))) {
                invalid = true;
                break;
            }
        }
        if (invalid || modInfo.id.isEmpty()) {
            throw new RuntimeException(modInfo.fileName +
                    " has an invalid mod id: \"" + modInfo.id + "\"");
        }
        assertValidModField(modInfo.name, "name", modInfo.fileName);
        assertValidModField(modInfo.version, "version", modInfo.fileName);
        assertValidModField(modInfo.description, "description", modInfo.fileName);
        assertValidModField(modInfo.authors, "authors", modInfo.fileName);
    }

    private static void loadAllDependencyBundlesForDev(HashSet<String> loadedBundles) {
        for (String dependencyBundle : DependencyHelper.availableDependencyBundles) {
            if (loadedBundles.add(dependencyBundle)) {
                for (DependencyHelper.Dependency dependency : DependencyHelper.getDependencyBundle(dependencyBundle)) {
                    DependencyHelper.loadDependency(dependency);
                }
            }
        }
    }

    private static void loadDependencyBundlesForMod(HashSet<String> loadedBundles, ModInfo modInfo) {
        for (String dependencyBundle : modInfo.getRequestedDependencyBundles()) {
            if (!DependencyHelper.availableDependencyBundles.contains(dependencyBundle)) {
                throw new RuntimeException("Mod " + idAndFile(modInfo) + " is asking for " + dependencyBundle +
                        " dependency bundle, but it is missing on FoxLoader " + BuildConfig.FOXLOADER_VERSION);
            }
            if (loadedBundles.add(dependencyBundle)) {
                for (DependencyHelper.Dependency dependency : DependencyHelper.getDependencyBundle(dependencyBundle)) {
                    DependencyHelper.loadDependency(dependency);
                }
            }
        }
    }

    private static void assertValidModField(String value, String fieldName, String modName) {
        boolean invalid = false;
        boolean allowNewLines = "description".equals(fieldName);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (allowNewLines && c == '\n') {
                continue; // Allow new lines in descriptions
            }
            if (c <= '\u001F' || c == '\u007F') {
                invalid = true;
                break;
            }
        }
        if (invalid) {
            throw new RuntimeException(modName +
                    " has an invalid mod " + fieldName + ": \"" + value + "\"");
        }
    }

    private static class JarInJarInfo {
        final File file;
        final String jarPath;
        FileInfo registered;

        private JarInJarInfo(File file, String jarPath) {
            this.file = file;
            this.jarPath = jarPath;
        }
    }

    private static class EarlyModRegistryInfo {
        final LoadingPlugin loadingPlugin;
        final ModInfo modInfo;
        final boolean loaded;
        final boolean direct;

        private EarlyModRegistryInfo(LoadingPlugin loadingPlugin, ModInfo modInfo, boolean loaded, boolean direct) {
            this.loadingPlugin = loadingPlugin;
            this.modInfo = modInfo;
            this.loaded = loaded;
            this.direct = direct;
        }

        private void register(HashSet<String> loadedBundles) {
            loadDependencyBundlesForMod(loadedBundles, this.modInfo);
            FoxLauncher.getFoxClassLoader().addFileToClassLoader(this.modInfo);
            modContainers.put(this.modInfo.id, new ModContainer(this.loadingPlugin, this.modInfo));
        }
    }

    public static final class Internal {
        public static void markAddMixin(ModContainer modContainer) {
            if (modContainer != null) {
                modContainer.markAddMixin();
            }
        }
    }
}
