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
package com.fox2code.foxloader.patching;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.dependencies.DependencyHelper;
import com.fox2code.foxloader.launcher.FileInfo;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.patching.game.GamePatches;
import com.fox2code.foxloader.patching.mixin.MixinModLoader;
import com.fox2code.foxloader.utils.Platform;
import com.fox2code.rebuild.ClassDataProvider;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Objects;

public final class PreLoader {
    private static final File tmpRoot = new File(FoxLauncher.getGameDir(), ".foxloader");
    private static final File tmpDir = new File(tmpRoot,
            File.separator + "internal" + File.separator + "patched");
    private static final File patchedFile = new File(tmpDir,
            "ReIndev-v" + BuildConfig.REINDEV_VERSION + "-fl" + BuildConfig.FOXLOADER_VERSION + ".jar");
    private static File devPatchedFile = null;
    // Use reference to allow memory to be freed on demand
    private static Reference<ClassDataProvider> classDataProviderReference;
    private static boolean aggressivelyFreeClassDataProvider = false;
    private static PatchingLoaderExtensions patchingLoaderExtensions;

    public static void initializePatching() {
        if (!tmpDir.isDirectory() && !tmpDir.mkdirs()) {
            throw new RuntimeException("Failed to create temporary patched directory");
        } else if (Platform.getPlatform() == Platform.WINDOWS) {
            try {
                Files.setAttribute(tmpRoot.toPath(), "dos:hidden", Boolean.TRUE);
            } catch (IOException ignored) {}
        }
        FoxLauncher.getFoxClassLoader().installMixinInfoPatch(PatchingLoaderExtensions::patchMixinInfo);
        IMixinTransformer mixinTransformer = MixinModLoader.initializeMixin(FoxLauncher.isClient());
        FoxLauncher.getFoxClassLoader().installWrappedExtensions(
                patchingLoaderExtensions = new PatchingLoaderExtensions(mixinTransformer));
        if (!FoxLauncher.DEVELOPING_FOXLOADER && !FoxLauncher.DEV_MODE) {
            try {
                if (!patchedFile.exists()) {
                    File file = DependencyHelper.loadDependencyAsFile(DependencyHelper.reIndevDependencySlim);
                    GamePatches.patchSlimJar(file, patchedFile);
                }
                FoxLauncher.getFoxClassLoader().setPatchedSlimInfo(new FileInfo(patchedFile));
            } catch (IOException e) {
                throw new RuntimeException("Failed to patch ReIndev", e);
            }
        }
    }

    public static File getPatchedFile() {
        if (FoxLauncher.DEVELOPING_FOXLOADER || FoxLauncher.DEV_MODE) {
            if (devPatchedFile != null) return devPatchedFile;
            try {
                URL resource = PreLoader.class.getResource("/font.txt");
                Objects.requireNonNull(resource, "resource");
                URLConnection urlConnection = resource.openConnection();
                File devPatchedFileTmp = null;
                if (urlConnection instanceof JarURLConnection) {
                    devPatchedFileTmp = new File(((JarURLConnection) urlConnection).getJarFileURL().toURI().getPath());
                }
                Objects.requireNonNull(devPatchedFileTmp, "devPatchedFileTmp");
                devPatchedFile = devPatchedFileTmp;
                return devPatchedFileTmp;
            } catch (IOException | URISyntaxException ioe) {
                throw new RuntimeException("Failed to load patchedFile path in dev mode", ioe);
            }
        }
        return patchedFile;
    }


    public static void addClassTransformer(ClassTransformer classTransformer) {
        patchingLoaderExtensions.addClassTransformer(Objects.requireNonNull(classTransformer));
    }

    public static ClassNode transformClassForMixins(
            FileInfo fileInfo, String className, ClassNode classNode) {
        return patchingLoaderExtensions.transformClassForMixins(fileInfo, className, classNode);
    }

    public static ClassDataProvider getClassDataProvider() {
        ClassDataProvider classDataProvider;
        if (classDataProviderReference != null &&
                (classDataProvider = classDataProviderReference.get()) != null) {
            return classDataProvider;
        }
        classDataProvider = new ClassDataProvider(FoxLauncher.getFoxClassLoader());
        classDataProviderReference = aggressivelyFreeClassDataProvider ?
                new WeakReference<>(classDataProvider) : new SoftReference<>(classDataProvider);
        return classDataProvider;
    }

    public static byte[] downgradeClassBytes(byte[] rawClassBytes, String className) {
        return patchingLoaderExtensions.downgradeClassBytes(rawClassBytes, className);
    }

    // Aggressively free the ClassDataProvider to reduce memory usage at the cost of class loading performance.
    public static void aggressivelyFreeClassDataProvider() {
        if (aggressivelyFreeClassDataProvider) return;
        aggressivelyFreeClassDataProvider = true;
        ClassDataProvider classDataProvider;
        if (classDataProviderReference != null &&
                (classDataProvider = classDataProviderReference.get()) != null) {
            classDataProviderReference = new WeakReference<>(classDataProvider);
        }
    }
}
