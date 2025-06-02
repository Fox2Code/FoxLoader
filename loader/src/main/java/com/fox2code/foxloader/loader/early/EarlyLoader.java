package com.fox2code.foxloader.loader.early;

import com.fox2code.foxloader.launcher.FoxLauncher;
import net.minecraft.common.CoreConstants;
import net.minecraft.common.util.logging.LogAgent;

/**
 * Initialize a ICoreAccess early to allow game code to work properly during pre-initialization.
 */
public final class EarlyLoader {
    static final LogAgent EARLY_LOG_AGENT = new LogAgent("EARLY", null);
    private EarlyLoader() { throw new AssertionError(); }

    public static void earlyLoad() {
        if (CoreConstants.CORE != null) return;
        if (FoxLauncher.isClient()) {
            CoreConstants.CORE = new Minecraft();
        } else {
            CoreConstants.CORE = new MinecraftServer();
        }
    }
}
