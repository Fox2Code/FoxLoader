/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
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
package com.fox2code.foxloader.dev;

import com.fox2code.foxloader.dependencies.DependencyHelper;
import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.patching.dev.DevelopmentSourcePatcher;
import com.fox2code.foxloader.patching.game.GamePatches;
import com.fox2code.foxloader.utils.Platform;
import com.fox2code.foxloader.utils.SourceUtil;
import com.fox2code.foxloader.utils.io.CertificateHelper;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;

/**
 * Used for debugging FoxLoader dev plugin.
 */
public class Main {
    private static final File SELF_ABS_FILE = new File(".").getAbsoluteFile();
    private static final File PATCHED = new File(SELF_ABS_FILE, BuildConfig.DEV_PATCHED_JAR_NAME);
    private static final File UNPICKED = new File(SELF_ABS_FILE,
            BuildConfig.DEV_PATCHED_JAR_NAME.replace(".jar", "-unpicked.jar"));
    private static final File DECOMPILED = new File(SELF_ABS_FILE,
            BuildConfig.DEV_PATCHED_JAR_NAME.replace(".jar", "-sources.jar"));

    static {
        DependencyHelper.DependencyImpl.install(DevDependencyImpl.INSTANCE);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length == 0 || "help".equals(args[0])) {
            System.out.println("FoxLoader dev plugin cli debug!");
            System.out.println(" version -> Show FoxLoader version");
            System.out.println(" unpick  -> Full unpick test");
            System.out.println(" decomp  -> Full decompile test");
            return;
        }
        switch (args[0]) {
            case "version": {
                System.out.println(
                        "FoxLoader " + BuildConfig.FOXLOADER_VERSION +
                        " for ReIndev " + BuildConfig.REINDEV_VERSION);
                break;
            }
            case "unpick": {
                deleteAllDecompFiles();
                System.out.println("Loading patching dependencies...");
                loadPatchingDependencies();
                System.out.println("Acquiring slim jar...");
                File slimJar = DependencyHelper.loadDependencyAsFile(DependencyHelper.reIndevDependencySlim);
                System.out.println("Patching slim jar...");
                GamePatches.patchSlimJar(slimJar, PATCHED);
                System.out.println("Unpicking patched jar...");
                DevelopmentSourcePatcher.unpickPatchedJar(PATCHED, UNPICKED);
                break;
            }
            case "decomp": {
                if (DependencyHelper.vineFlower.javaSupport > Platform.getJvmVersion()) {
                    throw new RuntimeException("VineFlower requires at least java " +
                            DependencyHelper.vineFlower.javaSupport + " to run!");
                }
                deleteAllDecompFiles();
                System.out.println("Loading patching dependencies...");
                loadPatchingDependencies();
                DependencyHelper.loadDependencyAsFile(DependencyHelper.vineFlower);
                System.out.println("Acquiring slim jar...");
                File slimJar = DependencyHelper.loadDependencyAsFile(DependencyHelper.reIndevDependencySlim);
                System.out.println("Patching slim jar...");
                GamePatches.patchSlimJar(slimJar, PATCHED);
                System.out.println("Unpicking patched jar...");
                DevelopmentSourcePatcher.unpickPatchedJar(PATCHED, UNPICKED);
                System.out.println("Decompiling unpicked jar...");
                DecompileHelper.decompileExec();
                break;
            }
            default: {
                System.out.println("Unknown command: " + args[0]);
                break;
            }
        }
    }

    private static void loadPatchingDependencies() {
        CertificateHelper.initializeSafe();
        DependencyHelper.setMCLibraryRoot(new File(Platform.getAppDir("minecraft"), "libraries"));
        for (DependencyHelper.Dependency dependency : DependencyHelper.commonDependencies) {
            if (dependency.name.startsWith("org.ow2.asm:")) {
                DependencyHelper.loadDependencySelf(dependency);
            }
        }
    }

    private static void deleteAllDecompFiles() throws IOException {
        if (PATCHED.exists() && !PATCHED.delete()) {
            throw new IOException("Failed to delete patched file!");
        }
        if (UNPICKED.exists() && !UNPICKED.delete()) {
            throw new IOException("Failed to delete unpicked file!");
        }
        if (DECOMPILED.exists() && !DECOMPILED.delete()) {
            throw new IOException("Failed to delete decompiled file!");
        }
    }

    private static class DecompileHelper {
        private static void decompileExec() throws IOException, InterruptedException {
            // Equivalent of: new FoxLoaderDecompiler(unpickedJarFox, sourcesJarFox).decompile()
            FoxJavaExec foxJavaExec = new FoxJavaExec(Platform.getPlatform().javaBin);
            foxJavaExec.addFile(SourceUtil.getSourceFile(Type.class));
            foxJavaExec.addFile(SourceUtil.getSourceFile(DecompileHelper.class));
            foxJavaExec.addFile(DependencyHelper.loadDependencyAsFile(DependencyHelper.vineFlower));
            // An issue reported by SalTay seems to indicate this class
            // being accessible at runtime is sometimes a requirement?
            File sourceFile = SourceUtil.getSourceFileOfClassName(
                    "org.gradle.internal.classpath.Instrumented");
            if (sourceFile != null) {
                foxJavaExec.addFile(sourceFile);
            }
            foxJavaExec.setMainClass("com.fox2code.foxloader.decompiler.FoxLoaderDecompiler");
            foxJavaExec.exec("default", Main.UNPICKED.getAbsolutePath(), Main.DECOMPILED.getAbsolutePath());
        }
    }
}
