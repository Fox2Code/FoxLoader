package com.fox2code.foxloader.launcher;

import com.fox2code.foxloader.launcher.utils.Platform;

import java.io.*;
import java.net.*;
import java.nio.file.Files;

public class DependencyHelper {
    public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";
    public static final String SPONGE_POWERED = "https://repo.spongepowered.org/maven";
    public static final String JITPACK = "https://jitpack.io";
    public static final String MODRINTH = "https://api.modrinth.com/maven";
    private static final String GRADLE_USER_AGENT;

    static {
        String javaVendor = System.getProperty("java.vendor");
        String javaVersion = System.getProperty("java.version");
        String javaVendorVersion = System.getProperty("java.vm.version");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        GRADLE_USER_AGENT = String.format("Gradle/7.5.1 (%s;%s;%s) (%s;%s;%s)",
                osName, osVersion, osArch, javaVendor, javaVersion, javaVendorVersion);
    }

    // Extra dependencies not included in ReIndev
    public static final Dependency[] commonDependencies = new Dependency[]{
            new Dependency("org.ow2.asm:asm:9.4", MAVEN_CENTRAL, "org.objectweb.asm.ClassVisitor"),
            new Dependency("org.ow2.asm:asm-tree:9.4", MAVEN_CENTRAL, "org.objectweb.asm.tree.ClassNode"),
            new Dependency("org.ow2.asm:asm-analysis:9.4", MAVEN_CENTRAL, "org.objectweb.asm.tree.analysis.Analyzer"),
            new Dependency("org.ow2.asm:asm-commons:9.4", MAVEN_CENTRAL, "org.objectweb.asm.commons.InstructionAdapter"),
            new Dependency("org.ow2.asm:asm-util:9.4", MAVEN_CENTRAL, "org.objectweb.asm.util.CheckClassAdapter"),
            new Dependency("com.google.code.gson:gson:2.2.4", MAVEN_CENTRAL, "com.google.gson.Gson"),
            new Dependency("com.google.guava:guava:21.0", MAVEN_CENTRAL, "com.google.common.io.Files"),
            new Dependency("org.apache.commons:commons-lang3:3.3.2", MAVEN_CENTRAL, "org.apache.commons.lang3.tuple.Pair"),
            new Dependency("org.spongepowered:mixin:0.8.5", SPONGE_POWERED, "org.spongepowered.asm.mixin.Mixins"),
            new Dependency("com.github.LlamaLad7:MixinExtras:0.2.0-beta.4", JITPACK, "com.llamalad7.mixinextras.MixinExtrasBootstrap"),
    };

    public static final Dependency sparkDependency =
            new Dependency(BuildConfig.SPARK_DEPENDENCY, MODRINTH, "me.lucko.spark.common.SparkPlugin");

    public static final Dependency[] clientDependencies = new Dependency[]{
            new Dependency("net.silveros:reindev:" + BuildConfig.REINDEV_VERSION,
                    BuildConfig.CLIENT_URL, "net.minecraft.client.Minecraft")
    };
    public static final Dependency[] serverDependencies = new Dependency[]{
            new Dependency("net.silveros:reindev-server:" + BuildConfig.REINDEV_VERSION,
                    BuildConfig.SERVER_URL, "net.minecraft.server.MinecraftServer")
    };

    private static File mcLibraries;

