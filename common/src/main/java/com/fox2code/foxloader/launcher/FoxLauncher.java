package com.fox2code.foxloader.launcher;

import com.fox2code.foxloader.launcher.utils.Platform;
import com.fox2code.foxloader.launcher.utils.SourceUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Logger;

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
    private static boolean client, wronglyInstalled, wronglyInstalledUnrecoverable;
    static FoxClassLoader foxClassLoader;
    static File gameDir;
    public static String initialUsername;
    public static String initialSessionId;
    private static boolean hasLogger = false;

    public static void markWronglyInstalled() {
        if (foxClassLoader == null) wronglyInstalled = true;
    }

    public static void markWronglyInstalledUnrecoverable() {
        if (foxClassLoader == null) {
            wronglyInstalledUnrecoverable = true;
            initForClientFromArgs(new String[0]);
            wronglyInstalled = true;
        }
    }

    public static boolean isWronglyInstalled() {
        return (client && wronglyInstalled) ||
                wronglyInstalledUnrecoverable;
    }

    static void initForClientFromArgs(String[] args) {
        if (foxClassLoader != null)
            throw new IllegalStateException("FoxClassLoader already initialized!");
        if (wronglyInstalled && wronglyInstalledUnrecoverable)
            throw new IllegalStateException("FoxClassLoader cannot initialize!");
        client = true;
        File gameDir = null;
        if (args.length < 2) {
            initialUsername = // Allow username defines
                    args.length == 0 ? "Player" : args[0];
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
        if (wronglyInstalledUnrecoverable) return;
        foxClassLoader = new FoxClassLoader();
        foxClassLoader.addTransformerExclusion("org.lwjgl.");
        foxClassLoader.addTransformerExclusion("org.objectweb.asm.");
        foxClassLoader.addTransformerExclusion("org.spongepowered.asm.");
        foxClassLoader.addTransformerExclusion("org.spongepowered.tools.");
        foxClassLoader.addTransformerExclusion("com.llamalad7.mixinextras.");
        foxClassLoader.addTransformerExclusion("com.bawnorton.mixinsquared.");
        foxClassLoader.addTransformerExclusion("com.fox2code.foxloader.loader.");
        installLoggerHelper(true); // Install special logger before libraries loading
        DependencyHelper.loadDependencies(true);
    }

    static void initForServerFromArgs(String[] args) {
        if (foxClassLoader != null)
            throw new IllegalStateException("FoxClassLoader already initialized!");
        if (wronglyInstalled && wronglyInstalledUnrecoverable)
            throw new IllegalStateException("FoxClassLoader cannot initialize!");
        client = false;
        File parent = null;
        // When double-clicked on, the jar may be launched in the
        // user home directory instead of where the file is located.
        if (Objects.equals(System.getProperty("user.dir"), System.getProperty("user.home"))) {
            System.setProperty("user.dir", (parent = SourceUtil.getSourceFile(FoxLauncher.class)
                    .getAbsoluteFile().getParentFile()).getAbsolutePath());
        }
        FoxLauncher.gameDir = new File("").getAbsoluteFile();
        if (parent != null && !parent.getPath().equals(FoxLauncher.gameDir.getPath())) {
            String message = "FoxLoader was unable to recover an invalid initial\n" +
                    "state caused by your desktop environment due to the current\n" +
                    "JVM not allowing fixing up the current JVM state.\n\n" +
                    "Please launch the server via your terminal or PowerShell";
            System.out.println("-----\n" + message + "\n-----");
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, message,
                        "FoxLoader server launch failure",
                        JOptionPane.ERROR_MESSAGE);
            }
            System.exit(-1);
            return;
        }
        // Special case for development environment.
        if (isDirGradle(gameDir)) {
            throw new RuntimeException("You should not run a server inside a gradle project!");
        }
        if (LoggerHelper.devEnvironment) {
            launcherType = LauncherType.GRADLE;
        }
        if (wronglyInstalledUnrecoverable) return;
        foxClassLoader = new FoxClassLoader();
        foxClassLoader.addTransformerExclusion("org.objectweb.asm.");
        foxClassLoader.addTransformerExclusion("org.spongepowered.asm.");
        foxClassLoader.addTransformerExclusion("org.spongepowered.tools.");
        foxClassLoader.addTransformerExclusion("com.llamalad7.mixinextras.");
        foxClassLoader.addTransformerExclusion("com.bawnorton.mixinsquared.");
        foxClassLoader.addTransformerExclusion("com.fox2code.foxloader.loader.");
        installLoggerHelper(false); // Install special logger before libraries loading
        DependencyHelper.loadDependencies(false);
    }

    private static void installLoggerHelper(boolean client) {
        if (hasLogger) return;
        boolean installed = false;
        try {
            File logFile = new File(gameDir, (LoggerHelper.devEnvironment ?
                    (client ? "client-latest.log" : "server-latest.log") : "latest.log"));
            installed = LoggerHelper.install(logFile);
        } catch (Throwable ignored) {}
        if (!installed) {
            System.out.println("Failed to install log helper!");
        }
        hasLogger = installed;
    }

    public static void installLoggerHelperOn(Logger logger) {
        if (hasLogger) LoggerHelper.installOn(logger);
    }

    public static void setEarlyMinecraftURL(URL url) {
        if (foxClassLoader != null) {
            throw new IllegalStateException(
                    "Class loader has already been initialized!");
        }
        FoxClassLoader.earlyMinecraftURL = url;
    }

    private static boolean isDirGradle(File file) {
        return new File(file, "gradle").exists() && (
                new File(file, "build.gradle.kts").exists() ||
                        new File(file, "build.gradle").exists());
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
