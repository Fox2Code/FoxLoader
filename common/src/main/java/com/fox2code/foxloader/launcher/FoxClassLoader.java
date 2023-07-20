package com.fox2code.foxloader.launcher;

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
    private final HashMap<String, byte[]> injectedClasses;
    private URLClassLoader minecraftExclusiveSource;
    private boolean allowLoadingGame;
    private WrappedExtensions wrappedExtensions;
    private ArrayList<URL> coreMods;
    private boolean didPrintedTransformFail = false;
    static URL earlyMinecraftURL;

    FoxClassLoader() {
        super(new URL[0], FoxClassLoader.class.getClassLoader());
        this.exclusions = new LinkedList<>();
        this.classTransformers = new LinkedList<>();
        this.injectedClasses = new HashMap<>();
        // Allow to set a Minecraft URL before loader is initialized.
        if (earlyMinecraftURL != null) {
            this.setMinecraftURL(earlyMinecraftURL);
            earlyMinecraftURL = null;
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("net.minecraft.") ||
                name.startsWith("paulscode.sound.") ||
                name.startsWith("com.jcraft.")) {
            if (!allowLoadingGame) {
                throw new ClassNotFoundException("Cannot load \"" + name + "\" during pre init");
            }
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                c = findClassImpl(name, null);
            }
            return c;
        } else if ((name.startsWith("com.fox2code.foxloader.") &&
                    !name.startsWith("com.fox2code.foxloader.launcher.")) ||
                // Check mixins to fix them in development environment.
                name.startsWith("com.llamalad7.mixinextras.") ||
                name.startsWith("org.spongepowered.") ||
                name.startsWith("org.objectweb.asm.")) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                c = findClassImpl(name, null);
            }
            return c;
        } else {
            Class<?> c = findLoadedClass(name);
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
            return c;
        }
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
            } else if ((name.startsWith("net.minecraft.") ||
                    name.startsWith("paulscode.sound.") ||
                    name.startsWith("com.jcraft."))) {
                resourceClassLoader = // Do not allow mods to act as jar mods blindly.
                        minecraftExclusiveSource != null ?
                        minecraftExclusiveSource : getParent();
            } else {
                resourceClassLoader = this;
            }
            if (resource == null) {
                resource =
                        resourceClassLoader.getResource(
                                name.replace('.', '/').concat(".class"));
            }
            URLConnection urlConnection;
            if (resource == null) {
                urlConnection = null;
                bytes = injectedClasses.remove(name);
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
                // We need to apply some patches to mixins to make the actually work.
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
            }
            if (name.equals(CLASS_TO_DUMP)) {
                Files.write(new File(FoxLauncher.gameDir, "class_dump.class").toPath(), bytes);
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
        if ((name.startsWith("net/minecraft/") &&
                name.endsWith(".class")) ||
                name.equals("font.txt")) {
            if (minecraftExclusiveSource != null) {
                return minecraftExclusiveSource.findResource(name);
            } else {
                return this.getParent().getResource(name);
            }
        }
        if (name.startsWith("assets/foxloader/certificates/")) {
            return this.getParent().getResource(name);
        }
        URL resource = this.findResource(name);
        if (resource != null) return resource;
        if (minecraftExclusiveSource != null) {
            resource = minecraftExclusiveSource.findResource(name);
            if (resource != null) return resource;
        }
        return this.getParent().getResource(name);
    }

    public boolean isClassLoaded(String className) {
        return this.findLoadedClass(className) != null;
    }

    public void addClassTransformers(ClassTransformer classTransformer) {
        String pkg = classTransformer.getClass().getPackage().getName();
        if (!isTransformExclude(pkg)) {
            exclusions.add(pkg);
        }
        classTransformers.add(classTransformer);
    }

    public void injectRuntimeClass(String className, byte[] classData) {
        if (isClassLoaded(className))
            throw new IllegalStateException("Cannot redefine already loaded classes");
        injectedClasses.put(className, classData);
    }

    public int getClassTransformersCount() {
        return classTransformers.size();
    }

    public void addTransformerExclusion(String exclusion) {
        exclusions.add(exclusion);
    }

    public void addURL(URL url) {
        super.addURL(url);
    }

    public void addCoreModURL(URL url) {
        if (allowLoadingGame)
            throw new IllegalStateException("Minecraft jar already loaded!");
        if (coreMods == null)
            coreMods = new ArrayList<>(16);
       coreMods.add(url);
    }

    public void setMinecraftURL(URL url) {
        if (allowLoadingGame)
            throw new IllegalStateException("Minecraft jar already loaded!");
        minecraftExclusiveSource = new URLClassLoader(makeURLClassPathForSource(url), null);
    }

    public void setPatchedMinecraftURL(URL url) {
        if (allowLoadingGame)
            throw new IllegalStateException("Minecraft jar already loaded!");
        minecraftExclusiveSource = new URLClassLoader(new URL[]{url}, null);
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
                setMinecraftURL(getMinecraftSource());
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
        } else if (earlyMinecraftURL != null) {
            return earlyMinecraftURL;
        } else throw new IOException("Invalid reindev.jar source...");
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
