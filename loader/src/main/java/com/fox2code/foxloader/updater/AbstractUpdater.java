package com.fox2code.foxloader.updater;

import com.fox2code.flexver.FlexVerPredicate;
import com.fox2code.foxloader.launcher.BuildConfig;
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
                !this.modContainer.getModInfo().flexver
                        .isGreaterOrEqual(this.latestVersion);
    }

    public boolean canUpdate() {
        return this.hasUpdate();
    }

    public static boolean reIndevVersionPatternMismatch(String accept) {
        return accept == null || !(accept.equals(BuildConfig.REINDEV_VERSION) ||
                FlexVerPredicate.parse(accept).match(BuildConfig.REINDEV_VERSION));
    }
}
