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
package com.fox2code.foxloader.loader.java;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.dependencies.DependencyHelper;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.*;
import com.fox2code.foxloader.patching.ClassTransformer;
import com.fox2code.foxloader.patching.PreLoader;
import com.fox2code.foxloader.patching.mixin.MixinModLoader;
import com.fox2code.foxloader.updater.AbstractUpdater;
import com.fox2code.foxloader.updater.FoxLoaderUpdater;
import com.fox2code.foxloader.updater.MavenUpdater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class JavaLoadingPlugin extends LoadingPlugin {
    public static final JavaLoadingPlugin JAVA_LOADING_PLUGIN = new JavaLoadingPlugin();
    private static final boolean DISABLE_SPARK = Boolean.getBoolean("foxloader.disable-spark");
    private ModInfo reIndevModInfo;

    private JavaLoadingPlugin() {
        super("java");
    }

    public ModInfo getReIndevModInfo() throws IOException {
        if (this.reIndevModInfo == null) {
            this.reIndevModInfo = new JavaModInfo(PreLoader.getPatchedFile(), null,
                    "reindev", "ReIndev", BuildConfig.REINDEV_VERSION,
                    // I took that description from Silveros livestream description
                    "A mod designed to bring back the old feel of Minecraft while also extending it, " +
                            "by adding new biomes, blocks, mobs, items, and more!",
                    "Silveros, DasJafss, Fox2Code, icanttellyou, Zero_DSRS_VX, kivattt",
                    "icon32.png", "any", false, Long.MAX_VALUE, null);
        }
        return this.reIndevModInfo;
    }

    @Override
    public @Nullable ModInfo getModInfo(@NotNull File mod, @Nullable String jarPath) throws Exception {
        if (mod.getName().endsWith(".jar") && (jarPath == null || jarPath.endsWith(".jar"))) {
            JavaModInfo javaModInfo = new JavaModInfo(mod, jarPath);
            if (javaModInfo.id != null && !javaModInfo.id.isEmpty() &&
                    javaModInfo.forFoxLoaderVersion != null &&
                    (javaModInfo.forFoxLoaderVersion.equals("*") ||
                            javaModInfo.forFoxLoaderVersion.startsWith(
                                    BuildConfig.FOXLOADER_VERSION_MAJOR + "."))) {
                return javaModInfo;
            }
        }
        return null;
    }

    @Override
    public @NotNull List<ModInfo> getInjectedModInfo() {
        ArrayList<ModInfo> injected = new ArrayList<>();
        if (!DISABLE_SPARK) {
            File spark = DependencyHelper.loadDependencySafe(DependencyHelper.sparkDependency);
            if (spark != null) {
                try {
                    injected.add(new JavaModInfo(spark, null, "spark", "Spark", BuildConfig.SPARK_VERSION,
                            "Spark is a performance profiling plugin/mod for Minecraft clients, servers and proxies.\n\n" +
                                    "Note: This specific version is bundled with FoxLoader",
                            "Luck", "assets/spark/icon.png", "any", true, 0,
                            "com.fox2code.foxloader.spark.FoxLoaderSparkPlugin"));
                } catch (IOException e) {
                    ModLoaderInit.getModLoaderLogger().log(Level.WARNING, "Failed to add spark as a ModInfo");
                }
            }
        }
        return injected;
    }

    @Override
    public void preLoadModContainer(@NotNull ModContainer modContainer) {
        JavaModInfo modInfo = (JavaModInfo) modContainer.getModInfo();
        if (modInfo.classTransformer != null && !modInfo.classTransformer.isEmpty()) {
            try {
                PreLoader.addClassTransformer(Class.forName(
                        modInfo.classTransformer, false, FoxLauncher.getFoxClassLoader())
                        .asSubclass(ClassTransformer.class).getConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to load " + modInfo.classTransformer, e);
            }
        }
        if (modInfo.mixin != null && !modInfo.mixin.isEmpty()) {
            MixinModLoader.addMixinConfigurationSafe(modInfo.id, modInfo.mixin, modInfo.explicitMixin);
        }
    }

    @Override
    public @Nullable Mod loadModContainer(@NotNull ModContainer modContainer) throws ReflectiveOperationException {
        // We are guaranteed to have created that mod info ourselves, so the cast is fine.
        JavaModInfo modInfo = (JavaModInfo) modContainer.getModInfo();
        if (modInfo.main != null && !modInfo.main.isEmpty()) {
            Constructor<? extends Mod> constructor = Class.forName(
                    modInfo.main, false, FoxLauncher.getFoxClassLoader())
                    .asSubclass(Mod.class).getConstructor();
            if (constructor.getDeclaringClass().getClassLoader() ==
                    JavaLoadingPlugin.class.getClassLoader()) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance();
        }
        return null;
    }

    @Override
    public int getModDisplayFlags(ModContainer modContainer) {
        JavaModInfo javaModInfo = ((JavaModInfo) modContainer.getModInfo());
        switch (javaModInfo.id) {
            case "reindev":
                return DISPLAY_FLAG_GAME_CONTENT;
            case "foxloader":
                return DISPLAY_FLAG_LIBRARY;
        }
        int displayFlags = 0;
        if (javaModInfo.explicitMixin || (javaModInfo.mixin != null &&
                FoxLauncher.getFoxClassLoader().findResource(javaModInfo.mixin) != null)) {
            displayFlags |= DISPLAY_FLAG_MIXIN;
        }
        if (javaModInfo.loadingPlugin != null && !javaModInfo.loadingPlugin.isEmpty()) {
            displayFlags |= DISPLAY_FLAG_LIBRARY;
        }
        return displayFlags;
    }

    @Override
    public @Nullable AbstractUpdater makeModContainerUpdater(ModContainer modContainer) {
        JavaModInfo javaModInfo = ((JavaModInfo) modContainer.getModInfo());
        if ("foxloader".equals(javaModInfo.id)) {
            return FoxLoaderUpdater.INSTANCE;
        }
        if (javaModInfo.jitPack != null) {
            return new MavenUpdater(modContainer, "https://jitpack.io/", javaModInfo.jitPack);
        }
        return null;
    }
}
