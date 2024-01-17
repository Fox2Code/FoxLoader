package com.fox2code.foxloader.launcher;

import com.fox2code.foxloader.launcher.utils.Enumerations;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Files;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.*;

public final class FoxClassLoader extends URLClassLoader {
    private static final String CLASS_TO_DUMP = System.getProperty("foxloader.dump-class");
    private static final String MIXIN_CONFIG = "org.spongepowered.asm.mixin.transformer.MixinConfig";
    private static final String MIXIN_PRE_PROCESSOR = "org.spongepowered.asm.mixin.transformer.MixinPreProcessorStandard";
    private static final URL[] NO_URLs = new URL[0];
    private final LinkedList<String> exclusions;
    private final LinkedList<ClassTransformer> classTransformers;
    private final LinkedList<ClassGenerator> classGenerators;
    private final HashMap<String, byte[]> injectedClasses;
    private URLClassLoader gameExclusiveSource;
    private boolean allowLoadingGame;
    private WrappedExtensions wrappedExtensions;
    private ArrayList<URL> coreMods;
    private boolean didPrintedTransformFail = false;
    private boolean patchedExclusiveSource = false;
    private URL minecraftURL;
    static URL earlyMinecraftURL;

    FoxClassLoader() {
        super(new URL[0], FoxClassLoader.class.getClassLoader());
        this.exclusions = new LinkedList<>();
        this.classTransformers = new LinkedList<>();
        this.classGenerators = new LinkedList<>();
        this.injectedClasses = new HashMap<>();
        this.classGenerators.add(this.injectedClasses::remove);
        // Allow to set a Minecraft URL before loader is initialized.
        if (earlyMinecraftURL != null) {
            this.setMinecraftURL(earlyMinecraftURL);
            earlyMinecraftURL = null;
        }
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
                c = findClassImpl(name, null);
            }
        } else if ((name.startsWith("com.fox2code.foxloader.") &&
                    !name.startsWith("com.fox2code.foxloader.launcher.")) ||
                // Check mixins to fix them in development environment.
                isSpecialClassName(name)) {
            c = findLoadedClass(name);
            if (c == null) {
                c = findClassImpl(name, null);
            }
        } else {
            c = findLoadedClass(name);
            if (c == null) {
                URL resource = findResource(name.replace('.', '/') + ".class");
                if (resource != null) {
                    c = findClassImpl(name, resource);
                } else try {
                    c = super.loadClass(name, false);
                } catch (SecurityException securityException) {
                    throw new ClassNotFoundException(name, securityException);
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
                        gameExclusiveSource != null ?
                                gameExclusiveSource : getParent();
            } else {
                resourceClassLoader = this;
            }
            if (resource == null) {
                resource =
                        resourceClassLoader.getResource(
                                name.replace('.', '/').concat(".class"));
            }
            URLConnection urlConnection;
            ClassGenerator generator = null;
            if (resource == null) {
                urlConnection = null;
                for (ClassGenerator classGenerator : this.classGenerators) {
                    bytes = classGenerator.generate(name);
                    if (bytes != null) {
                        generator = classGenerator;
                        break;
                    }
                }
                if (bytes == null) {
                    throw new ClassNotFoundException(name);
                }
            } else {
                urlConnection = resource.openConnection();
                InputStream is = urlConnection.getInputStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                int nRead;
                byte[] data = new byte[16384];

                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                bytes = buffer.toByteArray();
            }
            String tmpName = name.replace('/','.');
            if (wrappedExtensions != null && !isTransformExclude(tmpName)) {
                for (ClassTransformer classTransformer : classTransformers) {
                    if (classTransformer == generator) continue;
                    try {
                        bytes = classTransformer.transform(bytes, tmpName);
                    } catch (Throwable e) {
                        if (!this.didPrintedTransformFail) {
                            this.didPrintedTransformFail = true; // Only print first failure.
                            Files.write(new File(FoxLauncher.gameDir, "transform_fail.class").toPath(), bytes);
                        }
                        throw new ClassTransformException("Can't transform "+name+" for "+ classTransformer.getClass().getName(), e);
                    }
                }
                try {
                    bytes = wrappedExtensions.computeFrames(bytes);
                } catch (Exception e) {
                    Files.write(new File(FoxLauncher.gameDir, "compute_fail.class").toPath(), bytes);
                    throw new ClassTransformException("Can't compute frames for "+name, e);
                }
            } else switch (name) {
                // We need to apply some patches to mixins to make them actually work.
                case MIXIN_CONFIG:
                    if (wrappedExtensions == null)
                        throw new ClassTransformException("wrappedExtensions not initialized yet");
                    bytes = wrappedExtensions.patchMixinConfig(bytes);
                    break;
                case MIXIN_PRE_PROCESSOR:
                    if (wrappedExtensions == null)
                        throw new ClassTransformException("wrappedExtensions not initialized yet");
                    bytes = wrappedExtensions.patchMixinPreProcessorStandard(bytes);
                    break;
            }

            URL url = null;
            if (urlConnection instanceof JarURLConnection) {
                url = ((JarURLConnection) urlConnection).getJarFileURL();
            } else if (generator != null) {
                url = generator.source(name);
            }
            if (name.equals(CLASS_TO_DUMP)) {
                Files.write(new File(FoxLauncher.gameDir, "class_dump.class").toPath(), bytes);
                String loaderType = this.gameExclusiveSource == resourceClassLoader ? "exclusive" :
                        this == resourceClassLoader ? "main" : "parent";
                new Throwable("Dumped " + CLASS_TO_DUMP + " with source " + url +
                        " from " + loaderType + " loader").printStackTrace();
            }
            clas = defineClass(name,bytes,0,bytes.length, url == null ?
                    null : new CodeSource(url, new CodeSigner[]{}));
            return clas;
        } catch (ClassFormatError ioe) {
            if (bytes != null) try {
                Files.write(new File(FoxLauncher.gameDir, "load_fail.class").toPath(), bytes);
            } catch (IOException ignored) {}
            throw new ClassNotFoundException(name, ioe);
        } catch (ClassTransformException cte) {
            cte.printStackTrace();
            throw new ClassNotFoundException(name, cte);
        } catch (Exception ioe) {
            throw new ClassNotFoundException(name, ioe);
        }
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
        return super.findResource(name);
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
        } else if (isSpecialClassName(className)) {
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

    public void addClassTransformers(ClassTransformer classTransformer) {
        String pkg = classTransformer.getClass().getPackage().getName();
        if (!isTransformExclude(pkg)) {
            this.exclusions.add(pkg);
        }
        this.classTransformers.add(classTransformer);
    }

    public void addClassGenerator(ClassGenerator classGenerator) {
        this.classGenerators.add(Objects.requireNonNull(classGenerator));
    }

    public void injectRuntimeClass(String className, byte[] classData) {
        if (isClassLoaded(className))
            throw new IllegalStateException("Cannot redefine already loaded classes");
        this.injectedClasses.put(className, classData);
    }

    public int getClassTransformersCount() {
        return this.classTransformers.size();
    }

    public int getClassGeneratorsCount() {
        return this.classGenerators.size();
    }

    public void addTransformerExclusion(String exclusion) {
        this.exclusions.add(exclusion);
    }

    public void addURL(URL url) {
        super.addURL(url);
    }

    public void addCoreModURL(URL url) {
        if (this.allowLoadingGame)
            throw new IllegalStateException("Minecraft jar already loaded!");
        if (this.coreMods == null)
            this.coreMods = new ArrayList<>(16);
        this.coreMods.add(url);
    }

    public void setMinecraftURL(URL url) {
        if (this.allowLoadingGame)
            throw new IllegalStateException("Minecraft jar already loaded!");
        this.gameExclusiveSource = new URLClassLoader(makeURLClassPathForSource(url), null);
        this.patchedExclusiveSource = false;
        this.minecraftURL = url;
    }

    public void setPatchedMinecraftURL(URL url) {
        if (this.allowLoadingGame)
            throw new IllegalStateException("Minecraft jar already loaded!");
        URL minecraftURL;
        try {
            minecraftURL = this.getOriginalMinecraftSource();
        } catch (IOException e) {
            throw new IllegalStateException("Original Minecraft jar not set", e);
        }
        this.gameExclusiveSource = new URLClassLoader(new URL[]{url, minecraftURL}, null);
        this.patchedExclusiveSource = true;
        this.minecraftURL = minecraftURL;
    }

    public URL[] makeURLClassPathForSource(URL source) {
        if (coreMods != null) {
            ArrayList<URL> urls = new ArrayList<>(coreMods); urls.add(source);
            return urls.toArray(NO_URLs);
        } else {
            return new URL[]{source};
        }
    }

    public void allowLoadingGame() {
        if (allowLoadingGame) return;
        if (coreMods != null) {
            try {
                if (!patchedExclusiveSource) {
                    setMinecraftURL(getOriginalMinecraftSource());
                }
                coreMods = null;
                allowLoadingGame = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else allowLoadingGame = true;
    }

    public URL getMinecraftSource() throws IOException {
        URLConnection urlConnection = Objects.requireNonNull(
                this.getResource("font.txt")).openConnection();
        if (urlConnection instanceof JarURLConnection) {
          return ((JarURLConnection) urlConnection).getJarFileURL();
        } else if (this.minecraftURL != null) {
            return this.minecraftURL;
        } else throw new IOException("Invalid reindev.jar source...");
    }

    public URL getOriginalMinecraftSource() throws IOException {
        return this.minecraftURL != null ? this.minecraftURL : this.getMinecraftSource();
    }

    public boolean isAllowLoadingGame() {
        return allowLoadingGame;
    }

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public boolean isTransformExclude(String className) {
        for (String excl:exclusions) {
            if (className.startsWith(excl)) {
                return true;
            }
        }
        return false;
    }

    public void installWrappedExtensions(WrappedExtensions wrappedExtensions) {
        if (this.wrappedExtensions != null)
            throw new IllegalStateException("Wrapped Extension Already Installed!");
        this.wrappedExtensions = Objects.requireNonNull(wrappedExtensions);
    }

    public static boolean isSpecialClassName(String cls) {
        // Check mixins to fix them in development environment.
        return cls.startsWith("com.llamalad7.mixinextras.") ||
                cls.startsWith("com.bawnorton.mixinsquared.") ||
                cls.startsWith("org.spongepowered.") ||
                cls.startsWith("org.objectweb.asm.");
    }

    public static boolean isGameClassName(String cls) {
        // Allow game pre-transforming
        return cls.startsWith("net.minecraft.") ||
                cls.startsWith("com.indigo3d.") ||
                cls.startsWith("paulscode.sound.") ||
                cls.startsWith("com.jcraft.");
    }

    public static boolean isGamePath(String path) {
        // Only allow core-mods to modify these files
        return path.startsWith("net/minecraft/") ||
                path.startsWith("com/indigo3d/") ||
                path.startsWith("paulscode/sound/") ||
                path.startsWith("com/jcraft/") ||
                // font.txt is a protected game file
                path.equals("font.txt");
    }

    private static class ClassTransformException extends Exception {
        public ClassTransformException(String message) {
            super(message);
        }

        public ClassTransformException(String message, Throwable e) {
            super(message, e);
        }
    }

    /**
     * Since we assume the current class loader may not have ASM loaded,
     * we must use a wrapper to use ASM "inside" the class loader.
     */
    public static abstract class WrappedExtensions {
        public abstract byte[] computeFrames(byte[] classData);

        public abstract byte[] patchMixinConfig(byte[] classData);

        public abstract byte[] patchMixinPreProcessorStandard(byte[] classData);
    }
}
