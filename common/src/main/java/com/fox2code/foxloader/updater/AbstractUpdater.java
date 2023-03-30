package com.fox2code.foxloader.updater;

import com.fox2code.foxloader.loader.ModContainer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public abstract class AbstractUpdater {
    public final ModContainer modContainer;
    String latestVersion;
    boolean updateConsumed;

    protected AbstractUpdater(ModContainer modContainer) {
        this.modContainer = modContainer;
    }

    @Nullable
    protected abstract String findLatestVersion() throws IOException;

    protected abstract void doUpdate() throws IOException;

    public final String getLatestVersion() {
        return this.latestVersion;
    }

    public boolean hasUpdate() {
        return this.latestVersion != null &&
                !this.latestVersion.equals(
                        this.modContainer.version);
    }
}
