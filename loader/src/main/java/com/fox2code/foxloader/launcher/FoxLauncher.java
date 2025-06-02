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

import com.fox2code.foxloader.dependencies.DependencyHelper;
import com.fox2code.foxloader.utils.Platform;
import com.fox2code.foxloader.utils.SourceUtil;
import com.fox2code.foxloader.utils.io.CertificateHelper;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public final class FoxLauncher {
    static {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "false");
        System.setProperty("user.language", "en");
        String handlers = System.getProperty("java.protocol.handler.pkgs");
        if (handlers == null) handlers = "";
        System.setProperty("java.protocol.handler.pkgs",
                "com.fox2code.foxloader.launcher.protocols|" + handlers);
        try {
            new URL("fl:test");
        } catch (MalformedURLException e) {
            URL.setURLStreamHandlerFactory(
                    FoxClassLoader.FoxLoaderURLStreamHandler.INSTANCE);
            markWronglyInstalled();
        }
        Arrays.sort(new Object[]{0, 1, 2});
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
        // Some Linux-Tkg versions string can be unbearably long
        // Let's shorten it a bit for F3 menu in that case
        String osVersion = System.getProperty("os.version");
        if (osVersion.endsWith("-generic_v3")) {
            System.setProperty("os.version",
                    osVersion.substring(0, osVersion.length() - 11));
        }
        DependencyHelper.DependencyImpl.install(GameDependencyImpl.INSTANCE);
    }

    public static final File foxLoaderFile = SourceUtil.getSourceFile(FoxLauncher.class);
    public static final boolean DEVELOPING_FOXLOADER = FoxLauncher.foxLoaderFile.isDirectory();
    public static final boolean DEV_MODE = Boolean.getBoolean("foxloader.dev-mode");
    public static final HashMap<String, Object> mixinProperties = new HashMap<>();
    static LauncherType launcherType = LauncherType.UNKNOWN;
    private static EnvironmentType environmentType;
    private static boolean wronglyInstalled, wronglyInstalledUnrecoverable;
    static final ArrayList<File> filesToLoad = new ArrayList<>();
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
        return wronglyInstalled || wronglyInstalledUnrecoverable;
    }

    static void initForClientFromArgs(String[] args) {
        if (foxClassLoader != null)
            throw new IllegalStateException("FoxClassLoader already initialized!");
        if (wronglyInstalledUnrecoverable)
            throw new IllegalStateException("FoxClassLoader cannot initialize!");
        environmentType = EnvironmentType.CLIENT;
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
        initializeClassLoaderCommon(); // Initialize class loader
        CertificateHelper.initializeSafe(); // Initialize custom root CA for SSL/Https
        installLoggerHelper(true); // Install special logger before libraries loading
        DependencyHelper.loadCoreDependencies(true);
        initializeClassPath();
    }

    static void initForServer() {
        if (foxClassLoader != null)
            throw new IllegalStateException("FoxClassLoader already initialized!");
        if (wronglyInstalledUnrecoverable)
            throw new IllegalStateException("FoxClassLoader cannot initialize!");
        environmentType = EnvironmentType.SERVER;
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
        initializeClassLoaderCommon(); // Initialize class loader
        CertificateHelper.initializeSafe(); // Initialize custom root CA for SSL/Https
        installLoggerHelper(false); // Install special logger before libraries loading
        DependencyHelper.loadCoreDependencies(false);
        initializeClassPath();
    }

    private static void initializeClassLoaderCommon() {
        if (Platform.getJvmVersion() > 9) {
            foxClassLoader = new FoxClassLoader("FoxLoader");
        } else {
            foxClassLoader = new FoxClassLoader();
        }
        foxClassLoader.addTransformerExclusion("org.objectweb.asm.");
        foxClassLoader.addTransformerExclusion("org.spongepowered.asm.");
        foxClassLoader.addTransformerExclusion("org.spongepowered.tools.");
        foxClassLoader.addTransformerExclusion("com.llamalad7.mixinextras.");
        foxClassLoader.addTransformerExclusion("com.bawnorton.mixinsquared.");
        foxClassLoader.addTransformerExclusion("com.moulberry.mixinconstraints.");
        foxClassLoader.addTransformerExclusion("com.fox2code.foxevents.");
        foxClassLoader.addTransformerExclusion("com.fox2code.foxloader.loader.");
        foxClassLoader.addTransformerExclusion("com.fox2code.foxloader.patching.");
        foxClassLoader.addTransformerExclusion("com.fox2code.foxloader.network.");
        foxClassLoader.addTransformerExclusion("xyz.wagyourtail.jvmdg.");
        Thread.currentThread().setContextClassLoader(foxClassLoader);
    }

    private static void initializeClassPath() {
        HashSet<String> blackListedJars = new HashSet<>(Arrays.asList(
                BuildConfig.DEV_PATCHED_JAR_NAME,
                BuildConfig.DEV_PATCHED_JAR_NAME.replace(".jar", "-1.jar"),
                BuildConfig.DEV_PATCHED_JAR_NAME.replace(".jar", "-2.jar"),
                "reindev-" + BuildConfig.REINDEV_VERSION + "-fl" + BuildConfig.FOXLOADER_VERSION + ".jar",
                "spark-" + BuildConfig.SPARK_VERSION + "-fabric.jar", // <- We support spark in FoxLoader
                "patching-" + BuildConfig.FOXLOADER_VERSION + ".jar",
                "jinput-platform-2.0.5-natives-linux.jar",
                "jinput-platform-2.0.5-natives-windows.jar",
                "jinput-platform-2.0.5-natives-osx.jar"
        ));
        String javaHome = System.getProperty("java.home");
        String classPath = System.getProperty("java.class.path");
        Pattern pattern = Pattern.compile(File.pathSeparator, Pattern.LITERAL);
        for (String path : pattern.split(classPath)) {
            if (path.startsWith(javaHome)) continue;
            File file = new File(path).getAbsoluteFile();
            if (foxLoaderFile.getPath().equals(file.getPath())) continue;
            if (file.isDirectory()) {
                try {
                    foxClassLoader.addFileToClassLoader(new FileInfo(file));
                } catch (IOException e) {
                    printEarlyStackTrace(e);
                }
            } else if (file.length() != 0 && !blackListedJars.contains(file.getName()) &&
                    !isLWJGLFileName(file.getName())) {
                filesToLoad.add(file);
            } else if (file.exists()) {
                try {
                    foxClassLoader.injectMissingFileInfoUnchecked(new FileInfo(file));
                } catch (IOException e) {
                    printEarlyStackTrace(e);
                }
            }
        }
    }

    private static boolean isLWJGLFileName(String fileName) {
        return fileName.startsWith("lwjgl-") || fileName.startsWith("lwjgl_util-") ||
                fileName.startsWith("jinput-") || fileName.startsWith("jutils-");
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

    public static List<File> filesToLoad() {
        return Collections.unmodifiableList(filesToLoad);
    }

    public static void installLoggerHelperOn(Logger logger) {
        if (hasLogger) LoggerHelper.installOn(logger);
    }

    public static void notifyJAnsiInstalled() {
        if (hasLogger) LoggerHelper.onJAnsiInstalled();
    }

    public static boolean wantJAnsi() {
        return hasLogger && LoggerHelper.consoleSupportColor &&
                (LoggerHelper.devEnvironment || !isClient());
    }

    public static boolean hasAnsi() {
        return LoggerHelper.consoleSupportColor;
    }

    private static boolean isDirGradle(File file) {
        return new File(file, "gradle").exists() && (
                new File(file, "build.gradle.kts").exists() ||
                        new File(file, "build.gradle").exists());
    }

    static void runClientWithArgs(String[] args) throws Throwable {
        CertificateHelper.install();
        Thread.currentThread().setContextClassLoader(foxClassLoader);
        MethodHandles.lookup().unreflect(
                Class.forName("com.fox2code.foxloader.loader.ModLoaderInit", true, foxClassLoader)
                        .getDeclaredMethod("launchModdedClient", String[].class)).invoke((Object) args);
    }

    static void runServerWithArgs(String[] args) throws Throwable {
        CertificateHelper.install();
        Thread.currentThread().setContextClassLoader(foxClassLoader);
        MethodHandles.lookup().unreflect(
                Class.forName("com.fox2code.foxloader.loader.ModLoaderInit", true, foxClassLoader)
                        .getDeclaredMethod("launchModdedServer", String[].class)).invoke((Object) args);
    }

    public static File getGameDir() {
        return gameDir;
    }

    public static FoxClassLoader getFoxClassLoader() {
        return foxClassLoader;
    }

    public static boolean isClient() {
        return environmentType == EnvironmentType.CLIENT;
    }

    public static boolean isServer() {
        return environmentType == EnvironmentType.SERVER;
    }

    public static EnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public static LauncherType getLauncherType() {
        return launcherType;
    }

    public static void printEarlyStackTrace(Throwable t) {
        t.printStackTrace(hasLogger ? System.err : System.out);
    }
}
