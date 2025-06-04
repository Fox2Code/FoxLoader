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
package com.fox2code.foxloader.updater;

import com.fox2code.flexver.FlexVer;
import com.fox2code.foxloader.dependencies.DependencyHelper;
import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.launcher.LauncherType;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.ModLoaderInit;
import com.fox2code.foxloader.utils.Platform;
import com.fox2code.foxloader.utils.io.NetUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public final class FoxLoaderUpdater extends MavenUpdater {
    private static final String ALL_LOADERS_URL = "https://cdn.fox2code.com/maven/foxloader-version-map.json";
    public static final FoxLoaderUpdater INSTANCE = new FoxLoaderUpdater();

    private FoxLoaderUpdater() {
        super(ModLoaderInit.getModContainer("foxloader"), DependencyHelper.FOX2CODE,
                "com.fox2code.FoxLoader:loader", "foxloader.version");
    }

    @Override
    protected String findLatestVersion() throws IOException {
        String allLoaders = NetUtils.downloadAsString(ALL_LOADERS_URL);
        JsonObject jsonObject = ModLoaderInit.gson.fromJson(allLoaders, JsonObject.class);
        FlexVer latestLoaderFlexVer = null;
        String latestLoaderVersion = null;
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String loaderVersion = entry.getKey();
            if (!loaderVersion.startsWith(BuildConfig.FOXLOADER_VERSION_MAJOR + ".")) {
                continue;
            }
            if (latestLoaderFlexVer == null || latestLoaderFlexVer.isLesser(loaderVersion)) {
                latestLoaderVersion = loaderVersion;
                latestLoaderFlexVer = FlexVer.parse(loaderVersion);
            }
        }
        this.latestMavenVersion = latestLoaderVersion;
        return latestLoaderVersion;
    }

    @Override
    protected void doUpdate() throws IOException {
        updateFoxLoaderImpl(this.latestMavenVersion, this.getUrlForLatestJar(), null);
    }

    public static boolean updateFoxLoaderFromMod(File file) throws IOException {
        if (!ModLoaderInit.Internal.isInPreBootupStage()) {
            throw new IllegalStateException("Cannot be called at runtime");
        }
        String fileName = file.getName();
        if (!((fileName.startsWith("foxloader-") ||
                fileName.startsWith("loader-")) &&
                fileName.endsWith(".jar"))) {
            return false;
        }
        String foxLoaderVersion, mainClass;
        try (JarFile jarFile = new JarFile(file)) {
            Attributes attributes = jarFile.getManifest().getMainAttributes();
            foxLoaderVersion = attributes.getValue("FoxLoader-Version");
            mainClass = attributes.getValue("Main-Class");
        } catch (IOException e) {
            return false;
        }
        // Allow FoxLoader when it goes through the installer, but not when it is from a server only jar.
        if (foxLoaderVersion == null || foxLoaderVersion.isEmpty() ||
                mainClass == null || !mainClass.startsWith("com.fox2code.foxloader.")) {
            if (mainClass != null && mainClass.startsWith("com.fox2code.foxloader.launcher.")) {
                throw new IOException("Cannot upgrade from a server only jar. (File: " + fileName + ")");
            }
            return false;
        }
        ModLoaderInit.getModLoaderLogger().info("Found FoxLoader " +
                foxLoaderVersion + " in the mods folder: " + fileName);
        if (FoxLauncher.DEV_MODE || FoxLauncher.DEVELOPING_FOXLOADER) {
            ModLoaderInit.getModLoaderLogger().info(
                    "Cannot update in a development environment, shutting down...");
            return true;
        }
        if (foxLoaderVersion.equals(BuildConfig.FOXLOADER_VERSION)) {
            ModLoaderInit.getModLoaderLogger().info(
                    "Cannot update to the same version as itself, shutting down...");
            return true;
        }
        ModLoaderInit.getModLoaderLogger().info("Updating to FoxLoader " + foxLoaderVersion);
        updateFoxLoaderImpl(foxLoaderVersion, null, file);
        ModLoaderInit.getModLoaderLogger().info("Update completed, shutting down...");
        return true;
    }

    private static void updateFoxLoaderImpl(String updateVersion, String remoteFoxLoaderJar, File localFoxLoaderJar) throws IOException {
        if (remoteFoxLoaderJar == null && localFoxLoaderJar == null) {
            throw new IOException("Both remoteFoxLoaderJar and localFoxLoaderJar are null...");
        }
        File dest = null;
        String[] args;
        LauncherType launcherType = FoxLauncher.getLauncherType();
        ModLoaderInit.getModLoaderLogger().info(
                "Updating to " + updateVersion + " from " + launcherType + " launcher");
        switch (launcherType) {
            case MMC_LIKE:
                File libraries = ModLoaderInit.getModContainer("foxloader").getModInfo().file.getParentFile();
                dest = new File(libraries, "foxloader-" + updateVersion + ".jar");
                // fall-through
            case VANILLA_LIKE:
                args = new String[]{null, "-jar", null, "--update", launcherType.name()};
                break;
            default:
                return;
        }
        if (dest == null) {
            File updateTmp = new File(ModLoader.getConfigFolder(), "update-tmp");
            if (!updateTmp.exists() && !updateTmp.mkdirs()) {
                ModLoaderInit.getModLoaderLogger()
                        .warning("Unable to create update tmp folder.");
                return;
            }
            dest = new File(updateTmp, "foxloader-" + updateVersion + ".jar");
        }
        if (BuildConfig.FOXLOADER_VERSION.equals(updateVersion) &&
                FoxLauncher.getLauncherType() != LauncherType.BIN) {
            // Can happen if wrongly installed
            if (!dest.equals(FoxLauncher.foxLoaderFile)) {
                Files.copy(FoxLauncher.foxLoaderFile.toPath(), dest.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } else if (localFoxLoaderJar != null && localFoxLoaderJar.exists()) {
            Files.copy(localFoxLoaderJar.toPath(), dest.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } else {
            try (FileOutputStream fileOutputStream = new FileOutputStream(dest)) {
                NetUtils.downloadTo(remoteFoxLoaderJar, fileOutputStream);
            }
        }
        args[0] = Platform.getPlatform().javaBin.getPath();
        args[2] = dest.getAbsolutePath();
        ModLoaderInit.getModLoaderLogger().info("Command: " + Arrays.toString(args));
        final Process process = new ProcessBuilder(args).directory(dest.getParentFile()).start();
        if (process.isAlive()) {
            new Thread(() -> {
                Scanner scanner = new Scanner(process.getInputStream());
                String line;
                while (process.isAlive() &&
                        (line = scanner.next()) != null) {
                    System.out.println("Update: " + line);
                    Thread.yield();
                }
                if (!process.isAlive()) {
                    System.out.println("Updated with exit code " + process.exitValue());
                }
            }, "Output log thread");
        } else {
            System.out.println("Updated with exit code " + process.exitValue());
        }
    }

    @Override
    public boolean canUpdate() {
        if (!(FoxLauncher.isClient() && super.hasUpdate())) {
            return false;
        }
        LauncherType launcherType = FoxLauncher.getLauncherType();
        switch (launcherType) {
            case MMC_LIKE:
            case VANILLA_LIKE:
                return true;
            default:
                return false;
        }
    }
}
