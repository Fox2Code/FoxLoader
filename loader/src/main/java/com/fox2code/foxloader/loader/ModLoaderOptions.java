package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.config.ConfigEntry;

public final class ModLoaderOptions {
    public static final ModLoaderOptions INSTANCE = new ModLoaderOptions();

    private ModLoaderOptions() {}

    @ConfigEntry(configComment = "FoxLoader will check for updates when it boot-up")
    public boolean checkForUpdates = true;

    @ConfigEntry(configComment =
            "Max sink priority value allowed in cables\n" +
            "This is effectively the maximum update range for cables.",
            lowerBounds = 4, upperBounds = 512)
    public int maxSinkPriorityValue = 256;
}
