package com.fox2code.foxloader.spark;

import me.lucko.spark.common.tick.AbstractTickHook;

final class FoxLoaderSparkTickHook extends AbstractTickHook {
    private final FoxLoaderSparkPlugin foxLoaderSparkPlugin;

    public FoxLoaderSparkTickHook(FoxLoaderSparkPlugin foxLoaderSparkPlugin) {
        this.foxLoaderSparkPlugin = foxLoaderSparkPlugin;
    }

    @Override
    public void start() {
        this.foxLoaderSparkPlugin.tickHook = this;
    }

    @Override
    public void close() {
        if (this.foxLoaderSparkPlugin.tickHook == this) {
            this.foxLoaderSparkPlugin.tickHook = null;
        }
    }

    void callOnTick() {
        super.onTick();
    }
}
