package com.fox2code.foxloader.spark;

import com.fox2code.foxloader.launcher.BuildConfig;
import me.lucko.spark.common.platform.PlatformInfo;

public class FoxLoaderSparkPlatformInfo implements PlatformInfo {
    private final Type type;

    public FoxLoaderSparkPlatformInfo(Type type) {
        this.type = type;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public String getName() {
        return "FoxLoader";
    }

    @Override
    public String getVersion() {
        return BuildConfig.FOXLOADER_VERSION;
    }

    @Override
    public String getMinecraftVersion() {
        return "ReIndev " + BuildConfig.REINDEV_VERSION;
    }
}
