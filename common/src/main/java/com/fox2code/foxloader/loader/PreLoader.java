package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.FoxClassLoader;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.launcher.utils.Platform;
import com.fox2code.foxloader.launcher.utils.SourceUtil;
import com.fox2code.foxloader.loader.rebuild.ClassDataProvider;
import com.fox2code.foxloader.loader.transformer.*;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class PreLoader {
    private static final LinkedHashSet<PreClassTransformer> preTransformers = new LinkedHashSet<>();
    private static final LinkedHashSet<PreClassTransformer> preTransformersInternal = new LinkedHashSet<>();
    private static final PreLoadMetaJarHash preLoadMetaJarHash = new PreLoadMetaJarHash();
    private static final boolean ignoreMinecraftCache = Boolean.getBoolean("foxloader.ignore-cache");
    private static ClassDataProvider classDataProviderOverride;
    private static boolean prePatchInitialized;
    private static final String metaInfPath = "META-INF/MANIFEST.MF";
    private static final byte[] metaInf = ("Manifest-Version: 1.0\n" +
            "FoxLoader-Transformer-Version: " + BuildConfig.FOXLOADER_TRANSFORMER_VERSION +
            "Multi-Release: true\n").getBytes(StandardCharsets.UTF_8);
    private static JvmCompatTransformer jvmCompatTransformer = null;
    private static final boolean devFoxLoader = FoxLauncher.foxLoaderFile.getAbsolutePath().replace('\\', '/')
            .endsWith("/common/build/libs/common-" + BuildConfig.FOXLOADER_VERSION + ".jar");
    private static final HashSet<File> preComputedFilesForHash = new HashSet<>();
    private static boolean ignoreMinecraft = false;
    private static final ArrayList<File> coreMods = new ArrayList<>();

    static {
        if (devFoxLoader) {
            preLoadMetaJarHash.addLong(FoxLauncher.foxLoaderFile.length());
        }
        preComputedFilesForHash.add(FoxLauncher.foxLoaderFile);
        preLoadMetaJarHash.addString(BuildConfig.REINDEV_VERSION);
        preLoadMetaJarHash.addString(BuildConfig.FOXLOADER_TRANSFORMER_VERSION);
        FoxClassLoader foxClassLoader = FoxLauncher.getFoxClassLoader();
        if (foxClassLoader != null) { // foxClassLoader is null in dev mode
            foxClassLoader.addClassTransformers((bytes, className) -> {
                // Ignore minecraft when we use a pre patched jar.
                // =====================================================
                // But in DEV_MODE we use a FoxLoader patched jar as input for
                // secondary pre patched jar, so in DEV_MODE ignoreMinecraft
                // is false and only mods and none of FoxLoader pre-patches
                // are loaded at runtime.
                if (ignoreMinecraft && (
                        className.startsWith("net.minecraft.") ||
                        className.startsWith("paulscode.sound.") ||
                        className.startsWith("com.jcraft."))) {
                    return bytes;
                }
                ClassReader classReader = new ClassReader(bytes);
                ClassNode classNode = new ClassNode();
                classReader.accept(classNode, 0);
                for (PreClassTransformer preClassTransformer : preTransformers) {
                    try {
                        preClassTransformer.transform(classNode, className);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                classNode.accept(classWriter);
                return classWriter.toByteArray();
            });
        }
    }

    static void patchInternal(ClassNode classNode) {
        final String className = classNode.name.replace('/', '.');
        for (PreClassTransformer preClassTransformer : preTransformersInternal) {
            try {
                preClassTransformer.transform(classNode, className);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void patchForMixin(ClassNode classNode, String className) {
        for (PreClassTransformer preClassTransformer : preTransformers) {
            try {
                preClassTransformer.transform(classNode, className);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (jvmCompatTransformer != null) {
            jvmCompatTransformer.transform(classNode, className);
        }
    }

    static void addCoreMod(File coreMod) {
        URL url;
        try {
            url = coreMod.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        FoxLauncher.getFoxClassLoader().addCoreModURL(url);
        preLoadMetaJarHash.addLong(coreMod.length());
        FoxLauncher.getFoxClassLoader().addURL(url);
        coreMods.add(coreMod);
    }

    static void initializePrePatch(boolean client) {
        prePatchInitialized = true;
        preLoadMetaJarHash.freeze();
        final String currentHash = preLoadMetaJarHash.getHash();
        String previousHash = "";
        File configFolder = ModLoader.foxLoader.configFolder;
        if (!configFolder.exists() && !configFolder.mkdirs()) {
            ModLoader.foxLoader.logger.severe("Can't create FoxLoader config folder!");
            return;
        }
        File jar = new File(configFolder, client ? "PatchedMinecraftClient.jar" : "PatchedMinecraftServer.jar");
        File hash = new File(configFolder, client ? "PatchedMinecraftClient.hash" : "PatchedMinecraftServer.hash");
        if (jar.exists() && hash.exists()) {
            try {
                previousHash = new String(Files.readAllBytes(
                        hash.toPath()), StandardCharsets.UTF_8);
            } catch (Exception ignored) {}
        }
        ModLoader.foxLoader.logger.info("PreLoader hash: " + currentHash);
        if (currentHash.equals(previousHash) && !ignoreMinecraftCache) {
            ModLoader.foxLoader.logger.info("Existing patched jar exists, using that!");
            try {
                FoxLauncher.getFoxClassLoader().setPatchedMinecraftURL(jar.toURI().toURL());
                if (!ModLoader.DEV_MODE) ignoreMinecraft = true;
            } catch (MalformedURLException e) {
                ModLoader.foxLoader.logger.log(Level.SEVERE, "Failed to set early source", e);
            }
            return;
        }
        try {
            if (jar.exists() && !jar.delete()) {
                ModLoader.foxLoader.logger.severe("Can't delete previous patched jar");
                return;
            }
            ModLoader.foxLoader.logger.info("Patching jar file...");
            File sourceJar = new File(FoxLauncher.getFoxClassLoader()
                    .getMinecraftSource().toURI().getPath());
            ModLoader.foxLoader.logger.info("Source jar file: " + sourceJar.getAbsolutePath());
            patchJar(sourceJar, jar, false);
            Files.write(hash.toPath(), currentHash.getBytes(StandardCharsets.UTF_8));
            ModLoader.foxLoader.logger.info("Jar patched successfully, using that!");
            FoxLauncher.getFoxClassLoader().setPatchedMinecraftURL(jar.toURI().toURL());
            if (!ModLoader.DEV_MODE) ignoreMinecraft = true;
        } catch (Exception e) {
            ModLoader.foxLoader.logger.log(Level.SEVERE, "Failed to patch jar file", e);
        }
    }

    static void loadPrePatches(boolean client) {
        preTransformers.clear();
        if (FoxLauncher.getFoxClassLoader() != null) {
            final int jvmVersion = Platform.getJvmVersion();
            if (jvmVersion < 11) {
                ModLoader.foxLoader.logger.info( // Tell the user we are doing that :3
                        "Registering JvmCompatTransformer to run Java11 code on Java" + jvmVersion);
                preTransformers.add(jvmCompatTransformer = new JvmCompatTransformer(jvmVersion));
            } else {
                ModLoader.foxLoader.logger.info( // Tell the user their jvm version
                        "You are currently running on Java" + jvmVersion);
                jvmCompatTransformer = null;
            }
        }
        registerPrePatch(new VarNameTransformer());
        registerPrePatch(new RegistryTransformer());
        if (client) {
            registerPrePatch(new MinecraftClientDebugTransformer());
            registerPrePatch(new FrustrumHelperTransformer());
            registerPrePatch(new NetworkMappingTransformer());
        }
    }

    public static void patchReIndevForDev(File in, File out, boolean client) throws IOException {
        if (FoxLauncher.getFoxClassLoader() != null)
            throw new IllegalStateException("Not in development environment!");
        loadPrePatches(client);
        registerPrePatch(new DevelopmentModeTransformer());
        patchJar(in, out, false);
    }

    public static void patchDevReIndevForSource(File in, File out) throws IOException {
        if (FoxLauncher.getFoxClassLoader() != null)
            throw new IllegalStateException("Not in development environment!");
        preTransformers.clear();
        registerPrePatch(new DevelopmentSourceTransformer());
        patchJar(in, out, true);
    }

    private static void patchJar(File in, File out, boolean ignoreFrames) throws IOException {
        LinkedHashMap<String, byte[]> hashMap = new LinkedHashMap<>();
        hashMap.put(metaInfPath, metaInf); // Set META-INF first
        final byte[] empty = new byte[0];
        final byte[] buffer = new byte[2048];
        ZipEntry entry;
        int nRead;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ArrayList<File> sources = new ArrayList<>(coreMods);
        sources.add(in);
        for (File source : sources) {
            try (ZipInputStream zipInputStream = new ZipInputStream(
                    new BufferedInputStream(Files.newInputStream(source.toPath())))) {
                while (null != (entry = zipInputStream.getNextEntry())) {
                    if (!entry.isDirectory() &&
                            !hashMap.containsKey(entry.getName())) {
                        baos.reset();
                        while ((nRead = zipInputStream.read(buffer, 0, buffer.length)) != -1) {
                            baos.write(buffer, 0, nRead);
                        }
                        byte[] bytes = baos.toByteArray();
                        if (bytes.length == 0) bytes = empty;
                        hashMap.put(entry.getName(), bytes);
                    }
                }
            }
        }
        ClassDataProvider classDataProvider = new ClassDataProvider(new URLClassLoader(
                new URL[]{in.toURI().toURL()}, PreLoader.class.getClassLoader()) {
            @Nullable
            @Override
            public URL getResource(String name) {
                URL resource = this.findResource(name);

                return resource != null ? resource :
                        super.getParent().getResource(name);
            }
        }, PreLoader::patchInternal);
        classDataProvider.addClasses(hashMap);
        classDataProviderOverride = classDataProvider;
        for (Map.Entry<String, byte[]> element : hashMap.entrySet()) {
            String entryName = element.getKey();
            if (entryName.endsWith(".class")) {
                entryName = entryName.substring(0,
                        entryName.length() - 6).replace('/', '.');
                ClassReader classReader = new ClassReader(element.getValue());
                ClassNode classNode = new ClassNode();
                classReader.accept(classNode,
                        ignoreFrames ? ClassReader.SKIP_FRAMES : 0);
                patchForMixin(classNode, entryName);
                ClassWriter classWriter = ignoreFrames ?
                        new ClassWriter(ClassWriter.COMPUTE_MAXS) :
                        classDataProviderOverride.newClassWriter();
                classNode.accept(classWriter);
                element.setValue(classWriter.toByteArray());
            }
        }
        classDataProviderOverride = null;
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                new BufferedOutputStream(Files.newOutputStream(out.toPath())))) {
            for (Map.Entry<String, byte[]> element : hashMap.entrySet()) {
                final ZipEntry zipEntry = new ZipEntry(element.getKey());
                final byte[] data = element.getValue();
                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.write(data);
            }
        }
    }

    public static ClassDataProvider getClassDataProvider() {
        return classDataProviderOverride != null ?
                classDataProviderOverride : ModLoader.classDataProvider;
    }

    static void registerPrePatch(PreClassTransformer classTransformer) {
        if (prePatchInitialized)
            throw new IllegalStateException("Minecraft already pre patched");
        File file = SourceUtil.getSourceFile(classTransformer.getClass());
        if (!preComputedFilesForHash.add(file)) {
            preLoadMetaJarHash.addLong(file.length());
        }
        preLoadMetaJarHash.addString(classTransformer.getClass().getName());
        preTransformers.add(classTransformer);
        if (classTransformer.changeClassStructure()) {
            preTransformersInternal.add(classTransformer);
        }
    }

    /**
     * This allows to calculate a hash for a set of mods.
     * <p>
     * The calls are order sensitive, and should stay order sensitive.
     */
    public static class PreLoadMetaJarHash {
        private final MessageDigest digest;
        private final ByteArrayOutputStream baos;
        private final DataOutputStream dos;
        private byte[] cache;

        public PreLoadMetaJarHash() {
            try {
                this.digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            this.baos = new ByteArrayOutputStream();
            this.dos = new DataOutputStream(this.baos);
        }

        public void addString(String text) {
            if (this.cache != null)
                throw new IllegalStateException("Hash has been frozen");
            try {
                this.dos.writeUTF(text);
                this.dos.writeByte(0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void addInt(int i) {
            if (this.cache != null)
                throw new IllegalStateException("Hash has been frozen");
            try {
                this.dos.writeInt(i);
                this.dos.writeByte(0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void addLong(long l) {
            if (this.cache != null)
                throw new IllegalStateException("Hash has been frozen");
            try {
                this.dos.writeLong(l);
                this.dos.writeByte(0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public byte[] makeHash() {
            if (this.cache != null) return this.cache;
            byte[] result = this.digest.digest(this.baos.toByteArray());
            if (result.length != 32) {
                throw new AssertionError(
                        "Result hash is not the result hash of a SHA-256 hash " +
                                "(got " + result.length + ", expected 32)");
            }
            return result;
        }

        public String getHash() {
            byte[] hash = makeHash();
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02X ", b));
            }
            return builder.toString();
        }

        public void freeze() {
            this.cache = this.makeHash();
        }
    }

    static JvmCompatTransformer getJvmCompatTransformer() {
        return jvmCompatTransformer;
    }
}
