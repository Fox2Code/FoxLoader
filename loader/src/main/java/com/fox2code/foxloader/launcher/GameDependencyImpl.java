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
package com.fox2code.foxloader.launcher;

import com.fox2code.foxloader.dependencies.DependencyFileInfo;
import com.fox2code.foxloader.dependencies.DependencyHelper;
import com.fox2code.foxloader.utils.Platform;

import java.io.File;
import java.io.IOException;
import java.net.URL;

final class GameDependencyImpl extends DependencyHelper.DependencyImpl {
    static final GameDependencyImpl INSTANCE = new GameDependencyImpl();

    private GameDependencyImpl() {}

    @Override
    public boolean isDevelopingFoxLoader() {
        return FoxLauncher.DEVELOPING_FOXLOADER;
    }

    @Override
    public boolean isDevelopingMod() {
        return FoxLauncher.DEV_MODE;
    }

    @Override
    public boolean hasClass(String cls) {
        return FoxLauncher.getFoxClassLoader().hasClass(cls);
    }

    @Override
    public void addDependency(File file, DependencyHelper.Dependency dependency, boolean isMinecraft) throws IOException {
        FoxLauncher.filesToLoad.remove(file);
        if (isMinecraft) {
            FoxLauncher.getFoxClassLoader().setOriginalSlimFileInfo(new DependencyFileInfo(file, dependency));
        } else {
            FoxLauncher.getFoxClassLoader().addFileToClassLoader(new DependencyFileInfo(file, dependency));
        }
    }

    @Override
    public void checkAddDependency(File file, DependencyHelper.Dependency dependency) throws IOException {
        FoxClassLoader foxClassLoader = FoxLauncher.getFoxClassLoader();
        if (!foxClassLoader.isFileInClassLoader(file)) {
            foxClassLoader.addFileToClassLoader(new DependencyFileInfo(file, dependency));
        }
    }

    @Override
    public boolean isClassLoaderInitialized() {
        return FoxLauncher.getFoxClassLoader() != null;
    }

    @Override
    public void printStackTrace(Throwable throwable) {
        FoxLauncher.printEarlyStackTrace(throwable);
    }

    @Override
    public File checkMCLibraryRoot(File mcLibraries) {
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
        if (mcLibraries == null) {
            mcLibraries = new File(mcLibrariesPath + "libraries");
        } else if (FoxLauncher.launcherType == LauncherType.UNKNOWN &&
                mcLibrariesPath.equals(mcLibraries.getAbsoluteFile().getParent())) {
            FoxLauncher.launcherType = LauncherType.VANILLA_LIKE;
        }
        return mcLibraries;
    }

    @Override
    public URL getClassResource(String className) {
        return FoxLauncher.getFoxClassLoader()
                .getResource(className.replace('.', '/') + ".class");
    }
}
