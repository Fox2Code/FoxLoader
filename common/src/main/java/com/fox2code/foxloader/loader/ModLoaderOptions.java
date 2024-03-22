package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.config.ConfigEntry;

public class ModLoaderOptions {
    public static final ModLoaderOptions INSTANCE = new ModLoaderOptions();

    private ModLoaderOptions() {}

    @ConfigEntry(configName = "Check for updates")
    public boolean checkForUpdates = true;
}
