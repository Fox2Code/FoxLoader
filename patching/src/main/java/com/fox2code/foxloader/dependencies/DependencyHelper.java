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
import java.net.*;
import java.nio.file.Files;
import java.util.*;
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

    public static final Dependency lwjglx = new Dependency(
            "com.fox2code:lwjglx:" + BuildConfig.LWJGLX_VERSION,
            FOX2CODE, "org.lwjgl.system.LWJGLXHelper", null, "d29d5a65988b001dd83d474cd1a1ecb659f353b2ebab5877454cd7d19c1a08bf");

    public static final Dependency jansi = new Dependency(
            "org.fusesource.jansi:jansi:" + BuildConfig.JANSI_VERSION, MAVEN_CENTRAL,
            "org.fusesource.jansi.AnsiConsole", null, "0b7b8b003a90ea491579b62f5118828e45112914c65589b00faa49d6ec785839");

    public static final Dependency annotations =
            new Dependency("org.jetbrains:annotations:24.1.0", MAVEN_CENTRAL,
                    "org.jetbrains.annotations.NotNull", null, "27a770dc7ce50500918bb8c3c0660c98290630ec796b5e3cf6b90f403b3033c6");

    public static final Dependency GSON_DEPENDENCY = // Used by installer.
            new Dependency("com.google.code.gson:gson:2.10.1", MAVEN_CENTRAL,
                    "com.google.gson.Gson", null, "4241c14a7727c34feea6507ec801318a3d4a90f070e4525681079fb94ee4c593");

    // Extra dependencies not included in ReIndev
    public static final Dependency jvmDowngraderCore =
            new Dependency("xyz.wagyourtail.jvmdowngrader:jvmdowngrader:" + BuildConfig.JVM_DOWNGRADER_VERSION,
                    MAVEN_CENTRAL, "xyz.wagyourtail.jvmdg.ClassDowngrader", null, "6f3543575eb970f7e8e0f5e87c16486e3a8db198d391ff525e67a0d15879b0b7");
    public static final Dependency jvmDowngraderJavaAPI =
            new Dependency("xyz.wagyourtail.jvmdowngrader:jvmdowngrader-java-api:" + BuildConfig.JVM_DOWNGRADER_VERSION,
                    MAVEN_CENTRAL, "xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_ClassLoader", null, "6bebc6fc4950c7121b5ff76ff1b792bec8dcb9b8d0bb949cb8bfddda59c1e51b");

    public static final Dependency[] commonDependencies = new Dependency[]{
            new Dependency("org.slf4j:slf4j-api:" + BuildConfig.SLF4J_VERSION, MAVEN_CENTRAL, "org.slf4j.Logger", null, "7b751d952061954d5abfed7181c1f645d336091b679891591d63329c622eb832"),
            new Dependency("org.ow2.asm:asm:" + BuildConfig.ASM_VERSION, MAVEN_CENTRAL, "org.objectweb.asm.ClassVisitor", null, "03d99a74ad1ee5c71334ef67437f4ef4fe3488caa7c96d8645abc73c8e2017d4"),
            new Dependency("org.ow2.asm:asm-tree:" + BuildConfig.ASM_VERSION, MAVEN_CENTRAL, "org.objectweb.asm.tree.ClassNode", null, "42178f3775c9c63f9e5e1446747d29b4eca4d91bd6e75e5c43cfa372a47d38c6"),
            new Dependency("org.ow2.asm:asm-analysis:" + BuildConfig.ASM_VERSION, MAVEN_CENTRAL, "org.objectweb.asm.tree.analysis.Analyzer", null , "6a15d28e8bd29ba4fd5bca4baf9b50e8fba2d7b51fbf78cfa0c875a7214c678b"),
            new Dependency("org.ow2.asm:asm-commons:" + BuildConfig.ASM_VERSION, MAVEN_CENTRAL, "org.objectweb.asm.commons.InstructionAdapter", null, "db2f6f26150bbe7c126606b4a1151836bcc22a1e05a423b3585698bece995ff8"),
            new Dependency("org.ow2.asm:asm-util:" + BuildConfig.ASM_VERSION, MAVEN_CENTRAL, "org.objectweb.asm.util.CheckClassAdapter", null, "3842e13cfe324ee9ab7cdc4914be9943541ead397c17e26daf0b8a755bede717"),
            GSON_DEPENDENCY, new Dependency("com.google.guava:guava:21.0", MAVEN_CENTRAL, "com.google.common.io.Files", null, "972139718abc8a4893fa78cba8cf7b2c903f35c97aaf44fa3031b0669948b480"),
            new Dependency("it.unimi.dsi:fastutil-core:" + BuildConfig.FASTUTIL_VERSION, MAVEN_CENTRAL, "it.unimi.dsi.fastutil.Pair", null, "edae1dc6de09e46ab7646616125cc0575a6810f17ed6298bfc6e8dc85f9c4258"),
            new Dependency("com.unascribed:ears-api:" + BuildConfig.EARS_VERSION, UNASCRIBED, "com.unascribed.ears.api.features.EarsFeatures", null, "6482fe4f9473c1b49e0513684e95647a95dca289528b5fdcad3347c193500871"),
            new Dependency("com.unascribed:ears-common:" + BuildConfig.EARS_VERSION, UNASCRIBED, "com.unascribed.ears.common.EarsCommon", null, "8ec4fb89e30901abf1f9d35c5ed5ac4d0d57d212dc4f4260566b33fbc826ca0d"),
            new Dependency("net.fabricmc:sponge-mixin:" + BuildConfig.FABRIC_MIXIN_VERSION, FABRIC_MC,
                    "org.spongepowered.asm.mixin.Mixins", null, "bf65996f6edd93ba4061a175e2c814205c1ba694357a1e5a336c7ba5078ca376"),
            new Dependency("io.github.llamalad7:mixinextras-common:" + BuildConfig.MIXIN_EXTRAS_VERSION, MAVEN_CENTRAL,
                    "com.llamalad7.mixinextras.MixinExtrasBootstrap", null, "42c6bdf93c12cbf90f5451988dfed763ab8489ce2e40a1d843518b309ff0abaa"),
            new Dependency("com.github.bawnorton.mixinsquared:mixinsquared-common:" + BuildConfig.MIXIN_SQUARED_VERSION,
                    FOX2CODE, "com.bawnorton.mixinsquared.MixinSquaredBootstrap", null, "5ae421a724f2cc9b06ade3da79fb3224e8073fdeac9a35df6f16ae59147a1abb"),
            new Dependency("com.fox2code:ReBuild:" + BuildConfig.REBUILD_VERSION, FOX2CODE,
                    "com.fox2code.rebuild.ClassDataProvider", null, "6bb4ac7ae84ec752e505c4dc885c003e2550baf4c327a5c9f9c44379a756e43d"),
            new Dependency("com.fox2code.FoxEvents:core:" + BuildConfig.FOX_EVENTS_VERSION, FOX2CODE,
                    "com.fox2code.foxevents.FoxEvents", null, "316fe03dae63f306e8462db752732b1e3359f23f8177fbc4fb02dbabf0271e01"),
            new Dependency("com.fox2code:FoxFlexVer:" + BuildConfig.FOX_FLEX_VER_VERSION, FOX2CODE,
                    "com.fox2code.flexver.FlexVer", null, "4cf356d6c05c1a7008d90500945df21e4bac32e3a09309efc012a5a373431c0b"),
            jvmDowngraderCore, jvmDowngraderJavaAPI, // jvmDowngrader has special handling in dev plugin
    };

    public static final Dependency[] lwjgl2Dependencies = new Dependency[]{
            new Dependency("org.lwjgl.lwjgl:lwjgl:2.9.3", MAVEN_CENTRAL, "org.lwjgl.LWJGLUtil",
                    null, "527d509f60132e5b2653c7fc0f8cf299d6f698f4a8013342bef47705dc57ed3f"),
            new Dependency("org.lwjgl.lwjgl:lwjgl_util:2.9.3", MAVEN_CENTRAL, "org.lwjgl.util.Color",
                    null, "6f4a261384e1688a3359420efce5275c8693b822a8c6eaa1e20d5cc126a51571"),
    };

    public static final Dependency[] commonDependenciesModernJava = new Dependency[]{
            new Dependency("blue.endless:jankson:" + BuildConfig.JANKSON_VERSION, SLEEPING_TOWN,
                    "blue.endless.jankson.api.Jankson", null,
                    "9e22cfd3d4eb59da6026f6d8310b498dc1775d86c7f92b8301f7aa30ac7d2e51", 21),
            new Dependency("com.moulberry:mixinconstraints:" + BuildConfig.MIXIN_CONSTRAINTS_VERSION,
                    MAVEN_CENTRAL, "com.moulberry.mixinconstraints.MixinConstraints", null,
                    "67e1fca9cb518f8afb356105c7816587e02880c2acca3bbdb6e08c8e967fc1be", 17),
    };

    public static final Dependency sparkDependency =
            new Dependency(BuildConfig.SPARK_DEPENDENCY, MODRINTH, "me.lucko.spark.common.SparkPlugin",
                    null, "6944504c201f20845713f710fbaed48b29bd6f4176b94ac047fee548719c2637");

    public static final Dependency vineFlower = new Dependency(
            BuildConfig.VINEFLOWER_DEPENDENCY, MAVEN_CENTRAL,
            "org.jetbrains.java.decompiler.main.Fernflower", null,
            "a615d07ddbbcd489369674f40e42df639c32be95410890b38f173d5c1e2ea39c", 17);

    public static final Dependency reIndevDependencySlim =
            new Dependency("net.silveros:reindev-slim:" + BuildConfig.REINDEV_VERSION,
                    BuildConfig.SLIM_URL, "net.minecraft.server.MinecraftServer",
                    null, BuildConfig.SLIM_SHA256_SUM);

    // Dependencies bundles
    public static final Set<String> availableDependencyBundles =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("kotlin", "jna")));

    public static final Dependency[] kotlinDependencyBundle = new Dependency[]{
            new Dependency("org.jetbrains.kotlin:kotlin-reflect:" + BuildConfig.KOTLIN_VERSION,
                    MAVEN_CENTRAL, "kotlin.reflect.full.KClasses", null, "8209083a4a7c4e476d9842b078308fa96a96f07a6bd99f05f3586ee13289ab26"),
            new Dependency("org.jetbrains.kotlin:kotlin-stdlib:" + BuildConfig.KOTLIN_VERSION,
                    MAVEN_CENTRAL, "kotlin.KotlinVersion", null, "8836ccffd3585fadda9901244b20d42901d2f3cd581058d8434e2ffabcf3a3e7"),
            new Dependency("org.jetbrains.kotlinx:atomicfu-jvm:" + BuildConfig.KOTLINX_ATOMICFU_VERSION,
                    MAVEN_CENTRAL, "kotlinx.atomicfu.AtomicRef", null, "97cd462f8e8ab92571ab8805070e4450c52cb1ad63c224208f71a10943a01e46"),
            new Dependency("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:" + BuildConfig.KOTLINX_COLLECTIONS_IMMUTABLE_VERSION,
                    MAVEN_CENTRAL, "kotlinx.collections.immutable.ImmutableCollection", null, "d767014ad0c9a27d27d26fd38e7afa030aee0d141338f108781ff02ecf2fdab5"),
            new Dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:" + BuildConfig.KOTLINX_COROUTINES_VERSION,
                    MAVEN_CENTRAL, "kotlinx.coroutines.CoroutineDispatcher", null, "5ca175b38df331fd64155b35cd8cae1251fa9ee369709b36d42e0a288ccce3fd"),
            new Dependency("org.jetbrains.kotlinx:kotlinx-datetime-jvm:" + BuildConfig.KOTLINX_DATETIME_VERSION,
                    MAVEN_CENTRAL, "kotlinx.datetime.DateTimeUnit", null, "3b8b98657c9aff3be7f6b4b575e9f3fb9a3a1f452a801da0f438901734acdade"),
            new Dependency("org.jetbrains.kotlinx:kotlinx-io-bytestring-jvm:" + BuildConfig.KOTLINX_IO_VERSION,
                    MAVEN_CENTRAL, "kotlinx.io.bytestring.ByteString", null, "3da805e9da2ff3cb119f744dcd11de6a18e32a5b933518f17418ba5795cfa775"),
            new Dependency("org.jetbrains.kotlinx:kotlinx-io-core-jvm:" + BuildConfig.KOTLINX_IO_VERSION,
                    MAVEN_CENTRAL, "kotlinx.io.Buffer", null, "610770cc855edf1c57d9fcd7c69fd040af57adaf8834505f91805de680783d13"),
            new Dependency("org.jetbrains.kotlinx:kotlinx-metadata-jvm:" + BuildConfig.KOTLINX_METADATA_VERSION,
                    MAVEN_CENTRAL, "kotlinx.metadata.KmClass", null, "d42f4bac60b81c4fdcef1c666aed4181d00401e5e696e496f44935fe32fea58f"),
            new Dependency("org.jetbrains.kotlinx:kotlinx-serialization-cbor-jvm:" + BuildConfig.KOTLINX_SERIALIZATION_VERSION,
                    MAVEN_CENTRAL, "kotlinx.serialization.cbor.Cbor", null, "e45f32f0d80e1164593cfcf9f1907ae39871084c6aa08e49acc2e8e22e1d93ce"),
            new Dependency("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:" + BuildConfig.KOTLINX_SERIALIZATION_VERSION,
                    MAVEN_CENTRAL, "kotlinx.serialization.KSerializer", null, "1f0afa172110e45a7231ef1b44ae8fd84c1ebaff96f3fc3ad68ef8c48120b59c"),
            new Dependency("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:" + BuildConfig.KOTLINX_SERIALIZATION_VERSION,
                    MAVEN_CENTRAL, "kotlinx.serialization.json.Json", null, "d94cc34cae39246a1af74fda63f9c4812ce12216ef641d5fa3bbbb539a6922d8"),
    };

    public static final Dependency[] jnaDependencyBundle = new Dependency[]{
            new Dependency("net.java.dev.jna:jna:" + BuildConfig.JNA_VERSION,
                    MAVEN_CENTRAL, "com.sun.jna.Native", null, "260c4b1e22b1db9e110ee441c4f13ce115f841fa48c41d78750986214b395557"),
    };

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
        if ((DependencyImpl.CURRENT_IMPL.hasClass("org.lwjgl.opengl.GL11") &&
                !DependencyImpl.CURRENT_IMPL.hasClass("org.lwjgl.opengl.Display")) ||
                DependencyImpl.CURRENT_IMPL.hasClass("org.lwjgl.system.LWJGLXHelper")) {
            // If we are on LWJGL3 so let's use LWJGL3
            loadDependency(lwjglx);
        } else {
            for (Dependency dependency : lwjgl2Dependencies) {
                loadDependencySafe(dependency);
            }
        }
        if (!(DependencyImpl.CURRENT_IMPL.isDevelopingFoxLoader() ||
                DependencyImpl.CURRENT_IMPL.isDevelopingMod())) {
            loadDependencyImpl(reIndevDependencySlim, true, false, true, null, false);
        }
        checkDependency(annotations);
        if (DependencyImpl.CURRENT_IMPL.isDevelopingFoxLoader()) {
            loadDependencyAsFile(vineFlower);
            loadDependencyAsFile(lwjglx);
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
                    realSha256 = IOUtils.toHex(IOUtils.sha256Of(file));
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
        Throwable cause = null;
        try {
            hashString = IOUtils.toHex(IOUtils.sha256Of(file));
        } catch (IOException e) {
            hashString = "";
            cause = e;
        }
        if (!dependency.sha256Sum.equals(hashString)) {
            boolean deleteSuccessful = keepFile || file.delete();
            if (!deleteSuccessful) {
                file.deleteOnExit();
            }
            if (errorOut) {
                throw new RuntimeException("Remote dependency " + dependency.name + " checksum mismatch " +
                        "(got: " + hashString + ", expected: " + dependency.sha256Sum + ")", cause);
            }
            if (!deleteSuccessful) {
                throw new RuntimeException("Can't delete dependency with checksum mismatch " + dependency.name, cause);
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
                DependencyImpl.CURRENT_IMPL.printStackTrace(e);
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

    public static Dependency[] getDependencyBundle(String bundleName) {
        switch (bundleName) {
            case "kotlin":
                return kotlinDependencyBundle;
            case "jna":
                return jnaDependencyBundle;
        }
        throw new NoSuchElementException(bundleName);
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

        public String getNameForVersion(String newVersion) {
            String[] depKeys = newVersion.split(":");
            if (newVersion.equals(depKeys[2])) {
                return this.name;
            }
            return this.getNameForVersionImpl(depKeys, newVersion);
        }

        public Dependency getDependencyForVersion(String newVersion) {
            String[] depKeys = newVersion.split(":");
            if (newVersion.equals(depKeys[2])) {
                return this;
            }
            return new Dependency(this.getNameForVersionImpl(depKeys, newVersion),
                    this.repository, this.classCheck, this.fallbackUrl, null, this.javaSupport);
        }

        private String getNameForVersionImpl(String[] depKeys, String newVersion) {
            depKeys[2] = newVersion;
            StringJoiner stringJoiner = new StringJoiner(":");
            for (String token : depKeys) {
                stringJoiner.add(token);
            }
            return stringJoiner.toString();
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
