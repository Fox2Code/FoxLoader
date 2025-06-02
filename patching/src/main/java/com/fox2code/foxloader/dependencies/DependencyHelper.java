package com.fox2code.foxloader.dependencies;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.utils.SourceUtil;
import com.fox2code.foxloader.utils.io.IOUtils;
import com.fox2code.foxloader.utils.io.NetUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.*;
import java.nio.file.Files;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DependencyHelper {
    private static final File JAR_FILE = SourceUtil.getSourceFile(DependencyHelper.class);
    private static final File JAR_FOLDER = JAR_FILE.getParentFile();
    private static final Logger LOGGER = Logger.getLogger("FoxLoader");
    public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";
    public static final String SLEEPING_TOWN = "https://repo.sleeping.town/";
    public static final String FABRIC_MC = "https://maven.fabricmc.net/";
    public static final String MODRINTH = "https://api.modrinth.com/maven";
    public static final String FOX2CODE = "https://cdn.fox2code.com/maven";
    public static final String UNASCRIBED = "https://repo.unascribed.com";

    public static final Dependency foxLoader = new Dependency(
            "com.fox2code.FoxLoader:loader:" + BuildConfig.FOXLOADER_VERSION,
            FOX2CODE, "com.fox2code.foxloader.loader.ModLoader");

    public static final Dependency jansi = new Dependency(
            "org.fusesource.jansi:jansi:" + BuildConfig.JANSI_VERSION, MAVEN_CENTRAL,
            "org.fusesource.jansi.AnsiConsole", null, "b7b8b003a90ea491579b62f5118828e45112914c65589b00faa49d6ec785839");

    private static final Dependency annotations =
            new Dependency("org.jetbrains:annotations:24.1.0", MAVEN_CENTRAL,
                    "org.jetbrains.annotations.NotNull", null, "27a770dc7ce50500918bb8c3c0660c98290630ec796b5e3cf6b90f403b3033c6");

    public static final Dependency GSON_DEPENDENCY = // Used by installer.
            new Dependency("com.google.code.gson:gson:2.10.1", MAVEN_CENTRAL,
                    "com.google.gson.Gson", null, "4241c14a7727c34feea6507ec801318a3d4a90f070e4525681079fb94ee4c593");

    // Extra dependencies not included in ReIndev
    public static final Dependency jvmDowngraderCore =
            new Dependency("xyz.wagyourtail.jvmdowngrader:jvmdowngrader:" + BuildConfig.JVM_DOWNGRADER_VERSION,
                    MAVEN_CENTRAL, "xyz.wagyourtail.jvmdg.ClassDowngrader", null, "67a4af05344288af64d3c2ca6aaca11cb6e6325cd4d5086362a9beec35b9c2ae");
    public static final Dependency jvmDowngraderJavaAPI =
            new Dependency("xyz.wagyourtail.jvmdowngrader:jvmdowngrader-java-api:" + BuildConfig.JVM_DOWNGRADER_VERSION,
                    MAVEN_CENTRAL, "xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_ClassLoader", null, "7bb394e54a1908739f4493eeeb1898aa8bf74f925a9b494cb773e6ce75e2ade1");

    public static final Dependency[] commonDependencies = new Dependency[]{
            new Dependency("org.slf4j:slf4j-api:" + BuildConfig.SLF4J_VERSION, MAVEN_CENTRAL, "org.slf4j.Logger", null, "7b751d952061954d5abfed7181c1f645d336091b679891591d63329c622eb832"),
            new Dependency("org.ow2.asm:asm:" + BuildConfig.ASM_VERSION, MAVEN_CENTRAL, "org.objectweb.asm.ClassVisitor", null, "876eab6a83daecad5ca67eb9fcabb063c97b5aeb8cf1fca7a989ecde17522051"),
            new Dependency("org.ow2.asm:asm-tree:" + BuildConfig.ASM_VERSION, MAVEN_CENTRAL, "org.objectweb.asm.tree.ClassNode", null, "14b7880cb7c85eed101e2710432fc3ffb83275532a6a894dc4c4095d49ad59f1"),
            new Dependency("org.ow2.asm:asm-analysis:" + BuildConfig.ASM_VERSION, MAVEN_CENTRAL, "org.objectweb.asm.tree.analysis.Analyzer", null , "e640732fbcd3c6271925a504f125e38384688f4dfbbf92c8622dfcee0d09edb9"),
            new Dependency("org.ow2.asm:asm-commons:" + BuildConfig.ASM_VERSION, MAVEN_CENTRAL, "org.objectweb.asm.commons.InstructionAdapter", null, "3301a1c1cb4c59fcc5292648dac1d7c5aed4c0f067dfbe88873b8cdfe77404f4"),
            new Dependency("org.ow2.asm:asm-util:" + BuildConfig.ASM_VERSION, MAVEN_CENTRAL, "org.objectweb.asm.util.CheckClassAdapter", null, "8ba0460ecb28fd0e2980e5f3ef3433a513a457bc077f81a53bdc75b587a08d15"),
            GSON_DEPENDENCY, new Dependency("com.google.guava:guava:21.0", MAVEN_CENTRAL, "com.google.common.io.Files", null, "972139718abc8a4893fa78cba8cf7b2c903f35c97aaf44fa3031b0669948b480"),
            new Dependency("it.unimi.dsi:fastutil-core:" + BuildConfig.FASTUTIL_VERSION, MAVEN_CENTRAL, "it.unimi.dsi.fastutil.Pair", null, "379a73e00790fc865318acc0550e2e8d108934758295df18bce6719bd0038c6d"),
            new Dependency("com.unascribed:ears-api:" + BuildConfig.EARS_VERSION, UNASCRIBED, "com.unascribed.ears.api.features.EarsFeatures", null, "6482fe4f9473c1b49e0513684e95647a95dca289528b5fdcad3347c193500871"),
            new Dependency("com.unascribed:ears-common:" + BuildConfig.EARS_VERSION, UNASCRIBED, "com.unascribed.ears.common.EarsCommon", null, "8ec4fb89e30901abf1f9d35c5ed5ac4d0d57d212dc4f4260566b33fbc826ca0d"),
            new Dependency("net.fabricmc:sponge-mixin:" + BuildConfig.FABRIC_MIXIN_VERSION, FABRIC_MC,
                    "org.spongepowered.asm.mixin.Mixins", null, "1dd2b778ed5283bce6b6b07d9690d86d956b17a7103efafad47073db1599584"),
            new Dependency("io.github.llamalad7:mixinextras-common:" + BuildConfig.MIXIN_EXTRAS_VERSION, MAVEN_CENTRAL,
                    "com.llamalad7.mixinextras.MixinExtrasBootstrap", null, "6a2c6f39f285348635ba1e0e914d066fe718c207e220a49012e8b347cb27fbda"),
            new Dependency("com.bawnorton.mixinsquared:mixinsquared-common:" + BuildConfig.MIXIN_SQUARED_VERSION,
                    FOX2CODE, "com.bawnorton.mixinsquared.MixinSquaredBootstrap", null, "d80619866e6d8c00bdeeaf6484357c1bbd4006fe0154537e9137213ab82bdaca"),
            new Dependency("com.fox2code:ReBuild:" + BuildConfig.REBUILD_VERSION, FOX2CODE,
                    "com.fox2code.rebuild.ClassDataProvider", null, "6bb4ac7ae84ec752e505c4dc885c003e2550baf4c327a5c9f9c44379a756e43d"),
            new Dependency("com.fox2code.FoxEvents:core:" + BuildConfig.FOX_EVENTS_VERSION, FOX2CODE,
                    "com.fox2code.foxevents.FoxEvents", null, "19a43c993892ea3dd80d66bcb1a8b12ff4d96f0774f913c62bd34bb395c7af5f"),
            new Dependency("com.fox2code:FoxFlexVer:" + BuildConfig.FOX_FLEX_VER_VERSION, FOX2CODE,
                    "com.fox2code.flexver.FlexVer", null, "4cf356d6c05c1a7008d90500945df21e4bac32e3a09309efc012a5a373431c0b"),
            jvmDowngraderCore, jvmDowngraderJavaAPI, // jvmDowngrader has special handling in dev plugin
    };

    public static final Dependency[] commonDependenciesModernJava = new Dependency[]{
            new Dependency("blue.endless:jankson:" + BuildConfig.JANKSON_VERSION, SLEEPING_TOWN,
                    "blue.endless.jankson.api.Jankson", null,
                    "3816dd5705f4d6e097fca9ddce79baa105931baacd5e4c0912fdb5f1c8c091e3", 21),
            new Dependency("com.moulberry:mixinconstraints:" + BuildConfig.MIXIN_CONSTRAINTS_VERSION,
                    MAVEN_CENTRAL, "com.moulberry.mixinconstraints.MixinConstraints", null,
                    "942b311295c144eb4bc4accc7164d6779ebfcc5856359ff9664128b0a95f6046", 21),
    };

    public static final Dependency sparkDependency =
            new Dependency(BuildConfig.SPARK_DEPENDENCY, MODRINTH, "me.lucko.spark.common.SparkPlugin",
                    null, "1e25b3ca4b7278ff3fdc4f7baa5692d30c2ac8be80b7eae9e1c4edb09f74b753");

    public static final Dependency vineFlower = new Dependency(
            BuildConfig.VINEFLOWER_DEPENDENCY, MAVEN_CENTRAL,
            "org.jetbrains.java.decompiler.main.Fernflower", null,
            "a615d07ddbbcd489369674f40e42df639c32be95410890b38f173d5c1e2ea39c", 17);

    public static final Dependency reIndevDependencySlim =
            new Dependency("net.silveros:reindev-slim:" + BuildConfig.REINDEV_VERSION,
                    BuildConfig.SLIM_URL, "net.minecraft.server.MinecraftServer",
                    null, BuildConfig.SLIM_SHA256_SUM);

    private static File mcLibraries;

    public static File getLibrariesFolder() {
        return Objects.requireNonNull(mcLibraries);
    }

    public static void loadCoreDependencies(boolean client) {
        if (client) {
            URL url = DependencyHelper.class.getResource("/org/lwjgl/opengl/GLChecks.class");
            if (url != null) {
                try {
                    URLConnection urlConnection = url.openConnection();
                    if (urlConnection instanceof JarURLConnection) {
                        url = ((JarURLConnection) urlConnection).getJarFileURL();
                        String lwjglPath = url.toURI().getPath();
                        int i = lwjglPath.indexOf("org/lwjgl/");
                        if (i != -1 && lwjglPath.endsWith(".jar")) {
                            mcLibraries = new File(lwjglPath.substring(0, i)).getAbsoluteFile();
                        }
                    }
                } catch (IOException | URISyntaxException ignored) {}
            }
            setMCLibraryRoot();
        } else {
            mcLibraries = new File("libraries").getAbsoluteFile();
        }
        for (Dependency dependency : commonDependencies) {
            loadDependencyBuiltIn(dependency);
        }
        if (!(DependencyImpl.CURRENT_IMPL.isDevelopingFoxLoader() ||
                DependencyImpl.CURRENT_IMPL.isDevelopingMod())) {
            loadDependencyImpl(reIndevDependencySlim, true, false, true, null, false);
        }
        checkDependency(annotations);
        if (DependencyImpl.CURRENT_IMPL.isDevelopingFoxLoader()) {
            loadDependencyAsFile(vineFlower);
        }
    }

    // Modern java dependencies need to be loaded after JVM downgrader has been installed
    public static void loadModernJavaDependencies() {
        for (Dependency dependency : commonDependenciesModernJava) {
            loadDependency(dependency);
        }
    }

    public static void setMCLibraryRoot() {
        mcLibraries = DependencyImpl.CURRENT_IMPL.checkMCLibraryRoot(mcLibraries);
    }

    public static void setMCLibraryRoot(File mcLibraryRoot) {
        if (!DependencyImpl.CURRENT_IMPL.isDev()) {
            throw new IllegalStateException("Not in dev mode");
        }
        if (Objects.equals(mcLibraries, mcLibraryRoot)) {
            return;
        }
        if (mcLibraries != null) {
            throw new IllegalStateException("mcLibraryRoot has been already set!");
        }
        mcLibraries = mcLibraryRoot;
    }

    public static File loadDependencySafe(Dependency dependency) {
        if (dependency == null) return null;
        try {
            return loadDependencyImpl(dependency, false, DependencyImpl.CURRENT_IMPL.isDev(), false, null, false);
        } catch (Exception e) {
            boolean fatal = DependencyImpl.CURRENT_IMPL.isDevelopingFoxLoader() &&
                    e.getMessage().startsWith("Mismatching hash for ");
            LOGGER.log(fatal ? Level.SEVERE : Level.WARNING,
                    "Loading library " + dependency.name + " failed", e);
            if (fatal) {
                System.exit(1);
            }
            return null;
        }
    }

    public static File loadDependency(Dependency dependency) {
        return loadDependencyImpl(dependency, false, DependencyImpl.CURRENT_IMPL.isDev(), false, null, false);
    }

    private static void loadDependencyBuiltIn(Dependency dependency) {
        loadDependencyImpl(dependency, false, DependencyImpl.CURRENT_IMPL.isDev(), true, null, false);
    }

    private static File loadDependencyImpl(
            Dependency dependency, boolean minecraft, boolean dev, boolean builtIn, File alternative, boolean dry) {
        if ((dependency == foxLoader || !dev) && hasClass(dependency.classCheck)) {
            File file = SourceUtil.getSourceFileOfClassName(getClassCheck(dependency));
            if (file == null) {
                URL url = DependencyImpl.CURRENT_IMPL.getClassResource(dependency.classCheck);
                throw new IllegalStateException(dependency.classCheck + " -> " + url);
            }
            if (dependency != foxLoader && DependencyImpl.CURRENT_IMPL.isDevelopingFoxLoader()) {
                String realSha256 = null;
                try {
                    realSha256 = new BigInteger(1, IOUtils.sha256Of(file)).toString(16);
                } catch (IOException e) {
                    DependencyImpl.CURRENT_IMPL.printStackTrace(e);
                }
                if (realSha256 != null && !realSha256.equals(dependency.sha256Sum)) {
                    throw new IllegalStateException( // This is a sanity check when in development
                            "Mismatching hash for " + dependency.name +
                                    " (" + dependency.sha256Sum + " != " + realSha256 + ")");

                }
            }
            if (!dev) {
                try {
                    DependencyImpl.CURRENT_IMPL.checkAddDependency(file, dependency);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load " + dependency.name, e);
                }
            }
            return file;
        }
        if (dry) return null;
        if (dependency == foxLoader) {
            throw new IllegalStateException("In DependencyHelper, but missing in gradle? ");
        }
        if (builtIn && DependencyImpl.CURRENT_IMPL.isDevelopingFoxLoader()) {
            throw new IllegalStateException( // This is a sanity check when in development
                    "In DependencyHelper, but missing in gradle? " + dependency);
        }
        if (dev && alternative != null && alternative.isFile()) {
            if (checkHashOrDeleteEx(alternative, dependency, false, true)) {
                return alternative;
            }
        }
        // Allow providing slim jar file manually or for local testing.
        if ((dev || minecraft) && dependency == reIndevDependencySlim) {
            // localFileDev is useful for local testing.
            File localFileDev = new File(JAR_FOLDER, BuildConfig.SLIM_JAR_NAME);
            if (checkHashOrDeleteEx(localFileDev, dependency, false, true)) {
                if (minecraft && !dev) {
                    try {
                        DependencyImpl.CURRENT_IMPL.addDependency(localFileDev, dependency, true);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load " + dependency.name, e);
                    }
                }
                return localFileDev;
            }
        }
        String postURL = resolvePostURL(dependency.name);
        File file = new File(mcLibraries, fixUpPath(postURL));
        if (!file.isAbsolute()) file = file.getAbsoluteFile();
        boolean justDownloaded = false;
        checkHashOrDelete(file, dependency, false);
        if (!file.exists() && !"null".equals(dependency.repository)) {
            File parentFile = file.getParentFile();
            if (!parentFile.isDirectory() && !parentFile.mkdirs()) {
                throw new RuntimeException("Cannot create dependency directory for " + dependency.name);
            }
            IOException fallBackIoe = null;
            try (OutputStream os = Files.newOutputStream(file.toPath())) {
                justDownloaded = true;
                NetUtils.downloadTo(new URL(dependency.repository.endsWith(".jar") ?
                        dependency.repository : dependency.repository + "/" + postURL), os);
            } catch (IOException ioe) {
                if (dependency.fallbackUrl != null) {
                    fallBackIoe = ioe;
                } else {
                    if (file.exists() && !file.delete()) file.deleteOnExit();
                    throw new RuntimeException("Cannot download " + dependency.name, ioe);
                }
            }
            if (fallBackIoe != null) {
                try (OutputStream os = Files.newOutputStream(file.toPath())) {
                    justDownloaded = true;
                    NetUtils.downloadTo(new URL(dependency.fallbackUrl), os);
                } catch (IOException ioe) {
                    if (file.exists() && !file.delete()) file.deleteOnExit();
                    throw new RuntimeException("Cannot download " + dependency.name, fallBackIoe);
                }
            }
        }
        checkHashOrDelete(file, dependency, true);
        if (dev) return file; // We don't have a FoxClass loader in dev environment.
        try {
            DependencyImpl.CURRENT_IMPL.addDependency(file, dependency, minecraft);
            if (hasClass(dependency.classCheck)) {
                System.out.println("Loaded " +
                        dependency.name + " -> " + file.getPath());
            } else {
                if (!justDownloaded) {
                    // Assume file is corrupted if load failed.
                    if (file.exists() && !file.delete()) file.deleteOnExit();
                    loadDependency(dependency);
                    return file;
                }
                throw new RuntimeException("Failed to load " +
                        dependency.name + " -> " + file.getPath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + dependency.name, e);
        }
        return file;
    }

    private static String getClassCheck(Dependency dependency) {
        return dependency.classCheck;
    }

    private static void checkHashOrDelete(File file, Dependency dependency, boolean errorOut) {
        checkHashOrDeleteEx(file, dependency, errorOut, false);
    }

    private static boolean checkHashOrDeleteEx(File file, Dependency dependency, boolean errorOut, boolean keepFile) {
        if (dependency.sha256Sum == null || !file.exists()) return false;
        String hashString;
        try {
            hashString = new BigInteger(1, IOUtils.sha256Of(file)).toString(16);
        } catch (IOException e) {
            hashString = "";
        }
        if (!dependency.sha256Sum.equals(hashString)) {
            boolean deleteSuccessful = keepFile || file.delete();
            if (errorOut) {
                throw new RuntimeException("Remote dependency " + dependency.name + " checksum mismatch " +
                        "(got: " + hashString + ", expected: " + dependency.sha256Sum + ")");
            }
            if (!deleteSuccessful) {
                throw new RuntimeException("Can't delete dependency with checksum mismatch " + dependency.name);
            }
            return false;
        }
        return true;
    }

    public static boolean skipDevSources(Dependency dependency) {
        return dependency.name.startsWith("com.unascribed:ears-");
    }

    public static class Agent {
        private static Instrumentation inst = null;

        public static void premain(final String agentArgs, final Instrumentation inst) {
            if (DependencyImpl.CURRENT_IMPL.isClassLoaderInitialized())
                throw new IllegalStateException("FoxClassLoader already initialized!");
            Agent.inst = inst;
        }

        public static void agentmain(final String agentArgs, final Instrumentation inst) {
            if (DependencyImpl.CURRENT_IMPL.isClassLoaderInitialized())
                throw new IllegalStateException("FoxClassLoader already initialized!");
            Agent.inst = inst;
        }

        static boolean supported() {
            return inst != null;
        }

        static void addToClassPath(final File library) {
            try {
                inst.appendToSystemClassLoaderSearch(new JarFile(library));
            } catch (final IOException e) {
                System.err.println("Failed to add jar to ClassPath");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    public static File loadDependencyAsFile(Dependency dependency) {
        return loadDependencyAsFile(dependency, null);
    }

    public static File loadDependencyAsFile(Dependency dependency, File alternative) {
        if (mcLibraries == null) {
            if (!DependencyImpl.CURRENT_IMPL.isClassLoaderInitialized()) {
                // We should never reach here...
                throw new IllegalStateException("FoxLoader DependencyHelper didn't initialized properly");
            }
            setMCLibraryRoot();
        }
        return loadDependencyImpl(dependency, false, true, false, alternative, false);
    }

    public static File getLocalSlimReIndevJarFile() {
        if (DependencyImpl.DEFAULT_IMPL != DependencyImpl.CURRENT_IMPL) {
            try {
                return loadDependencyAsFile(reIndevDependencySlim);
            } catch (RuntimeException e) {
                return null;
            }
        }
        // localFileDev is useful for local testing.
        File localFileDev = new File(JAR_FOLDER, BuildConfig.SLIM_JAR_NAME);
        if (checkHashOrDeleteEx(localFileDev, reIndevDependencySlim, false, true)) {
            return localFileDev;
        }
        return null;
    }

    public static void checkDependency(Dependency dependency) {
        loadDependencyImpl(dependency, false, false, false, null, true);
    }

    public static void loadDependencySelf(Dependency dependency) {
        if (DependencyImpl.CURRENT_IMPL.isClassLoaderInitialized())
            throw new IllegalStateException("FoxClassLoader already initialized!");
        if (DependencyHelper.class.getClassLoader().getResource(
                dependency.classCheck.replace('.', '/') + ".class") != null) {
            return; // Great news, we already have the library loaded!
        }
        File file = loadDependencyAsFile(dependency);
        if (file == null) {
            // If null it means it's already in class path.
            return;
        }
        if (Agent.supported()) {
            Agent.addToClassPath(file);
        } else {
            final ClassLoader loader = ClassLoader.getSystemClassLoader();
            if (!(loader instanceof URLClassLoader)) {
                throw new RuntimeException("System ClassLoader is not URLClassLoader");
            }
            try {
                final Method addURL = getURLClassLoaderAddMethod(loader);
                if (addURL == null) {
                    throw new RuntimeException("Unable to find method to add library jar to System ClassLoader");
                }
                addURL.setAccessible(true);
                addURL.invoke(loader, file.toURI().toURL());
            } catch (final ReflectiveOperationException | MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Method getURLClassLoaderAddMethod(final Object o) {
        Class<?> clazz = o.getClass();
        Method m = null;
        while (m == null) {
            try {
                m = clazz.getDeclaredMethod("addURL", URL.class);
            } catch (final NoSuchMethodException ignored) {
                clazz = clazz.getSuperclass();
                if (clazz == null) {
                    return null;
                }
            }
        }
        return m;
    }

    private static String fixUpPath(String path) {
        return File.separatorChar == '\\' ?
                path.replace('/', '\\') : path;
    }

    public static boolean hasClass(String cls) {
        return DependencyImpl.CURRENT_IMPL.hasClass(cls);
    }

    private static String resolvePostURL(String string) {
        String[] depKeys = string.split(":");
        // "org.joml:rrrr:${jomlVersion}"      => ${repo}/org/joml/rrrr/1.9.12/rrrr-1.9.12.jar
        // "org.joml:rrrr:${jomlVersion}:rrrr" => ${repo}/org/joml/rrrr/1.9.12/rrrr-1.9.12-rrrr.jar
        if (depKeys.length == 3) {
            return depKeys[0].replace('.','/')+"/"+depKeys[1]+"/"+depKeys[2]+"/"+depKeys[1]+"-"+depKeys[2]+".jar";
        }
        if (depKeys.length == 4) {
            return depKeys[0].replace('.','/')+"/"+depKeys[1]+"/"+depKeys[2]+"/"+depKeys[1]+"-"+depKeys[2]+"-"+depKeys[3]+".jar";
        }
        throw new RuntimeException("Invalid Dep");
    }

    public static class Dependency {
        public final String name, repository, classCheck, fallbackUrl, sha256Sum;
        public final int javaSupport;

        public Dependency(String name, String repository, String classCheck) {
            this(name, repository, classCheck, null, null, 8);
        }

        public Dependency(String name, String repository, String classCheck, String fallbackUrl) {
            this(name, repository, classCheck, fallbackUrl, null, 8);
        }

        public Dependency(String name, String repository, String classCheck, String fallbackUrl, String sha256Sum) {
            this(name, repository, classCheck, fallbackUrl, sha256Sum, 8);
        }

        public Dependency(String name, String repository, String classCheck, String fallbackUrl, String sha256Sum, int javaSupport) {
            this.name = name;
            this.repository = repository;
            this.classCheck = classCheck;
            this.fallbackUrl = fallbackUrl;
            this.sha256Sum = sha256Sum;
            this.javaSupport = javaSupport;

        }

        @Override
        public String toString() {
            return "Dependency{" +
                    "name='" + name + '\'' +
                    ", repository='" + repository + '\'' +
                    ", classCheck='" + classCheck + '\'' +
                    ", fallbackUrl='" + fallbackUrl + '\'' +
                    ", sha256Sum='" + sha256Sum + '\'' +
                    '}';
        }
    }

    public static abstract class DependencyImpl {
        private static final DependencyImpl DEFAULT_IMPL = new DependencyImpl() {};
        private static DependencyImpl CURRENT_IMPL = DEFAULT_IMPL;

        public static void install(DependencyImpl dependencyImpl) {
            Objects.requireNonNull(dependencyImpl, "dependencyImpl");
            if (CURRENT_IMPL == dependencyImpl) {
                return;
            }
            if (DEFAULT_IMPL != CURRENT_IMPL) {
                throw new IllegalStateException("Impl already has been replaced");
            }
            CURRENT_IMPL = dependencyImpl;
        }

        public boolean isDevelopingFoxLoader() {
            return false;
        }

        public boolean isDevelopingMod() {
            return false;
        }

        public boolean hasClass(String cls) {
            return false;
        }

        public void addDependency(File file, Dependency dependency,boolean isMinecraft) throws IOException {
            throw new UnsupportedOperationException("Not implemented");
        }

        public void checkAddDependency(File file, Dependency dependency) throws IOException {
            throw new UnsupportedOperationException("Not implemented");
        }

        public boolean isClassLoaderInitialized() {
            return false;
        }

        public boolean isDev() {
            return false;
        }

        public void printStackTrace(Throwable throwable) {
            throwable.printStackTrace(System.out);
        }

        public File checkMCLibraryRoot(File mcLibraries) {
            throw new UnsupportedOperationException("Not implemented");
        }

        public URL getClassResource(String classCheck) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
