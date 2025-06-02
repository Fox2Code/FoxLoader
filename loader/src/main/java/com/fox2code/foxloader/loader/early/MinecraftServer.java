package com.fox2code.foxloader.loader.early;

import com.fox2code.foxloader.launcher.FoxLauncher;
import net.minecraft.common.CoreConstants;
import net.minecraft.common.ICoreAccess;
import net.minecraft.common.entity.player.PlayerInteractionHandler;
import net.minecraft.common.util.logging.LogAgent;

import java.io.File;

final class MinecraftServer implements ICoreAccess {
    @Override
    public File getMinecraftDir() {
        return FoxLauncher.getGameDir();
    }

    @Override
    public PlayerInteractionHandler getPlayerInteractionHandler() {
        throw new IllegalStateException("getPlayerInteractionHandler called too early");
    }

    @Override
    public void tickSprint(int i) {
        throw new IllegalStateException("tickSprint called too early");
    }

    @Override
    public LogAgent getLogger() {
        if (CoreConstants.CORE != this) {
            return CoreConstants.CORE.getLogger();
        } else {
            return EarlyLoader.EARLY_LOG_AGENT;
        }
    }
}
