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
package com.fox2code.foxloader.launcher;

import com.fox2code.foxloader.launcher.protocols.fl.Handler;
import com.fox2code.foxloader.utils.Enumerations;
import com.fox2code.foxloader.utils.Platform;
import com.fox2code.foxloader.utils.async.AsyncItrLinkedList;
import com.fox2code.foxloader.utils.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Files;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class FoxClassLoader extends URLClassLoader implements ClassLoaderMarker {
    private static final String MIXIN_INFO = "org.spongepowered.asm.mixin.transformer.MixinInfo";
    private static final String CLASS_TO_DUMP = System.getProperty("foxloader.dump-class");
    private static final String FL_JAR_IN_JAR_PROTOCOL = "fl";
    static final CodeSigner[] NO_CodeSigners = new CodeSigner[0];
    private static final URL[] NO_URLs = new URL[0];

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final HashMap<String, CodeSource> codeSourceCache = new HashMap<>();
    private final HashMap<String, FileInfo> fileInfoCache = new HashMap<>();
    private final AsyncItrLinkedList<String> exclusions = new AsyncItrLinkedList<>();
    private final HashMap<String, byte[]> injectedClasses = new HashMap<>();
    private final HashMap<String, URL> injectedResources = new HashMap<>();
    private URLClassLoader gameExclusiveSource;
    private boolean allowLoadingGame;
    private WrappedExtensions wrappedExtensions;
    private Function<byte[], byte[]> mixinInfoPatch;
    private boolean patchedExclusiveSource = false;
    private URL reIndevURL;

    FoxClassLoader() {
        super(NO_URLs, FoxClassLoader.class.getClassLoader(), FoxLoaderURLStreamHandler.INSTANCE);
    }

    // Patched via FoxLoaderPatches, Java9+ only
    FoxClassLoader(String name) {
        // super(name, NO_URLs, FoxClassLoader.class.getClassLoader());
        super(NO_URLs, FoxClassLoader.class.getClassLoader(), FoxLoaderURLStreamHandler.INSTANCE);
    }

    @Override
    public ClassLoader getClassLoader() {
        return this;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c;
        if (isGameClassName(name)) {
            if (!allowLoadingGame) {
                throw new ClassNotFoundException("Cannot load \"" + name + "\" during pre init");
            }
            c = findLoadedClass(name);
            if (c == null) {
                synchronized (getClassLoadingLock(name)) {
                    c = findClassImpl(name, null);
                }
            }
        } else if (isFoxLoaderLaunchClass(name)) {
            // Load classes from the parent class loader
            c = super.loadClass(name, resolve);
        } else if (name.startsWith("com.fox2code.foxloader.") ||
                // fix gson loading failing in development environment
                ((FoxLauncher.DEVELOPING_FOXLOADER || FoxLauncher.DEV_MODE) &&
                        isDevSpecialClassName(name)) ||
                // Check mixins to fix them in development environment.
                isSpecialClassName(name)) {
            c = findLoadedClass(name);
            if (c == null) {
                synchronized (getClassLoadingLock(name)) {
                    c = findClassImpl(name, null);
                }
            }
        } else {
            c = findLoadedClass(name);
            if (c == null) {
                URL resource = this.findResource(name.replace('.', '/') + ".class");
                if (resource != null) {
                    synchronized (getClassLoadingLock(name)) {
                        c = findClassImpl(name, resource);
                    }
                } else {
                    try {
                        c = super.loadClass(name, false);
                    } catch (SecurityException securityException) {
                        throw new ClassNotFoundException(name, securityException);
                    } catch (ClassNotFoundException | UnsupportedClassVersionError e) {
                        synchronized (getClassLoadingLock(name)) {
                            c = findClassImpl(name, null);
                        }
                    }
                }
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name); // return findClass(name, null);
    }

    private Class<?> findClassImpl(String name, URL resource) throws ClassNotFoundException {
        Class<?> clas = this.findLoadedClass(name);
        if (clas != null) return clas;
        byte[] bytes = null;
        try {
            final String packageName = name.lastIndexOf('.') == -1 ? "" : name.substring(0, name.lastIndexOf('.'));
            if (getPackage(packageName) == null) {
                definePackage(packageName, null, null, null, null, null, null, null);
            }
            ClassLoader resourceClassLoader;
            if (name.startsWith("com.fox2code.foxloader.")) {
                resourceClassLoader = getParent();
            } else if (isGameClassName(name)) {
                resourceClassLoader = // Do not allow mods to act as jar mods blindly.
                        gameExclusiveSource != null ? gameExclusiveSource : getParent();
            } else {
                resourceClassLoader = this;
            }
            if (resource == null) {
                resource = resourceClassLoader.getResource(
                        name.replace('.', '/').concat(".class"));
            }
            URL url = null;
            if (resource != null) {
                URLConnection urlConnection = resource.openConnection();
                if (urlConnection instanceof JarURLConnection) {
                    url = ((JarURLConnection) urlConnection).getJarFileURL();
                }
                InputStream is = urlConnection.getInputStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                int nRead;
                byte[] data = new byte[16384];

                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                bytes = buffer.toByteArray();
            }
            if (name.equals(CLASS_TO_DUMP) && wrappedExtensions != null) {
                wrappedExtensions.info("Loading " + name);
            }
            FileInfo fileInfo = url == null ? null :
                    FL_JAR_IN_JAR_PROTOCOL.equals(url.getProtocol()) ?
                            FoxLoaderURLStreamHandler.JAR_IN_JAR_MAP.get(url.getPath()) :
                            this.fileInfoCache.get(url.toExternalForm());
            String tmpName = name.replace('/','.');
            if (wrappedExtensions != null && // Allow specialized
                    (tmpName.startsWith("xyz.wagyourtail.jvmdg.j") || !isSpecialClassName(tmpName))) {
                String internalName = name.replace('.','/');
                Map<String, byte[]> data = wrappedExtensions.downgradeClass(internalName, bytes);
                if (data != null) {
                    bytes = data.get(internalName);
                }
            } else if (tmpName.startsWith("xyz.wagyourtail.jvmdg.j") && bytes != null) {
                int version = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
                if (version > 52) { // Allow loading java8 code early
                    throw new ClassNotFoundException("Loaded too early: " + tmpName);
                }
            }
            // We need to patch MixinInfo to make MixinConstraints work universally.
            if (MIXIN_INFO.equals(tmpName)) {
                if (mixinInfoPatch == null) {
                    throw new Error("MixinInfo class loaded before wrappedExtensions");
                }
                bytes = mixinInfoPatch.apply(bytes);
            }
            // We need to apply some patches to mixins to make them actually work.
            if (wrappedExtensions != null && (bytes == null || !isTransformExclude(tmpName))) {
                try {
                    bytes = wrappedExtensions.transformClass(fileInfo, tmpName, bytes);
                } catch (Exception e) {
                    if (bytes != null) {
                        Files.write(new File(FoxLauncher.gameDir, "compute_fail.class").toPath(), bytes);
                    }
                    throw new ClassTransformException("Can't compute frames for "+name, e);
                } catch (Error error) {
                    if (bytes != null && !(error instanceof VirtualMachineError)) {
                        Files.write(new File(FoxLauncher.gameDir, "compute_fail.class").toPath(), bytes);
                    }
                    throw error;
                }
            }
            if (bytes == null) {
                throw new ClassNotFoundException(name);
            }
            // Mixins can increase the JVM version of a class, check again for downgrade.
            int targetJvm = (((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF) & 0xFFFF) - 44;
            if (targetJvm > Platform.getJvmVersion() && wrappedExtensions != null) {
                String internalName = name.replace('.','/');
                Map<String, byte[]> data = wrappedExtensions.downgradeClass(internalName, bytes);
                if (data != null) {
                    bytes = data.get(internalName);
                }
            }

            if (name.equals(CLASS_TO_DUMP)) {
                Files.write(new File(FoxLauncher.gameDir, "class_dump.class").toPath(), bytes);
                String loaderType = this.gameExclusiveSource == resourceClassLoader ? "exclusive" :
                        this == resourceClassLoader ? "main" : "parent";
                FoxLauncher.printEarlyStackTrace(
                        new Throwable("Dumped " + CLASS_TO_DUMP + " with source " + url +
                                " from " + loaderType + " loader"));
            }
            clas = defineClass(name,bytes,0,bytes.length,
                    fileInfo != null ? fileInfo.codeSource : codeSourceFromURL(url));
            return clas;
        } catch (ClassFormatError ioe) {
            if (bytes != null) try {
                Files.write(new File(FoxLauncher.gameDir, "load_fail.class").toPath(), bytes);
            } catch (IOException ignored) {}
            throw new ClassNotFoundException(name, ioe);
        } catch (ClassTransformException cte) {
            FoxLauncher.printEarlyStackTrace(cte);
            throw new ClassNotFoundException(name, cte);
        } catch (Exception ioe) {
            throw new ClassNotFoundException(name, ioe);
        }
    }

    private CodeSource codeSourceFromURL(final URL url) {
        if (url == null) return null;
        return this.codeSourceCache.computeIfAbsent(url.toString(),
                k -> new CodeSource(url, NO_CodeSigners));
    }

    public void injectMissingFileInfo(FileInfo fileInfo) {
        fileInfo.assertLocalFile();
        final String urlStr = fileInfo.source.toString();
        boolean inCls = FoxLauncher.foxLoaderFile == fileInfo.file;
        if (!inCls) {
            for (URL url : this.getURLs()) {
                if (url.toString().equals(urlStr)) {
                    inCls = true;
                    break;
                }
            }
            if (!inCls && this.reIndevURL != null &&
                    this.reIndevURL.toString().equals(urlStr)) {
                inCls = true;
            }
        }
        if (inCls) {
            this.fileInfoCache.putIfAbsent(urlStr, fileInfo);
        }
    }

    void injectMissingFileInfoUnchecked(FileInfo fileInfo) {
        this.fileInfoCache.putIfAbsent(fileInfo.source.toString(), fileInfo);
    }

    @Override
    public URL getResource(String name) {
        // Don't allow mods from adding classes in net.minecraft.
        if (isGamePath(name)) {
            if (gameExclusiveSource != null) {
                return gameExclusiveSource.findResource(name);
            } else {
                return this.getParent().getResource(name);
            }
        }
        if (name.startsWith("assets/foxloader/certificates/")) {
            return this.getParent().getResource(name);
        }
        URL resource = this.findResource(name);
        if (resource != null) return resource;
        if (gameExclusiveSource != null) {
            resource = gameExclusiveSource.findResource(name);
            if (resource != null) return resource;
        }
        return this.getParent().getResource(name);
    }

    @Override
    public URL findResource(String name) {
        if (isGamePath(name)) {
            if (gameExclusiveSource != null) {
                return gameExclusiveSource.findResource(name);
            } else {
                return this.getParent().getResource(name);
            }
        }
        URL resource = super.findResource(name);
        if (resource != null) return resource;
        return this.injectedResources.get(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        if (isGamePath(name)) {
            return Enumerations.optional(this.findResource(name));
        }
        return super.findResources(name);
    }

    public boolean isClassLoaded(String className) {
        return this.findLoadedClass(className) != null;
    }

    public boolean isClassInClassPath(String className) {
        final String path = className.replace('.', '/') + ".class";

        if (className.startsWith("com.fox2code.foxloader.")) {
            return this.getParent().getResource(path) != null;
        } else if (isGameClassName(className)) {
            if (gameExclusiveSource != null)
                return gameExclusiveSource.getResource(path) != null;
            return this.findResource(path) != null;
        } else {
            return this.getResource(path) != null;
        }
    }

    public boolean hasClass(String className) {
        return this.isClassLoaded(className) ||
                this.isClassInClassPath(className) ||
                this.injectedClasses.containsKey(className);
    }

    // Used to help with JVMDowngrader
    public byte[] accessGetRawClassBytes(String className) throws IOException {
        byte[] rawClassBytes = this.injectedClasses.get(className);
        if (rawClassBytes != null) return rawClassBytes;
        final String path = className.replace('.', '/') + ".class";
        final URL resource = this.getResource(path);
        return resource == null ? null : IOUtils.readAllBytes(resource.openStream());
    }

    public void injectRuntimeClass(String className, byte[] classData) {
        if (isClassLoaded(className))
            throw new IllegalStateException("Cannot redefine already loaded classes");
        this.injectedClasses.put(className, classData);
    }

    public void injectResource(String resourceName, URL resource) {
        if (isGamePath(resourceName))
            throw new IllegalArgumentException("Cannot redefine core game resource file");
        this.injectedResources.put(resourceName, resource);
    }

    public void addTransformerExclusion(String exclusion) {
        if (exclusion.startsWith("net.minecraft.") ||
                "net.minecraft.".startsWith(exclusion)) {
            throw new IllegalArgumentException(
                    "Cannot exclude the game itself: \"" + exclusion + "\"");
        }
        if (!this.exclusions.contains(exclusion)) {
            this.exclusions.add(exclusion);
        }
    }

    /**
     * Add URL to the class path
     *
     * @param url the URL to be added to the search path of URLs
     * @deprecated use {@link #addFileToClassLoader(FileInfo)} instead
     */
    @Override
    @Deprecated
    public void addURL(URL url) {
        // Just a wrapper for "addFileToClassLoader()"
        if (!"file".equals(url.getProtocol())) {
            throw new IllegalArgumentException("Can only accept \"file:/\" URLs");
        }
        try {
            File file = new File(url.toURI().getPath());
            if (!file.exists()) {
                throw new IOException("Target file do not exists (Path: " + file.getPath() + ")");
            }
            this.addFileToClassLoader(new FileInfo(file));
        } catch (IOException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void addFileToClassLoader(FileInfo fileInfo) {
        fileInfo.assertLocalFile();
        final String urlStr = fileInfo.source.toString();
        if (this.fileInfoCache.containsKey(urlStr)) {
            return;
        }
        this.fileInfoCache.put(urlStr, fileInfo);
        if (isJavaArchiveSafe(fileInfo)) {
            URL url = fileInfo.source;
            if (fileInfo.jarPath != null) {
                url = FoxLoaderURLStreamHandler.register(fileInfo);
            }
            super.addURL(url);
        }
    }

    public boolean isFileInClassLoader(File file) {
        if (file == null) {
            return false;
        }
        try {
            return this.fileInfoCache.containsKey(
                    file.toURI().toURL().toString());
        } catch (MalformedURLException e) {
            return false;
        }
    }

    void setOriginalSlimFileInfo(FileInfo slimFileInfo) {
        slimFileInfo.assertLocalFile();
        if (this.allowLoadingGame)
            throw new IllegalStateException("Minecraft jar already loaded!");
        this.gameExclusiveSource = new URLClassLoader(new URL[]{slimFileInfo.source}, null);
        this.patchedExclusiveSource = false;
        this.reIndevURL = slimFileInfo.source;
    }

    public void setPatchedSlimInfo(FileInfo slimFileInfo) {
        slimFileInfo.assertLocalFile();
        if (this.allowLoadingGame)
            throw new IllegalStateException("Minecraft jar already loaded!");
        if (this.patchedExclusiveSource)
            throw new IllegalStateException("Patched slim jar already set!");
        this.gameExclusiveSource = new URLClassLoader(new URL[]{slimFileInfo.source}, null);
        this.patchedExclusiveSource = true;
        this.reIndevURL = slimFileInfo.source;
    }

    public void allowLoadingGame() {
        if (this.allowLoadingGame) return;
        if (this.wrappedExtensions == null)
            throw new IllegalStateException("WrappedExtensions not installed yet");
        allowLoadingGame = true;
    }

    public boolean isAllowLoadingGame() {
        return this.allowLoadingGame;
    }

    public boolean isTransformExclude(String className) {
        for (String excl:exclusions) {
            if (className.startsWith(excl)) {
                return true;
            }
        }
        return false;
    }

    public void installMixinInfoPatch(Function<byte[], byte[]> mixinInfoPatch) {
        if (this.mixinInfoPatch != null)
            throw new IllegalStateException("MixinInfo Patch Already Installed!");
        this.mixinInfoPatch = Objects.requireNonNull(mixinInfoPatch);
    }

    public void installWrappedExtensions(WrappedExtensions wrappedExtensions) {
        if (this.wrappedExtensions != null)
            throw new IllegalStateException("Wrapped Extension Already Installed!");
        this.wrappedExtensions = Objects.requireNonNull(wrappedExtensions);
    }

    public FileInfo findFileInfoFromJarURL(URL resource) {
        return resource == null ? null :
                FL_JAR_IN_JAR_PROTOCOL.equals(resource.getProtocol()) ?
                        FoxLoaderURLStreamHandler.JAR_IN_JAR_MAP.get(resource.getPath()) :
                        this.fileInfoCache.get(resource.toExternalForm());
    }

    public static boolean isSpecialClassName(String cls) {
        // Check ears and mixins to fix them in development environment.
        return cls.startsWith("org.lwjgl.") ||
                cls.startsWith("org.bouncycastle.") ||
                cls.startsWith("com.unascribed.ears.") ||
                cls.startsWith("com.llamalad7.mixinextras.") ||
                cls.startsWith("com.bawnorton.mixinsquared.") ||
                cls.startsWith("com.moulberry.mixinconstraints.") ||
                cls.startsWith("fr.catcore.cursedmixinextensions.") ||
                cls.startsWith("org.spongepowered.") ||
                cls.startsWith("org.objectweb.asm.") ||
                cls.startsWith("com.fox2code.rebuild.") ||
                cls.startsWith("com.fox2code.foxevents.") ||
                // We also need to add Java21+ dependencies here to prevent crashes.
                cls.startsWith("blue.endless.jankson.") ||
                // Cover an edge case of loading spark in a development environment
                cls.startsWith("me.lucko.spark.") ||
                // Special case for JVMDowngrader
                cls.startsWith("xyz.wagyourtail.jvmdg.") ||
                // Special case for JFallback
                cls.startsWith("com.fox2code.jfallback.") ||
                cls.startsWith("jfallback.");
    }

    public static boolean isDevSpecialClassName(String cls) {
        return cls.startsWith("com.google.gson.");
    }

    public static boolean isFoxLoaderLaunchClass(String cls) {
        // Fix Gson when developing FoxLoader
        if (FoxLauncher.DEVELOPING_FOXLOADER && cls.startsWith("com.google.gson.")) {
            return true;
        }
        // Classes used by the Launcher and that can be loaded before FoxClassLoader is initialized.
        return cls.startsWith("com.fox2code.foxloader.dependencies.") ||
                cls.startsWith("com.fox2code.foxloader.launcher.") ||
                cls.startsWith("com.fox2code.foxloader.utils.");
    }

    public static boolean isGameClassName(String cls) {
        // Allow game pre-transforming
        return cls.startsWith("net.minecraft.") ||
                cls.startsWith("com.mojang.nbt.") ||
                cls.startsWith("com.indigo3d.") ||
                cls.startsWith("paulscode.sound.") ||
                cls.startsWith("com.jcraft.");
    }

    public static boolean isGamePath(String path) {
        // Only allow core-mods to modify these files
        return path.startsWith("net/minecraft/") ||
                path.startsWith("com/mojang/nbt/") ||
                path.startsWith("com/indigo3d/") ||
                path.startsWith("paulscode/sound/") ||
                path.startsWith("com/jcraft/") ||
                // pack.png & font.txt are protected game files
                path.equals("pack.png") || path.equals("font.txt");
    }

    public Collection<FileInfo> loadingClassPath() {
        return Collections.unmodifiableCollection(this.fileInfoCache.values());
    }

    private static boolean isJavaArchiveSafe(FileInfo fileInfo) {
        if (fileInfo.isRemote()) {
            return false;
        } else if (fileInfo.isJavaArchive0 != null) {
            return fileInfo.isJavaArchive0;
        } else {
            return fileInfo.file.getName().endsWith(".jar") &&
                    (fileInfo.jarPath == null || fileInfo.jarPath.endsWith(".jar"));
        }
    }

    private static class ClassTransformException extends Exception {
        public ClassTransformException(String message) {
            super(message);
        }

        public ClassTransformException(String message, Throwable e) {
            super(message, e);
        }
    }

    public static final class FoxLoaderURLStreamHandler implements URLStreamHandlerFactory {
        public static final FoxLoaderURLStreamHandler INSTANCE = new FoxLoaderURLStreamHandler();
        private static final URLStreamHandler HANDLER = new Handler();
        static final ConcurrentHashMap<String, FileInfo> JAR_IN_JAR_MAP = new ConcurrentHashMap<>();

        static URL register(FileInfo fileInfo) {
            FileInfo registeredInfo = JAR_IN_JAR_MAP.putIfAbsent(fileInfo.fileName, fileInfo);
            if (registeredInfo != fileInfo) {
                return null;
            }
            try {
                return new URL(FL_JAR_IN_JAR_PROTOCOL, null, -1, fileInfo.fileName, HANDLER);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        private FoxLoaderURLStreamHandler() {}

        @Override
        public URLStreamHandler createURLStreamHandler(String protocol) {
            return FL_JAR_IN_JAR_PROTOCOL.equals(protocol) ? HANDLER : null;
        }

        public static FileInfo getFileInfoForJarInJarPath(String path) {
            return FoxLoaderURLStreamHandler.JAR_IN_JAR_MAP.get(path);
        }
    }

    /**
     * Since we assume the current class loader may not have ASM loaded,
     * we must use a wrapper to use ASM "inside" the class loader.
     */
    public static abstract class WrappedExtensions {
        public abstract Map<String, byte[]> downgradeClass(final String className,final byte[] classData);

        public abstract byte[] transformClass(FileInfo fileInfo, String className, byte[] classData);

        public abstract void info(String message);
    }
}
