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

    public boolean hasClass(String cls) {
        return FoxLauncher.getFoxClassLoader().hasClass(cls);
    }

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

    public boolean isClassLoaderInitialized() {
        return FoxLauncher.getFoxClassLoader() != null;
    }

    public void printStackTrace(Throwable throwable) {
        FoxLauncher.printEarlyStackTrace(throwable);
    }

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