    static void loadDependencies(boolean client) {
        String mcLibrariesPath;
        switch (Platform.getPlatform()) {
            case WINDOWS:
                mcLibrariesPath = System.getenv("APPDATA") + "\\.minecraft\\";
                break;
            case MACOS:
                mcLibrariesPath = System.getProperty("user.home") + "/Library/Application Support/minecraft/";
                break;
            case LINUX:
                mcLibrariesPath = System.getProperty("user.home") + "/.minecraft/";
                break;
            default:
                throw new RuntimeException("Unsupported operating system");
        }
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
                            mcLibraries = new File(lwjglPath.substring(0, i));
                        }
                    }
                } catch (IOException | URISyntaxException ignored) {}
            }
            if (mcLibraries == null) {
                mcLibraries = new File(mcLibrariesPath + "libraries");
            }
        } else {
            mcLibraries = new File("libraries").getAbsoluteFile();
        }
        for (Dependency dependency : commonDependencies) {
            loadDependency(dependency);
        }
        for (Dependency dependency : (client ? clientDependencies : serverDependencies)) {
            loadDependency(dependency, true, false);
        }
    }

    public static void loadDevDependencies(File cacheRoot, boolean client) {
        if (FoxLauncher.getFoxClassLoader() != null) return; // Why???
        mcLibraries = cacheRoot;
        for (Dependency dependency : (client ? clientDependencies : serverDependencies)) {
            loadDependency(dependency, true, true);
        }
    }

    public static boolean loadDependencySafe(Dependency dependency) {
        try {
            loadDependency(dependency, false, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void loadDependency(Dependency dependency) {
        loadDependency(dependency, false, false);
    }

    private static void loadDependency(Dependency dependency, boolean minecraft, boolean dev) {
        if (!dev && hasClass(dependency.classCheck)) return;
        String postURL = resolvePostURL(dependency.name);
        File file = new File(mcLibraries, fixUpPath(postURL));
        boolean justDownloaded = false;
        if (!file.exists()) {
            File parentFile = file.getParentFile();
            if (!parentFile.isDirectory() && !parentFile.mkdirs()) {
                throw new RuntimeException("Cannot create dependency directory for " + dependency.name);
            }
            try (OutputStream os = Files.newOutputStream(file.toPath())) {
                justDownloaded = true;
                downloadTo(new URL(dependency.repository.endsWith(".jar") ?
                        dependency.repository : dependency.repository + "/" + postURL), os);
            } catch (IOException ioe) {
                if (file.exists() && !file.delete()) file.deleteOnExit();
                throw new RuntimeException("Cannot download " + dependency.name, ioe);
            }
        }
        if (dev) return; // We don't have a FoxClass loader in dev environment.
        try {
            if (minecraft) {
                FoxLauncher.getFoxClassLoader().setMinecraftURL(file.toURI().toURL());
            } else {
                FoxLauncher.getFoxClassLoader().addURL(file.toURI().toURL());
            }
            if (hasClass(dependency.classCheck)) {
                System.out.println("Loaded " +
                        dependency.name + " -> " + file.getPath());
            } else {
                if (!justDownloaded) {
                    // Assume file is corrupted if load failed.
                    if (file.exists() && !file.delete()) file.deleteOnExit();
                    loadDependency(dependency);
                    return;
                }
                throw new RuntimeException("Failed to load " +
                        dependency.name + " -> " + file.getPath());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String fixUpPath(String path) {
        return Platform.getPlatform() == Platform.WINDOWS ?
                path.replace('/', '\\') : path;
    }

    public static boolean hasClass(String cls) {
        return FoxLauncher.getFoxClassLoader().getResource(cls.replace('.', '/') + ".class") != null;
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

    private static void downloadTo(URL url, OutputStream outputStream) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        con.setConnectTimeout(5000);
        con.setRequestProperty("Connection", "keep-alive");
        con.setRequestProperty("User-Agent", GRADLE_USER_AGENT);
        try (InputStream is = con.getInputStream()) {
            byte[] byteChunk = new byte[4096];
            int n;

            while ((n = is.read(byteChunk)) > 0) {
                outputStream.write(byteChunk, 0, n);
            }

            outputStream.flush();
        }
    }

    public static class Dependency {
        public final String name, repository, classCheck;

        public Dependency(String name, String repository, String classCheck) {
            this.name = name;
            this.repository = repository;
            this.classCheck = classCheck;
        }
    }
}
