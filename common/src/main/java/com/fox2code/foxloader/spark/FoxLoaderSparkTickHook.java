package com.fox2code.foxloader.spark;

import me.lucko.spark.common.tick.AbstractTickHook;

public class FoxLoaderSparkTickHook extends AbstractTickHook {
    private final FoxLoaderSparkPlugin foxLoaderSparkPlugin;

    public FoxLoaderSparkTickHook(FoxLoaderSparkPlugin foxLoaderSparkPlugin) {
        this.foxLoaderSparkPlugin = foxLoaderSparkPlugin;
    }

    @Override
    public void start() {
        this.foxLoaderSparkPlugin.tickHook = this;
    }

    public void close() {
        if (this.foxLoaderSparkPlugin.tickHook == this) {
            this.foxLoaderSparkPlugin.tickHook = null;
        }
    }

    @Override
    protected void onTick() {
        super.onTick();
    }
}
