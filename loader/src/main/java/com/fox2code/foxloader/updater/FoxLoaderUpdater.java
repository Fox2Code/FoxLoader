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
        File dest = null;
        String[] args;
        LauncherType launcherType = FoxLauncher.getLauncherType();
        ModLoaderInit.getModLoaderLogger().info(
                "Updating to " + this.latestMavenVersion + " from " + launcherType + " launcher");
        switch (launcherType) {
            default:
                return;
            case MMC_LIKE:
                File libraries = ModLoaderInit.getModContainer("foxloader").getModInfo().file.getParentFile();
                dest = new File(libraries, "foxloader-" + this.latestMavenVersion + ".jar");
            case VANILLA_LIKE:
                args = new String[]{null, "-jar", null, "--update", launcherType.name()};
        }
        if (dest == null) {
            File updateTmp = new File(ModLoader.getConfigFolder(), "update-tmp");
            if (!updateTmp.exists() && !updateTmp.mkdirs()) {
                ModLoaderInit.getModLoaderLogger()
                        .warning("Unable to create update tmp folder.");
                return;
            }
            dest = new File(updateTmp, "foxloader-" + this.latestMavenVersion + ".jar");
        }
        if (BuildConfig.FOXLOADER_VERSION.equals(this.latestMavenVersion) &&
                FoxLauncher.getLauncherType() != LauncherType.BIN) {
            // Can happen if wrongly installed
            if (!dest.equals(FoxLauncher.foxLoaderFile)) {
                Files.copy(FoxLauncher.foxLoaderFile.toPath(), dest.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            try (FileOutputStream fileOutputStream = new FileOutputStream(dest)) {
                NetUtils.downloadTo(this.getUrlForLatestJar(), fileOutputStream);
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
