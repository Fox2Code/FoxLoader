package com.fox2code.foxloader.launcher;

import com.fox2code.foxloader.launcher.utils.Platform;
import com.fox2code.foxloader.launcher.utils.SourceUtil;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

public class FoxLauncher {
    static {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        System.setProperty("user.language", "en");
        if (Platform.getJvmVersion() < 17) {
            try {
                System.setSecurityManager(null);
            } catch (Throwable ignored) {}
        }

        // Preload some classes to improve reliability.
        String[] classToPreload = new String[]{
                "java.io.File", "sun.nio.ch.FileChannelImpl"};
        for (String cls : classToPreload) {
            try {
                Class.forName(cls);
            } catch (ClassNotFoundException ignored) {}
        }
    }

    public static final File foxLoaderFile = SourceUtil.getSourceFile(FoxLauncher.class);
    public static final HashMap<String, Object> mixinProperties = new HashMap<>();
    static LauncherType launcherType = LauncherType.UNKNOWN;
    private static boolean client;
    static FoxClassLoader foxClassLoader;
    static File gameDir;
    public static String initialUsername;
    public static String initialSessionId;

    static void initForClientFromArgs(String[] args) {
        if (foxClassLoader != null)
            throw new IllegalStateException("FoxClassLoader already initialized!");
        client = true;
        File gameDir = null;
        if (args.length < 2) {
            initialUsername = // Allow username defines
                    args.length < 1 ? "Player" : args[0];
            initialSessionId = "-";
        } else {
            initialUsername = args[0];
            initialSessionId = args[1];
            for (int i = 2; i < args.length; i++) {
                if (args[i].equals("--gameDir")) {
                    gameDir = new File(args[i + 1]).getAbsoluteFile();
                    break;
                }
            }
        }
        if (gameDir == null) {
            gameDir = new File("").getAbsoluteFile();
            // Special case for development environment.
            if (isDirGradle(gameDir)) {
                (gameDir = new File(gameDir, "run")).mkdirs();
            }
        }
        System.setProperty("user.dir", gameDir.getPath());
        FoxLauncher.gameDir = gameDir;
        if (LoggerHelper.devEnvironment) {
            launcherType = LauncherType.GRADLE;
        }
        foxClassLoader = new FoxClassLoader();
        foxClassLoader.addTransformerExclusion("org.lwjgl.");
        foxClassLoader.addTransformerExclusion("org.objectweb.asm.");
        foxClassLoader.addTransformerExclusion("org.spongepowered.asm.");
        foxClassLoader.addTransformerExclusion("org.spongepowered.tools.");
        foxClassLoader.addTransformerExclusion("com.llamalad7.mixinextras.");
        foxClassLoader.addTransformerExclusion("com.fox2code.foxloader.loader.");
        DependencyHelper.loadDependencies(true);
        installLoggerHelper();
    }

    static void initForServerFromArgs(String[] args) {
        if (foxClassLoader != null)
            throw new IllegalStateException("FoxClassLoader already initialized!");
        client = false;
        FoxLauncher.gameDir = new File("").getAbsoluteFile();
        // Special case for development environment.
        if (isDirGradle(gameDir)) {
            throw new RuntimeException("You should not run a server inside a gradle project!");
        }
        if (LoggerHelper.devEnvironment) {
            launcherType = LauncherType.GRADLE;
        }
        foxClassLoader = new FoxClassLoader();
        foxClassLoader.addTransformerExclusion("org.objectweb.asm.");
        foxClassLoader.addTransformerExclusion("org.spongepowered.asm.");
        foxClassLoader.addTransformerExclusion("org.spongepowered.tools.");
        foxClassLoader.addTransformerExclusion("com.llamalad7.mixinextras.");
        foxClassLoader.addTransformerExclusion("com.fox2code.foxloader.loader.");
        DependencyHelper.loadDependencies(false);
        installLoggerHelper();
    }

    private static void installLoggerHelper() {
        boolean installed = false;
        try {
            File logFile = new File(gameDir, (LoggerHelper.devEnvironment ?
                    (client ? "client-latest.log" : "server-latest.log") : "latest.log"));
            installed = LoggerHelper.install(logFile);
        } catch (Throwable ignored) {}
        if (!installed) {
            System.out.println("Failed to install log helper!");
        }
    }

    public static void setEarlyMinecraftURL(URL url) {
        if (foxClassLoader != null) {
            throw new IllegalStateException(
                    "Class loader has already been initialized!");
        }
        FoxClassLoader.earlyMinecraftURL = url;
    }

    private static boolean isDirGradle(File file) {
        return new File(gameDir, "gradle").exists() && (
                new File(gameDir, "build.gradle.kts").exists() ||
                        new File(gameDir, "build.gradle").exists());
    }

    static void runClientWithArgs(String[] args) throws ReflectiveOperationException {
        Thread.currentThread().setContextClassLoader(foxClassLoader);
        Class.forName("com.fox2code.foxloader.loader.ClientModLoader", true, foxClassLoader)
                .getDeclaredMethod("launchModdedClient", String[].class).invoke(null, (Object) args);
    }

    static void runServerWithArgs(String[] args) throws ReflectiveOperationException {
        Thread.currentThread().setContextClassLoader(foxClassLoader);
        Class.forName("com.fox2code.foxloader.loader.ServerModLoader", true, foxClassLoader)
                .getDeclaredMethod("launchModdedServer", String[].class).invoke(null, (Object) args);
    }

    public static File getGameDir() {
        return gameDir;
    }

    public static FoxClassLoader getFoxClassLoader() {
        return foxClassLoader;
    }

    public static boolean isClient() {
        return client;
    }

    public static LauncherType getLauncherType() {
        return launcherType;
    }
}
