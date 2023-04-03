package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.launcher.LauncherType;
import com.fox2code.foxloader.updater.JitPackUpdater;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

public final class FLJitPackUpdater extends JitPackUpdater {
    public FLJitPackUpdater() {
        super(ModLoader.foxLoader, "foxloader.version");
    }

    @Nullable
    @Override
    protected String findLatestVersion() throws IOException {
        if (ModLoader.foxLoader.file.getName().contains("-with-") ||
                FoxLauncher.getLauncherType() == LauncherType.UNKNOWN) {
            return null;
        }
        return super.findLatestVersion();
    }

    @Override
    protected void doUpdate() throws IOException {
        Objects.requireNonNull(ModLoader.foxLoader.getMod(), "WTF???");
        String latestVersion = this.getLatestVersion();
        if (FoxLauncher.getLauncherType() == LauncherType.GRADLE) {
            System.out.println("Change the dev plugin version to " + latestVersion + " to update FoxLoader");
            return;
        }
        System.out.println("Calling loaderHandleDoFoxLoaderUpdate");
        Mod mod = ModLoader.foxLoader.getMod();
        if (mod == null)
            throw new AssertionError("mod == null");
        mod.loaderHandleDoFoxLoaderUpdate(
                latestVersion, this.getUrlForLatestJar());
    }
}